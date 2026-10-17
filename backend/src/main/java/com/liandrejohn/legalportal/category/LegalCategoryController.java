package com.liandrejohn.legalportal.category;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal-categories")
@RequiredArgsConstructor
class LegalCategoryController {

    private final LegalCategoryService legalCategoryService;

    /**
     * Lists the active legal categories for the Create/Edit Draft dropdown. Requests without an authenticated session
     * are rejected with 401 by the security filter chain before reaching this method.
     */
    @GetMapping
    List<LegalCategoryResponse> list() {
        return legalCategoryService.findActiveCategories();
    }

}
