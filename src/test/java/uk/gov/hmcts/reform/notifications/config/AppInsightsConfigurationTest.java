package uk.gov.hmcts.reform.notifications.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppInsightsConfigurationTest {

    @Test
    void shouldCreateTelemetryClient() {
        final AppInsightsConfiguration appInsightsConfiguration = new AppInsightsConfiguration();
        assertThat(appInsightsConfiguration.telemetryClient()).isInstanceOf(TelemetryClient.class);
    }
}
