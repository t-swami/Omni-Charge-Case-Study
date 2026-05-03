package com.omnicharge.eureka_server;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
        EurekaServerApplication app = new EurekaServerApplication();
        assertNotNull(app);
    }

    @Test
    void mainMethodCallsSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(
                    eq(EurekaServerApplication.class),
                    any(String[].class)
            )).thenReturn(null);

            EurekaServerApplication.main(new String[]{});

            mockedSpring.verify(() -> SpringApplication.run(
                    eq(EurekaServerApplication.class),
                    any(String[].class)
            ));
        }
    }

    @Test
    void mainMethodWithArgs() {
        try (MockedStatic<SpringApplication> mockedSpring = mockStatic(SpringApplication.class)) {
            mockedSpring.when(() -> SpringApplication.run(
                    eq(EurekaServerApplication.class),
                    any(String[].class)
            )).thenReturn(null);

            String[] args = {"--server.port=0", "--spring.profiles.active=test"};
            EurekaServerApplication.main(args);

            mockedSpring.verify(() -> SpringApplication.run(
                    eq(EurekaServerApplication.class),
                    eq(args)
            ));
        }
    }
}
