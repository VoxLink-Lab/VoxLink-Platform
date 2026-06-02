package voxlink.server.src.main.network.voice;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks which users are connected to which voice channels and stores their UDP endpoint addresses.
 */
public class VoiceSessionManager {

    private static VoiceSessionManager instance;

    // Maps a channelId to a set of User IDs in that channel
    private final Map<Integer, Set<Integer>> channelMembers = new ConcurrentHashMap<>();
    
    // Maps a userId to their UDP endpoint address
    private final Map<Integer, InetSocketAddress> userEndpoints = new ConcurrentHashMap<>();

    // Maps an expected voice token to a userId (for UDP connection authentication)
    private final Map<String, Integer> pendingTokens = new ConcurrentHashMap<>();

    // Maps a userId to the channelId they are currently in
    private final Map<Integer, Integer> userChannels = new ConcurrentHashMap<>();

    private VoiceSessionManager() {}

    public static synchronized VoiceSessionManager getInstance() {
        if (instance == null) {
            instance = new VoiceSessionManager();
        }
        return instance;
    }

    public void generateToken(int userId, int channelId, String token) {
        pendingTokens.put(token, userId);
        userChannels.put(userId, channelId);
    }

    public boolean authenticateEndpoint(String token, InetSocketAddress address) {
        Integer userId = pendingTokens.remove(token);
        if (userId != null) {
            userEndpoints.put(userId, address);
            
            Integer channelId = userChannels.get(userId);
            if (channelId != null) {
                channelMembers.computeIfAbsent(channelId, k -> new CopyOnWriteArraySet<>()).add(userId);
            }
            return true;
        }
        return false;
    }

    public void removeUser(int userId) {
        userEndpoints.remove(userId);
        Integer channelId = userChannels.remove(userId);
        if (channelId != null) {
            Set<Integer> members = channelMembers.get(channelId);
            if (members != null) {
                members.remove(userId);
                if (members.isEmpty()) {
                    channelMembers.remove(channelId);
                }
            }
        }
    }

    public Set<Integer> getChannelMembers(int channelId) {
        return channelMembers.getOrDefault(channelId, new CopyOnWriteArraySet<>());
    }

    public InetSocketAddress getUserEndpoint(int userId) {
        return userEndpoints.get(userId);
    }
}
