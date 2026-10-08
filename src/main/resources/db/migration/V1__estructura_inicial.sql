create table usuario (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  nombre varchar(100) not null unique,
  password varchar(100) not null,
  rol varchar(30) not null,
  activo boolean not null
);

create table cliente (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  nombre varchar(150) not null,
  telefono varchar(30),
  consentimiento_whatsapp boolean not null,
  consentimiento_fecha timestamp(6) null
);

create table empleado (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  nombre varchar(150) not null,
  activo boolean not null
);

create table vehiculo (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  patente varchar(20) not null unique,
  tipo varchar(30) not null,
  marca varchar(100) not null,
  modelo varchar(100) not null,
  cliente_id bigint,
  foreign key (cliente_id) references cliente(id),
  token_publico varchar(64) not null unique,
  kilometraje bigint,
  kilometraje_fecha timestamp(6) null
);

create table servicio_catalogo (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  nombre varchar(150) not null,
  categoria varchar(30) not null,
  precio decimal(14,2) not null,
  tipo_comision varchar(30) not null,
  comision decimal(14,2) not null,
  activo boolean not null
);

create table orden_trabajo (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  vehiculo_id bigint not null,
  foreign key (vehiculo_id) references vehiculo(id),
  cliente_id bigint,
  foreign key (cliente_id) references cliente(id),
  estado varchar(30) not null,
  ingreso timestamp(6) not null,
  actualizacion timestamp(6) not null,
  finalizacion timestamp(6) null,
  entrega timestamp(6) null,
  notas_internas varchar(2000) not null,
  token_temporal varchar(64) not null unique,
  presupuesto_version int not null,
  version_aprobada int,
  aprobacion_nota varchar(500),
  aprobacion_fecha timestamp(6) null,
  medio_previsto varchar(30),
  kilometraje_ingreso bigint,
  vehiculo_activo bigint unique
);

create table orden_servicio (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  orden_id bigint not null,
  foreign key (orden_id) references orden_trabajo(id),
  catalogo_id bigint not null,
  foreign key (catalogo_id) references servicio_catalogo(id),
  nombre varchar(150) not null,
  categoria varchar(30) not null,
  precio decimal(14,2) not null,
  tipo_comision varchar(30) not null,
  comision decimal(14,2) not null,
  etapa_actual int not null,
  estado varchar(30) not null,
  resumen_publico varchar(2000) not null,
  finalizacion timestamp(6) null,
  ciclo int not null
);

create table repuesto (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  orden_id bigint not null,
  foreign key (orden_id) references orden_trabajo(id),
  nombre varchar(150) not null,
  cantidad int not null,
  costo decimal(14,2) not null,
  precio decimal(14,2) not null
);

create table presupuesto (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  orden_id bigint not null,
  foreign key (orden_id) references orden_trabajo(id),
  numero int not null,
  detalle text not null,
  total decimal(14,2) not null,
  fecha timestamp(6) not null,
  actor varchar(100) not null
);

create table pago (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  orden_id bigint not null unique,
  foreign key (orden_id) references orden_trabajo(id),
  importe decimal(14,2) not null,
  medio varchar(30) not null,
  fecha timestamp(6) not null,
  actor varchar(100) not null
);

create table historial (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  orden_id bigint not null,
  foreign key (orden_id) references orden_trabajo(id),
  fecha timestamp(6) not null,
  actor varchar(100) not null,
  accion varchar(2000) not null
);

create table liquidacion (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  empleado_id bigint not null,
  foreign key (empleado_id) references empleado(id),
  desde date not null,
  hasta date not null,
  importe decimal(14,2) not null,
  medio varchar(30) not null,
  fecha timestamp(6) not null,
  actor varchar(100) not null
);

create table ganancia (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  servicio_id bigint not null,
  foreign key (servicio_id) references orden_servicio(id),
  empleado_id bigint not null,
  foreign key (empleado_id) references empleado(id),
  importe decimal(14,2) not null,
  fecha timestamp(6) not null,
  liquidacion_id bigint,
  foreign key (liquidacion_id) references liquidacion(id),
  concepto varchar(500) not null,
  clave varchar(120) not null unique
);

create table lectura_kilometraje (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  vehiculo_id bigint not null,
  foreign key (vehiculo_id) references vehiculo(id),
  kilometraje bigint not null,
  fecha timestamp(6) not null,
  actor varchar(100) not null,
  motivo varchar(500) not null
);

create table mantenimiento_programado (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  vehiculo_id bigint not null,
  foreign key (vehiculo_id) references vehiculo(id),
  destinatario_id bigint,
  foreign key (destinatario_id) references cliente(id),
  origen_id bigint,
  foreign key (origen_id) references orden_servicio(id),
  realizado_con_id bigint,
  foreign key (realizado_con_id) references orden_servicio(id),
  descripcion varchar(300) not null,
  fecha_objetivo date,
  kilometraje_objetivo bigint,
  estado varchar(30) not null,
  anticipacion_dias int not null,
  avisos boolean not null,
  revision int not null
);

create table recordatorio (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  mantenimiento_id bigint not null,
  foreign key (mantenimiento_id) references mantenimiento_programado(id),
  destinatario_id bigint not null,
  foreign key (destinatario_id) references cliente(id),
  tipo varchar(30) not null,
  revision int not null,
  programado timestamp(6) not null,
  estado varchar(30) not null,
  intentos int not null,
  proveedor_id varchar(255) unique,
  actualizado timestamp(6) not null,
  detalle varchar(500) not null,
  clave varchar(100) not null unique
);

create table intento_notificacion (
  id bigint auto_increment primary key,
  version bigint not null default 0,
  recordatorio_id bigint not null,
  foreign key (recordatorio_id) references recordatorio(id),
  fecha timestamp(6) not null,
  resultado varchar(500) not null
);

create table servicio_catalogo_etapa (propietario_id bigint not null, posicion int not null, nombre varchar(100) not null, estado varchar(30) not null, primary key(propietario_id,posicion), foreign key(propietario_id) references servicio_catalogo(id));

create table orden_servicio_etapa (propietario_id bigint not null, posicion int not null, nombre varchar(100) not null, estado varchar(30) not null, primary key(propietario_id,posicion), foreign key(propietario_id) references orden_servicio(id));

create table servicio_empleado (servicio_id bigint not null, empleado_id bigint not null, primary key(servicio_id,empleado_id), foreign key(servicio_id) references orden_servicio(id), foreign key(empleado_id) references empleado(id));

create index idx_orden_cola on orden_trabajo(estado, ingreso);

create index idx_recordatorio_pendiente on recordatorio(estado, programado);

create index idx_mantenimiento_vehiculo on mantenimiento_programado(vehiculo_id, estado);

create index idx_ganancia_periodo on ganancia(empleado_id, fecha, liquidacion_id);

create unique index uk_presupuesto_version on presupuesto(orden_id,numero);
