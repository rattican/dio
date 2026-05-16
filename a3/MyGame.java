package a3;

import tage.*;
import tage.Light.LightType;
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
import org.joml.Matrix4f;

import tage.networking.IGameConnection.ProtocolType;
import java.net.InetAddress;
import java.net.UnknownHostException;

import tage.shapes.AnimatedShape.EndType;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.audio.*;

import org.joml.Quaternionf;
import org.joml.Matrix4f;
//import org.joml.Vector3f as JomlVector3f;

import a3.MyGame;

public class MyGame extends VariableFrameRateGame
{
	//networking
	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	private boolean isConnected = false;
	private static Engine engine;

	// audio and background/sound effects
	private IAudioManager audioMgr;
	private Sound bgMusic, attackSfx, dolphinSfx;

	// game state
	private boolean paused = false;
	private boolean gameOver = false;
	private boolean gameWon = false;
	private boolean axesVisible = true;

	// player state
	private boolean running = false;
	private float vals[] = new float[16];

	// track elapsed time for HUD
	private double lastFrameTime, currFrameTime, elapsTime;
	private static final float MOVE_SPEED = 1f;	// for fwd/backward time-based movement

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
	private Vector3f spawnpoint;

	// HUD messages
	private String hudMsg = "GAME START! DONT TOUCH The Pyramids AND RED DIO!";

	// input manager and game object related stuff
	private InputManager im;
	private GameObject dio, pyr1, pyr2, pyr3, home, x, y, z, photo, ground, sky, logo, terrain, castle;
	private ObjShape dioS, npcS, pyrS, homeS, xS, yS, zS, photoS, groundS, skyS, logoS, ghostS, terrainS, castleS;
	private TextureImage dioTx, pyrTx1, pyrTx2, pyrTx3, brick, groundTx, skyTx, logoTx, ghostT, grassTx, hillsTx, npcTx, enemyTx, castleTx;
	private Light light1, light2, light3, light4, dioLight;
	private boolean lightOn = true;

	// quaternion and matrix for physics
	private boolean rotate = false, lerp = true;
	private float interpolation = 0.0f, speed = 0.01f;
	private Matrix4f matStart = new Matrix4f().identity();
	private Matrix4f matInterp = new Matrix4f().identity();
	private Matrix4f matEnd = new Matrix4f().identity();
	private Quaternionf quatStart = new Quaternionf();
	private Quaternionf quatInterp = new Quaternionf();
	private Quaternionf quatEnd = new Quaternionf();

	// physics related
	private PhysicsEngine physicsEngine;
	private PhysicsObject physicsObj1, physicsObj2, physicsPlane;

	// camera orbit
	private CameraOrbit3D orbit;
	//dolphin NPC:
	private GhostNPC ghostNPS;

	//for networking:
	private String myType; // to make sure which character is it

	// for animation:
	private float hitTimer = 0.0f;
	private boolean isMoving;
	private boolean isHitting;

	//game logic: 
	private int greendioRemaining = 5; //match the NPC controller

	public MyGame(String serverAddress, int serverPort, String protocol, String role) { 
		super(); 
		this.myType = role;
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
		}

	public static void main(String[] args)
{	
		System.setProperty("jogl.disable.opengl.core", "true");
		if (args.length < 4) {
			System.out.println("Usage: java a3.MyGame <IP> <Port> <Protocol> <Role>");
		}
		else {
			MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2], args[3]);
			engine = new Engine(game);
			engine.initializeSystem();
			game.buildGame();
			game.startGame();
		}
	}

	// getters
	public GameObject getAvatar() { return dio; }
	public Vector3f getPlayerPosition() { return dio.getWorldLocation(); }
	public float getPlayerYaw() { 
		// Extract yaw from the rotation matrix
		Matrix4f rotMatrix = dio.getWorldRotation();
		// For a Y-axis rotation, the yaw can be extracted from the rotation matrix
		// Using atan2 of elements affected by Y rotation
		float m00 = rotMatrix.m00();
		float m02 = rotMatrix.m02();
		float yaw = (float)java.lang.Math.atan2(m02, m00);
		return (float)java.lang.Math.toDegrees(yaw);
	}
	public GameObject getTerrain() { return terrain; }
	//for NPC getter :
	public ObjShape getNPCshape() { return npcS; }
	public TextureImage getNPCtexture() { return npcTx; }
	public TextureImage getENEMYtexture() { return enemyTx; }
	//from code07a2
	//public GameObject getAvatar() { return avatar; }
	public ObjShape getGhostShape() { return ghostS; }
	//public ObjShape getGhostShape() { return dioS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public ObjShape getDioShape() { return dioS; }
	//game logic: if the dio/miku collide with the red dio, game over

	public void setGameOver(boolean v) { 
		this.gameOver = v; 
		if (v == true) {
			attackSfx.play();
			this.running = false; // Stop the physics engine
			this.paused = true;   // Pause movement
			hudMsg = "GAME OVER! A predator dolphin caught you!";
		}
	}
	//game logic: if the player hits all the green dio, win the game
	public void decrementGreenDolphinCount() {
		this.greendioRemaining--;
		if (greendioRemaining <= 0) {
			gameWon = true;
			hudMsg = "ALL GREEN DIOS SAVED! YOU WIN!";
		}
	}
	public TextureImage getDioTexture() { return dioTx; }
	public Engine getEngine() {return engine;}
	public void setIsConnected(boolean v) { isConnected = v; }
	public String getAvatarType() { return myType; }// Returns "dio" or "miku" based on bat}
	private void setupNetworking() {
    	isClientConnected = false;
    	gm = new GhostManager(this); // Initialize manager for other players 
    	try {
        	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
    		} catch (UnknownHostException e) { e.printStackTrace();
    	} catch (IOException e) { 
			e.printStackTrace(); 
			}
    	if (protClient == null) {
        	System.out.println("missing protocol host");
    	} else {
        	protClient.sendJoinMessage(); // Start  handshake 
    	}
	}

	// check collisions; call in update()
	private void checkCollisions() {
		if (gameOver || gameWon) {return;}

		// track Dio's location and store distances between Dio and pyramids
		Vector3f dioLocation = dio.getWorldLocation();
		float dist1 = dioLocation.distance(pyr1.getWorldLocation());
		float dist2 = dioLocation.distance(pyr2.getWorldLocation());
		float dist3 = dioLocation.distance(pyr3.getWorldLocation());

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
		else hudMsg = "DONT TOUCH Pyramids and RED DIO!";

		// check distance to home for win condition
		float homeDist = dioLocation.distance(spawnpoint);
		if (homeDist < HOME_DIST && picturesTaken == 3){hudMsg = "Press SPACE to get off DIO and WIN!";}
		else if (homeDist < HOME_DIST) {hudMsg = "Welcome home! Take photos of all the pyramids to win!";}
	}

	// resets Dio back at the house if crashed
	private void resetDio() {
		dio.setLocalLocation(spawnpoint);
		dio.setLocalRotation(new Matrix4f().rotationY((float)java.lang.Math.toRadians(135.0f)));
		gameOver = false;
		hudMsg = "DIO reset! Try again!";
	}

	// create photo and attachments to DIO
	private void attachPhoto(TextureImage photoTx, int pyramidIdx) {
		photo = new GameObject(dio, photoS, photoTx);

		float offset = 0.5f + (picturesTaken * 0.5f);
		photo.setLocalLocation(new Vector3f(-2.0f, offset, -0.5f));
		photo.setLocalScale(new Matrix4f().scaling(0.15f));
		photo.setLocalRotation(new Matrix4f().rotationX((float)java.lang.Math.toRadians(-90)));

		photosArray.add(photo);
		photosTexture.add(photoTx);
	}

	// display all photos onto DIO home's wall
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
	{	
		dioS = new ImportedModel("dio.obj");
		npcS = new ImportedModel("dio.obj");
		pyrS = new Pyramid();
		homeS = new DioHouse();
		xS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		yS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		zS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		photoS = new Plane();
		groundS = new Plane();
		skyS = new Sphere();
		logoS = new Plane();
		ghostS = new AnimatedShape("miku.rkm", "miku.rks");
		((AnimatedShape)ghostS).loadAnimation("WALK", "walk_miku.rka");
		((AnimatedShape)ghostS).loadAnimation("HIT", "hit_miku.rka");
		terrainS = new TerrainPlane(128);
		castleS = new ImportedModel("castle.obj");
	}

	@Override
	public void loadSounds() {
		AudioResource rsrc1, rsrc2, rsrc3;
		audioMgr = engine.getAudioManager();

		// background music
		rsrc1 = audioMgr.createAudioResource("bgMusic.wav", AudioResourceType.AUDIO_SAMPLE);
		bgMusic = new Sound(rsrc1, SoundType.SOUND_MUSIC, 1, true);
		bgMusic.initialize(audioMgr);

		// attack sound effect when player hits enemy
		rsrc2 = audioMgr.createAudioResource("attackSfx.wav", AudioResourceType.AUDIO_SAMPLE);
		attackSfx = new Sound(rsrc2, SoundType.SOUND_EFFECT, 15, false);
		attackSfx.initialize(audioMgr);

		// dolphin sfx (3D)
		rsrc3 = audioMgr.createAudioResource("dolphinSfx.wav", AudioResourceType.AUDIO_SAMPLE);
		dolphinSfx = new Sound(rsrc3, SoundType.SOUND_EFFECT, 100, true);
		dolphinSfx.initialize(audioMgr);
		dolphinSfx.setMaxDistance(500f);
		dolphinSfx.setMinDistance(0.5f);
		dolphinSfx.setRollOff(0.5f);
	}

	@Override
	public void loadTextures()
	{	dioTx = new TextureImage("dio.png");
		pyrTx1 = new TextureImage("sand_brick.jpg");
		pyrTx2 = new TextureImage("blue_brick.jpg");
		pyrTx3 = new TextureImage("rocky_brick.jpg");
		brick = new TextureImage("marble_brick.jpg");
		groundTx = new TextureImage("rocky_ground.jpg");
		skyTx = new TextureImage("day_sky.jpg");
		logoTx = new TextureImage("dolphin_logo.png");
		ghostT = new TextureImage("miku.png"); 
		grassTx = new TextureImage("grass.jpg");
		hillsTx = new TextureImage("hills.jpg");
		npcTx = new TextureImage("dio_green.png");
		enemyTx = new TextureImage("dio_red.png");
		castleTx = new TextureImage("brick1.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale, initialRotation;
		float avatarStartY = myType.equalsIgnoreCase("miku") ? 8.0f : 3.0f;
		initialTranslation = (new Matrix4f()).translation(2, avatarStartY, 0);
		//initialScale = (new Matrix4f()).scaling(3.0f);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));

		logo = new GameObject(GameObject.root(), logoS, logoTx);
		logo.setLocalLocation(new Vector3f(-12f,3f,-12f));
		logo.setLocalScale(new Matrix4f().scaling(2f,2f,2f));
		logo.setLocalRotation(new Matrix4f().rotationX((float)java.lang.Math.toRadians(90)));

		sky = new GameObject(GameObject.root(), skyS, skyTx);
		sky.setLocalLocation(new Vector3f(0f,0f,0f));
		sky.setLocalScale(new Matrix4f().scaling(200f));
		sky.getRenderStates().hasLighting(false);

		// world ground floor
		ground = new GameObject(GameObject.root(), groundS, groundTx);
		ground.setLocalScale(new Matrix4f().scaling(100f));
		ground.setLocalLocation(new Vector3f(0f, 0f, 0f));
		ground.getRenderStates().setTiling(1);
		ground.getRenderStates().setTileFactor(8);
		ground.getRenderStates().hasLighting(true);

		// build local avatar in the center of the window
		if (myType.equalsIgnoreCase("miku")) {
			dio = new GameObject(GameObject.root(), ghostS, ghostT);
			initialScale = (new Matrix4f()).scaling(0.5f);
			dio.setLocalTranslation(new Matrix4f().translation(0, -4.0f, 0));
		} else {
			dio = new GameObject(GameObject.root(), dioS, dioTx);
			initialScale = (new Matrix4f()).scaling(1.5f);
			dio.setLocalTranslation(new Matrix4f().translation(0, -0.9f, 0));
		}
		dio.setLocalTranslation(initialTranslation);
		dio.setLocalScale(initialScale);
		dio.setLocalRotation(initialRotation);
		dio.getRenderStates().hasLighting(true);

		terrain = new GameObject(GameObject.root(), terrainS, grassTx);
		terrain.setLocalLocation(new Vector3f(-82.5f,0f,82.5f));
		terrain.setLocalScale(new Matrix4f().scaling(15f,1.2f,15f));
		terrain.setHeightMap(hillsTx);
		// set tiling for terrain textures
		terrain.getRenderStates().setTiling(2);
		terrain.getRenderStates().setTileFactor(10);
		terrain.getRenderStates().hasLighting(true);

		castle = new GameObject(GameObject.root(), castleS, castleTx);
		castle.setLocalLocation(new Vector3f(0f,0f,0f));
		castle.setLocalScale(new Matrix4f().scaling(100f,30f,100f));
		castle.getRenderStates().setTiling(1);
		castle.getRenderStates().setTileFactor(10);

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
		pyr1.getRenderStates().hasLighting(true);

		// build pyramid at far left of the window
		pyr2 = new GameObject(GameObject.root(), pyrS, pyrTx2);	// moon texture
		initialTranslation = (new Matrix4f()).translation(-32,2,0);
		initialScale = (new Matrix4f()).scaling(2f);
		pyr2.setLocalTranslation(initialTranslation);
		pyr2.setLocalScale(initialScale);
		pyr2.getRenderStates().hasLighting(true);

		// build pyramid at back of the window
		pyr3 = new GameObject(GameObject.root(), pyrS, pyrTx3);	// rocky texture
		initialTranslation = (new Matrix4f()).translation(0,2,-35);
		initialScale = (new Matrix4f()).scaling(1.5f);
		pyr3.setLocalTranslation(initialTranslation);
		pyr3.setLocalScale(initialScale);
		pyr3.getRenderStates().hasLighting(true);

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
	{	Light.setGlobalAmbient(0.8f, 0.8f, 0.8f);
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
		// light for dio
		dioLight = new Light();
		dioLight.setLocation(dio.getLocalLocation());
		dioLight.setDiffuse(0.5f, 0.5f, 0.5f);
		// dioLight.setType(Light);
		(engine.getSceneGraph()).addLight(dioLight);
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
		orbit = new CameraOrbit3D(engine, dio);
		orbit.addTarget(dio);
		engine.getSceneGraph().addNodeController(orbit);
		orbit.enable();

		// set main cam position
		Camera mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		mainCam.setLocation(new Vector3f(0,5,15));
		mainCam.lookAt(dio.getWorldLocation());

		setupNetworking();

		// input manager and mappings
		im = engine.getInputManager();

		// movement actions
		FwdAction fwdAction = new FwdAction(this, protClient); // 		
		BackAction backAction = new BackAction(this);
		TurnAction turnAction = new TurnAction(this, protClient);
		KeyboardTurnLeftAction kbLeft = new KeyboardTurnLeftAction(this);
		KeyboardTurnRightAction kbRight = new KeyboardTurnRightAction(this);
		HitAction hitAction = new HitAction(this);
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
		// hit action for attack animation
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SPACE, hitAction,
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// init physics objects
		matStart.rotateY(1.57f);
		matStart.rotateZ(0.7f);
		matEnd.rotateY(-0.7f);
		matEnd.rotateX(-0.7f);
		matEnd.rotateZ(0.7f);

		// init sound settings
		bgMusic.setLocation(ground.getWorldLocation());
		attackSfx.setLocation(dio.getWorldLocation());
		dolphinSfx.setLocation(logo.getWorldLocation());

		//setEarParameters();
		bgMusic.play();
		dolphinSfx.play();
	}

	public void setEarParameters() {
		Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(dio.getWorldLocation());
		Vector3f fwd = new Vector3f(cam.getN()).mul(-1f);
		audioMgr.getEar().setOrientation(fwd, new Vector3f(0,1,0));
	}

	@Override
	public void initializePhysicsObjects(){
		float[] gravity = {0f,-20f,0f};
		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		// create physics world
		float mass = 1.0f;
		float up[] = {0f,1f,0f};
		float radius = myType.equalsIgnoreCase("miku") ? 0.3f : 0.75f;
		float height = myType.equalsIgnoreCase("miku") ? 1.0f : 2.0f;
		Vector3f loc;
		Quaternionf rot;

		// creates physics obj for DIO
		loc = dio.getWorldLocation();
		rot = new Quaternionf();
		dio.getWorldRotation().getNormalizedRotation(rot);
		physicsObj1 = (engine.getSceneGraph()).addPhysicsCapsule(mass, loc, rot, 0, radius, height);
		physicsObj1.setBounciness(0.8f);
		physicsObj1.setAngularFactor(0f); // locked to prevent falling over
		physicsObj1.setDamping(0.5f,0.5f); // prevents sliding
		physicsObj1.setFriction(0.5f); // also to prevent movement
		physicsObj1.disableSleeping();
		dio.setPhysicsObject(physicsObj1);

		loc = ground.getWorldLocation();
		rot = new Quaternionf();
		ground.getWorldRotation().getNormalizedRotation(rot);
		physicsPlane = (engine.getSceneGraph()).addPhysicsStaticPlane(loc, rot, up, 0f);
		physicsPlane.setBounciness(1f);
		ground.setPhysicsObject(physicsPlane);
	}

	protected void processNetworking(float elapsTime) {
    	if (protClient != null)
        	protClient.processPackets();
		}
	//for miku animation
	public void setIsMoving(boolean m) { isMoving = m; }
	public void setIsHitting(boolean h) { isHitting = h; }

	@Override
	public void update()
	{	// interpolate rotations for smooth movement
		if (rotate) {
			if (interpolation < 1.0f) {
				interpolation += speed;

				// create quats from matrices
				matStart.getNormalizedRotation(quatStart);
				matEnd.getNormalizedRotation(quatEnd);
				// interpolate between quats
				quatInterp = new Quaternionf(quatStart);
				if (lerp)	quatInterp.nlerp(quatEnd, interpolation);
				else	quatInterp.slerp(quatEnd, interpolation);
				// converts back to matrix and sets rotation
				quatInterp.get(matInterp);
				dio.setLocalRotation(matInterp);
			}
		}
		//dynamic HUD layout:
		RenderSystem rs = engine.getRenderSystem();
		int currentWidth = (int)rs.getViewport("MAIN").getActualWidth();
		int currentHeight = (int)rs.getViewport("MAIN").getActualHeight();
		int centerNPCX = (currentWidth / 2) - 100; 
		int topNPCY = currentHeight - 60; 
		// updates elapsed time
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) {elapsTime += (currFrameTime - lastFrameTime) / 1000.0;}

		// save frame time for physics
		float frame = (float)(currFrameTime - lastFrameTime) / 1000.0f;

		// also updates hudMsgs
		checkCollisions();

		//---------ANIMATION LOGIC ------
		if (dio.getShape() instanceof AnimatedShape) {
			AnimatedShape as = (AnimatedShape) dio.getShape();

			// 1. If J was just pressed, start the animation and the timer
			if (isHitting) {
				as.playAnimation("HIT", 1.0f, EndType.STOP, 0);
				
				// --- NEW CLOBBER LOGIC ---
				float hitRange = 4.0f; 
				int hitID = -1;

				// Get the list from your ProtocolClient
				for (GhostNPC g : protClient.getNPCList()) {
					float dist = getPlayerPosition().distance(g.getWorldLocation());
					
					// Check if the dolphin is close enough
					// Note: You can check if it's friendly by looking at the texture if needed
					if (dist < hitRange) {
						hitID = g.getUniqueID();
						break; 
					}
				}

				// If we found a dolphin near our swing, tell the server to kill it
				if (hitID != -1 && protClient != null) {
					System.out.println("HIT CONNECTED! Removing Dolphin: " + hitID);
					protClient.sendRemoveNPCMessage(hitID);
					//if no more green dio left, win the game
					if (greendioRemaining <= 0) {
						gameWon = true;
						hudMsg = "ALL GREEN DIOS KILLED! YOU WIN!";
					}
				}
				isHitting = false;
				hitTimer = 1.0f; // Lock animation for 1 second
			}
			// If the timer is still running, let the HIT play
			if (hitTimer > 0) {
				hitTimer -= frame; // Countdown
				// Do NOT play any other animations here
			} 
			//  If we aren't hitting, then we can walk
			else if (isMoving) {
				as.playAnimation("WALK", 0.5f, EndType.LOOP, 0);
			} 
			else {
				as.stopAnimation();
			}

			as.updateAnimation();
		}
		isMoving = false;

		// build and set HUD
		int elapsTimeSec = Math.round((float)elapsTime);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		//String scoreStr = Integer.toString(picturesTaken);
		String scoreStr = "Green Dios Left: " + greendioRemaining;

		// strings
		String dispStr1 = "Time = " + elapsTimeStr + " ; " + scoreStr;
		String dispStr2 = hudMsg;

		// colors and positions
		Vector3f hud1Color = new Vector3f(1.0f, 0.85f, 0.0f);
		Vector3f hud2Color = new Vector3f(0,1,0);
		(engine.getHUDmanager()).setHUD1(dispStr1 + " || " + dispStr2, hud1Color,centerNPCX, topNPCY);
		(engine.getHUDmanager()).setHUD2("Avatar: " + dio.getWorldLocation().toString(), hud2Color, 1350, 720);

		// update skybox with camera
		Vector3f camLoc = engine.getRenderSystem()
                        .getViewport("MAIN")
                        .getCamera()
                        .getLocation();
		sky.setLocalLocation(camLoc);

		// update altitude of DIO based on height map
		Vector3f loc = dio.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		float heightOffset = myType.equalsIgnoreCase("miku") ? 1.8f : 0.5f;		//dio.setLocalLocation(new Vector3f(loc.x(), height + heightOffset, loc.z()));
		if (dio.getPhysicsObject() != null) {
			if (loc.y() < (height + heightOffset)) {
				// FIX: Create a float array instead of passing the Vector3f directly
				float[] newLoc = { loc.x(), height + heightOffset, loc.z() };
				dio.getPhysicsObject().setLocation(newLoc);
				
				// Also kill Y-velocity to prevent "bouncing" through the floor
				float[] currentVel = dio.getPhysicsObject().getLinearVelocity();
				float[] stopYVel = { currentVel[0], 0f, currentVel[2] };
				dio.getPhysicsObject().setLinearVelocity(stopYVel);
			}
		}

		// update physics
		if (running) {
			physicsEngine.update(frame);
			for (GameObject go : engine.getSceneGraph().getGameObjects()){
				PhysicsObject po = go.getPhysicsObject();
				if (go.getPhysicsObject() != null) {

					// set translation
					Vector3f poLoc = po.getLocation();
					go.setLocalLocation(poLoc);

					// set rotation if not avatar
					if (go != dio) {
						Quaternionf rot = go.getPhysicsObject().getRotation();
						Matrix4f rotMat = new Matrix4f();
						rot.get(rotMat);
						go.setLocalRotation(rotMat);
					}
				}
			}
		}

		// update inputs and camera according to game conditions
		if (!gameOver || gameWon) {
			running = true;
			im.update(frame);}

		// update sound
		bgMusic.setLocation(ground.getWorldLocation());
		attackSfx.setLocation(dio.getWorldLocation());
		dolphinSfx.setLocation(logo.getWorldLocation());
		setEarParameters();

		// process networking packets
		processNetworking((float)elapsTime);
	}

	//helper for RKS:
	public float getPlayerScale() {
        return myType.equalsIgnoreCase("miku") ? 0.55f : 1.5f;
    }
	public void sendNetworkMovementUpdate() {   
        if (protClient != null) {   
            protClient.sendMoveMessage(dio.getWorldLocation());
        }
    }
	
	@Override
	public void keyPressed(KeyEvent e)
	{	Vector3f loc, fwd, newLocation, camU, camV, camN;
		Matrix4f rot;
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		switch (e.getKeyCode())
		{	case KeyEvent.VK_TAB: // pause/unpause game
				paused = !paused;
				break;
			case KeyEvent.VK_1:
				rotate = false;
				dio.setLocalRotation(matStart);
				break;
			case KeyEvent.VK_2:
				rotate = false;
				dio.setLocalRotation(matEnd);
				break;
			case KeyEvent.VK_3:
				dio.setLocalRotation(matStart);
				interpolation = 0.0f;
				rotate = true; lerp = true;
				break;
			case KeyEvent.VK_4:
				dio.setLocalRotation(matStart);
				interpolation = 0.0f;
				rotate = true; lerp = false;
				break;
			case KeyEvent.VK_Q:
				if (lightOn){
					light1.disable();
					light2.disable();
					light3.disable();
					light4.disable();
					dioLight.enable();
					lightOn = false;
				} else {
					light1.enable();
					light2.enable();
					light3.enable();
					light4.enable();
					dioLight.disable();
					lightOn = true;
				}
				break;
			case KeyEvent.VK_C: // toggle axes
				axesVisible = !axesVisible;
				if (axesVisible) {
        			x.setLocalScale(new Matrix4f().scaling(10f));
					y.setLocalScale(new Matrix4f().scaling(10f));
					z.setLocalScale(new Matrix4f().scaling(10f));
					// visualizes physics world
					//engine.enableGraphicsWorldRender();
					engine.enablePhysicsWorldRender();
				} else {
					x.setLocalScale(new Matrix4f().scaling(0f));
					y.setLocalScale(new Matrix4f().scaling(0f));
					z.setLocalScale(new Matrix4f().scaling(0f));
					// visualizes physics world
					//engine.disableGraphicsWorldRender();
					engine.disablePhysicsWorldRender();
				}
				break;
			case KeyEvent.VK_SPACE:	// win/lose condition
				if (gameOver) { resetDio(); break; }

				float homeDist = dio.getWorldLocation().distance(spawnpoint);
				// win condition
				if (!gameWon && homeDist < HOME_DIST && picturesTaken == 3){
					gameWon = true;
					pulse.enable();
					displayPhotos();
					hudMsg = "YOU WIN! You returned home with all 3 photos!";
				}
				break;
			case KeyEvent.VK_R: // reset game
				resetDio();
				break;
			case KeyEvent.VK_P:	// take picture (rectangle texture) if close enough
				if (!gameOver && !gameWon) {
					Vector3f dioLoc = dio.getWorldLocation();
					float dist1 = dioLoc.distance(pyr1.getWorldLocation());
					float dist2 = dioLoc.distance(pyr2.getWorldLocation());
					float dist3 = dioLoc.distance(pyr3.getWorldLocation());

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
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	
				protClient.sendByeMessage();
			}
		}
	}
	
	// Method to handle avatar rotation and send update to server
	public void rotateAvatarAndSendUpdate(float yawDelta)
	{	dio.globalYaw(yawDelta);
		if (protClient != null)
		{	protClient.sendMoveMessage(dio.getWorldLocation());
		}
	}
}
