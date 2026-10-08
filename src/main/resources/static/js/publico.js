"use strict";
const $ = (s) => document.querySelector(s);
const esc = (x) =>
  String(x ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const mode = document.body.dataset.modo;
const endpoint =
  "/api/" + mode + "/" + encodeURIComponent(document.body.dataset.token);
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
  KILOMETRAJE_SIN_VERIFICAR: "Kilómetros sin verificar",
};
const badge = (s) =>
  `<span class="badge ${esc(s)}">${esc(labels[s] || s)}</span>`;
const date = (d) =>
  d
    ? (d.length === 10
        ? new Date(d + "T12:00:00")
        : new Date(d)
      ).toLocaleDateString("es-AR", {
        timeZone: "America/Argentina/Cordoba",
        day: "2-digit",
        month: "long",
        year: "numeric",
      })
    : "Sin fecha definida";
const km = (value) =>
  value == null ? "Sin lectura" : value.toLocaleString("es-AR") + " km";
const finishedServices = (t) =>
  t.servicios.filter((s) => s.estado === "TERMINADO");
const serviceDate = (t) =>
  finishedServices(t)
    .map((s) => s.finalizacion)
    .filter(Boolean)
    .sort()
    .at(-1) || t.finalizacion;
const workDetails = (t) =>
  `<div class="completed-services">${finishedServices(t)
    .map(
      (s) =>
        `<div class="completed-service"><span class="work-check" aria-hidden="true">✓</span><div><h3>${esc(s.nombre)}</h3><p class="work-description">${esc(s.resumen || "El taller todavía no cargó un detalle de este trabajo.")}</p></div></div>`,
    )
    .join(
      "",
    )}</div>${t.repuestos.length ? `<div class="public-parts"><h3>Repuestos de esta visita</h3><ul>${t.repuestos.map((r) => `<li>${esc(r)}</li>`).join("")}</ul></div>` : ""}`;
function renderFicha(d) {
  const history = d.trabajos
    .filter((t) => finishedServices(t).length)
    .sort((a, b) => (serviceDate(b) || "").localeCompare(serviceDate(a) || ""));
  const latest = history[0];
  const pending = d.trabajos.filter((t) =>
    t.servicios.some((s) => !["TERMINADO", "CANCELADO"].includes(s.estado)),
  );
  const priorities = { VENCIDO: 0, VENCE_HOY: 1, PROXIMO: 2, VIGENTE: 3 };
  const maintenance = [...d.mantenimientos].sort(
    (a, b) => (priorities[a.estado] ?? 4) - (priorities[b.estado] ?? 4),
  );
  return `<section class="public-hero vehicle-passport"><div class="passport-top"><span class="eyebrow">TU FICHA DE TALLER</span><span class="passport-tag">Historial permanente</span></div><h1>${esc(d.vehiculo)}</h1><p>Patente ${esc(d.patente)} · Cada visita, en un mismo lugar.</p><div class="passport-stats"><div><span>Último service</span><strong>${latest ? date(serviceDate(latest)) : "Sin registrar"}</strong></div><div><span>Última lectura</span><strong>${km(d.kilometraje)}</strong></div><div><span>Visitas con trabajos realizados</span><strong>${history.length}</strong></div></div></section>
  ${pending.length ? `<section class="panel current-work"><span class="eyebrow">EN EL TALLER AHORA</span><h2>Así va tu vehículo</h2>${pending.map((t) => `<div class="current-services">${t.servicios.map((s) => `<div class="service-line"><div class="panel-header"><strong>${esc(s.nombre)}</strong>${badge(s.estado)}</div><p>${esc(s.etapa)}</p></div>`).join("")}</div>`).join("")}</section>` : ""}
  <div class="ficha-grid"><section class="panel latest-service"><span class="eyebrow">${latest ? "ÚLTIMO SERVICE" : "TUS TRABAJOS REALIZADOS"}</span><h2>Qué se le hizo</h2>${latest ? `<div class="visit-meta"><span>${date(serviceDate(latest))}</span><span>${latest.kilometraje == null ? "Kilometraje de ingreso sin registrar" : km(latest.kilometraje)}</span></div>${workDetails(latest)}` : `<div class="ficha-empty"><span class="empty-symbol" aria-hidden="true">✓</span><h3>Tu historial empieza con el primer service</h3><p>Todavía no hay trabajos de taller terminados registrados. Acá vas a ver qué se hizo, los controles y los repuestos de cada visita.</p></div>`}</section>
  <section class="panel next-service"><span class="eyebrow">PARA SEGUIR CUIDÁNDOLO</span><h2>Próximos mantenimientos</h2>${maintenance.length ? maintenance.map((m) => `<article class="maintenance-card maintenance-${esc(m.estado)}"><div class="panel-header">${badge(m.estado)}</div><h3>${esc(m.descripcion)}</h3><dl><div><dt>Fecha prevista</dt><dd>${date(m.fechaObjetivo)}</dd></div>${m.kilometrajeObjetivo != null ? `<div><dt>Kilometraje objetivo</dt><dd>${km(m.kilometrajeObjetivo)}</dd></div>` : ""}</dl>${m.estado === "VENCIDO" ? '<p class="maintenance-advice">Se alcanzó la fecha o el kilometraje previsto. Coordiná el próximo service con el taller.</p>' : m.estado === "VENCE_HOY" ? '<p class="maintenance-advice">El mantenimiento está previsto para hoy.</p>' : m.estado === "PROXIMO" ? '<p class="maintenance-advice">Se acerca la fecha de tu próximo service.</p>' : ""}${m.kilometrajeObjetivo != null && !m.kilometrajeVerificado ? "<p>Kilometraje sin verificar.</p>" : ""}</article>`).join("") : '<div class="ficha-empty"><h3>Sin programación</h3><p>El taller todavía no definió el próximo mantenimiento.</p></div>'}<p class="fine mileage-note">Última lectura: ${km(d.kilometraje)}${d.kilometrajeFecha ? " · " + date(d.kilometrajeFecha) : ""}. El estado por kilómetros depende de esta lectura. Si hay fecha y kilómetros, se aplica el primer límite alcanzado.</p></section></div>
  <section class="panel history-section"><div class="panel-header"><div><span class="eyebrow">EL RECORRIDO DE TU VEHÍCULO</span><h2>Visitas anteriores</h2></div><span class="history-count">${Math.max(0, history.length - 1)}</span></div>${
    history.length > 1
      ? `<div class="public-timeline">${history
          .slice(1)
          .map(
            (t) =>
              `<article><div class="visit-meta"><span>${date(serviceDate(t))}</span><span>${t.kilometraje == null ? "" : km(t.kilometraje)}</span></div>${workDetails(t)}</article>`,
          )
          .join("")}</div>`
      : '<p class="muted">Las próximas visitas se sumarán a este historial. El último service aparece destacado arriba.</p>'
  }</section>
  <aside class="qr-reminder"><span aria-hidden="true">↗</span><div><strong>Un QR para todas tus visitas</strong><p>Guardá este enlace para consultar los trabajos y próximos mantenimientos. Aunque venza un service, tu historial sigue disponible.</p></div></aside>`;
}
let busy = false,
  unavailable = false;
async function load() {
  if (busy || unavailable) return;
  busy = true;
  try {
    const response = await fetch(endpoint, {
      headers: { Accept: "application/json" },
    });
    const d = await response.json();
    if (!response.ok) {
      if ([404, 410].includes(response.status)) {
        unavailable = true;
        events.close();
        $("#public-content").innerHTML =
          `<div class="empty"><h1>Este enlace no está disponible</h1><p>Revisá que hayas copiado el enlace completo o consultá al taller. Las fichas no vencen por tiempo.</p></div>`;
        $("#connection").textContent = "Enlace no disponible";
        return;
      }
      throw Error();
    }
    $("#public-content").innerHTML = renderFicha(d);
    $("#last-update").textContent =
      "Última consulta: " + new Date().toLocaleTimeString("es-AR");
  } catch (e) {
    $("#connection").textContent = "Sin conexión · reintentando";
    $("#connection").classList.add("offline");
  } finally {
    busy = false;
  }
}
const events = new EventSource(endpoint + "/eventos");
events.onopen = () => {
  $("#connection").textContent = "En vivo";
  $("#connection").classList.remove("offline");
};
events.addEventListener("actualizar", load);
events.onerror = () => {
  if (!unavailable) {
    $("#connection").textContent = "Reconectando";
    $("#connection").classList.add("offline");
  }
};
setInterval(() => {
  if (!document.hidden) load();
}, 10000);
load();
