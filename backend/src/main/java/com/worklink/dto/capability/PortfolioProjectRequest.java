package com.worklink.dto.capability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record PortfolioProjectRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @Size(max = 3000) String description,
        @URL(message = "Project URL must be valid") @Size(max = 500) String projectUrl,
        @URL(message = "Repository URL must be valid") @Size(max = 500) String repositoryUrl,
        @Size(max = 500) String technologiesUsed,
        @Size(max = 150) String role,
        @PastOrPresent(message = "Completion date cannot be in the future") LocalDate completionDate,
        @URL(message = "Image URL must be valid") @Size(max = 500) String imageUrl
) { }
