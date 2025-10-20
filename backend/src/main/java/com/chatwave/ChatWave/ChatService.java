package com.chatwave.ChatWave;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;

    public ChatService(UserRepository userRepository, MessageRepository messageRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.groupRepository = groupRepository;
    }

    public User createOrLogin(String username) {
        User user = userRepository.findByUsername(username);
        if(user == null) { user = new User(username); }
        else {
            if(user.isOnline()) {
                throw new IllegalStateException("User is already logged in from another session.");
            }
            user.setOnline(true);
        }
        return userRepository.save(user);
    }

    public void logoutUser(String username) {
        User user = userRepository.findByUsername(username);
        if(user != null) {
            user.setOnline(false);
            userRepository.save(user);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Group createGroup(String groupName, Set<User> members) {
        Group group = groupRepository.getGroupByName(groupName);
        if(group != null) { return null; }
        group = new Group(groupName,members);
        return groupRepository.save(group);
    }

    public Group getGroupByName(String groupName) {
        return groupRepository.getGroupByName(groupName);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Message saveMessage(String sender, String receiver, String message) {
        Message msg = new Message(sender, receiver, message);
        return messageRepository.save(msg);
    }

    public List<Message> getPrivateConversations(String user1, String user2) {
        List<Message> conv1 = messageRepository.findBySenderAndRecipient(user1, user2);
        List<Message> conv2 = messageRepository.findBySenderAndRecipient(user2, user1);
        conv1.addAll(conv2);
        conv1.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
        return conv1;
    }

    public List<Message> getGroupMessages(String groupName) {
        return messageRepository.findByRecipient(groupName);
    }
}
