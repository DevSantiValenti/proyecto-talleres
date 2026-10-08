# Gestión de Taller

Aplicación web en Java 21 / Spring Boot 4.1.1, Thymeleaf y MySQL. Aplicación exclusiva para taller mecánico, con órdenes, repuestos, personal, fichas permanentes, historial y recordatorios.

## Arranque local

Requiere Java 21 y MySQL 8.4. Crear una base **vacía** y un usuario con permisos sobre ella. Flyway crea el esquema; Hibernate lo valida. No se usa `ddl-auto=update` ni se modifica automáticamente un esquema heredado.

```bash
export DB_URL='jdbc:mysql://localhost:3306/lavadero'
export DB_USER='lavadero'
read -rsp 'Contraseña MySQL: ' DB_PASSWORD; export DB_PASSWORD
read -rsp 'Contraseña administrador (12 caracteres mínimo): ' ADMIN_PASSWORD; export ADMIN_PASSWORD
export ADMIN_USERNAME='admin'
export APP_BASE_URL='http://localhost:8080'
./mvnw spring-boot:run
```

Abrir http://localhost:8080. El administrador se crea **solo si no existe ninguna cuenta**. No hay contraseña predeterminada. Cambiar `ADMIN_PASSWORD` después no modifica cuentas existentes: se administran desde **Cuentas de acceso**. Editar o desactivar una cuenta cierra sus sesiones existentes. BCrypt limita la contraseña a 72 bytes UTF-8.

Si Maven no puede escribir en su caché habitual, usar `-Dmaven.repo.local=/ruta/escribible/m2`.

### URL pública para dominio o túnel

En `src/main/resources/application.properties`, `app.base-url` define la dirección que se usa en los enlaces públicos, QR de tickets, tarjetas de vehículos y recordatorios de WhatsApp. Podés reemplazar la línea por tu URL:

```properties
app.base-url=https://mi-tunel.trycloudflare.com
```

También podés conservar la configuración con variables y establecer `APP_BASE_URL=https://taller.midominio.com` antes de iniciar. `PUBLIC_URL` sigue funcionando como alternativa para instalaciones anteriores. Usá la URL completa con `http://` o `https://`, sin barra final ni rutas adicionales. Reiniciá Spring Boot después de cambiarla. En Docker Compose se toma de `DOMAIN`.

Esta propiedad cambia los enlaces generados; el túnel o dominio debe estar configurado para apuntar a la aplicación. Los tokens y el historial se conservan, pero un QR ya impreso mantiene la dirección anterior: si cambia el dominio del túnel, descargá e imprimí un nuevo ticket o tarjeta. Para reutilizar siempre el mismo QR, usá un dominio estable.

### Primera configuración

1. Crear cuentas `OPERACION` y `PANTALLA`, y fichas de empleados independientes.
2. Cargar servicios y sus etapas: `nombre | EN_ESPERA`, una o más etapas `EN_PROCESO` y una última `TERMINADO`.
3. Cargar clientes y vehículos. El teléfono debe incluir código internacional; registrar consentimiento solo cuando el cliente lo haya dado.
4. Ingresar una orden, seleccionar servicios y precios. En taller, aprobar el presupuesto antes de comenzar.
5. Asignar empleados y avanzar desde la cola. Descargar el ticket PDF, registrar el pago y entregar el vehículo.
6. Programar mantenimientos desde su panel, con fecha y/o kilómetros.

Las cuentas compartidas identifican la cuenta utilizada, no a la persona que hizo clic. La TV abre `/pantalla`; su cuenta no puede modificar registros ni consultar importes.

## Migración a taller e interfaz

La migración `V3__solo_taller.sql` desactiva los servicios del antiguo catálogo de lavado y retira las columnas de categoría y token temporal. Conserva órdenes, vehículos, importes, historial, mantenimientos y los tokens permanentes. Las versiones V1 y V2 se mantienen intactas. No se borran registros históricos: los servicios antiguos permanecen como referencias desactivadas.

Se retiraron `/seguimiento/**`, `/api/seguimiento/**` y las acciones de renovar QR. El código público ahora vive en `IFichaVehiculoService`, `FichaVehiculoServiceImpl` y `FichaVehiculoController`, dentro de la estructura de paquetes acordada. Se conserva el paquete base `com.analistas.lavadero` y el nombre de base de datos existente para no romper instalaciones.

Al entrar se muestran las órdenes. Vehículos y próximos services tienen accesos directos; los catálogos, personal y usuarios se agrupan en Administración. En móvil las tablas se presentan como tarjetas, los formularios ocupan la pantalla y la navegación principal permanece al pie. Las etapas se editan con campos y botones. Precios, correcciones e historial administrativo se agrupan en desplegables.

Antes de actualizar una instalación existente, realizar el respaldo habitual. Reiniciar con la nueva versión aplica Flyway; no ejecutar una versión anterior contra el esquema nuevo. La ficha conserva su token entre visitas y al cambiar el propietario. Para que la dirección completa siga siendo fija, mantener un dominio estable en `app.base-url`.

## Reglas del sistema

- Una orden activa por vehículo, con uno o varios servicios. La patente se normaliza.
- Las etapas y comisiones se copian a cada trabajo. Cambiar el catálogo no altera órdenes anteriores.
- Toda modificación económica invalida la aprobación del presupuesto. Se conservan sus versiones.
- Un único pago completo por orden. No se admiten señas, pagos mixtos ni modificaciones económicas directas luego de cobrar.
- Una orden solo se entrega terminada y pagada. Las cancelaciones cobradas requieren conciliación fuera de este flujo; el sistema no ejecuta reembolsos.
- Comisiones fijas o porcentuales sobre mano de obra, repartidas entre participantes con redondeo exacto. Se generan al finalizar, aunque no se haya cobrado.
- Reabrir un servicio no entregado crea movimientos de reversión y una nueva finalización genera nuevas ganancias. Las liquidaciones anteriores permanecen intactas.
- Ajustes manuales de ganancias con motivo y clave de idempotencia. Las liquidaciones incluyen únicamente movimientos pendientes del período seleccionado.
- Cada vehículo tiene una única ficha permanente. Todos sus tickets y su tarjeta usan el mismo QR, sin vencimiento ni rotación desde la aplicación.
- El cambio de propietario conserva el QR y el historial técnico; cancela/desactiva los avisos al destinatario anterior. El administrador debe configurar nuevamente los avisos para el nuevo cliente.
- La ficha pública muestra solo información técnica; las notas internas y los importes quedan separados.
- En el detalle de una orden, **Repuestos → Agregar / Editar / Quitar** permite cambiar nombre, cantidad, costo y precio antes de terminar o cobrar. Cada cambio genera otra versión del presupuesto. Si está terminada, primero reabrir el servicio; las órdenes cobradas o cerradas conservan sus importes. El panel explica el bloqueo.
- **Orden → Qué se hizo** permite describir qué se hizo, incluso después de entregar. El texto aparece en la ficha pública al terminar ese servicio, sin modificar cobros ni comisiones.
- Mantenimiento: fecha y/o kilómetros, primero que se alcance. El cliente no modifica el odómetro. La fecha no reserva un turno.
- Las pantallas usan SSE y consultas cada 10 segundos; al reconectar recuperan el estado actual.
- Moneda ARS, fechas del negocio en `America/Argentina/Cordoba`, instantes almacenados en UTC.

## WhatsApp Cloud API

Deshabilitado por defecto; no se envían mensajes reales sin configuración. No se automatiza WhatsApp Web.

Configurar las variables de `.env.example`: token con permisos de envío, ID del número, secreto de la app y token de verificación. Mantener la versión de Graph API dentro de las versiones admitidas por Meta.

Crear y aprobar una plantilla del idioma configurado con **cuatro parámetros de texto en el cuerpo**, en este orden:

1. Vehículo (marca y modelo).
2. Mantenimiento recomendado.
3. Fecha y/o kilometraje objetivo.
4. URL completa de la ficha del vehículo.

Ejemplo de texto de plantilla: «Recordatorio de mantenimiento para {{1}}: {{2}}. Próximo service: {{3}}. Consultá tu historial en {{4}}. Para dejar de recibir avisos respondé BAJA».

Registrar el webhook HTTPS `https://TU_DOMINIO/webhooks/whatsapp`, usar `WHATSAPP_VERIFY_TOKEN` en la verificación y suscribir los eventos de mensajes. Las solicitudes POST se verifican mediante HMAC-SHA256 sobre el cuerpo original y `WHATSAPP_APP_SECRET`.

Los avisos por fecha se programan a las 09:00, con anticipación inicial de 7 días y otro aviso al vencimiento. Por kilómetros se evalúan al procesar la lectura registrada (máximo un minuto con el scheduler habilitado). No se acumulan avisos antiguos. Las respuestas BAJA, STOP o CANCELAR retiran el consentimiento.

Se persisten recordatorios e intentos. Un 429 permite hasta tres intentos; fallos permanentes se muestran como fallidos. Un timeout o respuesta incierta requiere revisión en Meta y no se reenvía automáticamente. «Aceptado» no significa «Entregado»: la entrega se confirma por webhook. Revisar periódicamente el panel de recordatorios y las condiciones/costos de Meta.

## Producción con HTTPS

1. Copiar `.env.example` a `.env`, completar secretos y dominio, y limitar sus permisos (`chmod 600 .env`).
2. Apuntar el DNS al servidor; habilitar los puertos 80 y 443.
3. Ejecutar `docker compose up -d --build`.
4. Comprobar login, PDF, QR desde un celular y `/actuator/health`.

Caddy gestiona HTTPS y pasa los eventos SSE sin buffering. MySQL no se publica en el host. Solo el proxy expone puertos; los encabezados reenviados se confían bajo esa topología. La aplicación usa una sola instancia. No activar logs de acceso con tokens QR completos ni habilitar logging HTTP de credenciales.

El contenedor de aplicación se ejecuta sin root. Los datos MySQL y certificados están en volúmenes persistentes. Contratar hosting, registrar dominio y activar Meta son pasos externos: el repositorio no los realiza automáticamente.

## Respaldo y restauración

```bash
./scripts/backup.sh
# Copiar el archivo a almacenamiento externo seguro.
docker compose stop app
./scripts/restore.sh --confirm backups/ARCHIVO.sql.gz
docker compose start app
```

Programar el backup diariamente en el servidor y conservar copias fuera de él. El backup usa una transacción consistente y contiene tokens, historial y recordatorios; tratarlo como información privada. No ejecutar `docker compose down -v` sobre datos que se quieran conservar.

Probar restauraciones periódicamente en una base aislada. Verificar cantidades de órdenes, ganancias, mantenimientos y recordatorios, abrir un QR conocido y comprobar Flyway antes de volver a operar.

## Pruebas

```bash
./mvnw test
```

Se ejecuta el flujo de integración con H2 en modo MySQL y las migraciones reales. Si Docker está disponible, `MySqlContenedorTest` repite el flujo contra MySQL 8.4 usando Testcontainers; de lo contrario informa esas pruebas como omitidas.

También se puede ejecutar contra una **base MySQL exclusiva de pruebas**; estas pruebas vacían sus tablas de negocio:

```bash
./mvnw -Dtest=FlujoIntegracionTest,WhatsAppFirmaTest \
  -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/lavadero_test \
  -Dspring.datasource.username=usuario_pruebas \
  -Dspring.datasource.password=CLAVE_PRUEBAS \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver test
```

La suite cubre aprobaciones, repuestos, comisiones y centavos, liquidaciones, correcciones, conflictos concurrentes, privacidad, permisos, CSRF, enlaces permanentes entre visitas y propietarios, programación y cancelación de recordatorios, envíos simulados, reintentos y webhooks. El PDF se renderiza y su QR se decodifica en las pruebas. Las pruebas no envían WhatsApp reales.

## Estructura obligatoria

Entidades y clases de dominio en `model/domain`; DTO en `model/domain/dto`; repositorios `I{Entidad}Repository` en `model/repository`; interfaces `I{Nombre}Service` e implementaciones `{Nombre}ServiceImpl` juntas en `model/service`; configuración en `web/config`; controladores en `web/controller`; recursos en `templates` y `static/css`, `static/js`, `static/img`. No agregar paquetes arbitrarios.

## Límites de esta versión

Un negocio y una sede; sin stock, proveedores, agenda, cuentas de clientes, facturación fiscal, pasarela de pagos ni impresión ESC/POS. Las consultas administrativas cargan el historial del negocio; para grandes volúmenes será necesario incorporar paginación. El acceso público es de tipo portador: quien conserva el QR puede ver la información técnica permitida sin vencimiento automático. La ficha no contiene datos personales de propietarios.

## Verificación de esta entrega

- 37 pruebas aprobadas: reglas del negocio, permisos, privacidad, repuestos, comisiones, recordatorios, PDF/QR y migración. Se ejecutaron tanto con H2 como con MySQL 8.4 aislado (la prueba específica de migración usa su propia base H2).
- Migración V2 → V3 comprobada además sobre una base MySQL con datos anteriores: mismas órdenes y vehículos, tokens permanentes intactos y catálogo de lavado desactivado.
- Los endpoints temporales y de rotación devuelven 404; el mismo QR funciona diez años después y tras nuevas visitas. Cambiar el propietario cancela avisos previos y conserva la ficha.
- UI de escritorio y móvil revisada con el navegador: login, órdenes, repuestos, catálogo con editor de etapas y ficha pública. Las tablas móviles se muestran como tarjetas y los formularios ocupan la pantalla.
- PDF renderizado y QR permanente decodificado automáticamente. WhatsApp probado con respuestas simuladas y firmas HMAC, sin mensajes reales.
- No se modificó la base del negocio ni se ejecutó un despliegue de producción. Reiniciar con el nuevo JAR aplica la migración; realizar previamente el respaldo habitual.
