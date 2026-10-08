package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServicioCatalogoServiceImpl implements IServicioCatalogoService {
    private final IServicioCatalogoRepository repository;

    @Transactional(readOnly = true)
    public List<CatalogoDto> listar() {
        return repository.findAll().stream().map(this::dto).toList();
    }

    public CatalogoDto guardar(Long id, CatalogoEntrada d) {
        exigir(
                d.tipoComision() != TipoComision.PORCENTAJE
                        || d.comision().compareTo(new BigDecimal("100")) <= 0,
                "El porcentaje debe ser entre 0 y 100");
        exigir(
                d.etapas().size() >= 2
                        && d.etapas().getFirst().estado() == EstadoTrabajo.EN_ESPERA
                        && d.etapas().getLast().estado() == EstadoTrabajo.TERMINADO,
                "Las etapas deben comenzar en espera y terminar en terminado");
        int anterior = 0;
        for (int i = 0; i < d.etapas().size(); i++) {
            var estado = d.etapas().get(i).estado();
            int orden =
                    switch (estado) {
                        case EN_ESPERA -> 0;
                        case EN_PROCESO -> 1;
                        case TERMINADO -> 2;
                        default -> -1;
                    };
            exigir(
                    orden >= anterior && orden >= 0 && (orden != 2 || i == d.etapas().size() - 1),
                    "Secuencia de etapas inválida");
            anterior = orden;
        }
        ServicioCatalogo e =
                id == null ? new ServicioCatalogo() : repository.bloquear(id).orElseThrow();
        e.setNombre(d.nombre().trim());
        e.setPrecio(dinero(d.precio()));
        e.setTipoComision(d.tipoComision());
        e.setComision(dinero(d.comision()));
        e.setActivo(d.activo());
        e.getEtapas().clear();
        d.etapas().forEach(x -> e.getEtapas().add(new Etapa(x.nombre(), x.estado())));
        repository.save(e);
        return dto(e);
    }

    private CatalogoDto dto(ServicioCatalogo e) {
        return new CatalogoDto(
                e.getId(),
                e.getNombre(),
                e.getPrecio(),
                e.getTipoComision(),
                e.getComision(),
                e.isActivo(),
                e.getEtapas().stream()
                        .map(x -> new EtapaDto(x.getNombre(), x.getEstado()))
                        .toList());
    }
}
