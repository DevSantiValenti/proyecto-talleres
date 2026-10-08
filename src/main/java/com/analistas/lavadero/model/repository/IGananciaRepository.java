package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IGananciaRepository extends JpaRepository<Ganancia, Long> {
    List<Ganancia> findByEmpleadoIdOrderByFechaDesc(Long id);

    List<Ganancia> findByServicioId(Long id);

    boolean existsByClave(String clave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Ganancia e where e.id = :id")
    Optional<Ganancia> bloquear(@Param("id") Long id);
}
