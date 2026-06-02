package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.util.List;
import java.util.function.Consumer;

/**
 * FriendModel handles client-side logic for friends and Direct Messages.
 */
public class FriendModel {

    private static FriendModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private final AppState appState;

    private FriendModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.appState = AppState.getInstance();
    }

    public static synchronized FriendModel getInstance() {
        if (instance == null) {
            instance = new FriendModel();
        }
        return instance;
    }

    // Send a friend request
    public void addFriend(String username, Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.FRIEND_ADD);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("username", username);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.FRIEND_ADD_SUCCESS) {
                    if (callback != null) callback.accept(true);
                } else if (response.getResponseType() == ResponseType.FRIEND_ADD_FAILURE) {
                    if (callback != null) callback.accept(false);
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Fetch the friend list
    public void fetchFriends(Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.FRIEND_LIST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.FRIEND_LIST_DATA) {
                    @SuppressWarnings("unchecked")
                    List<UserDTO> friends = (List<UserDTO>) response.get("friends");
                    if (friends != null) {
                        appState.setFriends(friends);
                        if (callback != null) callback.accept(true);
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Fetch direct messages
    public void fetchDirectMessages(Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.DM_LIST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.DM_LIST_DATA) {
                    @SuppressWarnings("unchecked")
                    List<ChannelDTO> dms = (List<ChannelDTO>) response.get("channels");
                    if (dms != null) {
                        appState.setDirectMessages(dms);
                        if (callback != null) callback.accept(true);
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Create a new direct message
    public void createDirectMessage(int targetUserId, Consumer<ChannelDTO> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.DM_CREATE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("targetUserId", targetUserId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.DM_CREATE_SUCCESS) {
                    ChannelDTO dm = (ChannelDTO) response.get("channel");
                    if (dm != null) {
                        appState.addDirectMessage(dm);
                        if (callback != null) callback.accept(dm);
                    }
                } else if (response.getResponseType() == ResponseType.DM_CREATE_FAILURE) {
                    if (callback != null) callback.accept(null);
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Create a new direct message by username
    public void createDirectMessageByUsername(String targetUsername, Consumer<ChannelDTO> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.DM_CREATE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("targetUsername", targetUsername);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.DM_CREATE_SUCCESS) {
                    ChannelDTO dm = (ChannelDTO) response.get("channel");
                    if (dm != null) {
                        appState.addDirectMessage(dm);
                        if (callback != null) callback.accept(dm);
                    }
                } else if (response.getResponseType() == ResponseType.DM_CREATE_FAILURE) {
                    if (callback != null) callback.accept(null);
                }
                connection.removePacketListener(this);
            }
        });
    }
}
