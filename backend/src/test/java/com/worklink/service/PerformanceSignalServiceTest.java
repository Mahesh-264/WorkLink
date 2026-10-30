package com.worklink.service;

import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.PerformanceHistory;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.repository.PerformanceHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PerformanceSignalServiceTest {
    private final PerformanceHistoryRepository histories = mock(PerformanceHistoryRepository.class);
    private final PerformanceSignalService service = new PerformanceSignalService(histories);

    @Test void summarizesOnlyPersistedOutcomeEvidenceAndBatchesFreelancers() {
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(17L);
        PerformanceHistory row = new PerformanceHistory(); row.setFreelancer(freelancer);
        row.setProjectStatus(ProjectStatus.COMPLETED); row.setCompletionDate(LocalDate.now());
        row.setOnTime(true); row.setMilestoneCount(4); row.setApprovedMilestoneCount(3); row.setClientRating(5);
        when(histories.findByFreelancerIdIn(List.of(17L))).thenReturn(List.of(row));

        var result = service.forFreelancers(List.of(17L)).get(17L);

        // Mean of completed, on-time, milestone (75), and rating (100) observations.
        assertThat(result.score()).isEqualTo(93.75);
        assertThat(result.availability()).isEqualTo("AVAILABLE");
        assertThat(result.projectsConsidered()).isEqualTo(1);
        verify(histories).findByFreelancerIdIn(List.of(17L));
    }

    @Test void missingOutcomeEvidenceRemainsUnavailable() {
        FreelancerProfile freelancer = new FreelancerProfile(); freelancer.setId(18L);
        PerformanceHistory row = new PerformanceHistory(); row.setFreelancer(freelancer);
        row.setProjectStatus(ProjectStatus.IN_PROGRESS);
        when(histories.findByFreelancerIdIn(List.of(18L))).thenReturn(List.of(row));

        var result = service.forFreelancers(List.of(18L)).get(18L);

        assertThat(result.score()).isNull();
        assertThat(result.availability()).isEqualTo("UNAVAILABLE");
        assertThat(result.projectsConsidered()).isEqualTo(1);
    }
}
