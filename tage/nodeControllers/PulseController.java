package tage.nodeControllers;

import tage.*;
import org.joml.*;

/**
 * PulseController is a node controller used to apply a pulsing-like animation to a GameObject by scaling it up and down in accordance to elapsed time. It utilizes the sin wave function to emulate the continuous rise and fall of the graphed equation.
 * <p>
 * The controller is currently applied to the avatar's home when the win condition is fulfilled.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class PulseController extends NodeController {
    private float maxScale = 3.4f;
    private float minScale = 3f;
    private float speed = 0.01f;
    private float t = 0f;

    public PulseController() {
        super();
    }

    /**
     * Applies scale transformation to the given GameObject. Scale changes between minimum and max value using sin funct based on elapsed time.
     * 
     * @param go    GameObject to be pulsed
     */
    @Override
    public void apply(GameObject go) {
        t += speed * getElapsedTime();

        float s = minScale + (float)(org.joml.Math.sin(t) * 0.5 + 0.5) * (maxScale - minScale);

        go.setLocalScale(new Matrix4f().scaling(s));
    }
}
