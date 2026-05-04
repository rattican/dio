package tage.input.action;

import a3.MyGame;
import a3.ProtocolClient;

import tage.*;
import tage.input.action.AbstractInputAction; 
import net.java.games.input.Event; 
import org.joml.*;

import a3.MyGame;

/**
 * FwdAction is a custom input action added to the TAGE engine. This handles forward movement for the avatar on both keyboard and gamepad inputs.
 * <p>
 * Movement is scaled by elapsed frame time for smooth frame-rate motion.
 * 
 * Added by:
 * @author Emily Kuang， Hoi Yin Li
 * @version Spring 2026
 */

public class FwdAction extends AbstractInputAction
{   	
    private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector4f fwdDirection;
	private ProtocolClient protClient;

    public FwdAction(MyGame g, ProtocolClient p) { 
        game = g;
        protClient = p;
    }
    /**
     * Moves avatar forward based on value from Event e. Values near zero are ignored to prevent collision to ground.
     * 
     * @param time  elapsed time since last frame used for time-based movement
     * @param e input event containing axis/key-value
     */

	@Override
	public void performAction(float time, Event e)
	{	av = game.getAvatar();
    	oldPosition = av.getWorldLocation();
    	fwdDirection = new Vector4f(0f,0f,1f,1f);
    	fwdDirection.mul(av.getWorldRotation());   
    // Scale movement
   		fwdDirection.mul(0.1f * time); 
    	newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
    	av.setLocalLocation(newPosition);
    // Send network update
    	game.sendNetworkMovementUpdate();
		game.setIsMoving(true);
	}
}


