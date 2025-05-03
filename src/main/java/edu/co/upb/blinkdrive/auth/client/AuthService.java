package edu.co.upb.blinkdrive.auth.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    String authenticate(String username, String password) throws RemoteException;
    boolean validateToken(String token) throws RemoteException;
    boolean registerUser(String username, String password) throws RemoteException;
    boolean revokeToken(String token) throws RemoteException;
}