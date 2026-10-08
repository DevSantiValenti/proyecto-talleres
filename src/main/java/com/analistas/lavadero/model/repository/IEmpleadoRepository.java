package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IEmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> bloquear(@Param("id") Long id);
}
