package com.worklink.controller;

import com.worklink.dto.evaluation.MatchingEvaluationRecordResponse;
import com.worklink.dto.evaluation.MatchingEvaluationSummaryResponse;
import com.worklink.entity.enums.MatchingOutcomeLabel;
import com.worklink.service.MatchingEvaluationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation/matching")
@PreAuthorize("hasRole('ADMIN')")
public class MatchingEvaluationController {
    private final MatchingEvaluationService service;

    public MatchingEvaluationController(MatchingEvaluationService service) {
        this.service = service;
    }

    @GetMapping
    public List<MatchingEvaluationRecordResponse> records(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long freelancerId,
            @RequestParam(required = false) MatchingOutcomeLabel outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDates(from, to);
        return service.list(projectId, freelancerId, outcome, from, to);
    }

    @GetMapping("/summary")
    public MatchingEvaluationSummaryResponse summary(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long freelancerId,
            @RequestParam(required = false) MatchingOutcomeLabel outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDates(from, to);
        return service.summary(projectId, freelancerId, outcome, from, to);
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
    }
}
