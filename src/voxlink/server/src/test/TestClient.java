package voxlink.server.src.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) {
        String hostname = "127.0.0.1";
        int port = 8080;

        System.out.println("Attempting to connect to the VoxLink server on " + hostname + ":" + port);

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println(" Connected to the server!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Read welcome message
            System.out.println("SERVER: " + in.readLine());

            // ------------------------------------------
            // TEST 1: Register a new user
            // ------------------------------------------
            String registerJson = "{\"action\":\"REGISTER\",\"username\":\"testuser1\",\"email\":\"test1@example.com\",\"password\":\"mypassword\"}";
            System.out.println("\n[TEST 1] Sending Registration request...");
            System.out.println("-> " + registerJson);
            out.println(registerJson);

            // Wait for response
            System.out.println("<- SERVER RESPONSE: " + in.readLine());

            // ------------------------------------------
            // TEST 2: Login with the new user
            // ------------------------------------------
            String loginJson = "{\"action\":\"LOGIN\",\"username\":\"testuser1\",\"password\":\"mypassword\"}";
            System.out.println("\n[TEST 2] Sending Login request...");
            System.out.println("-> " + loginJson);
            out.println(loginJson);

            // Wait for response
            System.out.println("<- SERVER RESPONSE: " + in.readLine());

            // ------------------------------------------
            // TEST 3: Disconnect gracefully
            // ------------------------------------------
            System.out.println("\n[TEST 3] Sending QUIT command...");
            out.println("QUIT");
            System.out.println("<- SERVER RESPONSE: " + in.readLine());

        } catch (Exception e) {
            System.out.println(" Could not connect to the server. Is it running?");
            e.printStackTrace();
        }
    }
}
