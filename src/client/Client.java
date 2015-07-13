package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

import server.GameStats;

public interface Client extends Remote{
	
	public void isAlive() throws RemoteException;

	public void notifyPlayer(GameStats gameStats) throws RemoteException ;
	
	public void notifyEnd() throws RemoteException;
	
}
