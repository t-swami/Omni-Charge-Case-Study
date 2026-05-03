package com.omnicharge.operator_service.config;

import com.omnicharge.operator_service.filter.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private JwtAuthFilter jwtAuthFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> response
								.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
						.accessDeniedHandler((request, response, accessDeniedException) -> response
								.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						// View operators and plans - any authenticated user (GET only)
						.requestMatchers(HttpMethod.GET, "/api/operators/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/plans/**").authenticated()
						// Add, update, patch, delete operators - ADMIN only
						.requestMatchers(HttpMethod.POST, "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/operators/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/operators/**").hasAuthority("ROLE_ADMIN")
						// Add, update, patch, delete plans - ADMIN only
						.requestMatchers(HttpMethod.POST, "/api/plans/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/plans/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/plans/**").hasAuthority("ROLE_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/plans/**").hasAuthority("ROLE_ADMIN")
						// Everything else requires authentication
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
