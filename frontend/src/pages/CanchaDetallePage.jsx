import { fechaLocal, hoyLocal } from "@/lib/fechas";
import { useState, useEffect } from "react";
import { AlertTriangle, Edit, ShieldAlert, Trash2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import { VolverButton } from "@/components/ui/volver-button";
import { CLASES_ESTADO, CLASES_SUPERFICIE, etiquetaSuperficie } from "@/features/canchas/model/cancha";
import { EliminarCanchaDialog } from "@/features/canchas/components/EliminarCanchaDialog";

const ETIQUETA_DIA = {
  MONDAY: "Lunes",
  TUESDAY: "Martes",
  WEDNESDAY: "Miércoles",
  THURSDAY: "Jueves",
  FRIDAY: "Viernes",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
};

const ORDEN_DIA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

const formatoHora = (hora) => hora?.slice(0, 5) || "";
const formatoFechaCorta = (fecha) => {
  const [, mes, dia] = fecha.split("-");
  return `${dia}/${mes}`;
};
const unDiaDespues = (fecha) => {
  const d = new Date(`${fecha}T00:00:00`);
  d.setDate(d.getDate() + 1);
  return fechaLocal(d);
};

export default function CanchaDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [showModal, setShowModal] = useState(false);
  const [cancha, setCancha] = useState(null);
  const [horarios, setHorarios] = useState([]);
  const [bloqueoHoy, setBloqueoHoy] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const cargarDatos = async () => {
      try {
        const hoy = hoyLocal();
        const limite = new Date();
        limite.setDate(limite.getDate() + 89);
        const hasta = fechaLocal(limite);

        const [{ data: datosCancha }, { data: datosHorarios }, { data: datosBloqueos }] = await Promise.all([
          axiosClient.get(`/canchas/${id}`),
          axiosClient.get(`/canchas/${id}/horarios`),
          axiosClient.get(`/canchas/${id}/bloqueos`, { params: { desde: hoy, hasta } }),
        ]);
        setCancha(datosCancha);
        setHorarios([...datosHorarios].sort((a, b) => ORDEN_DIA.indexOf(a.dia) - ORDEN_DIA.indexOf(b.dia)));

        // El bloqueo "vigente" es el tramo de días consecutivos (mismo motivo) que incluye hoy
        const deHoy = datosBloqueos.find((b) => b.fecha === hoy);
        if (deHoy) {
          let desdeRango = deHoy.fecha;
          let hastaRango = deHoy.fecha;
          const porFecha = new Map(datosBloqueos.map((b) => [b.fecha, b]));
          let cursor = unDiaDespues(hastaRango);
          while (porFecha.has(cursor) && porFecha.get(cursor).motivo === deHoy.motivo) {
            hastaRango = cursor;
            cursor = unDiaDespues(cursor);
          }
          setBloqueoHoy({ ...deHoy, desdeRango, hastaRango });
        } else {
          setBloqueoHoy(null);
        }
      } catch {
        setError(true);
      } finally {
        setCargando(false);
      }
    };
    cargarDatos();
  }, [id]);

  if (cargando) return <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">Cargando datos...</div>;

  if (error || !cancha) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos de la cancha.</p>
        <button onClick={() => navigate('/canchas')} className="text-primary font-bold hover:underline">Volver a canchas</button>
      </div>
    );
  }

  const estadoVisual = cancha.enMantenimiento ? "Mantenimiento" : (cancha.activa ? "Disponible" : "Deshabilitada");

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">{cancha.identificador}</h1>
        <div className="flex items-center gap-4">
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]">
            {estadoVisual}
          </span>
          <VolverButton to="/canchas" />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Datos</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              <div><dt className="text-xs text-muted-foreground">Nombre</dt><dd className="mt-1 text-sm font-medium">{cancha.identificador}</dd></div>
              <div>
                <dt className="text-xs text-muted-foreground">Superficie</dt>
                <dd className="mt-1">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${CLASES_SUPERFICIE[cancha.tipoSuperficie] || "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"}`}>
                    {cancha.superficie || etiquetaSuperficie(cancha.tipoSuperficie)}
                  </span>
                </dd>
              </div>
              <div><dt className="text-xs text-muted-foreground">Capacidad</dt><dd className="mt-1 text-sm font-medium">{cancha.capacidad} jugadores</dd></div>
              <div><dt className="text-xs text-muted-foreground">Precio por hora</dt><dd className="mt-1 text-sm font-medium tabular-nums">$ {cancha.precioPorHora?.toLocaleString("es-AR")}</dd></div>
              <div>
                <dt className="text-xs text-muted-foreground">Tipo de cancha</dt>
                <dd className="mt-1 text-sm font-medium">{cancha.tipoCanchaNombre || "Sin tipo predeterminado"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Horario</dt>
                <dd className="mt-1 text-sm font-medium">
                  {horarios.length === 0 ? (
                    <span className="text-muted-foreground font-normal">Sin horario cargado</span>
                  ) : horarios.every((h) => h.desde === horarios[0].desde && h.hasta === horarios[0].hasta) ? (
                    <span>{formatoHora(horarios[0].desde)} a {formatoHora(horarios[0].hasta)}</span>
                  ) : (
                    <ul className="space-y-0.5">
                      {horarios.map((h) => (
                        <li key={h.id}>
                          {ETIQUETA_DIA[h.dia] || h.dia}: {formatoHora(h.desde)} a {formatoHora(h.hasta)}
                        </li>
                      ))}
                    </ul>
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Estado</dt>
                <dd className="mt-1">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${CLASES_ESTADO[estadoVisual]}`}>
                    {estadoVisual}
                  </span>
                </dd>
              </div>
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Características</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div className="flex items-center gap-2">
                <span className={`inline-block w-2.5 h-2.5 rounded-full ${cancha.iluminacion ? "bg-primary" : "bg-muted-foreground/30"}`} />
                <span className={`text-sm ${cancha.iluminacion ? "font-semibold text-foreground" : "text-muted-foreground line-through"}`}>
                  Iluminación
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`inline-block w-2.5 h-2.5 rounded-full ${cancha.techada ? "bg-primary" : "bg-muted-foreground/30"}`} />
                <span className={`text-sm ${cancha.techada ? "font-semibold text-foreground" : "text-muted-foreground line-through"}`}>
                  Techada
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`inline-block w-2.5 h-2.5 rounded-full ${cancha.tribuna ? "bg-primary" : "bg-muted-foreground/30"}`} />
                <span className={`text-sm ${cancha.tribuna ? "font-semibold text-foreground" : "text-muted-foreground line-through"}`}>
                  Tribuna
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`inline-block w-2.5 h-2.5 rounded-full ${cancha.aptaTorneos ? "bg-primary" : "bg-muted-foreground/30"}`} />
                <span className={`text-sm ${cancha.aptaTorneos ? "font-semibold text-foreground" : "text-muted-foreground line-through"}`}>
                  Apta torneos
                </span>
              </div>
            </div>

            {cancha.observaciones && (
              <div className="mt-5 pt-4 border-t border-border">
                <p className="text-xs text-muted-foreground font-semibold mb-1">Observaciones</p>
                <p className="text-sm text-foreground whitespace-pre-wrap">{cancha.observaciones}</p>
              </div>
            )}
          </div>

          {bloqueoHoy && (
            <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
              <h2 className="text-base font-bold tracking-tight">Bloqueo vigente</h2>
              {bloqueoHoy.motivo && <p className="mt-0.5 text-sm text-muted-foreground mb-4">{bloqueoHoy.motivo}</p>}
              <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]">
                <ShieldAlert size={18} />
                <p className="text-sm font-semibold">
                  Bloqueada del {formatoFechaCorta(bloqueoHoy.desdeRango)} al {formatoFechaCorta(bloqueoHoy.hastaRango)}
                </p>
              </div>
            </div>
          )}
        </div>

        <div>
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Acciones</h2>
            <div className="flex flex-col gap-2">
              <button onClick={() => navigate(`/canchas/editar/${id}`)} className="w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors cursor-pointer">
                <Edit size={18} /> Editar
              </button>
              <button onClick={() => setShowModal(true)} className="w-full flex items-center justify-center gap-2 h-10 px-5 rounded-full text-sm font-semibold text-[var(--estado-error-fg)] hover:bg-[var(--estado-error-bg)] transition-colors cursor-pointer">
                <Trash2 size={18} /> Eliminar
              </button>
            </div>
          </div>
        </div>
      </div>

      <EliminarCanchaDialog
        abierto={showModal}
        cancha={cancha}
        onCerrar={() => setShowModal(false)}
        onEliminada={() => navigate("/canchas")}
      />
    </div>
  );
}
