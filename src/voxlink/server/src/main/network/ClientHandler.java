package voxlink.server.src.main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import voxlink.server.src.main.service.AuthService;
import voxlink.server.src.main.service.ChannelService;
import voxlink.server.src.main.service.MessageService;
import voxlink.server.src.main.service.WorkspaceService;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    // Services to handle business logic for this specific client
    private final AuthService authService;
    private final WorkspaceService workspaceService;
    private final ChannelService channelService;
    private final MessageService messageService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        
        // Initialize services for this client session
        this.authService = new AuthService();
        this.workspaceService = new WorkspaceService();
        this.channelService = new ChannelService();
        this.messageService = new MessageService();
    }

    @Override
    public void run() {
        try {
            // Setup input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Welcome to the VoxLink Server!");

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received from client: " + request);
                
                // -------------------------------------------------------------
                // TODO: PHASE 5 - The Communication Protocol goes here!
                // We will parse the 'request' (e.g., JSON), determine the action 
                // (e.g., "LOGIN", "SEND_MESSAGE"), and call the right service.
                // -------------------------------------------------------------
                
                if (request.equalsIgnoreCase("QUIT")) {
                    out.println("Goodbye!");
                    break;
                }

                out.println("Server echo: " + request);
            }
            
        } catch (IOException e) {
            System.err.println("Exception in ClientHandler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
