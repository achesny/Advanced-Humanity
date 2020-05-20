package com.zatyware.anthropologie;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_N;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.zatyware.engine.GameItem;
import com.zatyware.engine.IGameLogic;
import com.zatyware.engine.MouseInput;
import com.zatyware.engine.Scene;
import com.zatyware.engine.SceneLight;
import com.zatyware.engine.SkyBox;
import com.zatyware.engine.Window;
import com.zatyware.engine.graph.Camera;
import com.zatyware.engine.graph.DirectionalLight;
import com.zatyware.engine.graph.Material;
import com.zatyware.engine.graph.Mesh;
import com.zatyware.engine.graph.OBJLoader;
import com.zatyware.engine.graph.PointLight;
import com.zatyware.engine.graph.Renderer;
import com.zatyware.engine.graph.SpotLight;
import com.zatyware.engine.graph.Texture;
import com.zatyware.engine.graph.PointLight.Attenuation;

public class Game implements IGameLogic {
	
	private static final float MOUSE_SENSITIVITY = 0.2f;
	private final Vector3f cameraInc;
	private final Renderer renderer;
	private final Camera camera;
	private Scene scene;
	private Hud hud;
	private float lightAngle;
	private static final float CAMERA_POS_STEP = 0.05f;

	public Game() {
		renderer = new Renderer();
		camera = new Camera();
		cameraInc = new Vector3f(0.0f, 0.0f, 0.0f);
		lightAngle = -90;
	}

	@Override
	public void init(Window window) throws Exception {
		renderer.init(window);
		
		scene = new Scene();
		
		// Setup GameItems
		float reflectance = 1f;

		Mesh mesh = OBJLoader.loadMesh("/models/cube.obj");
	    Texture texture = new Texture("textures/grassblock.png");
	    Material material = new Material(texture, reflectance);
	    mesh.setMaterial(material);
	    
	    float blockScale = 0.5f;
	    float skyBoxScale = 10.0f;
	    float extension = 2.0f;
	    
	    float startx = extension * (-skyBoxScale + blockScale);
	    float startz = extension * (skyBoxScale - blockScale);
	    float starty = -1.0f;
	    float inc = blockScale * 2;
	    
	    float posx = startx;
	    float posz = startz;
	    float incy = 0.0f;
	    int NUM_ROWS = (int)(extension * skyBoxScale * 2 / inc);
	    int NUM_COLS = (int)(extension * skyBoxScale * 2 / inc);
	    GameItem[] gameItems = new GameItem[NUM_ROWS * NUM_COLS];
	    for(int i=0; i<NUM_COLS; i++) {
	    	for(int j=0; j<NUM_COLS; j++) {
	    		GameItem gameItem = new GameItem(mesh);
	    		gameItem.setScale(blockScale);
	    		incy = Math.random() > 0.9f ? blockScale * 2 : 0f;
	    		gameItem.setPosition(posx, starty + incy, posz);
	    		gameItems[i*NUM_COLS + j] = gameItem;
	    		
	    		posx += inc;
	    	}
	    	posx = startx;
	    	posz -= inc;
	    }
	    scene.setGameItems(gameItems);
	    
	    // Setup SkyBox
	    SkyBox skyBox = new SkyBox("/models/skybox.obj", "textures/skybox.png");
	    skyBox.setScale(skyBoxScale);
	    scene.setSkyBox(skyBox);
	    
	    // Setup Lights
	    setupLights();
	    
	    // Create HUD
	    hud = new Hud("DEMO");

	    camera.getPosition().x = 0.65f;
	    camera.getPosition().y = 1.15f;
	    camera.getPosition().z = 4.34f;
	}

	private void setupLights() {
        SceneLight sceneLight = new SceneLight();
        scene.setSceneLight(sceneLight);
        
        //Ambient Light
        sceneLight.setAmbientLight(new Vector3f(0.3f, 0.3f, 0.3f));
        
        // Directional Light
        float lightIntensity = 1.0f;
        Vector3f lightPosition = new Vector3f(-1, 0, 0);
        sceneLight.setDirectionalLight(new DirectionalLight(new Vector3f(1, 1, 1), lightPosition, lightIntensity));		
	}
	
	@Override
	public void input(Window window, MouseInput mouseInput) {
		cameraInc.set(0, 0, 0);
		if (window.isKeyPressed(GLFW_KEY_W)) {
			cameraInc.z = -1;
		} else if (window.isKeyPressed(GLFW_KEY_S)) {
			cameraInc.z = 1;
		}
		if (window.isKeyPressed(GLFW_KEY_A)) {
			cameraInc.x = -1;
		} else if (window.isKeyPressed(GLFW_KEY_D)) {
			cameraInc.x = 1;
		}
		if (window.isKeyPressed(GLFW_KEY_Z)) {
			cameraInc.y = -1;
		} else if (window.isKeyPressed(GLFW_KEY_X)) {
			cameraInc.y = 1;
		}
	}

	@Override
	public void update(float interval, MouseInput mouseInput) {
		// Update camera based on mouse
		if (mouseInput.isRightButtonPressed()) {
			Vector2f rotVec = mouseInput.getDisplVec();
			camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
			
			// Update HUD compass
			hud.rotateCompass(camera.getRotation().y);
		}
		
		// Update camera position
		camera.movePosition(cameraInc.x * CAMERA_POS_STEP, cameraInc.y * CAMERA_POS_STEP, cameraInc.z * CAMERA_POS_STEP);
		
		SceneLight sceneLight = scene.getSceneLight();
		
		// Update directional light direction, intensity, and color
		DirectionalLight directionalLight = sceneLight.getDirectionalLight();
		lightAngle += 1.1f;
		if (lightAngle > 90) {
			directionalLight.setIntensity(0);
			if (lightAngle >= 360) {
				lightAngle = -90;
			}
		} else if (lightAngle <= -80 || lightAngle >= 80) {
			float factor = 1 - (float) (Math.abs(lightAngle) - 80) / 10.0f;
			directionalLight.setIntensity(factor);
			directionalLight.getColor().y = Math.max(factor, 0.9f);
			directionalLight.getColor().z = Math.max(factor, 0.5f);
		} else {
			directionalLight.setIntensity(1);
			directionalLight.getColor().x = 1;
			directionalLight.getColor().y = 1;
			directionalLight.getColor().z = 1;
		}
		double angRad = Math.toRadians(lightAngle);
		directionalLight.getDirection().x = (float) Math.sin(angRad);
		directionalLight.getDirection().y = (float) Math.cos(angRad);
	}

	@Override
	public void render(Window window) {
		hud.updateSize(window);
		renderer.render(window, camera, scene, hud);
	}
	
	@Override
	public void cleanup() {
		renderer.cleanup();
		GameItem[] gameItems = scene.getGameItems();
		for (GameItem gameItem : gameItems) {
			gameItem.getMesh().cleanUp();
		}
		hud.cleanup();
	}

}
