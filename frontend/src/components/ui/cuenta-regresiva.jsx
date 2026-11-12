import { useEffect, useState } from "react";
import { Timer } from "lucide-react";

// Reloj del tiempo que queda para pagar: lo calcula contra venceEn del backend, así no se atrasa ni se reinicia al recargar.
const segundosHasta = (venceEn) => Math.max(0, Math.floor((new Date(venceEn).getTime() - Date.now()) / 1000));

const aReloj = (segundos) => {
  const minutos = String(Math.floor(segundos / 60)).padStart(2, "0");
  return `${minutos}:${String(segundos % 60).padStart(2, "0")}`;
};

export function CuentaRegresiva({ venceEn, prefijo, className = "" }) {
  const [segundos, setSegundos] = useState(() => segundosHasta(venceEn));

  useEffect(() => {
    const intervalo = setInterval(() => setSegundos(segundosHasta(venceEn)), 1000);
    return () => clearInterval(intervalo);
  }, [venceEn]);

  return (
    <span className={`inline-flex items-center gap-1.5 ${className}`} role="timer" aria-label={segundos > 0 ? `Quedan ${aReloj(segundos)} para pagar` : "Se terminó el tiempo para pagar"}>
      <Timer className="size-3.5 shrink-0" />
      {segundos > 0 ? (
        <>
          {prefijo && <span>{prefijo}</span>}
          <span className="font-mono tabular-nums">{aReloj(segundos)}</span>
        </>
      ) : (
        <span>Tiempo agotado</span>
      )}
    </span>
  );
}
