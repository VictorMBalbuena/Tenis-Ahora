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
import { dniFormateado, nombreCompleto } from "../model/entrenador";
import { ActivoBadge, ValidacionBadge } from "@/components/ui/estado-badges";

function AccionesDeFila({ entrenador, onVer, onEditar, onImprimir, onEliminar }) {
  const nombre = nombreCompleto(entrenador);
  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    { icono: Pencil, texto: "Modificar a", alAccionar: onEditar },
    { icono: Printer, texto: "Imprimir la ficha de", alAccionar: onImprimir },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button key={texto} variant="ghost" size="icon-sm" aria-label={`${texto} ${nombre}`} onClick={() => alAccionar(entrenador)}>
          <Icono />
        </Button>
      ))}
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-destructive hover:bg-destructive/10"
        aria-label={`Eliminar a ${nombre}`}
        onClick={() => onEliminar(entrenador)}
      >
        <Trash2 />
      </Button>
    </div>
  );
}

function AccionesTarjetaMobile({ entrenador, onVer, onEditar, onImprimir, onEliminar }) {
  const nombre = nombreCompleto(entrenador);
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 w-full pt-1">
      <Button
        variant="outline"
        size="sm"
        className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
        aria-label={`Ver el detalle de ${nombre}`}
        title={`Ver el detalle de ${nombre}`}
        onClick={() => onVer(entrenador)}
      >
        <Eye className="size-4 shrink-0 text-muted-foreground" />
        <span>Ver</span>
      </Button>
      <Button
        variant="outline"
        size="sm"
        className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
        aria-label={`Modificar a ${nombre}`}
        title={`Modificar a ${nombre}`}
        onClick={() => onEditar(entrenador)}
      >
        <Pencil className="size-4 shrink-0 text-muted-foreground" />
        <span>Editar</span>
      </Button>
      <Button
        variant="outline"
        size="sm"
        className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
        aria-label={`Imprimir la ficha de ${nombre}`}
        title={`Imprimir la ficha de ${nombre}`}
        onClick={() => onImprimir(entrenador)}
      >
        <Printer className="size-4 shrink-0 text-muted-foreground" />
        <span>Ficha</span>
      </Button>
      <Button
        variant="outline"
        size="sm"
        className="min-h-[44px] justify-center gap-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 hover:text-destructive border-destructive/20"
        aria-label={`Eliminar a ${nombre}`}
        title={`Eliminar a ${nombre}`}
        onClick={() => onEliminar(entrenador)}
      >
        <Trash2 className="size-4 shrink-0" />
        <span>Baja</span>
      </Button>
    </div>
  );
}

function CeldaPersona({ entrenador }) {
  return (
    <div className="min-w-0">
      <p className="truncate font-medium">{nombreCompleto(entrenador)}</p>
      <p className="truncate text-xs text-muted-foreground">{entrenador.email}</p>
    </div>
  );
}

// Listado de entrenadores (CU27): tabla desde md hacia arriba y tarjetas en teléfono, mismo criterio que ProfesoresTabla.
export function EntrenadoresTabla({ entrenadores, pagina = 0, totalPaginas = 1, totalElementos = 0, onCambiarPagina, onVer, onEditar, onImprimir, onEliminar }) {
  const acciones = { onVer, onEditar, onImprimir, onEliminar };

  return (
    <div className="space-y-4">
      <div className="hidden md:block overflow-hidden rounded-xl border border-border bg-card shadow-xs">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Entrenador</TableHead>
              <TableHead className="hidden lg:table-cell">DNI</TableHead>
              <TableHead>Especialidad</TableHead>
              <TableHead>Certificación</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {entrenadores.map((entrenador) => (
              <TableRow key={entrenador.id}>
                <TableCell>
                  <CeldaPersona entrenador={entrenador} />
                </TableCell>
                <TableCell className="hidden lg:table-cell tabular-nums">{dniFormateado(entrenador.dni)}</TableCell>
                <TableCell className="max-w-[200px]">
                  <span className="block truncate text-sm">{entrenador.especialidad || "—"}</span>
                </TableCell>
                <TableCell>
                  <ValidacionBadge persona={entrenador} />
                </TableCell>
                <TableCell>
                  <ActivoBadge persona={entrenador} />
                </TableCell>
                <TableCell>
                  <AccionesDeFila entrenador={entrenador} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <ul className="space-y-3 md:hidden">
        {entrenadores.map((entrenador) => (
          <li key={entrenador.id} className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <CeldaPersona entrenador={entrenador} />
              <ActivoBadge persona={entrenador} className="shrink-0" />
            </div>

            <dl className="mt-3 space-y-1.5 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">DNI</dt>
                <dd className="tabular-nums">{dniFormateado(entrenador.dni)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Especialidad</dt>
                <dd className="min-w-0 truncate text-right">{entrenador.especialidad || "—"}</dd>
              </div>
            </dl>

            <div className="mt-3">
              <ValidacionBadge persona={entrenador} />
            </div>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesTarjetaMobile entrenador={entrenador} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {totalPaginas > 1 && (
        <nav aria-label="Paginación de entrenadores" className="flex flex-wrap items-center justify-between gap-3">
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
