package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import a3.MyGame;
import net.java.games.input.Event;

/**
 * Was used only in A1 assignment.
 * <p>
 * YawAction is a custom input action added to the TAGE engine and applies local-space yaw rotation based on keyboard input.
 * <p>
 * This handles and gets applied to camera rotations.
 * 
 * Originally provided by Scott Gordon in 'code03a_ManualObj+CameraMovement.pdf'
 * @author Emily Kuang
 * @version Spring 2026
 */

public class YawAction extends AbstractInputAction{
    private MyGame game;
    private Camera cam;
    private Vector3f right, up, fwd;
    public YawAction(MyGame g) { game = g;}

    /**
     * Rotates specified Camera around local y-axis and is triggered by keyboard input.
     * 
     * @param time  elapsed time since last update
     * @param e input event representing key pressed
     */
    @Override
    public void performAction(float time, Event e){
        float keyValue = e.getValue();
        if (-0.2 < keyValue && keyValue < 0.2) return;

        // retrieve variables
        cam = (game.getEngine().getRenderSystem()).getViewport("MAIN").getCamera();
        right = cam.getU();
        up = cam.getV();
        fwd = cam.getN();
        // rotate cam coords
        right.rotateAxis(0.001f, up.x(), up.y(), up.z());
        fwd.rotateAxis(0.001f, up.x(), up.y(), up.z());
        // set cam coords
        cam.setU(right);
        cam.setN(fwd);
    }
}
