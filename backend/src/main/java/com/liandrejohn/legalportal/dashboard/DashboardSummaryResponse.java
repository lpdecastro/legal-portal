package com.liandrejohn.legalportal.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.liandrejohn.legalportal.cases.CaseStatus;
import com.liandrejohn.legalportal.cases.LegalCase;

/** The client's case counts by status and their newest cases. */
public record DashboardSummaryResponse(Statistics statistics, List<RecentCase> recentCases) {

    public record Statistics(long total, long draft, long submitted, long closed) {

        /** Statuses missing from {@code counts} count as zero. */
        static Statistics from(Map<CaseStatus, Long> counts) {
            long draft = counts.getOrDefault(CaseStatus.DRAFT, 0L);
            long submitted = counts.getOrDefault(CaseStatus.SUBMITTED, 0L);
            long closed = counts.getOrDefault(CaseStatus.CLOSED, 0L);
            return new Statistics(draft + submitted + closed, draft, submitted, closed);
        }

    }

    public record RecentCase(UUID id, String title, CaseStatus status, Instant createdAt) {

        static RecentCase from(LegalCase legalCase) {
            return new RecentCase(legalCase.getId(), legalCase.getTitle(), legalCase.getStatus(),
                    legalCase.getCreatedAt());
        }

    }

}
