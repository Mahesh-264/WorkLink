package com.worklink.entity;

import com.worklink.entity.enums.ProjectStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "performance_history", uniqueConstraints = @UniqueConstraint(name = "uk_performance_freelancer_project", columnNames = {"freelancer_id", "project_id"}))
public class PerformanceHistory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "freelancer_id", nullable = false) private FreelancerProfile freelancer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private Project project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "proposal_id", nullable = false) private Proposal acceptedProposal;
    @Enumerated(EnumType.STRING) @Column(name = "project_status", nullable = false, length = 30) private ProjectStatus projectStatus;
    @Column(name = "completion_date", nullable = false) private LocalDate completionDate;
    @Column(name = "deadline") private LocalDate deadline;
    @Column(name = "on_time") private Boolean onTime;
    @Column(name = "milestone_count", nullable = false) private int milestoneCount;
    @Column(name = "approved_milestone_count", nullable = false) private int approvedMilestoneCount;
    @Column(name = "client_rating") private Integer clientRating;
    @Column(name = "client_review", length = 2000) private String clientReview;
    @Column(name = "freelancer_activity_count", nullable = false) private int freelancerActivityCount;
    @Column(name = "client_activity_count", nullable = false) private int clientActivityCount;
    @Column(name = "activity_count", nullable = false) private int activityCount;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public FreelancerProfile getFreelancer(){return freelancer;} public void setFreelancer(FreelancerProfile v){freelancer=v;}
    public Project getProject(){return project;} public void setProject(Project v){project=v;}
    public Proposal getAcceptedProposal(){return acceptedProposal;} public void setAcceptedProposal(Proposal v){acceptedProposal=v;}
    public ProjectStatus getProjectStatus(){return projectStatus;} public void setProjectStatus(ProjectStatus v){projectStatus=v;}
    public LocalDate getCompletionDate(){return completionDate;} public void setCompletionDate(LocalDate v){completionDate=v;}
    public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate v){deadline=v;}
    public Boolean getOnTime(){return onTime;} public void setOnTime(Boolean v){onTime=v;}
    public int getMilestoneCount(){return milestoneCount;} public void setMilestoneCount(int v){milestoneCount=v;}
    public int getApprovedMilestoneCount(){return approvedMilestoneCount;} public void setApprovedMilestoneCount(int v){approvedMilestoneCount=v;}
    public Integer getClientRating(){return clientRating;} public void setClientRating(Integer v){clientRating=v;}
    public String getClientReview(){return clientReview;} public void setClientReview(String v){clientReview=v;}
    public int getFreelancerActivityCount(){return freelancerActivityCount;} public void setFreelancerActivityCount(int v){freelancerActivityCount=v;}
    public int getClientActivityCount(){return clientActivityCount;} public void setClientActivityCount(int v){clientActivityCount=v;}
    public int getActivityCount(){return activityCount;} public void setActivityCount(int v){activityCount=v;}
}
