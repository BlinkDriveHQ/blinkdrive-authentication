package edu.co.upb.blinkdrive.auth.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class AuthClientDemo {
    private AuthService authService;
    private String currentToken = null;
    private Scanner scanner = new Scanner(System.in);
    
    public AuthClientDemo() {
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            
            // Look up the remote object
            authService = (AuthService) registry.lookup("AuthService");
            
            System.out.println("Connected to Authentication Server");
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Client exception: " + e.toString());
        }
    }
    
    public void runDemo() {
        boolean running = true;
        
        while (running) {
            System.out.println("\n===== Authentication Client Demo =====");
            System.out.println("1. Register new user");
            System.out.println("2. Authenticate (login)");
            System.out.println("3. Validate current token");
            System.out.println("4. Revoke current token (logout)");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            
            int choice = Integer.parseInt(scanner.nextLine());
            
            try {
                switch (choice) {
                    case 1 -> registerUser();
                    case 2 -> authenticate();
                    case 3 -> validateToken();
                    case 4 -> revokeToken();
                    case 5 -> {
                        running = false;
                        System.out.println("Exiting...");
                    }
                    default -> System.out.println("Invalid option!");
                }
            } catch (RemoteException e) {
                System.err.println("Error communicating with the server: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    private void registerUser() throws RemoteException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        boolean success = authService.registerUser(username, password);
        if (success) {
            System.out.println("User registered successfully!");
        } else {
            System.out.println("Registration failed. Username may already exist.");
        }
    }
    
    private void authenticate() throws RemoteException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        String token = authService.authenticate(username, password);
        if (token != null) {
            currentToken = token;
            System.out.println("Authentication successful!");
            System.out.println("Token: " + token.substring(0, 10) + "...");
        } else {
            System.out.println("Authentication failed. Invalid credentials.");
        }
    }
    
    private void validateToken() throws RemoteException {
        if (currentToken == null) {
            System.out.println("No token available. Please authenticate first.");
            return;
        }
        
        boolean isValid = authService.validateToken(currentToken);
        if (isValid) {
            System.out.println("Token is valid!");
        } else {
            System.out.println("Token is invalid or expired.");
            currentToken = null;
        }
    }
    
    private void revokeToken() throws RemoteException {
        if (currentToken == null) {
            System.out.println("No token available. Please authenticate first.");
            return;
        }
        
        boolean revoked = authService.revokeToken(currentToken);
        if (revoked) {
            System.out.println("Token successfully revoked.");
            currentToken = null;
        } else {
            System.out.println("Failed to revoke token.");
        }
    }
    
    public static void main(String[] args) {
        AuthClientDemo client = new AuthClientDemo();
        client.runDemo();
    }
}
