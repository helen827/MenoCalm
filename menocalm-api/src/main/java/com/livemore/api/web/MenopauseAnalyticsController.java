package com.livemore.api.web;

import com.livemore.api.service.MenopauseWeeklyReportService;
import com.livemore.api.web.dto.WeeklyReportMarkdownDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
public class MenopauseAnalyticsController {

    private final MenopauseWeeklyReportService weeklyReportService;

    public MenopauseAnalyticsController(MenopauseWeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @PostMapping("/weekly-report")
    public WeeklyReportMarkdownDto weeklyReport(
            @RequestParam("userId") String userId,
            Authentication authentication
    ) {
        return weeklyReportService.buildWeeklyMarkdown(authentication.getName(), userId);
    }
}
