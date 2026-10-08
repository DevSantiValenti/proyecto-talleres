package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IRepuestoRepository extends JpaRepository<Repuesto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Repuesto e where e.id = :id")
    Optional<Repuesto> bloquear(@Param("id") Long id);
}
