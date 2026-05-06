package a3;

import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class NPCSpinGreet extends BTAction {
    private NPC npc;

    public NPCSpinGreet(NPC n) {
        this.npc = n;
    }

    @Override
    protected BTStatus update(float elapsedMilliSecs) {
        // Stop forward motion and start spinning!
        npc.setSpinning(true);
        return BTStatus.BH_SUCCESS;
    }
}