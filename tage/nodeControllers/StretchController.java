package tage.nodeControllers;

import tage.*;
import org.joml.*;

/**
 * StretchController is a NodeController that applies scaling animation to a GameObject based on elapsed time. The object gets stretched then contracts overtime.
 * <p>
 * The animation is currently applied to each pyramid and takes effect when that particular pyramid gets their photo taken of successfully.
 * 
 * Originally provided by Dr. Scott Gordon in 'code06b_CustomNodeControllers.pdf'
 * @author Emily Kuang
 * @version Spring 2026
 */

public class StretchController extends NodeController{
    private float scaleRate = .0003f;
    private float cycleTime = 2000.0f;
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    private Matrix4f curScale, newScale;
    private Engine engine;

    public StretchController(Engine e, float cTime){
        super();
        cycleTime = cTime;
        engine = e;
        newScale = new Matrix4f();
    }

    /**
     * Applies stretch animation based on time to the given GameObject.
     * Scale goes back and forth between min-max values.
     * 
     * @param go    GameObject being stretched
     */
    public void apply(GameObject go){
        // store times
        float elapsedTime = super.getElapsedTime();
        totalTime += elapsedTime/1000.0f;

        // compare times
        if (totalTime > cycleTime){
            direction = -direction;
            totalTime = 0.0f;
        }

        // scale with time
        curScale = go.getLocalScale();
        float scaleAmt = 1.0f + direction * scaleRate * elapsedTime;
        newScale.scaling(curScale.m00() * scaleAmt, curScale.m11(), curScale.m22());
        go.setLocalScale(newScale);
    }
}
