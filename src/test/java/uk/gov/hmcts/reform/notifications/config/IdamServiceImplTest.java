package uk.gov.hmcts.reform.notifications.config;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.notifications.config.security.idam.IdamServiceImpl;
import uk.gov.hmcts.reform.notifications.dtos.response.IdamUserIdResponse;
import uk.gov.hmcts.reform.notifications.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.reform.notifications.exceptions.UserNotFoundException;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = MOCK)
@SuppressWarnings("PMD")
public class IdamServiceImplTest {

    @InjectMocks
    private IdamServiceImpl idamService;

    @Mock
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    private static final String TOKEN = "Bearer 131313";

    @Test
    public void getResponseOnValidToken() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList(TOKEN));

        IdamUserIdResponse mockIdamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()
            .familyName("VP")
            .givenName("VP")
            .name("VP")
            .sub("V_P@gmail.com")
            .roles(Arrays.asList("vp"))
            .uid("986-erfg-kjhg-123")
            .build();

        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(mockIdamUserIdResponse, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                       eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);

        IdamUserIdResponse idamUserIdResponse = idamService.getUserId(header);
        assertEquals(mockIdamUserIdResponse.getUid(), idamUserIdResponse.getUid());
    }

    @Test
    public void getExceptionOnInvalidToken() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList(TOKEN));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                       eq(IdamUserIdResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "user not found"));

        assertThrows(UserNotFoundException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
    public void getExceptionOnTokenReturnsNullResponse() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList(TOKEN));

        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                       eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);
        assertThrows(UserNotFoundException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
    public void getExceptionOnValidToken() throws Exception {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList(TOKEN));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                       eq(IdamUserIdResponse.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT, "Gateway timeout"));
        assertThrows(GatewayTimeoutException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
    public void validateResponseDto() {
        IdamUserIdResponse idamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()
            .familyName("VP")
            .givenName("VP")
            .name("VP")
            .sub("V_P@gmail.com")
            .roles(Arrays.asList("vp"))
            .uid("986-erfg-kjhg-123")
            .build();

        assertEquals("VP", idamUserIdResponse.getFamilyName());
        assertEquals("VP", idamUserIdResponse.getGivenName());
        assertEquals("VP", idamUserIdResponse.getName());
        assertEquals(Arrays.asList("vp"), idamUserIdResponse.getRoles());
        assertEquals("986-erfg-kjhg-123", idamUserIdResponse.getUid());
        assertEquals("V_P@gmail.com", idamUserIdResponse.getSub());
    }

}
