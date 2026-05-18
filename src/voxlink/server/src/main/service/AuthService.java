package voxlink.server.src.main.service;

import voxlink.server.src.main.model.User;
import voxlink.server.src.main.repository.UserRepository;
import voxlink.server.src.main.util.PasswordHasher;

public class AuthService {
    
    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    /**
     * Registers a new user.
     * @return The registered User object if successful, or null if the username or email already exists.
     */
    public User registerUser(String username, String email, String plainPassword) {
        // Check if username or email already exists
        if (userRepository.getUserByUsername(username) != null) {
            System.out.println("Registration failed: Username already exists.");
            return null;
        }
        if (userRepository.getUserByEmail(email) != null) {
            System.out.println("Registration failed: Email already exists.");
            return null;
        }

        // Hash the password
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);

        // Create the user object
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(hashedPassword);
        newUser.setStatus("OFFLINE");

        // Save to database
        if (userRepository.createUser(newUser)) {
            System.out.println("User registered successfully: " + username);
            return newUser;
        } else {
            System.out.println("Registration failed: Database error.");
            return null;
        }
    }

    /**
     * Authenticates a user.
     * @return The User object if authentication is successful, or null if it fails.
     */
    public User loginUser(String username, String plainPassword) {
        User user = userRepository.getUserByUsername(username);
        if (user == null) {
            System.out.println("Login failed: User not found.");
            return null;
        }

        // Verify password
        if (PasswordHasher.verifyPassword(plainPassword, user.getPasswordHash())) {
            // Update status to ONLINE
            userRepository.updateUserStatus(user.getUserId(), "ONLINE");
            user.setStatus("ONLINE");
            System.out.println("User logged in successfully: " + username);
            return user;
        } else {
            System.out.println("Login failed: Incorrect password.");
            return null;
        }
    }
    
    /**
     * Logs out a user.
     */
    public void logoutUser(int userId) {
        userRepository.updateUserStatus(userId, "OFFLINE");
        System.out.println("User logged out successfully.");
    }
}
