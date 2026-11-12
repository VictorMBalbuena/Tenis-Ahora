import { ChevronLeft, ChevronRight, Eye, Pencil, Printer, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "cn";
import {
  ETIQUETA_CALCULO,
  beneficio,
  etiqueta,
  formaDeCalculo,
} from "../model/descuento";

/**
 * Listado de descuentos (CU64).
 *
 * Tabla desde md hacia arriba y tarjetas en teléfono. No hay columna de
 * nombre porque la entidad no tiene ese campo: lo que identifica a un
 * descuento es su tipo, y las condiciones lo explican.
 */

export function EstadoBadge({ activo, className }) {
  return (
    <Badge
      className={cn(
        activo
          ? "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
          : "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
        className,
      )}
    >
      {activo ? "Activo" : "Inactivo"}
    </Badge>
  );
}

/** Las cuatro acciones de fila, con el descuento nombrado en cada etiqueta. */
function AccionesDeFila({ descuento, onVer, onEditar, onImprimir, onEliminar }) {
  const nombre = etiqueta(descuento);
  const acciones = [
    { icono: Eye, texto: "Ver el detalle del descuento", alAccionar: onVer },
    { icono: Pencil, texto: "Modificar el descuento", alAccionar: onEditar },
    { icono: Printer, texto: "Imprimir el descuento", alAccionar: onImprimir },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button
          key={texto}
          variant="ghost"
          size="icon-sm"
          aria-label={`${texto} ${nombre}`}
          onClick={() => alAccionar(descuento)}
        >
          <Icono />
        </Button>
      ))}
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-destructive hover:bg-destructive/10"
        aria-label={`Eliminar el descuento ${nombre}`}
        onClick={() => onEliminar(descuento)}
      >
        <Trash2 />
      </Button>
    </div>
  );
}

export function DescuentosTabla({
  descuentos,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onEditar,
  onImprimir,
  onEliminar,
}) {
  const acciones = { onVer, onEditar, onImprimir, onEliminar };

  return (
    <div className="space-y-4">
      {/* Vista de Escritorio y Tablets */}
      <div className="hidden overflow-hidden rounded-xl border border-border bg-card shadow-xs md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[20%]">Descuento</TableHead>
              <TableHead className="w-[14%]">Beneficio</TableHead>
              <TableHead className="hidden w-[14%] lg:table-cell">
                Forma
              </TableHead>
              <TableHead className="w-[26%]">Condiciones</TableHead>
              <TableHead className="w-[10%]">Estado</TableHead>
              <TableHead className="w-[16%] text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {descuentos.map((descuento) => {
              const forma = formaDeCalculo(descuento);
              return (
                <TableRow key={descuento.id}>
                  <TableCell className="font-medium">
                    {etiqueta(descuento)}
                  </TableCell>
                  <TableCell
                    className={cn(
                      "font-mono text-sm tabular-nums",
                      forma
                        ? "font-semibold text-[var(--estado-ok-fg)]"
                        : "text-muted-foreground",
                    )}
                  >
                    {beneficio(descuento)}
                  </TableCell>
                  <TableCell className="hidden lg:table-cell">
                    {forma ? (
                      <Badge variant="outline" className="font-normal">
                        {ETIQUETA_CALCULO[forma]}
                      </Badge>
                    ) : (
                      <span className="text-sm text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  <TableCell className="max-w-[280px]">
                    <span className="line-clamp-2 text-sm text-muted-foreground">
                      {descuento.condiciones || "Sin condiciones cargadas"}
                    </span>
                  </TableCell>
                  <TableCell>
                    <EstadoBadge activo={descuento.activo} />
                  </TableCell>
                  <TableCell className="text-right">
                    <AccionesDeFila descuento={descuento} {...acciones} />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {/* Vista de Teléfono */}
      <ul className="space-y-3 md:hidden">
        {descuentos.map((descuento) => (
          <li
            key={descuento.id}
            className="rounded-xl border border-border bg-card p-4 shadow-xs"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate font-medium">{etiqueta(descuento)}</p>
                <p className="font-mono text-sm font-semibold tabular-nums text-[var(--estado-ok-fg)]">
                  {beneficio(descuento)}
                </p>
              </div>
              <EstadoBadge activo={descuento.activo} className="shrink-0" />
            </div>

            <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">
              {descuento.condiciones || "Sin condiciones cargadas"}
            </p>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila descuento={descuento} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {/* Barra de Paginación */}
      {totalPaginas > 1 && (
        <nav
          aria-label="Paginación de descuentos"
          className="flex flex-wrap items-center justify-between gap-3"
        >
          <p className="text-sm text-muted-foreground">
            Mostrando página{" "}
            <strong className="text-foreground">{pagina + 1}</strong> de{" "}
            <strong className="text-foreground">{totalPaginas}</strong> (
            {totalElementos} en total)
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
