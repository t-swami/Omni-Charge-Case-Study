package com.omnicharge.config_server;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
        ConfigServerApplication app = new ConfigServerApplication();
        assertNotNull(app);
    }

    @Test
    void mainMethodCallsSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(
                    eq(ConfigServerApplication.class),
                    any(String[].class)
            )).thenReturn(null);

            ConfigServerApplication.main(new String[]{});

            mockedSpring.verify(() -> SpringApplication.run(
                    eq(ConfigServerApplication.class),
                    any(String[].class)
            ));
        }
    }

    @Test
    void mainMethodWithArgs() {
        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(
                    eq(ConfigServerApplication.class),
                    any(String[].class)
            )).thenReturn(null);

            String[] args = {"--server.port=0", "--spring.profiles.active=test"};
            ConfigServerApplication.main(args);

            mockedSpring.verify(() -> SpringApplication.run(
                    eq(ConfigServerApplication.class),
                    eq(args)
            ));
        }
    }
}
