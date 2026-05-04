package a3;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position, float yaw, String modelName) throws IOException
	{	
		ObjShape s;
    	TextureImage t;
    	float scale;
		float yOffset = getYOffsetForModel(modelName);
		System.out.println("adding ghost with ID --> " + id);
		if (modelName.equalsIgnoreCase("miku")) 
		{
        	s = game.getGhostShape(); // Miku
        	t = game.getGhostTexture();
        	scale = 0.55f;
    	} else {
        	s = game.getDioShape(); // DIO
        	t = game.getDioTexture();
        	scale = 3.0f;
    	}
		// Apply Y offset based on model type
		Vector3f adjustedPosition = new Vector3f(position.x(), position.y() + yOffset, position.z());
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, adjustedPosition, modelName);
		newAvatar.getRenderStates().setRenderHiddenFaces(true);
		Matrix4f initialScale = (new Matrix4f()).scaling(scale);		
		newAvatar.setLocalScale(initialScale);
		// Set the rotation based on yaw
		Matrix4f rotation = new Matrix4f().rotationY((float)java.lang.Math.toRadians(yaw));
		newAvatar.setLocalRotation(rotation);
		ghostAvatars.add(newAvatar);
	}

	private float getYOffsetForModel(String modelName)
	{
		return 0f;
	}
	
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}
	public void updateGhostAvatar(UUID id, Vector3f position, float yaw, float scale)
	{	
		GhostAvatar ghost = findAvatar(id);
    	if (ghost != null) {
        	// RKA: Translation/Location
        	ghost.setLocalLocation(position);
        
        	// RKM: Rotation (Matrix from Yaw)
			Matrix4f rot = new Matrix4f().rotationY((float)java.lang.Math.toRadians(yaw));
        
        	// RKS: Scaling
        	ghost.setLocalScale(new Matrix4f().scaling(scale));
    	}
	}	
}
