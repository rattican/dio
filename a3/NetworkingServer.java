package a3;

import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer 
{
	private GameAIServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;
	private NPCController npcCtrl;

	public NetworkingServer(int serverPort, String protocol) 
	{	
		npcCtrl = new NPCController();
		try 
		{	if(protocol.toUpperCase().compareTo("TCP") == 0)
			{	
				thisTCPServer = new GameServerTCP(serverPort);
			}
			else
			{	
				thisUDPServer = new GameAIServerUDP(serverPort, npcCtrl);
			}
		} 
		catch (IOException e) 
		{	
			e.printStackTrace();
		}
		if (thisUDPServer != null) {
            npcCtrl.start(thisUDPServer); 
            System.out.println("Warning: NPC Controller did not start because UDP Server is null (TCP was selected).");
        }
	}

	public static void main(String[] args) 
	{	if(args.length > 1)
		{	
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}
