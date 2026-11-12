import { useState } from "react";
import { Loader2 } from "lucide-react";
import { CampoLabel } from "@/components/ui/campo-label";
import { mensajeDeError } from "@/lib/erroresApi";
import { buscarJugadorPorDni } from "../api/reservasApi";

// Suma a un socio como acompañante buscándolo por DNI: nadie conoce el id interno de otro socio.
export function BuscadorDeAcompaniante({ idx, acompaniante, onEncontrado, onCambiarDni }) {
  const [dni, setDni] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState(null);

  const buscar = async () => {
    if (!/^\d{7,8}$/.test(dni)) {
      setError("El DNI debe tener 7 u 8 números.");
      return;
    }
    try {
      setBuscando(true);
      setError(null);
      onEncontrado(await buscarJugadorPorDni(dni));
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo buscar al socio."));
    } finally {
      setBuscando(false);
    }
  };

  return (
    <>
      <CampoLabel htmlFor={`acomp-${idx}-socio-dni`} info="Sin puntos, con 7 u 8 números.">DNI del socio</CampoLabel>
      <div className="flex gap-2">
        <input
          id={`acomp-${idx}-socio-dni`}
          type="text"
          inputMode="numeric"
          placeholder="DNI sin puntos"
          value={dni}
          onChange={(e) => {
            setDni(e.target.value.replace(/\D/g, "").slice(0, 8));
            if (acompaniante.personaId) onCambiarDni();
          }}
          className="flex w-full min-w-0 rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors placeholder:text-muted-foreground/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring font-mono"
        />
        <button
          type="button"
          onClick={buscar}
          disabled={buscando}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-full border border-input bg-background px-6 py-3 text-[15px] font-semibold transition-colors hover:bg-muted disabled:opacity-50"
        >
          {buscando && <Loader2 className="h-4 w-4 animate-spin" />}
          Buscar
        </button>
      </div>
      {error && <p role="alert" className="text-xs text-[var(--estado-error-fg)]">{error}</p>}
      {acompaniante.personaId && !error && (
        <p className="text-xs font-semibold text-[var(--estado-ok-fg)]">
          {acompaniante.nombre} {acompaniante.apellido} quedó como acompañante.
        </p>
      )}
    </>
  );
}
