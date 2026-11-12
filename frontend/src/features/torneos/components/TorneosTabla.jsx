import { Eye, Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { esLiga, etiquetaDeTipo } from "../model/competencia";

const ESTADOS_MAP = {
  PLANIFICADO: { label: "Inscripción abierta", variant: "default" },
  INSCRIPCION_ABIERTA: { label: "Inscripción abierta", variant: "default" },
  EN_CURSO: { label: "En curso", variant: "secondary" },
  FINALIZADO: { label: "Finalizado", variant: "outline" },
  CANCELADO: { label: "Cancelado", variant: "destructive" },
};

const SUPERFICIES_MAP = {
  LADRILLO: "Polvo de ladrillo",
  CEMENTO: "Cemento",
  PASTO: "Césped",
};

export function TorneosTabla({
  torneos = [],
  esAdmin = false,
  onVerDetalle,
  onEditar,
  onEliminar,
}) {
  if (!torneos.length) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
        No hay torneos registrados.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-card shadow-sm">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-border bg-muted/40 text-xs font-semibold uppercase text-muted-foreground">
          <tr>
            <th className="py-3 px-4">Código / ID</th>
            <th className="py-3 px-4">Nombre</th>
            <th className="py-3 px-4">Tipo</th>
            <th className="py-3 px-4">Categoría</th>
            <th className="py-3 px-4">Modalidad</th>
            <th className="py-3 px-4">Superficie</th>
            <th className="py-3 px-4 text-center">Cupos</th>
            <th className="py-3 px-4 text-center">Estado</th>
            <th className="py-3 px-4 text-right">Acciones</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {torneos.map((t) => {
            const inscriptos = t.cantidadInscriptos ?? t.inscriptos?.length ?? 0;
            const cupoMax = t.cupoMaximo ?? 16;
            const estadoInfo = ESTADOS_MAP[t.estado] || {
              label: t.estado || "Desconocido",
              variant: "outline",
            };

            return (
              <tr key={t.id} className="hover:bg-muted/30 transition-colors">
                <td className="py-3 px-4 font-mono text-xs text-muted-foreground">
                  #{t.id}
                </td>
                <td className="py-3 px-4 font-semibold text-foreground">
                  {t.nombre}
                </td>
                <td className="py-3 px-4">
                  <Badge variant={esLiga(t) ? "default" : "secondary"}>{etiquetaDeTipo(t)}</Badge>
                </td>
                <td className="py-3 px-4 text-muted-foreground">{t.categoria}</td>
                <td className="py-3 px-4 text-muted-foreground">{t.modalidad}</td>
                <td className="py-3 px-4 text-muted-foreground">
                  {SUPERFICIES_MAP[t.tipoSuperficie] || t.tipoSuperficie?.replace(/_/g, " ") || "-"}
                </td>
                <td className="py-3 px-4 text-center font-mono text-xs tabular-nums">
                  {inscriptos}/{cupoMax}
                </td>
                <td className="py-3 px-4 text-center">
                  <Badge variant={estadoInfo.variant}>{estadoInfo.label}</Badge>
                </td>
                <td className="py-3 px-4 text-right">
                  <div className="flex items-center justify-end gap-1.5">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onVerDetalle?.(t)}
                      className="h-8 gap-1 px-2.5 text-xs"
                    >
                      <Eye className="size-3.5" />
                      Ver detalle
                    </Button>

                    {esAdmin && (
                      <>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => onEditar?.(t)}
                          className="h-8 gap-1 px-2.5 text-xs"
                        >
                          <Pencil className="size-3.5" />
                          Editar
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => onEliminar?.(t)}
                          className="h-8 gap-1 px-2.5 text-xs"
                        >
                          <Trash2 className="size-3.5" />
                          Eliminar
                        </Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
