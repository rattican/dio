package a3;

import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	UUID uuid;
	String modelName;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p, String modelType) 
	{	super(GameObject.root(), s, t);
		uuid = id;
		modelName = modelType;
		setPosition(p);
	}
	
	public UUID getID() { return uuid; }
	public String getModelName() { return modelName; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public Vector3f getPosition() { return getWorldLocation(); }
}
