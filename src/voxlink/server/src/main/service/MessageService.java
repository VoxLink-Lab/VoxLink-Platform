//package voxlink.server.src.main.service;
//
//import voxlink.server.src.main.model.Message;
//import voxlink.server.src.main.repository.MessageRepository;
//
//import java.util.List;
//
//public class MessageService {
//
//    private final MessageRepository messageRepository;
//
//    public MessageService() {
//        this.messageRepository = new MessageRepository();
//    }
//
//    public Message sendChannelMessage(int senderId, int channelId, String content) {
//        Message message = new Message();
//        message.setSenderId(senderId);
//        message.setChannelId(channelId);
//        message.setContent(content);
//
//        if (messageRepository.createMessage(message)) {
//            return message;
//        }
//        return null;
//    }
//
//    public Message sendDirectMessage(int senderId, int receiverId, String content) {
//        Message message = new Message();
//        message.setSenderId(senderId);
//        message.setReceiverId(receiverId); // Needs Integer if model uses Integer
//        message.setContent(content);
//
//        if (messageRepository.createMessage(message)) {
//            return message;
//        }
//        return null;
//    }
//
//    public List<Message> getChannelMessages(int channelId) {
//        return messageRepository.getMessagesByChannelId(channelId);
//    }
//
//    public List<Message> getDirectMessages(int user1Id, int user2Id) {
//        return messageRepository.getDirectMessages(user1Id, user2Id);
//    }
//}
//
