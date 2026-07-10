package com.koustav.kaptur.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /**
     * Provides a Clock fixed to IST (Asia/Kolkata) timezone. Inject this Clock
     * wherever you need the current time.
     */
    @Bean
    public Clock istClock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
