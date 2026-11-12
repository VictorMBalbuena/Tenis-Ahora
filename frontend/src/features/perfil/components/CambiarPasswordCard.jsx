import { useState } from "react";
import { Eye, EyeOff, KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SuccessBanner } from "@/components/ui/success-banner";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { cambiarMiPassword } from "../api/perfilApi";
import { LARGO_MAXIMO_PASSWORD, LARGO_MINIMO_PASSWORD, fortalezaPassword, validarCambioPassword } from "../model/perfil";

const VACIO = { passwordActual: "", passwordNueva: "", repetir: "" };

const COLOR_FORTALEZA = ["", "bg-destructive", "bg-[var(--estado-aviso-fg)]", "bg-[var(--estado-ok-fg)]"];

/** Barra que dice qué tan fuerte es la contraseña nueva. No bloquea nada. */
function Fortaleza({ password }) {
  const { nivel, etiqueta } = fortalezaPassword(password);
  if (!nivel) return null;

  return (
    <div className="space-y-1.5">
      <div className="flex gap-1.5" aria-hidden="true">
        {[1, 2, 3].map((tramo) => (
          <span
            key={tramo}
            className={`h-1.5 flex-1 rounded-full ${tramo <= nivel ? COLOR_FORTALEZA[nivel] : "bg-muted"}`}
          />
        ))}
      </div>
      <p className="text-xs text-muted-foreground">Seguridad: {etiqueta}</p>
    </div>
  );
}

/** Campo de contraseña con el ojito para verla. */
function CampoPassword({ id, etiqueta, valor, error, ayuda, onChange }) {
  const [visible, setVisible] = useState(false);

  return (
    <CampoFormulario id={id} etiqueta={etiqueta} ayuda={ayuda} error={error}>
      {(props) => (
        <div className="relative">
          <input
            {...props}
            type={visible ? "text" : "password"}
            autoComplete={id === "password-actual" ? "current-password" : "new-password"}
            className={`${CLASE_INPUT} pr-12`}
            value={valor}
            onChange={onChange}
          />
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? `Ocultar ${etiqueta.toLowerCase()}` : `Mostrar ${etiqueta.toLowerCase()}`}
            className="absolute top-1/2 right-4 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
          >
            {visible ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
      )}
    </CampoFormulario>
  );
}

/** Cambio de la propia contraseña: pide la actual además de la nueva. */
export function CambiarPasswordCard() {
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);

  const cambiar = (campo) => (e) => {
    setDatos((prev) => ({ ...prev, [campo]: e.target.value }));
    setAviso("");
  };

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");
    setAviso("");

    const nuevosErrores = validarCambioPassword(datos);
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    setGuardando(true);
    try {
      await cambiarMiPassword({ passwordActual: datos.passwordActual, passwordNueva: datos.passwordNueva });
      setDatos(VACIO);
      setErrores({});
      setAviso("Se cambió tu contraseña.");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo cambiar la contraseña."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} noValidate className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <div>
        <h2 className="text-lg font-bold text-foreground">Contraseña</h2>
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>

      {mensajeError && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeError}
        </p>
      )}

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
        <CampoPassword
          id="password-actual"
          etiqueta="Contraseña actual"
          valor={datos.passwordActual}
          error={errores.passwordActual}
          onChange={cambiar("passwordActual")}
        />

        <div className="space-y-3 sm:col-span-2 sm:grid sm:grid-cols-2 sm:gap-6 sm:space-y-0">
          <div className="space-y-3">
            <CampoPassword
              id="password-nueva"
              etiqueta="Contraseña nueva"
              ayuda={`Entre ${LARGO_MINIMO_PASSWORD} y ${LARGO_MAXIMO_PASSWORD} caracteres`}
              valor={datos.passwordNueva}
              error={errores.passwordNueva}
              onChange={cambiar("passwordNueva")}
            />
            <Fortaleza password={datos.passwordNueva} />
          </div>

          <CampoPassword
            id="password-repetir"
            etiqueta="Repetir la nueva"
            valor={datos.repetir}
            error={errores.repetir}
            onChange={cambiar("repetir")}
          />
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={guardando}>
          <KeyRound size={18} strokeWidth={2.5} />
          {guardando ? "Cambiando..." : "Cambiar contraseña"}
        </Button>
      </div>
    </form>
  );
}
