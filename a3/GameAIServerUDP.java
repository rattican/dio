package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import tage.networking.server.GameConnectionServer;
import tage.networking.IGameConnection.ProtocolType;
//written by Hoi Yin Li for NPC server base on the lecture code

public class GameAIServerUDP extends GameServerUDP
{
    NPCController npcCtrl;
    public GameAIServerUDP(int localPort, NPCController npc) throws IOException 
    {	
        super(localPort);
        npcCtrl = npc;
    }
    //----additional protocol for NPC:

    //ask clients if their avatar is near the NPC
    public void sendCheckForAvatarNear()
    { 
        try { 
            for (NPC npc : npcCtrl.getNPCList()) {
                String message = new String("isnr");
                message += "," + npc.getId(); // Index 1: Dolphin ID
                message += "," + npc.getX();  // Index 2: X
                message += "," + npc.getY();  // Index 3: Y
                message += "," + npc.getZ();  // Index 4: Z
                message += "," + npcCtrl.getCriteria(); // Index 5: Proximity Threshold (8.0)
                sendPacketToAll(message);
            }
        }
        catch (IOException e) { 
            System.out.println("couldnt send isnr msg"); e.printStackTrace(); 
        }
    }

    public void sendNPCinfo() {
        try {
            // Loop through and broadcast coordinates for all 5 dolphins
            for (NPC npc : npcCtrl.getNPCList()) {
                String message = new String("mnpc"); 
                message += "," + npc.getId(); // Sends unique ID so client knows which one to move
                message += "," + npc.getX();
                message += "," + npc.getY();
                message += "," + npc.getZ();
                message += "," + npc.getYaw();
                message += "," + (npc.isEnemy() ? "1" : "0");
                sendPacketToAll(message);
            }
        } catch (IOException e) {
            System.out.println("couldnt send NPC info");
            e.printStackTrace();
        }
    }
    @Override
    public void processPacket(Object o, InetAddress senderIP, int port)
    {
        super.processPacket(o, senderIP, port);

        String strMessage = (String) o;
        String[] messageTokens = strMessage.split(",");
        
        if (messageTokens.length > 0) {
            if (messageTokens[0].compareTo("needNPC") == 0) { 
                System.out.println("Server sending initial spawns to client...");
                UUID clientID = UUID.fromString(messageTokens[1]);
                sendNPCstart(clientID);
            }
            
            // Expected packet format: isnear, [clientID], [npcID], [true/false]
            if (messageTokens[0].compareTo("isnear") == 0) { 
                try {
                    int npcID = Integer.parseInt(messageTokens[2]); // Index 2 is the Dolphin ID
                    boolean isNear = Boolean.parseBoolean(messageTokens[3]); // Index 3 is true/false
                    
                    for (NPC npc : npcCtrl.getNPCList()) {
                        if (npc.getId() == npcID) {
                            npc.setPlayerNear(isNear); // Mark this specific dolphin!
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void handleNearTiming(UUID clientID)
    { 
        npcCtrl.setNearFlag(true);
    }
    //tell the client about the NPC that is spawned
    public void sendCreateNPCmsg(UUID clientID, String[] position)
    { 
        try
            { System.out.println("server telling clients about an NPC");
            String message = new String("createNPC," + clientID.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            forwardPacketToAll(message, clientID);
            } 
        catch (IOException e) { e.printStackTrace(); }
    }
    public void sendNPCstart(UUID clientID) {
        try {
            // Tell a newly connected client to spawn all dolphins
            for (NPC npc : npcCtrl.getNPCList()) {
                String message = new String("createNPC");
                message += "," + npc.getId(); // Sends unique ID
                message += "," + npc.getX();
                message += "," + npc.getY();
                message += "," + npc.getZ();
                message += "," + npc.getYaw();
                message += "," + (npc.isEnemy() ? "1" : "0"); // 1 for Enemy, 0 for Friend
                sendPacket(message, clientID); 
            }
        } catch (IOException e) {
            System.out.println("could not send createNPC message");
            e.printStackTrace();
        }
    }
}