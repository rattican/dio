package a3;

import java.util.ArrayList;
import java.util.Random;

public class NPCController 
{
    private GameAIServerUDP server;
    private ArrayList<NPC> npcList = new ArrayList<>();
    private final int NUM_DOLPHINS = 5; // Spawns exactly 5 dolphins!

    private float criteria = 8.0f; // Distance threshold to trigger spinning
    private long lastTickUpdateTime;

    public NPCController() 
    {
        Random rand = new Random();
        // Instantiate 5 dolphins at different random points on the map
        for (int i = 0; i < NUM_DOLPHINS; i++) {
            NPC npc = new NPC(i);
            // Randomize starting positions so they are spread out
            int rx = rand.nextInt(40) - 20; // range: -20 to 20
            int rz = rand.nextInt(20) - 10; // range: -10 to 10
            npc.randomizeLocation(rx, rz);
            npcList.add(npc);
        }
    }

    public void start(GameAIServerUDP s) {
        server = s;
        npcLoop();
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

                    // 2. Execute Action based on proximity state
                    if (playerIsNear) {
                        npc.setSpinning(true);
                    } else {
                        npc.setSpinning(false);
                        npc.setSpeed(0.1); // Normal patrol speed
                    }

                    // 3. Move the physical dolphin
                    npc.updateLocation();
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