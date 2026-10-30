package com.worklink.dto.milestone;

import com.worklink.entity.enums.MilestoneStatus;
import jakarta.validation.constraints.NotNull;

public record MilestoneReviewRequest(
        @NotNull MilestoneStatus status,
        String feedback
) {}
