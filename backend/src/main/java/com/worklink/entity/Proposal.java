package com.worklink.entity;

import com.worklink.entity.enums.ProposalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Proposal entity connecting Freelancer to Project bid with terms and unique bid constraint.
 */
@Entity
@Table(
        name = "proposals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_freelancer_proposal", columnNames = {"project_id", "freelancer_id"})
        },
        indexes = {
                @Index(name = "idx_proposal_status", columnList = "status"),
                @Index(name = "idx_proposal_project", columnList = "project_id")
        }
)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private FreelancerProfile freelancer;

    @Column(name = "cover_letter", nullable = false, length = 3000)
    private String coverLetter;

    @Column(name = "proposed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal proposedAmount;

    @Column(name = "estimated_days", nullable = false)
    private Integer estimatedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProposalStatus status = ProposalStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_project_id")
    private PortfolioProject portfolioReference;

    public Proposal() {
    }

    public Proposal(Project project, FreelancerProfile freelancer, String coverLetter, BigDecimal proposedAmount, Integer estimatedDays, ProposalStatus status, PortfolioProject portfolioReference) {
        this.project = project;
        this.freelancer = freelancer;
        this.coverLetter = coverLetter;
        this.proposedAmount = proposedAmount;
        this.estimatedDays = estimatedDays;
        this.status = status;
        this.portfolioReference = portfolioReference;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public FreelancerProfile getFreelancer() {
        return freelancer;
    }

    public void setFreelancer(FreelancerProfile freelancer) {
        this.freelancer = freelancer;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(BigDecimal proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public Integer getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(Integer estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public PortfolioProject getPortfolioReference() {
        return portfolioReference;
    }

    public void setPortfolioReference(PortfolioProject portfolioReference) {
        this.portfolioReference = portfolioReference;
    }
}

