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
public class EmpleadoServiceImpl implements IEmpleadoService {
    private final IEmpleadoRepository repository;

    @Transactional(readOnly = true)
    public List<EmpleadoDto> listar() {
        return repository.findAll().stream()
                .map(e -> new EmpleadoDto(e.getId(), e.getNombre(), e.isActivo()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public EmpleadoDto guardar(Long id, EmpleadoEntrada d) {
        Empleado e = id == null ? new Empleado() : repository.bloquear(id).orElseThrow();
        e.setNombre(d.nombre().trim());
        e.setActivo(d.activo());
        repository.save(e);
        return new EmpleadoDto(e.getId(), e.getNombre(), e.isActivo());
    }
}
