import { useEffect, useState } from "react";

/** Rebota el valor para no pegarle al servidor en cada tecla. */
export function useValorDemorado(valor, ms = 300) {
  const [demorado, setDemorado] = useState(valor);

  useEffect(() => {
    const t = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);

  return demorado;
}
