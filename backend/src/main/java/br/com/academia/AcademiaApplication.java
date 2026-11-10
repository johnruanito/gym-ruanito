package br.com.academia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AcademiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademiaApplication.class, args);
    }
}
