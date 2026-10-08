package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IOrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    List<OrdenTrabajo> findAllByOrderByIngresoDesc();

    List<OrdenTrabajo> findByVehiculoIdOrderByIngresoDesc(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OrdenTrabajo e where e.id = :id")
    Optional<OrdenTrabajo> bloquear(@Param("id") Long id);
}
