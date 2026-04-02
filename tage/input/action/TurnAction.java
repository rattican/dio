package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction;
import a3.MyGame;
import net.java.games.input.Event;

/**
 * TurnAction is a custom input action added to the TAGE engine. This handles left/right turning for the avatar for both keyboard and gamepad inputs.
 * <p>
 * This reads axis values from the input device and applies a global yaw rotation to the avatar. A small deadzone was added to prevent controller drift.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class TurnAction extends AbstractInputAction
{   private MyGame game;
    public TurnAction(MyGame g) { game = g;}

    /**
     * Applies world-space yaw rotation to avatar based on input's axis value. Small values between -0.2 and 0.2  gets ignored to fix controller drift.
     * 
     * @param time  elapsed time since last frame
     * @param e input event containing axis value for turning
     */
    @Override 
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();

        if (-0.2 < keyValue && keyValue < 0.2) return; // helped fix drifting

        float turnSpeed = 0.015f;
        // rotate globally
        GameObject av = game.getAvatar();
        av.globalYaw(-turnSpeed * keyValue * time);
    }
}