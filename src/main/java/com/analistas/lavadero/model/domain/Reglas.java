package com.analistas.lavadero.model.domain;

import org.springframework.security.core.context.SecurityContextHolder;

import java.math.*;
import java.security.SecureRandom;
import java.util.*;

public final class Reglas {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Reglas() {}

    public static void exigir(boolean valido, String mensaje) {
        if (!valido) throw new ReglaNegocioException(mensaje);
    }

    public static String token() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String actor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "sistema" : a.getName();
    }

    public static boolean admin() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a != null
                && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    public static BigDecimal dinero(BigDecimal n) {
        exigir(n != null && n.signum() >= 0 && n.precision() - n.scale() <= 12, "Importe inválido");
        return n.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static void version(EntidadBase e, long version) {
        exigir(
                e.getVersion() == version,
                "El registro cambió en otra pantalla. Actualizá e intentá nuevamente.");
    }

    public static String patente(String p) {
        return p.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static String ocultarPatente(String p) {
        return p.length() < 4 ? "***" : p.substring(0, 2) + "***" + p.substring(p.length() - 2);
    }

    public static String nombre(Vehiculo v) {
        return v.getTipo() + " · " + v.getMarca() + " " + v.getModelo();
    }

    public static boolean cerrado(OrdenTrabajo o) {
        return o.getEstado() == EstadoTrabajo.ENTREGADO || o.getEstado() == EstadoTrabajo.CANCELADO;
    }
}
