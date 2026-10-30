package com.worklink.repository;

import com.worklink.entity.Proposal;
import com.worklink.entity.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProjectId(Long projectId);
    List<Proposal> findByFreelancerId(Long freelancerId);
    Optional<Proposal> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    boolean existsByProjectIdAndFreelancerIdAndStatusIn(Long projectId, Long freelancerId, List<ProposalStatus> statuses);
    List<Proposal> findByProjectIdAndStatus(Long projectId, ProposalStatus status);
}
