package a3;

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

import tage.networking.IGameConnection.ProtocolType;
import java.net.InetAddress;
import java.net.UnknownHostException;

import a3.MyGame;
import org.joml.Matrix4f;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.rml.Quaternionf;

/*
	Milestone 1: Networking, SkyBox, Terrain, UV Unwrapped Models (2)
	
	Haley's TO DO:
	xxxx

	Emily's TO DO:
	Modify second camera so skybox is not visible in overhead view
	Add terrain features in distance
	Replace main dolphin model with the UV unwrapped cat model from Blender
	**Rewrite pyramid game to 2.5D neighborhood combat game
*/

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

	// game state stuff
	private boolean paused = false;
	private boolean gameOver = false;
	private boolean gameWon = false;
	private boolean axesVisible = true;

	// player state
	private boolean walking = false;
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
	private String hudMsg = "GAME START! Take photos of the pyramids!";

	// input manager and game object related stuff
	private InputManager im;
	private GameObject dio, pyr1, pyr2, pyr3, home, x, y, z, photo, ground, sky, logo, terrain;
	private ObjShape dioS, pyrS, homeS, xS, yS, zS, photoS, groundS, skyS, logoS, ghostS, terrainS;
	private TextureImage dioTx, pyrTx1, pyrTx2, pyrTx3, brick, groundTx, skyTx, logoTx, ghostT, grassTx, hillsTx;
	private Light light1, light2, light3, light4;

	// physics related
	private PhysicsEngine physicsEngine;
	private PhysicsObject physicsObj1, physicsObj2, physicsPlane;

	// camera orbit
	private CameraOrbit3D orbit;

	//for networking:
	private String myType; // to make sure which character is it

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

	//from code07a2
	//public GameObject getAvatar() { return avatar; }
	public ObjShape getGhostShape() { return ghostS; }
	//public ObjShape getGhostShape() { return dioS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public ObjShape getDioShape() { return dioS; }

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
		else hudMsg = "Visit pyramids to take photos";

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
		pyrS = new Pyramid();
		homeS = new DioHouse();
		xS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		yS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		zS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		photoS = new Plane();
		groundS = new Plane();
		skyS = new Sphere();
		logoS = new Plane();
		ghostS = new ImportedModel("miku.obj");
		terrainS = new TerrainPlane(128);
	}

	@Override
	public void loadTextures()
	{	dioTx = new TextureImage("dio_uv.png");
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
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale, initialRotation;
		float avatarStartY = myType.equalsIgnoreCase("miku") ? 8.0f : 3.0f;
		initialTranslation = (new Matrix4f()).translation(2, avatarStartY, 0);
		initialScale = (new Matrix4f()).scaling(3.0f);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(135.0f));

		logo = new GameObject(GameObject.root(), logoS, logoTx);
		logo.setLocalLocation(new Vector3f(-12f,3f,-12f));
		logo.setLocalScale(new Matrix4f().scaling(2f,2f,2f));
		logo.setLocalRotation(new Matrix4f().rotationX((float)java.lang.Math.toRadians(90)));

		sky = new GameObject(GameObject.root(), skyS, skyTx);
		sky.setLocalLocation(new Vector3f(0f,0f,0f));
		sky.setLocalScale(new Matrix4f().scaling(-200f));	// flips inside out
		sky.getRenderStates().hasLighting(false);

		ground = new GameObject(GameObject.root(), groundS, groundTx);
		ground.setLocalScale(new Matrix4f().scaling(10f));
		ground.setLocalLocation(new Vector3f(0f, 0f, 0f));
		ground.getRenderStates().setTiling(1);
		ground.getRenderStates().setTileFactor(4);
		ground.getRenderStates().hasLighting(true);

		// build local avatar in the center of the window
		if (myType.equalsIgnoreCase("miku")) {
			dio = new GameObject(GameObject.root(), ghostS, ghostT);
			initialScale = (new Matrix4f()).scaling(0.55f);
		} else {
			dio = new GameObject(GameObject.root(), dioS, dioTx);
			initialScale = (new Matrix4f()).scaling(1.5f);
		}
		dio.setLocalTranslation(initialTranslation);
		dio.setLocalScale(initialScale);
		dio.setLocalRotation(initialRotation);

		terrain = new GameObject(GameObject.root(), terrainS, grassTx);
		terrain.setLocalLocation(new Vector3f(0f,0f,0f));
		terrain.setLocalScale(new Matrix4f().scaling(20f,1f,20f));
		terrain.setHeightMap(hillsTx);
		// set tiling for terrain textures
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);

		// build DIO in the center of the window
		dio = new GameObject(GameObject.root(), dioS, dioTx);
		dio.setLocalTranslation(initialTranslation);
		dio.setLocalScale(initialScale);
		dio.setLocalRotation(initialRotation);

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
	public void initializePhysicsObjects(){
		float[] gravity = {0f,-5f,0f};
		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		// create physics world
		float mass = 1.0f;
		float up[] = {0f,1f,0f};
		float radius = 0.75f;
		float height = 2.0f;
		Vector3f loc;
		Quaternionf rot;

		// creates physics obj for DIO
		loc = dio.getWorldLocation();
		rot = new Quaternionf();
		(dio.getWorldLocation()).getNormalizedRotation(rot);
		physicsObj1 = (engine.getSceneGraph()).addPhysicsCapsule(mass, loc, rot, 0, radius, height);
		physicsObj1.setBounciness(0.8f);
		physicsObj1.disableSleeping();
		dio.setPhysicsObject(physicsObj1);

		/*
		// for MIKU
		loc = miku.getWorldLocation();
		rot = new Quaternionf();
		(miku.getWorldLocation()).getNormalizedRotation(rot);
		physicsObj2 = (engine.getSceneGraph()).addPhysicsCapsule(mass, loc, rot, 0, radius, height);
		physicsObj2.setBounciness(0.8f);
		physicsObj2.disableSleeping();
		miku.setPhysicsObject(physicsObj2);
		*/

		loc = ground.getWorldLocation();
		rot = new Quaternionf();
		(ground.getWorldLocation()).getNormalizedRotation(rot);
		physicsPlane = (engine.getSceneGraph()).addPhysicsStaticPlane(loc, rot, up, 0f);
		physicsPlane.setBounciness(1f);
		ground.setPhysicsObject(physicsPlane);

		// visualizes physics world
		engine.enableGraphicsWorldRender();
		engine.enablePhysicsWorldRender();
	}

	protected void processNetworking(float elapsTime) {
    	if (protClient != null)
        	protClient.processPackets();
		}

	@Override
	public void update()
	{	// updates elapsed time
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) {elapsTime += (currFrameTime - lastFrameTime) / 1000.0;}

		// check collisions and updates hudMsgs as necessary
		checkCollisions();
		//checking if packets received by the client from the server

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
		float heightOffset = myType.equalsIgnoreCase("miku") ? 2.0f : 1.0f;
		dio.setLocalLocation(new Vector3f(loc.x(), height + heightOffset, loc.z()));

		// update physics
		if (walking) {
			physicsEngine.update((float)elapsTime/1000.0f);
			for (GameObject go : engine.getSceneGraph().getGameObjects()){
				if (go.getPhysicsObject() != null) {
					Vector3f newLoc = go.getPhysicsObject().getLocation();
					Matrix4f locMat = new Matrix4f();
					locMat.set(3,0,loc.x());
					locMat.set(3,1,loc.y());
					locMat.set(3,2,loc.z());
					go.setLocalLocation(newLoc);
				}
				// set rotations
				Quarternion rot = go.getPhysicsObject().getRotation();
				Matrix4f rotMat = new Matrix4f();
				rot.get(rotMat);
				go.setLocalRotation(rotMat);
			}
		}

		// update inputs and camera according to game conditions
		if (!gameOver || gameWon) {
			walking = true;
			im.update((float)elapsTime);}

		// process networking packets
		processNetworking((float)elapsTime);
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
			{	protClient.sendByeMessage();
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
	
	// Method to send network movement update
	public void sendNetworkMovementUpdate()
	{	if (protClient != null)
		{	protClient.sendMoveMessage(dio.getWorldLocation());
		}
	}
}