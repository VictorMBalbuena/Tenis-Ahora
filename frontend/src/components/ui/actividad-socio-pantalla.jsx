import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { DiasDeLaSemana, EstadoDeInscripcion, conValor, moneda } from "@/components/ui/actividad-socio-card";
import { Button } from "@/components/ui/button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { VolverButton } from "@/components/ui/volver-button";
import { listarMisInscripciones, previsualizarBaja } from "@/features/inscripciones/api/inscripcionesApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";

// Detalle de una clase o un entrenamiento para el socio, con la misma estructura que el resto de los detalles: datos y acciones.
function Dato({ etiqueta, valor }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="mt-1 text-sm font-medium text-foreground">{valor}</dd>
    </div>
  );
}

const BOTON_PRINCIPAL = "inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-8 py-3 text-[15px] font-bold text-primary-foreground transition-colors hover:brightness-95 disabled:opacity-50";

export function ActividadSocioPantalla({ tipoEtiqueta, volver, obtener, describir, inscribirme, darmeDeBaja }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const socioId = useAuthStore((estado) => estado.user?.id);
  const [actividad, setActividad] = useState(null);
  const [inscripcion, setInscripcion] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [baja, setBaja] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let cancelado = false;
    // Por separado: son de módulos distintos y el administrador puede haber dejado permiso sobre uno y no
    // sobre el otro. Si la actividad se pudo traer, se muestra aunque no se sepan las inscripciones propias.
    Promise.allSettled([obtener(id), listarMisInscripciones()])
      .then(([datos, propias]) => {
        if (cancelado) return;

        if (datos.status === "fulfilled") {
          setActividad(datos.value);
          if (propias.status === "fulfilled") {
            setInscripcion(propias.value.find((i) => i.activa && i.actividadId === datos.value.id) ?? null);
          }
        }

        const fallo = datos.status === "rejected" ? datos.reason : propias.reason;
        setError(fallo ? mensajeDeError(fallo, `No se pudo cargar ${tipoEtiqueta}.`) : "");
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
  }, [id, obtener, tipoEtiqueta]);

  const ejecutar = async (accion, mensajeDeFallo) => {
    setError("");
    setEnviando(true);
    try {
      await accion();
    } catch (e) {
      setError(mensajeDeError(e, mensajeDeFallo));
    } finally {
      setEnviando(false);
    }
  };

  const inscribir = () => ejecutar(async () => {
    const nueva = await inscribirme(actividad.id);
    if (nueva.estado === "PENDIENTE_PAGO") {
      navigate(`/inscripciones/${nueva.id}/cobro`);
    } else {
      navigate(volver, { state: { aviso: `Te inscribiste a "${actividad.nombre}".` } });
    }
  }, "No se pudo completar la inscripción.");

  const pedirBaja = () => ejecutar(async () => setBaja(await previsualizarBaja(inscripcion.id)), "No se pudo consultar la baja.");

  const confirmarBaja = () => ejecutar(async () => {
    await darmeDeBaja(actividad.id, socioId);
    navigate(volver, { state: { aviso: `Te diste de baja de "${actividad.nombre}".` } });
  }, "No se pudo cancelar la inscripción.");

  if (cargando) {
    return <p className="py-12 text-center text-sm text-muted-foreground">Cargando...</p>;
  }
  if (!actividad) {
    return <ErrorBanner>{error || "No se encontró."}</ErrorBanner>;
  }

  const datos = describir(actividad);
  const sinLugar = (actividad.cupoDisponible ?? 0) <= 0;
  const porPagar = inscripcion?.estado === "PENDIENTE_PAGO" || Number(inscripcion?.saldoPendiente) > 0;

  return (
    <div className="w-full min-w-0 space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="min-w-0 text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">{actividad.nombre}</h1>
        <div className="flex shrink-0 items-center gap-4">
          {inscripcion && <EstadoDeInscripcion inscripcion={inscripcion} />}
          <VolverButton to={volver} />
        </div>
      </div>

      {error && <ErrorBanner>{error}</ErrorBanner>}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="min-w-0 space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-5 text-base font-bold tracking-tight">Datos</h2>
            <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
              <Dato etiqueta="Tipo" valor={datos.etiqueta} />
              <Dato etiqueta={datos.etiquetaInstructor} valor={datos.instructor} />
              <Dato etiqueta="Cancha" valor={actividad.cancha ?? "A confirmar"} />
              <Dato etiqueta="Horario" valor={datos.horario} />
              <Dato etiqueta="Vigencia" valor={datos.vigencia} />
              <Dato etiqueta="Lugares libres" valor={Math.max(0, actividad.cupoDisponible ?? 0)} />
              {datos.extra && <Dato etiqueta={datos.extra.etiqueta} valor={datos.extra.valor} />}
              <div className="sm:col-span-2">
                <dt className="mb-2 text-xs text-muted-foreground">Días</dt>
                <dd><DiasDeLaSemana dias={actividad.dias} /></dd>
              </div>
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-5 text-base font-bold tracking-tight">Pago</h2>
            <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
              <Dato etiqueta="Valor de inscripción" valor={conValor(actividad) ? moneda(inscripcion?.montoTotal ?? actividad.valorMensual) : "Sin cargo"} />
              {inscripcion && <Dato etiqueta="Ya pagaste" valor={moneda(inscripcion.montoPagado)} />}
              {inscripcion?.estado === "PENDIENTE_PAGO" && <Dato etiqueta="Seña a pagar" valor={moneda(inscripcion.montoSenia)} />}
              {inscripcion?.estado !== "PENDIENTE_PAGO" && Number(inscripcion?.saldoPendiente) > 0 && <Dato etiqueta="Saldo" valor={moneda(inscripcion.saldoPendiente)} />}
            </dl>
            {!inscripcion && conValor(actividad) && (
              <p className="mt-4 text-sm text-muted-foreground">Para inscribirte pagás una seña. Hasta pagarla, tu lugar queda retenido.</p>
            )}
          </div>
        </div>

        <div className="min-w-0">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-5 text-base font-bold tracking-tight">Acciones</h2>
            <div className="flex flex-col gap-2">
              {!inscripcion && (
                <button type="button" disabled={sinLugar || enviando} onClick={inscribir} className={BOTON_PRINCIPAL}>
                  {enviando && <Loader2 className="size-4 animate-spin" />}
                  {sinLugar ? "Sin lugares" : "Inscribirme"}
                </button>
              )}
              {porPagar && (
                <Link to={`/inscripciones/${inscripcion.id}/cobro`} className={BOTON_PRINCIPAL}>
                  {inscripcion.estado === "PENDIENTE_PAGO" ? "Pagar seña" : "Pagar saldo"}
                </Link>
              )}
              {inscripcion && !baja && (
                <Button variant="outline" className="w-full rounded-full" disabled={enviando} onClick={pedirBaja}>Darme de baja</Button>
              )}
              {baja && (
                <div className="space-y-3 rounded-lg bg-[var(--estado-aviso-bg)] p-4">
                  <p className="text-sm text-[var(--estado-aviso-fg)]">{baja.mensaje}</p>
                  <div className="flex gap-2">
                    <Button variant="outline" className="flex-1 rounded-full" onClick={() => setBaja(null)}>Volver</Button>
                    <Button variant="destructive" className="flex-1 rounded-full" disabled={enviando} onClick={confirmarBaja}>Confirmar baja</Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
