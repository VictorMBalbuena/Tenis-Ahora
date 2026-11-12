import { CuentaRegresiva } from "@/components/ui/cuenta-regresiva";
import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  CreditCard,
  ArrowLeft,
  Pencil,
  Printer,
  Trash2,
  AlertTriangle,
  Loader2,
  XCircle,
  Play,
  CheckCircle2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { obtenerReserva, confirmarCancelacion } from "../api/reservasApi";
import {
  ESTADOS_RESERVA,
  formatearMoneda,
  formatearFecha,
  formatearHorarioTurno,
} from "../model/reserva";
import { EliminarReservaDialog } from "../components/EliminarReservaDialog";
import { CancelarReservaDialog } from "../components/CancelarReservaDialog";
import { IniciarAlquilerDialog } from "../components/IniciarAlquilerDialog";
import { FinalizarAlquilerDialog } from "../components/FinalizarAlquilerDialog";

function formatearDni(dni) {
  if (!dni) return "";
  const str = String(dni);
  return str.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function obtenerIniciales(nombre, apellido) {
  const n = (nombre || "").trim().charAt(0);
  const a = (apellido || "").trim().charAt(0);
  return `${n}${a}`.toUpperCase() || "—";
}

export function ReservaDetallePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const esPersonal = user?.rol === "ADMINISTRADOR" || user?.rol === "EMPLEADO";

  const [reserva, setReserva] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [modalEliminarAbierto, setModalEliminarAbierto] = useState(false);
  const [modalCancelarAbierto, setModalCancelarAbierto] = useState(false);
  const [modalIniciarAbierto, setModalIniciarAbierto] = useState(false);
  const [modalFinalizarAbierto, setModalFinalizarAbierto] = useState(false);

  useEffect(() => {
    let cancelado = false;
    async function cargar() {
      try {
        setCargando(true);
        setError(null);
        const data = await obtenerReserva(id);
        if (!cancelado) {
          setReserva(data);
        }
      } catch (err) {
        if (!cancelado) {
          setError(
            err?.response?.data?.message ||
              err?.message ||
              "Error al cargar la reserva",
          );
        }
      } finally {
        if (!cancelado) {
          setCargando(false);
        }
      }
    }
    if (id) {
      cargar();
    }
    return () => {
      cancelado = true;
    };
  }, [id]);

  if (cargando) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !reserva) {
    return (
      <div className="w-full p-6 space-y-4">
        <div className="rounded-xl border border-danger-bg bg-danger-bg/20 p-4 text-danger-fg">
          <p className="font-semibold">
            {error || "No se encontró la reserva solicitada."}
          </p>
        </div>
        <Button variant="outline" onClick={() => navigate("/reservas")}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Volver al listado
        </Button>
      </div>
    );
  }

  const estadoConfig = ESTADOS_RESERVA[reserva.estado] || {
    label: reserva.estado || "—",
    clase: "bg-neutral-bg text-neutral-fg",
  };

  const codigo = reserva.numero || reserva.codigo || `RES-${reserva.id}`;
  const canchaNombre =
    reserva.cancha ||
    reserva.canchaNombre ||
    (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
  const precioTotal = reserva.montoTotal ?? reserva.precioTotal ?? 0;

  const jugadorTitular =
    reserva.jugadores?.find((j) => j.titular) || reserva.socioTitular || {};
  const nombreTitular =
    jugadorTitular.nombre && jugadorTitular.apellido
      ? `${jugadorTitular.nombre} ${jugadorTitular.apellido}`
      : jugadorTitular.nombreCompleto || jugadorTitular.nombre || "—";
  const dniTitular = jugadorTitular.documento || jugadorTitular.dni || "";

  const acompaniantes = reserva.jugadores
    ? reserva.jugadores.filter((j) => !j.titular)
    : Array.isArray(reserva.acompaniantes)
      ? reserva.acompaniantes
      : [];
  const insumos = Array.isArray(reserva.insumos) ? reserva.insumos : [];

  return (
    <div
      data-testid="reserva-detalle-page"
      className="w-full min-w-0 space-y-6 pb-10"
    >
      {/* Encabezado y Breadcrumbs */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl text-foreground">
            {codigo}
          </h1>
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${estadoConfig.clase}`}
          >
            {estadoConfig.label}
          </span>
          <Button
            type="button"
            variant="ghost"
            className="rounded-full"
            onClick={() => (window.history.length > 1 ? navigate(-1) : navigate("/reservas"))}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            <span>Volver</span>
          </Button>
        </div>
      </div>

      {/* Grid Principal */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Columna Izquierda (lg:col-span-2) */}
        <div className="space-y-6 lg:col-span-2">
          {/* Tarjeta Datos */}
          <div className="rounded-xl border border-border bg-card shadow-sm">
            <div className="px-8 pt-8">
              <h2 className="text-lg font-bold text-foreground">
                Datos
              </h2>
            </div>
            <div className="px-8 pb-8 pt-6">
              <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
                <div>
                  <dt className="text-xs text-muted-foreground">Código</dt>
                  <dd className="mt-1 text-sm font-medium text-foreground">
                    {codigo}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Estado</dt>
                  <dd className="mt-1">
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${estadoConfig.clase}`}
                    >
                      {estadoConfig.label}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Cancha</dt>
                  <dd className="mt-1 text-sm font-medium text-foreground">
                    {canchaNombre}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">
                    Fecha y horario
                  </dt>
                  <dd className="mt-1 text-sm font-medium text-foreground">
                    {reserva.fecha
                      ? `${formatearFecha(reserva.fecha)} - ${formatearHorarioTurno(reserva.horaInicio, reserva.horaFin)}`
                      : "—"}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Titular</dt>
                  <dd className="mt-1 text-sm font-medium text-foreground">
                    {nombreTitular}{" "}
                    {dniTitular ? `- DNI ${formatearDni(dniTitular)}` : ""}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">Jugadores</dt>
                  <dd className="mt-1 text-sm font-medium text-foreground">
                    {reserva.modalidad === "DOBLES"
                      ? "4 (dobles)"
                      : "2 (single)"}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">
                    Total del alquiler
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-foreground">
                    {formatearMoneda(precioTotal)}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">
                    {reserva.estado === "PENDIENTE_PAGO" ? "Seña a pagar" : "Pagado"}
                  </dt>
                  <dd className="mt-1 flex items-center gap-2 text-sm font-medium text-foreground">
                    <span>{formatearMoneda(reserva.estado === "PENDIENTE_PAGO" ? reserva.montoSenia : reserva.montoPagado)}</span>
                  </dd>
                </div>
                <div>
                  <dt className="text-xs text-muted-foreground">
                    Saldo pendiente
                  </dt>
                  <dd className="mt-1 flex items-center gap-2 text-sm font-medium text-foreground">
                    <span>
                      {formatearMoneda(
                        reserva.saldoPendiente ??
                          precioTotal - (reserva.montoSenia || 0),
                      )}
                    </span>
                    <span className="inline-flex items-center rounded-full bg-warning-bg px-2 py-0.5 text-xs font-semibold text-warning-fg">
                      Al finalizar
                    </span>
                  </dd>
                </div>
              </dl>
            </div>
          </div>

          {/* Tarjeta Jugadores registrados */}
          <div className="rounded-xl border border-border bg-card shadow-sm">
            <div className="px-8 pt-8">
              <h2 className="text-lg font-bold text-foreground">
                Jugadores registrados
              </h2>
            </div>
            <div className="px-8 pb-8 pt-6">
              <div className="space-y-3">
                {/* Titular */}
                <div className="flex items-center justify-between gap-3 rounded-lg border border-border p-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-bold text-foreground">
                      {obtenerIniciales(
                        jugadorTitular.nombre,
                        jugadorTitular.apellido,
                      )}
                    </span>
                    <div className="min-w-0">
                      <p className="truncate font-semibold text-sm text-foreground">
                        {nombreTitular}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        {dniTitular ? `DNI ${formatearDni(dniTitular)}` : "—"}
                      </p>
                    </div>
                  </div>
                  <span className="inline-flex items-center rounded-full bg-brand-bg px-2.5 py-1 text-xs font-semibold text-brand-fg">
                    Titular
                  </span>
                </div>

                {/* Acompañantes */}
                {acompaniantes.map((jugador, idx) => {
                  const nombreJugador =
                    jugador.nombre && jugador.apellido
                      ? `${jugador.nombre} ${jugador.apellido}`
                      : jugador.nombreCompleto ||
                        jugador.nombre ||
                        `Acompañante ${idx + 1}`;
                  const dniJugador = jugador.documento || jugador.dni || "";
                  return (
                    <div
                      key={jugador.id || idx}
                      className="flex items-center justify-between gap-3 rounded-lg border border-border p-3"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-bold text-foreground">
                          {obtenerIniciales(jugador.nombre, jugador.apellido)}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-sm text-foreground">
                            {nombreJugador}
                          </p>
                          <p className="truncate text-xs text-muted-foreground">
                            {dniJugador
                              ? `DNI ${formatearDni(dniJugador)}`
                              : "—"}
                          </p>
                        </div>
                      </div>
                      <span className="inline-flex items-center rounded-full bg-neutral-bg px-2.5 py-1 text-xs font-semibold text-neutral-fg">
                        Acompañante
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Tarjeta Insumos asignados */}
          <div className="rounded-xl border border-border bg-card shadow-sm">
            <div className="px-8 pt-8">
              <h2 className="text-lg font-bold text-foreground">
                Insumos asignados
              </h2>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {reserva.estado === "EN_CURSO"
                  ? "En uso en cancha — descontados del depósito"
                  : reserva.estado === "FINALIZADA"
                    ? "Reintegrados al depósito del club"
                    : "Se descuentan del depósito al iniciar el alquiler"}
              </p>
            </div>
            <div className="px-8 pb-8 pt-6">
              {insumos.length > 0 ? (
                <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
                  {insumos.map((insumo, idx) => (
                    <div key={insumo.id || idx}>
                      <dt className="text-xs text-muted-foreground">
                        {insumo.nombre}
                      </dt>
                      <dd className="mt-1 text-sm font-medium text-foreground">
                        {insumo.cantidad}{" "}
                        {insumo.cantidad === 1 ? "unidad" : "unidades"}
                      </dd>
                    </div>
                  ))}
                  <div>
                    <dt className="text-xs text-muted-foreground">
                      Estado de insumos
                    </dt>
                    <dd className="mt-1 text-sm font-medium text-foreground">
                      {reserva.estado === "EN_CURSO"
                        ? "En uso en cancha"
                        : reserva.estado === "FINALIZADA"
                          ? "Devueltos al depósito"
                          : "Asignados (pendiente de entrega)"}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">
                      Reposición
                    </dt>
                    <dd className="mt-1 text-sm font-medium text-foreground">
                      Automática al finalizar turno
                    </dd>
                  </div>
                </dl>
              ) : (
                <p className="text-xs text-muted-foreground">
                  No se asignaron insumos adicionales a esta reserva.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Columna Derecha (Acciones y Cancelación) */}
        <div className="space-y-6">
          {/* Tarjeta Acciones */}
          <div className="rounded-xl border border-border bg-card shadow-sm">
            <div className="px-8 pt-8">
              <h2 className="text-lg font-bold text-foreground">
                Acciones
              </h2>
            </div>
            <div className="px-8 pb-8 pt-6 space-y-2">
              {esPersonal && reserva.estado === "CONFIRMADA" && (
                <Button
                  type="button"
                  className="w-full justify-center rounded-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
                  onClick={() => setModalIniciarAbierto(true)}
                >
                  <Play className="mr-2 h-4 w-4 fill-current" />
                  <span>Iniciar alquiler (ingreso)</span>
                </Button>
              )}
              {esPersonal && reserva.estado === "EN_CURSO" && (
                <Button
                  type="button"
                  className="w-full justify-center rounded-full bg-blue-600 hover:bg-blue-700 text-white font-semibold"
                  onClick={() => setModalFinalizarAbierto(true)}
                >
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                  <span>Finalizar alquiler (salida)</span>
                </Button>
              )}
              {reserva.estado === "PENDIENTE_PAGO" && reserva.venceEn && (
                <div className="space-y-1 rounded-xl bg-[var(--estado-aviso-bg)] px-4 py-3 text-xs font-semibold text-[var(--estado-aviso-fg)]">
                  <CuentaRegresiva venceEn={reserva.venceEn} prefijo="Turno retenido" className="text-sm" />
                  <p>Si no se paga la seña a tiempo, se libera.</p>
                </div>
              )}
              {(reserva.estado === "PENDIENTE_PAGO" || Number(reserva.saldoPendiente) > 0) && reserva.estado !== "CANCELADA" && (
                <Button
                  type="button"
                  className="w-full justify-center rounded-full"
                  onClick={() => navigate(`/reservas/${reserva.id}/cobro`)}
                >
                  <CreditCard className="mr-2 h-4 w-4" />
                  <span>
                    {esPersonal ? "Cobrar" : "Pagar"} {reserva.estado === "PENDIENTE_PAGO" ? "seña" : "saldo"}
                  </span>
                </Button>
              )}
              {esPersonal && (
                <Button
                  type="button"
                  variant="outline"
                  className="w-full justify-center rounded-full"
                  onClick={() => navigate(`/reservas/editar/${reserva.id}`)}
                >
                  <Pencil className="mr-2 h-4 w-4" />
                  <span>Editar</span>
                </Button>
              )}
              <Button
                type="button"
                variant="outline"
                className="w-full justify-center rounded-full"
                onClick={() => window.print()}
              >
                <Printer className="mr-2 h-4 w-4" />
                <span>Imprimir</span>
              </Button>
              {esPersonal && (
                <Button
                  type="button"
                  variant="ghost"
                  className="w-full justify-center rounded-full text-danger-fg hover:bg-danger-bg"
                  onClick={() => setModalEliminarAbierto(true)}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  <span>Eliminar</span>
                </Button>
              )}
            </div>
          </div>

          {/* Tarjeta Cancelación */}
          <div className="rounded-xl border border-border bg-card shadow-sm">
            <div className="px-8 pt-8">
              <h2 className="text-lg font-bold text-foreground">
                Cancelación
              </h2>
            </div>
            <div className="px-8 pb-8 pt-6 space-y-3">
              <div className="flex items-center gap-3 rounded-xl bg-danger-bg/20 px-4 py-3 text-danger-fg border border-danger-bg/30">
                <AlertTriangle className="h-5 w-5 shrink-0" />
                <p className="text-xs font-semibold">
                  Política de cancelación sujeta a antelación
                </p>
              </div>
              <Button
                type="button"
                variant="destructive"
                className="w-full justify-center rounded-full"
                onClick={() => setModalCancelarAbierto(true)}
              >
                <XCircle className="mr-2 h-4 w-4" />
                <span>Cancelar reserva</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Diálogos modales de eliminar y cancelar */}
      <EliminarReservaDialog
        open={modalEliminarAbierto}
        reserva={reserva}
        onOpenChange={setModalEliminarAbierto}
        onConfirm={async () => {
          await confirmarCancelacion(reserva.id);
          navigate("/reservas");
        }}
      />

      <CancelarReservaDialog
        open={modalCancelarAbierto}
        reserva={reserva}
        onCerrar={() => setModalCancelarAbierto(false)}
        onCanceladaExitosamente={() => {
          setModalCancelarAbierto(false);
          navigate("/reservas", { state: { aviso: "Reserva cancelada." } });
        }}
      />

      <IniciarAlquilerDialog
        open={modalIniciarAbierto}
        reserva={reserva}
        onOpenChange={setModalIniciarAbierto}
        onIniciado={(actualizada) => {
          setReserva(actualizada);
        }}
      />

      <FinalizarAlquilerDialog
        open={modalFinalizarAbierto}
        reserva={reserva}
        onOpenChange={setModalFinalizarAbierto}
        onFinalizado={(actualizada) => {
          setReserva(actualizada);
        }}
      />
    </div>
  );
}

export default ReservaDetallePage;
