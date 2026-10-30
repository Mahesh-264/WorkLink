package com.worklink.repository;
import com.worklink.entity.TrustHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
public interface TrustHistoryRepository extends JpaRepository<TrustHistory, Long> {
    Optional<TrustHistory> findFirstByFreelancerIdOrderByCalculatedAtDesc(Long freelancerId);
    Optional<TrustHistory> findTopByFreelancerIdOrderByCalculatedAtDescIdDesc(Long freelancerId);
    List<TrustHistory> findByFreelancerIdInOrderByCalculatedAtDescIdDesc(Collection<Long> freelancerIds);
}
