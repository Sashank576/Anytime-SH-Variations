package main;

import app.StartDesktopApp;
import mcts.EntropySHUCTAnytime;
import mcts.ExampleUCT;
import mcts.StandardClustering;
import mcts.DoubleIterationClustering;
import mcts.SHUCT;
import mcts.SHUCTAnyTime;
import mcts.SHUCTTime;
import utils.AIRegistry;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class LaunchLudii
{
	
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// Register our example AIs
		if (!AIRegistry.registerAI("Example UCT", () -> {return new ExampleUCT(-1);}, (game) -> {return new ExampleUCT(-1).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");
		
		if (!AIRegistry.registerAI("SHUCT", () -> {return new SHUCT(2000);}, (game) -> {return new SHUCT(2000).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("SHUCTTime", () -> {return new SHUCTTime();}, (game) -> {return new SHUCTTime().supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("SHUCTAnyTime", () -> {return new SHUCTAnyTime(true, -1, -1);}, (game) -> {return new SHUCTAnyTime(true, -1, -1).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("EntropySHUCTAnytime", () -> {return new EntropySHUCTAnytime(true, -1, 0.5, -1);}, (game) -> {return new EntropySHUCTAnytime(true, -1, 0.3875, -1).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("StandardClustering", () -> {return new StandardClustering(true, -1, -1);}, (game) -> {return new StandardClustering(true, -1, -1).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		if (!AIRegistry.registerAI("DoubleIterationClustering", () -> {return new DoubleIterationClustering(true, -1, -1);}, (game) -> {return new DoubleIterationClustering(true, -1, -1).supportsGame(game);}))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		// Run Ludii
		StartDesktopApp.main(new String[0]);
	}

}
