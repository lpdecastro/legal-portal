package com.liandrejohn.legalportal.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.liandrejohn.legalportal.TestcontainersConfiguration;
import com.liandrejohn.legalportal.auth.RegisterRequest;
import com.liandrejohn.legalportal.auth.RegistrationService;

/**
 * Checks the session cookie against a real server: the cookie attributes from {@code server.servlet.session.cookie}
 * aren't applied to Spring Session's cookie in a mock web environment.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SessionCookieTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RegistrationService registrationService;

    @Test
    void csrfEndpointIssuesSecureSessionCookie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/csrf"))
            .build();

        HttpResponse<Void> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.discarding());
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Set-Cookie")).singleElement()
            .asString()
            .startsWith("SESSION=")
            .contains("Path=/", "Secure", "HttpOnly", "SameSite=Lax");
    }

    @Test
    void loginIssuesNewSecureSessionCookie() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        registrationService.register(new RegisterRequest("John", "Doe", email, "password123456"));

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> csrf = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/csrf")).build(),
                    HttpResponse.BodyHandlers.ofString());
            String preLoginCookie = csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
            String token = JsonPath.read(csrf.body(), "$.token");

            HttpRequest login = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Cookie", preLoginCookie)
                .header("X-XSRF-TOKEN", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers
                    .ofString("{\"email\":\"%s\",\"password\":\"password123456\"}".formatted(email)))
                .build();
            HttpResponse<Void> response = client.send(login, HttpResponse.BodyHandlers.discarding());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().allValues("Set-Cookie")).singleElement()
                .asString()
                .startsWith("SESSION=")
                .doesNotStartWith(preLoginCookie + ";")
                .contains("Path=/", "Secure", "HttpOnly", "SameSite=Lax");
        }
    }

}
