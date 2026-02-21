package _2.LTW.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("default")
public class TimeConfig {

    @Bean
    public Clock clock(){

        return Clock.systemDefaultZone();

    }

}
