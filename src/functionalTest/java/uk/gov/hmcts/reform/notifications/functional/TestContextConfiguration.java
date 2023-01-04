package uk.gov.hmcts.reform.notifications.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.path.json.config.JsonPathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

import static io.restassured.config.JsonConfig.jsonConfig;

@Configuration
@ComponentScan("uk.gov.hmcts.reform.notifications.functional")
@PropertySource("classpath:application-functional.yaml")
public class TestContextConfiguration {

    @Value("https://ccpay-notifications-service-pr-100.service.core-compute-preview.internal")
    private String baseURL;

    @PostConstruct
    public void initialize() {
        RestAssured.config = RestAssured.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper())
            )
            .jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = baseURL;
    }
}
