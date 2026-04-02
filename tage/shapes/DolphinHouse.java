package tage.shapes;

import tage.*;
import org.joml.*;

/**
 * DolphinHouse is custom ManualObject created to represent the dolphin's house. It serves as the gallery to store the three photos taken at each site, once win conditions are met.
 * <p>
 * The player must 'hop off' the avatar using the [SPACE] button when all three photos are taken when at this spawnpoint. The photos should be hung on a wall.
 * 
 * Modification(s):
 * <ul>
 * <li>Removed roof</li>
 * <li>Removed one wall to act as an entry way</li>
 * </ul>
 * 
 * Added by:
 * @author Emily Kuang
 * @version Spring 2026
 */

public class DolphinHouse extends ManualObject {
    public DolphinHouse() {
        super();
        
        // 4-wall rectangular house + roof: 4 walls × 6 vertices = 24 vertices
        Vector3f[] vertices = new Vector3f[18];
        Vector2f[] texCoords = new Vector2f[18];
        Vector3f[] normals = new Vector3f[18];
        
        int vIndex = 0;
        
        // FRONT WALL (z = 2)
        // Triangle 1
        vertices[vIndex] = new Vector3f(-2, -1, 2);
        vertices[vIndex+1] = new Vector3f(2, -1, 2);
        vertices[vIndex+2] = new Vector3f(2, 1, 2);
        // Triangle 2
        vertices[vIndex+3] = new Vector3f(-2, -1, 2);
        vertices[vIndex+4] = new Vector3f(2, 1, 2);
        vertices[vIndex+5] = new Vector3f(-2, 1, 2);
        
        texCoords[vIndex] = new Vector2f(0, 0);
        texCoords[vIndex+1] = new Vector2f(1, 0);
        texCoords[vIndex+2] = new Vector2f(1, 1);
        texCoords[vIndex+3] = new Vector2f(0, 0);
        texCoords[vIndex+4] = new Vector2f(1, 1);
        texCoords[vIndex+5] = new Vector2f(0, 1);
        
        for (int i = vIndex; i < vIndex+6; i++) {
            normals[i] = new Vector3f(0, 0, 1);
        }
        vIndex += 6;
        
        // BACK WALL (z = -2)
        // Triangle 1
        vertices[vIndex] = new Vector3f(-2, -1, -2);
        vertices[vIndex+1] = new Vector3f(-2, 1, -2);
        vertices[vIndex+2] = new Vector3f(2, 1, -2);
        // Triangle 2
        vertices[vIndex+3] = new Vector3f(-2, -1, -2);
        vertices[vIndex+4] = new Vector3f(2, 1, -2);
        vertices[vIndex+5] = new Vector3f(2, -1, -2);
        
        texCoords[vIndex] = new Vector2f(0, 0);
        texCoords[vIndex+1] = new Vector2f(0, 1);
        texCoords[vIndex+2] = new Vector2f(1, 1);
        texCoords[vIndex+3] = new Vector2f(0, 0);
        texCoords[vIndex+4] = new Vector2f(1, 1);
        texCoords[vIndex+5] = new Vector2f(1, 0);
        
        for (int i = vIndex; i < vIndex+6; i++) {
            normals[i] = new Vector3f(0, 0, -1);
        }
        vIndex += 6;
        
        // LEFT WALL (x = -2)
        // Triangle 1
        vertices[vIndex] = new Vector3f(-2, -1, 2);
        vertices[vIndex+1] = new Vector3f(-2, 1, 2);
        vertices[vIndex+2] = new Vector3f(-2, 1, -2);
        // Triangle 2
        vertices[vIndex+3] = new Vector3f(-2, -1, 2);
        vertices[vIndex+4] = new Vector3f(-2, 1, -2);
        vertices[vIndex+5] = new Vector3f(-2, -1, -2);
        
        texCoords[vIndex] = new Vector2f(0, 0);
        texCoords[vIndex+1] = new Vector2f(0, 1);
        texCoords[vIndex+2] = new Vector2f(1, 1);
        texCoords[vIndex+3] = new Vector2f(0, 0);
        texCoords[vIndex+4] = new Vector2f(1, 1);
        texCoords[vIndex+5] = new Vector2f(1, 0);
        
        for (int i = vIndex; i < vIndex+6; i++) {
            normals[i] = new Vector3f(-1, 0, 0);
        }
        vIndex += 6;
        
        // Set number of vertices FIRST, then load the data
        setNumVertices(18);
        setVertices(vertices);
        setTexCoords(texCoords);
        setNormals(normals);
    }    
}
