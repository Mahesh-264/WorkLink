package com.worklink.service;

import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.MatchingEvaluationRecord;
import com.worklink.entity.PerformanceHistory;
import com.worklink.entity.Project;
import com.worklink.entity.Proposal;
import com.worklink.entity.enums.MatchingOutcomeLabel;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.MatchingEvaluationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEvaluationServiceTest {
    @Mock MatchingEvaluationRecordRepository records;
    @InjectMocks MatchingEvaluationService service;

    @Test void recordsOnlyObservedCompletionAndDerivesFieldsFromPersistedHistory() {
        Project project = new Project(); project.setId(5L);
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(7L);
        Proposal proposal = new Proposal(); proposal.setId(9L);
        PerformanceHistory history = new PerformanceHistory();
        history.setProject(project); history.setFreelancer(freelancer); history.setAcceptedProposal(proposal);
        history.setProjectStatus(ProjectStatus.COMPLETED); history.setOnTime(true); history.setClientRating(5);
        history.setMilestoneCount(4); history.setApprovedMilestoneCount(3);
        when(records.findByProjectIdAndFreelancerId(5L, 7L)).thenReturn(Optional.empty());
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MatchingEvaluationRecord result = service.recordCompletedOutcome(history);

        assertThat(result.getProject()).isSameAs(project);
        assertThat(result.getFreelancer()).isSameAs(freelancer);
        assertThat(result.getAcceptedProposal()).isSameAs(proposal);
        assertThat(result.getOutcomeLabel()).isEqualTo(MatchingOutcomeLabel.POSITIVE_OUTCOME);
        assertThat(result.getProjectCompleted()).isTrue();
        assertThat(result.getCompletedOnTime()).isTrue();
        assertThat(result.getClientRating()).isEqualTo(5);
        assertThat(result.getMilestoneSuccessRate()).isEqualTo(75.0);
        assertThat(result.getEvaluatedAt()).isNotNull();
    }

    @Test void duplicateCaptureReturnsExistingRecordAndDoesNotDuplicate() {
        PerformanceHistory history = history(true, true);
        MatchingEvaluationRecord existing = new MatchingEvaluationRecord();
        when(records.findByProjectIdAndFreelancerId(5L, 7L)).thenReturn(Optional.of(existing));

        assertThat(service.recordCompletedOutcome(history)).isSameAs(existing);
        verify(records, never()).save(any());
    }

    @Test void nullHistoryDoesNotCreateFabricatedEvaluation() {
        assertThat(service.recordCompletedOutcome(null)).isNull();
        verifyNoInteractions(records);
    }

    @Test void outcomeStatesUseObservedCompletionAndDeadlineEvidence() {
        assertThat(MatchingEvaluationService.classify(null, null)).isEqualTo(MatchingOutcomeLabel.NOT_EVALUATED);
        assertThat(MatchingEvaluationService.classify(false, null)).isEqualTo(MatchingOutcomeLabel.NEGATIVE_OUTCOME);
        assertThat(MatchingEvaluationService.classify(true, true)).isEqualTo(MatchingOutcomeLabel.POSITIVE_OUTCOME);
        assertThat(MatchingEvaluationService.classify(true, false)).isEqualTo(MatchingOutcomeLabel.NEGATIVE_OUTCOME);
        assertThat(MatchingEvaluationService.classify(true, null)).isEqualTo(MatchingOutcomeLabel.INSUFFICIENT_DATA);
    }

    @Test void summaryCountsMultipleRecordsAndAveragesAvailableRatings() {
        MatchingEvaluationRecord positive = record(MatchingOutcomeLabel.POSITIVE_OUTCOME, true, true, 5);
        MatchingEvaluationRecord insufficient = record(MatchingOutcomeLabel.INSUFFICIENT_DATA, true, null, 3);
        when(records.findFiltered(null, null, null, null, null)).thenReturn(List.of(positive, insufficient));

        var summary = service.summary(null, null, null, null, null);

        assertThat(summary.totalRecords()).isEqualTo(2);
        assertThat(summary.positiveOutcomes()).isEqualTo(1);
        assertThat(summary.insufficientDataRecords()).isEqualTo(1);
        assertThat(summary.completedProjects()).isEqualTo(2);
        assertThat(summary.completedOnTimeProjects()).isEqualTo(1);
        assertThat(summary.averageClientRating()).isEqualTo(4.0);
    }

    @Test void filteringForwardsProjectFreelancerOutcomeAndInclusiveDates() {
        LocalDate fromDate = LocalDate.of(2026, 1, 2);
        LocalDate toDate = LocalDate.of(2026, 1, 5);
        when(records.findFiltered(eq(5L), eq(7L), eq(MatchingOutcomeLabel.POSITIVE_OUTCOME), any(), any())).thenReturn(List.of());

        assertThat(service.list(5L, 7L, MatchingOutcomeLabel.POSITIVE_OUTCOME, fromDate, toDate)).isEmpty();
        ArgumentCaptor<java.time.LocalDateTime> from = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        ArgumentCaptor<java.time.LocalDateTime> to = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        verify(records).findFiltered(eq(5L), eq(7L), eq(MatchingOutcomeLabel.POSITIVE_OUTCOME), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(fromDate.atStartOfDay());
        assertThat(to.getValue()).isEqualTo(toDate.plusDays(1).atStartOfDay().minusNanos(1));
    }

    private PerformanceHistory history(boolean completed, Boolean onTime) {
        PerformanceHistory h = new PerformanceHistory(); h.setProject(project()); h.setFreelancer(freelancer());
        h.setProjectStatus(completed ? ProjectStatus.COMPLETED : ProjectStatus.CANCELLED); h.setOnTime(onTime); return h;
    }
    private Project project() { Project p = new Project(); p.setId(5L); return p; }
    private FreelancerProfile freelancer() { FreelancerProfile f = new FreelancerProfile(); f.setId(7L); return f; }
    private MatchingEvaluationRecord record(MatchingOutcomeLabel label, Boolean completed, Boolean onTime, Integer rating) {
        MatchingEvaluationRecord r = new MatchingEvaluationRecord(); r.setOutcomeLabel(label); r.setProjectCompleted(completed); r.setCompletedOnTime(onTime); r.setClientRating(rating); return r;
    }
}
