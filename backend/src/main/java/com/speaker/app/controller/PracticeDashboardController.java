package com.speaker.app.controller;

import com.speaker.app.dto.PracticeDashboardDto;
import com.speaker.app.service.intf.PracticeDashboardServiceIntf;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 练习数据看板 API，与 {@link PracticeController} 对练流程解耦。
 * 路径仍为 {@code /api/practice/stats/dashboard}，前端无需改动。
 */
@RestController
@RequestMapping("/api/practice/stats")
public class PracticeDashboardController {

    private final PracticeDashboardServiceIntf practiceDashboardService;

    public PracticeDashboardController(PracticeDashboardServiceIntf practiceDashboardService) {
        this.practiceDashboardService = practiceDashboardService;
    }

    @GetMapping("/dashboard")
    public PracticeDashboardDto dashboardStats() {
        return practiceDashboardService.getDashboardStats();
    }
}
