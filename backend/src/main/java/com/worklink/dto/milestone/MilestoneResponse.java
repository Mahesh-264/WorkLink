package com.worklink.dto.milestone;

import com.worklink.entity.enums.MilestoneStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MilestoneResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        BigDecimal amount,
        LocalDate deadline,
        MilestoneStatus status,
        String submissionNote,
        String submissionUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
