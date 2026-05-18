package a3;

import tage.*;
import org.joml.Vector3f;
import org.joml.Matrix4f;

public class GhostNPC extends GameObject
{
    private int uniqueID; // Unique ID assigned by the server
    private int id;
    private TextureImage myTexture;

    public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.uniqueID = id; // Initialize unique ID with the same value as id
        this.setLocalLocation(p);
        this.myTexture = t;
    }

    // Helper to change position
    public void setPosition(Vector3f p) {
        this.setLocalLocation(p);
    }
    public int getUniqueID() {
        return uniqueID;
    }
    //helper for game logic:
    public TextureImage getTexture() { return myTexture; }
    // Handled on client if the server still sends sizing packets, 
    // but since we aren't changing size, we can leave basic scale setup
    public void setSize(boolean big) {
        if (!big) {
            this.setLocalScale((new Matrix4f()).scaling(1.0f)); // default scale
        } else {
            this.setLocalScale((new Matrix4f()).scaling(1.0f)); 
        }
    }
}