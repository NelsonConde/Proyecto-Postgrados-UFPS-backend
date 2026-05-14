package ufps.edu.co;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ufps.edu.co")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}