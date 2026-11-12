import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// La dirección del backend sale de la variable de entorno VITE_API_URL (ver .env.example): no va escrita en el código.
const BASE_URL = import.meta.env.VITE_API_URL;

if (!BASE_URL) {
  throw new Error('Falta la variable de entorno VITE_API_URL. Copiá .env.example a .env.development y completala.');
}

// Endpoints donde un 401 significa "credenciales incorrectas" o "sin sesión", no "el access token venció"
const RUTAS_SIN_RENOVACION = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];

const axiosClient = axios.create({
  baseURL: BASE_URL,
  // Hace que el navegador mande y reciba la cookie httpOnly del refresh token
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let renovacionEnCurso = null;

/**
 * Pide un access token nuevo con la cookie del refresh token (la manda el navegador, el JavaScript no la ve).
 * Si varios pedidos la necesitan a la vez, comparten una sola renovación: el refresh token es de un solo uso y
 * renovar dos veces con el mismo cerraría la sesión sin motivo.
 */
export function renovarAccessToken() {
  if (!renovacionEnCurso) {
    // Se manda {} para que el pedido sea application/json: un formulario de otro sitio no puede hacerlo
    renovacionEnCurso = axios
      .post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true })
      .then(({ data }) => {
        useAuthStore.getState().setToken(data.accessToken);
        return data.accessToken;
      })
      .finally(() => {
        renovacionEnCurso = null;
      });
  }
  return renovacionEnCurso;
}

// Interceptor: Antes de que salga cualquier petición al backend, le inyectamos el JWT automáticamente
axiosClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor de respuesta: si el access token venció (401), lo renueva una sola vez y reintenta el pedido original.
axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { response, config } = error;

    if (!response || response.status !== 401 || config._reintentado || RUTAS_SIN_RENOVACION.includes(config.url)) {
      return Promise.reject(error);
    }

    try {
      config._reintentado = true;
      const accessToken = await renovarAccessToken();
      config.headers.Authorization = `Bearer ${accessToken}`;
      return axiosClient(config);
    } catch (refreshError) {
      useAuthStore.getState().logout();
      return Promise.reject(refreshError);
    }
  }
);

export default axiosClient;
