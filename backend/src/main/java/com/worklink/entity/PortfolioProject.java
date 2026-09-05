package com.worklink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * PortfolioProject entity showcasing past work deliverables of a freelancer.
 * Contributes to portfolio relevance scoring in the matching engine.
 */
@Entity
@Table(
        name = "portfolio_projects",
        indexes = {
                @Index(name = "idx_portfolio_freelancer", columnList = "freelancer_id")
        }
)
public class PortfolioProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 3000)
    private String description;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "technologies", length = 500)
    private String technologies;

    @Column(name = "project_role", length = 150)
    private String role;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public PortfolioProject() {
    }

    public PortfolioProject(FreelancerProfile freelancer, String title, String description, String projectUrl, String technologies, String imageUrl) {
        this.freelancer = freelancer;
        this.title = title;
        this.description = description;
        this.projectUrl = projectUrl;
        this.technologies = technologies;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FreelancerProfile getFreelancer() {
        return freelancer;
    }

    public void setFreelancer(FreelancerProfile freelancer) {
        this.freelancer = freelancer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    public String getTechnologies() {
        return technologies;
    }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
