package tage.input.action;

import tage.*;
import tage.input.action.AbstractInputAction;
import a3.MyGame;
import net.java.games.input.Event;

/**
 * KeyboardTurnRightAction is a custom input action specific to keyboard input. This will rotate the avatar right when key is pressed.
 * 
 * <p>
 * This action applies a fixed global space yaw rotation each frame.
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
*/

public class KeyboardTurnRightAction extends AbstractInputAction {
    private MyGame game;

    public KeyboardTurnRightAction(MyGame g) { game = g; }

    /**
     * Rotates avatar right through global yaw and is positive for right rotation.
     * 
     * @param time  elapsed time since last frame
     * @param e key pressed event to trigger action
     */
    @Override
    public void performAction(float time, Event e) {
        game.getAvatar().globalYaw(0.03f);
    }
}