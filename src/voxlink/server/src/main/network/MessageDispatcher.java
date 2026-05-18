package voxlink.server.src.main.network;

import voxlink.server.src.main.service.AuthService;
import voxlink.server.src.main.service.ChannelService;
import voxlink.server.src.main.service.MessageService;
import voxlink.server.src.main.service.WorkspaceService;

public class MessageDispatcher {

    private final AuthService authService;
    private final WorkspaceService workspaceService;
    private final ChannelService channelService;
    private final MessageService messageService;

    public MessageDispatcher() {
        this.authService = new AuthService();
        this.workspaceService = new WorkspaceService();
        this.channelService = new ChannelService();
        this.messageService = new MessageService();
    }

    /**
     * Parses the incoming client request and dispatches it to the appropriate service.
     * @param request The raw string from the client
     * @return The response to send back to the client
     */
    public String dispatch(String request) {
        // -------------------------------------------------------------
        // TODO: PHASE 5 - The Communication Protocol logic goes here!
        // We will parse the 'request' (e.g., JSON), determine the action 
        // (e.g., "LOGIN", "SEND_MESSAGE"), and call the right service.
        // -------------------------------------------------------------
        
        // For now, return a placeholder echo
        return "Server Processed: " + request;
    }
}

