package server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Srinivasan Venkatraman / Ashwini Ravi 
 *
 */
public class GameStats implements Serializable{
	
	/**
	 * System Generated serialVersionUID
	 */
	private static final long serialVersionUID = -4884610603316353434L;
	
	private Random random = new Random();

	/**
	 * noOfCells - No of rows/columns that constitute the grid	 * 
	 */
	private int noOfCells;
	
	/**
	 * noOfTreasures - Total Number of treasures available to be placed 	 * 
	 */	
	private int noOfTreasures;
	
	
	/**
	 * gameElement[][] - 2-D array to store the game elements - TREASURE/PLAYER/EMPTY	 * 
	 */	
	private GameElements[][] gameElement; 
	
	public boolean hasGameEnded;
	
	
	/**
	 * treasurePositionInfoMap - This map is used to store the location of the treasures and the no of treasures in a cell
	 * 
	 * Coordinates - position of treasure
	 * Integer - No of treasures in the given coordinate	 * 
	 */
	private Map<CoordinatesUtil, Integer> treasurePositionInfoMap = new TreeMap<CoordinatesUtil,Integer>(); // TreeMap is used to store the Coordinates in sorted order
	
	
	/**
	 * playersSet - Holds the players who are currently playing	 * 
	 */
	private Set<String> playersSet = new HashSet<String>();
	
	
	/**
	 * 
	 * playersToPositionsMap - contains the player and his corresponding position at a given time 
	 * 
	 */
	private Map<String, CoordinatesUtil> playersToPositionsMap = new HashMap<String, CoordinatesUtil>();
	
	private Map<String,Integer> playerToScoreMap = new HashMap<String,Integer>();
	
	public GameStats(int N,int M, Set<String> playersSet){		
		this.noOfCells = N;
		this.noOfTreasures = M;
		this.playersSet = playersSet;
		gameElement = new GameElements[this.noOfCells][this.noOfCells] ; // Game Elements to be placed in the N cross N Grid
		initializeGridToEmpty();
		placePlayers(this.playersSet);
		initializeTreasures();
		initializePlayerToScoreMap(this.playersSet);
	}

	private void initializePlayerToScoreMap(Set<String> playersSet) {
		for(String s : playersSet){
			playerToScoreMap.put(s, 0);
		}
		
	}

	/**
	 * This method is to assign a random place on the grid for each player 
	 * 
	 */
	private void placePlayers(Set<String> playersSet) {		
		for(String s : playersSet){
			
			int rowCoordinate = 0;			
			int columnCoordinate = 0;
			
			do{
				rowCoordinate = random.nextInt(this.noOfCells);
				columnCoordinate = random.nextInt(this.noOfCells);
			}while(this.gameElement[rowCoordinate][columnCoordinate] == GameElements.PLAYER);
			
			CoordinatesUtil playerCoordinate = new CoordinatesUtil(rowCoordinate, columnCoordinate);
			this.gameElement[rowCoordinate][columnCoordinate] = GameElements.PLAYER;
			this.playersToPositionsMap.put(s, playerCoordinate);
			System.out.println("The player " + s + " is placed at coordinate row - " + rowCoordinate + " , column - " + columnCoordinate);			
		}
		
	}

	/**
	 * 
	 * This method is used to initialize all the Cells in the Grid to EMPTY Elements
	 * 
	 */
	private void initializeGridToEmpty(){				
		// Initializing each of the Cells in the Grid to EMPTY Element
		for(int i=0; i<this.noOfCells; i++){
			for(int j=0; j<this.noOfCells; j++){
				this.gameElement[i][j] = GameElements.EMPTY;
			}
		}		
		System.out.println("The Cells in the Grid is filled with Empty Elements");
	}

	/**
	 * 
	 * This method is used to place the M treasures available across the randomly chosen cells
	 * Fill with TREASURE
	 * 
	 */
	private void initializeTreasures() {		
		
		// Choose Random Coordinates within the grid. Place the 
		System.out.println("No of treasures... " + this.noOfTreasures); 
		int treasuresLeft = this.noOfTreasures;
		
		int rowCoordinate = 0;
		int columnCoordinate = 0;

		for (int i = treasuresLeft; i > 0; ) {
			
			// Generate a random coordinate within the N X N grid
			do{
				rowCoordinate = random.nextInt(this.noOfCells);
				columnCoordinate = random.nextInt(this.noOfCells);
			} while(this.gameElement[rowCoordinate][columnCoordinate] == GameElements.PLAYER); // If the player is already present in the position, Generate a random coordinate again
			

			CoordinatesUtil treasureCoordinate = new CoordinatesUtil(rowCoordinate, columnCoordinate);
			this.gameElement[rowCoordinate][columnCoordinate] = GameElements.TREASURE;
			i = i - 1;

			// If a treasure is already present in the coordinate, update the increase by 1. Else, put count = 1
			if (treasurePositionInfoMap.containsKey(treasureCoordinate)) {
				treasurePositionInfoMap.put(treasureCoordinate, treasurePositionInfoMap.get(treasureCoordinate) + 1);
			} else {
				treasurePositionInfoMap.put(treasureCoordinate, 1);
			}
			System.out.println("Treasures Placed at row - " + rowCoordinate	+ " column - " + columnCoordinate);
		}

	}

	public Map<CoordinatesUtil, Integer> getTreasurePositionInfoMap() {
		return treasurePositionInfoMap;
	}

	public void setTreasurePositionInfoMap(
			Map<CoordinatesUtil, Integer> treasurePositionInfoMap) {
		this.treasurePositionInfoMap = treasurePositionInfoMap;
	}

	public Map<String, CoordinatesUtil> getPlayersToPositionsMap() {
		return playersToPositionsMap;
	}

	public void setPlayersToPositionsMap(
			Map<String, CoordinatesUtil> playersToPositionsMap) {
		this.playersToPositionsMap = playersToPositionsMap;
	}

	public Map<String, Integer> getPlayerToScoreMap() {
		return playerToScoreMap;
	}

	public void setPlayerToScoreMap(Map<String, Integer> playerToScoreMap) {
		this.playerToScoreMap = playerToScoreMap;
	}

	public GameElements[][] getGameElement() {
		return gameElement;
	}

	public void setGameElement(GameElements[][] gameElement) {
		this.gameElement = gameElement;
	}
	
	

}
