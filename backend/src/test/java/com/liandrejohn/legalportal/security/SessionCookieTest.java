package com.liandrejohn.legalportal.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

/**
 * Checks the session cookie against a real server: the cookie attributes from {@code server.servlet.session.cookie}
 * aren't applied to Spring Session's cookie in a mock web environment.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SessionCookieTest {

	@LocalServerPort
	private int port;

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

}
