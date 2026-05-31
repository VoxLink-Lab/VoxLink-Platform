package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.util.List;
import java.util.function.Consumer;

/**
 * ChannelModel handles client-side business logic for channel operations.
 */
public class ChannelModel {

    private static ChannelModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private final AppState appState;
    private final MessageCache messageCache;

    // Private constructor for singleton pattern
    private ChannelModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.appState = AppState.getInstance();
        this.messageCache = MessageCache.getInstance();
    }

    // Get singleton instance
    public static synchronized ChannelModel getInstance() {
        if (instance == null) {
            instance = new ChannelModel();
        }
        return instance;
    }

    // Fetch all channels for a workspace
    public void fetchChannels(int workspaceId, Consumer<FetchChannelsResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new FetchChannelsResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.CHANNEL_LIST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("workspaceId", workspaceId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.CHANNEL_LIST_DATA) {
                    @SuppressWarnings("unchecked")
                    List<ChannelDTO> channels = (List<ChannelDTO>) response.get("channels");

                    if (channels != null) {
                        appState.setCurrentChannels(channels);
                        if (callback != null) {
                            callback.accept(new FetchChannelsResult(true, channels, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new FetchChannelsResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new FetchChannelsResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Join a channel
    public void joinChannel(int channelId, Consumer<JoinChannelResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new JoinChannelResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.CHANNEL_JOIN);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.CHANNEL_JOIN_SUCCESS) {
                    ChannelDTO channel = (ChannelDTO) response.get("channel");

                    if (channel != null) {
                        channel.setHasJoined(true);
                        appState.addChannel(channel);

                        // Reset unread count for this channel
                        messageCache.resetUnreadCount(channelId);

                        if (callback != null) {
                            callback.accept(new JoinChannelResult(true, channel, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new JoinChannelResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new JoinChannelResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Leave channel
    public void leaveChannel(int channelId, Consumer<LeaveChannelResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new LeaveChannelResult(false, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.CHANNEL_LEAVE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.CHANNEL_LEAVE_SUCCESS) {
                    appState.removeChannel(channelId);
                    messageCache.clearChannel(channelId);

                    // If leaving current channel, clear it
                    ChannelDTO currentChannel = appState.getCurrentChannel();
                    if (currentChannel != null && currentChannel.getId() == channelId) {
                        appState.setCurrentChannel(null);
                    }

                    if (callback != null) {
                        callback.accept(new LeaveChannelResult(true, null));
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new LeaveChannelResult(false, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Create a new channel
    public void createChannel(String name, String description, int workspaceId,
                              String channelType, boolean isPrivate,
                              Consumer<CreateChannelResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new CreateChannelResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.CHANNEL_CREATE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("name", name);
        if (description != null) {
            packet.put("description", description);
        }
        packet.put("workspaceId", workspaceId);
        packet.put("channelType", channelType);
        packet.put("isPrivate", isPrivate);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.CHANNEL_CREATE_SUCCESS) {
                    ChannelDTO channel = (ChannelDTO) response.get("channel");

                    if (channel != null) {
                        appState.addChannel(channel);
                        if (callback != null) {
                            callback.accept(new CreateChannelResult(true, channel, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new CreateChannelResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new CreateChannelResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Get message histroy for the channel
    public void getMessageHistory(int channelId, int limit, int offset,
                                  Consumer<MessageHistoryResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new MessageHistoryResult(false, null, "Not authenticated"));
            }
            return;
        }

        // Check cache first
        if (offset == 0 && messageCache.hasChannelCache(channelId)) {
            List<MessageDTO> cachedMessages = messageCache.getMessages(channelId);
            if (!cachedMessages.isEmpty()) {
                if (callback != null) {
                    callback.accept(new MessageHistoryResult(true, cachedMessages, null));
                }
                return;
            }
        }

        Packet packet = new Packet(RequestType.CHANNEL_GET_HISTORY);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);
        packet.put("limit", limit);
        packet.put("offset", offset);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.CHANNEL_HISTORY_DATA) {
                    @SuppressWarnings("unchecked")
                    List<MessageDTO> messages = (List<MessageDTO>) response.get("messages");

                    if (messages != null) {
                        // Cache messages
                        if (offset == 0) {
                            messageCache.addMessages(messages, channelId);
                        }
                        if (callback != null) {
                            callback.accept(new MessageHistoryResult(true, messages, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new MessageHistoryResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new MessageHistoryResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Set current active channel
    public void setCurrentChannel(ChannelDTO channel) {
        appState.setCurrentChannel(channel);
        if (channel != null) {
            // Reset unread count for this channel
            messageCache.resetUnreadCount(channel.getId());
        }
    }

    // Get current active channel
    public ChannelDTO getCurrentChannel() {
        return appState.getCurrentChannel();
    }

    // Get all channels in the workspace
    public List<ChannelDTO> getCurrentChannels() {
        return appState.getCurrentChannels();
    }

    // Get channel by ID
    public ChannelDTO getChannelById(int channelId) {
        return appState.getChannelById(channelId);
    }

    // Get unread message count for a channel
    public int getUnreadCount(int channelId) {
        return messageCache.getUnreadCount(channelId);
    }

    // Get total unread counts across all channels
    public int getTotalUnreadCount() {
        return messageCache.getTotalUnreadCount();
    }

    // --- RESULT CLASSES ---

    public static class FetchChannelsResult {
        private final boolean success;
        private final List<ChannelDTO> channels;
        private final String errorMessage;

        public FetchChannelsResult(boolean success, List<ChannelDTO> channels, String errorMessage) {
            this.success = success;
            this.channels = channels;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public List<ChannelDTO> getChannels() { return channels; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class JoinChannelResult {
        private final boolean success;
        private final ChannelDTO channel;
        private final String errorMessage;

        public JoinChannelResult(boolean success, ChannelDTO channel, String errorMessage) {
            this.success = success;
            this.channel = channel;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public ChannelDTO getChannel() { return channel; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class LeaveChannelResult {
        private final boolean success;
        private final String errorMessage;

        public LeaveChannelResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class CreateChannelResult {
        private final boolean success;
        private final ChannelDTO channel;
        private final String errorMessage;

        public CreateChannelResult(boolean success, ChannelDTO channel, String errorMessage) {
            this.success = success;
            this.channel = channel;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public ChannelDTO getChannel() { return channel; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class MessageHistoryResult {
        private final boolean success;
        private final List<MessageDTO> messages;
        private final String errorMessage;

        public MessageHistoryResult(boolean success, List<MessageDTO> messages, String errorMessage) {
            this.success = success;
            this.messages = messages;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public List<MessageDTO> getMessages() { return messages; }
        public String getErrorMessage() { return errorMessage; }
    }
}