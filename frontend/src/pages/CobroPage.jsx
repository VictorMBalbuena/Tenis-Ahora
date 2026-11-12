import { useState, useEffect } from "react";
import { ArrowLeft, Banknote, CreditCard, QrCode, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { ErrorBanner } from "@/components/ui/error-banner";
import { VolverButton } from "@/components/ui/volver-button";
import { formatearFecha, formatearMoneda } from "@/features/reservas/model/reserva";
import CobroQR from "./CobroQR.jsx";
import ReservaConfirmada from "../components/ReservaConfirmada.jsx";

const MEDIOS_DEL_PERSONAL = [
  { id: "EFECTIVO", titulo: "Efectivo", detalle: "En la caja del club", Icono: Banknote },
  { id: "DEBITO", titulo: "Tarjeta de Debito", detalle: "Con la terminal del club", Icono: CreditCard },
  { id: "CREDITO", titulo: "Tarjeta de Credito", detalle: "Con la terminal del club", Icono: CreditCard },
  { id: "MERCADO_PAGO", titulo: "Mercado Pago", detalle: "Genera QR", Icono: QrCode },
];

const CLASE_CAMPO = "flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] text-foreground placeholder:text-muted-foreground/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:max-w-xs";

function duracionLegible(inicio, fin) {
  if (!inicio || !fin) return "—";
  const minutos = (h) => {
    const [hh, mm] = String(h).split(":").map(Number);
    return hh * 60 + mm;
  };
  const horas = (minutos(fin) - minutos(inicio)) / 60;
  return horas > 0 ? `${horas} ${horas === 1 ? "hora" : "horas"}` : "—";
}

function aVistaDeReserva(reserva) {
  const hhmm = (h) => (h ? String(h).slice(0, 5) : "—");
  const titular = (reserva.jugadores ?? []).find((j) => j.esTitular);
  const esSenia = reserva.estado === "PENDIENTE_PAGO";
  return {
    ...reserva,
    cancha: reserva.cancha ?? "—",
    fecha: formatearFecha(reserva.fecha),
    horario: `${hhmm(reserva.horaInicio)} a ${hhmm(reserva.horaFin)}`,
    duracion: duracionLegible(reserva.horaInicio, reserva.horaFin),
    jugadores: reserva.jugadores ?? [],
    total: Number(reserva.montoTotal ?? 0),
    pagado: Number(reserva.montoPagado ?? 0),
    saldo: Number(reserva.saldoPendiente ?? 0),
    importe: Number(esSenia ? reserva.montoSenia ?? 0 : reserva.saldoPendiente ?? 0),
    esSenia,
    codigo: reserva.numero ?? `#${reserva.id ?? ""}`,
    titular: titular ? `${titular.nombre} ${titular.apellido}` : "—",
    subtitulo: `Reserva ${reserva.numero ?? `#${reserva.id ?? ""}`}`,
    concepto: esSenia ? "seña" : "saldo",
    volver: "/reservas",
    filas: [
      ["Cancha", reserva.cancha ?? "—"],
      ["Fecha", formatearFecha(reserva.fecha)],
      ["Horario", `${hhmm(reserva.horaInicio)} a ${hhmm(reserva.horaFin)}`],
      ["Duración", duracionLegible(reserva.horaInicio, reserva.horaFin)],
    ],
  };
}

function aVistaDeActividad(inscripcion) {
  const hhmm = (h) => (h ? String(h).slice(0, 5) : "—");
  const esSenia = inscripcion.estado === "PENDIENTE_PAGO";
  return {
    total: Number(inscripcion.montoTotal ?? 0),
    pagado: Number(inscripcion.montoPagado ?? 0),
    saldo: Number(inscripcion.saldoPendiente ?? 0),
    importe: Number(esSenia ? inscripcion.montoSenia ?? 0 : inscripcion.saldoPendiente ?? 0),
    esSenia,
    codigo: inscripcion.actividad,
    subtitulo: inscripcion.actividad,
    concepto: esSenia ? "seña" : "saldo",
    volver: inscripcion.tipo === "CLASE" ? "/clases" : "/entrenamientos",
    filas: [
      [inscripcion.tipo === "CLASE" ? "Clase" : "Entrenamiento", inscripcion.actividad],
      ["Horario", `${hhmm(inscripcion.horaInicio)} a ${hhmm(inscripcion.horaFin)}`],
      ["Vigencia", `${formatearFecha(inscripcion.fechaInicio)} a ${formatearFecha(inscripcion.fechaFin)}`],
    ],
  };
}

function aVistaDeTorneo(inscripcion) {
  return {
    total: Number(inscripcion.montoTotal ?? 0),
    pagado: 0,
    saldo: Number(inscripcion.montoTotal ?? 0),
    importe: Number(inscripcion.montoTotal ?? 0),
    esSenia: false,
    codigo: inscripcion.torneo,
    subtitulo: inscripcion.torneo,
    concepto: "arancel",
    volver: `/torneos/${inscripcion.torneoId}`,
    filas: [["Torneo", inscripcion.torneo]],
  };
}

const ORIGENES = {
  reserva: { recurso: "/reservas", aVista: aVistaDeReserva, etiqueta: "la reserva", volver: "/reservas" },
  actividad: { recurso: "/actividades/inscripciones", aVista: aVistaDeActividad, etiqueta: "la inscripción", volver: "/dashboard" },
  torneo: { recurso: "/torneos/inscripciones", aVista: aVistaDeTorneo, etiqueta: "la inscripción", volver: "/torneos" },
};

function FilaResumen({ etiqueta, valor }) {
  return (
    <div className="flex justify-between gap-3 text-sm">
      <span className="text-muted-foreground">{etiqueta}</span>
      <span className="min-w-0 text-right font-medium text-foreground">{valor}</span>
    </div>
  );
}

export default function CobroPage({ origen = "reserva" }) {
  const navigate = useNavigate();
  const { idReserva, idInscripcion } = useParams();
  const id = idReserva ?? idInscripcion;
  const { recurso, aVista, etiqueta, volver } = ORIGENES[origen];
  const rol = useAuthStore((estado) => estado.user?.rol);
  const esPersonal = rol === "EMPLEADO" || rol === "ADMINISTRADOR";

  const [vista, setVista] = useState("SELECCION");
  const [medioSeleccionado, setMedioSeleccionado] = useState("MERCADO_PAGO");
  const [nroComprobante, setNroComprobante] = useState("");
  const [cuotas, setCuotas] = useState("");
  const [aprobado, setAprobado] = useState(true);

  const [reservaInfo, setReservaInfo] = useState(null);
  const [datosPago, setDatosPago] = useState(null);
  const [cobroId, setCobroId] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [errorCobro, setErrorCobro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    const fetchReserva = async () => {
      try {
        const { data } = await axiosClient.get(`${recurso}/${id}`);
        setReservaInfo(aVista(data));
      } catch (e) {
        setError(mensajeDeError(e, `No se pudo cargar ${etiqueta}.`));
      } finally {
        setCargando(false);
      }
    };

    if (id) fetchReserva();
  }, [id, recurso, aVista, etiqueta]);

  const registrarCobro = async (datos) => {
    const { data } = await axiosClient.post(`${recurso}/${id}/cobros`, {
      numeroOperacion: null,
      cuotas: null,
      aprobado: true,
      ...datos,
    });
    return data;
  };

  const handlePagoExitoso = async (datosRecibo) => {
    if (origen !== "reserva") {
      navigate(reservaInfo.volver, { state: { aviso: `Pago registrado: ${reservaInfo.subtitulo} quedó confirmada.` } });
      return;
    }
    try {
      const { data } = await axiosClient.get(`/reservas/${id}`);
      if (data.estado === "CANCELADA") {
        setError(`El pago se acreditó, pero la reserva ya había vencido. El club tiene que devolver ${formatearMoneda(data.montoReembolso ?? 0)}.`);
        return;
      }
      setReservaInfo(aVistaDeReserva(data));
    } catch {
      console.log("No se pudo releer la reserva después del pago.");
    }
    setDatosPago(datosRecibo);
    setVista("EXITO");
  };

  const handleGenerarCobro = async () => {
    setErrorCobro(null);
    const usaTerminal = medioSeleccionado === "DEBITO" || medioSeleccionado === "CREDITO";
    if (usaTerminal && nroComprobante.trim() === "") {
      setErrorCobro("Ingresá el número de comprobante de la terminal.");
      return;
    }
    const usaCuotas = medioSeleccionado === "CREDITO" && cuotas !== "";
    if (usaCuotas && (!Number.isInteger(Number(cuotas)) || Number(cuotas) < 1 || Number(cuotas) > 24)) {
      setErrorCobro("Las cuotas tienen que estar entre 1 y 24.");
      return;
    }
    setEnviando(true);
    try {
      if (medioSeleccionado === "MERCADO_PAGO") {
        const cobro = await registrarCobro({ medioPago: "QR" });
        setCobroId(cobro.id);
        setVista("QR");
      } else {
        const cobro = await registrarCobro({
          medioPago: medioSeleccionado,
          numeroOperacion: usaTerminal ? nroComprobante.trim() : null,
          cuotas: usaCuotas ? Number(cuotas) : null,
          aprobado: usaTerminal ? aprobado : true,
        });
        setCobroId(cobro.id);
        if (cobro.estado === "RECHAZADO") {
          setNroComprobante("");
          setAprobado(true);
          setErrorCobro("La tarjeta fue rechazada. Elegí otro medio de pago.");
          return;
        }
        await handlePagoExitoso({ medio: cobro.medioPago, operacion: cobro.numeroOperacion, reciboId: cobro.id });
      }
    } catch (err) {
      setErrorCobro(mensajeDeError(err, "No se pudo registrar el cobro."));
    } finally {
      setEnviando(false);
    }
  };

  if (cargando) {
    return (
      <div className="flex w-full flex-col items-center justify-center gap-4 py-20">
        <Loader2 className="animate-spin text-primary" size={36} />
        <p className="text-sm font-medium text-muted-foreground">Cargando datos del pago...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex w-full flex-col items-center justify-center gap-4 py-20">
        <ErrorBanner>{error}</ErrorBanner>
        <button onClick={() => navigate(volver)} className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-foreground transition-colors hover:bg-muted">
          <ArrowLeft size={16} /> Volver
        </button>
      </div>
    );
  }

  if (vista === "QR") {
    return (
      <CobroQR
        reservaInfo={reservaInfo}
        cobroId={cobroId}
        onCancelar={() => setVista("SELECCION")}
        onCambiarMedio={esPersonal ? (nuevoMedio) => {
          setMedioSeleccionado(nuevoMedio);
          setVista("SELECCION");
        } : undefined}
        onPagoExitoso={handlePagoExitoso}
      />
    );
  }

  if (vista === "EXITO") {
    return <ReservaConfirmada reservaInfo={reservaInfo} datosPago={datosPago} />;
  }

  const { concepto } = reservaInfo;
  const usaTerminal = medioSeleccionado === "DEBITO" || medioSeleccionado === "CREDITO";
  const sinSaldo = !reservaInfo.esSenia && reservaInfo.importe <= 0;

  return (
    <div className="w-full min-w-0 space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
            {esPersonal ? "Cobrar" : "Pagar"} {concepto}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">{reservaInfo.subtitulo}</p>
        </div>
        <VolverButton to={reservaInfo.volver} />
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <div className="min-w-0 rounded-xl border border-border bg-card p-8 shadow-sm lg:col-span-2">
          <h2 className="text-lg font-bold text-foreground">Medio de pago</h2>

          {esPersonal ? (
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {MEDIOS_DEL_PERSONAL.map(({ id, titulo, detalle, Icono }) => (
                <button
                  key={id}
                  type="button"
                  aria-pressed={medioSeleccionado === id}
                  onClick={() => setMedioSeleccionado(id)}
                  className={`flex flex-col items-center justify-center gap-1.5 rounded-xl border-2 p-4 text-center transition-colors ${
                    medioSeleccionado === id ? "border-primary bg-[var(--estado-brand-bg)]" : "border-input hover:bg-muted"
                  }`}
                >
                  <Icono size={22} className={medioSeleccionado === id ? "text-primary" : "text-muted-foreground"} />
                  <span className="text-sm font-semibold text-foreground">{titulo}</span>
                  <span className="text-xs text-muted-foreground">{detalle}</span>
                </button>
              ))}
            </div>
          ) : (
            <div className="mt-4 flex items-center gap-3 rounded-xl border border-input p-4">
              <QrCode size={22} className="shrink-0 text-primary" />
              <div className="min-w-0">
                <span className="block text-sm font-semibold text-foreground">Mercado Pago</span>
                <p className="text-xs text-muted-foreground">Pagás escaneando un código QR o con el link de pago.</p>
              </div>
            </div>
          )}

          {usaTerminal && (
            <div className="mt-5 space-y-1.5">
              <label htmlFor="comprobante" className="text-sm font-semibold text-foreground">Número de comprobante</label>
              <input
                id="comprobante"
                type="text"
                inputMode="numeric"
                placeholder="Ej: 000142"
                value={nroComprobante}
                onChange={(e) => setNroComprobante(e.target.value.replace(/\D/g, "").slice(0, 20))}
                className={CLASE_CAMPO}
              />
              <p className="text-xs text-muted-foreground">El que muestra la terminal.</p>
            </div>
          )}

          {medioSeleccionado === "CREDITO" && (
            <div className="mt-5 space-y-1.5">
              <label htmlFor="cuotas" className="text-sm font-semibold text-foreground">Cuotas</label>
              <input
                id="cuotas"
                type="number"
                min="1"
                max="24"
                step="1"
                placeholder="1"
                value={cuotas}
                onChange={(e) => setCuotas(e.target.value)}
                className={CLASE_CAMPO}
              />
            </div>
          )}

          {usaTerminal && (
            <div className="mt-5">
              <CustomCheckbox
                label="La terminal aprobó la operación"
                name="aprobado"
                checked={aprobado}
                onChange={(e) => setAprobado(e.target.checked)}
              />
            </div>
          )}
        </div>

        <aside className="min-w-0 space-y-4 rounded-xl border border-border bg-card p-8 shadow-sm">
          <h2 className="text-lg font-bold text-foreground">Resumen</h2>
          <div className="space-y-3">
            {reservaInfo.filas.map(([filaEtiqueta, valor]) => (
              <FilaResumen key={filaEtiqueta} etiqueta={filaEtiqueta} valor={valor} />
            ))}
          </div>
          <div className="space-y-3 border-t border-border pt-3">
            <FilaResumen etiqueta="Total" valor={formatearMoneda(reservaInfo.total)} />
            {reservaInfo.pagado > 0 && <FilaResumen etiqueta="Ya pagado" valor={formatearMoneda(reservaInfo.pagado)} />}
          </div>
          <div className="flex items-baseline justify-between gap-3 rounded-lg bg-[var(--estado-brand-bg)] p-3 text-[var(--estado-brand-fg)]">
            <span className="text-sm font-medium">{`${concepto.charAt(0).toUpperCase()}${concepto.slice(1)} a abonar`}</span>
            <span className="font-mono text-lg font-semibold tabular-nums">{formatearMoneda(reservaInfo.importe)}</span>
          </div>

          {sinSaldo && <p className="text-sm text-muted-foreground">No hay saldo pendiente.</p>}
          {errorCobro && (
            <div role="alert"><ErrorBanner>{errorCobro}</ErrorBanner></div>
          )}

          <button
            onClick={handleGenerarCobro}
            disabled={enviando || sinSaldo}
            className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-8 py-3 text-[15px] font-bold text-primary-foreground transition-colors hover:brightness-95 disabled:opacity-40"
          >
            {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
            {medioSeleccionado === "MERCADO_PAGO" ? "Generar QR y cobrar" : "Confirmar cobro"}
          </button>
        </aside>
      </div>
    </div>
  );
}
