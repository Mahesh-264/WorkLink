package com.worklink.service;

import com.worklink.dto.proposal.ProposalRequest;
import com.worklink.dto.proposal.ProposalResponse;
import com.worklink.dto.proposal.ProposalStatusRequest;
import com.worklink.entity.ClientProfile;
import com.worklink.entity.FreelancerProfile;
import com.worklink.entity.PortfolioProject;
import com.worklink.entity.Project;
import com.worklink.entity.Proposal;
import com.worklink.entity.User;
import com.worklink.entity.enums.ProjectStatus;
import com.worklink.entity.enums.ProposalStatus;
import com.worklink.exception.DuplicateResourceException;
import com.worklink.exception.ProfileNotFoundException;
import com.worklink.exception.ResourceNotFoundException;
import com.worklink.repository.ClientProfileRepository;
import com.worklink.repository.FreelancerProfileRepository;
import com.worklink.repository.PortfolioProjectRepository;
import com.worklink.repository.ProjectRepository;
import com.worklink.repository.ProposalRepository;
import com.worklink.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProposalService {
    private final ProposalRepository proposals;
    private final ProjectRepository projects;
    private final FreelancerProfileRepository freelancers;
    private final ClientProfileRepository clients;
    private final PortfolioProjectRepository portfolioProjects;
    private final CurrentUserProvider current;

    private static final List<ProposalStatus> ACTIVE_STATUSES = List.of(
            ProposalStatus.SUBMITTED,
            ProposalStatus.UNDER_REVIEW,
            ProposalStatus.SHORTLISTED,
            ProposalStatus.ACCEPTED
    );

    public ProposalService(
            ProposalRepository proposals,
            ProjectRepository projects,
            FreelancerProfileRepository freelancers,
            ClientProfileRepository clients,
            PortfolioProjectRepository portfolioProjects,
            CurrentUserProvider current) {
        this.proposals = proposals;
        this.projects = projects;
        this.freelancers = freelancers;
        this.clients = clients;
        this.portfolioProjects = portfolioProjects;
        this.current = current;
    }

    private FreelancerProfile me() {
        return freelancers.findByUserId(current.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);
    }

    private ClientProfile client() {
        return clients.findByUserId(current.requireCurrentUser().id())
                .orElseThrow(ProfileNotFoundException::new);
    }

    @Transactional
    public ProposalResponse create(Long projectId, ProposalRequest request) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        FreelancerProfile freelancer = me();

        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published projects can receive proposals");
        }
        if (project.getClient().getUser().getId().equals(freelancer.getUser().getId())) {
            throw new IllegalArgumentException("You cannot propose to your own project");
        }

        if (proposals.existsByProjectIdAndFreelancerIdAndStatusIn(projectId, freelancer.getId(), ACTIVE_STATUSES)) {
            throw new DuplicateResourceException("An active proposal already exists for this project");
        }

        PortfolioProject portfolio = null;
        if (request.portfolioProjectId() != null) {
            portfolio = portfolioProjects.findById(request.portfolioProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Portfolio project not found"));
            if (!portfolio.getFreelancer().getId().equals(freelancer.getId())) {
                throw new IllegalArgumentException("Portfolio project does not belong to you");
            }
        }

        Proposal proposal = new Proposal(
                project,
                freelancer,
                request.coverLetter().trim(),
                request.proposedBudget(),
                request.estimatedDuration(),
                ProposalStatus.SUBMITTED,
                portfolio
        );

        return map(proposals.save(proposal));
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> mine() {
        return proposals.findByFreelancerId(me().getId()).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProposalResponse mine(Long id) {
        Proposal p = proposals.findById(id)
                .filter(x -> x.getFreelancer().getId().equals(me().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        return map(p);
    }

    @Transactional
    public ProposalResponse withdraw(Long id) {
        Proposal p = proposals.findById(id)
                .filter(x -> x.getFreelancer().getId().equals(me().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (p.getStatus() == ProposalStatus.ACCEPTED || p.getStatus() == ProposalStatus.REJECTED || p.getStatus() == ProposalStatus.WITHDRAWN) {
            throw new IllegalArgumentException("This proposal cannot be withdrawn in its current state");
        }
        
        p.setStatus(ProposalStatus.WITHDRAWN);
        return map(proposals.save(p));
    }

    private Project owned(Long projectId) {
        return projects.findByIdAndClientId(projectId, client().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> project(Long id) {
        owned(id); // validates ownership
        return proposals.findByProjectId(id).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProposalResponse project(Long projectId, Long id) {
        owned(projectId);
        Proposal p = proposals.findById(id)
                .filter(x -> x.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        return map(p);
    }

    @Transactional
    public ProposalResponse status(Long projectId, Long id, ProposalStatusRequest request) {
        Project lockedProject = projects.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!lockedProject.getClient().getId().equals(client().getId())) {
            throw new ResourceNotFoundException("Project not found");
        }
        Proposal proposal = proposals.findById(id)
                .filter(x -> x.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (request.status() == ProposalStatus.SUBMITTED || request.status() == ProposalStatus.WITHDRAWN) {
            throw new IllegalArgumentException("Clients cannot set this status");
        }

        ProposalStatus currentStatus = proposal.getStatus();

        // Valid transitions check
        boolean valid = false;
        switch (currentStatus) {
            case SUBMITTED:
                valid = (request.status() == ProposalStatus.UNDER_REVIEW || request.status() == ProposalStatus.SHORTLISTED || request.status() == ProposalStatus.ACCEPTED || request.status() == ProposalStatus.REJECTED);
                break;
            case UNDER_REVIEW:
                valid = (request.status() == ProposalStatus.SHORTLISTED || request.status() == ProposalStatus.ACCEPTED || request.status() == ProposalStatus.REJECTED);
                break;
            case SHORTLISTED:
                valid = (request.status() == ProposalStatus.ACCEPTED || request.status() == ProposalStatus.REJECTED);
                break;
            default:
                break;
        }

        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + request.status());
        }

        if (request.status() == ProposalStatus.ACCEPTED) {
            if (lockedProject.getAcceptedFreelancer() != null || lockedProject.getStatus() != ProjectStatus.PUBLISHED) {
                throw new IllegalStateException("An accepted proposal already exists for this project");
            }
            lockedProject.setAcceptedFreelancer(proposal.getFreelancer());
            lockedProject.setStatus(ProjectStatus.IN_PROGRESS);
            projects.save(lockedProject);
            for (Proposal other : proposals.findByProjectId(projectId)) {
                if (!other.getId().equals(proposal.getId()) && ACTIVE_STATUSES.contains(other.getStatus())) {
                    other.setStatus(ProposalStatus.REJECTED);
                }
            }
        }

        proposal.setStatus(request.status());
        return map(proposals.save(proposal));
    }

    private ProposalResponse map(Proposal p) {
        User u = p.getFreelancer().getUser();
        Long portfolioId = p.getPortfolioReference() != null ? p.getPortfolioReference().getId() : null;
        String portfolioTitle = p.getPortfolioReference() != null ? p.getPortfolioReference().getTitle() : null;
        return new ProposalResponse(
                p.getId(),
                p.getProject().getId(),
                p.getProject().getTitle(),
                p.getFreelancer().getId(),
                u.getFirstName() + " " + u.getLastName(),
                p.getCoverLetter(),
                p.getProposedAmount(),
                p.getEstimatedDays(),
                p.getStatus().name(),
                portfolioId,
                portfolioTitle,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
