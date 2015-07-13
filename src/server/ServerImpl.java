package server;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import client.Client;

/**
 * @author Srinivasan Venkatraman / Ashwini Ravi
 *
 */
public class ServerImpl implements Server, Runnable, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3185081637139863696L;	

	private Map<String, String> userInfoMap; // stores the user name and password for all the players
	
	private Map<String,Client> playersMap; // stores the user details for the players currently playing the game
	
	private int timeToWait = 20; // time for the server to wait before the game to begin
	
	private int gridSize; // size of the square grid 
	
	private int mazeCount; // No of mazes
	
	private GameStats gameStats;
	
	public ServerImpl() throws RemoteException{
		this.userInfoMap = new ConcurrentHashMap<String, String>();		
		this.playersMap = new ConcurrentHashMap<String, Client>();
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see server.Server#sayHello()
	 */
	@Override
	public String sayHello() throws RemoteException {
		return "Hello";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see server.Server#checkAvailableUserName(java.lang.String)
	 */
	@Override
	public boolean checkAvailableUserName(String userName) {
		boolean isAvailable = true;
		if (this.userInfoMap.containsKey(userName)) {
			System.out.println("Map Values are" + this.userInfoMap);
			isAvailable = false;
		}
		return isAvailable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see server.Server#addUser(java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized void addUser(String userName, String password) {
		if (null != userName && null != password) {
			this.userInfoMap.put(userName, password);
			System.out.println("Map after insert" + this.userInfoMap);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see server.Server#validateUser(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean validateUser(String userName, String password) {
		boolean isValidUser = false;
		if (null != userName && null != password
				&& this.userInfoMap.containsKey(userName)
				&& password.equals(this.userInfoMap.get(userName))) {
			isValidUser = true;
		}

		return isValidUser;
	}

	public static void main(String args[]) {
		
		Server serverStub = null;
		Registry registry = null;
		
		try {
			ServerImpl serverObject = new ServerImpl();
			
			String gridsize = "5";
			do{
				gridsize = JOptionPane.showInputDialog(null, "Enter the No of rows and columns in the grid", "INPUT", JOptionPane.OK_OPTION);
				
			}while(StringUtils.isEmpty(gridsize));
			
			String mazeCount = "5";
			do{
				mazeCount = JOptionPane.showInputDialog(null, "Enter the No MAZES", "INPUT", JOptionPane.OK_OPTION);
				
			}while(StringUtils.isEmpty(mazeCount));
			
			serverObject.gridSize = Integer.parseInt(gridsize);
			serverObject.mazeCount = Integer.parseInt(mazeCount);
			
			serverStub = (Server) UnicastRemoteObject.exportObject(serverObject, 0);
			registry = LocateRegistry.createRegistry(1234);
			registry.bind("mazeGame", serverStub);
			System.out.println("Server Up and Running!!!!");
			//serverObject.gridSize = 5;
		} catch (Exception e) {
			try {
				e.printStackTrace();
				registry.unbind("mazeGame");
				registry.bind("mazeGame",serverStub);
				System.err.println("Server ready");
			} catch (Exception ee) {
				System.err.println("Server exception: " + ee.toString());
				ee.printStackTrace();
			}
		}

	}

	@Override
	public synchronized boolean addUserToGame(String userID, Client clientImpl) throws RemoteException {
		if(this.playersMap.containsKey(userID)){
			return false;
		}/*else{
			this.playersMap.put(userID, clientImpl);
			return true;
		}*/
		return true;
	}

	@Override
	public synchronized boolean joinUserToGame(String userID, Client clientImpl)
			throws RemoteException {
		if(this.timeToWait == 20){
			Thread t = new Thread(this);
			t.start();
		}
		if(this.timeToWait == 0){
			return false;
		}
		this.playersMap.put(userID, clientImpl);
		return true;
	}

	@Override
	public void run() {
		int timeToWait = this.timeToWait;
		while(timeToWait > 0){
			System.out.println("Time to wait " + timeToWait);
			try{
				timeToWait--;
				Thread.sleep(1000L);
				this.timeToWait = timeToWait;
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		System.out.println("Time to wait final value " + this.timeToWait);
		if(this.timeToWait == 0){
			// Updating the GameStats 			
			GameStats gameStats = new GameStats(this.gridSize, this.mazeCount, this.playersMap.keySet());		
			this.gameStats = gameStats;			
			
			// Notifying the User that the game has started
			Iterator<Entry<String, Client>> clientIterator = this.playersMap.entrySet().iterator();
			while (clientIterator.hasNext()) {
				Entry<String, Client> clientEntry = clientIterator.next();
				System.out.println("Players playing the game are::::"+ clientEntry.getKey());
				try {
					//clientEntry.getValue().notifyPlayer(gameStats);
					clientEntry.getValue().notifyPlayer(this.gameStats);
				} catch (RemoteException e) {
					System.err.println("Player " + clientEntry.getKey()+ "has quit the game");
					System.err.print("Removing player....");
					clientIterator.remove();
					this.gameStats.getPlayersToPositionsMap().remove(clientEntry.getKey());
					this.gameStats.getPlayerToScoreMap().remove(clientEntry.getKey());
					e.printStackTrace();
				}
			}
			
			// Changes for handling the game and player crash
			while(!this.gameStats.hasGameEnded){
				try{
					Thread.sleep(5000L);
					Iterator<Entry<String, Client>> clients = this.playersMap.entrySet().iterator();
					while (clients.hasNext()) {
						Entry<String, Client> clientEntry = clients.next();
						System.out.println("Players playing the game are::::" + clientEntry.getKey());
						try {
							clientEntry.getValue().isAlive();
						} catch (RemoteException e) {
							System.err.println("Player " + clientEntry.getKey()+ "has quit the game");
							System.err.print("Removing player....");
							clients.remove();
							this.gameStats.getPlayersToPositionsMap().remove(clientEntry.getKey());
							this.gameStats.getPlayerToScoreMap().remove(clientEntry.getKey());
							if(this.gameStats.getPlayersToPositionsMap().size() <= 1 || this.gameStats.getPlayerToScoreMap().size() <= 1){
								this.gameStats.hasGameEnded = true;
							}
						}
					}
					
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
			
			if(playersMap.size() == 0){
				JOptionPane.showMessageDialog(null, null, "Sorry!!!!! The Game has only One Player left. Game will be terminated!!!!",JOptionPane.WARNING_MESSAGE);		
			}else{
				for(String clientID : this.playersMap.keySet()){
					Client client = (Client) this.playersMap.get(clientID);
					try {
						client.notifyEnd();
					} catch (RemoteException e) {
						System.err.println("Game has ended.. System will be refreshed for the new game to begin...");
					}
				}
			}			
			JOptionPane.showMessageDialog(null, "GAME ENDED!!! Recycling the Server!!!!",  "MAZE GAME", JOptionPane.INFORMATION_MESSAGE);
			this.playersMap.clear();
			this.userInfoMap.clear();
			this.gameStats = null;
			this.timeToWait = 20;
			
		}
		
	}

	@Override
	public int getGridSize() throws RemoteException {
		return this.gridSize;
	}
	
	/* (non-Javadoc)
	 * @see server.Server#moveUser(java.lang.String, server.CoordinatesUtil)
	 */
	public synchronized GameStats moveUser(String userID, CoordinatesUtil coordinatesUtil)  {
		// Check for the user and get the current position of the user
		if(this.gameStats.getPlayersToPositionsMap().containsKey(userID)){
			// update the position of the user
			CoordinatesUtil currentPosition = this.gameStats.getPlayersToPositionsMap().get(userID);
			this.gameStats.getGameElement()[currentPosition.getColumnCoordinate()][currentPosition.getColumnCoordinate()] = GameElements.EMPTY;
			this.gameStats.getGameElement()[coordinatesUtil.getRowCoordinate()][coordinatesUtil.getColumnCoordinate()] = GameElements.PLAYER;
			
			// update the position of player in the map
			this.gameStats.getPlayersToPositionsMap().put(userID, coordinatesUtil);	
			// check for available treasure at the current coordinate
			if(this.gameStats.getTreasurePositionInfoMap().containsKey(coordinatesUtil)){
				// get the number of treasures at the coordinate
				int treasureCount = this.gameStats.getTreasurePositionInfoMap().get(coordinatesUtil);
				if(this.gameStats.getPlayerToScoreMap().containsKey(userID)){
					// get the player's current score
					int playerScore = this.gameStats.getPlayerToScoreMap().get(userID);
					// update the score for the player
					this.gameStats.getPlayerToScoreMap().put(userID, playerScore+treasureCount);					
				}
				// remove the coordinate from the map after treasure retrieved
				this.gameStats.getTreasurePositionInfoMap().remove(coordinatesUtil);	
				
				if(gameStats.getTreasurePositionInfoMap().size() == 0){
					gameStats.hasGameEnded = true;
				}
			}
		}
		return this.gameStats;
	}
}
