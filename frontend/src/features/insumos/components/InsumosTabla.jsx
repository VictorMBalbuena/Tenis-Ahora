import {
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Eye,
  Pencil,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { stockTotal } from "../model/insumo";
import { BarraStock } from "./BarraStock";
import { EstadoStockBadge } from "./EstadoStockBadge";
import { CategoriaInsumoBadge } from "./CategoriaInsumoBadge";

/**
 * Listado de insumos (CU102).
 *
 * Dos presentaciones del mismo dato: tabla desde md hacia arriba y tarjetas
 * en teléfono, porque seis columnas no entran en 390px sin recortar texto.
 */

/** Las cuatro acciones de fila, con el nombre del insumo en cada etiqueta. */
function AccionesDeFila({ insumo, onVer, onEditar, onMovimiento, onEliminar }) {
  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    { icono: Pencil, texto: "Modificar", alAccionar: onEditar },
    {
      icono: ArrowUpDown,
      texto: "Reponer o descontar stock de",
      alAccionar: onMovimiento,
    },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button
          key={texto}
          variant="ghost"
          size="icon-sm"
          aria-label={`${texto} ${insumo.nombre}`}
          onClick={() => alAccionar(insumo)}
        >
          <Icono />
        </Button>
      ))}
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-destructive hover:bg-destructive/10"
        aria-label={`Eliminar ${insumo.nombre}`}
        onClick={() => onEliminar(insumo)}
      >
        <Trash2 />
      </Button>
    </div>
  );
}

export function InsumosTabla({
  insumos,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onEditar,
  onMovimiento,
  onEliminar,
}) {
  const acciones = { onVer, onEditar, onMovimiento, onEliminar };

  return (
    <div className="space-y-4">
      {/* Vista de Escritorio y Tablets */}
      <div className="hidden overflow-hidden rounded-xl border border-border bg-card shadow-xs md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[26%]">Insumo</TableHead>
              <TableHead className="hidden w-[14%] lg:table-cell">
                Categoría
              </TableHead>
              <TableHead className="w-[20%]">Disponible / total</TableHead>
              <TableHead className="w-[10%] text-right">En uso</TableHead>
              <TableHead className="hidden w-[10%] text-right lg:table-cell">
                Mínimo
              </TableHead>
              <TableHead className="w-[12%]">Estado</TableHead>
              <TableHead className="w-[18%] text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {insumos.map((insumo) => (
              <TableRow key={insumo.id}>
                <TableCell className="font-medium">{insumo.nombre}</TableCell>
                <TableCell className="hidden lg:table-cell">
                  <CategoriaInsumoBadge categoria={insumo.categoria} />
                </TableCell>
                <TableCell>
                  <BarraStock insumo={insumo} />
                </TableCell>
                <TableCell className="text-right font-mono text-sm tabular-nums">
                  {insumo.enUso}
                </TableCell>
                <TableCell className="hidden text-right font-mono text-sm tabular-nums text-muted-foreground lg:table-cell">
                  {insumo.cantidadMinima}
                </TableCell>
                <TableCell>
                  <EstadoStockBadge insumo={insumo} />
                </TableCell>
                <TableCell className="text-right">
                  <AccionesDeFila insumo={insumo} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista de Teléfono */}
      <ul className="space-y-3 md:hidden">
        {insumos.map((insumo) => (
          <li
            key={insumo.id}
            className="rounded-xl border border-border bg-card p-4 shadow-xs"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate font-medium">{insumo.nombre}</p>
                <div className="mt-0.5">
                  <CategoriaInsumoBadge categoria={insumo.categoria} />
                </div>
              </div>
              <EstadoStockBadge insumo={insumo} className="shrink-0" />
            </div>

            <dl className="mt-3 grid grid-cols-1 gap-y-1.5 text-sm">
              {[
                ["Disponible", insumo.cantidadDisponible],
                ["En uso", insumo.enUso],
                ["Mínimo", insumo.cantidadMinima],
                ["Total", stockTotal(insumo)],
              ].map(([etiqueta, valor]) => (
                <div key={etiqueta} className="flex justify-between gap-2">
                  <dt className="text-muted-foreground">{etiqueta}</dt>
                  <dd className="font-mono tabular-nums">{valor}</dd>
                </div>
              ))}
            </dl>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila insumo={insumo} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {/* Barra de Paginación */}
      {totalPaginas > 1 && (
        <nav
          aria-label="Paginación de insumos"
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
