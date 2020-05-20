package com.zatyware.anthropologie;

import com.zatyware.engine.GameEngine;
import com.zatyware.engine.IGameLogic;

public class GameMain {

	public static void main(String[] args) {
		try {
			boolean vSync = true;
			IGameLogic gameLogic = new Game();
			GameEngine gameEng = new GameEngine("GAME", 600, 480, vSync, gameLogic);
			gameEng.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

}
