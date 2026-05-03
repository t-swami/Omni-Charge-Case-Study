package com.omnicharge.api_gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("CorsConfig Unit Tests")
class CorsConfigTest {

    @Test
    @DisplayName("corsWebFilter() - Creates and returns a CorsWebFilter bean")
    void corsWebFilter_returnsBean() {
        CorsConfig config = new CorsConfig();
        CorsWebFilter filter = config.corsWebFilter();
        
        assertNotNull(filter, "CorsWebFilter bean should not be null");
    }
}
