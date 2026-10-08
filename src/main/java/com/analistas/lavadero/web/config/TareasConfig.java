package com.analistas.lavadero.web.config;

import com.analistas.lavadero.model.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TareasConfig {
    private final IRecordatorioService recordatorios;
    private final IEventoService eventos;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void recordatorios() {
        recordatorios.programar();
        recordatorios.despachar();
    }

    @Scheduled(fixedDelay = 20000)
    public void latido() {
        eventos.latido();
    }
}
