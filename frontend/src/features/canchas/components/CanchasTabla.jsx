import { ChevronLeft, ChevronRight, Edit3, Eye, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CLASES_ESTADO, CLASES_SUPERFICIE, determinarEstado, etiquetaSuperficie } from "../model/cancha";

/**
 * Listado de canchas (CU de Canchas).
 *
 * Misma idea que InsumosTabla: tabla desde md hacia arriba y tarjetas en
 * teléfono, con la tabla/tarjeta en su propio componente en vez de vivir
 * adentro de la página.
 */
export function CanchasTabla({
  canchas,
  pagina = 0,
  totalPaginas = 0,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onEditar,
  onEliminar,
}) {
  const desde = totalElementos === 0 ? 0 : pagina * 20 + 1;
  const hasta = Math.min((pagina + 1) * 20, totalElementos);

  return (
    <>
      {/* Vista de Escritorio y Tablets */}
      <div className="hidden overflow-x-auto rounded-xl border border-border md:block">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs font-bold uppercase tracking-wider text-muted-foreground">
              <th className="px-4 py-3">Cancha</th>
              <th className="px-4 py-3">Tipo de cancha</th>
              <th className="px-4 py-3">Superficie</th>
              <th className="px-4 py-3">Capacidad</th>
              <th className="px-4 py-3 text-right">Precio / hora</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3 text-right">
                <span className="sr-only">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {canchas.length === 0 ? (
              <tr>
                <td colSpan="7" className="py-8 text-center text-muted-foreground">
                  No hay canchas registradas en el sistema.
                </td>
              </tr>
            ) : (
              canchas.map((cancha) => {
                const estadoVisual = determinarEstado(cancha);
                return (
                  <tr key={cancha.id} className="border-b border-border/60 transition-colors last:border-0 hover:bg-muted">
                    <td className="px-4 py-3.5 font-medium">{cancha.identificador}</td>
                    <td className="px-4 py-3.5">
                      {cancha.tipoCanchaNombre ? (
                        <span className="font-medium text-foreground">{cancha.tipoCanchaNombre}</span>
                      ) : (
                        <span className="text-xs text-muted-foreground italic">Sin tipo</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${CLASES_SUPERFICIE[cancha.tipoSuperficie] || "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"}`}>
                        {cancha.superficie || etiquetaSuperficie(cancha.tipoSuperficie)}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 tabular-nums">{cancha.capacidad} jugadores</td>
                    <td className="px-4 py-3.5 text-right tabular-nums">
                      {cancha.precioPorHora ? `$ ${cancha.precioPorHora.toLocaleString("es-AR")}` : "-"}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${CLASES_ESTADO[estadoVisual]}`}>
                        {estadoVisual}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-0.5">
                        <Button variant="ghost" size="icon-sm" aria-label="Ver detalle" onClick={() => onVer(cancha)}>
                          <Eye size={18} />
                        </Button>
                        <Button variant="ghost" size="icon-sm" aria-label="Editar" onClick={() => onEditar(cancha)}>
                          <Edit3 size={18} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          aria-label="Eliminar"
                          className="hover:text-[var(--estado-error-fg)]"
                          onClick={() => onEliminar(cancha)}
                        >
                          <Trash2 size={18} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Vista de Teléfono */}
      <div className="space-y-3 md:hidden">
        {canchas.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">No hay canchas registradas en el sistema.</p>
        ) : (
          canchas.map((cancha) => {
            const estadoVisual = determinarEstado(cancha);
            return (
              <div key={cancha.id} className="rounded-xl border border-border bg-card p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold">{cancha.identificador}</p>
                    <p className="mt-0.5 truncate text-xs text-muted-foreground">
                      {cancha.tipoCanchaNombre ? `${cancha.tipoCanchaNombre} • ` : ""}
                      {cancha.superficie || etiquetaSuperficie(cancha.tipoSuperficie)}
                    </p>
                  </div>
                  <span className={`shrink-0 inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${CLASES_ESTADO[estadoVisual]}`}>
                    {estadoVisual}
                  </span>
                </div>
                <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                  <div>
                    <dt className="text-[11px] text-muted-foreground">Capacidad</dt>
                    <dd className="text-sm font-medium tabular-nums">{cancha.capacidad} jug.</dd>
                  </div>
                  <div>
                    <dt className="text-[11px] text-muted-foreground">Precio/hora</dt>
                    <dd className="text-sm font-medium tabular-nums">
                      {cancha.precioPorHora ? `$ ${cancha.precioPorHora.toLocaleString("es-AR")}` : "-"}
                    </dd>
                  </div>
                </dl>
                <div className="mt-4 flex gap-2">
                  <Button variant="outline" size="sm" className="flex-1" onClick={() => onVer(cancha)}>
                    Ver
                  </Button>
                  <Button variant="outline" size="sm" className="flex-1" onClick={() => onEditar(cancha)}>
                    Editar
                  </Button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Barra de Paginación */}
      <nav className="flex items-center justify-between gap-3 pt-2" aria-label="Paginación">
        <p className="text-xs tabular-nums text-muted-foreground">
          {totalElementos === 0 ? "0 de 0" : `${desde}–${hasta} de ${totalElementos}`}
        </p>
        <div className="flex items-center gap-1.5">
          <Button
            variant="outline"
            size="icon-sm"
            aria-label="Pagina anterior"
            disabled={pagina === 0}
            onClick={() => onCambiarPagina(Math.max(0, pagina - 1))}
          >
            <ChevronLeft size={18} />
          </Button>
          <Button
            variant="outline"
            size="icon-sm"
            aria-label="Pagina siguiente"
            disabled={pagina + 1 >= totalPaginas}
            onClick={() => onCambiarPagina(pagina + 1)}
          >
            <ChevronRight size={18} />
          </Button>
        </div>
      </nav>
    </>
  );
}
