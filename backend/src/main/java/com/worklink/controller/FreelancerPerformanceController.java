package com.worklink.controller;
import com.worklink.dto.performance.PerformanceHistoryResponse;
import com.worklink.service.PerformanceHistoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/freelancer/performance")
public class FreelancerPerformanceController {
 private final PerformanceHistoryService service; public FreelancerPerformanceController(PerformanceHistoryService service){this.service=service;}
 @GetMapping public List<PerformanceHistoryResponse> mine(){return service.mine();}
 @GetMapping("/{performanceId}") public PerformanceHistoryResponse mine(@PathVariable Long performanceId){return service.mine(performanceId);}
}
