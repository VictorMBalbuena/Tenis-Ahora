import { useState } from "react";
import { jwtDecode } from "jwt-decode";
import { AlertCircle, Check, Loader2, UserCheck, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/authStore";
import { buscarCompaneroPorDni, inscribirTorneo } from "../api/torneosApi";
import { mensajeDeError } from "@/lib/erroresApi";

/**
 * Quién está usando la pantalla, según el token: el id y el rol salen de la misma fuente, así no
 * puede pasar que se mande el id de alguien con el rol de otro.
 */
function sesionActual(token) {
  if (!token) return { id: null, rol: null };
  try {
    const { sub, rol } = jwtDecode(token);
    return { id: Number(sub), rol: rol ?? null };
  } catch {
    return { id: null, rol: null };
  }
}

/**
 * Un socio buscado por DNI. Mientras no se encontró muestra el campo; una vez encontrado, muestra
 * su nombre con la opción de cambiarlo. El DNI es lo único que una persona sabe de otra: el id es
 * interno y no se le pide a nadie.
 */
function BuscadorDeSocio({ id, etiqueta, dni, onDniChange, socio, onBuscar, onLimpiar, buscando }) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {etiqueta} <span className="text-destructive">*</span>
      </label>

      {socio ? (
        <div className="flex items-center justify-between gap-3 rounded-lg bg-[var(--estado-ok-bg)] px-3.5 py-2.5 text-sm text-[var(--estado-ok-fg)]">
          <span className="flex min-w-0 items-center gap-2">
            <Check className="size-4 shrink-0" aria-hidden="true" />
            <span className="min-w-0 truncate font-semibold">
              {socio.nombre} {socio.apellido}
            </span>
          </span>
          <button
            type="button"
            onClick={onLimpiar}
            className="flex shrink-0 items-center gap-1 font-medium underline-offset-2 hover:underline"
          >
            <X className="size-3.5" aria-hidden="true" />
            Cambiar
          </button>
        </div>
      ) : (
        <div className="flex gap-2">
          <input
            id={id}
            inputMode="numeric"
            value={dni}
            onChange={(e) => onDniChange(e.target.value.replace(/\D/g, "").slice(0, 8))}
            onKeyDown={(e) => {
              // Enter busca al socio, no envía la inscripción.
              if (e.key === "Enter") {
                e.preventDefault();
                onBuscar();
              }
            }}
            placeholder="Ej: 30111222"
            autoComplete="off"
            className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
          <Button type="button" variant="outline" onClick={onBuscar} disabled={buscando || !dni}>
            {buscando ? <Loader2 className="size-4 animate-spin" aria-hidden="true" /> : "Buscar"}
          </Button>
        </div>
      )}
    </div>
  );
}

export function InscribirTorneoDialog({ open, torneo, onClose, onInscripto }) {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const sesion = sesionActual(token);
  // El socio se inscribe a sí mismo; el empleado y el administrador inscriben socios del club, así
  // que a ellos hay que preguntarles también quién es el titular. El backend ya lo permite: solo le
  // prohíbe al socio inscribir a terceros.
  // El rol sale del token, que es lo que el backend va a mirar; si todavía no está (se recupera por
  // refresh al recargar), vale el del usuario guardado, para no mostrarle a un socio el formulario
  // del personal del club.
  const seInscribeASiMismo = (sesion.rol ?? user?.rol) === "SOCIO";

  const [dniTitular, setDniTitular] = useState("");
  const [titular, setTitular] = useState(null);
  const [dniPareja, setDniPareja] = useState("");
  const [companero, setCompanero] = useState(null);
  const [buscando, setBuscando] = useState(null);
  const [nombreEquipo, setNombreEquipo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");
  const [prevKey, setPrevKey] = useState(() => `${torneo?.id}-${open}`);

  if (prevKey !== `${torneo?.id}-${open}`) {
    setPrevKey(`${torneo?.id}-${open}`);
    setDniTitular("");
    setTitular(null);
    setDniPareja("");
    setCompanero(null);
    setError("");
    setNombreEquipo("");
  }

  if (!torneo) return null;

  const esPareja = torneo.modalidad === "PAREJA";

  async function buscar(quien, dni, guardar) {
    if (dni.length < 7) {
      setError("El DNI tiene que tener 7 u 8 números.");
      return;
    }
    setBuscando(quien);
    setError("");
    try {
      guardar(await buscarCompaneroPorDni(dni));
    } catch (err) {
      setError(mensajeDeError(err, "No hay un socio activo con ese DNI."));
    } finally {
      setBuscando(null);
    }
  }

  async function handleConfirmar(e) {
    e?.preventDefault();
    setError("");

    if (seInscribeASiMismo && !sesion.id) {
      setError("No se pudo identificar la sesión del socio.");
      return;
    }

    if (!seInscribeASiMismo && !titular) {
      setError("Buscá por DNI al socio que se inscribe.");
      return;
    }

    if (esPareja && !companero) {
      setError("Buscá por DNI al compañero antes de confirmar.");
      return;
    }

    const jugador1Id = seInscribeASiMismo ? sesion.id : titular.id;

    if (esPareja && companero.id === jugador1Id) {
      setError("El compañero tiene que ser otro socio.");
      return;
    }

    setEnviando(true);
    try {
      const req = {
        jugador1Id,
        parejaId: esPareja ? companero.id : null,
        nombreEquipo: nombreEquipo.trim() || null,
      };
      const res = await inscribirTorneo(torneo.id, req);
      onInscripto?.(res);
      onClose();
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo completar la inscripción."));
    } finally {
      setEnviando(false);
    }
  }

  const costoFormateado = torneo.costoInscripcion != null
    ? `$${Number(torneo.costoInscripcion).toLocaleString("es-AR")}`
    : "$0";

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UserCheck className="size-5 text-primary" />
            {seInscribeASiMismo ? "Inscripción a Torneo" : "Inscribir socios"}
          </DialogTitle>
          <DialogDescription>
            {seInscribeASiMismo ? (
              <>Confirmá tu participación en <strong>{torneo.nombre}</strong>.</>
            ) : (
              <>Inscribí {esPareja ? "a una pareja" : "a un socio"} en <strong>{torneo.nombre}</strong>.</>
            )}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleConfirmar} className="space-y-4">
          <div className="rounded-xl border border-border bg-muted/40 p-3.5 space-y-1.5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Categoría:</span>
              <span className="font-semibold">{torneo.categoria}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Modalidad:</span>
              <span className="font-semibold">{torneo.modalidad}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Arancel de inscripción:</span>
              <span className="font-bold text-primary">{costoFormateado}</span>
            </div>
          </div>

          {!seInscribeASiMismo && (
            <BuscadorDeSocio
              id="dniTitular"
              etiqueta={esPareja ? "DNI del primer jugador" : "DNI del socio"}
              dni={dniTitular}
              onDniChange={(v) => { setDniTitular(v); if (error) setError(""); }}
              socio={titular}
              onBuscar={() => buscar("titular", dniTitular, setTitular)}
              onLimpiar={() => { setTitular(null); setDniTitular(""); }}
              buscando={buscando === "titular"}
            />
          )}

          {esPareja ? (
            <BuscadorDeSocio
              id="dniPareja"
              etiqueta={seInscribeASiMismo ? "DNI de tu compañero" : "DNI del segundo jugador"}
              dni={dniPareja}
              onDniChange={(v) => { setDniPareja(v); if (error) setError(""); }}
              socio={companero}
              onBuscar={() => buscar("pareja", dniPareja, setCompanero)}
              onLimpiar={() => { setCompanero(null); setDniPareja(""); }}
              buscando={buscando === "pareja"}
            />
          ) : (
            <p className="text-xs text-muted-foreground">La modalidad singles no requiere compañero.</p>
          )}

          {esPareja && (
            <div className="space-y-1.5">
              <label htmlFor="nombreEquipo" className="text-sm font-medium">
                Nombre de equipo (opcional)
              </label>
              <input
                id="nombreEquipo"
                type="text"
                value={nombreEquipo}
                onChange={(e) => setNombreEquipo(e.target.value)}
                placeholder="Ej: Los Invencibles"
                className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
          )}

          {error && (
            <p
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {error}
            </p>
          )}

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={enviando}>
              {enviando ? "Inscribiendo..." : "Confirmar inscripción"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
