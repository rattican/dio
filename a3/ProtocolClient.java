package a3;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import java.util.ArrayList;

import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private GhostNPC ghostNPC;
	//array list for all dolphins
	private ArrayList<GhostNPC> npcList = new ArrayList<>();

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	private GhostNPC findGhostNPC(int id) {
        for (GhostNPC npc : npcList) {
            if (npc.getUniqueID() == id) {
                return npc;
            }
        }
        return null;
    }
	
	@Override
    protected void processPacket(Object message)
    {   
        String strMessage = (String)message;
        System.out.println("message received -->" + strMessage);
        String[] messageTokens = strMessage.split(",");
        
        if(messageTokens.length > 0)
        {
            // 1. Handle Spawning (createNPC)
            // Packet Format: createNPC, npcID, x, y, z, yaw
            if (messageTokens[0].compareTo("createNPC") == 0) { 
                int npcId = Integer.parseInt(messageTokens[1]); 
                Vector3f ghostPosition = new Vector3f(
                    Float.parseFloat(messageTokens[2]), 
                    Float.parseFloat(messageTokens[3]), 
                    Float.parseFloat(messageTokens[4])  
                );
                float ghostYaw = Float.parseFloat(messageTokens[5]); 
                
                try {
                    createGhostNPC(npcId, ghostPosition);
                    GhostNPC target = findGhostNPC(npcId);
                    if (target != null) {
                        target.setLocalRotation(new org.joml.Matrix4f().rotationY((float)java.lang.Math.toRadians(ghostYaw)));
                    }
                } catch (IOException e) {
                    System.out.println("Failed to spawn Ghost NPC ID: " + npcId);
                }
            }

            // 2. Handle Movement Updates (mnpc)
            // Packet Format: mnpc, npcID, x, y, z, yaw
            if (messageTokens[0].compareTo("mnpc") == 0) {
                int npcId = Integer.parseInt(messageTokens[1]); 
                Vector3f ghostPosition = new Vector3f(
                    Float.parseFloat(messageTokens[2]), 
                    Float.parseFloat(messageTokens[3]), 
                    Float.parseFloat(messageTokens[4])  
                );
                float ghostYaw = Float.parseFloat(messageTokens[5]);
                
                // FIXED: Pass the npcId to update the correct dolphin!
                updateGhostNPC(npcId, ghostPosition, 1.0);
                
                GhostNPC target = findGhostNPC(npcId);
                if (target != null) {
                    target.setLocalRotation(new org.joml.Matrix4f().rotationY((float)java.lang.Math.toRadians(ghostYaw)));
                }
            }

            // 3. Handle Distance Checks (isnr)
            // Packet Format: isnr, npcID, x, y, z, criteria
            if (messageTokens[0].compareTo("isnr") == 0) {
                int npcId = Integer.parseInt(messageTokens[1]); // ID is token 1
                Vector3f npcPosition = new Vector3f(
                    Float.parseFloat(messageTokens[2]), // X is token 2
                    Float.parseFloat(messageTokens[3]), // Y is token 3
                    Float.parseFloat(messageTokens[4])  // Z is token 4
                );
                float criteria = Float.parseFloat(messageTokens[5]); // Criteria is token 5
            
                // Calculate distance between local player (dio/miku) and this specific dolphin
                float dist = game.getPlayerPosition().distance(npcPosition);
            
                // Send proximity update back to the server for this specific dolphin ID
                try {
                    if (dist < criteria) {
                        sendPacket("isnear," + id.toString() + "," + npcId + ",true");
                    } else {
                        sendPacket("isnear," + id.toString() + "," + npcId + ",false");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 4. Handle JOIN message
            if(messageTokens[0].compareTo("join") == 0)
            {   
                if(messageTokens[1].compareTo("success") == 0)
                {   
                    System.out.println("join success confirmed");
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                    try {
                        sendPacket("needNPC," + id.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(messageTokens[1].compareTo("failure") == 0)
                {   
                    System.out.println("join failure confirmed");
                    game.setIsConnected(false);
                }   
            }
            
            // 5. Handle BYE message
            if(messageTokens[0].compareTo("bye") == 0)
            {   
                UUID ghostID = UUID.fromString(messageTokens[1]);
                ghostManager.removeGhostAvatar(ghostID);
            }
            
            // 6. Handle CREATE or DETAILS_FOR messages
            if (messageTokens[0].compareTo("create") == 0 || messageTokens[0].compareTo("dsfr") == 0) {
                UUID ghostID;
                Vector3f ghostPos;
                String modelName;
                float yaw = 0f;

                if (messageTokens[0].equals("create")) {
                    ghostID = UUID.fromString(messageTokens[1]);
                    ghostPos = new Vector3f(
                        Float.parseFloat(messageTokens[2]),
                        Float.parseFloat(messageTokens[3]),
                        Float.parseFloat(messageTokens[4]));
                    if (messageTokens.length == 7) {
                        yaw = Float.parseFloat(messageTokens[5]);
                        modelName = messageTokens[6];
                    } else {
                        yaw = 135.0f; 
                        modelName = messageTokens[5];
                    }
                } else {
                    ghostID = UUID.fromString(messageTokens[1]);
                    ghostPos = new Vector3f(
                        Float.parseFloat(messageTokens[2]),
                        Float.parseFloat(messageTokens[3]),
                        Float.parseFloat(messageTokens[4]));
                    if (messageTokens.length == 7) {
                        yaw = Float.parseFloat(messageTokens[5]);
                        modelName = messageTokens[6];
                    } else {
                        yaw = 135.0f; 
                        modelName = messageTokens[5];
                    }
                }

                try {
                    ghostManager.createGhostAvatar(ghostID, ghostPos, yaw, modelName);
                } catch (IOException e) { 
                    e.printStackTrace(); 
                }
            }

            // 7. Handle WANTS_DETAILS message
            if (messageTokens[0].compareTo("wsds") == 0)
            {
                UUID ghostID = UUID.fromString(messageTokens[1]);
                sendDetailsForMessage(ghostID, game.getPlayerPosition());
            }
            
            // 8. Handle MOVE message
            if (messageTokens[0].compareTo("move") == 0)
            {
                UUID ghostID = UUID.fromString(messageTokens[1]);
                Vector3f ghostPosition = new Vector3f(
                    Float.parseFloat(messageTokens[2]),
                    Float.parseFloat(messageTokens[3]),
                    Float.parseFloat(messageTokens[4]));

                float yaw = 135.0f; 
                float scale = 1.5f; 
                if (messageTokens.length > 5) {
                    yaw = Float.parseFloat(messageTokens[5]);
                }
                if (messageTokens.length > 6) {
                    scale = Float.parseFloat(messageTokens[6]);
                }
                ghostManager.updateGhostAvatar(ghostID, ghostPosition, yaw, scale);
            } 
        }   
    }

    //--------GHOST NPC SECTIONs -------
    private void createGhostNPC(int id, Vector3f position) throws IOException {
        if (findGhostNPC(id) == null) {
            if (game.getNPCshape() == null || game.getNPCtexture() == null) {
                System.out.println("CRITICAL WARNING: Client NPC shape or texture is NULL!");
                return;
            }

            GhostNPC newDolphin = new GhostNPC(id, game.getNPCshape(), game.getNPCtexture(), position);
            
            newDolphin.setLocalScale((new org.joml.Matrix4f()).scaling(3.0f)); 
            newDolphin.getRenderStates().setRenderHiddenFaces(true);
            newDolphin.getRenderStates().hasLighting(true);
            
            npcList.add(newDolphin);
            System.out.println("Successfully registered Dolphin ID: " + id + " to rendering list.");
        }
    }

    private void updateGhostNPC(int id, Vector3f position, double gsize) {
        boolean gs;
        GhostNPC targetDolphin = findGhostNPC(id);
        
        if (targetDolphin == null) {
            try {
                createGhostNPC(id, position);
                targetDolphin = findGhostNPC(id);
            } catch (IOException e) {
                System.out.println("error creating ghost npc");
            }
        }
        
        if (targetDolphin != null) {
            targetDolphin.setPosition(position); 
            
            if (gsize == 1.0) gs = false; else gs = true;
            targetDolphin.setSize(gs);
        }
    }

    //-------- STANDARD MULTIPLAYER MESSAGES --------
    
    public void sendJoinMessage()
    {   try 
        {   sendPacket(new String("join," + id.toString()));
        } catch (IOException e) 
        {   e.printStackTrace();
        }   
    }
    
    public void sendByeMessage()
    {   try 
        {   sendPacket(new String("bye," + id.toString()));
        } catch (IOException e) 
        {   e.printStackTrace();
        }   
    }
    
    public void sendCreateMessage(Vector3f position)
    {   
        try {   
            String message = "create," + id.toString();
            message += "," + position.x() + "," + position.y() + "," + position.z();
            message += "," + game.getPlayerYaw();
            message += "," + game.getAvatarType(); 
            sendPacket(message);
        } 
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public void sendDetailsForMessage(UUID remoteId, Vector3f position)
    {   
        try {   
            String message = "dsfr," + remoteId.toString() + "," + id.toString();
            message += "," + position.x() + "," + position.y() + "," + position.z();
            message += "," + game.getPlayerYaw();
            message += "," + game.getAvatarType();
            sendPacket(message);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sendMoveMessage(Vector3f position)
    {   try 
        {   String message = new String("move," + id.toString());
            message += "," + position.x();
            message += "," + position.y();
            message += "," + position.z();
            message += "," + game.getPlayerYaw();
            message += "," + (game.getAvatarType().equalsIgnoreCase("miku") ? 0.55f : 1.5f); // RKS (Scale)
            sendPacket(message);
        } catch (IOException e) 
        {   e.printStackTrace();
        }   
    }
}