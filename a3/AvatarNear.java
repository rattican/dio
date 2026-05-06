package a3;

import tage.ai.behaviortrees.BTCondition; 
//base on code14a

public class AvatarNear extends BTCondition
{ 
    private NPC npc; 
    private NPCController npcc; // Matched NPCController naming convention
    private GameAIServerUDP server; 

    public AvatarNear(GameAIServerUDP s, NPCController c, NPC n, boolean toNegate)
    { 
        super(toNegate); 
        this.server = s; 
        this.npcc = c; 
        this.npc = n; 
    }

    @Override
    protected boolean check()
    { 
        server.sendCheckForAvatarNear(); // Asks clients to evaluate proximity
        return npcc.getNearFlag(); // Returns whether any client responded with "isnear"
    } 
}