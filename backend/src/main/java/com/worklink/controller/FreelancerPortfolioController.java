package com.worklink.controller;

import com.worklink.dto.capability.PortfolioProjectRequest;
import com.worklink.dto.capability.PortfolioProjectResponse;
import com.worklink.service.FreelancerCapabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/freelancer/portfolio") @PreAuthorize("hasRole('FREELANCER')")
public class FreelancerPortfolioController {
    private final FreelancerCapabilityService service;
    public FreelancerPortfolioController(FreelancerCapabilityService service) { this.service = service; }
    @GetMapping public List<PortfolioProjectResponse> getPortfolio() { return service.getCurrentPortfolio(); }
    @PostMapping public ResponseEntity<PortfolioProjectResponse> create(@Valid @RequestBody PortfolioProjectRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createPortfolioItem(request)); }
    @PutMapping("/{portfolioId}") public PortfolioProjectResponse update(@PathVariable Long portfolioId, @Valid @RequestBody PortfolioProjectRequest request) { return service.updatePortfolioItem(portfolioId, request); }
    @DeleteMapping("/{portfolioId}") public ResponseEntity<Void> delete(@PathVariable Long portfolioId) { service.deletePortfolioItem(portfolioId); return ResponseEntity.noContent().build(); }
}
