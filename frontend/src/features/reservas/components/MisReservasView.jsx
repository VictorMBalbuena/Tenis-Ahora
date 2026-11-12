import { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Calendar, Eye, Loader2, Plus, Ban, CreditCard } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { obtenerMisReservas } from "../api/reservasApi";
import {
  ESTADOS_RESERVA,
  formatearMoneda,
  formatearHorarioTurno,
  extraerMensajeError, formatearFecha } from "../model/reserva";
import { CancelarReservaDialog } from "./CancelarReservaDialog";

export function MisReservasView() {
  const navigate = useNavigate();
  const location = useLocation();
  const [aviso, setAviso] = useState(location.state?.aviso ?? null);
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [filtroProximas, setFiltroProximas] = useState("proximas");
  const [filtroEstado, setFiltroEstado] = useState("TODOS");

  const [reservaACancelar, setReservaACancelar] = useState(null);
  const [modalCancelarAbierto, setModalCancelarAbierto] = useState(false);

  const cargarReservas = useCallback(() => {
    let montado = true;
    const params = {};
    if (filtroProximas === "proximas") params.proximas = true;
    if (filtroProximas === "pasadas") params.proximas = false;
    if (filtroEstado && filtroEstado !== "TODOS") params.estado = filtroEstado;

    obtenerMisReservas(params)
      .then((data) => {
        if (!montado) return;
        const lista = Array.isArray(data?.contenido)
          ? data.contenido
          : Array.isArray(data)
          ? data
          : [];
        setReservas(
          filtroProximas === "proximas" && filtroEstado === "TODOS"
            ? lista.filter((r) => r.estado !== "CANCELADA" && r.estado !== "FINALIZADA")
            : lista,
        );
        setError(null);
        setCargando(false);
      })
      .catch((err) => {
        if (!montado) return;
        setError(extraerMensajeError(err, "No se pudieron cargar tus reservas."));
        setCargando(false);
      });

    return () => {
      montado = false;
    };
  }, [filtroProximas, filtroEstado]);

  useEffect(() => {
    cargarReservas();
  }, [cargarReservas]);

  const abrirCancelar = (reserva) => {
    setReservaACancelar(reserva);
    setModalCancelarAbierto(true);
  };

  return (
    <div className="w-full min-w-0 space-y-6">
      {/* Encabezado conforme a mockup portal-reservas.html */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
            Mis reservas
          </h1>
          <p className="mt-1 text-sm text-muted-foreground hidden sm:block">
            Consultá el historial y estado de tus turnos de tenis.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <Button
            onClick={() => navigate("/reservas/nuevo")}
            className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-40 bg-primary text-primary-foreground hover:brightness-95 h-10 px-5"
          >
            <Plus className="h-[18px] w-[18px] shrink-0" />
            <span>Reservar cancha</span>
          </Button>
        </div>
      </div>

      {aviso && <SuccessBanner>{aviso}</SuccessBanner>}

      {/* Barra de Filtros (Pills + selector de estado) */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-1.5 overflow-x-auto scroll-thin">
          {[
            { id: "proximas", label: "Próximas" },
            { id: "pasadas", label: "Pasadas" },
            { id: "todas", label: "Todas" },
          ].map((f) => {
            const activo = filtroProximas === f.id;
            return (
              <button
                key={f.id}
                type="button"
                onClick={() => setFiltroProximas(f.id)}
                className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
                  activo
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted"
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="filtro-estado-mis-reservas" className="sr-only">
            Filtrar por estado
          </label>
          <FiltroPill
            valor={filtroEstado}
            onChange={(valor) => { setFiltroEstado(valor); }}
            opciones={[{ value: "TODOS", label: "Todos los estados" }, { value: "CONFIRMADA", label: "Confirmadas" }, { value: "PENDIENTE_PAGO", label: "Turnos retenidos" }, { value: "EN_CURSO", label: "En curso" }, { value: "CANCELADA", label: "Canceladas" }, { value: "FINALIZADA", label: "Finalizadas" }]}
          />
        </div>
      </div>

      {/* Feedback de error */}
      {error && (
        <div className="p-4 rounded-xl border border-destructive/20 bg-destructive/10 text-destructive text-sm flex items-center justify-between">
          <span>{error}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setCargando(true);
              cargarReservas();
            }}
          >
            Reintentar
          </Button>
        </div>
      )}

      {/* Contenido principal */}
      {cargando ? (
        <div className="py-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <p className="text-sm">Cargando reservas...</p>
        </div>
      ) : reservas.length === 0 ? (
        <div className="py-16 text-center rounded-2xl border border-dashed border-border bg-card/50 p-8 space-y-4">
          <Calendar className="w-12 h-12 text-muted-foreground mx-auto" />
          <div className="space-y-1">
            <h3 className="text-lg font-semibold text-foreground">
              No tenés reservas registradas
            </h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              Explorá los turnos disponibles y asegurá tu cancha en simples pasos.
            </p>
          </div>
        </div>
      ) : (
        <>
          {/* Tarjetas Mobile conforme a mockup portal-reservas.html (gobernado por CSS puro) */}
          <div className="md:hidden space-y-3">
            {reservas.map((reserva) => {
              const numero = reserva.numero || reserva.codigo || `RES-${String(reserva.id).padStart(4, "0")}`;
              const nombreCancha =
                reserva.cancha ||
                reserva.canchaNombre ||
                (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
              const horario = formatearHorarioTurno(
                reserva.horaInicio,
                reserva.horaFin
              );
              const estadoInfo =
                ESTADOS_RESERVA[reserva.estado] || {
                  label: reserva.estado || "Desconocido",
                  clase: "bg-muted text-muted-foreground",
                };
              const esPendiente = reserva.estado === "PENDIENTE_PAGO";
              const puedeCancelar =
                reserva.estado !== "CANCELADA" && reserva.estado !== "FINALIZADA";

              return (
                <div key={reserva.id} className="rounded-xl border border-border bg-card p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-foreground">
                        {formatearFecha(reserva.fecha)} {horario}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-muted-foreground">
                        {nombreCancha} · <span className="font-mono">{numero}</span>
                      </p>
                    </div>
                    <div className="shrink-0">
                      <span
                        className={`inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs ${estadoInfo.clase}`}
                      >
                        {estadoInfo.label}
                      </span>
                    </div>
                  </div>

                  <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                    <div>
                      <dt className="text-[11px] text-muted-foreground">Total</dt>
                      <dd className="text-sm font-medium font-mono tabular-nums text-foreground">
                        {formatearMoneda(reserva.montoTotal)}
                      </dd>
                    </div>
                    {reserva.montoSenia != null && (
                      <div>
                        <dt className="text-[11px] text-muted-foreground">Seña</dt>
                        <dd className="text-sm font-medium font-mono tabular-nums text-foreground">
                          {formatearMoneda(reserva.montoSenia)}
                        </dd>
                      </div>
                    )}
                  </dl>

                  <div className="mt-4 flex gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/reservas/${reserva.id}`)}
                      className="flex-1 rounded-full text-xs font-semibold h-8"
                    >
                      <Eye className="h-3.5 w-3.5 mr-1 text-muted-foreground" />
                      <span>Ver detalle</span>
                    </Button>
                    {esPendiente && (
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => navigate(`/reservas/${reserva.id}/cobro`)}
                        className="flex-1 rounded-full text-xs font-semibold h-8 bg-primary text-primary-foreground hover:brightness-95"
                      >
                        <CreditCard className="h-3.5 w-3.5 mr-1" />
                        <span>Pagar seña</span>
                      </Button>
                    )}
                    {puedeCancelar && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => abrirCancelar(reserva)}
                        className="rounded-full text-xs font-semibold h-8 px-3 text-destructive border-destructive/30 hover:bg-destructive/10"
                      >
                        <Ban className="h-3.5 w-3.5 mr-1" />
                        <span>Cancelar</span>
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Tabla Desktop conforme a mockup portal-reservas.html (gobernado por CSS puro) */}
          <div className="hidden md:block rounded-xl border border-border bg-card p-2 overflow-x-auto overflow-y-hidden scroll-thin" tabIndex={0}>
            <table className="w-full text-sm">
              <caption className="sr-only">Mis reservas</caption>
              <thead>
                <tr>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Código
                  </th>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Fecha y horario
                  </th>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Cancha
                  </th>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground text-right">
                    Total
                  </th>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Estado
                  </th>
                  <th scope="col" className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground text-right">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {reservas.map((reserva) => {
                  const numero = reserva.numero || reserva.codigo || `RES-${String(reserva.id).padStart(4, "0")}`;
                  const nombreCancha =
                    reserva.cancha ||
                    reserva.canchaNombre ||
                    (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
                  const horario = formatearHorarioTurno(
                    reserva.horaInicio,
                    reserva.horaFin
                  );
                  const estadoInfo =
                    ESTADOS_RESERVA[reserva.estado] || {
                      label: reserva.estado || "Desconocido",
                      clase: "bg-muted text-muted-foreground",
                    };
                  const esPendiente = reserva.estado === "PENDIENTE_PAGO";
                  const puedeCancelar =
                    reserva.estado !== "CANCELADA" && reserva.estado !== "FINALIZADA";

                  return (
                    <tr key={reserva.id} className="border-t transition-colors hover:bg-muted/50">
                      <td className="px-4 py-3.5 align-middle text-sm">
                        <span className="font-mono text-xs text-muted-foreground tabular-nums">
                          {numero}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm font-medium text-foreground">
                        <span className="whitespace-nowrap">
                          {formatearFecha(reserva.fecha)} {horario}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-foreground">
                        {nombreCancha}
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-right text-foreground">
                        <span className="font-mono tabular-nums font-medium">
                          {formatearMoneda(reserva.montoTotal)}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm">
                        <span
                          className={`inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs ${estadoInfo.clase}`}
                        >
                          {estadoInfo.label}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-right">
                        <div className="flex items-center justify-end gap-1.5">

                          {esPendiente && (
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => navigate(`/reservas/${reserva.id}/cobro`)}
                              className="rounded-full text-xs font-semibold h-8 px-3.5 bg-primary text-primary-foreground hover:brightness-95"
                            >
                              <CreditCard className="h-3.5 w-3.5 mr-1" />
                              <span>Pagar seña</span>
                            </Button>
                          )}

                          {puedeCancelar && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => abrirCancelar(reserva)}
                              className="rounded-full text-xs font-semibold h-8 px-3 text-destructive hover:bg-destructive/10"
                            >
                              <Ban className="h-3.5 w-3.5 mr-1" />
                              <span>Cancelar</span>
                            </Button>
                          )}

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`/reservas/${reserva.id}`)}
                            className="rounded-full text-xs font-semibold h-8 px-3.5"
                          >
                            <Eye className="h-3.5 w-3.5 mr-1 text-muted-foreground" />
                            <span>Ver detalle</span>
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Modal de cancelación */}
      <CancelarReservaDialog
        abierta={modalCancelarAbierto}
        reserva={reservaACancelar}
        onCerrar={() => {
          setModalCancelarAbierto(false);
          setReservaACancelar(null);
        }}
        onCanceladaExitosamente={() => {
          setAviso("Reserva cancelada.");
          setModalCancelarAbierto(false);
          setReservaACancelar(null);
          setCargando(true);
          cargarReservas();
        }}
      />
    </div>
  );
}

export default MisReservasView;
