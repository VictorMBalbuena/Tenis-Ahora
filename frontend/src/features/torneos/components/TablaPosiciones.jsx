import { Trophy } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { conSigno } from "../model/competencia";

/**
 * La tabla de posiciones de una liga.
 *
 * No hay columna de puntos: en tenis no hay empate, así que los partidos ganados son la puntuación. Lo que
 * desempata son las diferencias de sets y de games, y sale todo de los resultados ya cargados.
 */
export function TablaPosiciones({ posiciones = [], cargando = false }) {
  if (cargando) {
    return <p className="text-sm text-muted-foreground">Calculando la tabla…</p>;
  }

  if (posiciones.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-border p-8 text-center">
        <p className="text-sm text-muted-foreground">
          Todavía no hay inscriptos en esta liga, así que no hay tabla que mostrar.
        </p>
      </div>
    );
  }

  const seJugoAlgo = posiciones.some((p) => p.jugados > 0);

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-xl border border-border bg-card shadow-sm">
        <table className="w-full text-sm">
          <caption className="sr-only">Tabla de posiciones de la liga</caption>
          <thead className="border-b border-border bg-muted/40 text-xs font-semibold uppercase text-muted-foreground">
            <tr>
              <th scope="col" className="py-3 px-4 text-left">#</th>
              <th scope="col" className="py-3 px-4 text-left">Participante</th>
              <th scope="col" className="py-3 px-3 text-center" title="Partidos jugados">PJ</th>
              <th scope="col" className="py-3 px-3 text-center" title="Partidos ganados">PG</th>
              <th scope="col" className="py-3 px-3 text-center" title="Partidos perdidos">PP</th>
              <th scope="col" className="py-3 px-3 text-center" title="Sets a favor y en contra">Sets</th>
              <th scope="col" className="py-3 px-3 text-center" title="Diferencia de sets">Dif.</th>
              <th scope="col" className="py-3 px-3 text-center" title="Games a favor y en contra">Games</th>
              <th scope="col" className="py-3 px-3 text-center" title="Diferencia de games">Dif.</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {posiciones.map((p) => {
              const difSets = p.setsAFavor - p.setsEnContra;
              const difGames = p.gamesAFavor - p.gamesEnContra;
              const lidera = p.puesto === 1 && seJugoAlgo;

              return (
                <tr key={p.inscripcionId ?? p.puesto} className={lidera ? "bg-primary/5" : "hover:bg-muted/30"}>
                  <td className="py-3 px-4 font-mono text-xs tabular-nums text-muted-foreground">{p.puesto}</td>
                  <td className="py-3 px-4 font-semibold text-foreground">
                    <span className="flex items-center gap-2">
                      {p.participante}
                      {lidera && (
                        <Badge variant="default" className="gap-1">
                          <Trophy className="size-3" /> Puntero
                        </Badge>
                      )}
                    </span>
                  </td>
                  <td className="py-3 px-3 text-center tabular-nums">{p.jugados}</td>
                  <td className="py-3 px-3 text-center font-semibold tabular-nums">{p.ganados}</td>
                  <td className="py-3 px-3 text-center tabular-nums text-muted-foreground">{p.perdidos}</td>
                  <td className="py-3 px-3 text-center tabular-nums text-muted-foreground">
                    {p.setsAFavor}-{p.setsEnContra}
                  </td>
                  <td className="py-3 px-3 text-center tabular-nums">{conSigno(difSets)}</td>
                  <td className="py-3 px-3 text-center tabular-nums text-muted-foreground">
                    {p.gamesAFavor}-{p.gamesEnContra}
                  </td>
                  <td className="py-3 px-3 text-center tabular-nums text-muted-foreground">{conSigno(difGames)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-muted-foreground">
        Ordenada por partidos ganados. Si hay empate, desempata la diferencia de sets y después la de games.
        {!seJugoAlgo && " Todavía no se jugó ningún partido, así que están todos en cero."}
      </p>
    </div>
  );
}
