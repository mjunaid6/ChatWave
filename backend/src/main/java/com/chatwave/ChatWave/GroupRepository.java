package com.chatwave.ChatWave;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Group getGroupByName(String groupName);
}
