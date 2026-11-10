package br.com.academia.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio injetável: permite testar regras de horário (entrada/saída) de forma determinística.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock(AcademiaProperties props) {
        return Clock.system(props.zone());
    }
}
