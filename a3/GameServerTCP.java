package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerTCP extends GameConnectionServer<UUID> 
{
	public GameServerTCP(int localPort) throws IOException 
	{		super(localPort, ProtocolType.TCP);
	}
	
	@Override
	public void acceptClient(IClientInfo ci, Object o)
	{
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// JOIN -- Case where client just joined the server
			// Received Message Format: (join,localId)
			if(messageTokens[0].compareTo("join") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				addClient(ci, clientID);
				sendJoinedMessage(clientID, true);
			}
		}
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// Case where client just joined the server
			// Received Message Format: (join,localId)
			/*
			if(messageTokens[0].compareTo("join") == 0)
			{
				try 
				{	IClientInfo ci;
					ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);			
					addClient(ci, clientID);
					sendJoinedMessage(clientID, true);
				} 
				catch (IOException e) 
				{	e.printStackTrace();
				}
			}
			*/
			
			// Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
			}
			
			// Case where server receives a CREATE message
			// Received Message Format: (create,localId,x,y,z,yaw,model) or (create,localId,x,y,z,model)
			if(messageTokens[0].compareTo("create") == 0)
			{   UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				String yaw;
				String model;
				if (messageTokens.length == 7) {
					// New format with yaw: 7 tokens
					yaw = messageTokens[5];
					model = messageTokens[6];
				} else {
					// Old format without yaw: 6 tokens
					yaw = "135.0";
					model = messageTokens[5];
				}
				sendCreateMessages(clientID, pos, yaw, model);
				sendWantsDetailsMessages(clientID);
			}
			
			// Case where server receives a DETAILS-FOR message
			// Received Message Format: (dsfr,remoteId,localId,x,y,z,yaw,model) or (dsfr,remoteId,localId,x,y,z,model)
			if(messageTokens[0].compareTo("dsfr") == 0)
			{   UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				String yaw;
				String model;
				if (messageTokens.length == 8) {
					// New format with yaw: 8 tokens
					yaw = messageTokens[6];
					model = messageTokens[7];
				} else {
					// Old format without yaw: 7 tokens
					yaw = "135.0";
					model = messageTokens[6];
				}
				sendDetailsForMessage(clientID, remoteID, pos, yaw, model);
			}
			
			// Case where server receives a MOVE message
			// Received Message Format: (move,localId,x,y,z,yaw) or (move,localId,x,y,z)
			if(messageTokens[0].compareTo("move") == 0)
			{   UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				String yaw;
				if (messageTokens.length >= 6) {
					// New format with yaw
					yaw = messageTokens[5];
				} else {
					// Old format without yaw
					yaw = "135.0";
				}
				sendMoveMessages(clientID, pos, yaw);
			}
		}
	}

	/**
	 * Informs the client who just requested to join the server if their if their 
	 * request was able to be granted. 
	 * <p>
	 * Message Format: (join,success) or (join,failure)
	 */
	public void sendJoinedMessage(UUID clientID, boolean success)
	{	try 
		{	String message = new String("join,");
			if(success)
				message += "success";
			else
				message += "failure";
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Informs a client that the avatar with the identifier remoteId has left the server. 
	 * This message is meant to be sent to all client currently connected to the server 
	 * when a client leaves the server.
	 * <p>
	 * Message Format: (bye,remoteId)
	 */
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Informs a client that a new avatar has joined the server with the unique identifier 
	 * remoteId. This message is intended to be send to all clients currently connected to 
	 * the server when a new client has joined the server and sent a create message to the 
	 * server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	 * connected to the server. 
	 * <p>
	 * Message Format: (create,remoteId,x,y,z,yaw,model) where x, y, z represent the position and yaw is rotation
	 */
	public void sendCreateMessages(UUID clientID, String[] position, String yaw, String model)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			message += "," + model;
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Informs a client of the details for a remote clients avatar. This message is in response 
	 * to the server receiving a DETAILS_FOR message from a remote client. That remote clients 
	 * messages localId becomes the remoteId for this message, and the remote clients messages 
	 * remoteId is used to send this message to the proper client. 
	 * <p>
	 * Message Format: (dsfr,remoteId,x,y,z,yaw,model) where x, y, z represent the position and yaw is rotation.
	 */
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position, String yaw, String model)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			message += "," + model;
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Informs a local client that a remote client wants the local clients avatars information. 
	 * This message is meant to be sent to all clients connected to the server when a new client 
	 * joins the server. 
	 * <p>
	 * Message Format: (wsds,remoteId)
	 */
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Informs a client that a remote clients avatar has changed position. x, y, and z represent 
	 * the new position of the remote avatar. This message is meant to be forwarded to all clients
	 * connected to the server when it receives a MOVE message from the remote client.   
	 * <p>
	 * Message Format: (move,remoteId,x,y,z,yaw) where x, y, z represent the position and yaw is rotation.
	 */
	public void sendMoveMessages(UUID clientID, String[] position, String yaw)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
}
