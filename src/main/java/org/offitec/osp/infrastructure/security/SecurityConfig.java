package org.offitec.osp.infrastructure.security;

import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.offitec.osp.domain.port.TemporaryPasswordGeneratorPort;
import org.offitec.osp.infrastructure.adapter.PasswordEncoderPortImpl;
import org.offitec.osp.infrastructure.adapter.TemporaryPasswordGeneratorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoderPort passwordEncoder(){

        return new PasswordEncoderPortImpl();
    }

    @Bean
    public TemporaryPasswordGeneratorPort temporaryPasswordGeneratorPort(){

        return new TemporaryPasswordGeneratorImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth", "/auth/logout", "/error").permitAll()
                        .requestMatchers("/account/activate", "/account/forgot-password-request", "/account/reset-password", "/account/delete-account").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}") java.util.List<String> allowedOrigins) {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        // Cache CORS preflight responses for an hour so cross-origin POST/PUT/DELETE
        // requests don't pay an extra OPTIONS round-trip every few seconds in prod.
        configuration.setMaxAge(3600L);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
