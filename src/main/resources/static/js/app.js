"use strict";
const $ = (s) => document.querySelector(s);
const mode = document.body.dataset.modo;
const isAdmin = mode === "ADMIN";
const readOnly = mode === "PANTALLA";
const S = {
  page: isAdmin ? "ordenes" : "cola",
  datos: {
    clientes: [],
    vehiculos: [],
    empleados: [],
    catalogo: [],
    usuarios: [],
  },
  cola: [],
  ordenes: [],
  ganancias: [],
  liquidaciones: [],
  mantenimientos: [],
  recordatorios: [],
};
const labels = {
  RECIBIDO: "Recibido",
  EN_ESPERA: "En espera",
  EN_PROCESO: "En proceso",
  TERMINADO: "Terminado",
  ENTREGADO: "Entregado",
  CANCELADO: "Cancelado",
  VIGENTE: "Vigente",
  PROXIMO: "Próximo",
  VENCE_HOY: "Vence hoy",
  VENCIDO: "Vencido",
  REALIZADO: "Realizado",
  PENDIENTE: "Pendiente",
  ACEPTADO: "Aceptado",
  ENVIANDO: "Enviando",
  FALLIDO: "Fallido",
  INCIERTO: "Revisar envío",
  KILOMETRAJE_SIN_VERIFICAR: "Kilómetros sin verificar",
};
const escape = (x) =>
  String(x ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const money = (n) =>
  new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 2,
  }).format(n ?? 0);
const date = (d, time = false) =>
  d
    ? (d.length === 10
        ? new Date(d + "T12:00:00")
        : new Date(d)
      ).toLocaleString("es-AR", {
        timeZone: "America/Argentina/Cordoba",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        ...(time ? { hour: "2-digit", minute: "2-digit" } : {}),
      })
    : "—";
const localDay = (d) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Cordoba",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(d);
const badge = (x) =>
  `<span class="badge ${escape(x)}">${escape(labels[x] || x)}</span>`;
const btn = (action, label, id = "", extra = "", cls = "small") =>
  `<button class="${cls}" data-action="${action}" data-id="${id}" ${extra}>${label}</button>`;
const empty = (title, text = "") =>
  `<div class="empty"><h3>${escape(title)}</h3><p>${escape(text)}</p></div>`;
const heading = (title, subtitle, actions = "") =>
  `<div class="page-heading"><div><h1>${title}</h1><p>${subtitle}</p></div><div class="actions">${actions}</div></div>`;
const table = (heads, rows) =>
  rows.length
    ? `<div class="table-wrap"><table><thead><tr>${heads.map((x) => `<th>${x}</th>`).join("")}</tr></thead><tbody>${rows.map((r) => `<tr>${r.map((c, i) => `<td data-label="${escape(heads[i] || "Acciones")}">${c}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`
    : empty(
        "Todavía no hay registros",
        "Los datos que cargues aparecerán acá.",
      );
async function api(url, method = "GET", body) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET")
    headers[$("meta[name=csrf-header]").content] = $(
      "meta[name=csrf-token]",
    ).content;
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 401) {
    location.href = "/login";
    throw Error("Tu sesión terminó.");
  }
  const type = response.headers.get("content-type") || "";
  const data = type.includes("application/json") ? await response.json() : null;
  if (!response.ok)
    throw Error(
      data?.mensaje ||
        "No se pudo completar la acción. Actualizá e intentá de nuevo.",
    );
  return data;
}
let toastTimer;
function toast(text) {
  $("#toast").textContent = text;
  $("#toast").hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => ($("#toast").hidden = true), 5000);
}
function modal(title, html, submit, afterSave) {
  $("#dialog-title").textContent = title;
  $("#dialog-content").innerHTML = submit
    ? `<form id="modal-form"><div class="notice danger" id="form-error" hidden></div>${html}<div class="form-footer"><button class="primary" type="submit">Guardar</button></div></form>`
    : html;
  if (!$("#dialog").open) $("#dialog").showModal();
  if (submit)
    $("#modal-form").addEventListener(
      "invalid",
      (e) => {
        let parent = e.target.parentElement;
        while (parent) {
          if (parent.tagName === "DETAILS") parent.open = true;
          parent = parent.parentElement;
        }
      },
      true,
    );
  if (submit)
    $("#modal-form").onsubmit = async (e) => {
      e.preventDefault();
      const form = e.currentTarget;
      const button = form.querySelector("button[type=submit]");
      button.disabled = true;
      try {
        await submit(new FormData(form));
        $("#dialog").close();
        await refresh(true);
        toast("Cambios guardados");
        if (afterSave) await afterSave();
      } catch (error) {
        const errorBox = $("#form-error");
        if (errorBox) {
          errorBox.textContent = error.message;
          errorBox.hidden = false;
        } else toast(error.message);
      } finally {
        button.disabled = false;
      }
    };
}
$("#menu-close").onclick = () => $("#sidebar").classList.remove("open");
$("#dialog-close").onclick = () => $("#dialog").close();
$("#menu-toggle").onclick = () => $("#sidebar").classList.toggle("open");
const input = (label, name, value = "", type = "text", extra = "") =>
  `<label>${label}<input type="${type}" name="${name}" value="${escape(value)}" ${extra}></label>`;
const textarea = (label, name, value = "", extra = "") =>
  `<label>${label}<textarea name="${name}" ${extra}>${escape(value)}</textarea></label>`;
const select = (label, name, items, value = "", blank = false) =>
  `<label>${label}<select name="${name}">${blank ? '<option value="">Sin seleccionar</option>' : ""}${items.map(([id, text]) => `<option value="${escape(id)}" ${String(id) === String(value) ? "selected" : ""}>${escape(text)}</option>`).join("")}</select></label>`;
const check = (label, name, value) =>
  `<label class="check"><input type="checkbox" name="${name}" ${value ? "checked" : ""}>${label}</label>`;
const paymentOptions = ["EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO"].map(
  (x) => [x, x],
);
const required = "required";
const numeric = 'min="0" step="0.01" required';
const num = (f, key) => Number(f.get(key));
const optionalNum = (f, key) => (f.get(key) ? Number(f.get(key)) : null);
const activeServices = () => S.datos.catalogo.filter((x) => x.activo);
const vehicleOptions = () =>
  S.datos.vehiculos.map((v) => [v.id, `${v.patente} · ${v.marca} ${v.modelo}`]);
function nav() {
  if (S.navPage === S.page) return;
  S.navPage = S.page;
  const primary = isAdmin
    ? [
        ["ordenes", "Órdenes"],
        ["vehiculos", "Vehículos"],
        ["mantenimientos", "Próximos services"],
        ["cola", "Trabajo en curso"],
      ]
    : [["cola", "Trabajo en curso"]];
  const secondary = [
    ["resumen", "Resumen"],
    ["clientes", "Clientes"],
    ["catalogo", "Servicios"],
    ["personal", "Personal y ganancias"],
    ["usuarios", "Usuarios"],
  ];
  const link = ([id, text]) =>
    `<a href="#${id}" data-page="${id}" ${S.page === id ? 'aria-current="page"' : ""} class="${S.page === id ? "active" : ""}">${text}</a>`;
  const expanded =
    $("#admin-navigation")?.open || secondary.some(([id]) => id === S.page);
  $("#navigation").innerHTML =
    primary.map(link).join("") +
    (isAdmin
      ? `<details id="admin-navigation" ${expanded ? "open" : ""}><summary>Administración</summary>${secondary.map(link).join("")}</details>`
      : "") +
    (!readOnly
      ? '<a href="/pantalla" target="_blank" rel="noopener">Pantalla del taller</a>'
      : "");
  $("#mobile-navigation").innerHTML = isAdmin
    ? primary.slice(0, 3).map(link).join("")
    : "";
}
async function refresh(all = false) {
  if (S.loading) {
    S.refreshQueued = true;
    S.allQueued = S.allQueued || all;
    return;
  }
  S.loading = true;
  try {
    const jobs = [api("/api/ordenes").then((x) => (S.cola = x))];
    if (isAdmin) {
      if (all) jobs.push(api("/api/admin/datos").then((x) => (S.datos = x)));
      if (
        ["resumen", "ordenes", "vehiculos", "mantenimientos"].includes(
          S.page,
        ) ||
        all
      )
        jobs.push(api("/api/admin/ordenes").then((x) => (S.ordenes = x)));
      if (["resumen", "personal"].includes(S.page) || all)
        jobs.push(api("/api/admin/ganancias").then((x) => (S.ganancias = x)));
      if (S.page === "personal" || all)
        jobs.push(
          api("/api/admin/liquidaciones").then((x) => (S.liquidaciones = x)),
        );
      if (["resumen", "mantenimientos"].includes(S.page) || all)
        jobs.push(
          api("/api/admin/mantenimientos").then((x) => (S.mantenimientos = x)),
        );
      if (S.page === "mantenimientos" || all)
        jobs.push(
          api("/api/admin/recordatorios").then((x) => (S.recordatorios = x)),
        );
    } else if (!readOnly && all)
      jobs.push(api("/api/empleados").then((x) => (S.datos.empleados = x)));
    await Promise.all(jobs);
    render();
    $("#last-update").textContent =
      "Actualizado " + new Date().toLocaleTimeString("es-AR");
  } catch (e) {
    toast(e.message);
    $("#connection").textContent = "Sin conexión";
    $("#connection").classList.add("offline");
  } finally {
    S.loading = false;
    if (S.refreshQueued) {
      const pendingAll = S.allQueued;
      S.refreshQueued = false;
      S.allQueued = false;
      refresh(pendingAll);
    }
  }
}
function ordersTable(orders) {
  return table(
    ["Orden / vehículo", "Servicios", "Ingreso", "Estado", "Total / pago", ""],
    orders.map((o) => [
      `<strong>#${o.id} · ${escape(o.vehiculo.patente)}</strong><small>${escape(o.vehiculo.marca + " " + o.vehiculo.modelo)}</small>`,
      o.servicios
        .filter((s) => s.estado !== "CANCELADO")
        .map((s) => escape(s.nombre))
        .join("<br>"),
      date(o.ingreso, true),
      badge(o.estado),
      `${money(o.total)}<small>${o.pago ? "Pagado · " + o.pago.medio : "Pendiente de pago"}</small>`,
      btn("detalle", "Ver orden", o.id),
    ]),
  );
}
function render() {
  const focusId = document.activeElement?.id;
  const selection = document.activeElement?.selectionStart;
  const preserved = [
    ...document.querySelectorAll(
      "#content .toolbar input,#content .toolbar select",
    ),
  ].map((e) => ({ id: e.id, name: e.name, value: e.value }));
  nav();
  $("#breadcrumb").textContent =
    "Taller / " +
    ({
      resumen: "Resumen",
      cola: "Trabajo en curso",
      ordenes: "Órdenes",
      vehiculos: "Vehículos",
      clientes: "Clientes",
      catalogo: "Servicios",
      personal: "Personal y ganancias",
      mantenimientos: "Mantenimientos",
      usuarios: "Cuentas de acceso",
    }[S.page] || "");
  const fn = {
    resumen: dashboard,
    cola: board,
    ordenes: orders,
    vehiculos: vehicles,
    clientes: clients,
    catalogo: catalog,
    personal: staff,
    mantenimientos: maintenance,
    usuarios: users,
  };
  $("#content").innerHTML = (fn[S.page] || board)();
  for (const item of preserved) {
    const e = item.id
      ? document.getElementById(item.id)
      : document.querySelector('#content .toolbar [name="' + item.name + '"]');
    if (e) {
      e.value = item.value;
      e.dispatchEvent(new Event("input", { bubbles: true }));
    }
  }
  if (focusId && document.getElementById(focusId)) {
    const el = document.getElementById(focusId);
    el.focus({ preventScroll: true });
    if (selection != null && el.setSelectionRange)
      el.setSelectionRange(selection, selection);
  }
}
function dashboard() {
  const hoy = localDay(new Date());
  const cobrados = S.ordenes
    .filter((o) => o.pago && localDay(new Date(o.pago.fecha)) === hoy)
    .reduce((sum, o) => sum + Number(o.pago.importe), 0);
  const pendientes = S.ganancias
    .filter((g) => !g.liquidacionId)
    .reduce((n, g) => n + Number(g.importe), 0);
  const stats = [
    ["En el negocio", S.cola.length, "Vehículos activos", "◇"],
    [
      "En proceso",
      S.cola.filter((o) => o.estado === "EN_PROCESO").length,
      "Trabajos en marcha",
      "↻",
    ],
    ["Cobrado hoy", money(cobrados), "Pagos registrados", "＄"],
    [
      "Comisiones pendientes",
      money(pendientes),
      "Por liquidar al personal",
      "♙",
    ],
  ];
  return (
    heading(
      "Resumen del taller",
      "Una mirada a lo que está pasando hoy.",
      btn("nueva-orden", "Nueva orden", "", "", "primary"),
    ) +
    `<div class="stats">${stats.map(([title, value, note, icon]) => `<div class="stat"><div class="stat-title">${title}<span class="stat-icon">${icon}</span></div><div class="stat-value">${value}</div><span class="stat-note">${note}</span></div>`).join("")}</div><section class="panel"><div class="panel-header"><h2>Actividad reciente</h2><a href="#ordenes" data-page="ordenes">Ver todas las órdenes</a></div>${ordersTable(S.ordenes.slice(0, 6))}</section><div class="split"><section class="panel"><h2>Próximos services</h2>${table(
      ["Vehículo", "Mantenimiento", "Fecha", "Estado"],
      S.mantenimientos
        .filter((m) => m.estadoAdministrativo === "PENDIENTE")
        .slice(0, 4)
        .map((m) => [
          escape(m.vehiculo),
          escape(m.descripcion),
          date(m.fechaObjetivo),
          badge(m.estado),
        ]),
    )}</section></div>`
  );
}
function board() {
  const cols = [
    [
      "EN_ESPERA",
      "Por comenzar",
      (o) => ["RECIBIDO", "EN_ESPERA"].includes(o.estado),
    ],
    ["EN_PROCESO", "En proceso", (o) => o.estado === "EN_PROCESO"],
    ["TERMINADO", "Listos para retirar", (o) => o.estado === "TERMINADO"],
  ];
  return (
    heading(
      readOnly ? "El trabajo de hoy" : "Trabajo en curso",
      readOnly
        ? "Servicios y estados actualizados por el equipo."
        : "Asigná al equipo y acompañá cada etapa.",
      !readOnly
        ? btn("actualizar", "Actualizar")
        : btn("fullscreen", "Pantalla completa"),
    ) +
    `<div class="toolbar"><input id="board-search" type="search" placeholder="Buscar por patente…" aria-label="Buscar patente"></div><div class="board">${cols
      .map(
        ([status, title, test]) =>
          `<section class="board-column"><div class="column-title"><span>${title}</span>${S.cola.filter(test).length}</div>${
            S.cola
              .filter(test)
              .map(
                (o) =>
                  `<article class="order-card" data-plate="${escape(o.patente)}"><div class="card-top"><span>ORDEN #${o.id}</span><span>${date(o.ingreso, true)}</span></div><div class="plate">${escape(o.patente)}</div><div class="car-model">${escape(o.vehiculo)}</div>${o.servicios.map((s) => `<div class="service-line"><strong>${escape(s.nombre)}</strong><p>${escape(s.etapa)} · ${s.empleados.length ? s.empleados.map((e) => escape(e.nombre)).join(", ") : "Sin personal asignado"}</p>${badge(s.estado)}${!s.habilitado && !["TERMINADO", "CANCELADO"].includes(s.estado) ? "<p>Presupuesto pendiente de aprobación</p>" : ""}<progress class="progress" value="${s.etapaActual}" max="${s.etapasTotal - 1}"></progress>${!readOnly && !["TERMINADO", "CANCELADO"].includes(s.estado) ? `<div class="actions">${btn("asignar", "Asignar", o.id, `data-sid="${s.id}"`)}${s.habilitado ? btn("avanzar", s.etapaActual === s.etapasTotal - 2 ? "Terminar" : "Avanzar", o.id, `data-sid="${s.id}"`, "small primary") : ""}</div>` : ""}</div>`).join("")}${!readOnly ? `<div class="actions service-line">${btn("lectura", "Registrar km", o.vehiculoId)}${isAdmin ? btn("detalle", "Detalle", o.id) : ""}</div>` : ""}</article>`,
              )
              .join("") || '<p class="muted">Sin vehículos en esta etapa</p>'
          }</section>`,
      )
      .join("")}</div>`
  );
}
function orders() {
  return (
    heading(
      "Órdenes de trabajo",
      "Cada ingreso, desde el presupuesto hasta la entrega.",
      btn("nueva-orden", "Nueva orden", "", "", "primary"),
    ) +
    `<section class="panel"><div class="toolbar"><input id="order-search" type="search" placeholder="Buscar patente o número…" aria-label="Buscar orden"><select id="order-filter" aria-label="Filtrar estado"><option value="">Todos los estados</option>${["RECIBIDO", "EN_ESPERA", "EN_PROCESO", "TERMINADO", "ENTREGADO", "CANCELADO"].map((x) => `<option value="${x}">${labels[x]}</option>`).join("")}</select></div><div id="orders-table">${ordersTable(S.ordenes)}</div></section>`
  );
}
function vehicles() {
  return (
    heading(
      "Vehículos",
      "Una ficha, toda su historia. Autos, motos y más.",
      btn("vehiculo-form", "Nuevo vehículo", "", "", "primary"),
    ) +
    `<section class="panel"><div class="toolbar"><input id="vehicle-search" type="search" placeholder="Buscar patente, vehículo o cliente" aria-label="Buscar vehículo"></div>${table(
      ["Patente / vehículo", "Tipo", "Cliente", "Última lectura", ""],
      S.datos.vehiculos.map((v) => [
        `<strong>${escape(v.patente)}</strong><small>${escape(v.marca + " " + v.modelo)}</small>`,
        escape(v.tipo),
        escape(
          S.datos.clientes.find((c) => c.id === v.clienteId)?.nombre ||
            "Sin cliente",
        ),
        `${v.kilometraje ?? "Sin lectura"}${v.kilometraje !== null ? " km" : ""}<small>${date(v.kilometrajeFecha, true)}</small>`,
        `<div class="actions">${btn("vehiculo-historial", "Historial", v.id)}<details class="more-actions"><summary>Más opciones</summary>${btn("vehiculo-form", "Editar vehículo", v.id)}${btn("lectura", "Registrar km", v.id)}</details><a class="button small" href="${escape(v.enlace)}" target="_blank" rel="noopener">Ficha</a><a class="button small" href="/admin/vehiculos/${v.id}/qr.pdf" target="_blank" rel="noopener">QR / PDF</a></div>`,
      ]),
    )}</section>`
  );
}
function clients() {
  return (
    heading(
      "Clientes",
      "Datos de contacto y consentimiento para recordatorios.",
      btn("cliente-form", "Nuevo cliente", "", "", "primary"),
    ) +
    `<section class="panel">${table(
      ["Nombre", "Teléfono internacional", "WhatsApp", ""],
      S.datos.clientes.map((c) => [
        escape(c.nombre),
        escape(c.telefono || "—"),
        c.consentimientoWhatsapp ? badge("ACEPTADO") : "Sin consentimiento",
        btn("cliente-form", "Editar", c.id),
      ]),
    )}</section>`
  );
}
function catalog() {
  return (
    heading(
      "Servicios",
      "Precios, comisiones y etapas adaptadas a tu negocio.",
      btn("catalogo-form", "Nuevo servicio", "", "", "primary"),
    ) +
    `<section class="panel"><div class="toolbar"><label class="check"><input id="catalog-inactive" type="checkbox" ${S.includeInactive ? "checked" : ""}>Mostrar servicios desactivados</label></div>${table(
      ["Servicio", "Precio sugerido", "Comisión", "Etapas", ""],
      S.datos.catalogo
        .filter((c) => c.activo || S.includeInactive)
        .map((c) => [
          `<strong>${escape(c.nombre)}</strong><small>${c.activo ? "Activo" : "Desactivado"}</small>`,
          money(c.precio),
          c.tipoComision === "FIJA" ? money(c.comision) : `${c.comision}%`,
          c.etapas.map((e) => escape(e.nombre)).join(" "),
          btn("catalogo-form", "Editar", c.id),
        ]),
    )}</section>`
  );
}
function staff() {
  const rows = S.datos.empleados.map((e) => {
    const gs = S.ganancias.filter((g) => g.empleadoId === e.id);
    return [
      `<strong>${escape(e.nombre)}</strong><small>${e.activo ? "Activo" : "Desactivado"}</small>`,
      money(
        gs
          .filter((g) => !g.liquidacionId)
          .reduce((a, g) => a + Number(g.importe), 0),
      ),
      money(
        gs
          .filter((g) => g.liquidacionId)
          .reduce((a, g) => a + Number(g.importe), 0),
      ),
      btn("empleado-form", "Editar", e.id),
    ];
  });
  return (
    heading(
      "Personal y ganancias",
      "El trabajo de cada persona, registrado con claridad.",
      btn("empleado-form", "Empleado") +
        btn("liquidar", "Liquidar período", "", "", "primary"),
    ) +
    `<section class="panel">${table(["Empleado", "Pendiente", "Liquidado", ""], rows)}</section><section class="panel"><div class="panel-header"><h2>Movimientos de ganancias</h2>${btn("ajuste", "Registrar ajuste")}</div><div class="toolbar">${input("Desde", "desde", "", "date", 'id="gain-from"')}${input("Hasta", "hasta", "", "date", 'id="gain-to"')}${select("Empleado", "empleado", [["", "Todos"], ...S.datos.empleados.map((e) => [e.id, e.nombre])])}</div><div id="gains-table">${gainsTable(S.ganancias)}</div></section><section class="panel"><h2>Liquidaciones realizadas</h2>${table(
      ["Empleado", "Período", "Importe", "Medio", "Pagado"],
      S.liquidaciones.map((l) => [
        escape(l.empleado),
        `${date(l.desde)} — ${date(l.hasta)}`,
        money(l.importe),
        l.medio,
        date(l.fecha, true),
      ]),
    )}</section>`
  );
}
function gainsTable(items) {
  return table(
    ["Empleado", "Concepto", "Fecha", "Importe", "Liquidación"],
    items.map((g) => [
      escape(g.empleado),
      escape(g.concepto) + `<small>Servicio #${g.servicioId}</small>`,
      date(g.fecha, true),
      money(g.importe),
      g.liquidacionId ? "#" + g.liquidacionId : "Pendiente",
    ]),
  );
}
function maintenance() {
  return (
    heading(
      "Mantenimientos",
      "Acompañá al cliente después de cada visita.",
      btn("mantenimiento-form", "Programar service", "", "", "primary"),
    ) +
    `<div class="notice ${S.datos.whatsappHabilitado ? "" : "warn"}">${S.datos.whatsappHabilitado ? "WhatsApp conectado. Los avisos programados se procesan automáticamente." : "WhatsApp deshabilitado. Los mantenimientos están disponibles en la ficha QR; los mensajes se activan al configurar Meta."}</div><section class="panel">${table(
      ["Vehículo", "Mantenimiento", "Próximo service", "Estado", ""],
      S.mantenimientos.map((m) => [
        escape(m.vehiculo),
        escape(m.descripcion),
        `${date(m.fechaObjetivo)}<small>${m.kilometrajeObjetivo ? m.kilometrajeObjetivo + " km" : ""}</small>`,
        badge(m.estado),
        m.estadoAdministrativo === "PENDIENTE"
          ? `<div class="actions">${btn("mantenimiento-form", "Editar", m.id)}${btn("mantenimiento-realizar", "Realizado", m.id)}${btn("mantenimiento-cancelar", "Cancelar", m.id)}</div>`
          : "—",
      ]),
    )}</section><section class="panel"><h2>Recordatorios de WhatsApp</h2>${table(
      [
        "Vehículo / servicio",
        "Tipo",
        "Programado",
        "Estado",
        "Intentos / detalle",
      ],
      S.recordatorios.map((r) => [
        `${escape(r.vehiculo)}<small>${escape(r.descripcion)}</small>`,
        r.tipo,
        date(r.programado, true),
        badge(r.estado),
        `${r.intentos}<small>${escape(r.detalle)}</small>`,
      ]),
    )}</section>`
  );
}
function users() {
  return (
    heading(
      "Cuentas de acceso",
      "Las cuentas compartidas son independientes de los empleados.",
      btn("usuario-form", "Nueva cuenta", "", "", "primary"),
    ) +
    `<section class="panel">${table(
      ["Usuario", "Permisos", "Estado", ""],
      S.datos.usuarios.map((u) => [
        escape(u.nombre),
        u.rol,
        u.activo ? "Activo" : "Desactivado",
        btn("usuario-form", "Editar", u.id),
      ]),
    )}</section>`
  );
}
function clienteForm(id) {
  const c = S.datos.clientes.find((x) => x.id === id) || {};
  modal(
    c.id ? "Editar cliente" : "Nuevo cliente",
    input("Nombre", "nombre", c.nombre, "text", 'required maxlength="150"') +
      input(
        "Teléfono con código de país, por ejemplo +549351…",
        "telefono",
        c.telefono,
        "tel",
        'pattern="[+]?[1-9][0-9]{7,14}"',
      ) +
      check(
        "El cliente autorizó recibir recordatorios de mantenimiento",
        "consentimiento",
        c.consentimientoWhatsapp,
      ),
    (f) =>
      api(
        "/api/admin/clientes" + (c.id ? "/" + c.id : ""),
        c.id ? "PUT" : "POST",
        {
          nombre: f.get("nombre"),
          telefono: f.get("telefono"),
          consentimientoWhatsapp: f.has("consentimiento"),
        },
      ),
  );
}
function vehiculoForm(id) {
  const v = S.datos.vehiculos.find((x) => x.id === id) || {};
  modal(
    v.id ? "Editar vehículo" : "Nuevo vehículo",
    `<div class="form-grid">${input("Patente", "patente", v.patente, "text", 'required maxlength="20"')}${select(
      "Tipo",
      "tipo",
      ["AUTO", "MOTO", "CAMIONETA", "OTRO"].map((x) => [x, x]),
      v.tipo,
    )}${input("Marca", "marca", v.marca, "text", required)}${input("Modelo", "modelo", v.modelo, "text", required)}<div class="wide">${select(
      "Cliente",
      "clienteId",
      S.datos.clientes.map((c) => [c.id, c.nombre]),
      v.clienteId,
      true,
    )}</div></div>${v.id ? '<div class="notice warn">Cambiar el propietario renueva el QR y desactiva los avisos anteriores.</div>' : ""}`,
    (f) =>
      api(
        "/api/admin/vehiculos" + (v.id ? "/" + v.id : ""),
        v.id ? "PUT" : "POST",
        {
          patente: f.get("patente"),
          tipo: f.get("tipo"),
          marca: f.get("marca"),
          modelo: f.get("modelo"),
          clienteId: optionalNum(f, "clienteId"),
        },
      ),
  );
}
function empleadoForm(id) {
  const e = S.datos.empleados.find((x) => x.id === id) || {};
  modal(
    e.id ? "Editar empleado" : "Nuevo empleado",
    input("Nombre", "nombre", e.nombre, "text", required) +
      check("Empleado activo", "activo", e.activo ?? true),
    (f) =>
      api(
        "/api/admin/empleados" + (e.id ? "/" + e.id : ""),
        e.id ? "PUT" : "POST",
        { nombre: f.get("nombre"), activo: f.has("activo") },
      ),
  );
}
function usuarioForm(id) {
  const u = S.datos.usuarios.find((x) => x.id === id) || {};
  modal(
    u.id ? "Editar cuenta" : "Nueva cuenta",
    input("Nombre de usuario", "nombre", u.nombre, "text", required) +
      input(
        u.id
          ? "Nueva contraseña (opcional)"
          : "Contraseña (mínimo 12 caracteres)",
        "password",
        "",
        "password",
        (u.id ? "" : "required ") +
          'minlength="12" maxlength="72" autocomplete="new-password"',
      ) +
      select(
        "Permisos",
        "rol",
        ["ADMIN", "OPERACION", "PANTALLA"].map((x) => [x, x]),
        u.rol || "OPERACION",
      ) +
      check("Cuenta activa", "activo", u.activo ?? true),
    (f) =>
      api(
        "/api/admin/usuarios" + (u.id ? "/" + u.id : ""),
        u.id ? "PUT" : "POST",
        {
          nombre: f.get("nombre"),
          password: f.get("password") || null,
          rol: f.get("rol"),
          activo: f.has("activo"),
        },
      ),
  );
}
const etapaRow = (e, fixed = false) =>
  `<div class="stage-row">${input("Nombre de la etapa", "etapaNombre", e.nombre, "text", 'required maxlength="100"')}${select("Estado", "etapaEstado", [fixed ? [e.estado, labels[e.estado]] : ["EN_PROCESO", "En proceso"]], e.estado)}${!fixed ? '<button type="button" data-action="quitar-etapa">Quitar etapa</button>' : ""}</div>`;
function catalogoForm(id) {
  const c = S.datos.catalogo.find((x) => x.id === id) || {};
  const etapas = c.etapas || [
    { nombre: "En espera", estado: "EN_ESPERA" },
    { nombre: "En reparación", estado: "EN_PROCESO" },
    { nombre: "Terminado", estado: "TERMINADO" },
  ];
  modal(
    c.id ? "Editar servicio" : "Nuevo servicio",
    input(
      "Nombre del servicio",
      "nombre",
      c.nombre,
      "text",
      'required maxlength="150"',
    ) +
      input(
        "Precio de mano de obra",
        "precio",
        c.precio ?? 0,
        "number",
        numeric,
      ) +
      `<details class="form-options"><summary>Comisión y etapas</summary>${select(
        "Comisión del personal",
        "tipoComision",
        [
          ["PORCENTAJE", "Porcentaje de la mano de obra"],
          ["FIJA", "Importe fijo"],
        ],
        c.tipoComision,
      )}${input("Porcentaje o importe", "comision", c.comision ?? 0, "number", numeric)}<h3>Etapas del servicio</h3><p class="fine">Podés agregar diagnóstico, reparación o prueba. El primer y último estado se conservan.</p><div id="stage-list">${etapas.map((e, i) => etapaRow(e, i === 0 || i === etapas.length - 1 || e.estado === "EN_ESPERA")).join("")}</div><button type="button" data-action="agregar-etapa">Agregar etapa</button></details>` +
      check("Disponible para nuevas órdenes", "activo", c.activo ?? true),
    (f) =>
      api(
        "/api/admin/catalogo" + (c.id ? "/" + c.id : ""),
        c.id ? "PUT" : "POST",
        {
          nombre: f.get("nombre"),
          precio: num(f, "precio"),
          tipoComision: f.get("tipoComision"),
          comision: num(f, "comision"),
          activo: f.has("activo"),
          etapas: f.getAll("etapaNombre").map((nombre, i) => ({
            nombre,
            estado: f.getAll("etapaEstado")[i],
          })),
        },
      ),
  );
}
function nuevaOrden() {
  if (!S.datos.vehiculos.length || !activeServices().length) {
    toast("Primero cargá un vehículo y al menos un servicio.");
    return;
  }
  modal(
    "Ingresar vehículo",
    select("Vehículo", "vehiculoId", vehicleOptions()) +
      `<label>Servicios</label><div class="check-list">${activeServices()
        .map(
          (c) =>
            `<div class="detail-block">${check(escape(c.nombre), "servicio-" + c.id, false)}${input("Precio acordado", "precio-" + c.id, c.precio, "number", numeric)}</div>`,
        )
        .join("")}</div>` +
      select(
        "Método de pago previsto",
        "medioPrevisto",
        paymentOptions,
        "",
        true,
      ) +
      textarea(
        "Observaciones internas (no visibles al cliente)",
        "notasInternas",
        "",
        'maxlength="2000"',
      ),
    async (f) => {
      const servicios = activeServices()
        .filter((c) => f.has("servicio-" + c.id))
        .map((c) => ({ catalogoId: c.id, precio: num(f, "precio-" + c.id) }));
      if (!servicios.length) throw Error("Seleccioná al menos un servicio.");
      await api("/api/admin/ordenes", "POST", {
        vehiculoId: num(f, "vehiculoId"),
        servicios,
        notasInternas: f.get("notasInternas"),
        medioPrevisto: f.get("medioPrevisto") || null,
      });
    },
  );
}
async function detalle(id) {
  const o = await api("/api/admin/ordenes/" + id);
  const open = !["ENTREGADO", "CANCELADO"].includes(o.estado);
  const editable = open && !o.pago && o.estado !== "TERMINADO";
  const bloqueo = !open
    ? "Orden cerrada. Se conserva el historial."
    : o.pago
      ? "El pago ya fue registrado. No se modifican sus importes."
      : "Para cambiar repuestos o precios, reabrí el servicio desde Más opciones.";
  S.detalle = o;
  const service = (s) => {
    const activo = open && !["TERMINADO", "CANCELADO"].includes(s.estado);
    const aprobado = o.versionAprobada === o.presupuestoVersion;
    return `<article class="detail-block"><div class="panel-header"><h3>${escape(s.nombre)}</h3>${badge(s.estado)}</div><p>${escape(s.etapa)} · ${money(s.precio)}</p><p class="muted">${s.empleados.map((e) => escape(e.nombre)).join(", ") || "Sin personal asignado"}</p>${s.resumenPublico ? `<p class="work-description">${escape(s.resumenPublico)}</p>` : ""}<div class="actions">${activo ? btn("asignar", "Asignar personal", id, `data-sid="${s.id}"`) + (aprobado ? btn("avanzar", "Siguiente etapa", id, `data-sid="${s.id}"`, "primary") : "") : ""}${o.estado !== "CANCELADO" && s.estado !== "CANCELADO" ? btn("resumen", "Qué se hizo", id, `data-sid="${s.id}"`) : ""}</div><details class="more-actions"><summary>Más opciones</summary><p>Comisión: ${s.tipoComision === "FIJA" ? money(s.comision) : s.comision + "%"}</p>${open && s.estado === "TERMINADO" ? btn("reabrir", "Reabrir servicio", id, `data-sid="${s.id}"`) : ""}${activo && !o.pago ? btn("precio", "Cambiar precio", id, `data-sid="${s.id}"`) + btn("cancelar-servicio", "Cancelar servicio", id, `data-sid="${s.id}"`, "danger-button") : ""}</details></article>`;
  };
  modal(
    "Orden #" + o.id,
    `<div class="detail-header"><div><h2>${escape(o.vehiculo.patente)}</h2><p>${escape(o.vehiculo.marca + " " + o.vehiculo.modelo)} · ${o.cliente ? escape(o.cliente.nombre) : "Sin cliente"}</p></div>${badge(o.estado)}</div><p class="fine">Ingreso: ${date(o.ingreso, true)}</p><div class="order-total"><div><span>Total de la orden</span><strong>${money(o.total)}</strong><small>${o.pago ? "Pagado · " + o.pago.medio : "Pendiente de pago"}</small></div><div class="actions">${open && !o.pago ? btn("cobrar", "Registrar pago", id) : ""}${o.estado === "TERMINADO" && o.pago ? btn("entregar", "Entregar vehículo", id, "", "primary") : ""}</div></div>${open && o.versionAprobada !== o.presupuestoVersion ? `<div class="notice"><p>El presupuesto necesita aprobación para continuar.</p>${btn("aprobar", "Aprobar presupuesto", id, "", "primary")}</div>` : ""}<div class="actions document-actions"><a class="button" href="${escape(o.vehiculo.enlace)}" target="_blank" rel="noopener">Ver ficha</a><a class="button" href="/admin/ordenes/${id}/ticket.pdf" target="_blank" rel="noopener">Descargar ticket</a><a class="button" href="/admin/vehiculos/${o.vehiculo.id}/qr.pdf" target="_blank" rel="noopener">Descargar QR</a></div><h3>Trabajos</h3>${o.servicios.map(service).join("")}${editable ? btn("agregar-servicio", "Agregar trabajo", id) : ""}<section class="parts-section"><div class="panel-header"><h3>Repuestos</h3>${editable ? btn("repuesto", "Agregar repuesto", id) : ""}</div>${
      o.repuestos.length
        ? table(
            [
              "Repuesto",
              "Cantidad",
              "Costo interno",
              "Precio unitario",
              "Subtotal",
              "Acciones",
            ],
            o.repuestos.map((r) => [
              escape(r.nombre),
              r.cantidad,
              money(r.costo),
              money(r.precio),
              money(r.precio * r.cantidad),
              editable
                ? `<div class="actions">${btn("editar-repuesto", "Editar", id, `data-rid="${r.id}"`)}${btn("quitar-repuesto", "Quitar", id, `data-rid="${r.id}"`)}</div>`
                : "—",
            ]),
          )
        : '<p class="muted">Sin repuestos cargados.</p>'
    }${!editable ? `<p class="fine">${bloqueo}</p>` : '<p class="fine">Los cambios requieren aprobar el nuevo presupuesto.</p>'}</section><details class="form-options"><summary>Notas y administración</summary>${o.notasInternas ? `<p class="work-description">${escape(o.notasInternas)}</p>` : "<p>Sin notas internas.</p>"}<p class="fine">Estas notas no aparecen en la ficha pública.</p>${open && !o.pago ? btn("cancelar-orden", "Cancelar orden", id, "", "danger-button") : ""}</details><details class="form-options"><summary>Presupuestos e historial</summary>${o.presupuestos.map((p) => `<div class="detail-block"><strong>Presupuesto ${p.numero} · ${money(p.total)}</strong><p class="work-description">${escape(p.detalle)}</p><small>${date(p.fecha, true)} · ${escape(p.actor)}</small></div>`).join("")}<h3>Actividad</h3><div class="timeline">${o.historial.map((h) => `<div>${escape(h.accion)}<small>${date(h.fecha, true)} · ${escape(h.actor)}</small></div>`).join("")}</div></details>`,
  );
}
async function orderAction(action, id, sid, rid) {
  const o = await api("/api/admin/ordenes/" + id);
  const orderModal = (title, html, submit) =>
    modal(title, html, submit, () => detalle(id));
  const version = o.version;
  const base = "/api/admin/ordenes/" + id;
  const s = o.servicios.find((s) => s.id === sid);
  if (action === "cobrar") {
    orderModal(
      "Registrar pago completo",
      `<div class="notice">Importe a cobrar: <strong>${money(o.total)}</strong>. Este registro no procesa pagos electrónicos.</div>` +
        select("Medio de pago", "medio", paymentOptions, o.medioPrevisto),
      (f) => api(base + "/cobrar", "POST", { version, medio: f.get("medio") }),
    );
    return;
  }
  if (action === "precio") {
    orderModal(
      "Cambiar precio acordado",
      input("Nuevo precio", "precio", s.precio, "number", numeric) +
        input("Motivo", "motivo", "", "text", required),
      (f) =>
        api(base + "/servicios/" + sid + "/precio", "PUT", {
          version,
          precio: num(f, "precio"),
          motivo: f.get("motivo"),
        }),
    );
    return;
  }
  if (action === "resumen") {
    orderModal(
      "Qué se le hizo al vehículo",
      `<div class="notice">Este detalle se muestra al cliente en su ficha cuando el servicio está terminado. Describí los trabajos realizados, controles y recomendaciones; evitá datos privados.</div>` +
        textarea(
          "Detalle del trabajo visible al cliente",
          "resumenPublico",
          s.resumenPublico || "",
          'required maxlength="2000" rows="6"',
        ),
      (f) =>
        api(base + "/servicios/" + sid + "/resumen", "PUT", {
          version,
          resumenPublico: f.get("resumenPublico"),
        }),
    );
    return;
  }
  if (["repuesto", "editar-repuesto"].includes(action)) {
    const r =
      action === "editar-repuesto"
        ? o.repuestos.find((r) => r.id === rid)
        : null;
    orderModal(
      r ? "Editar repuesto" : "Agregar repuesto",
      `<div class="notice">El costo es interno. El precio de venta se suma al presupuesto según la cantidad.</div>` +
        input(
          "Nombre",
          "nombre",
          r?.nombre || "",
          "text",
          required + ' maxlength="150"',
        ) +
        `<div class="form-grid">${input("Cantidad", "cantidad", r?.cantidad ?? 1, "number", 'required min="1" max="10000"')}${input("Costo interno unitario", "costo", r?.costo ?? 0, "number", numeric)}${input("Precio de venta unitario", "precio", r?.precio ?? 0, "number", numeric)}</div>`,
      (f) =>
        api(base + "/repuestos" + (r ? "/" + r.id : ""), r ? "PUT" : "POST", {
          version,
          nombre: f.get("nombre"),
          cantidad: num(f, "cantidad"),
          costo: num(f, "costo"),
          precio: num(f, "precio"),
        }),
    );
    return;
  }
  if (action === "agregar-servicio") {
    orderModal(
      "Agregar servicio",
      select(
        "Servicio",
        "catalogoId",
        activeServices().map((c) => [c.id, c.nombre]),
      ) +
        input(
          "Precio acordado",
          "precio",
          activeServices()[0]?.precio || 0,
          "number",
          numeric,
        ),
      (f) =>
        api(base + "/servicios?version=" + version, "POST", {
          catalogoId: num(f, "catalogoId"),
          precio: num(f, "precio"),
        }),
    );
    return;
  }
  const map = {
    aprobar: [
      "Aprobar presupuesto",
      "Registrá el medio por el que el cliente aceptó el presupuesto.",
      "/aprobar",
    ],
    entregar: [
      "Entregar vehículo",
      "El trabajo está terminado y el cobro registrado.",
      "/entregar",
    ],
    "cancelar-orden": [
      "Cancelar orden",
      "La cancelación queda registrada en el historial.",
      "/cancelar",
    ],
    reabrir: [
      "Reabrir servicio",
      "Se revierten las comisiones de esta finalización mediante ajustes.",
      "/servicios/" + sid + "/reabrir",
    ],
    "cancelar-servicio": [
      "Cancelar servicio",
      "Se generará una nueva versión del presupuesto.",
      "/servicios/" + sid + "/cancelar",
    ],
    "quitar-repuesto": [
      "Quitar repuesto",
      "Se generará una nueva versión del presupuesto.",
      "/repuestos/" + rid + "/quitar",
    ],
  };
  const [title, note, path] = map[action];
  orderModal(
    title,
    `<div class="notice warn">${note}</div>` +
      input(
        action === "aprobar"
          ? "Constancia de aprobación"
          : "Motivo / observación",
        "motivo",
        "",
        "text",
        action === "entregar" ? "" : required,
      ),
    (f) => api(base + path, "POST", { version, motivo: f.get("motivo") || "" }),
  );
}
async function operationalAction(action, id, sid) {
  const cola = await api("/api/ordenes");
  const returnToOrder =
    isAdmin && $("#dialog").open ? () => detalle(id) : undefined;
  const operationModal = (title, html, submit) =>
    modal(title, html, submit, returnToOrder);
  const o = cola.find((x) => x.id === id);
  if (!o) throw Error("La orden ya no está activa.");
  const s = o.servicios.find((x) => x.id === sid);
  if (action === "asignar") {
    const emps = await api("/api/empleados");
    operationModal(
      "Asignar personal · " + s.nombre,
      `<div class="check-list">${emps
        .map((e) =>
          check(
            escape(e.nombre),
            "empleado-" + e.id,
            s.empleados.some((x) => x.id === e.id),
          ),
        )
        .join("")}</div>`,
      async (f) => {
        const empleados = emps
          .filter((e) => f.has("empleado-" + e.id))
          .map((e) => e.id);
        if (!empleados.length) throw Error("Seleccioná al menos un empleado.");
        await api(`/api/ordenes/${id}/servicios/${sid}/asignar`, "POST", {
          version: o.version,
          empleados,
        });
      },
    );
  } else
    operationModal(
      "Avanzar servicio · " + s.nombre,
      `<div class="notice">Etapa actual: ${escape(s.etapa)}. El cambio se verá en el taller y en la ficha del cliente.</div>` +
        textarea(
          "Resumen para el cliente (visible al terminar)",
          "resumenPublico",
          s.resumenPublico,
          'maxlength="2000"',
        ),
      (f) =>
        api(`/api/ordenes/${id}/servicios/${sid}/avanzar`, "POST", {
          version: o.version,
          resumenPublico: f.get("resumenPublico"),
        }),
    );
}
function lectura(id) {
  const v = S.datos.vehiculos.find((v) => v.id === id);
  modal(
    "Registrar kilometraje",
    input(
      "Kilómetros",
      "kilometraje",
      v?.kilometraje ?? "",
      "number",
      'required min="0" step="1"',
    ) +
      input(
        "Motivo de la lectura",
        "motivo",
        "Ingreso al negocio",
        "text",
        required,
      ) +
      (isAdmin
        ? check(
            "Corrección administrativa (permite disminuir la lectura)",
            "correccion",
            false,
          )
        : ""),
    (f) =>
      api("/api/vehiculos/" + id + "/kilometraje", "POST", {
        kilometraje: num(f, "kilometraje"),
        motivo: f.get("motivo"),
        correccion: f.has("correccion"),
      }),
  );
}
function mantenimientoForm(id) {
  const m = S.mantenimientos.find((m) => m.id === id) || {};
  const terminados = S.ordenes.flatMap((o) =>
    o.servicios
      .filter((s) => s.estado === "TERMINADO")
      .map((s) => [s.id, `${o.vehiculo.patente} · #${o.id} · ${s.nombre}`]),
  );
  modal(
    m.id ? "Reprogramar mantenimiento" : "Programar mantenimiento",
    select("Vehículo", "vehiculoId", vehicleOptions(), m.vehiculoId) +
      input(
        "Descripción",
        "descripcion",
        m.descripcion,
        "text",
        'required maxlength="300"',
      ) +
      select(
        "Servicio terminado que origina la recomendación",
        "origenId",
        terminados,
        m.origenId,
        true,
      ) +
      `<div class="form-grid">${input("Fecha del próximo service", "fechaObjetivo", m.fechaObjetivo, "date")}${input("Kilometraje objetivo", "kilometrajeObjetivo", m.kilometrajeObjetivo, "number", 'min="1" step="1"')}${input("Avisar con cuántos días de anticipación", "anticipacionDias", m.anticipacionDias ?? 7, "number", 'required min="0" max="365"')}</div>` +
      check(
        "Enviar recordatorios por WhatsApp (requiere consentimiento)",
        "avisos",
        m.avisos ?? false,
      ) +
      `<p class="fine">Completá al menos fecha o kilometraje. Se aplica el límite que se alcance primero. Por fecha se avisa a las 09:00 y no se reserva un turno.</p>`,
    (f) => {
      const datos = {
        vehiculoId: num(f, "vehiculoId"),
        origenId: optionalNum(f, "origenId"),
        descripcion: f.get("descripcion"),
        fechaObjetivo: f.get("fechaObjetivo") || null,
        kilometrajeObjetivo: optionalNum(f, "kilometrajeObjetivo"),
        anticipacionDias: num(f, "anticipacionDias"),
        avisos: f.has("avisos"),
      };
      return api(
        "/api/admin/mantenimientos" + (m.id ? "/" + m.id : ""),
        m.id ? "PUT" : "POST",
        m.id ? { version: m.version, datos } : datos,
      );
    },
  );
}
function liquidar() {
  const hoy = localDay(new Date());
  modal(
    "Liquidar ganancias por período",
    select(
      "Empleado",
      "empleadoId",
      S.datos.empleados.map((e) => [e.id, e.nombre]),
    ) +
      `<div class="form-grid">${input("Desde", "desde", hoy.slice(0, 8) + "01", "date", required)}${input("Hasta", "hasta", hoy, "date", required)}</div>` +
      select("Medio de pago", "medio", paymentOptions) +
      `<div class="notice">Se registrará el pago de todas las ganancias pendientes de este empleado dentro del período, incluidos los ajustes.</div>`,
    (f) =>
      api("/api/admin/liquidaciones", "POST", {
        empleadoId: num(f, "empleadoId"),
        desde: f.get("desde"),
        hasta: f.get("hasta"),
        medio: f.get("medio"),
      }),
  );
}
function ajuste() {
  const key = crypto.randomUUID();
  modal(
    "Ajuste de ganancias",
    select(
      "Empleado",
      "empleadoId",
      S.datos.empleados.map((e) => [e.id, e.nombre]),
    ) +
      input(
        "ID del servicio (figura en el registro de ganancias)",
        "servicioId",
        "",
        "number",
        'required min="1"',
      ) +
      input(
        "Importe del ajuste (negativo para descontar)",
        "importe",
        "",
        "number",
        'required step="0.01"',
      ) +
      input("Motivo", "motivo", "", "text", required) +
      `<p class="fine">El ajuste se registra separado de las ganancias y liquidaciones originales.</p>`,
    (f) =>
      api("/api/admin/ganancias/ajustes", "POST", {
        empleadoId: num(f, "empleadoId"),
        servicioId: num(f, "servicioId"),
        importe: num(f, "importe"),
        motivo: f.get("motivo"),
        clave: key,
      }),
  );
}
document.addEventListener("click", async (e) => {
  const page = e.target.closest("[data-page]");
  if (page) {
    e.preventDefault();
    S.page = page.dataset.page;
    location.hash = S.page;
    $("#sidebar").classList.remove("open");
    await refresh(true);
    return;
  }
  const b = e.target.closest("[data-action]");
  if (!b) return;
  e.preventDefault();
  const action = b.dataset.action,
    id = Number(b.dataset.id),
    sid = Number(b.dataset.sid),
    rid = Number(b.dataset.rid);
  try {
    if (action === "agregar-etapa") {
      const list = $("#stage-list");
      if (list.children.length >= 20)
        return toast("Podés usar hasta 20 etapas.");
      list.lastElementChild.insertAdjacentHTML(
        "beforebegin",
        etapaRow({ nombre: "", estado: "EN_PROCESO" }),
      );
      list.lastElementChild.previousElementSibling
        .querySelector("input")
        .focus();
      return;
    }
    if (action === "quitar-etapa") {
      b.closest(".stage-row").remove();
      return;
    }
    const forms = {
      "cliente-form": clienteForm,
      "vehiculo-form": vehiculoForm,
      "empleado-form": empleadoForm,
      "usuario-form": usuarioForm,
      "catalogo-form": catalogoForm,
      "mantenimiento-form": mantenimientoForm,
    };
    if (forms[action]) return forms[action](id);
    if (action === "nueva-orden") return nuevaOrden();
    if (action === "detalle") return await detalle(id);
    if (action === "lectura") return lectura(id);
    if (action === "liquidar") return liquidar();
    if (action === "ajuste") return ajuste();
    if (action === "actualizar") return await refresh(true);
    if (action === "fullscreen") {
      if (document.fullscreenElement) await document.exitFullscreen();
      else await document.documentElement.requestFullscreen();
      return;
    }
    if (["asignar", "avanzar"].includes(action))
      return await operationalAction(action, id, sid);
    if (action === "vehiculo-historial") {
      const lecturas = await api("/api/admin/vehiculos/" + id + "/lecturas");
      modal(
        "Historial del vehículo",
        `<h3>Órdenes</h3>${ordersTable(S.ordenes.filter((o) => o.vehiculo.id === id))}<h3>Lecturas de kilometraje</h3>${table(
          ["Km", "Fecha", "Usuario", "Motivo"],
          lecturas.map((l) => [
            l.kilometraje,
            date(l.fecha, true),
            escape(l.actor),
            escape(l.motivo),
          ]),
        )}`,
      );
      return;
    }
    if (action === "mantenimiento-realizar") {
      const m = S.mantenimientos.find((x) => x.id === id);
      const servicios = S.ordenes
        .filter((o) => o.vehiculo.id === m.vehiculoId)
        .flatMap((o) =>
          o.servicios
            .filter((s) => s.estado === "TERMINADO")
            .map((s) => [s.id, `Orden #${o.id} · ${s.nombre}`]),
        );
      if (!servicios.length)
        throw Error("Primero terminá el servicio de taller correspondiente.");
      modal(
        "Marcar mantenimiento realizado",
        select(
          "Servicio que cumplió el mantenimiento",
          "servicioId",
          servicios,
        ),
        (f) =>
          api("/api/admin/mantenimientos/" + id + "/realizar", "POST", {
            version: m.version,
            servicioId: num(f, "servicioId"),
          }),
      );
      return;
    }
    if (action === "mantenimiento-cancelar") {
      const m = S.mantenimientos.find((x) => x.id === id);
      modal(
        "Cancelar mantenimiento",
        '<div class="notice warn">Se cancelarán los avisos pendientes de este mantenimiento.</div>',
        () =>
          api("/api/admin/mantenimientos/" + id + "/cancelar", "POST", {
            version: m.version,
          }),
      );
      return;
    }
    await orderAction(action, id, sid, rid);
  } catch (error) {
    toast(error.message);
  }
});
document.addEventListener("input", (e) => {
  if (e.target.id === "catalog-inactive") {
    const changed = Boolean(S.includeInactive) !== e.target.checked;
    S.includeInactive = e.target.checked;
    if (changed) render();
    return;
  }
  if (e.target.id === "vehicle-search") {
    const q = e.target.value.toLocaleUpperCase("es-AR");
    document
      .querySelectorAll("#content tbody tr")
      .forEach(
        (row) =>
          (row.hidden = !row.textContent
            .toLocaleUpperCase("es-AR")
            .includes(q)),
      );
  }
  if (e.target.id === "board-search")
    document
      .querySelectorAll("[data-plate]")
      .forEach(
        (c) =>
          (c.hidden = !c.dataset.plate.includes(e.target.value.toUpperCase())),
      );
  if (["order-search", "order-filter"].includes(e.target.id)) {
    const q = $("#order-search").value.toUpperCase(),
      st = $("#order-filter").value;
    $("#orders-table").innerHTML = ordersTable(
      S.ordenes.filter(
        (o) =>
          (o.vehiculo.patente.includes(q) || String(o.id).includes(q)) &&
          (!st || o.estado === st),
      ),
    );
  }
  if (S.page === "personal" && e.target.closest(".toolbar")) {
    const from = $("#gain-from").value,
      to = $("#gain-to").value,
      employee = $("select[name=empleado]").value;
    $("#gains-table").innerHTML = gainsTable(
      S.ganancias.filter(
        (g) =>
          (!employee || String(g.empleadoId) === employee) &&
          (!from || localDay(new Date(g.fecha)) >= from) &&
          (!to || localDay(new Date(g.fecha)) <= to),
      ),
    );
  }
});
$("#today").textContent = new Date().toLocaleDateString("es-AR", {
  weekday: "long",
  day: "numeric",
  month: "long",
});
if (
  isAdmin &&
  [
    "resumen",
    "cola",
    "ordenes",
    "vehiculos",
    "clientes",
    "catalogo",
    "personal",
    "mantenimientos",
    "usuarios",
  ].includes(location.hash.slice(1))
)
  S.page = location.hash.slice(1);
let connected = false;
const events = new EventSource("/api/eventos/cola");
events.onopen = () => {
  connected = true;
  $("#connection").textContent = "En vivo";
  $("#connection").classList.remove("offline");
};
events.addEventListener("actualizar", () => refresh());
events.onerror = () => {
  connected = false;
  $("#connection").textContent = "Reconectando";
  $("#connection").classList.add("offline");
};
setInterval(() => {
  if (!document.hidden) refresh();
}, 10000);
refresh(true);
