package com.worklink.dto.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long projectId,
        Long reviewerId,
        String reviewerName,
        Long revieweeId,
        String revieweeName,
        Integer rating,
        String feedback,
        LocalDateTime createdAt
) {}
