package a3;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			if (messageTokens[0].compareTo("create") == 0 || messageTokens[0].compareTo("dsfr") == 0) {
    			UUID ghostID;
    			Vector3f ghostPos;
    			String modelName;

		float yaw = 0f;
		if (messageTokens[0].equals("create")) {
			// Format: create, uuid, x, y, z, [yaw], model
			// Handle both old format (6 tokens) and new format (7 tokens with yaw)
			ghostID = UUID.fromString(messageTokens[1]);
			ghostPos = new Vector3f(
				Float.parseFloat(messageTokens[2]),
				Float.parseFloat(messageTokens[3]),
				Float.parseFloat(messageTokens[4]));
			if (messageTokens.length == 7) {
				// New format with yaw: create, uuid, x, y, z, yaw, model
				yaw = Float.parseFloat(messageTokens[5]);
				modelName = messageTokens[6];
			} else {
				// Old format without yaw: create, uuid, x, y, z, model
				yaw = 135.0f; // default yaw
				modelName = messageTokens[5];
			}
		} else {
			// Format: dsfr, remoteId, x, y, z, [yaw], model
			// Handle both old format (6 tokens) and new format (7 tokens with yaw)
			ghostID = UUID.fromString(messageTokens[1]);
			ghostPos = new Vector3f(
				Float.parseFloat(messageTokens[2]),
				Float.parseFloat(messageTokens[3]),
				Float.parseFloat(messageTokens[4]));
			if (messageTokens.length == 7) {
				// New format with yaw: dsfr, remoteId, x, y, z, yaw, model
				yaw = Float.parseFloat(messageTokens[5]);
				modelName = messageTokens[6];
			} else {
				// Old format without yaw: dsfr, remoteId, x, y, z, model
				yaw = 135.0f; // default yaw
				modelName = messageTokens[5];
			}
		}

		try {
			ghostManager.createGhostAvatar(ghostID, ghostPos, yaw, modelName);
			} catch (IOException e) { 
				e.printStackTrace(); 
			}
	}

			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z[,yaw])
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				float yaw = 135.0f; // default yaw
				if (messageTokens.length > 5) {
					// New format includes yaw
					yaw = Float.parseFloat(messageTokens[5]);
				}
				ghostManager.updateGhostAvatar(ghostID, ghostPosition, yaw);
	}	}	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the clients Avatars position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z,yaw,model) where x, y, z represent position and yaw is rotation

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
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z,yaw,model) where x, y, z represent position and yaw is rotation.

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
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z,yaw) where x, y, z represent position and yaw is rotation.

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + game.getPlayerYaw();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
}
