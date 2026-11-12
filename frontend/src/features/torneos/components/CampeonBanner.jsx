import { Trophy } from "lucide-react";

// Muestra al ganador cuando la final ya tiene resultado.
export function CampeonBanner({ partidos = [] }) {
  const ultimaRonda = Math.max(0, ...partidos.map((p) => p.ronda));
  const final = partidos.find((p) => p.ronda === ultimaRonda && p.resultado);
  if (!final) return null;

  return (
    <section className="flex items-center gap-4 rounded-xl bg-primary p-6 text-primary-foreground shadow-sm">
      <span className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary-foreground/15">
        <Trophy className="size-7" />
      </span>
      <div className="min-w-0">
        <p className="text-sm font-medium opacity-80">Campeón</p>
        <p className="truncate text-2xl font-extrabold tracking-[-.02em]">{final.resultado.replace("Gana ", "")}</p>
        <p className="truncate text-sm opacity-90">
          Final: {final.participanteANombre} vs {final.participanteBNombre} · {final.sets}
        </p>
      </div>
    </section>
  );
}
