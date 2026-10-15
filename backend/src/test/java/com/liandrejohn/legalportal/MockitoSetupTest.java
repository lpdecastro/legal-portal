package com.liandrejohn.legalportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Plain JUnit 5 + Mockito unit test (no Spring context) showing the pattern for service-level tests.
 */
@ExtendWith(MockitoExtension.class)
class MockitoSetupTest {

	@Mock
	private Clock clock;

	@Test
	void mockReturnsStubbedValue() {
		Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
		when(clock.instant()).thenReturn(fixed);

		assertThat(clock.instant()).isEqualTo(fixed);
		verify(clock).instant();
	}

}
