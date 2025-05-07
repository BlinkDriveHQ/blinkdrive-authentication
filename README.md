# BlinkDrive Authentication Server

A Java-based RMI service that manages authentication and authorization for the BlinkDrive Distributed File System.

## Overview

The Authentication Server provides secure user authentication and JWT token generation/validation services for all BlinkDrive components, implementing a central access control mechanism through Java RMI technology.

## Features

- **User Management**: Registration and credential validation
- **JWT Token Generation**: Creation of secure, time-limited JSON Web Tokens
- **Token Validation**: Verification of token integrity and expiration
- **Token Revocation**: Support for explicit token invalidation (logout)
- **RMI Integration**: Remote invocation support for distributed architecture

## Requirements

- Java 17 or higher
- Maven 3.8+
- MariaDB/MySQL database

## Setup and Configuration

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/blinkdrive-auth.git
   cd blinkdrive-auth
   ```

2. Configure database settings in `src/main/resources/application.properties`:

   ```properties
   db.url=jdbc:mariadb://localhost:3306/blinkdrive-authentication
   db.user=username
   db.password=password
   db.driver=org.mariadb.jdbc.Driver
   ```

3. Create database schema:

   ```bash
   mysql -u username -p < src/main/resources/scripts/database.sql
   ```

4. Build the project:

   ```bash
   mvn clean package
   ```

5. Run the server:

   ```bash
   java -jar target/blinkdrive-auth-0.0.1-SNAPSHOT.jar
   ```

## API Reference

### RMI Interface

```java
public interface AuthService extends Remote {
    String authenticate(String username, String password) throws RemoteException;
    boolean validateToken(String token) throws RemoteException;
    boolean registerUser(String username, String password) throws RemoteException;
    boolean revokeToken(String token) throws RemoteException;
}
```

### Client Usage Example

```java
// Get reference to the RMI registry
Registry registry = LocateRegistry.getRegistry("localhost", 1099);
    
// Look up the remote AuthService object
AuthService authService = (AuthService) registry.lookup("AuthService");

// Use the service
String token = authService.authenticate("username", "password");
boolean isValid = authService.validateToken(token);
```

## Security Considerations

- JWT tokens are signed using HMAC-SHA256
- Passwords are hashed with SHA-256 + salt before storage
- Token expiration is set to 1 hour by default
- RMI communications should be protected by firewall rules

## Database Schema

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Development

### Project Structure

```md
src
├── main
│   ├── java
│   │   └── edu/co/upb/blinkdrive
│   │       ├── auth
│   │       │   ├── client      - RMI client interface
│   │       │   └── database    - Database connectivity
│   │       ├── BlinkdriveAuthentication.java - Entry point
│   │       └── AuthServiceImpl.java        - Implementation
│   └── resources
│       ├── application.properties - Configuration
│       └── scripts               - SQL scripts
└── test
    └── java                     - Test classes
```

### Running the Demo Client

The project includes a demo client to test the authentication service:

```bash
java -cp target/blinkdrive-auth-0.0.1-SNAPSHOT.jar edu.co.upb.blinkdrive.auth.client.AuthClientDemo
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
