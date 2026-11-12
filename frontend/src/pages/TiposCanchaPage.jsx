import { useState, useEffect, useCallback } from "react";
import { Plus, Pencil, Trash2, FileDown, Layers, AlertTriangle, Eye } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { VolverButton } from "@/components/ui/volver-button";
import { SuccessBanner } from "@/components/ui/success-banner";
import { CLASES_SUPERFICIE, etiquetaSuperficie } from "@/features/canchas/model/cancha";
import { listarTiposCancha, descargarReportePdf } from "@/features/canchas/api/tiposCanchaApi";
import { EliminarTipoCanchaDialog } from "@/features/canchas/components/EliminarTipoCanchaDialog";

export default function TiposCanchaPage() {
  const navigate = useNavigate();
  const [tipos, setTipos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(false);
  const [descargandoPdf, setDescargandoPdf] = useState(false);

  const [aEliminar, setAEliminar] = useState(null);
  const [aviso, setAviso] = useState("");

  const cargarTipos = useCallback(async () => {
    setCargando(true);
    setError(false);
    try {
      const data = await listarTiposCancha();
      setTipos(data);
    } catch {
      setError(true);
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargarTipos();
  }, [cargarTipos]);

  async function alEliminar(mensaje) {
    await cargarTipos();
    setAEliminar(null);
    setAviso(mensaje);
  }

  async function handleDescargarReporte() {
    setDescargandoPdf(true);
    try {
      const blob = await descargarReportePdf();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "reporte-tipos-cancha.pdf";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Error al descargar reporte PDF:", err);
      alert("No se pudo generar el reporte PDF.");
    } finally {
      setDescargandoPdf(false);
    }
  }

  if (error) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se pudo conectar con el servidor para cargar los tipos de cancha.</p>
        <button onClick={cargarTipos} className="font-bold text-primary hover:underline">
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Tipos de cancha</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Definición de superficies y capacidades para las canchas del club.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" onClick={handleDescargarReporte} disabled={descargandoPdf || cargando}>
            <FileDown size={18} />
            <span>{descargandoPdf ? "Generando..." : "Descargar reporte (PDF)"}</span>
          </Button>
          <Button onClick={() => navigate("/canchas/tipos/nuevo")}>
            <Plus size={18} strokeWidth={2.5} />
            <span>Nuevo tipo</span>
          </Button>
          <VolverButton to="/canchas" />
        </div>
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>

      <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
        <h2 className="mb-5 text-base font-bold tracking-tight">Listado de tipos de cancha</h2>

        {cargando ? (
          <p className="py-10 text-center text-sm text-muted-foreground">Cargando tipos de cancha...</p>
        ) : tipos.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-12 text-center text-muted-foreground">
            <Layers size={36} />
            <p className="text-sm">No hay tipos de cancha registrados.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="pb-3 font-semibold">Nombre</th>
                  <th className="pb-3 font-semibold">Superficie</th>
                  <th className="pb-3 font-semibold">Capacidad</th>
                  <th className="pb-3 font-semibold">Canchas operativas</th>
                  <th className="pb-3 text-right font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {tipos.map((tipo) => (
                  <tr key={tipo.id} className="hover:bg-muted/40 transition-colors">
                    <td className="py-3.5 pr-4 font-medium">
                      <div>{tipo.nombre}</div>
                      {tipo.descripcion && (
                        <div className="text-xs text-muted-foreground font-normal">{tipo.descripcion}</div>
                      )}
                    </td>
                    <td className="py-3.5 pr-4">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${CLASES_SUPERFICIE[tipo.tipoSuperficie] || "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"}`}>
                        {tipo.superficie || etiquetaSuperficie(tipo.tipoSuperficie)}
                      </span>
                    </td>
                    <td className="py-3.5 pr-4 text-muted-foreground">
                      {tipo.capacidad} jugadores
                    </td>
                    <td className="py-3.5 pr-4 text-muted-foreground">
                      {tipo.cantidadCanchas} cancha(s)
                    </td>
                    <td className="py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => navigate(`/canchas/tipos/${tipo.id}`)}
                          title="Ver detalle del tipo de cancha"
                        >
                          <Eye size={16} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => navigate(`/canchas/tipos/editar/${tipo.id}`)}
                          title="Modificar tipo de cancha"
                        >
                          <Pencil size={16} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => setAEliminar(tipo)}
                          title="Dar de baja tipo de cancha"
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 size={16} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <EliminarTipoCanchaDialog
        abierto={Boolean(aEliminar)}
        tipoCancha={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alEliminar}
      />
    </div>
  );
}
