package com.worklink.controller;
import com.worklink.dto.trust.TrustScoreResponse;
import com.worklink.service.TrustScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/freelancer/trust-score")
public class FreelancerTrustScoreController {
    private final TrustScoreService service;
    public FreelancerTrustScoreController(TrustScoreService service){this.service=service;}
    @GetMapping public TrustScoreResponse get(){return service.currentFreelancerScore();}
}
