import { ChevronLeft, ChevronRight, Eye, Printer, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { conceptoLegible, estaAnulado, fechaHora, medioLegible, numero, pesos } from "../model/recibo";

function AccionesDeFila({ recibo, onVer, onImprimir, onAnular }) {
  const cual = `el recibo ${numero(recibo)}`;
  const anulado = estaAnulado(recibo);

  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer, visible: true },
    { icono: Printer, texto: "Imprimir", alAccionar: onImprimir, visible: true },
  ];

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones
        .filter((a) => a.visible)
        .map(({ icono: Icono, texto, alAccionar }) => (
          <Button key={texto} variant="ghost" size="icon-sm" aria-label={`${texto} ${cual}`} onClick={() => alAccionar(recibo)}>
            <Icono />
          </Button>
        ))}
      {!anulado && (
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-destructive hover:bg-destructive/10"
          aria-label={`Anular ${cual}`}
          onClick={() => onAnular(recibo)}
        >
          <Trash2 />
        </Button>
      )}
    </div>
  );
}

function CeldaNumero({ recibo }) {
  return (
    <div className="min-w-0">
      <p className="truncate font-medium tabular-nums">{numero(recibo)}</p>
      <p className="truncate text-xs text-muted-foreground">{fechaHora(recibo.fechaEmision)}</p>
    </div>
  );
}

// Listado de recibos (CU59): tabla desde md hacia arriba y tarjetas en teléfono.
export function RecibosTabla({
  recibos,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onImprimir,
  onAnular,
}) {
  const acciones = { onVer, onImprimir, onAnular };

  return (
    <div className="space-y-4">
      <div className="hidden md:block overflow-hidden rounded-xl border border-border bg-card shadow-xs">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>N° de recibo</TableHead>
              <TableHead>Concepto</TableHead>
              <TableHead className="hidden lg:table-cell">Medio de pago</TableHead>
              <TableHead className="text-right">Monto</TableHead>
              <TableHead className="text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {recibos.map((recibo) => (
              <TableRow key={recibo.id}>
                <TableCell className="max-w-[220px]">
                  <CeldaNumero recibo={recibo} />
                </TableCell>
                <TableCell className="max-w-[240px]">
                  <span className="block truncate text-sm">{conceptoLegible(recibo)}</span>
                  {estaAnulado(recibo) && (
                    <Badge className="mt-1 bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]">Anulado</Badge>
                  )}
                </TableCell>
                <TableCell className="hidden lg:table-cell text-sm">{medioLegible(recibo)}</TableCell>
                <TableCell className="text-right font-semibold tabular-nums">{pesos(recibo.monto)}</TableCell>
                <TableCell>
                  <AccionesDeFila recibo={recibo} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <ul className="space-y-3 md:hidden">
        {recibos.map((recibo) => (
          <li key={recibo.id} className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <CeldaNumero recibo={recibo} />
              {estaAnulado(recibo) && (
                <Badge className="shrink-0 bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]">Anulado</Badge>
              )}
            </div>

            <dl className="mt-3 space-y-1.5 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Concepto</dt>
                <dd className="min-w-0 truncate text-right">{conceptoLegible(recibo)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Medio de pago</dt>
                <dd>{medioLegible(recibo)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Monto</dt>
                <dd className="font-semibold tabular-nums">{pesos(recibo.monto)}</dd>
              </div>
            </dl>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila recibo={recibo} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {totalPaginas > 1 && (
        <nav aria-label="Paginación de recibos" className="flex flex-wrap items-center justify-between gap-3">
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
