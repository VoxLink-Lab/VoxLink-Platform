package voxlink.server.src.main.service;

import voxlink.server.src.main.model.Workspace;
import voxlink.server.src.main.model.Channel;
import voxlink.server.src.main.repository.WorkspaceRepository;
import voxlink.server.src.main.repository.ChannelRepository;

import java.util.List;
import java.util.UUID;

public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final ChannelRepository channelRepository;

    public WorkspaceService() {
        this.workspaceRepository = new WorkspaceRepository();
        this.channelRepository = new ChannelRepository();
    }

    public Workspace createWorkspace(String name, String description, int ownerId) {
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setDescription(description);
        workspace.setOwnerId(ownerId);
        
        // Generate a unique invite code
        String inviteCode = UUID.randomUUID().toString().substring(0, 8);
        workspace.setInviteCode(inviteCode);

        if (workspaceRepository.createWorkspace(workspace)) {
            // Create a default "general" channel for the new workspace
            Channel defaultChannel = new Channel();
            defaultChannel.setWorkspaceId(workspace.getWorkspaceId());
            defaultChannel.setName("general");
            defaultChannel.setType("TEXT");
            defaultChannel.setPrivate(false);
            channelRepository.createChannel(defaultChannel);

            System.out.println("Workspace created successfully: " + name);
            return workspace;
        }
        return null;
    }

    public Workspace joinWorkspace(String inviteCode) {
        return workspaceRepository.getWorkspaceByInviteCode(inviteCode);
    }

    public List<Workspace> getWorkspacesByOwner(int ownerId) {
        return workspaceRepository.getWorkspacesByOwnerId(ownerId);
    }
}

