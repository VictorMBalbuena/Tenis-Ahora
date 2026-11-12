import { Dumbbell } from "lucide-react";
import { ActividadSocioPantalla } from "@/components/ui/actividad-socio-pantalla";
import { ActividadesDelSocio } from "@/components/ui/actividades-del-socio";
import { desinscribirAlumno, inscribirme, listarEntrenamientos, obtenerEntrenamiento } from "../api/entrenamientosApi";
import { ETIQUETA_MODALIDAD, horario, rangoFechas } from "../model/entrenamiento";

// Entrenamientos que puede tomar el socio: la vista la comparte con clases, acá solo van los datos propios.
const describir = (entrenamiento) => ({
  etiqueta: entrenamiento.objetivo || (ETIQUETA_MODALIDAD[entrenamiento.modalidad] ?? entrenamiento.modalidad),
  horario: horario(entrenamiento),
  etiquetaInstructor: "Entrenador",
  instructor: entrenamiento.entrenador ?? "A confirmar",
  vigencia: rangoFechas(entrenamiento),
  extra: { etiqueta: "Modalidad", valor: ETIQUETA_MODALIDAD[entrenamiento.modalidad] ?? entrenamiento.modalidad },
});

export function EntrenamientosDelSocio({ vista }) {
  return (
    <ActividadesDelSocio
      titulo="Entrenamientos"
      tipo="ENTRENAMIENTO"
      ruta="/entrenamientos"
      plural="entrenamientos"
      Icono={Dumbbell}
      describir={describir}
      listar={listarEntrenamientos}
      inscribirme={inscribirme}
      vista={vista}
    />
  );
}

export function EntrenamientoDelSocio() {
  return (
    <ActividadSocioPantalla
      tipoEtiqueta="el entrenamiento"
      volver="/entrenamientos"
      obtener={obtenerEntrenamiento}
      describir={describir}
      inscribirme={inscribirme}
      darmeDeBaja={desinscribirAlumno}
    />
  );
}
