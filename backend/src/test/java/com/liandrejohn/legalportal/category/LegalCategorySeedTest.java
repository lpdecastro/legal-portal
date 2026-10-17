package com.liandrejohn.legalportal.category;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LegalCategorySeedTest {

    @Autowired
    private LegalCategoryService legalCategoryService;

    @Test
    void migrationSeedsActiveCategories() {
        assertThat(legalCategoryService.findActiveCategories()).extracting(LegalCategoryResponse::name)
            .contains("Employment Law", "Family Law")
            .isSorted();
    }

}
