package com.liandrejohn.legalportal.category;

import java.util.UUID;

/** A legal category a client can pick for a case. */
public record LegalCategoryResponse(UUID id, String name, String description) {

    static LegalCategoryResponse from(LegalCategory category) {
        return new LegalCategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

}
