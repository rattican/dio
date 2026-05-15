package a3;

import java.util.ArrayList;
import java.util.Random;

public class NPCController 
{
    Random rand = new Random();
    private GameAIServerUDP server;
    private ArrayList<NPC> npcList = new ArrayList<>();

    private float criteria = 8.0f; // Distance threshold to trigger spinning
    private long lastTickUpdateTime;

    public NPCController() 
    {
        for (int i = 0; i < 5; i++) {
        NPC npc = new NPC(i, false);
        npc.randomizeLocation(rand.nextInt(40)-20, rand.nextInt(20)-10);
        npcList.add(npc);
        }
    // Spawn 2 enemy dolphins (IDs 5 and 6)
        for (int i = 5; i < 7; i++) {
            NPC npc = new NPC(i, true);
            npc.randomizeLocation(rand.nextInt(40)-20, rand.nextInt(20)-10);
            npc.setSpeed(0.15); // Make them slightly faster!
            npcList.add(npc);
        }
    }

    public void start(GameAIServerUDP s) {
        server = s;
        npcLoop();
    }
    //the server will delete the NPC from array list
    public void removeNPC(int id) {
        for (int i = 0; i < npcList.size(); i++) {
            if (npcList.get(i).getId() == id) {
                npcList.remove(i);
                break;
            }
        }
    }

    public ArrayList<NPC> getNPCList() { return npcList; }
    public float getCriteria() { return criteria; }

    public void npcLoop() {
        lastTickUpdateTime = System.nanoTime();

        while (true) {
            long currentTime = System.nanoTime();
            float elapsedTickMilliSecs = (currentTime - lastTickUpdateTime) / 1000000.0f;

            // Tick physics, AI, and networking every 25ms
            if (elapsedTickMilliSecs >= 25.0f) {
                lastTickUpdateTime = currentTime;
                
                // Process behavior logic and coordinate translation for EACH dolphin independently
                for (NPC npc : npcList) {
                    // 1. Proximity Check: Is the player/Miku near THIS specific dolphin?
                    boolean playerIsNear = false;
                    // Look through active clients on the server to measure physical distance
                    if (server != null) {
                        playerIsNear = npc.isPlayerNear();
                    }
                    if (npc.isEnemy()) {
                        // RED DOLPHINS: Force them to keep updating/moving regardless of proximity
                         npc.updateLocation(); 
                    } else {
                     // GREEN DOLPHINS: Only move if a player is NOT near them (they freeze when you approach)
                        if (!playerIsNear) {
                            npc.updateLocation();
                        } 
                    }
                }
                // 4. Broadcast the synchronized coordinates of all 5 dolphins
                if (server != null) {
                    server.sendNPCinfo();
                    server.sendCheckForAvatarNear();
                }
            }
            
            Thread.yield();
        }
    }
    public NPC getNPC() {
        if (npcList != null && !npcList.isEmpty()) {
            return npcList.get(0);
        }
        return null;
    }
    public boolean getNearFlag() {
        // If any of our 5 dolphins have a player near them, return true
        for (NPC npc : npcList) {
            if (npc.isPlayerNear()) {
                return true;
            }
        }
        return false;
    }
    public void setNearFlag(boolean v) {
        // Update near status for all dolphins
        for (NPC npc : npcList) {
            npc.setPlayerNear(v);
        }
    }
}