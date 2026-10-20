package com.liandrejohn.legalportal.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;
import com.liandrejohn.legalportal.security.AuthenticatedUser;

@RestController
@Tag(name = ApiTags.DASHBOARD)
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
class DashboardController {

    private final DashboardService dashboardService;

    /** Case counts by status and the five newest cases of the session's client. */
    @GetMapping("/summary")
    DashboardSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.getSummary(user.getId());
    }

}
