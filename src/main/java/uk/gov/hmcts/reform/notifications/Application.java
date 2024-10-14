package uk.gov.hmcts.reform.notifications;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        ApplicationInsights.attach();
        SpringApplication.run(Application.class, args);
    }
}
