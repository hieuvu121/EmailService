package com.example.emailService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * spring-boot-starter-security is on the classpath, and without an explicit
 * chain Boot's default secures every request with generated-password basic
 * auth. That made /actuator/prometheus return 401 to Prometheus.
 *
 * This service exposes no public HTTP API — it consumes Kafka events and sends
 * mail — and neither its server port (8081) nor its management port (9090) is
 * published by docker-compose, so both are reachable only from inside the
 * compose network. Same posture as auth-service's SecurityConfig.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
