import { renovarAccessToken } from './axiosClient';
import { useAuthStore } from '../store/authStore';
import { obtenerMiPerfil } from '@/features/perfil/api/perfilApi';

/**
 * Se llama una vez al abrir la página. El access token solo vive en memoria, así que al recargar se perdió: si había
 * una sesión guardada (el usuario), se recupera pidiendo un access token nuevo con la cookie del refresh token.
 */
export async function restaurarSesion() {
  const { user, setInicializado, logout, setUsuario } = useAuthStore.getState();

  if (!user) {
    setInicializado();
    return;
  }

  try {
    await renovarAccessToken();

    // Si la sesión no tiene aún nombre o foto (ej: login previo que solo tenía el JWT), la completamos de /me
    if (!user.nombre) {
      try {
        const perfil = await obtenerMiPerfil();
        if (perfil) {
          setUsuario({
            nombre: perfil.nombre,
            apellido: perfil.apellido,
            fotoUrl: perfil.fotoUrl ?? null,
          });
        }
      } catch {
        // Silencioso
      }
    }

    setInicializado();
  } catch {
    logout(); // la cookie venció o la cuenta se deshabilitó: hay que volver a iniciar sesión
  }
}
