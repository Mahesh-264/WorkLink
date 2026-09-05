package com.worklink.dto.capability;

import java.time.LocalDate;

public record PortfolioProjectResponse(Long id, String title, String description, String projectUrl,
                                       String repositoryUrl, String technologiesUsed, String role,
                                       LocalDate completionDate, String imageUrl) { }
