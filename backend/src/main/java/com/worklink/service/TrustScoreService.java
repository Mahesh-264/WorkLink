package com.worklink.service;

import com.worklink.dto.trust.TrustScoreResponse;
import com.worklink.entity.*;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.repository.*;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TrustScoreService {
    private static final double[] WEIGHTS = {25,20,20,15,10,10};
    private static final double HALF_LIFE_DAYS = 180.0;
    private static final String AVAILABLE = "AVAILABLE";
    private static final String INSUFFICIENT = "INSUFFICIENT_DATA";

    private final PerformanceHistoryRepository performanceHistories;
    private final TrustHistoryRepository trustHistories;
    private final FreelancerProfileRepository freelancers;
    private final ProjectRepository projects;
    private final MilestoneRepository milestones;
    private final ReviewRepository reviews;
    private final ProjectActivityRepository activities;
    private final CurrentUserProvider current;

    public TrustScoreService(PerformanceHistoryRepository performanceHistories, TrustHistoryRepository trustHistories,
            FreelancerProfileRepository freelancers, ProjectRepository projects, MilestoneRepository milestones, ReviewRepository reviews,
            ProjectActivityRepository activities, CurrentUserProvider current) {
        this.performanceHistories=performanceHistories; this.trustHistories=trustHistories; this.freelancers=freelancers;
        this.projects=projects; this.milestones=milestones; this.reviews=reviews; this.activities=activities; this.current=current;
    }

    @Transactional
    public TrustScoreResponse currentFreelancerScore() {
        var principal=current.requireCurrentUser();
        if(principal.role()!=com.worklink.entity.enums.Role.FREELANCER) throw new AccessDeniedException("Freelancer role is required");
        FreelancerProfile freelancer=freelancers.findByUserId(principal.id()).orElseThrow(ProfileNotFoundException::new);
        List<PerformanceHistory> history=performanceHistories.findByFreelancerIdOrderByCompletionDateDesc(freelancer.getId()).stream()
                .filter(h->h.getProjectStatus()==ProjectStatus.COMPLETED).toList();
        LocalDate today=LocalDate.now();
        List<ProjectObservation> observations=history.stream().map(h->observe(h, freelancer, today)).toList();

        Double[] scores=new Double[6];
        List<Project> outcomeProjects=projects.findByAcceptedFreelancerId(freelancer.getId()).stream()
                .filter(p->p.getStatus()==ProjectStatus.COMPLETED||p.getStatus()==ProjectStatus.CANCELLED).toList();
        if(!outcomeProjects.isEmpty()) scores[0]=outcomeProjects.stream().filter(p->p.getStatus()==ProjectStatus.COMPLETED).count()*100.0/outcomeProjects.size();
        for(int i=1;i<5;i++) {
            final int component=i;
            scores[i]=weightedAverage(observations.stream().filter(o->o.values[component]!=null)
                    .map(o->new WeightedValue(o.values[component], recencyWeight(o.completionDate,today))).toList());
        }
        scores[5]=recentPerformance(observations,today);
        double availableWeight=0; for(int i=0;i<6;i++) if(scores[i]!=null) availableWeight+=WEIGHTS[i];
        double weightedTotal=0; for(int i=0;i<6;i++) if(scores[i]!=null) weightedTotal+=scores[i]*WEIGHTS[i];
        Double overall=availableWeight==0?null:round(weightedTotal/availableWeight);

        TrustHistory snapshot=new TrustHistory(); snapshot.setFreelancer(freelancer); snapshot.setOverallTrustScore(overall);
        snapshot.setCompletionReliabilityScore(scores[0]); snapshot.setDeadlineAdherenceScore(scores[1]); snapshot.setClientSatisfactionScore(scores[2]);
        snapshot.setMilestonePerformanceScore(scores[3]); snapshot.setCommunicationActivityScore(scores[4]); snapshot.setRecentPerformanceScore(scores[5]);
        snapshot.setProjectsConsidered(history.size()); snapshot.setCalculatedAt(LocalDateTime.now()); snapshot=trustHistories.save(snapshot);

        List<TrustScoreResponse.Component> components=new ArrayList<>();
        String[] names={"completionReliability","deadlineAdherence","clientSatisfaction","milestonePerformance","communicationActivity","recentPerformance"};
        String[] bases={"Completed accepted projects divided by completed or cancelled projects with an assigned freelancer; in-progress assignments are omitted.","Share of projects with known on-time outcome; projects without a deadline outcome are omitted.","Client review rating normalized from 1–5 (1→0, 5→100); projects without a client review are omitted.","Approved milestones divided by recorded milestones; projects without milestones are omitted.","Freelancer-authored project activities divided by all persisted project activities; this is participation only, not communication quality.","Recency-weighted mean of available per-project component scores; 180-day half-life, with project weights halving every 180 days."};
        for(int i=0;i<6;i++) components.add(new TrustScoreResponse.Component(names[i],WEIGHTS[i],scores[i],scores[i]==null?null:round(scores[i]*WEIGHTS[i]/availableWeight),scores[i]==null?INSUFFICIENT:AVAILABLE,bases[i]));
        boolean allComponentsAvailable=Arrays.stream(scores).allMatch(Objects::nonNull);
        String availability=history.isEmpty()?"NO_COMPLETED_PROJECTS":availableWeight==0?INSUFFICIENT:allComponentsAvailable?AVAILABLE:"PARTIAL_DATA";
        String explanation="Available component weights are renormalized to total 100%; missing components contribute no points. Recency weighting uses a 180-day half-life.";
        return new TrustScoreResponse(snapshot.getId(),overall,availability,history.size(),snapshot.getCalculatedAt(),components,explanation);
    }

    private ProjectObservation observe(PerformanceHistory h, FreelancerProfile freelancer, LocalDate today) {
        Double[] v=new Double[5]; Project p=h.getProject();
        v[0]=h.getProjectStatus()==ProjectStatus.COMPLETED?100.0:null;
        if(h.getOnTime()!=null) v[1]=h.getOnTime()?100.0:0.0;
        List<Review> projectReviews=reviews.findByProjectId(p.getId());
        Long freelancerUserId=freelancer.getUser().getId();
        projectReviews.stream().filter(r->r.getReviewee().getId().equals(freelancerUserId)
                && r.getReviewer().getId().equals(p.getClient().getUser().getId())).findFirst()
                .ifPresent(r->v[2]=clamp((r.getRating()-1)*25.0));
        List<Milestone> projectMilestones=milestones.findByProjectIdOrderByDeadlineAsc(p.getId());
        if(!projectMilestones.isEmpty()) v[3]=projectMilestones.stream().filter(m->"APPROVED".equals(m.getStatus().name())).count()*100.0/projectMilestones.size();
        List<ProjectActivity> projectActivities=activities.findByProjectIdOrderByCreatedAtAsc(p.getId());
        if(!projectActivities.isEmpty()) v[4]=projectActivities.stream().filter(a->a.getSender().getId().equals(freelancerUserId)).count()*100.0/projectActivities.size();
        return new ProjectObservation(h.getCompletionDate(),v);
    }

    private static Double recentPerformance(List<ProjectObservation> observations, LocalDate today) {
        List<WeightedValue> projectScores=new ArrayList<>();
        for(ProjectObservation o:observations) {
            double total=0; int count=0; for(Double score:o.values) if(score!=null){total+=score;count++;}
            if(count>0) projectScores.add(new WeightedValue(total/count,recencyWeight(o.completionDate,today)));
        }
        return weightedAverage(projectScores);
    }
    static double recencyWeight(LocalDate completed, LocalDate today) {
        if(completed==null) return 0;
        long days=Math.max(0,ChronoUnit.DAYS.between(completed,today));
        return Math.pow(0.5,days/HALF_LIFE_DAYS);
    }
    static Double weightedAverage(List<WeightedValue> values) {
        double weight=values.stream().mapToDouble(WeightedValue::weight).sum();
        if(weight==0) return null;
        return round(values.stream().mapToDouble(v->v.score*v.weight).sum()/weight);
    }
    static double clamp(double value){return Math.max(0,Math.min(100,value));}
    static double round(double value){return Math.round(value*100.0)/100.0;}
    record WeightedValue(double score,double weight) {}
    private record ProjectObservation(LocalDate completionDate,Double[] values) {}
}
