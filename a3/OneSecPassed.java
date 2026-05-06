package a3;

import tage.ai.behaviortrees.BTCondition;

public class OneSecPassed extends BTCondition {
    private NPCController npcc;
    private NPC npc;
    private long lastUpdateTime;

    public OneSecPassed(NPCController c, NPC n, boolean toNegate) {
        super(toNegate);
        this.npcc = c;
        this.npc = n;
        this.lastUpdateTime = System.nanoTime();
    }

    @Override
    protected boolean check() {
        long currentTime = System.nanoTime();
        float elapsedMilliSecs = (currentTime - lastUpdateTime) / 1000000.0f;
        if (elapsedMilliSecs >= 1000.0f) {
            lastUpdateTime = currentTime;
            return true;
        }
        return false;
    }
}