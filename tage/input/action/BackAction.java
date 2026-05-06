package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event; 
import org.joml.Vector3f;

import a3.MyGame;
import tage.physics.PhysicsObject;

/**
 * BackAction is a custom input action added to the TAGE engine. This handles backwards movement for the avatar on both keyboard and gamepad inputs.
 * <p>
 * Movement is scaled by elapsed frame time for smooth frame-rate motion.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class BackAction implements IAction {
    private MyGame game;

    public BackAction(MyGame g) { game = g; }

    /**
     * Moves avatar backward based on value from Event e. Values near zero are ignored to prevent ground collision.
     * 
     * @param time  elapsed time since last frame used for time-based movement
     * @param evt input event containing axis/key-value
     */
    @Override
    public void performAction(float time, Event evt) {
        GameObject av = game.getAvatar();
        PhysicsObject po = av.getPhysicsObject();
        if (po == null) return;
        Vector3f fwd = av.getWorldForwardVector();
        fwd.y = 0f;
        float speed = -0.01f * evt.getValue() * time;
        float vx = fwd.x * speed;
        float vz = fwd.z * speed;
        float[] vel = po.getLinearVelocity();
        po.setLinearVelocity(new float[]{vx, vel[1], vz});

        // float val = evt.getValue();
        // Vector3f fwd = av.getWorldForwardVector();
        // Vector3f newLoc = av.getWorldLocation().add(fwd.mul(-0.01f * val * time));
        // if (newLoc.y < 0f) newLoc.y = 0f;
        // av.setLocalLocation(newLoc);
        
        game.setIsMoving(true);
        // Send network update
        game.sendNetworkMovementUpdate();
    }
}
