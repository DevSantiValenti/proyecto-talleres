package com.analistas.lavadero.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.*;

@Service
public class EventoServiceImpl implements IEventoService {
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> canales =
            new ConcurrentHashMap<>();

    public SseEmitter suscribir(String canal) {
        SseEmitter e = new SseEmitter(60000L);
        var lista = canales.computeIfAbsent(canal, k -> new CopyOnWriteArrayList<>());
        lista.add(e);
        Runnable limpiar =
                () -> {
                    lista.remove(e);
                    if (lista.isEmpty()) canales.remove(canal, lista);
                };
        e.onCompletion(limpiar);
        e.onTimeout(
                () -> {
                    limpiar.run();
                    e.complete();
                });
        e.onError(x -> limpiar.run());
        enviar(e, "actualizar");
        return e;
    }

    public void publicar(Long vehiculoId, Long ordenId) {
        Runnable r =
                () -> {
                    emitir("cola");
                    if (vehiculoId != null) emitir("vehiculo:" + vehiculoId);
                    if (ordenId != null) emitir("orden:" + ordenId);
                };
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            r.run();
                        }
                    });
        else r.run();
    }

    private void emitir(String canal) {
        var lista = canales.get(canal);
        if (lista != null) lista.forEach(e -> enviar(e, "actualizar"));
    }

    private void enviar(SseEmitter e, String tipo) {
        try {
            e.send(SseEmitter.event().name(tipo).data("{}"));
        } catch (Exception ex) {
            e.complete();
        }
    }

    public void latido() {
        canales.values().forEach(l -> l.forEach(e -> enviar(e, "latido")));
    }
}
