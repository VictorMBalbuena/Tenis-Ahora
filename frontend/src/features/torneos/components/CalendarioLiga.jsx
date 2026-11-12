import { useMemo } from "react";
import { CalendarDays, Pencil } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

/**
 * El calendario de una liga, agrupado por fecha.
 *
 * No se dibuja como un cuadro porque una liga no tiene cuadro: nadie queda eliminado y todas las fechas se
 * juegan igual. Con una cantidad impar de participantes, en cada fecha hay uno que descansa, y por eso una
 * fecha puede tener un partido menos que otra.
 */
export function CalendarioLiga({ partidos = [], esAdmin = false, onCargarResultado }) {
  const fechas = useMemo(() => {
    const agrupado = new Map();
    partidos.forEach((partido) => {
      const numero = partido.ronda ?? 1;
      if (!agrupado.has(numero)) {
        agrupado.set(numero, []);
      }
      agrupado.get(numero).push(partido);
    });
    return [...agrupado.entries()]
      .sort(([a], [b]) => a - b)
      .map(([numero, suyos]) => ({
        numero,
        partidos: [...suyos].sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0)),
      }));
  }, [partidos]);

  if (fechas.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-border p-8 text-center">
        <p className="text-sm text-muted-foreground">
          El calendario todavía no está generado. Cuando cierre la inscripción, generalo desde el detalle de la liga.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {fechas.map((fecha) => (
        <section key={fecha.numero} className="rounded-xl border border-border bg-card shadow-sm">
          <header className="flex items-center gap-2 border-b border-border px-4 py-3">
            <CalendarDays className="size-4 text-muted-foreground" />
            <h3 className="text-sm font-bold">Fecha {fecha.numero}</h3>
            <span className="text-xs text-muted-foreground">
              {fecha.partidos.length} {fecha.partidos.length === 1 ? "partido" : "partidos"}
            </span>
          </header>

          <ul className="divide-y divide-border">
            {fecha.partidos.map((partido) => (
              <li key={partido.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                <span className="min-w-0 flex-1 text-sm">
                  <span className="font-medium">{partido.participanteANombre ?? "A definir"}</span>
                  <span className="mx-2 text-muted-foreground">vs</span>
                  <span className="font-medium">{partido.participanteBNombre ?? "A definir"}</span>
                </span>

                {partido.resultado ? (
                  <span className="flex items-center gap-2">
                    <span className="font-mono text-xs tabular-nums text-muted-foreground">{partido.sets}</span>
                    <Badge variant="secondary">{partido.resultado}</Badge>
                  </span>
                ) : (
                  <Badge variant="outline">Pendiente</Badge>
                )}

                {esAdmin && !partido.resultado && (
                  <Button variant="ghost" size="sm" onClick={() => onCargarResultado(partido)}>
                    <Pencil className="size-3.5" /> Cargar resultado
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
