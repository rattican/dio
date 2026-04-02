package tage.input.action;

import tage.*;
import net.java.games.input.Event;
import org.joml.Vector3f;

import a3.MyGame;

/**
 * OverheadPanAction works with the Viewport for the Overhead camera to move in one of four direction depending on the key being pressed. Movements include: up, down, left, right in accordance to an int direction.
 * <p>
 * The minimap gets panned through a switch case that labels each direction to a specific number between 0-3. It moves based on the number assigned to that provided movement.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class OverheadPanAction implements IAction
{   private Engine engine;
    private float panSpeed = 0.6f;
    private int direction;

    public OverheadPanAction(Engine e, int dir) {
        engine = e;
        direction = dir;
    }

    /**
     * Overhead camera gets panned based on configured direction. Can activate while repeatedly pressed down. Translates across either the x or z-axis since it's looking down from above.
     * 
     * @param time  elapsed time since last frame
     * @param evt key event being pressed to trigger
     */
    @Override
    public void performAction(float time, Event evt) {
        Camera cam = engine.getRenderSystem().getViewport("OVERHEAD").getCamera();
        Vector3f loc = cam.getLocation();

        switch (direction) {
            case 0: loc.z -= panSpeed; break;   // I = fwd
            case 1: loc.z += panSpeed; break;   // K = backward
            case 2: loc.x -= panSpeed; break;   // J = left
            case 3: loc.x += panSpeed; break;   // L = right
        }
        cam.setLocation(loc);
    }
}
