package com.worklink.dto.milestone;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MilestoneRequest(
        @NotBlank String title,
        @NotBlank String description,
        @PositiveOrZero BigDecimal amount,
        @NotNull LocalDate deadline
) {}
