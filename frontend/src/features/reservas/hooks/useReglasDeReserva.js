import { useEffect, useState } from "react";
import { obtenerReglasDeReserva } from "../api/reservasApi";

// Las reglas de las reservas salen siempre del servidor: nunca se escriben en el front.
export function useReglasDeReserva() {
  const [reglas, setReglas] = useState(null);

  useEffect(() => {
    let activo = true;
    Promise.resolve(obtenerReglasDeReserva())
      .then((datos) => {
        if (activo && datos) setReglas(datos);
      })
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  return reglas;
}
