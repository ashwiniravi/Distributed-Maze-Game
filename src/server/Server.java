package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import client.Client;

public interface Server extends Remote {

	/**
	 * This method returns a String
	 * 
	 * @return a String
	 * @throws RemoteException
	 */
	String sayHello() throws RemoteException;

	/**
	 * This method is used to validate the user
	 * 
	 * @param userName
	 * @param password
	 * @return true if the customer has entered the correct userName and
	 *         password
	 * @throws RemoteException
	 */
	boolean validateUser(String userName, String password) throws RemoteException;

	/**
	 * This method is used to check if the user name already exists or available
	 * for the new user
	 * 
	 * @param userName
	 * @return isAvailable
	 * @throws RemoteException
	 */
	boolean checkAvailableUserName(String userName) throws RemoteException;

	/**
	 * This method is used to add new users to the system
	 * 
	 * @param userName
	 * @param password
	 * @throws RemoteException
	 */
	void addUser(String userName, String password) throws RemoteException;

	boolean addUserToGame(String userID, Client clientImpl) throws RemoteException;
	
	boolean joinUserToGame(String userID, Client clientImpl) throws RemoteException;
	
	int getGridSize() throws RemoteException;
	
	//GameStats moveUser(String userID, GameStats gameStats, CoordinatesUtil coordinatesUtil) throws RemoteException;
	
	GameStats moveUser(String userID,  CoordinatesUtil coordinatesUtil) throws RemoteException;
	
}
