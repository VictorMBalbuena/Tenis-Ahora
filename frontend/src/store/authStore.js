import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({
      token: null, // Access token: vive solo en memoria. Nunca se guarda en el navegador.
      user: null, // { email, rol }: datos para mostrar en pantalla, no son credenciales
      inicializado: false, // true cuando ya se intentó recuperar la sesión al abrir la página

      // Se ejecuta desde el LoginPage al iniciar sesión con éxito
      setLogin: (token, user) => set({ token, user, inicializado: true }),

      // Se ejecuta desde el axiosClient cuando se renueva el access token
      setToken: (token) => set({ token }),

      setInicializado: () => set({ inicializado: true }),

      // Completa los datos del usuario sin tocar el token. Lo usa Mi perfil
      // para que el menu lateral muestre el nombre y la foto de verdad, que
      // no vienen en el token.
      setUsuario: (datos) => set((estado) => ({ user: { ...estado.user, ...datos } })),

      // Se ejecuta desde el menú lateral o cuando la sesión se cierra definitivamente
      logout: () => set({ token: null, user: null, inicializado: true }),
    }),
    {
      name: 'auth-storage', // Nombre con el que se guarda en el LocalStorage del navegador
      version: 1,
      // Solo se guarda lo que no es una credencial. El refresh token está en una cookie httpOnly que el JavaScript de la
      // página no puede leer, así que un XSS no puede robar la sesión.
      partialize: (state) => ({ user: state.user }),
      // Las versiones anteriores guardaban los tokens acá: se descartan en cuanto se abre la página
      migrate: (persistido) => ({ user: persistido?.user ?? null }),
    }
  )
);
