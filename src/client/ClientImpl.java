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
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

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
import server.Server;

/**
 * 
 * @author Srinivasan Venkatraman / Ashwini Ravi
 * 
 */
public class ClientImpl extends UnicastRemoteObject implements Client,ActionListener, Serializable {	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8338749616768388291L;
	
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
	
	public Registry registry;
	public Server stub = null;
	
	public String userID = null;
	public boolean hasGameStarted;
	private boolean playerJoined;
	public GameStats gameStats;
	private int gridSize; 
	
	public ClientImpl() throws RemoteException{
		initLoginFrame();
		this.hasGameStarted = false;
	}
	
	/**
	 * This method is used to initialize the login frame for the user to signup/login into the game
	 * 
	 */
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

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
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
							//GameStats gameStats = this.stub.moveUser(this.userID,this.gameStats,newCoordinate);
							if(this.gameStats != null && !this.gameStats.hasGameEnded){
								GameStats gameStats = this.stub.moveUser(this.userID,newCoordinate);								
								this.gameStats = gameStats;
								panel.invalidate();		
								if(panel != null && panel.getComponentCount() > 0 && panel.getComponents() != null){
									for(Component c : panel.getComponents()){
										if(c!= null){
											panel.remove(c);
										}										
									}
									paintPlayerTable(this.gameStats);
									paintTreasureTable(this.gameStats);
									paintMaze(this.gameStats);
								}
							}else{
								JOptionPane.showMessageDialog(null, "The Game has Ended!!!!", "MAZE GAME",JOptionPane.INFORMATION_MESSAGE);
								System.exit(0);
							}						
						} catch (RemoteException e) {							
						}
					}
				}		
			}
		}catch(Exception e){
			System.err.println("Some exception in action performed");
		}
				
	}
	
	/**
	 * The main method
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		
		try {
			ClientImpl clientImpl = new ClientImpl();			
			clientImpl.initServerProperties();
			while(!clientImpl.playerJoined){				
				Thread.sleep(100);
			}
			
			while(!clientImpl.hasGameStarted){
				Thread.sleep(100);
			}
			
			if(clientImpl.hasGameStarted){
				clientImpl.gridSize = clientImpl.stub.getGridSize();
				clientImpl.paintMaze(clientImpl.gameStats);
				clientImpl.paintPlayerTable(clientImpl.gameStats);
				clientImpl.paintTreasureTable(clientImpl.gameStats);
			}
		} catch (Exception e) {
		    System.out.println("Client exception: " + e.toString());
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
			//treasureLabel.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2)+100, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2 + 110, 500, 30);
			treasureLabel.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 310, 300, 35);
			panel.add(treasureLabel);
			
			JTable treasureTable = new JTable(rowData, columnNames);
			treasureTable.setEnabled(false);
			treasureTable.setForeground(Color.BLACK);
			//treasureTable.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2)+100, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2 + 150, 500, 300);
			treasureTable.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 350, 350, 300);
			treasureTable.setVisible(true);
			treasureTable.setBackground(Color.LIGHT_GRAY);
			
			JScrollPane pane = new JScrollPane(treasureTable);
			pane.setVisible(true);
			//pane.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2)+100, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2 + 150, 500, 300);
			pane.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5)+50, 350, 350, 300);
			
			
			panel.add(pane);
			panel.repaint();
		}catch(Exception e){
			System.err.println("Some error in UI treasure table");
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
			//rootPane.setBounds(0, 30, (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
			rootPane.setBounds(0, 10, (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.5) , (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
			rootPane.setVisible(true);		
			
			GridBagLayout layout = new GridBagLayout();	
			//layout.setConstraints(comp, constraints);
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
					System.out.println("North Button " + northButton);
					if(maze.containsKey(northButton)){
						if(StringUtils.equals(northButton, maze.get(northButton).getText())){
							maze.get(northButton).setEnabled(true);
						}
						
					}
						
					String southButton = String.valueOf(rowCoordinateSouth) + "-" + String.valueOf(coordinates.getColumnCoordinate());
					System.out.println("South Button" + southButton);
					if(maze.containsKey(southButton)){
						if(StringUtils.equals(southButton, maze.get(southButton).getText())){
							maze.get(southButton).setEnabled(true);
						}					
					}
						
					String eastButton = coordinates.getRowCoordinate() + "-" + String.valueOf(String.valueOf(columnCoordinateEast));
					System.out.println("East Button" + eastButton);
					if(maze.containsKey(eastButton)){
						if(StringUtils.equals(eastButton, maze.get(eastButton).getText())){
							maze.get(eastButton).setEnabled(true);
						}
					}
						
					String westButton = coordinates.getRowCoordinate() + "-" + String.valueOf(String.valueOf(columnCoordinateWest));
					System.out.println("West Button" + westButton);
					if(maze.containsKey(westButton)){
						if(StringUtils.equals(westButton, maze.get(westButton).getText())){
							maze.get(westButton).setEnabled(true);
						}
					}
						
					/*String northEastButton = String.valueOf(rowCoordinateNorth) + "-" + String.valueOf(columnCoordinateEast);
					System.out.println("North East Button" + northEastButton);
					if(maze.containsKey(northEastButton)){
						if(StringUtils.equals(northEastButton, maze.get(northEastButton).getText())){
							maze.get(northEastButton).setEnabled(true);
						}
					}
						
					String northWestButton = String.valueOf(rowCoordinateNorth) + "-" + String.valueOf(columnCoordinateWest);
					System.out.println("North West Button" + southButton);
					if(maze.containsKey(northWestButton)){
						if(StringUtils.equals(northWestButton, maze.get(northWestButton).getText())){
							maze.get(northWestButton).setEnabled(true);
						}
					}
						
					String southEastButton = String.valueOf(rowCoordinateSouth) + "-" + String.valueOf(columnCoordinateEast);
					System.out.println("South East Button" + southEastButton);
					if(maze.containsKey(southEastButton)){
						if(StringUtils.equals(southEastButton, maze.get(southEastButton).getText())){
							maze.get(southEastButton).setEnabled(true);
						}
					}
						
					String southWestButton = String.valueOf(rowCoordinateSouth) + "-" + String.valueOf(columnCoordinateWest);
					System.out.println("South West Button" + southWestButton);
					if(maze.containsKey(southWestButton)){
						if(StringUtils.equals(southWestButton, maze.get(southWestButton).getText())){
							maze.get(southWestButton).setEnabled(true);
						}
					}*/
				
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
	 * This method is used to initialize the server properties
	 * 
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	private void initServerProperties() throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(1234);
	    this.stub = (Server) registry.lookup("mazeGame");
	}
	
	/* (non-Javadoc)
	 * @see client.Client#notifyPlayer(server.GameStats)
	 */
	public void notifyPlayer(GameStats gameStats) throws RemoteException{		
		this.hasGameStarted = true;
		this.gameStats = gameStats;		
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
			//userTable.setBounds((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2)+100, 100, 500, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2);
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

	@Override
	public void isAlive() throws RemoteException {
		System.out.println("Hey there.. I am alive....");
		
	}

	@Override
	public void notifyEnd() throws RemoteException {
		System.out.println("Game Ended.....");
		System.exit(0);		
	}
}
 