package voxlink.server.src.main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final MessageDispatcher messageDispatcher;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        // Delegate routing logic to the dedicated MessageDispatcher
        this.messageDispatcher = new MessageDispatcher(); 
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
                
                if (request.equalsIgnoreCase("QUIT")) {
                    out.println("Goodbye!");
                    break;
                }

                // Delegate business logic routing to the MessageDispatcher
                String response = messageDispatcher.dispatch(request);
                out.println(response);
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
