package com.liandrejohn.legalportal.cases;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.category.LegalCategory;
import com.liandrejohn.legalportal.category.LegalCategoryRepository;
import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.error.FieldValidationException;
import com.liandrejohn.legalportal.security.AuthenticationRequiredException;
import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserStatus;

@Service
@RequiredArgsConstructor
class CaseService {

    private final LegalCaseRepository legalCaseRepository;

    private final ClientProfileRepository clientProfileRepository;

    private final LegalCategoryRepository legalCategoryRepository;

    /** Creates a draft owned by the session's client. */
    @Transactional
    CaseResponse createDraft(UUID userId, CreateCaseRequest request) {
        ClientProfile client = findActiveClient(userId);
        LegalCategory category = findActiveCategory(request.categoryId());

        LegalCase draft = LegalCase.draft(client, category, request.title(), request.description(),
                request.location());
        return CaseResponse.from(legalCaseRepository.save(draft));
    }

    /** Returns one of the client's own non-deleted cases, in any status. */
    @Transactional(readOnly = true)
    CaseResponse getCase(UUID userId, UUID caseId) {
        ClientProfile client = findActiveClient(userId);
        return legalCaseRepository.findByIdAndClientIdAndDeletedAtIsNull(caseId, client.getId())
            .map(CaseResponse::from)
            .orElseThrow(CaseNotFoundException::new);
    }

    /**
     * Applies the fields present in {@code request} to one of the client's own drafts. Ownership is checked before
     * status, so another client's case is a 404 whatever its status, and the category is only looked up for a draft.
     */
    @Transactional
    CaseResponse updateDraft(UUID userId, UUID caseId, UpdateCaseRequest request) {
        ClientProfile client = findActiveClient(userId);
        LegalCase legalCase = legalCaseRepository.findByIdAndClientIdAndDeletedAtIsNull(caseId, client.getId())
            .orElseThrow(CaseNotFoundException::new);
        if (legalCase.getStatus() != CaseStatus.DRAFT) {
            throw new InvalidCaseStateException("Only draft cases can be edited.");
        }

        // Bean Validation has already rejected an empty categoryId
        LegalCategory category = request.getCategoryId() != null
                ? findActiveCategory(request.getCategoryId().orElseThrow()) : legalCase.getCategory();
        legalCase.editDraft(category, valueOrCurrent(request.getTitle(), legalCase.getTitle()),
                valueOrCurrent(request.getDescription(), legalCase.getDescription()),
                valueOrCurrent(request.getLocation(), legalCase.getLocation()));
        // Flushing runs @PreUpdate, so the response carries the new updatedAt
        return CaseResponse.from(legalCaseRepository.saveAndFlush(legalCase));
    }

    /** Soft-deletes one of the client's own drafts. Ownership is checked before status, as for updates. */
    @Transactional
    void deleteDraft(UUID userId, UUID caseId) {
        ClientProfile client = findActiveClient(userId);
        LegalCase legalCase = legalCaseRepository.findByIdAndClientIdAndDeletedAtIsNull(caseId, client.getId())
            .orElseThrow(CaseNotFoundException::new);
        if (legalCase.getStatus() != CaseStatus.DRAFT) {
            throw new InvalidCaseStateException("Only draft cases can be deleted.");
        }
        legalCase.softDelete();
    }

    /** Unknown and inactive categories get the same message, so IDs of hidden categories aren't revealed. */
    private LegalCategory findActiveCategory(UUID categoryId) {
        return legalCategoryRepository.findByIdAndActiveTrue(categoryId)
            .orElseThrow(() -> new FieldValidationException("categoryId", "Select an active legal category"));
    }

    /** An omitted field ({@code null}) keeps its current value; an empty one clears it. */
    private static <T> T valueOrCurrent(Optional<T> field, T current) {
        return field != null ? field.orElse(null) : current;
    }

    /** Accounts deleted, suspended or disabled after login can't act on cases. */
    private ClientProfile findActiveClient(UUID userId) {
        return clientProfileRepository.findByUserId(userId).filter((profile) -> {
            User user = profile.getUser();
            return user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE;
        }).orElseThrow(AuthenticationRequiredException::new);
    }

}
