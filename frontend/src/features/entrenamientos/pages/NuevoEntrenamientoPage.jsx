import { useState } from "react";
import { Save } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { crearEntrenamiento } from "../api/entrenamientosApi";
import { validarEntrenamiento } from "../model/entrenamiento";
import { EntrenamientoFormCard } from "../components/EntrenamientoFormCard";

const VACIO = {
  nombre: "",
  entrenadorId: "",
  canchaId: "",
  objetivo: "",
  modalidad: "",
  fechaInicio: "",
  fechaFin: "",
  dias: [],
  horaInicio: "",
  horaFin: "",
  cupoMaximo: "",
  cupoMinimo: "",
  valorMensual: "",
  categoria: "",
};

function aEntrenamientoRequest(datos) {
  return {
    nombre: datos.nombre.trim(),
    entrenadorId: Number(datos.entrenadorId),
    canchaId: Number(datos.canchaId),
    objetivo: datos.objetivo.trim(),
    modalidad: datos.modalidad,
    fechaInicio: datos.fechaInicio,
    fechaFin: datos.fechaFin,
    dias: datos.dias,
    horaInicio: datos.horaInicio,
    horaFin: datos.horaFin,
    cupoMaximo: Number(datos.cupoMaximo),
    cupoMinimo: datos.cupoMinimo === "" ? undefined : Number(datos.cupoMinimo),
    valorMensual: datos.valorMensual === "" ? undefined : Number(datos.valorMensual),
    categoria: datos.categoria.trim() || undefined,
  };
}

/** Programar un entrenamiento (CU37). */
export default function NuevoEntrenamientoPage() {
  const navigate = useNavigate();
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const handleChange = (e) => setDatos((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");

    const nuevos = validarEntrenamiento(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    setGuardando(true);
    try {
      await crearEntrenamiento(aEntrenamientoRequest(datos));
      navigate("/entrenamientos", { state: { aviso: "Se programó el entrenamiento." } });
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo programar el entrenamiento."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={guardar} noValidate className="space-y-6">
      <VolverButton to="/entrenamientos" />

      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Nuevo entrenamiento</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Elegí el entrenador, la cancha, el objetivo, los días, el horario y el cupo.
        </p>
      </header>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <EntrenamientoFormCard datos={datos} errores={errores} onChange={handleChange} />

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => navigate("/entrenamientos")}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          <Save />
          {guardando ? "Guardando..." : "Programar entrenamiento"}
        </Button>
      </div>
    </form>
  );
}
