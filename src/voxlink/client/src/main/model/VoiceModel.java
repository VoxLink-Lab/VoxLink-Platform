package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.network.voice.VoiceClient;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.util.function.Consumer;

/**
 * VoiceModel manages TCP signaling for voice channels and lifecycle of the UDP VoiceClient.
 */
public class VoiceModel {

    private static VoiceModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private VoiceClient voiceClient;

    private VoiceModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
    }

    public static synchronized VoiceModel getInstance() {
        if (instance == null) {
            instance = new VoiceModel();
        }
        return instance;
    }

    public void joinVoiceChannel(int channelId, Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) return;

        Packet packet = new Packet(RequestType.VOICE_JOIN_REQUEST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.VOICE_JOIN_SUCCESS) {
                    String voiceToken = (String) response.get("voiceToken");
                    int voicePort = (int) response.get("voicePort");
                    
                    if (voiceClient != null) {
                        voiceClient.disconnect();
                    }
                    
                    // Start UDP client
                    voiceClient = new VoiceClient();
                    // Assuming server IP is the same as TCP
                    String serverIp = connection.getServerHost();
                    if (serverIp == null) serverIp = "127.0.0.1";
                    
                    voiceClient.connect(serverIp, voicePort, channelId, voiceToken);
                    
                    if (callback != null) callback.accept(true);
                } else if (response.getResponseType() == ResponseType.VOICE_JOIN_FAILURE) {
                    if (callback != null) callback.accept(false);
                }
                connection.removePacketListener(this);
            }
        });
    }

    public void leaveVoiceChannel(Consumer<Boolean> callback) {
        if (voiceClient != null) {
            voiceClient.disconnect();
            voiceClient = null;
        }

        Packet packet = new Packet(RequestType.VOICE_LEAVE_REQUEST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());

        connection.sendPacket(packet);
        if (callback != null) callback.accept(true);
    }
}
