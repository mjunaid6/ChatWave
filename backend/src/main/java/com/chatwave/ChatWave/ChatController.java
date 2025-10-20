package com.chatwave.ChatWave;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/login")
    @SendTo("topic/users")
    public User login(@Payload String username) {
        return chatService.createOrLogin(username);
    }

    @MessageMapping("/sendPrivate")
    @SendTo("/topic/messages")
    public Message sendPrivateMessage(@Payload Message message) {
        return chatService.saveMessage(message.getSender(),message.getRecipient(),message.getMsg());
    }

    @MessageMapping("/createGroup")
    @SendTo("topic/groups")
    public Group createGroup(@Payload Group group) {
        return chatService.createGroup(group.getGroupName(), group.getMembers());
    }

    @MessageMapping("/sendGroup")
    @SendTo("topic/messages")
    public Message sendGroupMessage(@Payload Message message) {
        return chatService.saveMessage(message.getSender(),message.getRecipient(),message.getMsg());
    }

}
