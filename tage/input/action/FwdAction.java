package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction; 
import net.java.games.input.Event; 
import org.joml.*;

import a3.MyGame;

/**
 * FwdAction is a custom input action added to the TAGE engine. This handles forward movement for the avatar on both keyboard and gamepad inputs.
 * <p>
 * Movement is scaled by elapsed frame time for smooth frame-rate motion.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class FwdAction extends AbstractInputAction
{   private MyGame game; 
    private GameObject av;
    private Vector3f oldPos, newPos;
    private Vector4f fwd;

    public FwdAction(MyGame g) { game = g;}

    /**
     * Moves avatar forward based on value from Event e. Values near zero are ignored to prevent collision to ground.
     * 
     * @param time  elapsed time since last frame used for time-based movement
     * @param e input event containing axis/key-value
     */
    @Override
    public void performAction(float time, Event e){
        float val = e.getValue();
        if (java.lang.Math.abs(val) < 0.2f) return;

        av = game.getAvatar();
        fwd = new Vector4f(0f,0f,-1f,0f);
        // rotate to world space and scale by time/input
        fwd.mul(av.getWorldRotation());
        fwd.mul(0.01f * time * -val);
        // move avatar
        oldPos = av.getWorldLocation();
        newPos = oldPos.add(fwd.x(), fwd.y(), fwd.z());

        // check ground collision for dolphin; reset back to 0f
        if (newPos.y < 1.0f)    newPos.y = 0f;

        av.setLocalLocation(newPos);
    }
}
