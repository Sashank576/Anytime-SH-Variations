package mcts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.RankUtils;
import other.context.Context;
import other.move.Move;

/**
 * An "AnyTime" Sequential Halving Agent Variation, utilizing UCB1 below the root node.
 * 
 * Only supports deterministic, alternating-move games.
 * 
 * This Standard Clustering Agent modifies the pruning strategy by testing every possible split of the children nodes
 * For every split, it calculates the Sum of Squared Errors (SSE) and selects the split that minimizes the SSE.
 * Since a dynamic number of nodes can be pruned, the iterations are split evenly across the remaining nodes in the round.
 * 
 * This class is a modified version of the Anytime Sequential Halving agent provided by Dominic Sagers.
 * @author Sashank Chapala
 */
public class StandardClustering extends AI
{
	
	//-------------------------------------------------------------------------
	
	/** Our player index */
	protected int player = -1;
	public boolean iterMode;
	public int totalIterations;

	//Value of exploration constant used in UCB1
	public double explorationConstant;
	
	/** We use this because our command line arguments only include an option for seconds */
	public int iterBudget = -1;
	
	//-------------------------------------------------------------------------
	//Necessary variables for the SH algorithm.
	/**
	 * Constructor
	 */
	public StandardClustering(boolean iterMode, int iterBudget, double explorationConstant)
	{
		this.friendlyName = "StandardClustering";
		this.iterMode = iterMode;

		if(explorationConstant == -1.0){
			//default value is sqrt(2)
			this.explorationConstant = Math.sqrt(2);
		}
		else{
			this.explorationConstant = explorationConstant;
		}

		if (iterMode)
		{
			this.iterBudget = iterBudget;
		}
		
	}
	
	public boolean stopConditionMet(final long stopTime, final int iterationBudget){
		if (this.iterMode)
		{
			if (this.totalIterations < iterationBudget)
			{
				return false;
			}
		}
		else
		{
			if (System.currentTimeMillis() < stopTime)
			{
				return false;
			}
		}
		return true;	
	}
	

	/*
	 * Algorithm idea
	 * currentConsideredNodes: The indexes of the nodes which are to be searched 
	 * 
	 * idx = 0
	 * 
	 * While(time not up){
	 * 	if(rootNotExpanded){
	 * 	expand root
	 * }else{
	 * 
	 * currentChild = currentNodes.get(idx);
	 * 	search currentChild
	 * 
	 * }
	 * if(idx + 1 == nodesToVisit){
	 * 	if(currentNodes.size() == 2){
	 * 
	 * 		currentNodes = root.children
	 * 
	* 	}else{
	* 
	* 	}
	 * 
	 * 	currentNodes = halveRoot();
	 * 	idx = 0;
	 * }else{
	 * idx++;
	 * }
	 * 
	 * 
	 */
	//-------------------------------------------------------------------------
	
	@Override
	public Move selectAction
	(
		final Game game,
		final Context context, 
		final double maxSeconds, 
		final int maxIterations, 
		final int maxDepth
	)
	{
		// Start out by creating a new root node (no tree reuse in this example)
		final Node root = new Node(null, null, context);
		
		// We'll respect any limitations on max seconds and max iterations (don't care about max depth)
		int iterationBudgetMultiplier = 1000;
		int iterationBudget;
		if (this.iterBudget == -1)
		{
			if (maxIterations > 0)
			{
				iterationBudget = maxIterations;
			}
			else
			{
				iterationBudget = Double.valueOf(maxSeconds).intValue() * iterationBudgetMultiplier;
			}
		}
		else
		{
			iterationBudget = this.iterBudget;
		}
		
		// this.timeBudget = Double.valueOf(maxSeconds).intValue();
		final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;

		// Our main loop through MCTS iterations
		//ArrayList<Integer> hist = new ArrayList<>();
		boolean rootFullyExpanded = false;
		int numPossibleMoves = root.unexpandedMoves.size();
		Node currentChild;
		int armVisitCount = 0;
		
		// A list containing the indices of the nodes we are searching from root.children
		ArrayList<Integer> currentChildrenIdx = new ArrayList<Integer>();
		for (int i = 0; i < root.children.size(); i++)
		{
			currentChildrenIdx.add(i);
		}

		// System.err.println("possible moves: " + numPossibleMoves);
		int rootNodesVisited = 0;
		
		int idx = 0;//keeps track of where we are in the index list

		// System.out.println("iterationBudget: " + this.iterationBudget + " \n numIterations: " + this.numIterations);
		// System.out.println("Iter per round: " + this.iterPerRound);
		//System.currentTimeMillis() < stopTime && 
		while 
		(
			!this.stopConditionMet(stopTime, iterationBudget) &&					
			!wantsInterrupt								// Respect GUI user clicking the pause button
		)
		{
			// Start in root node
			if (!rootFullyExpanded)
			{
				//System.out.println("starting root search");
				//System.out.println(rootNodesVisited);

				Node current = root;
				
				// Traverse tree
				while (true)
				{
					if (current.context.trial().over())
					{
						// We've reached a terminal state
						break;
					}
					
					current = ucb1Select(current, explorationConstant);
					
					if (current.visitCount == 0)
					{
						// We've expanded a new node, time for playout!
						break;
					}
				}
				
				Context contextEnd = current.context;
				
				if (!contextEnd.trial().over())
				{
					// Run a playout if we don't already have a terminal game state in node
					contextEnd = new Context(contextEnd);
					game.playout
					(
						contextEnd, 
						null, 
						-1.0, 
						null, 
						0, 
						200, 
						ThreadLocalRandom.current()
					);
				}
				
				// This computes utilities for all players at the of the playout,
				// which will all be values in [-1.0, 1.0]
				final double[] utilities = RankUtils.utilities(contextEnd);
				
				// Backpropagate utilities through the tree
				while (current != null)
				{
					current.visitCount += 1;
					for (int p = 1; p <= game.players().count(); ++p)
					{
						current.scoreSums[p] += utilities[p];
					}
					current = current.parent;
				}
				
				rootNodesVisited++;
				armVisitCount++;
				this.totalIterations++;

				if (rootNodesVisited == numPossibleMoves)
				{
					//System.out.println("First round over");
					rootFullyExpanded = true;
					currentChild = root.children.get(0); 
				}
			}
			else 
			{
				currentChild = root.children.get(currentChildrenIdx.get(idx));
				
				Node current = currentChild;
							
				// Traverse tree
				while (true)
				{
					if (current.context.trial().over())
					{
						// We've reached a terminal state
						break;
					}
					
					current = ucb1Select(current, explorationConstant);
					
					if (current.visitCount == 0)
					{
						// We've expanded a new node, time for playout!
						break;
					}
				}
				
				Context contextEnd = current.context;
				
				if (!contextEnd.trial().over())
				{
					// Run a playout if we don't already have a terminal game state in node
					contextEnd = new Context(contextEnd);
					game.playout
					(
						contextEnd, 
						null, 
						-1.0, 
						null, 
						0, 
						200, 
						ThreadLocalRandom.current()
					);
				}
				
				// This computes utilities for all players at the of the playout,
				// which will all be values in [-1.0, 1.0]
				final double[] utilities = RankUtils.utilities(contextEnd);
				
				// Backpropagate utilities through the tree
				while (current != null)
				{
					current.visitCount += 1;
					for (int p = 1; p <= game.players().count(); ++p)
					{
						current.scoreSums[p] += utilities[p];
					}
					current = current.parent;
				}
				
				// Increment iteration counts

				//hist.add(currentChildrenIdx.get(idx));
				armVisitCount++;
				this.totalIterations++;
				idx++;

			}

			if (armVisitCount == numPossibleMoves)
			{ //if we have visited the all children before halving
				armVisitCount = 0;
				if (currentChildrenIdx.size() <= 2)
				{ //if we have visited all children AND we have halved the amount of times required
					currentChildrenIdx = new ArrayList<Integer>(); //reset the index list

					for (int i = 0; i < root.children.size();i++)
					{
						currentChildrenIdx.add(i);
					}

					idx = 0;
				}
				else
				{ //We haven't finished halving, so we halve based on the current exploit values
					//System.out.println("before: " + currentChildrenIdx.toString());
					currentChildrenIdx = selectBestSplit(root, currentChildrenIdx);
					//hist.add(999);
					//System.out.println("After: " + currentChildrenIdx.toString());
					
					idx = 0;
				}
			}

			if (idx >= currentChildrenIdx.size())
			{ 
				idx = 0; 
			}
		}

		// displayHist(hist, this);
		// System.out.println("Iterations made: " + this.totalIterations);
		this.totalIterations = 0;
		//System.out.println(hist.toString());
		
		// Return the move we wish to play
		return finalMoveSelection(root);
	}

	/**This method takes the rootNode, sorts it's children by their exploit value, finds the best split that minimizes the SSE and returns the upper/better cluster of nodes from that split.
	 * @param rootNode
	 */
	public static ArrayList<Integer> selectBestSplit(final Node rootNode, final ArrayList<Integer> currentChildrenIndexes){
		ArrayList<Integer> newIndexes = new ArrayList<>();

		int numChildren = currentChildrenIndexes.size();
		if (numChildren > 2)
		{
			final int mover = rootNode.context.state().mover();

			//Make a list sorting each child node by value and then take the best half.

			// A list of lists where the first index of the inner list is the node 
			// index and the second index is the value of that node
			ArrayList<ArrayList<Double>> nodeValues = new ArrayList<>();
			
			for (int i = 0; i < numChildren; ++i) 
			{
				final Node child = rootNode.children.get(currentChildrenIndexes.get(i));
				final double exploit = child.scoreSums[mover] / child.visitCount;

				ArrayList<Double> val = new ArrayList<>();

				val.add((double) currentChildrenIndexes.get(i));
				val.add(exploit);
				nodeValues.add(val);
			}

			// sort in descending order based on the value (second index) of each inner list:
			nodeValues.sort(Comparator.comparingDouble((ArrayList<Double> list) -> list.get(1)).reversed());

			// System.out.println("nodeValues after sorting descending: " + nodeValues.toString());

			//Create variables to store the best split with the lowest Sum of Squared Errors and the index of that split
			double bestSSE = Double.POSITIVE_INFINITY;
			int bestIndex = -1;

			//Loop through the all nodes and test out every split point to find the one that minimizes the SSE
			for(int i = 1; i < nodeValues.size(); i++){
				//Split nodeValue into two clusters based on i
				ArrayList<ArrayList<Double>> upperCluster = new ArrayList<>(nodeValues.subList(0, i));
				ArrayList<ArrayList<Double>> lowerCluster = new ArrayList<>(nodeValues.subList(i, nodeValues.size()));
				//Calculate total SSE for both clusters
				double totalSSE = computeSumOfSquaredErrors(upperCluster) + computeSumOfSquaredErrors(lowerCluster);

				//Store best split/index
				if(totalSSE < bestSSE){
					bestSSE = totalSSE;
					bestIndex = i;
				}
			}

			//Only store the upper cluster since that represents the nodes with the highest exploit values
			//new list which only contains the nodes we want to keep in the tree:
			ArrayList<ArrayList<Double>> bestSplit = new ArrayList<>(nodeValues.subList(0, bestIndex));

			for (int i = 0; i < bestSplit.size(); i++)
			{
				newIndexes.add(Double.valueOf(bestSplit.get(i).get(0)).intValue());
			}
			
		}
		return newIndexes;
	}

	/**
	 * This method calculates the sum of squared errors for a cluster of nodes which contain exploit values.
	 * @param cluster
	 */
    public static double computeSumOfSquaredErrors(ArrayList<ArrayList<Double>> cluster) {
		double mean = 0.0;
		//Calculate the mean of the exploit values from all nodes in the cluster
		for (int i = 0; i < cluster.size(); i++){
			mean += cluster.get(i).get(1);
		}
		mean = mean/cluster.size();

		//Calculate the sum of squared errors for the cluster
		double sumOfSquaredErrors = 0.0;
		for (int i = 0; i < cluster.size(); i++){
			double deviation = cluster.get(i).get(1) - mean;
			sumOfSquaredErrors += Math.pow(deviation, 2);
		}
        return sumOfSquaredErrors;
    }

	/**
	 	*This function neatly prints the hist variable used in the algorithm. Displays value counts for each node index visited.
		 * @param hist
		 * @param algo
		 */
	public static void displayHist(ArrayList<Integer> hist, StandardClustering algo){
		 
		// Count occurrences of each integer
		HashMap<Integer, Integer> counts = new HashMap<>();
		System.out.println("VisitCounts\n______________");
		System.out.println(algo.friendlyName);
		for (Integer num : hist) {
			if(num != 999){
				counts.put(num, counts.getOrDefault(num, 0) + 1);
			}
			
		}
		
		// Display bin counts
		for (Integer key : counts.keySet()) {
			System.out.println("Number: " + key + ", Count: " + counts.get(key));
		}

		System.out.println("______________");
   }

	
	/**
	 * Selects child of the given "current" node according to UCB1 equation.
	 * This method also implements the "Expansion" phase of MCTS, and creates
	 * a new node if the given current node has unexpanded moves.
	 * 
	 * @param current
	 * @return Selected node (if it has 0 visits, it will be a newly-expanded node).
	 */
	public static Node ucb1Select(final Node current, final double explorationConstant)
	{
		if (!current.unexpandedMoves.isEmpty())
		{
			// randomly select an unexpanded move
			final Move move = current.unexpandedMoves.remove(
					ThreadLocalRandom.current().nextInt(current.unexpandedMoves.size()));
			
			// create a copy of context
			final Context context = new Context(current.context);
			
			// apply the move
			context.game().apply(context, move);
			
			// create new node and return it
			return new Node(current, move, context);
		}
		
		// use UCB1 equation to select from all children, with random tie-breaking
		Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        final double twoParentLog = Math.log(Math.max(1, current.visitCount));
        int numBestFound = 0;
        
        final int numChildren = current.children.size();
        final int mover = current.context.state().mover();

        for (int i = 0; i < numChildren; ++i) 
        {
        	final Node child = current.children.get(i);
        	final double exploit = child.scoreSums[mover] / child.visitCount;
        	final double explore = explorationConstant * Math.sqrt(twoParentLog / child.visitCount);
        
            final double ucb1Value = exploit + explore;
            
            if (ucb1Value > bestValue)
            {
                bestValue = ucb1Value;
                bestChild = child;
                numBestFound = 1;
            }
            else if 
            (
            	ucb1Value == bestValue && 
            	ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
            	// this case implements random tie-breaking
            	bestChild = child;
            }
        }
        
        return bestChild;
	}
	
	/**
	 * Selects best move based on the highest exploit value
	 * 
	 * @param rootNode
	 * @return
	 */
	public static Move finalMoveSelection(final Node rootNode)
	{
		Node bestChild = null;
        double bestExploit = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;
        
        final int numChildren = rootNode.children.size();
		final int mover = rootNode.context.state().mover();
        for (int i = 0; i < numChildren; ++i) 
        {
        	final Node child = rootNode.children.get(i);
        	final double exploit = child.scoreSums[mover] / child.visitCount;
            
            if (exploit > bestExploit)
            {
                bestExploit = exploit;
                bestChild = child;
                numBestFound = 1;
            }
            else if 
            (
            	exploit == bestExploit && 
            	ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
            	// this case implements random tie-breaking
            	bestChild = child;
            }
        }
        
        return bestChild.moveFromParent;
	}
	
	@Override
	public void initAI(final Game game, final int playerID)
	{
		this.player = playerID;
	}
	
	@Override
	public boolean supportsGame(final Game game)
	{
		if (game.isStochasticGame())
			return false;
		
		if (!game.isAlternatingMoveGame())
			return false;
		
		return true;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Inner class for nodes used by example UCT
	 * 
	 * @author Dennis Soemers
	 */
	private static class Node
	{
		/** Our parent node */
		protected final Node parent;
		
		/** The move that led from parent to this node */
		protected final Move moveFromParent;
		
		/** This objects contains the game state for this node (this is why we don't support stochastic games) */
		protected final Context context;
		
		/** Visit count for this node */
		protected int visitCount = 0;
		
		/** For every player, sum of utilities / scores backpropagated through this node */
		protected final double[] scoreSums;
		
		/** Child nodes */
		protected List<Node> children = new ArrayList<Node>();
		
		/** List of moves for which we did not yet create a child node */
		protected final FastArrayList<Move> unexpandedMoves;
		
		/**
		 * Constructor
		 * 
		 * @param parent
		 * @param moveFromParent
		 * @param context
		 */
		public Node(final Node parent, final Move moveFromParent, final Context context)
		{
			this.parent = parent;
			this.moveFromParent = moveFromParent;
			this.context = context;
			final Game game = context.game();
			scoreSums = new double[game.players().count() + 1];
			
			// For simplicity, we just take ALL legal moves. 
			// This means we do not support simultaneous-move games.
			unexpandedMoves = new FastArrayList<Move>(game.moves(context).moves());
			
			if (parent != null)
				parent.children.add(this);
		}
		
	}
	
	//-------------------------------------------------------------------------

}
