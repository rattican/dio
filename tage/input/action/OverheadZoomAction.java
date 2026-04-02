package tage.input.action;

import tage.*;
import net.java.games.input.Event;
import org.joml.Vector3f;

/**
 * OverheadZoomAction works with the Viewport for the Overhead camera to zoom in and out depending on the key being pressed.
 * <p>
 * Behavior is navigated through a boolean flag. The minimap also has a restriction to prevent from zooming past the ground of the map.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class OverheadZoomAction implements IAction{
    private Engine engine;
    private float zoomSpeed = 1.2f;
    private boolean zoomIn;

    public OverheadZoomAction(Engine e, boolean in) {engine = e; zoomIn = in;}

    /**
     * Overhead camera zooms in or out based on the assigned bool correlated with the action. Can be triggered continuously if button is held. There is prevention from zooming in past the ground.
     * 
     * @param time  elapsed time since last frame
     * @param evt   key event being pressed to trigger
     */
    @Override
    public void performAction(float time, Event evt){
        Camera cam = engine.getRenderSystem().getViewport("OVERHEAD").getCamera();
        Vector3f loc = cam.getLocation();

        if (zoomIn) loc.y -= zoomSpeed;
        else    loc.y += zoomSpeed;

        // prevent collision into ground
        if (loc.y < 5f) loc.y = 5f;

        cam.setLocation(loc);
    }
}
