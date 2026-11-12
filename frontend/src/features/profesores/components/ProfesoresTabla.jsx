import { ChevronLeft, ChevronRight, Eye, Pencil, Printer, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { dniFormateado, nombreCompleto } from "../model/profesor";
import { ActivoBadge, ValidacionBadge } from "@/components/ui/estado-badges";

/** Las cuatro acciones de fila, con el nombre de la persona en cada etiqueta. */
function AccionesDeFila({ profesor, onVer, onEditar, onImprimir, onEliminar }) {
  const nombre = nombreCompleto(profesor);
  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    { icono: Pencil, texto: "Modificar a", alAccionar: onEditar },
    { icono: Printer, texto: "Imprimir la ficha de", alAccionar: onImprimir },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button key={texto} variant="ghost" size="icon-sm" aria-label={`${texto} ${nombre}`} onClick={() => alAccionar(profesor)}>
          <Icono />
        </Button>
      ))}
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-destructive hover:bg-destructive/10"
        aria-label={`Eliminar a ${nombre}`}
        onClick={() => onEliminar(profesor)}
      >
        <Trash2 />
      </Button>
    </div>
  );
}

/** Nombre con el correo debajo, como en el mockup. */
function CeldaPersona({ profesor }) {
  return (
    <div className="min-w-0">
      <p className="truncate font-medium">{nombreCompleto(profesor)}</p>
      <p className="truncate text-xs text-muted-foreground">{profesor.email}</p>
    </div>
  );
}

// Listado de profesores (CU22): tabla desde md hacia arriba y tarjetas en teléfono, mismo criterio que InsumosTabla/CanchasTabla.
export function ProfesoresTabla({ profesores, pagina = 0, totalPaginas = 1, totalElementos = 0, onCambiarPagina, onVer, onEditar, onImprimir, onEliminar }) {
  const acciones = { onVer, onEditar, onImprimir, onEliminar };

  return (
    <div className="space-y-4">
      {/* Tabla: escritorio y tablet */}
      <div className="hidden md:block overflow-hidden rounded-xl border border-border bg-card shadow-xs">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Profesor</TableHead>
              <TableHead className="hidden lg:table-cell">DNI</TableHead>
              <TableHead>Título habilitante</TableHead>
              <TableHead>Certificación</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {profesores.map((profesor) => (
              <TableRow key={profesor.id}>
                <TableCell>
                  <CeldaPersona profesor={profesor} />
                </TableCell>
                <TableCell className="hidden lg:table-cell tabular-nums">{dniFormateado(profesor.dni)}</TableCell>
                <TableCell className="max-w-[240px]">
                  {profesor.tituloHabilitante ? (
                    <span className="block truncate text-sm">{profesor.tituloHabilitante}</span>
                  ) : (
                    <span className="text-sm text-muted-foreground">Sin cargar</span>
                  )}
                </TableCell>
                <TableCell>
                  <ValidacionBadge persona={profesor} />
                </TableCell>
                <TableCell>
                  <ActivoBadge persona={profesor} />
                </TableCell>
                <TableCell>
                  <AccionesDeFila profesor={profesor} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Tarjetas: teléfono */}
      <ul className="space-y-3 md:hidden">
        {profesores.map((profesor) => (
          <li key={profesor.id} className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <CeldaPersona profesor={profesor} />
              <ActivoBadge persona={profesor} className="shrink-0" />
            </div>

            <dl className="mt-3 space-y-1.5 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">DNI</dt>
                <dd className="tabular-nums">{dniFormateado(profesor.dni)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Título</dt>
                <dd className="min-w-0 truncate text-right">{profesor.tituloHabilitante || "Sin cargar"}</dd>
              </div>
            </dl>

            <div className="mt-3">
              <ValidacionBadge persona={profesor} />
            </div>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila profesor={profesor} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {/* Barra de Paginación */}
      {totalPaginas > 1 && (
        <nav aria-label="Paginación de profesores" className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            Mostrando página <strong className="text-foreground">{pagina + 1}</strong> de{" "}
            <strong className="text-foreground">{totalPaginas}</strong> ({totalElementos} en total)
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={pagina <= 0} onClick={() => onCambiarPagina(pagina - 1)} aria-label="Ir a la página anterior">
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
