import { useMemo } from "react";
import { Check, Trophy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { validarTanteadorTenis } from "../model/validadorTenis";

function obtenerNombreRonda(ronda, maxRonda) {
  if (ronda === maxRonda) return "Final";
  if (ronda === maxRonda - 1) return "Semifinal";
  if (ronda === maxRonda - 2) return "Cuartos de final";
  if (ronda === maxRonda - 3) return "Octavos de final";
  return `Ronda ${ronda}`;
}

export function FixtureTree({ partidos = [], esAdmin = false, onCargarResultado }) {
  const { rondasAgrupadas } = useMemo(() => {
    if (!partidos.length) return { rondasAgrupadas: [], maxRonda: 0 };

    const max = Math.max(...partidos.map((p) => p.ronda));
    const agrupado = {};

    for (const p of partidos) {
      if (!agrupado[p.ronda]) {
        agrupado[p.ronda] = [];
      }
      agrupado[p.ronda].push(p);
    }

    // Ordenar partidos dentro de cada ronda por orden o id
    Object.keys(agrupado).forEach((r) => {
      agrupado[r].sort((a, b) => (a.orden ?? a.id) - (b.orden ?? b.id));
    });

    const rondasKeys = Object.keys(agrupado)
      .map(Number)
      .sort((a, b) => a - b);

    return {
      rondasAgrupadas: rondasKeys.map((r) => ({
        ronda: r,
        titulo: obtenerNombreRonda(r, max),
        partidos: agrupado[r],
      })),
      maxRonda: max,
    };
  }, [partidos]);

  if (!partidos.length) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
        Aún no se ha generado el cuadro para este torneo.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto pb-4">
      <div className="flex gap-6 min-w-max items-start">
        {rondasAgrupadas.map(({ ronda, titulo, partidos: partidosRonda }) => (
          <div key={ronda} className="w-72 shrink-0 flex flex-col gap-4">
            <div className="flex items-center gap-2 pb-1 border-b border-border">
              <Trophy className="size-4 text-primary" />
              <h3 className="font-bold text-sm tracking-tight">{titulo}</h3>
              <span className="text-xs text-muted-foreground ml-auto">
                {partidosRonda.length} {partidosRonda.length === 1 ? "partido" : "partidos"}
              </span>
            </div>

            <div className="flex flex-col justify-around gap-4 h-full">
              {partidosRonda.map((p) => {
                const nombre1 =
                  p.participanteANombre ||
                  p.participante1?.nombre ||
                  p.participante1?.nombreEquipo ||
                  p.nombreJugador1 ||
                  "Por definir";
                const nombre2 =
                  p.participanteBNombre ||
                  p.participante2?.nombre ||
                  p.participante2?.nombreEquipo ||
                  p.nombreJugador2 ||
                  "Por definir";

                const tieneAmbosParticipantes = Boolean(
                  (p.participanteANombre && p.participanteBNombre) ||
                  (p.participanteAId && p.participanteBId) ||
                  (p.participante1 && p.participante2)
                );
                const pendiente = !p.sets;

                const valSets = p.sets ? validarTanteadorTenis(p.sets) : null;
                const ganadorEs1 =
                  p.ganadorId != null
                    ? (p.participante1?.id === p.ganadorId || p.participanteAId === p.ganadorId)
                    : (p.ganador === 1 || valSets?.ganador === 1);
                const ganadorEs2 =
                  p.ganadorId != null
                    ? (p.participante2?.id === p.ganadorId || p.participanteBId === p.ganadorId)
                    : (p.ganador === 2 || valSets?.ganador === 2);

                return (
                  <div
                    key={p.id}
                    className="rounded-xl border border-border bg-card shadow-sm overflow-hidden text-sm"
                  >
                    <div
                      className={`flex items-center justify-between gap-2 px-3 py-2 border-b border-border ${
                        ganadorEs1 ? "bg-primary/10 font-bold text-primary" : ""
                      }`}
                    >
                      <span className="truncate">{nombre1}</span>
                      {ganadorEs1 && <Check className="size-3.5 shrink-0" />}
                    </div>

                    <div
                      className={`flex items-center justify-between gap-2 px-3 py-2 ${
                        ganadorEs2 ? "bg-primary/10 font-bold text-primary" : ""
                      }`}
                    >
                      <span className="truncate">{nombre2}</span>
                      {ganadorEs2 && <Check className="size-3.5 shrink-0" />}
                    </div>

                    {p.sets && (
                      <div className="bg-muted/40 px-3 py-1.5 text-xs text-muted-foreground border-t border-border flex justify-between items-center">
                        <span>Resultado</span>
                        <span className="font-mono font-semibold">{p.sets}</span>
                      </div>
                    )}

                    {esAdmin && pendiente && tieneAmbosParticipantes && (
                      <div className="p-2 border-t border-border bg-muted/20">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => onCargarResultado?.(p)}
                          className="w-full h-8 text-xs font-semibold"
                        >
                          Cargar resultado
                        </Button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
