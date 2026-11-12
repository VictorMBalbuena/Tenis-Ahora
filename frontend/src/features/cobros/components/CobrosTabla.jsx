import { Ban, ChevronLeft, ChevronRight, Eye, Printer, ReceiptText } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { EstadoCobro, descripcionOrigen, esAnulable, esReembolso, fechaHora, pesos } from "../model/cobro";
import { ConceptoCobroBadge } from "./ConceptoCobroBadge";
import { EstadoCobroBadge } from "./EstadoCobroBadge";
import { MedioPagoBadge } from "./MedioPagoBadge";

function AccionesDeFila({ cobro, onVer, onImprimir, onEmitirRecibo, onAnular }) {
  const cual = `el cobro #${cobro.id}`;
  const acciones = [
    { icono: Eye, texto: "Ver el detalle de", alAccionar: onVer },
    { icono: Printer, texto: "Imprimir", alAccionar: onImprimir },
  ].filter(({ alAccionar }) => alAccionar);

  return (
    <div className="flex items-center justify-end gap-0.5">
      {acciones.map(({ icono: Icono, texto, alAccionar }) => (
        <Button key={texto} variant="ghost" size="icon-sm" aria-label={`${texto} ${cual}`} onClick={() => alAccionar(cobro)}>
          <Icono />
        </Button>
      ))}
      {onEmitirRecibo && cobro.estado === EstadoCobro.PAGADO && !cobro.recibo && (
        <Button variant="ghost" size="icon-sm" aria-label={`Emitir el recibo de ${cual}`} onClick={() => onEmitirRecibo(cobro)}>
          <ReceiptText />
        </Button>
      )}
      {onAnular && esAnulable(cobro) && (
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-destructive hover:bg-destructive/10"
          aria-label={`Anular ${cual}`}
          onClick={() => onAnular(cobro)}
        >
          <Ban />
        </Button>
      )}
    </div>
  );
}

function CeldaMonto({ cobro }) {
  const signo = esReembolso(cobro) ? "- " : "";
  return (
    <span className={`font-semibold tabular-nums ${esReembolso(cobro) ? "text-[var(--estado-error-fg)]" : ""}`}>
      {signo}
      {pesos(cobro.montoFinal)}
    </span>
  );
}

function CeldaOrigen({ cobro }) {
  return (
    <div className="min-w-0">
      <p className="truncate font-medium">{descripcionOrigen(cobro)}</p>
      <p className="truncate text-xs text-muted-foreground">{fechaHora(cobro.fecha)}</p>
    </div>
  );
}

// Listado de cobros (CU54): tabla desde md hacia arriba y tarjetas en teléfono, mismo criterio que EntrenadoresTabla.
export function CobrosTabla({
  cobros,
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onImprimir,
  onEmitirRecibo,
  onAnular,
}) {
  const acciones = { onVer, onImprimir, onEmitirRecibo, onAnular };

  return (
    <div className="space-y-4">
      <div className="hidden md:block overflow-hidden rounded-xl border border-border bg-card shadow-xs">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Se cobró por</TableHead>
              <TableHead>Concepto</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="hidden lg:table-cell">Medio de pago</TableHead>
              <TableHead className="text-right">Monto</TableHead>
              <TableHead className="hidden lg:table-cell">Recibo</TableHead>
              <TableHead className="text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {cobros.map((cobro) => (
              <TableRow key={cobro.id}>
                <TableCell className="max-w-[260px]">
                  <CeldaOrigen cobro={cobro} />
                </TableCell>
                <TableCell>
                  <ConceptoCobroBadge concepto={cobro.concepto} />
                </TableCell>
                <TableCell>
                  <EstadoCobroBadge estado={cobro.estado} />
                </TableCell>
                <TableCell className="hidden lg:table-cell">
                  <MedioPagoBadge medioPago={cobro.medioPago} />
                </TableCell>
                <TableCell className="text-right">
                  <CeldaMonto cobro={cobro} />
                </TableCell>
                <TableCell className="hidden lg:table-cell text-sm text-muted-foreground tabular-nums">
                  {cobro.recibo?.numeroRecibo || "—"}
                </TableCell>
                <TableCell>
                  <AccionesDeFila cobro={cobro} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <ul className="space-y-3 md:hidden">
        {cobros.map((cobro) => (
          <li key={cobro.id} className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <CeldaOrigen cobro={cobro} />
              <ConceptoCobroBadge concepto={cobro.concepto} className="shrink-0" />
            </div>

            <dl className="mt-3 space-y-1.5 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Estado</dt>
                <dd>
                  <EstadoCobroBadge estado={cobro.estado} />
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Medio de pago</dt>
                <dd>
                  <MedioPagoBadge medioPago={cobro.medioPago} />
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Monto</dt>
                <dd>
                  <CeldaMonto cobro={cobro} />
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">Recibo</dt>
                <dd className="tabular-nums">{cobro.recibo?.numeroRecibo || "—"}</dd>
              </div>
            </dl>

            <div className="mt-3 border-t border-border pt-2">
              <AccionesDeFila cobro={cobro} {...acciones} />
            </div>
          </li>
        ))}
      </ul>

      {totalPaginas > 1 && (
        <nav aria-label="Paginación de cobros" className="flex flex-wrap items-center justify-between gap-3">
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
