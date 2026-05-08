package a3;
import java.util.Random;

public class NPC
{ 
    private double locationX, locationY, locationZ;
    private double dir = 0.1;
    private double size = 1.0;
    private double speed = 0.1; 
    private float yaw = 90.0f;
    private int id;
    private boolean isEnemy;
    private int moveTimer = 0; 
    private Random rand = new Random();

    // --- State flags per dolphin ---
    private boolean isSpinning = false;
    private boolean isPlayerNear = false; // Tracks if player is close to THIS dolphin
    private final float SPIN_SPEED = 15.0f;

    public NPC(int id, boolean enemy)
    { 
        this.id = id;
        this.isEnemy = enemy;
        locationX = 0.0;
        locationY = 0.0;
        locationZ = 0.0;

    }

    public void randomizeLocation(int seedX, int seedZ)
    { 
        locationX = (double) seedX;
        locationY = 2.0;
        locationZ = (double) seedZ;
    }

    public int getId() { return id; }
    public double getX() { return locationX; } 
    public double getY() { return locationY; } 
    public double getZ() { return locationZ; } 
    public float getYaw() { return yaw; } // Getter for the server to send yaw to clients
    public boolean isEnemy() { return isEnemy; }

    public void setSpeed(double s) { this.speed = s; }
    public void setSpinning(boolean spin) { this.isSpinning = spin; }
    
    public void setPlayerNear(boolean near) { this.isPlayerNear = near; }
    public boolean isPlayerNear() { return isPlayerNear; }

    public void updateLocation() {
        this.isSpinning = this.isPlayerNear; 

        if (isSpinning) {
            // If spinning, we stop X/Z movement and just rotate
            yaw = (yaw + 15.0f) % 360.0f; 
        } 
        else {
            // 1. Randomize direction logic
            moveTimer--;
            if (moveTimer <= 0) {
                yaw = (float) rand.nextInt(360); 
                moveTimer = rand.nextInt(100) + 50; 
            }

            // 2. Trigonometry for 2D movement
            float angleRad = (float) Math.toRadians(yaw);
            locationX += Math.sin(angleRad) * speed;
            locationZ += Math.cos(angleRad) * speed;

            // 3. Boundary Box: Bounce off the "walls"
            if (Math.abs(locationX) > 40) { 
                yaw = (yaw + 180) % 360; 
                locationX = (locationX > 0) ? 39 : -39; // Snap back inside
            }
            if (Math.abs(locationZ) > 40) { 
                yaw = (yaw + 180) % 360; 
                locationZ = (locationZ > 0) ? 39 : -39; // Snap back inside
            }
        }
    }
}