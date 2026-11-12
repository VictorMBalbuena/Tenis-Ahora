import { useEffect, useState } from "react";
import { AlertTriangle, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SuccessBanner } from "@/components/ui/success-banner";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";
import { usePerfil } from "../hooks/usePerfil";
import { FotoPerfilCard } from "../components/FotoPerfilCard";
import { CuentaResumenCard } from "../components/CuentaResumenCard";
import { PerfilForm } from "../components/PerfilForm";

/**
 * Mi perfil: ver y editar los datos propios y la foto.
 *
 * Existe porque ningún rol puede editarse a sí mismo desde Usuarios: esa
 * pantalla pide rol EMPLEADO o ADMINISTRADOR. Un socio solo puede cambiar sus
 * datos desde acá.
 */
export function PerfilPage() {
  const { perfil, cargando, error, setPerfil, recargar } = usePerfil();
  const setUsuario = useAuthStore((estado) => estado.setUsuario);
  const [aviso, setAviso] = useState("");

  // El token solo trae el correo y el rol, así que el menú lateral mostraba el
  // correo recortado. Con el perfil cargado ya puede mostrar el nombre y la
  // foto de verdad.
  useEffect(() => {
    if (!perfil) return;
    setUsuario({ nombre: perfil.nombre, apellido: perfil.apellido, fotoUrl: perfil.fotoUrl ?? null });
  }, [perfil, setUsuario]);

  function alGuardar(actualizado, mensaje) {
    if (actualizado) setPerfil(actualizado);
    setAviso(mensaje);
  }

  function alCambiarFoto(fotoUrl, mensaje) {
    setPerfil({ ...perfil, fotoUrl });
    setAviso(mensaje);
  }

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center font-medium text-muted-foreground">
        Cargando tu perfil...
      </div>
    );
  }

  if (error || !perfil) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>{mensajeDeError(error, "No se pudo cargar tu perfil.")}</p>
        <Button variant="outline" onClick={recargar}>
          <RotateCcw size={18} />
          Volver a intentar
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-10 sm:pb-12">
      <header>
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Mi perfil</h1>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <FotoPerfilCard perfil={perfil} onCambiada={alCambiarFoto} />
        </div>
        <CuentaResumenCard perfil={perfil} />
      </div>

      <PerfilForm perfil={perfil} onGuardado={alGuardar} />
    </div>
  );
}
