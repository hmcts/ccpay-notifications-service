package uk.gov.hmcts.reform.notifications.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ActiveProfiles({"local","test"})
@SpringBootTest(webEnvironment = MOCK)
@SuppressWarnings("PMD")
class SwaggerPublisherTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @BeforeEach
    public void setup() {
        this.mvc = webAppContextSetup(webAppContext).build();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateDocs() throws Exception {
        generateDocsForGroup();
    }

    private void generateDocsForGroup() throws Exception {
        byte[] specs = mvc.perform(
                get("/v3/api-docs/notifications")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/ccpay-notifications-service.json"))) {
            outputStream.write(specs);
        }
    }
}
