package com.worklink.repository;
import com.worklink.entity.PerformanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
public interface PerformanceHistoryRepository extends JpaRepository<PerformanceHistory, Long> {
    List<PerformanceHistory> findByFreelancerIdOrderByCompletionDateDesc(Long freelancerId);
    Optional<PerformanceHistory> findByIdAndFreelancerId(Long id, Long freelancerId);
    Optional<PerformanceHistory> findByProjectId(Long projectId);
    boolean existsByFreelancerIdAndProjectId(Long freelancerId, Long projectId);
    List<PerformanceHistory> findByFreelancerIdIn(Collection<Long> freelancerIds);
}
