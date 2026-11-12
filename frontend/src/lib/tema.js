/**
 * Tema de la interfaz.
 *
 * Los colores del modo oscuro ya estaban definidos en index.css (la clase
 * `.dark`), pero no había forma de activarlos. Acá vive esa parte: se guarda
 * la preferencia en el navegador y se pone o se saca la clase en <html>.
 *
 * No va al backend a propósito: es por dispositivo, no por cuenta. Alguien
 * puede querer el club en claro en la computadora del mostrador y en oscuro
 * en su teléfono.
 */

export const Tema = {
  CLARO: "CLARO",
  OSCURO: "OSCURO",
  SISTEMA: "SISTEMA",
};

export const ETIQUETA_TEMA = {
  [Tema.CLARO]: "Claro",
  [Tema.OSCURO]: "Oscuro",
  [Tema.SISTEMA]: "El del sistema",
};

export const CLAVE = "tenisahora-tema";

const consultaOscuro = () =>
  typeof window !== "undefined" && typeof window.matchMedia === "function"
    ? window.matchMedia("(prefers-color-scheme: dark)")
    : null;

/** El tema elegido, o el del sistema si nunca se eligió uno. */
export function temaGuardado() {
  try {
    const guardado = localStorage.getItem(CLAVE);
    return Object.values(Tema).includes(guardado) ? guardado : Tema.SISTEMA;
  } catch {
    // Modo privado o almacenamiento bloqueado: se sigue con el del sistema.
    return Tema.SISTEMA;
  }
}

export function guardarTema(tema) {
  try {
    localStorage.setItem(CLAVE, tema);
  } catch {
    // Si no se puede guardar, el tema igual se aplica en esta pestaña.
  }
}

/** Si con este tema la pantalla queda oscura. */
export function esOscuro(tema) {
  if (tema === Tema.OSCURO) return true;
  if (tema === Tema.CLARO) return false;
  return Boolean(consultaOscuro()?.matches);
}

/** Pone o saca la clase `dark` en <html>, que es lo que leen los estilos. */
export function aplicarTema(tema) {
  if (typeof document === "undefined") return;
  document.documentElement.classList.toggle("dark", esOscuro(tema));
}

/** Aplica el tema guardado. Se llama una vez al arrancar la página. */
export function iniciarTema() {
  aplicarTema(temaGuardado());
}

/**
 * Avisa cuando el sistema cambia de claro a oscuro.
 *
 * Solo importa con el tema "el del sistema": si alguien eligió claro u
 * oscuro a mano, su elección manda. Devuelve la función para dejar de
 * escuchar.
 */
export function escucharSistema(alCambiar) {
  const consulta = consultaOscuro();
  if (!consulta) return () => {};

  consulta.addEventListener("change", alCambiar);
  return () => consulta.removeEventListener("change", alCambiar);
}
