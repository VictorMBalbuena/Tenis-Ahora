import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { actualizarClase, obtenerClase } from "../api/clasesApi";
import { validarClase } from "../model/clase";
import { ClaseFormCard } from "../components/ClaseFormCard";

/** Pasa la respuesta del backend a la forma que usa el formulario. */
function aFormulario(clase) {
  return {
    nombre: clase.nombre ?? "",
    profesorId: clase.profesorId != null ? String(clase.profesorId) : "",
    canchaId: clase.canchaId != null ? String(clase.canchaId) : "",
    nivel: clase.nivel ?? "",
    modalidad: clase.modalidad ?? "",
    fechaInicio: clase.fechaInicio ?? "",
    fechaFin: clase.fechaFin ?? "",
    dias: clase.dias ?? [],
    horaInicio: clase.horaInicio ? String(clase.horaInicio).slice(0, 5) : "",
    horaFin: clase.horaFin ? String(clase.horaFin).slice(0, 5) : "",
    cupoMaximo: clase.cupoMaximo != null ? String(clase.cupoMaximo) : "",
    valorMensual: clase.valorMensual != null ? String(clase.valorMensual) : "",
  };
}

/** Si dos conjuntos de días son el mismo, sin importar el orden. */
function mismosDias(a, b) {
  const sa = new Set(a ?? []);
  const sb = new Set(b ?? []);
  return sa.size === sb.size && [...sa].every((d) => sb.has(d));
}

/** Modificar una clase (CU33). En el PATCH solo viajan los campos que cambiaron. */
export default function EditarClasePage() {
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

    obtenerClase(id)
      .then((clase) => {
        if (!vigente) return;
        const form = aFormulario(clase);
        setOriginal(form);
        setDatos(form);
      })
      .catch((error) => {
        if (vigente) setMensajeError(mensajeDeError(error, "No se pudo cargar la clase."));
      })
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, [id]);

  const handleChange = (e) => setDatos((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  /** Solo los campos que efectivamente cambiaron, con el tipo que espera el backend. */
  function soloLoQueCambio() {
    const numericos = new Set(["profesorId", "canchaId", "cupoMaximo", "valorMensual"]);
    const cambios = {};
    for (const campo of Object.keys(datos)) {
      if (campo === "dias") {
        if (!mismosDias(datos.dias, original.dias)) cambios.dias = datos.dias;
        continue;
      }
      if (datos[campo] === original[campo]) continue;
      cambios[campo] = numericos.has(campo) ? Number(datos[campo]) : datos[campo];
    }
    return cambios;
  }

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");

    const nuevos = validarClase(datos);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    const cambios = soloLoQueCambio();
    if (Object.keys(cambios).length === 0) {
      navigate(`/clases/${id}`);
      return;
    }

    setGuardando(true);
    try {
      await actualizarClase(id, cambios);
      navigate(`/clases/${id}`, { state: { aviso: "Se guardaron los cambios de la clase." } });
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudieron guardar los cambios."));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return <p className="py-10 text-center text-sm text-muted-foreground">Cargando la clase...</p>;
  }

  if (!datos) {
    return (
      <div className="space-y-6">
        <VolverButton to="/clases" />
        <ErrorBanner>{mensajeError || "No se encontró la clase."}</ErrorBanner>
      </div>
    );
  }

  return (
    <form onSubmit={guardar} noValidate className="space-y-6">
      <VolverButton to={`/clases/${id}`} />

      <header>
        <h1 className="text-2xl font-semibold tracking-tight">Modificar clase</h1>
        <p className="mt-1 text-sm text-muted-foreground">{datos.nombre}</p>
      </header>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <ClaseFormCard datos={datos} errores={errores} onChange={handleChange} />

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => navigate(`/clases/${id}`)}>
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
