package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IHistorialRepository extends JpaRepository<Historial, Long> {
    List<Historial> findByOrdenIdOrderByFechaAsc(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Historial e where e.id = :id")
    Optional<Historial> bloquear(@Param("id") Long id);
}
