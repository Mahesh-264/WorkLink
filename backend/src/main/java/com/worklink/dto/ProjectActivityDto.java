package com.worklink.dto;

import com.worklink.entity.ProjectActivity;
import com.worklink.entity.enums.ProjectActivityType;
import com.worklink.entity.enums.Role;

import java.time.LocalDateTime;

public record ProjectActivityDto(
        Long id,
        Long projectId,
        Long senderId,
        String senderName,
        Role senderRole,
        ProjectActivityType type,
        String content,
        Integer progressPercentage,
        LocalDateTime createdAt
) {
    public static ProjectActivityDto fromEntity(ProjectActivity activity) {
        return new ProjectActivityDto(
                activity.getId(),
                activity.getProject().getId(),
                activity.getSender().getId(),
                activity.getSender().getFirstName() + " " + activity.getSender().getLastName(),
                activity.getSender().getRole(),
                activity.getType(),
                activity.getContent(),
                activity.getProgressPercentage(),
                activity.getCreatedAt()
        );
    }
}
