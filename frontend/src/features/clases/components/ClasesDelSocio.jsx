import { GraduationCap } from "lucide-react";
import { ActividadSocioPantalla } from "@/components/ui/actividad-socio-pantalla";
import { ActividadesDelSocio } from "@/components/ui/actividades-del-socio";
import { desinscribirAlumno, inscribirme, listarClases, obtenerClase } from "../api/clasesApi";
import { ETIQUETA_MODALIDAD, ETIQUETA_NIVEL, horario, nombreProfesor, vigencia } from "../model/clase";

// Clases que puede tomar el socio: la vista la comparte con entrenamientos, acá solo van los datos propios.
const describir = (clase) => ({
  etiqueta: `${ETIQUETA_NIVEL[clase.nivel] ?? clase.nivel} · ${ETIQUETA_MODALIDAD[clase.modalidad] ?? clase.modalidad}`,
  horario: horario(clase),
  etiquetaInstructor: "Profesor",
  instructor: nombreProfesor(clase),
  vigencia: vigencia(clase),
  extra: null,
});

export function ClasesDelSocio({ vista }) {
  return (
    <ActividadesDelSocio
      titulo="Clases"
      tipo="CLASE"
      ruta="/clases"
      plural="clases"
      Icono={GraduationCap}
      describir={describir}
      listar={listarClases}
      inscribirme={inscribirme}
      vista={vista}
    />
  );
}

export function ClaseDelSocio() {
  return (
    <ActividadSocioPantalla
      tipoEtiqueta="la clase"
      volver="/clases"
      obtener={obtenerClase}
      describir={describir}
      inscribirme={inscribirme}
      darmeDeBaja={desinscribirAlumno}
    />
  );
}
