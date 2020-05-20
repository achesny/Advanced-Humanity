package com.zatyware.engine;

import com.zatyware.engine.graph.Material;
import com.zatyware.engine.graph.Mesh;
import com.zatyware.engine.graph.OBJLoader;
import com.zatyware.engine.graph.Texture;

public class SkyBox extends GameItem {
	public SkyBox(String objModel, String textureFile) throws Exception {
		super();
		Mesh skyBoxMesh = OBJLoader.loadMesh(objModel);
		Texture skyBoxTexture = new Texture(textureFile);
		skyBoxMesh.setMaterial(new Material(skyBoxTexture, 0.0f));
		setMesh(skyBoxMesh);
		setPosition(0,0,0);
	}
}
