package com.worklink.service;

import com.worklink.entity.*;
import com.worklink.entity.enums.*;
import com.worklink.repository.*;
import com.worklink.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceHistoryServiceTest {
 @Mock PerformanceHistoryRepository histories; @Mock FreelancerProfileRepository freelancers; @Mock ProjectRepository projects;
 @Mock ProposalRepository proposals; @Mock MilestoneRepository milestones; @Mock ProjectActivityRepository activities; @Mock ReviewRepository reviews; @Mock CurrentUserProvider current;
 @Mock MatchingEvaluationService matchingEvaluationService;
 @InjectMocks PerformanceHistoryService service;

 @Test void createsOutcomeRecordFromCompletedProjectAndIsIdempotent(){
  var freelancerUser=new User("Free","Lancer","f@x.com","p",Role.FREELANCER,AccountStatus.ACTIVE); freelancerUser.setId(2L);
  var freelancer=new FreelancerProfile(); freelancer.setId(20L); freelancer.setUser(freelancerUser);
  var clientUser=new User("Client","One","c@x.com","p",Role.CLIENT,AccountStatus.ACTIVE); clientUser.setId(1L);
  var client=new ClientProfile(); client.setUser(clientUser);
  var project=new Project(); project.setId(10L); project.setStatus(ProjectStatus.COMPLETED); project.setClient(client); project.setAcceptedFreelancer(freelancer); project.setDeadline(LocalDate.now());
  var proposal=new Proposal(); proposal.setId(30L); proposal.setProject(project); proposal.setFreelancer(freelancer); proposal.setStatus(ProposalStatus.ACCEPTED);
  when(histories.findByProjectId(10L)).thenReturn(Optional.empty(), Optional.of(new PerformanceHistory()));
  when(proposals.findByProjectIdAndStatus(10L,ProposalStatus.ACCEPTED)).thenReturn(List.of(proposal));
  when(milestones.findByProjectIdOrderByDeadlineAsc(10L)).thenReturn(List.of()); when(activities.findByProjectIdOrderByCreatedAtAsc(10L)).thenReturn(List.of()); when(reviews.findByProjectId(10L)).thenReturn(List.of());
  when(histories.save(any())).thenAnswer(i->i.getArgument(0));
  var created=service.createForCompletedProject(project); var second=service.createForCompletedProject(project);
  assertThat(created.getFreelancer()).isSameAs(freelancer); assertThat(created.getAcceptedProposal()).isSameAs(proposal); assertThat(created.getProjectStatus()).isEqualTo(ProjectStatus.COMPLETED); assertThat(created.getOnTime()).isTrue(); assertThat(second).isNotNull();
  verify(histories,times(1)).save(any());
  verify(matchingEvaluationService,times(1)).recordCompletedOutcome(any());
 }
}
