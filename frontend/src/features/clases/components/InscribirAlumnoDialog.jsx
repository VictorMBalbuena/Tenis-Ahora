import { useEffect, useState } from "react";
import { AlertCircle, Search, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useValorDemorado } from "@/hooks/use-valor-demorado";
import { mensajeDeError } from "@/lib/erroresApi";
import { listarUsuarios } from "@/features/usuarios/api/usuariosApi";
import { cupoCompleto, lugaresLibres, vigencia, yaPaso } from "../model/clase";
import { inscribirAlumno } from "../api/clasesApi";

/**
 * Inscribir un alumno a una clase (CU81).
 *
 * La regla del módulo es que con el cupo completo no se puede inscribir
 * (RD01, RD04). Acá se corta antes de pedir nada al servidor, y el backend
 * la vuelve a validar por si la llamada viniera de otro lado.
 *
 * Los socios se buscan con la API del módulo de usuarios, para no duplicar
 * el endpoint ni su paginación.
 */
export function InscribirAlumnoDialog({ abierto, clase, onCerrar, onInscripto }) {
  const [busqueda, setBusqueda] = useState("");
  const [socios, setSocios] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [elegido, setElegido] = useState(null);
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const busquedaDemorada = useValorDemorado(busqueda);

  useEffect(() => {
    if (!abierto) return;

    let vigente = true;
    const filtros = /^\d+$/.test(busquedaDemorada.trim())
      ? { dni: busquedaDemorada.trim() }
      : { nombre: busquedaDemorada.trim() };

    listarUsuarios("SOCIO", { ...filtros, size: 8 })
      .then((data) => vigente && setSocios(data.contenido || []))
      .catch(() => vigente && setSocios([]))
      .finally(() => vigente && setBuscando(false));

    return () => {
      vigente = false;
    };
  }, [abierto, busquedaDemorada]);

  if (!clase) return null;

  const completo = cupoCompleto(clase);
  const pasada = yaPaso(clase);
  const bloqueada = completo || pasada;

  async function inscribir() {
    if (!elegido || bloqueada) return;
    setError("");
    setGuardando(true);
    try {
      await inscribirAlumno(clase.id, elegido.id);
      onInscripto(`Se inscribió a ${elegido.nombre} ${elegido.apellido} a la clase.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo inscribir al alumno."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Inscribir alumno</DialogTitle>
          <DialogDescription>
            {clase.nombre} ({vigencia(clase)}) &mdash; {clase.cupoOcupado ?? 0} de {clase.cupoMaximo} lugares
            ocupados.
          </DialogDescription>
        </DialogHeader>

        {/* La regla del módulo, dicha antes de dejar elegir a nadie */}
        {completo && (
          <p
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>
              <strong className="font-semibold">El cupo está completo.</strong> No se puede inscribir a nadie
              más hasta que se libere un lugar.
            </span>
          </p>
        )}

        {!completo && pasada && (
          <p
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <span>La clase ya pasó, así que no admite inscripciones nuevas.</span>
          </p>
        )}

        {!bloqueada && (
          <>
            <div className="space-y-1.5">
              <label htmlFor="buscar-socio" className="text-[14px] font-semibold text-black">
                Buscar socio
              </label>
              <div className="relative">
                <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="buscar-socio"
                  value={busqueda}
                  placeholder="Nombre o DNI..."
                  className="pl-8"
                  onChange={(e) => {
                    setBusqueda(e.target.value);
                    setBuscando(true);
                    setElegido(null);
                  }}
                />
              </div>
              <p className="text-xs text-muted-foreground">
                Quedan {lugaresLibres(clase)} {lugaresLibres(clase) === 1 ? "lugar" : "lugares"}.
              </p>
            </div>

            <ul className="max-h-60 space-y-2 overflow-y-auto">
              {buscando && <li className="py-4 text-center text-sm text-muted-foreground">Buscando...</li>}

              {!buscando && socios.length === 0 && (
                <li className="py-4 text-center text-sm text-muted-foreground">
                  Ningún socio coincide con la búsqueda.
                </li>
              )}

              {!buscando &&
                socios.map((s) => {
                  const seleccionado = elegido?.id === s.id;
                  return (
                    <li key={s.id}>
                      <button
                        type="button"
                        onClick={() => setElegido(s)}
                        aria-pressed={seleccionado}
                        className={`flex w-full items-center justify-between gap-3 rounded-lg border p-3 text-left transition-colors ${
                          seleccionado ? "border-primary bg-primary/10" : "border-border hover:bg-muted"
                        }`}
                      >
                        <span className="min-w-0">
                          <span className="block truncate text-sm font-medium">
                            {s.nombre} {s.apellido}
                          </span>
                          <span className="block truncate text-xs text-muted-foreground">{s.email}</span>
                        </span>
                        <span className="shrink-0 font-mono text-xs tabular-nums text-muted-foreground">
                          {s.dni}
                        </span>
                      </button>
                    </li>
                  );
                })}
            </ul>
          </>
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

        <DialogFooter>
          <Button variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button disabled={bloqueada || !elegido || guardando} onClick={inscribir}>
            <UserPlus />
            {guardando ? "Inscribiendo..." : "Inscribir"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
