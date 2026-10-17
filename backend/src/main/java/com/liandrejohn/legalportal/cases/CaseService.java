package com.liandrejohn.legalportal.cases;

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
        // Unknown and inactive categories get the same message, so IDs of hidden categories aren't revealed
        LegalCategory category = legalCategoryRepository.findByIdAndActiveTrue(request.categoryId())
            .orElseThrow(() -> new FieldValidationException("categoryId", "Select an active legal category"));

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

    /** Accounts deleted, suspended or disabled after login can't act on cases. */
    private ClientProfile findActiveClient(UUID userId) {
        return clientProfileRepository.findByUserId(userId).filter((profile) -> {
            User user = profile.getUser();
            return user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE;
        }).orElseThrow(AuthenticationRequiredException::new);
    }

}
