import { useState, useEffect } from "react";
import { AlertTriangle, Edit, Trash2, MapPin } from "lucide-react";
import { useNavigate, useParams, Link } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import { VolverButton } from "@/components/ui/volver-button";
import { Button } from "@/components/ui/button";
import { CLASES_SUPERFICIE, etiquetaSuperficie } from "@/features/canchas/model/cancha";
import { obtenerTipoCancha } from "@/features/canchas/api/tiposCanchaApi";
import { EliminarTipoCanchaDialog } from "@/features/canchas/components/EliminarTipoCanchaDialog";

export default function TipoCanchaDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [tipo, setTipo] = useState(null);
  const [canchasAsociadas, setCanchasAsociadas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(false);
  const [mostrarModalEliminar, setMostrarModalEliminar] = useState(false);

  useEffect(() => {
    const cargar = async () => {
      try {
        const [datosTipo, resCanchas] = await Promise.all([
          obtenerTipoCancha(id),
          axiosClient.get("/canchas?size=50").catch(() => ({ data: { contenido: [] } })),
        ]);
        setTipo(datosTipo);

        const todasCanchas = Array.isArray(resCanchas.data?.contenido)
          ? resCanchas.data.contenido
          : Array.isArray(resCanchas.data)
            ? resCanchas.data
            : [];
        const vinculadas = todasCanchas.filter((c) => String(c.tipoCanchaId) === String(id));
        setCanchasAsociadas(vinculadas);
      } catch {
        setError(true);
      } finally {
        setCargando(false);
      }
    };
    cargar();
  }, [id]);

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center font-medium text-muted-foreground">
        Cargando datos del tipo de cancha...
      </div>
    );
  }

  if (error || !tipo) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del tipo de cancha.</p>
        <button
          onClick={() => navigate("/canchas/tipos")}
          className="font-bold text-primary hover:underline"
        >
          Volver a tipos de cancha
        </button>
      </div>
    );
  }

  const estadoVisual = tipo.activo !== false ? "Activo" : "Inactivo";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">{tipo.nombre}</h1>
        <div className="flex items-center gap-4">
          <span className="rounded-full bg-[var(--estado-ok-bg)] px-3 py-1 text-xs font-bold text-[var(--estado-ok-fg)]">
            {estadoVisual}
          </span>
          <VolverButton to="/canchas/tipos" />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {/* Tarjeta de Datos Básicos */}
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-5 text-base font-bold tracking-tight">Datos del tipo de cancha</h2>
            <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
              <div>
                <dt className="text-xs text-muted-foreground">Nombre</dt>
                <dd className="mt-1 text-sm font-medium">{tipo.nombre}</dd>
              </div>

              <div>
                <dt className="text-xs text-muted-foreground">Superficie</dt>
                <dd className="mt-1">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${
                      CLASES_SUPERFICIE[tipo.tipoSuperficie] || "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"
                    }`}
                  >
                    {tipo.superficie || etiquetaSuperficie(tipo.tipoSuperficie)}
                  </span>
                </dd>
              </div>

              <div>
                <dt className="text-xs text-muted-foreground">Capacidad reglamentaria</dt>
                <dd className="mt-1 text-sm font-medium">
                  {tipo.capacidad} jugadores {tipo.capacidad === 2 ? "(Singles)" : "(Dobles / Singles)"}
                </dd>
              </div>

              <div>
                <dt className="text-xs text-muted-foreground">Canchas operativas asociadas</dt>
                <dd className="mt-1 text-sm font-medium">
                  {tipo.cantidadCanchas || canchasAsociadas.length} cancha(s)
                </dd>
              </div>

              <div>
                <dt className="text-xs text-muted-foreground">Estado</dt>
                <dd className="mt-1">
                  <span className="inline-flex items-center rounded-full bg-[var(--estado-ok-bg)] px-2.5 py-1 text-xs font-semibold text-[var(--estado-ok-fg)]">
                    {estadoVisual}
                  </span>
                </dd>
              </div>
            </dl>
          </div>

          {/* Tarjeta de Detalles Opcionales y Canchas Físicas */}
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-4 text-base font-bold tracking-tight">Detalles y observaciones</h2>
            <div className="space-y-4">
              <div>
                <p className="text-xs text-muted-foreground">Descripción técnica del piso</p>
                <p className="mt-1 text-sm text-foreground">
                  {tipo.descripcion || "Sin descripción adicional especificada para este tipo de cancha."}
                </p>
              </div>

              <div className="border-t border-border pt-4">
                <p className="text-xs text-muted-foreground mb-2">Canchas físicas asignadas a este tipo</p>
                {canchasAsociadas.length === 0 ? (
                  <p className="text-sm text-muted-foreground italic">
                    Actualmente ninguna cancha física tiene asignado este tipo.
                  </p>
                ) : (
                  <div className="flex flex-wrap gap-2 pt-1">
                    {canchasAsociadas.map((c) => (
                      <Link
                        key={c.id}
                        to={`/canchas/${c.id}`}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-muted/40 px-3 py-1.5 text-xs font-medium text-foreground hover:bg-muted transition-colors"
                      >
                        <MapPin size={13} className="text-primary" />
                        <span>{c.identificador}</span>
                        {c.precioPorHora && (
                          <span className="text-muted-foreground">(${c.precioPorHora.toLocaleString("es-AR")}/h)</span>
                        )}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Tarjeta Lateral de Acciones */}
        <div className="space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-4 text-base font-bold tracking-tight">Acciones</h2>
            <div className="flex flex-col gap-2">
              <Button
                variant="outline"
                className="w-full justify-start gap-2"
                onClick={() => navigate(`/canchas/tipos/editar/${tipo.id}`)}
              >
                <Edit size={16} />
                <span>Modificar tipo</span>
              </Button>

              <Button
                variant="ghost"
                className="w-full justify-start gap-2 text-destructive hover:bg-destructive/10 hover:text-destructive"
                onClick={() => setMostrarModalEliminar(true)}
              >
                <Trash2 size={16} />
                <span>Dar de baja</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      <EliminarTipoCanchaDialog
        abierto={mostrarModalEliminar}
        tipoCancha={tipo}
        onCerrar={() => setMostrarModalEliminar(false)}
        onEliminado={() => {
          navigate("/canchas/tipos");
        }}
      />
    </div>
  );
}

