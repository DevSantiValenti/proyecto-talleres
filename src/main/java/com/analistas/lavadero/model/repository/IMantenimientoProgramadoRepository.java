package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IMantenimientoProgramadoRepository
        extends JpaRepository<MantenimientoProgramado, Long> {
    List<MantenimientoProgramado> findByVehiculoIdOrderByIdDesc(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MantenimientoProgramado e where e.id = :id")
    Optional<MantenimientoProgramado> bloquear(@Param("id") Long id);
}
