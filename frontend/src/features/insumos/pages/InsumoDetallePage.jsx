import { useEffect, useState } from "react";
import { AlertTriangle, Edit, Trash2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { stockTotal } from "../model/insumo";
import { listarMovimientos, obtenerInsumo } from "../api/insumosApi";
import { EstadoStockBadge } from "../components/EstadoStockBadge";
import { CategoriaInsumoBadge } from "../components/CategoriaInsumoBadge";
import { EliminarInsumoDialog } from "../components/EliminarInsumoDialog";
import { HistorialMovimientos } from "../components/HistorialMovimientos";

export default function InsumoDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [showModal, setShowModal] = useState(false);
  const [insumo, setInsumo] = useState(null);
  const [movimientos, setMovimientos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    Promise.all([
      obtenerInsumo(id),
      // El historial es accesorio: si falla, el detalle se muestra igual.
      listarMovimientos(id, { size: 10 }).catch(() => ({ contenido: [] })),
    ])
      .then(([datosInsumo, pagina]) => {
        setInsumo(datosInsumo);
        setMovimientos(pagina.contenido || []);
      })
      .catch(() => setError(true))
      .finally(() => setCargando(false));
  }, [id]);

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">
        Cargando datos...
      </div>
    );
  }

  if (error || !insumo) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del insumo.</p>
        <button onClick={() => navigate("/insumos")} className="text-primary font-bold hover:underline">
          Volver a insumos
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">{insumo.nombre}</h1>
        <div className="flex items-center gap-4">
          <EstadoStockBadge insumo={insumo} />
          <VolverButton to="/insumos" />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Datos</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              <div>
                <dt className="text-xs text-muted-foreground">Insumo</dt>
                <dd className="mt-1 text-sm font-medium">{insumo.nombre}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Categoría</dt>
                <dd className="mt-1">
                  <CategoriaInsumoBadge categoria={insumo.categoria} />
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Stock total</dt>
                <dd className="mt-1 text-sm font-medium tabular-nums">{stockTotal(insumo)} unidades</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Disponibles</dt>
                <dd className="mt-1 text-sm font-medium tabular-nums">{insumo.cantidadDisponible}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Asignadas a turnos</dt>
                <dd className="mt-1 text-sm font-medium tabular-nums">{insumo.enUso}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Stock mínimo</dt>
                <dd className="mt-1 text-sm font-medium tabular-nums">{insumo.cantidadMinima}</dd>
              </div>
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Movimientos de stock</h2>
            <HistorialMovimientos movimientos={movimientos} />
          </div>
        </div>

        <div>
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Acciones</h2>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => navigate(`/insumos/editar/${id}`)}
                className="w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors"
              >
                <Edit size={18} /> Editar
              </button>
              <button
                onClick={() => setShowModal(true)}
                className="w-full flex items-center justify-center gap-2 h-10 px-5 rounded-full text-sm font-semibold text-[var(--estado-error-fg)] hover:bg-[var(--estado-error-bg)] transition-colors"
              >
                <Trash2 size={18} /> Eliminar
              </button>
            </div>
          </div>
        </div>
      </div>

      <EliminarInsumoDialog
        abierto={showModal}
        insumo={insumo}
        onCerrar={() => setShowModal(false)}
        onEliminado={() => navigate("/insumos")}
      />
    </div>
  );
}
