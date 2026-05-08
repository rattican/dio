package tage.input.action;

import a3.MyGame;
import a3.ProtocolClient;

import tage.*;
import tage.input.action.AbstractInputAction; 
import net.java.games.input.Event; 
import org.joml.Vector3f;
import tage.physics.PhysicsObject;;

public class HitAction extends AbstractInputAction {
    private MyGame game;
    public HitAction(MyGame g) { game = g; }

    @Override
    public void performAction(float time, net.java.games.input.Event evt) {
        System.out.println("HIT ACTION TRIGGERED");
        game.setIsHitting(true);
    }
}