import { ChevronLeft, ChevronRight, ClipboardCheck, Eye, Pencil, Printer, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import {
  ETIQUETA_ESTADO,
  ETIQUETA_MODALIDAD,
  cupoCompleto,
  diasTexto,
  fechaCorta,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreEntrenador,
  porcentajeOcupado,
  rangoFechas,
} from "../model/entrenamiento";

/** Ocupación del cupo: números arriba y barra como refuerzo visual. */
export function CupoBadge({ entrenamiento }) {
  const completo = cupoCompleto(entrenamiento);
  const libres = lugaresLibres(entrenamiento);
  const ocupado = entrenamiento.cupoOcupado ?? entrenamiento.inscriptos ?? 0;

  return (
    <div className="min-w-[95px] max-w-[120px] space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="font-medium tabular-nums">{ocupado}</span>
        <span className="text-muted-foreground tabular-nums">/ {entrenamiento.cupoMaximo}</span>
      </div>
      <div aria-hidden="true" className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={cn("h-full rounded-full transition-[width]", completo ? "bg-red-500" : "bg-primary")}
          style={{ width: `${porcentajeOcupado(entrenamiento)}%` }}
        />
      </div>
      <p className={cn("text-[11px]", completo ? "text-red-600 font-medium" : "text-muted-foreground")}>
        {completo ? "Cupo completo" : `${libres} ${libres === 1 ? "lugar" : "lugares"}`}
      </p>
    </div>
  );
}

function AccionesDeFila({ entrenamiento, onVer, onEditar, onAsistencia, onImprimir, onEliminar }) {
  const nombre =
    entrenamiento.nombre ||
    (entrenamiento.fecha ? `el entrenamiento del ${fechaCorta(entrenamiento.fecha)}` : "el entrenamiento");
  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    { icono: ClipboardCheck, texto: "Registrar la asistencia de", alAccionar: onAsistencia },
    ...(onEditar ? [{ icono: Pencil, texto: "Modificar", alAccionar: onEditar }] : []),
    { icono: Printer, texto: "Imprimir", alAccionar: onImprimir },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button
          key={texto}
          variant="ghost"
          size="icon-sm"
          aria-label={`${texto} ${nombre}`}
          onClick={() => alAccionar(entrenamiento)}
        >
          <Icono />
        </Button>
      ))}
      {onEliminar && (
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-destructive hover:bg-destructive/10"
          aria-label={`Eliminar ${nombre}`}
          onClick={() => onEliminar(entrenamiento)}
        >
          <Trash2 />
        </Button>
      )}
    </div>
  );
}

/** Listado de entrenamientos (CU39): tabla desde md hacia arriba y tarjetas en teléfono. */
export function EntrenamientosTabla({
  entrenamientos,
  esEntrenador = false,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onEditar,
  onAsistencia,
  onImprimir,
  onEliminar,
}) {
  const acciones = { onVer, onEditar, onAsistencia, onImprimir, onEliminar };

  return (
    <div className="space-y-4">
      <div className="hidden overflow-hidden rounded-xl border border-border bg-card shadow-xs md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className={esEntrenador ? "w-[36%]" : "w-[28%]"}>Entrenamiento</TableHead>
              <TableHead className={esEntrenador ? "w-[26%]" : "w-[22%]"}>Días y horario</TableHead>
              <TableHead className={esEntrenador ? "w-[12%]" : "w-[11%]"}>Cancha</TableHead>
              {!esEntrenador && (
                <TableHead className="w-[15%]">Entrenador</TableHead>
              )}
              <TableHead className={esEntrenador ? "w-[14%]" : "w-[12%]"}>Cupo</TableHead>
              <TableHead className={cn("text-right", esEntrenador ? "w-[12%] min-w-[130px]" : "w-[12%] min-w-[150px]")}>
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {entrenamientos.map((e) => (
              <TableRow key={e.id}>
                <TableCell>
                  <button
                    type="button"
                    onClick={() => onVer(e)}
                    className="font-medium text-foreground hover:text-primary hover:underline text-left cursor-pointer"
                  >
                    {e.nombre}
                  </button>
                  {e.objetivo && (
                    <span className="sr-only">{e.objetivo}</span>
                  )}
                </TableCell>
                <TableCell>
                  <p className="text-sm">{diasTexto(e)}</p>
                  <p className="text-xs text-muted-foreground tabular-nums">{horario(e)}</p>
                </TableCell>
                <TableCell className="text-sm">{nombreCancha(e)}</TableCell>
                {!esEntrenador && (
                  <TableCell className="text-sm">{nombreEntrenador(e)}</TableCell>
                )}
                <TableCell>
                  <CupoBadge entrenamiento={e} />
                </TableCell>
                <TableCell className="text-right whitespace-nowrap">
                  <AccionesDeFila entrenamiento={e} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista de teléfono hiper-simplificada (Modo Operate) */}
      <ul className="space-y-3 md:hidden">
        {entrenamientos.map((e) => {
          const nombre =
            e.nombre ||
            (e.fecha ? `el entrenamiento del ${fechaCorta(e.fecha)}` : "el entrenamiento");
          return (
            <li
              key={e.id}
              onClick={() => onVer(e)}
              onKeyDown={(ev) => {
                if (ev.target !== ev.currentTarget) return;
                if (ev.key === "Enter" || ev.key === " ") {
                  ev.preventDefault();
                  onVer(e);
                }
              }}
              tabIndex={0}
              role="button"
              aria-label={`Tarjeta del entrenamiento ${nombre}`}
              className="group relative block cursor-pointer rounded-2xl border border-border bg-card p-4 shadow-xs transition-all hover:border-foreground/30 hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-foreground group-hover:text-primary transition-colors">
                    {e.nombre}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground tabular-nums">
                    {diasTexto(e)} &middot; {horario(e)}
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {nombreCancha(e)}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1 shrink-0">
                  <span className="text-xs font-mono font-medium text-foreground tabular-nums">
                    {e.cupoOcupado ?? e.inscriptos ?? 0}/{e.cupoMaximo}
                  </span>
                  <span className="text-[11px] text-muted-foreground">
                    alumnos
                  </span>
                </div>
              </div>

              {/* Acciones móviles Mobile-First Operate: Tomar asistencia solo arriba, Detalle e Imprimir 50-50 abajo */}
              <div className="mt-3.5 space-y-2 border-t border-border pt-3">
                <Button
                  type="button"
                  size="default"
                  className="h-11 w-full gap-2 text-sm font-medium touch-manipulation shadow-xs"
                  onClick={(ev) => {
                    ev.stopPropagation();
                    onAsistencia(e);
                  }}
                  onKeyDown={(ev) => {
                    ev.stopPropagation();
                    if (ev.key === "Enter" || ev.key === " ") {
                      ev.preventDefault();
                      onAsistencia(e);
                    }
                  }}
                  aria-label={`Registrar asistencia de ${nombre}`}
                >
                  <ClipboardCheck className="size-4" />
                  Tomar asistencia
                </Button>

                <div className="grid grid-cols-2 gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="default"
                    className="h-10 w-full gap-2 text-xs font-medium touch-manipulation"
                    onClick={(ev) => {
                      ev.stopPropagation();
                      onVer(e);
                    }}
                    onKeyDown={(ev) => {
                      ev.stopPropagation();
                      if (ev.key === "Enter" || ev.key === " ") {
                        ev.preventDefault();
                        onVer(e);
                      }
                    }}
                    aria-label={`Ver detalle de ${nombre}`}
                  >
                    <Eye className="size-4" />
                    <span>Detalle</span>
                  </Button>

                  {onImprimir ? (
                    <Button
                      type="button"
                      variant="outline"
                      size="default"
                      className="h-10 w-full gap-2 text-xs font-medium touch-manipulation"
                      onClick={(ev) => {
                        ev.stopPropagation();
                        onImprimir(e);
                      }}
                      onKeyDown={(ev) => {
                        ev.stopPropagation();
                        if (ev.key === "Enter" || ev.key === " ") {
                          ev.preventDefault();
                          onImprimir(e);
                        }
                      }}
                      aria-label={`Imprimir ${nombre}`}
                    >
                      <Printer className="size-4" />
                      <span>Imprimir</span>
                    </Button>
                  ) : onEditar ? (
                    <Button
                      type="button"
                      variant="outline"
                      size="default"
                      className="h-10 w-full gap-2 text-xs font-medium touch-manipulation"
                      onClick={(ev) => {
                        ev.stopPropagation();
                        onEditar(e);
                      }}
                      aria-label={`Modificar ${nombre}`}
                    >
                      <Pencil className="size-4" />
                      <span>Modificar</span>
                    </Button>
                  ) : null}
                </div>

                {(onEditar || onEliminar) && (
                  <div className="flex justify-end gap-2 pt-0.5">
                    {onEditar && onImprimir && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-8 gap-1 text-xs text-muted-foreground touch-manipulation"
                        onClick={(ev) => {
                          ev.stopPropagation();
                          onEditar(e);
                        }}
                        aria-label={`Modificar ${nombre}`}
                      >
                        <Pencil className="size-3.5" />
                        <span>Modificar</span>
                      </Button>
                    )}
                    {onEliminar && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-8 gap-1 text-xs text-destructive hover:bg-destructive/10 touch-manipulation"
                        onClick={(ev) => {
                          ev.stopPropagation();
                          onEliminar(e);
                        }}
                        aria-label={`Eliminar ${nombre}`}
                      >
                        <Trash2 className="size-3.5" />
                        <span>Eliminar</span>
                      </Button>
                    )}
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      {totalPaginas > 1 && (
        <nav aria-label="Paginación de entrenamientos" className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            Mostrando página <strong className="text-foreground">{pagina + 1}</strong> de{" "}
            <strong className="text-foreground">{totalPaginas}</strong> ({totalElementos} en total)
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={pagina <= 0}
              onClick={() => onCambiarPagina(pagina - 1)}
              aria-label="Ir a la página anterior"
            >
              <ChevronLeft className="size-4" />
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={pagina >= totalPaginas - 1}
              onClick={() => onCambiarPagina(pagina + 1)}
              aria-label="Ir a la página siguiente"
            >
              Siguiente
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </nav>
      )}
    </div>
  );
}
