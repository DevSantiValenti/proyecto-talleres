package com.analistas.lavadero.model.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IEventoService {
    void publicar(Long vehiculoId, Long ordenId);

    SseEmitter suscribir(String canal);

    void latido();
}
