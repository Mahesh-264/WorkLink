package com.worklink.service;

import com.worklink.entity.*;
import com.worklink.entity.enums.*;
import com.worklink.repository.*;
import com.worklink.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {
    @Mock PerformanceHistoryRepository performance; @Mock TrustHistoryRepository trustHistory; @Mock FreelancerProfileRepository freelancers;
    @Mock ProjectRepository projects;
    @Mock MilestoneRepository milestones; @Mock ReviewRepository reviews; @Mock ProjectActivityRepository activities; @Mock CurrentUserProvider current;
    @InjectMocks TrustScoreService service;

    @Test void calculatesWeightedComponentsAndPersistsExplainableSnapshot() {
        Fixture f=fixture(5L); when(freelancers.findByUserId(77L)).thenReturn(Optional.of(f.freelancer)); when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(77L,"freelancer@test.com",Role.FREELANCER));
        when(performance.findByFreelancerIdOrderByCompletionDateDesc(5L)).thenReturn(List.of(f.history));
        when(projects.findByAcceptedFreelancerId(5L)).thenReturn(List.of(f.project));
        when(reviews.findByProjectId(11L)).thenReturn(List.of(new Review(f.project,f.clientUser,f.freelancerUser,4,"Good")));
        when(milestones.findByProjectIdOrderByDeadlineAsc(11L)).thenReturn(List.of(milestone(f.project,MilestoneStatus.APPROVED),milestone(f.project,MilestoneStatus.PENDING)));
        when(activities.findByProjectIdOrderByCreatedAtAsc(11L)).thenReturn(List.of(activity(f.project,f.freelancerUser),activity(f.project,f.clientUser)));
        when(trustHistory.save(any())).thenAnswer(i->{TrustHistory h=i.getArgument(0);h.setId(99L);return h;});

        var response=service.currentFreelancerScore();

        assertThat(response.overallTrustScore()).isEqualTo(58.0);
        assertThat(response.availability()).isEqualTo("AVAILABLE"); assertThat(response.projectsConsidered()).isEqualTo(1);
        assertThat(response.components()).extracting("score").containsExactly(100.0,0.0,75.0,50.0,50.0,55.0);
        assertThat(response.components()).extracting("contribution").containsExactly(25.0,0.0,15.0,7.5,5.0,5.5);
        ArgumentCaptor<TrustHistory> saved=ArgumentCaptor.forClass(TrustHistory.class); verify(trustHistory).save(saved.capture());
        assertThat(saved.getValue().getFreelancer()).isSameAs(f.freelancer); assertThat(saved.getValue().getOverallTrustScore()).isEqualTo(58.0); assertThat(saved.getValue().getCalculatedAt()).isNotNull();
        verify(freelancers).findByUserId(77L);
    }

    @Test void missingComponentsAreOmittedAndAvailableWeightsRenormalized() {
        Fixture f=fixture(5L); f.history.setOnTime(null); when(freelancers.findByUserId(77L)).thenReturn(Optional.of(f.freelancer)); when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(77L,"freelancer@test.com",Role.FREELANCER));
        when(performance.findByFreelancerIdOrderByCompletionDateDesc(5L)).thenReturn(List.of(f.history));
        when(projects.findByAcceptedFreelancerId(5L)).thenReturn(List.of(f.project));
        when(reviews.findByProjectId(11L)).thenReturn(List.of()); when(milestones.findByProjectIdOrderByDeadlineAsc(11L)).thenReturn(List.of()); when(activities.findByProjectIdOrderByCreatedAtAsc(11L)).thenReturn(List.of());
        when(trustHistory.save(any())).thenAnswer(i->i.getArgument(0));
        var response=service.currentFreelancerScore();
        assertThat(response.components().get(1).score()).isNull(); assertThat(response.components().get(1).availability()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(response.availability()).isEqualTo("PARTIAL_DATA");
        assertThat(response.components().get(0).contribution()).isEqualTo(71.43); assertThat(response.components().get(5).contribution()).isEqualTo(28.57);
        assertThat(response.overallTrustScore()).isEqualTo(100.0);
    }

    @Test void noCompletedProjectsReturnsExplicitUnavailableScoreAndStoresSnapshot() {
        when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(77L,"freelancer@test.com",Role.FREELANCER));
        FreelancerProfile freelancer=new FreelancerProfile(); freelancer.setId(5L); when(freelancers.findByUserId(77L)).thenReturn(Optional.of(freelancer));
        when(performance.findByFreelancerIdOrderByCompletionDateDesc(5L)).thenReturn(List.of()); when(projects.findByAcceptedFreelancerId(5L)).thenReturn(List.of()); when(trustHistory.save(any())).thenAnswer(i->i.getArgument(0));
        var response=service.currentFreelancerScore();
        assertThat(response.overallTrustScore()).isNull(); assertThat(response.availability()).isEqualTo("NO_COMPLETED_PROJECTS"); assertThat(response.projectsConsidered()).isZero();
        verify(trustHistory).save(argThat(s->s.getFreelancer()==freelancer&&s.getOverallTrustScore()==null&&s.getProjectsConsidered()==0));
    }

    @Test void recencyUsesHalfLifeAndRecentOutcomeHasGreaterInfluence() {
        LocalDate today=LocalDate.now();
        assertThat(TrustScoreService.recencyWeight(today.minusDays(180),today)).isEqualTo(0.5);
        assertThat(TrustScoreService.weightedAverage(List.of(new TrustScoreService.WeightedValue(100,TrustScoreService.recencyWeight(today,today)),new TrustScoreService.WeightedValue(0,TrustScoreService.recencyWeight(today.minusDays(180),today))))).isEqualTo(66.67);
    }

    @Test void multipleCompletedProjectsAreIncluded() {
        Fixture f=fixture(5L); f.history.setOnTime(true); PerformanceHistory older=fixture(5L).history; older.setProjectStatus(ProjectStatus.COMPLETED); older.setCompletionDate(LocalDate.now().minusDays(180)); older.setOnTime(false);
        when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(77L,"freelancer@test.com",Role.FREELANCER)); when(freelancers.findByUserId(77L)).thenReturn(Optional.of(f.freelancer));
        when(performance.findByFreelancerIdOrderByCompletionDateDesc(5L)).thenReturn(List.of(f.history,older));
        when(projects.findByAcceptedFreelancerId(5L)).thenReturn(List.of(f.project,older.getProject()));
        when(reviews.findByProjectId(anyLong())).thenReturn(List.of()); when(milestones.findByProjectIdOrderByDeadlineAsc(anyLong())).thenReturn(List.of()); when(activities.findByProjectIdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
        when(trustHistory.save(any())).thenAnswer(i->i.getArgument(0));
        var response=service.currentFreelancerScore();
        assertThat(response.projectsConsidered()).isEqualTo(2); assertThat(response.components().get(1).score()).isEqualTo(66.67);
    }

    @Test void cancelledAcceptedOutcomeReducesCompletionReliability() {
        Fixture f=fixture(5L); Project cancelled=new Project(); cancelled.setId(12L); cancelled.setAcceptedFreelancer(f.freelancer); cancelled.setStatus(ProjectStatus.CANCELLED);
        when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(77L,"freelancer@test.com",Role.FREELANCER)); when(freelancers.findByUserId(77L)).thenReturn(Optional.of(f.freelancer));
        when(performance.findByFreelancerIdOrderByCompletionDateDesc(5L)).thenReturn(List.of(f.history)); when(projects.findByAcceptedFreelancerId(5L)).thenReturn(List.of(f.project,cancelled));
        when(reviews.findByProjectId(anyLong())).thenReturn(List.of()); when(milestones.findByProjectIdOrderByDeadlineAsc(anyLong())).thenReturn(List.of()); when(activities.findByProjectIdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
        when(trustHistory.save(any())).thenAnswer(i->i.getArgument(0));
        var response=service.currentFreelancerScore();
        assertThat(response.components().get(0).score()).isEqualTo(50.0);
    }

    @Test void nonFreelancerPrincipalCannotReadFreelancerTrustScore() {
        when(current.requireCurrentUser()).thenReturn(new AuthenticatedUser(88L,"client@test.com",Role.CLIENT));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->service.currentFreelancerScore()).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(freelancers,performance,trustHistory);
    }

    private static Fixture fixture(Long freelancerId) {
        User freelancerUser=new User("Free","Lancer","freelancer@test.com","p",Role.FREELANCER,AccountStatus.ACTIVE); freelancerUser.setId(77L);
        User clientUser=new User("Client","One","client@test.com","p",Role.CLIENT,AccountStatus.ACTIVE); clientUser.setId(88L);
        FreelancerProfile freelancer=new FreelancerProfile(); freelancer.setId(freelancerId); freelancer.setUser(freelancerUser);
        ClientProfile client=new ClientProfile(); client.setUser(clientUser);
        Project project=new Project(); project.setId(11L); project.setClient(client); project.setAcceptedFreelancer(freelancer); project.setStatus(ProjectStatus.COMPLETED);
        PerformanceHistory history=new PerformanceHistory(); history.setFreelancer(freelancer); history.setProject(project); history.setProjectStatus(ProjectStatus.COMPLETED); history.setCompletionDate(LocalDate.now()); history.setOnTime(false);
        return new Fixture(freelancerUser,clientUser,freelancer,project,history);
    }
    private static Milestone milestone(Project p,MilestoneStatus status){Milestone m=new Milestone();m.setProject(p);m.setStatus(status);return m;}
    private static ProjectActivity activity(Project p,User sender){ProjectActivity a=new ProjectActivity();a.setProject(p);a.setSender(sender);return a;}
    private record Fixture(User freelancerUser,User clientUser,FreelancerProfile freelancer,Project project,PerformanceHistory history){}
}
