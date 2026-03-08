package _2.LTW.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
@Profile("dev")
public class DevClockConfig {

    @Bean
    public Clock clock(){

        return Clock.fixed(
                LocalDateTime.of(2026, 3, 10, 9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );

    }

}
