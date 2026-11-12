import { useEffect, useState } from "react";
import { Bell, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { SuccessBanner } from "@/components/ui/success-banner";
import { mensajeDeError } from "@/lib/erroresApi";
import { guardarMisPreferencias, obtenerMisPreferencias } from "../api/perfilApi";
import {
  DETALLE_PREFERENCIA,
  ETIQUETA_PREFERENCIA,
  PREFERENCIAS_POR_DEFECTO,
  conPreferenciasPorDefecto,
} from "../model/perfil";

const CLAVES = Object.keys(PREFERENCIAS_POR_DEFECTO);

/** Qué correos quiere recibir la persona. */
export function NotificacionesCard() {
  const [preferencias, setPreferencias] = useState(PREFERENCIAS_POR_DEFECTO);
  const [guardadas, setGuardadas] = useState(PREFERENCIAS_POR_DEFECTO);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState("");
  const [mensajeError, setMensajeError] = useState("");
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    let vigente = true;

    obtenerMisPreferencias()
      .then((datos) => {
        if (!vigente) return;
        const completas = conPreferenciasPorDefecto(datos);
        setPreferencias(completas);
        setGuardadas(completas);
      })
      .catch((error) => {
        if (vigente) setErrorCarga(mensajeDeError(error, "No se pudieron cargar tus preferencias."));
      })
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, []);

  const hayCambios = CLAVES.some((clave) => preferencias[clave] !== guardadas[clave]);

  function alternar(clave) {
    setPreferencias((prev) => ({ ...prev, [clave]: !prev[clave] }));
    setAviso("");
  }

  async function guardar() {
    setMensajeError("");
    setGuardando(true);
    try {
      const devueltas = await guardarMisPreferencias(preferencias);
      const completas = conPreferenciasPorDefecto(devueltas ?? preferencias);
      setPreferencias(completas);
      setGuardadas(completas);
      setAviso("Se guardaron tus preferencias.");
    } catch (error) {
      setMensajeError(mensajeDeError(error, "No se pudieron guardar las preferencias."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <div>
        <h2 className="text-lg font-bold text-foreground">Notificaciones</h2>
        <p className="mt-1 text-sm text-muted-foreground">Los avisos llegan al correo con el que iniciás sesión.</p>
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>

      {errorCarga && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorCarga}
        </p>
      )}

      {mensajeError && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeError}
        </p>
      )}

      {cargando ? (
        <p className="py-4 text-sm text-muted-foreground">Cargando tus preferencias...</p>
      ) : (
        <div className="space-y-4">
          {CLAVES.map((clave) => (
            <div key={clave} className="space-y-1">
              <CustomCheckbox
                label={ETIQUETA_PREFERENCIA[clave]}
                name={clave}
                checked={preferencias[clave]}
                onChange={() => alternar(clave)}
              />
              <p className="pl-9 text-xs text-muted-foreground">{DETALLE_PREFERENCIA[clave]}</p>
            </div>
          ))}
        </div>
      )}

      <div className="flex items-center justify-end gap-3">
        {!cargando && !hayCambios && (
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Bell className="size-3.5" />
            No hay cambios sin guardar.
          </p>
        )}
        <Button type="button" disabled={cargando || guardando || !hayCambios} onClick={guardar}>
          <Save size={18} strokeWidth={2.5} />
          {guardando ? "Guardando..." : "Guardar preferencias"}
        </Button>
      </div>
    </div>
  );
}
