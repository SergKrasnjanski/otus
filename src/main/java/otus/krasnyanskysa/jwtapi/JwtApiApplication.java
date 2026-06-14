package otus.krasnyanskysa.jwtapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JwtApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwtApiApplication.class, args);
    }
}

