package com.analistas.lavadero.web.config;

import com.analistas.lavadero.model.service.IUsuarioService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

import java.time.*;

@Configuration
public class AplicacionConfig {
    @Bean
    Clock clock(@Value("${app.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    ApplicationRunner inicializar(
            IUsuarioService usuarios,
            @Value("${app.admin.username}") String nombre,
            @Value("${app.admin.password}") String password) {
        return args -> usuarios.inicializar(nombre, password);
    }
}
