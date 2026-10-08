-- Retirar funciones de lavado sin eliminar órdenes, importes ni fichas existentes.
-- Las migraciones V1/V2 no se modifican para conservar sus checksums.
UPDATE servicio_catalogo SET activo = false, version = version + 1 WHERE categoria = 'LAVADO';
ALTER TABLE servicio_catalogo DROP COLUMN categoria;
ALTER TABLE orden_servicio DROP COLUMN categoria;
ALTER TABLE orden_trabajo DROP COLUMN token_temporal;
-- vehiculo.token_publico se conserva intacto: el QR permanente sigue siendo válido.
