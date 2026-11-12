import { useState } from "react";
import { Save } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { crearClase } from "../api/clasesApi";
import { validarClase } from "../model/clase";
import { ClaseFormCard } from "../components/ClaseFormCard";

const VACIO = {
  nombre: "",
  profesorId: "",
  canchaId: "",
  nivel: "",
  modalidad: "",
  fechaInicio: "",
  fechaFin: "",
  dias: [],
  horaInicio: "",
  horaFin: "",
  cupoMaximo: "",
  valorMensual: "",
};

function aClaseRequest(datos) {
  return {
    nombre: datos.nombre,
    profesorId: Number(datos.profesorId),
    canchaId: Number(datos.canchaId),
    nivel: datos.nivel,
    modalidad: datos.modalidad,
    fechaInicio: datos.fechaInicio,
    fechaFin: datos.fechaFin,
    dias: datos.dias,
    horaInicio: datos.horaInicio,
    horaFin: datos.horaFin,
    cupoMaximo: Number(datos.cupoMaximo),
    valorMensual: datos.valorMensual === "" ? undefined : Number(datos.valorMensual),
  };
}

/** Programar una clase (CU32) con su cupo maximo (CU70). */
export default function NuevaClasePage() {
  const navigate = useNavigate();
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const handleChange = (e) =>
    setDatos((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");

    const nuevos = validarClase(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    setGuardando(true);
    try {
      await crearClase(aClaseRequest(datos));
      navigate("/clases", { state: { aviso: "Se programo la clase." } });
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo programar la clase."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} noValidate className="space-y-6">
      <VolverButton to="/clases" />

      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Nueva clase</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Eleg&iacute; el profesor, la cancha, el horario y el cupo de alumnos.
        </p>
      </header>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <ClaseFormCard datos={datos} errores={errores} onChange={handleChange} />

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => navigate("/clases")}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          <Save />
          {guardando ? "Guardando..." : "Programar clase"}
        </Button>
      </div>
    </form>
  );
}
