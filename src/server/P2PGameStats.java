package server;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import client.P2PClient;

public class P2PGameStats extends GameStats implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -805106832151434940L;
	
	private String primaryServerID;
	private String backupServerID;
	private P2PClient primaryServer;
	private P2PClient backupServer;
	
	private Map<String,P2PClient> playersMap = null;
	private List<String> playersList = null;
	
	public P2PGameStats(int N, int M, Map<String,P2PClient> playersMap,List<String> playersList) {
		super(N, M, playersMap.keySet());
		this.playersMap = playersMap;
		this.playersList = playersList;
		this.primaryServerID = (String)playersList.get(0);
		this.backupServerID = (String)playersList.get(1);
		this.primaryServer = playersMap.get(primaryServerID);
		this.backupServer = playersMap.get(backupServerID);
		
		System.out.println("Primary Server ID: " + this.primaryServerID);
		System.out.println("Backup Server ID: " + this.backupServerID);  
	}

	public String getPrimaryServerID() {
		return primaryServerID;
	}

	public void setPrimaryServerID(String primaryServerID) {
		this.primaryServerID = primaryServerID;
	}

	public String getBackupServerID() {
		return backupServerID;
	}

	public void setBackupServerID(String backupServerID) {
		this.backupServerID = backupServerID;
	}

	public P2PClient getPrimaryServer() {
		return primaryServer;
	}

	public void setPrimaryServer(P2PClient primaryServer) {
		this.primaryServer = primaryServer;
	}

	public P2PClient getBackupServer() {
		return backupServer;
	}

	public void setBackupServer(P2PClient backupServer) {
		this.backupServer = backupServer;
	}

	public Map<String, P2PClient> getPlayersMap() {
		return playersMap;
	}

	public void setPlayersMap(Map<String, P2PClient> playersMap) {
		this.playersMap = playersMap;
	}

	public List<String> getPlayersList() {
		return playersList;
	}

	public void setPlayersList(List<String> playersList) {
		this.playersList = playersList;
	}

	public P2PGameStats move(String userID,CoordinatesUtil coordinatesUtil){
		// Check for the user and get the current position of the user
		if (this.getPlayersToPositionsMap().containsKey(userID)) {
			// update the position of the user
			CoordinatesUtil currentPosition = this.getPlayersToPositionsMap().get(userID);
			this.getGameElement()[currentPosition.getColumnCoordinate()][currentPosition.getColumnCoordinate()] = GameElements.EMPTY;
			this.getGameElement()[coordinatesUtil.getRowCoordinate()][coordinatesUtil.getColumnCoordinate()] = GameElements.PLAYER;

			// update the position of player in the map
			this.getPlayersToPositionsMap().put(userID, coordinatesUtil);
			// check for available treasure at the current coordinate
			if (this.getTreasurePositionInfoMap().containsKey(coordinatesUtil)) {
				// get the number of treasures at the coordinate
				int treasureCount = this.getTreasurePositionInfoMap().get(coordinatesUtil);
				if (this.getPlayerToScoreMap().containsKey(userID)) {
					// get the player's current score
					int playerScore = this.getPlayerToScoreMap().get(userID);
					// update the score for the player
					this.getPlayerToScoreMap().put(userID, playerScore + treasureCount);
				}
				// remove the coordinate from the map after treasure retrieved
				this.getTreasurePositionInfoMap().remove(coordinatesUtil);

				if (this.getTreasurePositionInfoMap().size() == 0) {
					this.hasGameEnded = true;
				}
			}
		}
		return this;
	}

}
