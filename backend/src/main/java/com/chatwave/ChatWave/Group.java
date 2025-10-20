package com.chatwave.ChatWave;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.*;

@Entity
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;
    private String groupName;
    private Set<User> members;

    public Group(){}
    public Group(String groupName, Set<User> members){
        this.groupName = groupName;
        this.members = members;
    }

    public Long getGroupId(){return this.groupId;}

    public String getGroupName(){return this.groupName;}

    public Set<User> getMembers(){return this.members;}

    public void setGroupName(String groupName){this.groupName = groupName;}
    public void setMembers(Set<User> members){this.members = members;}
}
