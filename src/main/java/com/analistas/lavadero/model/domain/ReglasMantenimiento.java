package com.analistas.lavadero.model.domain;

import java.time.*;

public final class ReglasMantenimiento {
    private ReglasMantenimiento() {}

    public static String estado(MantenimientoProgramado m, LocalDate hoy) {
        if (m.getEstado() != EstadoMantenimiento.PENDIENTE) return m.getEstado().name();
        Long lectura = m.getVehiculo().getKilometraje(), limite = m.getKilometrajeObjetivo();
        LocalDate fecha = m.getFechaObjetivo();
        if ((limite != null && lectura != null && lectura >= limite)
                || (fecha != null && fecha.isBefore(hoy))) return "VENCIDO";
        if (fecha != null && fecha.equals(hoy)) return "VENCE_HOY";
        if (fecha != null && !fecha.isAfter(hoy.plusDays(7))) return "PROXIMO";
        return limite != null && lectura == null && fecha == null
                ? "KILOMETRAJE_SIN_VERIFICAR"
                : "VIGENTE";
    }
}
