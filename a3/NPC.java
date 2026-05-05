package a3;

public class NPC
{ 
    private double locationX, locationY, locationZ;
    private double dir = 0.1;
    private double size = 1.0;
    private double speed = 0.1; 
    private float yaw = 90.0f;
    private int id;

    // --- State flags per dolphin ---
    private boolean isSpinning = false;
    private boolean isPlayerNear = false; // Tracks if player is close to THIS dolphin
    private final float SPIN_SPEED = 15.0f;

    public NPC(int id)
    { 
        this.id = id;
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
    
    public void setSpeed(double s) { this.speed = s; }
    public void setSpinning(boolean spin) { this.isSpinning = spin; }
    
    public void setPlayerNear(boolean near) { this.isPlayerNear = near; }
    public boolean isPlayerNear() { return isPlayerNear; }

    public void updateLocation()
    { 
        if (isSpinning) {
            // Spin in place
            yaw = (yaw + SPIN_SPEED) % 360.0f; 
        } 
        else {
            // Normal patrol movement back and forth
            if (locationX > 25.0) {
                dir = -1.0; 
                yaw = 270.0f; 
            }
            if (locationX < -25.0) {
                dir = 1.0; 
                yaw = 90.0f; 
            }
            locationX = locationX + (dir * speed); 
        }
    }
}