package com.liandrejohn.legalportal.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

/**
 * MockMvc shows that no handler is mapped. A running server answers 401 instead, because the 404's /error dispatch
 * requires authentication like any unknown path.
 */
@SpringBootTest(properties = { "OPENAPI_ENABLED=false", "SCALAR_ENABLED=false" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAreNotFound() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void scalarUiIsNotFound() throws Exception {
        mockMvc.perform(get("/scalar")).andExpect(status().isNotFound());
    }

}
