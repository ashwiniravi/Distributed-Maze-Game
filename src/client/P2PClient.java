package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

import server.CoordinatesUtil;
import server.P2PGameStats;

public interface P2PClient extends Remote{
	
	public void updateToPrimaryServer(P2PGameStats gameStats) throws RemoteException;
	
	public void update(P2PGameStats gameStats) throws RemoteException;
	
	public void notifyPlayer(P2PGameStats gameStats) throws RemoteException ;
	
	public void notifyEnd() throws RemoteException;
	
	public boolean validateUser(String userName, String password)throws RemoteException;
	
	public boolean checkAvailableUserName(String userName) throws RemoteException;
	
	public void addUser(String userName, String password) throws RemoteException;

	public boolean addUserToGame(String userID, P2PClient clientImpl) throws RemoteException;
	
	public boolean joinUserToGame(String userID, P2PClient clientImpl) throws RemoteException;
	
	public P2PGameStats moveUser(String userID,  CoordinatesUtil coordinatesUtil) throws RemoteException;
	
	public int getGridSize() throws RemoteException;
	
	public void isAlive() throws RemoteException;	
	
	public void updateToBackupServer(P2PGameStats gameStats) throws RemoteException;
	
	public void notifyPrimaryAndBacupServers(String primaryServerID, P2PClient primaryServer, String backupServerID, P2PClient backupServer) throws RemoteException;
}
