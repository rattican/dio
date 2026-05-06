package a3;

import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class NPCwalkaround extends BTAction {
    private NPC npc;

    public NPCwalkaround(NPC n) {
        this.npc = n;
    }

    @Override
    protected BTStatus update(float elapsedMilliSecs) {
        // Resume normal swimming patrol
        npc.setSpinning(false); // FIX: Stop spinning
        npc.setSpeed(0.1);      // Normal speed
        return BTStatus.BH_SUCCESS;
    }
}