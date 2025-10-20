package com.chatwave.ChatWave;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String username;
    private boolean online;

    public User() {}
    public User(String username) {
        this.username = username;
        this.online = true;
    }

    public Long getId() {
        return this.userId;
    }

    public String getUserName() {
        return this.username;
    }

    public boolean isOnline() {
        return this.online;
    }
    
    void setUserName(String username) {
        this.username = username;
    }

    void setOnline(boolean online){
        this.online = online;
    }

}