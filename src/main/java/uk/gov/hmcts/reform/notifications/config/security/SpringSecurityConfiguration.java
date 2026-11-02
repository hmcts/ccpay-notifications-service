package uk.gov.hmcts.reform.notifications.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.notifications.config.security.converter.JwtGrantedAuthoritiesConverter;

import java.io.IOException;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class SpringSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    private final ServiceAuthFilter serviceAuthFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final AccessDeniedHandler accessDeniedHandler = new BearerTokenAccessDeniedHandler();
    private final AuthenticationEntryPoint authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();

    private static final String PAYMENTS_ROLE = "payments";
    static final String DISPOSER_PAYMENT_USER_ROLE = "disposer-payment-user";
    private static final String AUTHORISED_REFUNDS_ROLE = "payments-refund";
    private static final String AUTHORISED_REFUND_APPROVER = "payments-refund-approver";

    public SpringSecurityConfiguration(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                       final ServiceAuthFilter serviceAuthFilter
    ) {
        this.serviceAuthFilter = serviceAuthFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/**",
                "/health",
                "/health/liveness",
                "/health/readiness",
                "/info",
                "/favicon.ico",
                "/"
            )
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .anonymous(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain serviceFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(this::handleAccessDenied)
                .authenticationEntryPoint(this::handleAuthenticationFailure)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/notifications/**")
                .access((authentication, context) -> AuthorityAuthorizationManager
                    .hasAnyAuthority(AUTHORISED_REFUNDS_ROLE, AUTHORISED_REFUND_APPROVER)
                    .check(authentication, context.getRequest()))
                .requestMatchers(HttpMethod.DELETE, "/notifications/v2/**")
                .access((authentication, context) -> AuthorityAuthorizationManager
                    .hasAnyAuthority(PAYMENTS_ROLE, DISPOSER_PAYMENT_USER_ROLE)
                    .check(authentication, context.getRequest()))
                .requestMatchers(HttpMethod.DELETE, "/notifications/**")
                .hasAuthority(PAYMENTS_ROLE)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {

        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuerUri);
        // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private void handleAccessDenied(HttpServletRequest request,
                                     HttpServletResponse response,
                                     AccessDeniedException exception) throws IOException, ServletException {
        if (isNotificationDeletion(request)) {
            LOG.error(
                "Notification deletion rejected for {}",
                request.getRequestURI()
            );
        }
        accessDeniedHandler.handle(request, response, exception);
    }

    private void handleAuthenticationFailure(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException exception) throws IOException, ServletException {
        if (isNotificationDeletion(request)) {
            LOG.error(
                "Notification deletion authentication failed for {}: {}",
                request.getRequestURI(),
                exception.getClass().getSimpleName()
            );
        }
        authenticationEntryPoint.commence(request, response, exception);
    }

    private boolean isNotificationDeletion(HttpServletRequest request) {
        return HttpMethod.DELETE.matches(request.getMethod())
            && request.getRequestURI() != null
            && request.getRequestURI().contains("/notifications/v2");
    }
}
