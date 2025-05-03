package edu.co.upb.blinkdrive;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;

import edu.co.upb.blinkdrive.auth.client.AuthService;
import edu.co.upb.blinkdrive.auth.database.DatabaseManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private static final long serialVersionUID = 1L;
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    
    public AuthServiceImpl() throws RemoteException {
        super();
    }
    
    @Override
    public String authenticate(String username, String password) throws RemoteException {
        try {
            if (validateCredentials(username, password)) {
                int userId = getUserId(username);
                return generateAndSaveToken(userId, username);
            }
            return null;
        } catch (NoSuchAlgorithmException | SQLException e) {
            throw new RemoteException("Authentication failed", e);
        }
    }
    
    @Override
    public boolean validateToken(String token) throws RemoteException {
        try {
            // First check if token exists and is valid in database
            if (!isTokenValidInDb(token)) {
                return false;
            }
            
            // Then verify JWT signature and expiration
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
            // Use the claims object to avoid unused warning
            claims.getSubject();
                
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | SignatureException | IllegalArgumentException | SQLException e) {
            return false;
        }
    }
    
    private boolean validateCredentials(String username, String password) throws SQLException, NoSuchAlgorithmException {
        String query = "SELECT password_hash, salt FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                
                String hashedPassword = hashPassword(password, salt);
                return hashedPassword.equals(storedHash);
            }
            return false;
        }
    }
    
    private int getUserId(String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("User not found");
        }
    }
    
    private String generateAndSaveToken(int userId, String username) throws SQLException {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_VALIDITY);
        
        String jwtToken = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
        
        // Save token to database
        String query = "INSERT INTO tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, jwtToken);
            stmt.setTimestamp(3, new Timestamp(expiryDate.getTime()));
            
            stmt.executeUpdate();
            return jwtToken;
        }
    }
    
    private boolean isTokenValidInDb(String token) throws SQLException {
        String query = "SELECT * FROM tokens WHERE token = ? AND expires_at > ? AND revoked = FALSE";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, token);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }
    
    // Method to register a new user
    @Override
    public boolean registerUser(String username, String password) throws RemoteException {
        try {
            // Check if username exists
            if (userExists(username)) {
                return false;
            }
            
            // Generate salt and hash password
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            
            // Save to database
            String query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, salt);
                
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (NoSuchAlgorithmException | SQLException e) {
            throw new RemoteException("Registration failed", e);
        }
    }
    
    private boolean userExists(String username) throws SQLException {
        String query = "SELECT 1 FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            return rs.next();
        }
    }
    
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes());
        byte[] hashedPassword = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedPassword);
    }
    
    // Method to revoke a token
    @Override
    public boolean revokeToken(String token) throws RemoteException {
        String query = "UPDATE tokens SET revoked = TRUE WHERE token = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, token);
            int rowsAffected = stmt.executeUpdate();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RemoteException("Failed to revoke token", e);
        }
    }
}
