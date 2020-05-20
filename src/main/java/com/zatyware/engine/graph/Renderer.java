package com.zatyware.engine.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import static org.lwjgl.opengl.GL11.*;
import com.zatyware.engine.GameItem;
import com.zatyware.engine.IHud;
import com.zatyware.engine.Scene;
import com.zatyware.engine.SceneLight;
import com.zatyware.engine.SkyBox;
import com.zatyware.engine.Utils;
import com.zatyware.engine.Window;

public class Renderer {
	
	private static final float FOV = (float) Math.toRadians(60.0f);
	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.0f;
	private static final int MAX_POINT_LIGHTS = 5;
	private static final int MAX_SPOT_LIGHTS = 5;
	private final Transformation transformation;
	private ShaderProgram sceneShaderProgram;
	private ShaderProgram hudShaderProgram;
	private ShaderProgram skyBoxShaderProgram;
	
	private float specularPower;
	
	public Renderer() {
		transformation = new Transformation();
		specularPower = 10f;
	}

	public void init(Window window) throws Exception {
		setupSkyBoxShader();
		setupSceneShader();
		setupHudShader();
	}
	
	private void setupSkyBoxShader() throws Exception {
		skyBoxShaderProgram = new ShaderProgram();
		skyBoxShaderProgram.createVertexShader(Utils.loadResource("/shaders/sb_vertex.vs"));
		skyBoxShaderProgram.createFragmentShader(Utils.loadResource("/shaders/sb_fragment.fs"));
		skyBoxShaderProgram.link();
		
		// Create uniforms for projection matrix
		skyBoxShaderProgram.createUniform("projectionMatrix");
		skyBoxShaderProgram.createUniform("modelViewMatrix");
		skyBoxShaderProgram.createUniform("texture_sampler");
		skyBoxShaderProgram.createUniform("ambientLight");
	}
	
	private void setupSceneShader() throws Exception {
		// Create shader
		sceneShaderProgram = new ShaderProgram();
		sceneShaderProgram.createVertexShader(Utils.loadResource("/shaders/vertex.vs"));
		sceneShaderProgram.createFragmentShader(Utils.loadResource("/shaders/fragment.fs"));
		sceneShaderProgram.link();
		
		sceneShaderProgram.createUniform("projectionMatrix");
		sceneShaderProgram.createUniform("modelViewMatrix");
		sceneShaderProgram.createUniform("texture_sampler");
		
		sceneShaderProgram.createMaterialUniform("material");
		
		sceneShaderProgram.createUniform("specularPower");
		sceneShaderProgram.createUniform("ambientLight");
		sceneShaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
		sceneShaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
		sceneShaderProgram.createDirectionalLightUniform("directionalLight");
	}
	
	private void setupHudShader() throws Exception {
		hudShaderProgram = new ShaderProgram();
		hudShaderProgram.createVertexShader(Utils.loadResource("/shaders/hud_vertex.vs"));
		hudShaderProgram.createFragmentShader(Utils.loadResource("/shaders/hud_fragment.fs"));
		hudShaderProgram.link();
		
		// Create uniforms for Ortographic-model projection matrix and base color
		hudShaderProgram.createUniform("projModelMatrix");
		hudShaderProgram.createUniform("color");
		hudShaderProgram.createUniform("hasTexture");
	}
	
	public void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}
	
	public void render(Window window, Camera camera, Scene scene, IHud hud) {
		clear();
		
		if (window.isResized()) {
			glViewport(0,0, window.getWidth(), window.getHeight());
			window.setResized(false);
		}
		
		renderScene(window, camera, scene);
		
		renderSkyBox(window, camera, scene);
		
		renderHud(window, hud);
	}
	
	private void renderSkyBox(Window window, Camera camera, Scene scene) {
		skyBoxShaderProgram.bind();
		
		skyBoxShaderProgram.setUniform("texture_sampler", 0);
		
		// Update projection Matrix
		Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, window.getWidth(), window.getHeight(), Z_NEAR, Z_FAR);
		skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);
		SkyBox skyBox = scene.getSkyBox();
		Matrix4f viewMatrix = transformation.getViewMatrix(camera);
		viewMatrix.m30(0);
		viewMatrix.m31(0);
		viewMatrix.m32(0);
		Matrix4f modelViewMatrix = transformation.getModelViewMatrix(skyBox, viewMatrix);
		skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
		skyBoxShaderProgram.setUniform("ambientLight", scene.getSceneLight().getAmbientLight());
		
		scene.getSkyBox().getMesh().render();
		
		skyBoxShaderProgram.unbind();
	}
	
	public void renderScene(Window window, Camera camera, Scene scene) {
		
		sceneShaderProgram.bind();
		
		// Update projection Matrix
		Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, window.getWidth(), window.getHeight(), Z_NEAR, Z_FAR);
		sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);
		
		// Update view Matrix
		Matrix4f viewMatrix = transformation.getViewMatrix(camera);
		
		// Update Light Uniforms
		SceneLight sceneLight = scene.getSceneLight();
		renderLights(viewMatrix, sceneLight);
		
		sceneShaderProgram.setUniform("texture_sampler", 0);
		
		// Render each gameItem
		GameItem[] gameItems = scene.getGameItems();
		for (GameItem gameItem : gameItems) {
			Mesh mesh = gameItem.getMesh();
			// Set model view matrix for this item
			Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
			sceneShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
			// Render the mesh for this game item
			sceneShaderProgram.setUniform("material", mesh.getMaterial());
			mesh.render();
		}
		
		sceneShaderProgram.unbind();
	}
	
	private void renderLights(Matrix4f viewMatrix, SceneLight sceneLight) {
		sceneShaderProgram.setUniform("ambientLight", sceneLight.getAmbientLight());
		sceneShaderProgram.setUniform("specularPower", specularPower);

		//Process Point Lights
		PointLight[] pointLightList = sceneLight.getPointLightList();
		int numLights = pointLightList != null?pointLightList.length:0;
		for (int i=0; i<numLights; i++) {
			// Get a copy of the light object and transform its position to view coordinates
			PointLight currPointLight = new PointLight(pointLightList[i]);
			Vector3f lightPos = currPointLight.getPosition();
			Vector4f aux = new Vector4f(lightPos, 1);
			aux.mul(viewMatrix);
			lightPos.x = aux.x;
			lightPos.y = aux.y;
			lightPos.z = aux.z;
			sceneShaderProgram.setUniform("pointLights", currPointLight, i);
		}
			
		// Process Spot Lights
		SpotLight[] spotLightList = sceneLight.getSpotLightList();
		numLights = spotLightList != null?spotLightList.length:0;
		for (int i=0; i<numLights; i++) {
			// Get a copy of the spot light object and transform its position and cone direction to view coordinates
			SpotLight currSpotLight = new SpotLight(spotLightList[i]);
			Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
			dir.mul(viewMatrix);
			currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));
			
			Vector3f spotLightPos = currSpotLight.getPointLight().getPosition();
			Vector4f auxSpot = new Vector4f(spotLightPos, 1);
			auxSpot.mul(viewMatrix);
			spotLightPos.x = auxSpot.x;
			spotLightPos.y = auxSpot.y;
			spotLightPos.z = auxSpot.z;
			
			sceneShaderProgram.setUniform("spotLights", currSpotLight, i);
		}
		
		// Get a copy of the directional light object and transform its position to view coordinates
		DirectionalLight currDirLight = new DirectionalLight(sceneLight.getDirectionalLight());
		Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
		dir.mul(viewMatrix);
		currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
		sceneShaderProgram.setUniform("directionalLight", currDirLight);
		
	}
	
	private void renderHud(Window window, IHud hud) {
		hudShaderProgram.bind();
		
		Matrix4f ortho = transformation.getOrthoProjectionMatrix(0, window.getWidth(), window.getHeight(), 0);
		for (GameItem gameItem : hud.getGameItems()) {
			Mesh mesh = gameItem.getMesh();
			// Set orthographic and model matrix for this HUD item
			Matrix4f projModelMatrix = transformation.getOrtoProjModelMatrix(gameItem, ortho);
			hudShaderProgram.setUniform("projModelMatrix", projModelMatrix);
			hudShaderProgram.setUniform("color", gameItem.getMesh().getMaterial().getAmbientColor());
			hudShaderProgram.setUniform("hasTexture", gameItem.getMesh().getMaterial().isTextured() ? 1 : 0);
			
			// Render the mesh for this HUD item
			mesh.render();
		}
		
		hudShaderProgram.unbind();
	}
	
	public void cleanup() {
		if (skyBoxShaderProgram != null) {
			skyBoxShaderProgram.cleanup();
		}
		if (sceneShaderProgram != null) {
			sceneShaderProgram.cleanup();
		}
		if (hudShaderProgram != null) {
			hudShaderProgram.cleanup();
		}
	}
}
