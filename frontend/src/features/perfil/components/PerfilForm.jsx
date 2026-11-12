import { useState } from "react";
import { Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { actualizarMiPerfil } from "../api/perfilApi";
import { aDatosDeFormulario, soloLoQueCambio, validarPerfil } from "../model/perfil";
import { DatosPerfilCard } from "./DatosPerfilCard";
import { DatosPerfilResumen } from "./DatosPerfilResumen";

/**
 * Formulario de los datos propios.
 *
 * El estado arranca del perfil que llega por props y lo mantiene este
 * componente, no la pantalla: asi no hace falta un efecto que copie el
 * perfil al formulario y pise lo que la persona este escribiendo.
 */
export function PerfilForm({ perfil, onGuardado }) {
  const [editando, setEditando] = useState(false);
  const [datos, setDatos] = useState(() => aDatosDeFormulario(perfil));
  const [original, setOriginal] = useState(() => aDatosDeFormulario(perfil));
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const hayCambios = Object.keys(soloLoQueCambio(datos, original)).length > 0;

  const cambiar = (campo) => (e) => {
    setDatos((prev) => ({ ...prev, [campo]: e.target.value }));
    onGuardado(null, "");
  };

  function editar() {
    setDatos(aDatosDeFormulario(perfil));
    setOriginal(aDatosDeFormulario(perfil));
    setEditando(true);
  }

  function descartar() {
    setDatos(original);
    setErrores({});
    setMensajeError("");
    setEditando(false);
    onGuardado(null, "");
  }

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");

    const nuevosErrores = validarPerfil(datos);
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    const cambios = soloLoQueCambio(datos, original);
    if (Object.keys(cambios).length === 0) {
      onGuardado(null, "No había nada para guardar.");
      return;
    }

    setGuardando(true);
    try {
      const actualizado = await actualizarMiPerfil(cambios);
      const guardados = aDatosDeFormulario(actualizado ?? { ...perfil, ...cambios });
      setDatos(guardados);
      setOriginal(guardados);
      setErrores({});
      setEditando(false);
      onGuardado(actualizado, "Se guardaron tus datos.");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudieron guardar tus datos."));
    } finally {
      setGuardando(false);
    }
  }

  if (!editando) {
    return <DatosPerfilResumen perfil={perfil} onEditar={editar} />;
  }

  return (
    <form onSubmit={guardar} noValidate className="space-y-6">
      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <DatosPerfilCard datos={datos} errores={errores} onChange={cambiar} />

      <div className="flex flex-wrap items-center justify-end gap-4">
        <Button type="button" variant="outline" disabled={guardando} onClick={descartar}>
          {hayCambios ? "Descartar cambios" : "Cancelar"}
        </Button>
        <Button type="submit" disabled={guardando}>
          <Save size={18} strokeWidth={2.5} />
          {guardando ? "Guardando..." : "Guardar cambios"}
        </Button>
      </div>
    </form>
  );
}
