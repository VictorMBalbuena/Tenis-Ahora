import { useRef, useState } from "react";
import { Camera, Trash2 } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { mensajeDeError } from "@/lib/erroresApi";
import { quitarMiFoto, subirMiFoto } from "../api/perfilApi";
import { TAMANIO_MAXIMO_FOTO, TIPOS_IMAGEN, iniciales, nombreCompleto, validarFoto } from "../model/perfil";

/**
 * Foto de perfil: subirla, cambiarla o volver a las iniciales.
 *
 * El `<input type="file">` va oculto y lo dispara el botón, porque el input
 * nativo no se puede estilar y queda fuera de lugar entre el resto.
 */
export function FotoPerfilCard({ perfil, onCambiada }) {
  const input = useRef(null);
  const [error, setError] = useState("");
  const [subiendo, setSubiendo] = useState(false);
  const [quitando, setQuitando] = useState(false);

  async function elegir(e) {
    const archivo = e.target.files?.[0];
    // Se limpia el input para que elegir la misma foto otra vez vuelva a disparar el cambio.
    e.target.value = "";
    if (!archivo) return;

    const problema = validarFoto(archivo);
    if (problema) {
      setError(problema);
      return;
    }

    setError("");
    setSubiendo(true);
    try {
      const { fotoUrl } = await subirMiFoto(archivo);
      onCambiada(fotoUrl, "Se actualizó tu foto de perfil.");
    } catch (e2) {
      setError(mensajeDeError(e2, "No se pudo subir la foto."));
    } finally {
      setSubiendo(false);
    }
  }

  async function quitar() {
    setError("");
    setQuitando(true);
    try {
      await quitarMiFoto();
      onCambiada(null, "Se quitó tu foto de perfil.");
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo quitar la foto."));
    } finally {
      setQuitando(false);
    }
  }

  return (
    <div className="flex h-full flex-col justify-center rounded-xl border border-border bg-card p-6 shadow-sm">
      <div className="flex flex-col items-center gap-5 sm:flex-row sm:items-center">
        <Avatar className="size-28 shrink-0 rounded-2xl sm:size-32">
          {perfil.fotoUrl && <AvatarImage src={perfil.fotoUrl} alt={`Foto de ${nombreCompleto(perfil)}`} />}
          <AvatarFallback className="rounded-2xl bg-primary/20 text-3xl font-bold text-foreground">
            {iniciales(perfil)}
          </AvatarFallback>
        </Avatar>

        <div className="min-w-0 flex-1 text-center sm:text-left">
          <h2 className="truncate text-xl font-bold">{nombreCompleto(perfil)}</h2>
          <p className="truncate text-sm text-muted-foreground">{perfil.email}</p>

          <div className="mt-4 flex flex-wrap justify-center gap-2 sm:justify-start">
            <Button type="button" variant="outline" disabled={subiendo} onClick={() => input.current?.click()}>
              <Camera />
              {subiendo ? "Subiendo..." : perfil.fotoUrl ? "Cambiar foto" : "Subir foto"}
            </Button>
            {perfil.fotoUrl && (
              <Button
                type="button"
                variant="ghost"
                className="text-destructive hover:bg-destructive/10"
                disabled={quitando}
                onClick={quitar}
              >
                <Trash2 />
                {quitando ? "Quitando..." : "Quitar"}
              </Button>
            )}
          </div>

          <p className="mt-2 text-xs text-muted-foreground">
            JPG, PNG o WebP, hasta {Math.round(TAMANIO_MAXIMO_FOTO / 1024 / 1024)} MB.
          </p>

          {error && (
            <p role="alert" className="mt-3 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              {error}
            </p>
          )}
        </div>
      </div>

      <input
        ref={input}
        type="file"
        accept={TIPOS_IMAGEN.join(",")}
        className="hidden"
        aria-label="Elegir una foto de perfil"
        onChange={elegir}
      />
    </div>
  );
}
