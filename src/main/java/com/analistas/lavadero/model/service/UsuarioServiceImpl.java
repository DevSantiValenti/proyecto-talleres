package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements IUsuarioService {
    private final IUsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final SessionRegistry sesiones;

    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombre) {
        var u =
                repository
                        .findByNombre(nombre)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getNombre())
                .password(u.getPassword())
                .roles(u.getRol().name())
                .disabled(!u.isActivo())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return repository.findAll().stream()
                .map(u -> new UsuarioDto(u.getId(), u.getNombre(), u.getRol(), u.isActivo()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void guardar(Long id, UsuarioEntrada d) {
        var cuentas = repository.bloquearTodas();
        Usuario u =
                id == null
                        ? new Usuario()
                        : cuentas.stream()
                                .filter(x -> x.getId().equals(id))
                                .findFirst()
                                .orElseThrow();
        String nombreAnterior = u.getNombre();
        repository
                .findByNombre(d.nombre())
                .ifPresent(x -> exigir(Objects.equals(x.getId(), id), "El usuario ya existe"));
        if (u.getRol() == Rol.ADMIN && (!d.activo() || d.rol() != Rol.ADMIN))
            exigir(
                    repository.findAll().stream()
                            .anyMatch(
                                    x ->
                                            x.getRol() == Rol.ADMIN
                                                    && x.isActivo()
                                                    && !x.getId().equals(id)),
                    "Debe quedar al menos un administrador activo");
        exigir(
                id != null || (d.password() != null && !d.password().isBlank()),
                "La contraseña inicial es obligatoria");
        if (d.password() != null && !d.password().isBlank()) {
            exigir(
                    d.password().length() >= 12
                            && d.password().getBytes(StandardCharsets.UTF_8).length <= 72,
                    "La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes");
            u.setPassword(encoder.encode(d.password()));
        }
        u.setNombre(d.nombre().trim());
        u.setRol(d.rol());
        u.setActivo(d.activo());
        repository.save(u);
        if (id != null)
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sesiones.getAllPrincipals().stream()
                                    .filter(
                                            p ->
                                                    p instanceof UserDetails d
                                                            && d.getUsername()
                                                                    .equals(nombreAnterior))
                                    .forEach(
                                            p ->
                                                    sesiones.getAllSessions(p, false)
                                                            .forEach(
                                                                    SessionInformation::expireNow));
                        }
                    });
    }

    public void inicializar(String nombre, String password) {
        if (repository.count() > 0) return;
        exigir(
                password != null
                        && password.length() >= 12
                        && password.getBytes(StandardCharsets.UTF_8).length <= 72,
                "Configurá ADMIN_PASSWORD con al menos 12 caracteres y hasta 72 bytes para crear el"
                        + " administrador inicial");
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setPassword(encoder.encode(password));
        u.setRol(Rol.ADMIN);
        u.setActivo(true);
        repository.save(u);
    }
}
