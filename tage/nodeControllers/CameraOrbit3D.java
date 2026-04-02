package tage.nodeControllers;

import tage.*;
import org.joml.Vector3f;

/**
 * CameraOrbit3D implements third perspective orbit camera to the TAGE engine. This allows orbiting around targets like GameObject. Can adjust elevation, zoom in/out, and offset management while avatar moves.
 * <p>
 * Camera position updated each frame based on yaw, pitch, and radius.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
*/

public class CameraOrbit3D extends NodeController{
    private Engine engine;
    private GameObject avatar;
    private Camera cam;

    private float azimuth = 0.0f;
    private float elevation = 20.0f;
    private float radius = 8.0f;

    public CameraOrbit3D(Engine e, GameObject av){
        super();
        engine = e;
        avatar = av;
    }

    /**
     * Updates camera's world spcae position based on current yaw, pitch, and radius.
     * Camera positioned relative to GameObject and oriented to always look at it.
     * 
     * @param go    camera GameObject being controlled
     */
    @Override
    public void apply(GameObject go){
        cam = engine.getRenderSystem().getViewport("MAIN").getCamera();

        // radius
        double radA = Math.toRadians(azimuth);
        double radE = Math.toRadians(elevation);

        // create coords
        float x = (float)(radius * Math.cos(radE) * Math.sin(radA));
        float y = (float)(radius * Math.sin(radE));
        float z = (float)(radius * Math.cos(radE) * Math.cos(radA));

        // locations
        Vector3f avatarLoc = avatar.getWorldLocation();
        Vector3f camLoc = new Vector3f(avatarLoc.x + x, avatarLoc.y + y, avatarLoc.z + z);
        cam.setLocation(camLoc);

        // set coords
        Vector3f n = new Vector3f(avatarLoc).sub(camLoc).normalize();
        Vector3f u = new Vector3f(0,1,0).cross(n).normalize();
        Vector3f v = new Vector3f(n).cross(u).normalize();

        // set cam
        cam.setN(n);
        cam.setU(u);
        cam.setV(v);
    }

    // setters
    public void orbitLeft(float f) {azimuth += f;}
    public void orbitRight(float f) {azimuth -= f;}
    public void orbitUp(float amt) {elevation += amt; }
    public void orbitDown(float amt) {elevation -= amt; }
    public void zoomIn(float amt) {radius -= amt; if (radius < 2) radius = 2; }
    public void zoomOut(float amt) {radius += amt; }
}