package a2;

import tage.*;
import tage.shapes.*;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import tage.input.*;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.action.*;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.nodeControllers.*;
import org.joml.Vector3f;

import a3.MyGame;

import org.joml.Matrix4f;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;

	// game state stuff
	private boolean paused = false;
	private boolean gameOver = false;
	private boolean gameWon = false;
	private boolean axesVisible = true;

	// track elapsed time for HUD
	private double lastFrameTime, currFrameTime, elapsTime;
	private static final float DOLPHIN_SPEED = 1f;	// for fwd/backward time-based movement

	// node controller
	private NodeController sc1, sc2, sc3, pulse;

	// picture variables
	private int picturesTaken = 0;
	private boolean[] pyramidPhotos = {false, false, false};
	private java.util.ArrayList<GameObject> photosArray = new java.util.ArrayList<GameObject>();
	private java.util.ArrayList<TextureImage> photosTexture = new java.util.ArrayList<TextureImage>();  // store photo textures

	// collision variables
	private static final float COLLISION_DIST = 4f;
	private static final float PHOTO_DIST = 10.5f;
	private static final float HOME_DIST = 9f;
	private Vector3f spawnpoint; // dolphin's home

	// HUD messages
	private String hudMsg = "GAME START! Take photos of the pyramids!";

	// input manager and game object related stuff
	private InputManager im;
	private GameObject dol, pyr1, pyr2, pyr3, home, x, y, z, photo, ground, sky, logo;
	private ObjShape dolS, pyrS, homeS, xS, yS, zS, photoS, groundS, skyS, logoS;
	private TextureImage doltx, pyrTx1, pyrTx2, pyrTx3, brick, groundTx, skyTx, logoTx;
	private Light light1, light2, light3, light4;

	private CameraOrbit3D orbit;

	public MyGame() { super(); }

	public static void main(String[] args)
	{	MyGame game = new MyGame();
		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	// getters
	public GameObject getAvatar() { return dol; }
	public Engine getEngine() {return engine;}

	// check collisions; call in update()
	private void checkCollisions() {
		if (gameOver || gameWon) {return;}

		// track dolphin location and store distances between dolphin and pyramids
		Vector3f dolphinLocation = dol.getWorldLocation();
		float dist1 = dolphinLocation.distance(pyr1.getWorldLocation());
		float dist2 = dolphinLocation.distance(pyr2.getWorldLocation());
		float dist3 = dolphinLocation.distance(pyr3.getWorldLocation());

		// compare distances for collision
		if (dist1 < COLLISION_DIST || dist2 < COLLISION_DIST || dist3 < COLLISION_DIST) {
			gameOver = true;
			hudMsg = "YOU LOSE! You crashed into a pyramid!";
			return;
		}

		// compare distances for photo taking and update HUD msg
		if (dist1 < PHOTO_DIST) hudMsg = "Press P to photograph pyramid 1";
		else if (dist2 < PHOTO_DIST) hudMsg = "Press P to photograph pyramid 2";
		else if (dist3 < PHOTO_DIST) hudMsg = "Press P to photograph pyramid 3";
		else hudMsg = "Visit pyramids to take photos";

		// check distance to home for win condition
		float homeDist = dolphinLocation.distance(spawnpoint);
		if (homeDist < HOME_DIST && picturesTaken == 3){hudMsg = "Press SPACE to get off the dolphin and WIN!";}
		else if (homeDist < HOME_DIST) {hudMsg = "Welcome home! Take photos of all the pyramids to win!";}
	}

	// resets dolphin back at the house if crashed
	private void resetDolphin() {
		dol.setLocalLocation(spawnpoint);
		dol.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(135.0f)));
		gameOver = false;
		hudMsg = "Dolphin reset! Try again!";
	}

	// create photo and attachments to dolphin
	private void attachPhoto(TextureImage photoTx, int pyramidIdx) {
		photo = new GameObject(dol, photoS, photoTx);

		float offset = 0.5f + (picturesTaken * 0.5f);
		photo.setLocalLocation(new Vector3f(-2.0f, offset, -0.5f));
		photo.setLocalScale(new Matrix4f().scaling(0.15f));
		photo.setLocalRotation(new Matrix4f().rotationX((float)java.lang.Math.toRadians(-90)));

		photosArray.add(photo);
		photosTexture.add(photoTx);
	}

	// display all photos onto dolphin home's wall
	private void displayPhotos(){
		// positions for wall
		float startX = -15f;
		float startY = 2f;
		float spacing = 3f;
		
		for (int i = 0; i < photosArray.size(); i++) {
			GameObject photo = photosArray.get(i);
			photo.setParent(GameObject.root());
			
			// position onto a wall
			float photoX = startX + (i * spacing);
			photo.setLocalLocation(new Vector3f(photoX, startY + 2, 5));
			photo.setLocalScale(new Matrix4f().scaling(1.2f));
		}
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("dolphinHighPoly.obj");
		pyrS = new Pyramid();
		homeS = new DolphinHouse();
		xS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		yS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		zS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		photoS = new Plane();
		groundS = new Plane();
		skyS = new Sphere();
		logoS = new Plane();
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.jpg");
		pyrTx1 = new TextureImage("sand_brick.jpg");
		pyrTx2 = new TextureImage("blue_brick.jpg");
		pyrTx3 = new TextureImage("rocky_brick.jpg");
		brick = new TextureImage("marble_brick.jpg");
		groundTx = new TextureImage("rocky_ground.jpg");
		skyTx = new TextureImage("day_sky.jpg");
		logoTx = new TextureImage("dolphin_logo.png");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale, initialRotation;
		initialTranslation = (new Matrix4f()).translation(2,1,0);
		initialScale = (new Matrix4f()).scaling(3.0f);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));

		logo = new GameObject(GameObject.root(), logoS, logoTx);
		logo.setLocalLocation(new Vector3f(-12f,3f,-12f));
		logo.setLocalScale(new Matrix4f().scaling(2f,2f,2f));
		logo.setLocalRotation(new Matrix4f().rotationX((float)java.lang.Math.toRadians(90)));

		sky = new GameObject(GameObject.root(), skyS, skyTx);
		sky.setLocalLocation(new Vector3f(0f,0f,0f));
		sky.setLocalScale(new Matrix4f().scaling(-250f,250f,250f));	// flips inside out
		sky.getRenderStates().hasLighting(false);

		ground = new GameObject(GameObject.root(), groundS, groundTx);
		ground.setLocalScale(new Matrix4f().scaling(200f, 1f, 200f));
		// ground.setLocalScale(new Matrix4f().scaling(200f));
		ground.setLocalLocation(new Vector3f(0f, 0f, 0f));

		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);
		dol.setLocalRotation(initialRotation);

		// build home floating above spawnpoint
		home = new GameObject(GameObject.root(), homeS, brick);
		initialTranslation = (new Matrix4f()).translation(-15f,2f,0f);
		home.setLocalTranslation(initialTranslation);
		home.setLocalScale(initialScale);
		spawnpoint = new Vector3f(-15f,2f,0f); // stores home location for respawning; after crashing into the pyramids

		// build pyramid at far right of the window
		pyr1 = new GameObject(GameObject.root(), pyrS, pyrTx1);	// ice texture
		initialTranslation = (new Matrix4f()).translation(47,2,0);
		initialScale = (new Matrix4f()).scaling(3.2f);
		pyr1.setLocalTranslation(initialTranslation);
		pyr1.setLocalScale(initialScale);

		// build pyramid at far left of the window
		pyr2 = new GameObject(GameObject.root(), pyrS, pyrTx2);	// moon texture
		initialTranslation = (new Matrix4f()).translation(-32,2,0);
		initialScale = (new Matrix4f()).scaling(2f);
		pyr2.setLocalTranslation(initialTranslation);
		pyr2.setLocalScale(initialScale);

		// build pyramid at back of the window
		pyr3 = new GameObject(GameObject.root(), pyrS, pyrTx3);	// rocky texture
		initialTranslation = (new Matrix4f()).translation(0,2,-35);
		initialScale = (new Matrix4f()).scaling(1.5f);
		pyr3.setLocalTranslation(initialTranslation);
		pyr3.setLocalScale(initialScale);

		// add rotation to pyramids
		sc1 = new StretchController(engine, 2f);
		sc1.addTarget(pyr1);
		engine.getSceneGraph().addNodeController(sc1);
		
		sc2 = new StretchController(engine, 1f);
		sc2.addTarget(pyr2);
		engine.getSceneGraph().addNodeController(sc2);
		
		sc3 = new StretchController(engine, 0.5f);
		sc3.addTarget(pyr3);
		engine.getSceneGraph().addNodeController(sc3);

		// add pulse to house
		pulse = new PulseController();
		pulse.addTarget(home);
		engine.getSceneGraph().addNodeController(pulse);

		// build world axes (xyz in RGB)
		x = new GameObject(GameObject.root(), xS);
		y = new GameObject(GameObject.root(), yS);
		z = new GameObject(GameObject.root(), zS);
		x.setLocalLocation(new Vector3f(0f, 1f, 0f));
		y.setLocalLocation(new Vector3f(0f, 1f, 0f));
		z.setLocalLocation(new Vector3f(0f, 1f, 0f));
		x.setLocalScale(new Matrix4f().scaling(10f));
		y.setLocalScale(new Matrix4f().scaling(10f));
		z.setLocalScale(new Matrix4f().scaling(10f));
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		// pyramid 1 light
		light1 = new Light();
		light1.setLocation(new Vector3f(47,10,0));
		light1.setDiffuse(1f, 0.5f, 0.5f);
		(engine.getSceneGraph()).addLight(light1);
		// pyramid 2
		light2 = new Light();
		light2.setLocation(new Vector3f(-32,10, 0));
		light2.setDiffuse(0.5f, 1f, 0.5f);
		(engine.getSceneGraph()).addLight(light2);
		// pyramid 3
		light3 = new Light();
		light3.setLocation(new Vector3f(0,10,35));
		light3.setDiffuse(0.5f, 0.5f, 1f);
		(engine.getSceneGraph()).addLight(light3);
		// light for inside home
		light4 = new Light();
		light4.setLocation(new Vector3f(-15,12,0));
		light4.setDiffuse(1f, 0.5f, 1f);
		(engine.getSceneGraph()).addLight(light4);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		RenderSystem rs = engine.getRenderSystem();
		// create second camera
		Camera cam2 = new Camera();
		cam2.setLocation(new Vector3f(0, 50, 0));
		cam2.lookAt(new Vector3f(0,0,0));
		// create second viewport for overhead in top right corner
		Viewport vp2 = rs.addViewport("OVERHEAD", 0.7f, 0.7f, 0.28f, 0.28f);
		vp2.setCamera(cam2);

		// create cam orbit 3d
		orbit = new CameraOrbit3D(engine, dol);
		orbit.addTarget(dol);
		engine.getSceneGraph().addNodeController(orbit);
		orbit.enable();

		// set main cam position
		Camera mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		mainCam.setLocation(new Vector3f(0,5,15));
		mainCam.lookAt(dol.getWorldLocation());

		// input manager and mappings
		im = engine.getInputManager();

		// movement actions
		FwdAction fwdAction = new FwdAction(this);
		BackAction backAction = new BackAction(this);
		TurnAction turnAction = new TurnAction(this);
		KeyboardTurnLeftAction kbLeft = new KeyboardTurnLeftAction(this);
		KeyboardTurnRightAction kbRight = new KeyboardTurnRightAction(this);
		// pan and zoom
		OverheadPanAction panUp = new OverheadPanAction(engine, 0);
		OverheadPanAction panDown = new OverheadPanAction(engine, 1);
		OverheadPanAction panLeft = new OverheadPanAction(engine, 2);
		OverheadPanAction panRight = new OverheadPanAction(engine, 3);
		OverheadZoomAction zoomIn = new OverheadZoomAction(engine, true);
		OverheadZoomAction zoomOut = new OverheadZoomAction(engine, false);

		// controller mapping for moving forward
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.Y, fwdAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// controller mapping for turning left and right
		im.associateActionWithAllGamepads( 
   			net.java.games.input.Component.Identifier.Axis.X, turnAction, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		// keyboard mapping for movements
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.W, fwdAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.A, kbLeft, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.D, kbRight,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.S, backAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
			// bind input actions for cam orbit 3d
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.LEFT, (evt, val) -> orbit.orbitLeft(-1f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RIGHT, (evt, val) -> orbit.orbitRight(-1f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.UP, (evt, val) -> orbit.orbitUp(1f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.DOWN, (evt, val) -> orbit.orbitDown(1f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.Z, (evt, val) -> orbit.zoomIn(0.2f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.X, (evt, val) -> orbit.zoomOut(0.2f), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
			// overhead zoom and pan input bindings
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.I, panUp,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.K, panDown, 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.J, panLeft,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.L, panRight,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.N, zoomIn,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.M, zoomOut,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	@Override
	public void update()
	{	// updates elapsed time
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) {elapsTime += (currFrameTime - lastFrameTime) / 1000.0;}

		// check collisions and updates hudMsgs as necessary
		checkCollisions();

		// build and set HUD
		int elapsTimeSec = Math.round((float)elapsTime);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String scoreStr = Integer.toString(picturesTaken);
		// strings
		String dispStr1 = "Time = " + elapsTimeStr + " : Photos Taken = " + scoreStr;
		String dispStr2 = hudMsg;
		// colors and positions
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(0,1,0);
		(engine.getHUDmanager()).setHUD1(dispStr1 + " || " + dispStr2, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2("Avatar: " + dol.getWorldLocation().toString(), hud2Color, 1350, 720);

		// update inputs and camera according to game conditions
		if (!gameOver || gameWon) im.update((float)elapsTime);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	Vector3f loc, fwd, newLocation, camU, camV, camN;
		Matrix4f rot;
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		switch (e.getKeyCode())
		{	case KeyEvent.VK_1: // pause/unpause game
				paused = !paused;
				break;
			case KeyEvent.VK_C: // show/hide axes
				axesVisible = !axesVisible;
				if (axesVisible) {
        			x.setLocalScale(new Matrix4f().scaling(10f));
					y.setLocalScale(new Matrix4f().scaling(10f));
					z.setLocalScale(new Matrix4f().scaling(10f));
				} else {
					x.setLocalScale(new Matrix4f().scaling(0f));
					y.setLocalScale(new Matrix4f().scaling(0f));
					z.setLocalScale(new Matrix4f().scaling(0f));
				}
				break;
			case KeyEvent.VK_SPACE:	// win/lose condition
				if (gameOver) { resetDolphin(); break; }

				float homeDist = dol.getWorldLocation().distance(spawnpoint);
				// win condition
				if (!gameWon && homeDist < HOME_DIST && picturesTaken == 3){
					gameWon = true;
					pulse.enable();
					displayPhotos();
					hudMsg = "YOU WIN! You returned home with all 3 photos!";
				}
				break;
			case KeyEvent.VK_R: // reset game
				resetDolphin();
				break;
			case KeyEvent.VK_P:	// take picture (rectangle texture) if close enough
				if (!gameOver && !gameWon) {
					Vector3f dolLoc = dol.getWorldLocation();
					float dist1 = dolLoc.distance(pyr1.getWorldLocation());
					float dist2 = dolLoc.distance(pyr2.getWorldLocation());
					float dist3 = dolLoc.distance(pyr3.getWorldLocation());

					if (dist1 < PHOTO_DIST && !pyramidPhotos[0]) {
						pyramidPhotos[0] = true;
						picturesTaken++;
						sc1.enable();
						attachPhoto(pyrTx1, 0);
						hudMsg = "Photo taken of Pyramid 1 (" + picturesTaken + "/3)";
					} else if (dist2 < PHOTO_DIST && !pyramidPhotos[1]) {
						pyramidPhotos[1] = true;
						picturesTaken++;
						sc2.enable();
						attachPhoto(pyrTx2, 1);
						hudMsg = "Photo of pyramid 2 taken! (" + picturesTaken + "/3)";
					} else if (dist3 < PHOTO_DIST && !pyramidPhotos[2]) {
						pyramidPhotos[2] = true;
						picturesTaken++;
						sc3.enable();
						attachPhoto(pyrTx3, 2);
						hudMsg = "Photo of pyramid 3 taken! (" + picturesTaken + "/3)";
					} else if (pyramidPhotos[0] && dist1 < PHOTO_DIST) {
						hudMsg = "Already photographed pyramid 1!";
					} else if (pyramidPhotos[1] && dist2 < PHOTO_DIST) {
						hudMsg = "Already photographed pyramid 2!";
					} else if (pyramidPhotos[2] && dist3 < PHOTO_DIST) {
						hudMsg = "Already photographed pyramid 3!";
					} else {
						hudMsg = "Not close enough to photograph!";
					}
				}
				break;
		}
		super.keyPressed(e);
	}
}