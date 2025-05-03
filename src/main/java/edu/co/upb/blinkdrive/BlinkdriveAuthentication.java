/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package edu.co.upb.blinkdrive;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import edu.co.upb.blinkdrive.auth.client.AuthService;

/**
 *
 * @author santi
 */
public class BlinkdriveAuthentication {

    public static void main(String[] args) {
        try {
            System.out.println("Starting Authentication Server...");
            
            // Create and export the service
            AuthService authService = new AuthServiceImpl();
            
            // Create and start the registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Bind the service to the registry
            registry.rebind("AuthService", authService);
            
            System.out.println("Authentication Server is running!");
            
        } catch (RemoteException e) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}
