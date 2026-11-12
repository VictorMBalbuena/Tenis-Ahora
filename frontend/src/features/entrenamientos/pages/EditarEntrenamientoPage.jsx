import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { actualizarEntrenamiento, obtenerEntrenamiento } from "../api/entrenamientosApi";
import { rangoFechas, validarEntrenamiento } from "../model/entrenamiento";
import { EntrenamientoFormCard } from "../components/EntrenamientoFormCard";

/** Pasa la respuesta del backend a la forma que usa el formulario. */
function aFormulario(e) {
  return {
    nombre: e.nombre ?? "",
    entrenadorId: e.entrenadorId != null ? String(e.entrenadorId) : "",
    canchaId: e.canchaId != null ? String(e.canchaId) : "",
    objetivo: e.objetivo ?? "",
    modalidad: e.modalidad ?? "",
    fechaInicio: e.fechaInicio ?? "",
    fechaFin: e.fechaFin ?? "",
    dias: e.dias ?? [],
    horaInicio: e.horaInicio ? String(e.horaInicio).slice(0, 5) : "",
    horaFin: e.horaFin ? String(e.horaFin).slice(0, 5) : "",
    cupoMaximo: e.cupoMaximo != null ? String(e.cupoMaximo) : "",
    cupoMinimo: e.cupoMinimo != null ? String(e.cupoMinimo) : "",
    valorMensual: e.valorMensual != null ? String(e.valorMensual) : "",
    categoria: e.categoria ?? "",
  };
}

const NUMERICOS = new Set(["entrenadorId", "canchaId", "cupoMaximo", "cupoMinimo", "valorMensual"]);

function cambioDeValor(campo, actual, original) {
  if (campo === "dias") {
    const a = [...actual].sort();
    const o = [...original].sort();
    return a.length !== o.length || a.some((d, i) => d !== o[i]);
  }
  return actual !== original;
}

/** Modificar un entrenamiento (CU38). En el PATCH solo viajan los campos que cambiaron. */
export default function EditarEntrenamientoPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [original, setOriginal] = useState(null);
  const [datos, setDatos] = useState(null);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    let vigente = true;

    obtenerEntrenamiento(id)
      .then((entrenamiento) => {
        if (!vigente) return;
        const form = aFormulario(entrenamiento);
        setOriginal(form);
        setDatos(form);
      })
      .catch((error) => {
        if (vigente) setMensajeError(mensajeDeError(error, "No se pudo cargar el entrenamiento."));
      })
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, [id]);

  const handleChange = (e) => setDatos((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  function soloLoQueCambio() {
    const cambios = {};
    for (const campo of Object.keys(datos)) {
      if (!cambioDeValor(campo, datos[campo], original[campo])) continue;
      cambios[campo] = NUMERICOS.has(campo) && datos[campo] !== "" ? Number(datos[campo]) : datos[campo];
    }
    return cambios;
  }

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");

    const nuevos = validarEntrenamiento(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    const cambios = soloLoQueCambio();
    if (Object.keys(cambios).length === 0) {
      navigate(`/entrenamientos/${id}`);
      return;
    }

    setGuardando(true);
    try {
      await actualizarEntrenamiento(id, cambios);
      navigate(`/entrenamientos/${id}`, { state: { aviso: "Se guardaron los cambios del entrenamiento." } });
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudieron guardar los cambios."));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return <p className="py-10 text-center text-sm text-muted-foreground">Cargando el entrenamiento...</p>;
  }

  if (!datos) {
    return (
      <div className="space-y-6">
        <VolverButton to="/entrenamientos" />
        <ErrorBanner>{mensajeError || "No se encontró el entrenamiento."}</ErrorBanner>
      </div>
    );
  }

  return (
    <form onSubmit={guardar} noValidate className="space-y-6">
      <VolverButton to={`/entrenamientos/${id}`} />

      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Modificar entrenamiento</h1>
        <p className="mt-1 text-sm text-muted-foreground">Entrenamiento vigente del {rangoFechas(datos)}.</p>
      </header>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <EntrenamientoFormCard datos={datos} errores={errores} onChange={handleChange} />

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => navigate(`/entrenamientos/${id}`)}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          <Save />
          {guardando ? "Guardando..." : "Guardar cambios"}
        </Button>
      </div>
    </form>
  );
}
