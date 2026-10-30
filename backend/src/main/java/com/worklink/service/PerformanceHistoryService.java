package com.worklink.service;

import com.worklink.dto.performance.PerformanceHistoryResponse;
import com.worklink.entity.*;
import com.worklink.entity.enums.MilestoneStatus;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.ProposalStatus;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.exception.ResourceNotFoundException;
import com.worklink.repository.*;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceHistoryService {
    private final PerformanceHistoryRepository histories; private final FreelancerProfileRepository freelancers;
    private final ProjectRepository projects; private final ProposalRepository proposals; private final MilestoneRepository milestones;
    private final ProjectActivityRepository activities; private final ReviewRepository reviews; private final CurrentUserProvider current;
    private final MatchingEvaluationService matchingEvaluationService;
    public PerformanceHistoryService(PerformanceHistoryRepository histories, FreelancerProfileRepository freelancers, ProjectRepository projects, ProposalRepository proposals, MilestoneRepository milestones, ProjectActivityRepository activities, ReviewRepository reviews, CurrentUserProvider current, MatchingEvaluationService matchingEvaluationService) {
        this.histories=histories; this.freelancers=freelancers; this.projects=projects; this.proposals=proposals; this.milestones=milestones; this.activities=activities; this.reviews=reviews; this.current=current; this.matchingEvaluationService=matchingEvaluationService;
    }
    @Transactional
    public PerformanceHistory createForCompletedProject(Project project) {
        if(project.getStatus()!=ProjectStatus.COMPLETED || project.getAcceptedFreelancer()==null) return null;
        return histories.findByProjectId(project.getId()).orElseGet(() -> {
            Proposal proposal=proposals.findByProjectIdAndStatus(project.getId(), ProposalStatus.ACCEPTED).stream().findFirst().orElse(null);
            if(proposal==null || !proposal.getFreelancer().getId().equals(project.getAcceptedFreelancer().getId())) return null;
            PerformanceHistory h=new PerformanceHistory(); h.setProject(project); h.setFreelancer(proposal.getFreelancer()); h.setAcceptedProposal(proposal);
            h.setProjectStatus(ProjectStatus.COMPLETED); LocalDate completed=LocalDate.now(); h.setCompletionDate(completed); h.setDeadline(project.getDeadline());
            h.setOnTime(project.getDeadline()==null?null:!completed.isAfter(project.getDeadline()));
            List<Milestone> ms=milestones.findByProjectIdOrderByDeadlineAsc(project.getId()); h.setMilestoneCount(ms.size()); h.setApprovedMilestoneCount((int)ms.stream().filter(m->m.getStatus()==MilestoneStatus.APPROVED).count());
            List<ProjectActivity> acts=activities.findByProjectIdOrderByCreatedAtAsc(project.getId()); Long freelancerUserId=proposal.getFreelancer().getUser().getId(); Long clientUserId=project.getClient().getUser().getId();
            h.setFreelancerActivityCount((int)acts.stream().filter(a->a.getSender().getId().equals(freelancerUserId)).count()); h.setClientActivityCount((int)acts.stream().filter(a->a.getSender().getId().equals(clientUserId)).count()); h.setActivityCount(acts.size());
            reviews.findByProjectId(project.getId()).stream().filter(r->r.getReviewer().getId().equals(clientUserId)&&r.getReviewee().getId().equals(freelancerUserId)).findFirst().ifPresent(r->{h.setClientRating(r.getRating());h.setClientReview(r.getFeedback());});
            PerformanceHistory saved=histories.save(h);
            matchingEvaluationService.recordCompletedOutcome(saved);
            return saved;
        });
    }
    @Transactional(readOnly=true) public List<PerformanceHistoryResponse> mine(){FreelancerProfile f=freelancers.findByUserId(current.requireCurrentUser().id()).orElseThrow(ProfileNotFoundException::new);return histories.findByFreelancerIdOrderByCompletionDateDesc(f.getId()).stream().map(PerformanceHistoryResponse::from).toList();}
    @Transactional(readOnly=true) public PerformanceHistoryResponse mine(Long id){FreelancerProfile f=freelancers.findByUserId(current.requireCurrentUser().id()).orElseThrow(ProfileNotFoundException::new);return histories.findByIdAndFreelancerId(id,f.getId()).map(PerformanceHistoryResponse::from).orElseThrow(()->new ResourceNotFoundException("Performance history not found"));}
    @Transactional(readOnly=true) public PerformanceHistoryResponse forClientProject(Long projectId){var user=current.requireCurrentUser();return histories.findByProjectId(projectId).filter(h->h.getProject().getClient().getUser().getId().equals(user.id())).map(PerformanceHistoryResponse::from).orElseThrow(()->new ResourceNotFoundException("Performance history not found"));}
}
