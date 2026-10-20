package com.liandrejohn.legalportal.cases;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import com.liandrejohn.legalportal.category.LegalCategory;
import com.liandrejohn.legalportal.client.ClientProfile;

/** A client's legal case. Named {@code LegalCase} because {@code case} is a Java keyword. */
@Entity
@Table(name = "cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalCase {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private ClientProfile client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private LegalCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Starts a new draft owned by {@code client}. */
    public static LegalCase draft(ClientProfile client, LegalCategory category, String title, String description,
            String location) {
        LegalCase legalCase = new LegalCase();
        legalCase.client = client;
        legalCase.category = category;
        legalCase.title = title;
        legalCase.description = description;
        legalCase.location = location;
        return legalCase;
    }

    /** Replaces the draft's editable fields. Callers check that the case is still a draft. */
    public void editDraft(LegalCategory category, String title, String description, String location) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
    }

    /** Moves the draft to {@code SUBMITTED}. Callers check that it is a complete draft. */
    public void submit() {
        status = CaseStatus.SUBMITTED;
        submittedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    /** Moves the submitted case to {@code CLOSED}. Callers check that it is submitted. */
    public void close() {
        status = CaseStatus.CLOSED;
        closedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    /** Soft-deletes the case; the row stays and owner lookups stop finding it. Callers check that it is a draft. */
    public void softDelete() {
        deletedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Sets both timestamps from one clock reading, truncated to PostgreSQL's microsecond precision, so a new case's
     * {@code createdAt} and {@code updatedAt} are equal and match what is read back later.
     */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

}
