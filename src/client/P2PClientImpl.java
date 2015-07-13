package client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import server.CoordinatesUtil;
import server.GameStats;
import server.P2PGameStats;

public class P2PClientImpl extends UnicastRemoteObject implements P2PClient,ActionListener,Runnable {	
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 3151942810877180510L;
	private Map<String, String> userInfoMap; // stores the user name and password for all the players	
	private Map<String,P2PClient> playersMap; // stores the user details for the players currently playing the game	
	private int timeToWait = -1; 	
	private int gridSize; // size of the square grid 	
	private int mazeCount; // No of mazes	
	private static boolean isFirstPlayer;
	private boolean isPrimaryServer;
	private boolean isBackupServer;
	
	public P2PClient stub = null;
	public JFrame mainFrame = new JFrame("MAZE GAME");
	public JPanel panel = new JPanel();
	public JLabel welcomeText = new JLabel("Welcome to the MAZE GAME.  Enter your user name and password to play.",JLabel.LEFT);
	public JLabel userName = new JLabel("USER NAME:",JLabel.LEFT);
	public JLabel password = new JLabel("PASSWORD",JLabel.LEFT);
	public JTextField userNameField = new JTextField();
	public JPasswordField passwordField = new JPasswordField();
	public JButton logIn = new JButton();
	public JButton signUp = new JButton();
	public String userNameText = null;
	public String passwordText = null;
	public JFrame frame = new JFrame("MAZE GAME");
	public JPanel gamePanel = new JPanel();
	public P2PClient p2pClient = null;	
	
	public String userID = null;
	public boolean hasGameStarted;
	private boolean playerJoined;
	public P2PGameStats gameStats;	
	private List<String> playersList;

	protected P2PClientImpl() throws RemoteException {
		super();		
		initLoginFrame();
		this.hasGameStarted = false;
	}

	
	
	@Override
	public void notifyPlayer(P2PGameStats gameStats) throws RemoteException {
		this.hasGameStarted = true;
		this.gameStats = gameStats;
		
		if(StringUtils.equals(gameStats.getPrimaryServerID(), this.userID)){
			this.isPrimaryServer = true;
			this.isBackupServer = false;
		}else if(StringUtils.equals(gameStats.getBackupServerID(), this.userID)){
			this.isBackupServer = true;
			this.isPrimaryServer = false;
		}
		
		if(isBackupServer){
			System.out.println("Back Up Server: " + this.userID + " starting the thread");
			this.timeToWait = -2;
			Thread t = new Thread(this);			
			t.start();
		}
	}

	@Override
	public void notifyEnd() throws RemoteException {
		JOptionPane.showMessageDialog(panel, "GAME HAS ENDED..." , "MAZE GAME", JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);

	}

	@Override
	public boolean validateUser(String userName, String password)throws RemoteException {
		boolean isValidUser = false;
		if (null != userName && null != password 
				&& this.userInfoMap.containsKey(userName)
				&& password.equals(this.userInfoMap.get(userName))) {
			isValidUser = true;
		}
		return isValidUser;
	}

	@Override
	public boolean checkAvailableUserName(String userName)throws RemoteException {
		boolean isAvailable = true;
		if (this.userInfoMap.containsKey(userName)) {			
			isAvailable = false;
		}
		return isAvailable;
	}

	@Override
	public void addUser(String userName, String password)throws RemoteException {
		if (null != userName && null != password) {
			this.userInfoMap.put(userName, password);		
		}
	}

	@Override
	public boolean addUserToGame(String userID, P2PClient clientImpl)throws RemoteException {
		if(this.playersMap.containsKey(userID)){
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean joinUserToGame(String userID, P2PClient clientImpl)throws RemoteException {
		
		// Start the timer after the first player has joined
		if(this.timeToWait == -1 && !this.hasGameStarted){			
			this.timeToWait = 20;
		}
		
		if(this.timeToWait == 20){
			Thread t = new Thread(this);
			t.start();
		}
		
		// For player joining after 20s, return not eligible to join
		if(this.timeToWait == 0 || this.timeToWait ==-1 || this.timeToWait ==-2){
			return false;
		}
		
		// Add the player to the player map if joined the game
		this.playersMap.put(userID, clientImpl);
		this.playersList.add(userID);
		return true;
	}

	@Override
	public synchronized P2PGameStats moveUser(String userID, CoordinatesUtil coordinatesUtil) throws RemoteException{		
		System.out.println("SERVER: " + this.userID + " handling the request from the user:" + userID);
		if(this.gameStats.hasGameEnded){
			return this.gameStats;
		}
		
		if (StringUtils.equals(this.gameStats.getBackupServerID(), this.userID)) {
			isBackupServer = true;
			isPrimaryServer = false;
		}
		if (StringUtils.equals(this.gameStats.getPrimaryServerID(), this.userID)) {
			isBackupServer = false;
			isPrimaryServer = true;
		}		
		this.gameStats = this.gameStats.move(userID,coordinatesUtil);		
		System.out.println("Updating the Backup Server " + this.gameStats.getBackupServerID());
		try{			
			this.gameStats.getBackupServer().update(this.gameStats);			
		}catch (Exception e) {
			System.err.println("Backup Server " + this.gameStats.getBackupServerID() + " has crashed... Creating a new backup server and updating the status....");
			createAndUpdateNewBackupServer(this.gameStats);
		}		
		return this.gameStats;	
	}
	
	
	public static void main(String []args) throws NotBoundException, AlreadyBoundException, RemoteException{
		
		P2PClient p2pClient = null;
		Registry registry = null;		
		String ipAddress = "localhost";
		int portNumber = 1099;
		String urlAddress = "rmi://" + ipAddress + ":" + portNumber + "/mazeGame" ;
		
		// The argument for main is as follows 
		// first player : java P2PClientImpl host [ip] [port]
		// for others :   java P2PClientImpl player [ip] [port]		
		if(args.length < 2){
			System.err.println("Error in the arguments... Please follow the below syntax and re-run...");
			System.err.println("USAGE - first player : java P2PClientImpl host [ip] [port]");
			System.err.println("USAGE - for other players :   java P2PClientImpl player [ip] [port]");
		}
		
		// identify if the player is the host using the first parameter	
		if(args.length > 0){
			if(StringUtils.equals("host", args[0])){
				isFirstPlayer = true;
			}
			
			// Get the IP Address from the User through Command Line
			if(StringUtils.isNotBlank(args[1])){
				ipAddress = args[1];
			}
			
			// Get the port number from the User through Command Line
			if(StringUtils.isNotBlank(args[2])){
				try{
					portNumber = Integer.parseInt(args[2]);
				}catch(NumberFormatException ex){
					portNumber = 1099;
				}
				
			}
		}		
		
		urlAddress = "rmi://" + ipAddress + ":" + portNumber + "/mazeGame" ;
		// If first player, create and start the rmiregistry
		if(isFirstPlayer){
			try {
				p2pClient = new P2PClientImpl();				
				registry = LocateRegistry.createRegistry(portNumber);				
				try {
					registry.bind(urlAddress, p2pClient);
				} catch (AlreadyBoundException e) {
					registry.unbind(urlAddress);
					registry.bind(urlAddress, p2pClient);
					e.printStackTrace();
				}
				System.out.println("Fist Player is ready now... Other players can join....");
				
				// Get the size of the maze grid from the user
				String gridsize = "5";
				do{
					gridsize = JOptionPane.showInputDialog(null, "Enter the No of rows and columns in the grid", "INPUT", JOptionPane.OK_OPTION);
					
				}while(StringUtils.isEmpty(gridsize));
				
				// Get the number of mazes to be placed in the grid from the user
				String mazeCount = "10";
				do{
					mazeCount = JOptionPane.showInputDialog(null, "Enter the No MAZES", "INPUT", JOptionPane.OK_OPTION);
					System.out.println("Maze count is " + mazeCount);
				}while(StringUtils.isEmpty(mazeCount));	
				
				try{
					((P2PClientImpl)p2pClient).gridSize = Integer.parseInt(gridsize);
				}catch(NumberFormatException exe){
					((P2PClientImpl)p2pClient).gridSize = 5;
				}
				
				try{
					((P2PClientImpl)p2pClient).mazeCount = Integer.parseInt(mazeCount);		
					System.out.println("Maze count is " + ((P2PClientImpl)p2pClient).mazeCount);
				}catch(NumberFormatException e){
					((P2PClientImpl)p2pClient).mazeCount = 10;
				}
				
				((P2PClientImpl)p2pClient).userInfoMap = new ConcurrentHashMap<String, String>();		
				((P2PClientImpl)p2pClient).playersMap = new ConcurrentHashMap<String, P2PClient>();
				((P2PClientImpl)p2pClient).playersList = new ArrayList<String>();
				
				
			} catch (RemoteException e) {				
				e.printStackTrace();
			}
		}
		
		// for all the players who play the game		
		if(p2pClient == null){
			try {
				p2pClient = new P2PClientImpl();
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}		
		
		((P2PClientImpl)p2pClient).initServerProperties(portNumber,urlAddress);
		
		
		while(!((P2PClientImpl)p2pClient).playerJoined){				
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		while(!((P2PClientImpl)p2pClient).hasGameStarted){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		
		if(((P2PClientImpl)p2pClient).hasGameStarted){
			((P2PClientImpl)p2pClient).gridSize = ((P2PClientImpl)p2pClient).stub.getGridSize();
			((P2PClientImpl)p2pClient).paintMaze(((P2PClientImpl)p2pClient).gameStats);
			((P2PClientImpl)p2pClient).paintPlayerTable(((P2PClientImpl)p2pClient).gameStats);
			((P2PClientImpl)p2pClient).paintTreasureTable(((P2PClientImpl)p2pClient).gameStats);
		}
		
	}

	@Override
	public void run() {		
		// If first player, make him the primary server
		if(isFirstPlayer){			
			this.isPrimaryServer = true;
			System.out.println("Primary Server is: " + this.userID);
		}
		
		int timeToWait = this.timeToWait;		
		//Wait for other players to join		
		if(timeToWait == -1){
			System.out.println("Waiting for Players to join the Game.....");
			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		
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
		if(this.timeToWait == 0){
			System.out.println("Players have joined the game.... Get Ready.....Starting the game.... ");
			// Updating the GameStats 
			// There should be at least two players to start the game
			if(this.playersMap.size() > 1){				
				P2PGameStats gameStats = new P2PGameStats(this.gridSize, this.mazeCount, this.playersMap,this.playersList);		
				this.gameStats = gameStats;	
			}else{
				System.err.println("There should be atleast two players to play the game.......");
				JOptionPane.showMessageDialog(panel, "Only One Player in the Game... Exiting the Game....", "MAZE GAME", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
					
			
			// Notifying the User that the game has started
			Iterator<Entry<String, P2PClient>> clientIterator = this.playersMap.entrySet().iterator();
			while (clientIterator.hasNext()) {
				Entry<String, P2PClient> clientEntry = clientIterator.next();
				System.out.println("Players playing the game : "+ clientEntry.getKey());
				try {					
					clientEntry.getValue().notifyPlayer(this.gameStats);					
				} catch (RemoteException e) {
					System.err.println("Player " + clientEntry.getKey()+ " has quit the game");
					System.err.println("Removing player....");
					clientIterator.remove();
					this.gameStats.getPlayersToPositionsMap().remove(clientEntry.getKey());
					this.gameStats.getPlayerToScoreMap().remove(clientEntry.getKey());
					e.printStackTrace();
				}
			}			
		}
		this.timeToWait = -2;
		// Changes for handling the game and player crash			
			while(isPrimaryServer && !this.gameStats.hasGameEnded){
				try{
					Thread.sleep(5000L);
					Iterator<Entry<String, P2PClient>> clients = this.gameStats.getPlayersMap().entrySet().iterator();								
					while (clients.hasNext()) {
						Entry<String, P2PClient> clientEntry = clients.next();
						System.out.println("Players playing the game are::::" + clientEntry.getKey());
						try {
							clientEntry.getValue().isAlive();							
						} catch (RemoteException e) {
							System.err.println("Player " + clientEntry.getKey()+ " has quit the game");
							System.err.println("Removing player....");
							clients.remove();
							this.gameStats.getPlayersList().remove(clientEntry.getKey());							
							this.gameStats.getPlayersToPositionsMap().remove(clientEntry.getKey());
							this.gameStats.getPlayerToScoreMap().remove(clientEntry.getKey());
							// Updating the backup server after removing						
							try {
								this.gameStats.getBackupServer().update(this.gameStats);
							} catch (RemoteException e1) {
								System.err.println("Back up server crashed..");
								System.err.println("Backup Server " + this.gameStats.getBackupServerID() + " has crashed... Creating a new backup server and updating the status....");
								createAndUpdateNewBackupServer(this.gameStats);								
							}
							if(this.gameStats.getPlayersToPositionsMap().size() <= 1 || this.gameStats.getPlayerToScoreMap().size() <= 1){
								this.gameStats.hasGameEnded = true;
							}
						}
					}
					
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}		
			while(isBackupServer && !this.gameStats.hasGameEnded){
				try{
					Thread.sleep(5000L);
					//System.out.println("Polling on the Primary server......" + this.gameStats.getPrimaryServerID());
					this.gameStats.getPrimaryServer().isAlive();
				}catch(RemoteException ex){					
					System.err.println("Primary Server: " + this.gameStats.getPrimaryServerID() + " has crashed...");							
					try{
						System.out.println("Updating the Backup Server: " + this.gameStats.getBackupServerID() + " to Primary Server..");
						this.gameStats.getBackupServer().updateToPrimaryServer(this.gameStats);
						isBackupServer =false;
						isPrimaryServer = true;						
						System.out.println("Updated Primary Server :"+ this.gameStats.getPrimaryServerID());
						System.out.println("Updated Backup Server  :"+ this.gameStats.getBackupServerID());	
						Thread.currentThread().interrupt();
						startThread(this.gameStats.getPrimaryServer());						
						break;						
					}catch(Exception exc){
						System.err.println("Primary and Backup servers have crashed...");						
					}					
				}catch(InterruptedException exe){
					exe.printStackTrace();
				}
			}
		
		if(this.gameStats.hasGameEnded && this.gameStats.getPlayersMap() != null && this.gameStats.getPlayersMap().size() > 0){
			for(String key : this.gameStats.getPlayersMap().keySet()){
				try {
					(this.gameStats.getPlayersMap().get(key)).notifyEnd();
				} catch (RemoteException e) {
					System.err.println("Error in notifying the player...");
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		try{
			JLabel errorLabel = new JLabel();
			errorLabel.setName("errorLabel");
			errorLabel.setBounds(20, 146, 500, 100);
			errorLabel.setForeground(Color.RED);		
			
			for(Component component : panel.getComponents()){
				if(StringUtils.equals("errorLabel", component.getName()) && component.isVisible()){
					component.setVisible(false);
					panel.remove(component);
					panel.repaint();
				}
			}
			if(actionEvent.getSource() instanceof JButton){
				JButton button = (JButton) actionEvent.getSource();			
				String userName = this.userNameField.getText();
				StringBuilder passwordBuilder = new StringBuilder();
				for(char c : this.passwordField.getPassword()){				
					passwordBuilder.append(c);
				}
				
				String password = passwordBuilder.toString();			
				if(StringUtils.isBlank(userName) || StringUtils.isBlank(password)){				
					errorLabel.setText("<html> Kindly enter your User Name and Password to Proceed.<br> If you are a new user, kindly Sign Up before you Login!!</html>");
					errorLabel.setVisible(true);				
					panel.add(errorLabel);
					panel.repaint();
				}else{
					// when the login button is clicked, this method is invoked
					if("login".equals(button.getActionCommand())){
						try {
							if(stub.validateUser(userName, password)){
								System.out.println("Login Successful");
								this.userID = this.userNameField.getText();
								for(Component component : panel.getComponents()){
									panel.remove(component);
								}
								JLabel welcomeUser = new JLabel();
								welcomeUser.setText("Welcome " + this.userID +  "," );
								welcomeUser.setVisible(true);
								welcomeUser.setBounds(0, 0, 500, 15);
								panel.add(welcomeUser);
								panel.repaint();
								
								if(!stub.addUserToGame(this.userID,this)){
									JOptionPane.showMessageDialog(panel, "You are already logged in.Log out from the other window.", "ERROR", JOptionPane.ERROR_MESSAGE);
								}else{
									if(stub.joinUserToGame(this.userID, this)){
										JOptionPane.showMessageDialog(panel, "Congratulations!! You are successfully added to the game", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
										this.playerJoined = true;
				
									}else{
										JOptionPane.showMessageDialog(panel, "Sorry!!The game has already begun. You are not added to the game", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
										System.exit(0);
									}
								}
								
							}else{
								System.out.println("Login unsuccessful");
								errorLabel.setText("User Name and Password did not match. Try again!!");
								errorLabel.setVisible(true);
								panel.add(errorLabel);
								panel.repaint();
							}
						} catch (RemoteException e) {
							System.err.println("Exception while validating the User");
						}				
					}else if("signup".equals(button.getActionCommand())){
						try {
							if(stub.checkAvailableUserName(userName)){
								System.out.println("This is a new User Name !!!!!!!!!");
								stub.addUser(userName, password);
								JOptionPane.showMessageDialog(panel, "Sign Up Successful!! Login to continue...", "SIGNUP", JOptionPane.INFORMATION_MESSAGE);
								
							}else{
								System.err.println("User Name already exists");
								errorLabel.setText("User Name already exists. Try a different one!!");
								errorLabel.setVisible(true);
								panel.add(errorLabel);
								panel.repaint();
							}
						} catch (RemoteException e) {
							System.err.println("Exception while adding the User");
						}	
					}else if("playGame".equals(button.getActionCommand())){											
						String buttonName = button.getName();
						String []coordinateArray = buttonName.split("-");
						int rowCoordinate = Integer.parseInt(coordinateArray[0]);
						int columnCoordinate = Integer.parseInt(coordinateArray[1]);
						CoordinatesUtil newCoordinate = new CoordinatesUtil(rowCoordinate, columnCoordinate);
						try {
							
							if(this.gameStats != null && !this.gameStats.hasGameEnded){
								System.out.println("PLAYER : " + this.userID + " making the move");
								System.out.println("Primary Server: " + this.gameStats.getPrimaryServerID());
								System.out.println("Backup Server: "+ this.gameStats.getBackupServerID());
								System.out.println("Sending the Request to the Primary Server: " + this.gameStats.getPrimaryServerID());
								try{									
									P2PGameStats gameStats = this.gameStats.getPrimaryServer().moveUser(this.userID,newCoordinate);
									this.gameStats = gameStats;
								}catch(RemoteException e){
									System.err.println("Primary Server: " + this.gameStats.getPrimaryServerID() + " has crashed....");																		
									try{
										System.out.println("Updating the Backup Server: " + this.gameStats.getBackupServerID() + " to Primary Server..");
										this.gameStats.getBackupServer().updateToPrimaryServer(this.gameStats);
										System.out.println("PLAYER: " + this.userID);
										System.out.println("Updated Primary Server: "+ this.gameStats.getPrimaryServerID());
										System.out.println("Updated Backup Server: "+ this.gameStats.getBackupServerID());									
									}catch(Exception ex){
										JOptionPane.showMessageDialog(panel, "ERROR!!!!!", "MAZE GAME", JOptionPane.ERROR_MESSAGE);
										System.err.println("Primary and Backup servers have crashed...");	
										System.exit(0);
									}
									
									try{
										System.out.println("Sending Request to Server: " + this.gameStats.getPrimaryServerID());
										P2PGameStats gameStats = this.gameStats.getPrimaryServer().moveUser(this.userID,newCoordinate);
										this.gameStats = gameStats;											
									}catch(RemoteException ex){										
										try{
											P2PGameStats gameStats = this.gameStats.getBackupServer().moveUser(this.userID,newCoordinate);
											this.gameStats = gameStats;	
										}catch(Exception exception){
											JOptionPane.showMessageDialog(panel, "ERROR!!!!!", "MAZE GAME", JOptionPane.ERROR_MESSAGE);											
											System.exit(0);
										}
																				
									}																	
								}
								if(this.gameStats.hasGameEnded == true || this.gameStats.getPlayersList().size() <= 1 || this.gameStats.getTreasurePositionInfoMap().size() == 0){									
									JOptionPane.showMessageDialog(panel, "GAME HAS ENDED", "MAZE GAME", JOptionPane.WARNING_MESSAGE);
									System.exit(0);
								}					
								panel.invalidate();		
								if(panel != null && panel.getComponentCount() > 0 && panel.getComponents() != null){
									for(Component c : panel.getComponents()){
										if(c!= null){
											panel.remove(c);
										}										
									}
									JLabel welcomeUser = new JLabel();
									welcomeUser.setText("Welcome " + this.userID +  "," );
									welcomeUser.setVisible(true);
									welcomeUser.setBounds(0, 0, 500, 15);
									panel.add(welcomeUser);
									panel.repaint();
									paintPlayerTable(this.gameStats);
									paintTreasureTable(this.gameStats);
									paintMaze(this.gameStats);
								}
							}else{
								JOptionPane.showMessageDialog(panel, "GAME HAS ENDED", "MAZE GAME", JOptionPane.WARNING_MESSAGE);
								System.exit(0);
							}									
						} catch (Exception e) {
							e.printStackTrace();						
						}						
					}
				}		
			}
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Some exception in action performed");
		}		
	}
	
public void initLoginFrame(){		
		mainFrame.setVisible(true);
		mainFrame.setSize((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth(), (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		panel.setLayout(null);
		panel.setName("Main Panel");
		panel.setVisible(true);
		panel.setBounds(0, 0, (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth(), (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		panel.setBackground(Color.WHITE);
		mainFrame.getContentPane().add(panel);
						
		welcomeText.setLocation(0, 5);
		welcomeText.setSize(500, 20);
		welcomeText.setVisible(true);
		panel.add(welcomeText);
		
		userName.setLocation(0, 40);
		userName.setSize(100, 20);
		userName.setVisible(true);
		panel.add(userName);		
		
		userNameField.setName("userName");
		userNameField.setEditable(true);
		userNameField.setVisible(true);
		userNameField.setBounds(150, 40, 200, 20);
		panel.add(userNameField);		
				
		password.setLocation(0, 70);  
		password.setSize(100, 20);
		password.setVisible(true);
		panel.add(password);		
		
		passwordField.setName("password");
		passwordField.setEditable(true);
		passwordField.setVisible(true);
		passwordField.setBounds(150, 70, 200, 20);
		panel.add(passwordField);		
		
		JLabel alreadyUser = new JLabel();
		alreadyUser.setText("Already User?");
		alreadyUser.setVisible(true);
		alreadyUser.setBounds(0, 125, 80, 20);
		panel.add(alreadyUser);
		
		logIn.setName("login");
		logIn.setVisible(true);
		logIn.setBounds(85, 125, 70, 20);
		logIn.setText("LOGIN");
		logIn.setActionCommand("login");
		logIn.addActionListener(this);
		panel.add(logIn);		
		
		JLabel newUser = new JLabel();
		newUser.setText("New User?");
		newUser.setVisible(true);
		newUser.setBounds(200, 125, 65, 20);
		panel.add(newUser);
		
		signUp.setName("signup");
		signUp.setVisible(true);
		signUp.setBounds(270, 125, 80, 20);
		signUp.setText("SIGNUP");
		signUp.setActionCommand("signup");
		signUp.addActionListener(this);
		panel.add(signUp);
		
		mainFrame.getContentPane().add(panel);	

	}


	private void initServerProperties(int port, String urlAddress) throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(port);
		this.stub = (P2PClient) registry.lookup(urlAddress);
	}
	
	/**
	 * This method is used to paint the player name and scores on the UI
	 * 
	 * @param gameStats
	 */
	private void paintPlayerTable(GameStats gameStats){
		try{
			Object columnNames [] = {"Player Name" , "Score"};
			Object rowData [][] = null;
			int count = 0;
			if(null != gameStats && null != gameStats.getPlayersToPositionsMap() && gameStats.getPlayersToPositionsMap().size() > 0){
				rowData = new Object[gameStats.getPlayersToPositionsMap().size()][2];
				for(String key : gameStats.getPlayersToPositionsMap().keySet()){
					rowData[count][0] = key;
					if(gameStats.getPlayerToScoreMap().containsKey(key)){
						rowData[count][1] = gameStats.getPlayerToScoreMap().get(key);
					}else{
						rowData[count][1] = 0;
					}				
					count++;
				}
			}
			
			JTable userTable = new JTable(rowData, columnNames);
			userTable.setEnabled(false);
			userTable.setForeground(Color.BLACK);			
			userTable.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 25, 300, 275);
			userTable.setVisible(true);
			userTable.setBackground(Color.LIGHT_GRAY);		
			JScrollPane pane = new JScrollPane(userTable);
			pane.setVisible(true);
			pane.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 25, 300, 275);
			panel.add(pane);
			panel.repaint();
		}catch(Exception e){
			System.err.println("Some exception in UI user table");
		}
	}
	
	/**
	 * This method is used to print the maze
	 * 
	 * @param gameStats
	 */
	private void paintMaze(GameStats gameStats) {	
		try{
			JRootPane rootPane = new JRootPane();
			rootPane.setBackground(Color.WHITE);			
			rootPane.setBounds(0, 10, (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5) , (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
			rootPane.setVisible(true);		
			
			GridBagLayout layout = new GridBagLayout();			
			GridBagConstraints gc = new GridBagConstraints();
			gc.fill = GridBagConstraints.NONE;
	        gc.weightx = 0.0;
	        gc.weighty = 0.0;
	        
			final Dimension dim = new Dimension(rootPane.getWidth()/this.gridSize, (rootPane.getHeight()-170)/this.gridSize);
			rootPane.setLayout(layout);
			JButton buttonArray[][] = new JButton[this.gridSize][this.gridSize];	
			Map<String,JButton> maze = new HashMap<String,JButton>();
			
			for(int i = 0; i < this.gridSize; i++) {
	            for(int j = 0; j < this.gridSize; j++) {
	            	buttonArray[i][j] = new JButton();
	            	buttonArray[i][j].setEnabled(false);
	            	buttonArray[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
	            	buttonArray[i][j].setMaximumSize(dim);
	            	buttonArray[i][j].setMinimumSize(dim);
	            	buttonArray[i][j].setPreferredSize(dim);  
	            	buttonArray[i][j].setActionCommand("playGame");
	            	buttonArray[i][j].addActionListener(this);
	            	buttonArray[i][j].setName(j+"-"+i);
	            	buttonArray[i][j].setText(j+"-"+i);
	            	gc.gridx = i+1;
	                gc.gridy = j+1;
	                rootPane.add(buttonArray[i][j], gc);
	                maze.put(buttonArray[i][j].getName(),buttonArray[i][j]);
	            }
	        }		 
			
			if(null != gameStats && null != gameStats.getPlayersToPositionsMap()){
				for(String playerId : gameStats.getPlayersToPositionsMap().keySet()){
					CoordinatesUtil coordinates = gameStats.getPlayersToPositionsMap().get(playerId);
					String coordinate = String.valueOf(coordinates.getRowCoordinate()) + "-" + String.valueOf(coordinates.getColumnCoordinate());
					if(maze.containsKey(coordinate)){
						JButton button = maze.get(coordinate);
						button.setEnabled(false);
						button.setText(playerId);
					}
				}
			}		
			
			if(null != gameStats && null != gameStats.getPlayersToPositionsMap() && gameStats.getPlayersToPositionsMap().containsKey(this.userID)){
				CoordinatesUtil coordinates = gameStats.getPlayersToPositionsMap().get(this.userID);
				String coordinate = String.valueOf(coordinates.getRowCoordinate()) + "-" + String.valueOf(coordinates.getColumnCoordinate());
				if(maze.containsKey(coordinate)){
					JButton button = maze.get(coordinate);
					button.setForeground(Color.RED);
					button.setEnabled(true);
					
					int rowCoordinateNorth = coordinates.getRowCoordinate() - 1;
					int rowCoordinateSouth = coordinates.getRowCoordinate() + 1;
					int columnCoordinateEast = coordinates.getColumnCoordinate() + 1;
					int columnCoordinateWest = coordinates.getColumnCoordinate() - 1;
						
					String northButton = String.valueOf(rowCoordinateNorth) + "-" + String.valueOf(coordinates.getColumnCoordinate());
					//System.out.println("North Button " + northButton);
					if(maze.containsKey(northButton)){
						if(StringUtils.equals(northButton, maze.get(northButton).getText())){
							maze.get(northButton).setEnabled(true);
						}						
					}
						
					String southButton = String.valueOf(rowCoordinateSouth) + "-" + String.valueOf(coordinates.getColumnCoordinate());
					//System.out.println("South Button" + southButton);
					if(maze.containsKey(southButton)){
						if(StringUtils.equals(southButton, maze.get(southButton).getText())){
							maze.get(southButton).setEnabled(true);
						}					
					}
						
					String eastButton = coordinates.getRowCoordinate() + "-" + String.valueOf(String.valueOf(columnCoordinateEast));					
					if(maze.containsKey(eastButton)){
						if(StringUtils.equals(eastButton, maze.get(eastButton).getText())){
							maze.get(eastButton).setEnabled(true);
						}
					}
						
					String westButton = coordinates.getRowCoordinate() + "-" + String.valueOf(String.valueOf(columnCoordinateWest));					
					if(maze.containsKey(westButton)){
						if(StringUtils.equals(westButton, maze.get(westButton).getText())){
							maze.get(westButton).setEnabled(true);
						}
					}
				}
			}		
			if(panel != null && rootPane != null && panel.getComponents()!= null && panel.getComponentCount() > 0){
				panel.add(rootPane);
				panel.repaint();				
				panel.revalidate();					
			}
		}catch(Exception e){
			System.err.println("Some Exception in printing the maze");
		}			
	}
	
	/**
	 * This method is used to paint the treasure table.
	 * Coordinates where treasures are present and number of treasures at each coordinates
	 * 
	 * @param gameStats
	 */
	private void paintTreasureTable(GameStats gameStats) {
		try{
			Object columnNames [] = {"X-Coordinate" , "Y-Coordinate" , "No. of Treasures Present"};
			Object rowData [][] = {{"-","-","-"}};
			int count = 0;
			if(null != gameStats && null != gameStats.getTreasurePositionInfoMap() && gameStats.getTreasurePositionInfoMap().size() > 0){
				rowData = new Object[gameStats.getTreasurePositionInfoMap().size()][3];
				for(CoordinatesUtil coordinate : gameStats.getTreasurePositionInfoMap().keySet()){
					rowData[count][0] = coordinate.getRowCoordinate();
					rowData[count][1] = coordinate.getColumnCoordinate();
					rowData[count][2] = gameStats.getTreasurePositionInfoMap().get(coordinate);
					count++;
				}
			}
			
			JLabel treasureLabel = new JLabel();
			treasureLabel.setText("<html> HURRY!! UNCOVER THE TREASURES!!!! <br>You can find them at the below Locations :) :) </html>");
			treasureLabel.setFont(new Font("Serif", Font.BOLD, 14));
			treasureLabel.setVisible(true);
			treasureLabel.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 310, 300, 35);
			panel.add(treasureLabel);
			
			JTable treasureTable = new JTable(rowData, columnNames);
			treasureTable.setEnabled(false);
			treasureTable.setForeground(Color.BLACK);
			treasureTable.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 350, 350, 300);
			treasureTable.setVisible(true);
			treasureTable.setBackground(Color.LIGHT_GRAY);
			
			JScrollPane pane = new JScrollPane(treasureTable);
			pane.setVisible(true);
			pane.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 350, 350, 300);			
			
			panel.add(pane);
			panel.repaint();
		}catch(Exception e){
			System.err.println("Some error in UI treasure table");
		}		
	}

	@Override
	public int getGridSize() throws RemoteException {
		return this.gridSize;
	}

	public boolean isPrimaryServer() {
		return isPrimaryServer;
	}

	public boolean isBackupServer() {
		return isBackupServer;
	}

	@Override
	public void isAlive() throws RemoteException {		
	}

	public void setPrimaryServer(boolean isPrimaryServer) {
		this.isPrimaryServer = isPrimaryServer;
	}



	public void setBackupServer(boolean isBackupServer) {
		this.isBackupServer = isBackupServer;
	}



	@Override
	public synchronized void update(P2PGameStats gameStats) throws RemoteException {
		System.out.println("SERVER: " + this.userID);		
		this.gameStats.setPrimaryServer(gameStats.getPrimaryServer());
		this.gameStats.setPrimaryServerID(gameStats.getPrimaryServerID());
		this.gameStats.setBackupServer(gameStats.getBackupServer());
		this.gameStats.setBackupServerID(gameStats.getBackupServerID());
		this.gameStats.setTreasurePositionInfoMap(gameStats.getTreasurePositionInfoMap());
		this.gameStats.setPlayersToPositionsMap(gameStats.getPlayersToPositionsMap());
		this.gameStats.setPlayerToScoreMap(gameStats.getPlayerToScoreMap());
		this.gameStats.setPlayersList(gameStats.getPlayersList());
		this.gameStats.setPlayersMap(gameStats.getPlayersMap());
		this.gameStats.setGameElement(gameStats.getGameElement());
		
		if (StringUtils.equals(this.gameStats.getBackupServerID(), this.userID)) {
			isBackupServer = true;
			isPrimaryServer = false;
		}
		if (StringUtils.equals(this.gameStats.getPrimaryServerID(), this.userID)) {
			isBackupServer = false;
			isPrimaryServer = true;
		}	
	}



	@Override
	public synchronized void updateToPrimaryServer(P2PGameStats gameStats) throws RemoteException {		
		System.out.println("SERVER: " + this.userID);		
		this.gameStats.getPlayersList().remove(gameStats.getPrimaryServerID());		
		this.gameStats.getPlayersMap().remove(gameStats.getPrimaryServerID());
		this.gameStats.getPlayersToPositionsMap().remove(gameStats.getPrimaryServerID());
		this.gameStats.getPlayerToScoreMap().remove(gameStats.getPrimaryServerID());		
		this.gameStats.setPrimaryServer(gameStats.getBackupServer());
		this.gameStats.setPrimaryServerID(gameStats.getBackupServerID());
		this.isPrimaryServer = true;
		this.isBackupServer = false;
		System.out.println("Choosing a new Backup server......");		
		if(this.gameStats.getPlayersList().size() > 1){			
			this.gameStats.setBackupServerID(this.gameStats.getPlayersList().get(1));
			System.out.println("New Backup Server: " + this.gameStats.getPlayersList().get(1));
			this.gameStats.setBackupServer(this.gameStats.getPlayersMap().get(this.gameStats.getPlayersList().get(1)));
			this.gameStats.getBackupServer().updateToBackupServer(gameStats);
			this.gameStats.getBackupServer().update(this.gameStats);
			notifyPrimaryAndBackupTOClients(this.gameStats);
		}else{
			System.err.println("Only One Player Playing the game....No Backup Server Avalilable...");			
			this.gameStats.hasGameEnded = true;			
		}		
	}



	@Override
	public synchronized void updateToBackupServer(P2PGameStats gameStats) throws RemoteException {
		System.out.println("SERVER: " + this.userID);
		System.out.println("Updating the Server: " + this.userID + " to Backup Server..");
		this.gameStats.setPrimaryServer(gameStats.getPrimaryServer());
		this.gameStats.setBackupServer(gameStats.getBackupServer());
		this.gameStats.setBackupServerID(gameStats.getBackupServerID());
		this.gameStats.setPrimaryServerID(gameStats.getPrimaryServerID());
		System.out.println("Primary Server :" + this.gameStats.getPrimaryServerID());
		System.out.println("Backup Server: " + this.gameStats.getBackupServerID());
		this.isPrimaryServer = false;
		this.isBackupServer = true;
		System.out.println("isPrimaryServer? " + isPrimaryServer);
		System.out.println("isBackupServer? " + isBackupServer);		
		Thread t = new Thread(this);
		t.start();		
	}	
	
	private void startThread(P2PClient client){		
		Thread t = new Thread(this);
		t.start();	
	}
	
	private void createAndUpdateNewBackupServer(P2PGameStats gameStats){
		this.gameStats.getPlayersMap().remove(this.gameStats.getBackupServerID());
		this.gameStats.getPlayersList().remove(this.gameStats.getBackupServerID());
		this.gameStats.getPlayerToScoreMap().remove(this.gameStats.getBackupServerID());
		this.gameStats.getPlayersToPositionsMap().remove(this.gameStats.getBackupServerID());
		if(this.gameStats.getPlayersList().size() > 1){					
			String newBackupServerID = this.gameStats.getPlayersList().get(1);
			System.out.println("Server " + newBackupServerID + " is chosen as the new Backup Server");
			P2PClient newBackupServer = this.gameStats.getPlayersMap().get(newBackupServerID);					
			this.gameStats.setBackupServerID(newBackupServerID);
			this.gameStats.setBackupServer(newBackupServer);
			try {
				this.gameStats.getBackupServer().updateToBackupServer(this.gameStats);
				this.gameStats.getBackupServer().update(this.gameStats);													
			} catch (RemoteException e2) {										
				e2.printStackTrace();
			}
			notifyPrimaryAndBackupTOClients(this.gameStats);
		}else{
			System.out.println("GAME ENDED....");			
			JOptionPane.showMessageDialog(null, "No more backup servers available..","MAZE GAME",JOptionPane.WARNING_MESSAGE);
			this.gameStats.hasGameEnded = true;
			System.exit(0);
		}		
	}



	@Override
	public void notifyPrimaryAndBacupServers(String primaryServerID, P2PClient primaryServer, String backupServerID, P2PClient backupServer) throws RemoteException {
		System.out.println("Notification Received from Server " + primaryServerID);
		this.gameStats.setPrimaryServerID(primaryServerID);
		this.gameStats.setPrimaryServer(primaryServer);
		this.gameStats.setBackupServerID(backupServerID);
		this.gameStats.setBackupServer(backupServer);
		System.out.println("Updated Primary Server:" + primaryServerID);
		System.out.println("Updated Backup Server: " + backupServerID);		
	}
	
	private void notifyPrimaryAndBackupTOClients(P2PGameStats gameStats){
		System.out.println("Primary and/or Backup servers updated.....Notifying the clients......");
		if(null != this.gameStats.getPlayersMap()){
			for(String userID : this.gameStats.getPlayersMap().keySet()){
				P2PClient client = this.gameStats.getPlayersMap().get(userID);
				try {
					client.notifyPrimaryAndBacupServers(this.gameStats.getPrimaryServerID(), this.gameStats.getPrimaryServer(), this.gameStats.getBackupServerID(), this.gameStats.getBackupServer());
				} catch (RemoteException e) {
					System.err.println("Player " + userID + " has crashed...");
				}
			}
		}
	}
}
