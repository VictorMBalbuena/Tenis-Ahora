import { ChevronLeft, ChevronRight, ClipboardCheck, Eye, Pencil, Printer, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import {
  ETIQUETA_MODALIDAD,
  ETIQUETA_NIVEL,
  cupoCompleto,
  diasCortos,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreProfesor,
  porcentajeOcupado,
  vigencia,
} from "../model/clase";

/** Ocupación del cupo: números arriba y barra como refuerzo visual (CU70). */
export function CupoBadge({ clase }) {
  const completo = cupoCompleto(clase);
  const libres = lugaresLibres(clase);

  return (
    <div className="min-w-[110px] space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="font-medium tabular-nums">{clase.cupoOcupado ?? 0}</span>
        <span className="text-muted-foreground tabular-nums">/ {clase.cupoMaximo}</span>
      </div>
      <div aria-hidden="true" className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={cn("h-full rounded-full transition-[width]", completo ? "bg-red-500" : "bg-primary")}
          style={{ width: `${porcentajeOcupado(clase)}%` }}
        />
      </div>
      <p className={cn("text-[11px]", completo ? "text-red-600 font-medium" : "text-muted-foreground")}>
        {completo ? "Cupo completo" : `${libres} ${libres === 1 ? "lugar" : "lugares"}`}
      </p>
    </div>
  );
}

function AccionesDeFila({ clase, onVer, onEditar, onAsistencia, onImprimir, onEliminar }) {
  const nombre = clase.nombre;
  
  // Solo agregamos la acción a la lista si la función no es null
  const acciones = [
    onVer && { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    onAsistencia && { icono: ClipboardCheck, texto: "Registrar la asistencia de", alAccionar: onAsistencia },
    onEditar && { icono: Pencil, texto: "Modificar", alAccionar: onEditar },
    onImprimir && { icono: Printer, texto: "Imprimir", alAccionar: onImprimir },
  ].filter(Boolean); // Este filter saca los nulls del array

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button
          key={texto}
          variant="ghost"
          size="icon-sm"
          aria-label={`${texto} ${nombre}`}
          onClick={() => alAccionar(clase)}
        >
          <Icono />
        </Button>
      ))}
      
      {/* El botón de eliminar solo se renderiza si onEliminar no es null */}
      {onEliminar && (
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-destructive hover:bg-destructive/10"
          aria-label={`Eliminar ${nombre}`}
          onClick={() => onEliminar(clase)}
        >
          <Trash2 />
        </Button>
      )}
    </div>
  );
}

/** Listado de clases (CU34): tabla desde md hacia arriba y tarjetas en teléfono. */
export function ClasesTabla({
  clases,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  esProfesor = false,
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
      {/* Vista de escritorio y tablets */}
      <div className="hidden overflow-hidden rounded-xl border border-border bg-card shadow-xs md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[14%]">Clase</TableHead>
              <TableHead className="w-[16%]">Vigencia y horario</TableHead>
              {!esProfesor && <TableHead className="w-[16%]">Profesor</TableHead>}
              <TableHead className="hidden w-[10%] lg:table-cell">Cancha</TableHead>
              <TableHead className="w-[10%]">Nivel</TableHead>
              <TableHead className="hidden w-[10%] lg:table-cell">Modalidad</TableHead>
              <TableHead className="w-[14%]">Cupo</TableHead>
              <TableHead className="w-[10%] text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {clases.map((clase) => (
              <TableRow key={clase.id}>
                <TableCell className="min-w-0 truncate font-medium">{clase.nombre}</TableCell>
                <TableCell>
                  <p className="text-xs">{vigencia(clase)}</p>
                  <p className="text-xs text-muted-foreground">{diasCortos(clase)} · {horario(clase)}</p>
                </TableCell>
                {!esProfesor && <TableCell>{nombreProfesor(clase)}</TableCell>}
                <TableCell className="hidden lg:table-cell">{nombreCancha(clase)}</TableCell>
                <TableCell>
                  <Badge variant="outline" className="font-normal">
                    {ETIQUETA_NIVEL[clase.nivel] ?? clase.nivel}
                  </Badge>
                </TableCell>
                <TableCell className="hidden lg:table-cell text-sm text-muted-foreground">
                  {ETIQUETA_MODALIDAD[clase.modalidad] ?? clase.modalidad}
                </TableCell>
                <TableCell>
                  <CupoBadge clase={clase} />
                </TableCell>
                <TableCell className="text-right">
                  <AccionesDeFila clase={clase} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista de teléfono */}
      <ul className="space-y-3 md:hidden">
        {clases.map((clase) => (
          <li key={clase.id} className="rounded-xl border border-border bg-card p-4 shadow-xs">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate font-medium">{clase.nombre}</p>
                <p className="text-xs text-muted-foreground">{vigencia(clase)}</p>
                <p className="text-xs text-muted-foreground">{diasCortos(clase)} · {horario(clase)}</p>
              </div>
              <Badge variant="outline" className="shrink-0 font-normal">
                {ETIQUETA_NIVEL[clase.nivel] ?? clase.nivel}
              </Badge>
            </div>

            <dl className="mt-3 space-y-1.5 text-sm">
              {!esProfesor && (
                <div className="flex justify-between gap-3">
                  <dt className="text-muted-foreground">Profesor</dt>
                  <dd className="min-w-0 truncate text-right">{nombreProfesor(clase)}</dd>
                </div>
              )}
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Cancha</dt>
                <dd>{nombreCancha(clase)}</dd>
              </div>
            </dl>

            <div className="mt-3">
              <CupoBadge clase={clase} />
            </div>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila clase={clase} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {/* Barra de paginación */}
      {totalPaginas > 1 && (
        <nav aria-label="Paginación de clases" className="flex flex-wrap items-center justify-between gap-3">
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