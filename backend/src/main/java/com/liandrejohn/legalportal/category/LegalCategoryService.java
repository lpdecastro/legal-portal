package com.liandrejohn.legalportal.category;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LegalCategoryService {

    private final LegalCategoryRepository legalCategoryRepository;

    /** The categories clients can choose from, sorted by name. */
    @Transactional(readOnly = true)
    public List<LegalCategoryResponse> findActiveCategories() {
        return legalCategoryRepository.findByActiveTrueOrderByNameAsc()
            .stream()
            .map(LegalCategoryResponse::from)
            .toList();
    }

}
