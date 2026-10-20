package com.liandrejohn.legalportal.dashboard;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.cases.CaseStatus;
import com.liandrejohn.legalportal.cases.LegalCaseRepository;
import com.liandrejohn.legalportal.cases.LegalCaseRepository.StatusCount;
import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.security.AuthenticationRequiredException;

@Service
@RequiredArgsConstructor
class DashboardService {

    private final LegalCaseRepository legalCaseRepository;

    private final ClientProfileRepository clientProfileRepository;

    /**
     * Counts the client's own non-deleted cases by status and lists the five newest. Accounts deleted, suspended or
     * disabled after login are rejected with 401, as for case endpoints.
     */
    @Transactional(readOnly = true)
    DashboardSummaryResponse getSummary(UUID userId) {
        ClientProfile client = clientProfileRepository.findActiveByUserId(userId)
            .orElseThrow(AuthenticationRequiredException::new);

        Map<CaseStatus, Long> counts = new EnumMap<>(CaseStatus.class);
        for (StatusCount count : legalCaseRepository.countByStatus(client.getId())) {
            counts.put(count.getStatus(), count.getCount());
        }
        List<DashboardSummaryResponse.RecentCase> recentCases = legalCaseRepository
            .findTop5ByClientIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(client.getId())
            .stream()
            .map(DashboardSummaryResponse.RecentCase::from)
            .toList();
        return new DashboardSummaryResponse(DashboardSummaryResponse.Statistics.from(counts), recentCases);
    }

}
