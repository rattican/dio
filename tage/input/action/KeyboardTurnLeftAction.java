package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction;
import a3.MyGame;
import net.java.games.input.Event;

/**
 * KeyboardLeftTurnAction is a custom input action that is specific to keyboard input. This will rotate the avatar left when key is pressed.
 * 
 * <p>
 * This action applies a fixed global space yaw rotation each frame
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class KeyboardTurnLeftAction extends AbstractInputAction {
    private MyGame game;

    public KeyboardTurnLeftAction(MyGame g) { game = g; }

    /**
     * Rotates avatar left through global yaw and is negative for left rotation.
     * 
     * @param time  elapsed time since last frame
     * @param e key pressed event to trigger action
     */
    @Override
    public void performAction(float time, Event e) {
        game.getAvatar().globalYaw(-0.03f);
    }
}