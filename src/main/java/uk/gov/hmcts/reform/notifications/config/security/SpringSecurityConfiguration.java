package uk.gov.hmcts.reform.notifications.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.notifications.config.security.converter.JwtGrantedAuthoritiesConverter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class SpringSecurityConfiguration {

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    private final ServiceAuthFilter serviceAuthFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

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
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/error").permitAll()
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
}
