package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IPresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    List<Presupuesto> findByOrdenIdOrderByNumeroDesc(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Presupuesto e where e.id = :id")
    Optional<Presupuesto> bloquear(@Param("id") Long id);
}
