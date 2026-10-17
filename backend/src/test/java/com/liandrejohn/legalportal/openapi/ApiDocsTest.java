package com.liandrejohn.legalportal.openapi;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsArePublicWithConfiguredTitleAndVersion() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.info.title").value("Legal Portal API"))
            .andExpect(jsonPath("$.info.version").value("v1"))
            .andExpect(jsonPath("$.paths['/api/v1/cases']").exists());
    }

    @Test
    void scalarUiIsPublic() throws Exception {
        mockMvc.perform(get("/scalar"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("/v3/api-docs")));
    }

}
