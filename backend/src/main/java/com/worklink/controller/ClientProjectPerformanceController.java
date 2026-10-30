package com.worklink.controller;
import com.worklink.dto.performance.PerformanceHistoryResponse;
import com.worklink.service.PerformanceHistoryService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/client/projects")
public class ClientProjectPerformanceController {
 private final PerformanceHistoryService service; public ClientProjectPerformanceController(PerformanceHistoryService service){this.service=service;}
 @GetMapping("/{projectId}/performance") public PerformanceHistoryResponse project(@PathVariable Long projectId){return service.forClientProject(projectId);}
}
