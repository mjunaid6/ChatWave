package com.chatwave.ChatWave;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.function.Function;

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msgId;
    private String sender;
    private String recipient;
    private String msg;
    private LocalDateTime timestamp;

    public Message() {}
    public Message(String sender, String recipient, String msg) {
        this.sender = sender;
        this.recipient = recipient;
        this.msg = msg;
        timestamp = LocalDateTime.now();
    }

    public Long getMsgId() {return this.msgId;}

    public String getSender() {return this.sender;}
    public String getRecipient() {return this.recipient;}
    public String getMsg() {return this.msg;}
    public LocalDateTime getTimestamp() {return this.timestamp;}
}
