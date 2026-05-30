//package voxlink.server.src.main.service;
//
//import voxlink.server.src.main.model.Channel;
//import voxlink.server.src.main.repository.ChannelRepository;
//
//import java.util.List;
//
//public class ChannelService {
//
//    private final ChannelRepository channelRepository;
//
//    public ChannelService() {
//        this.channelRepository = new ChannelRepository();
//    }
//
//    public Channel createChannel(int workspaceId, String name, String type, boolean isPrivate) {
//        Channel channel = new Channel();
//        channel.setWorkspaceId(workspaceId);
//        channel.setName(name);
//        channel.setType(type);
//        channel.setPrivate(isPrivate);
//
//        if (channelRepository.createChannel(channel)) {
//            System.out.println("Channel created successfully: " + name);
//            return channel;
//        }
//        return null;
//    }
//
//    public List<Channel> getWorkspaceChannels(int workspaceId) {
//        return channelRepository.getChannelsByWorkspaceId(workspaceId);
//    }
//
//    public boolean deleteChannel(int channelId) {
//        return channelRepository.deleteChannel(channelId);
//    }
//}
//
