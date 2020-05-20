package com.zatyware.engine;

import org.joml.Vector3f;

import com.zatyware.engine.graph.DirectionalLight;
import com.zatyware.engine.graph.PointLight;
import com.zatyware.engine.graph.SpotLight;

public class SceneLight {
	private Vector3f ambientLight;
	private PointLight[] pointLightList;
	private SpotLight[] spotLightList;
	private DirectionalLight directionalLight;
	
	public Vector3f getAmbientLight() {
		return ambientLight;
	}
	
	public void setAmbientLight(Vector3f ambientLight) {
		this.ambientLight = ambientLight;
	}

	public PointLight[] getPointLightList() {
		return pointLightList;
	}

	public void setPointLightList(PointLight[] pointLightList) {
		this.pointLightList = pointLightList;
	}

	public SpotLight[] getSpotLightList() {
		return spotLightList;
	}

	public void setSpotLightList(SpotLight[] spotLightList) {
		this.spotLightList = spotLightList;
	}

	public DirectionalLight getDirectionalLight() {
		return directionalLight;
	}

	public void setDirectionalLight(DirectionalLight directionalLight) {
		this.directionalLight = directionalLight;
	}
}
