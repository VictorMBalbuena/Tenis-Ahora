import { useEffect, useState } from "react";
import { AlertTriangle, Save } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { actualizarProfesor, obtenerProfesor } from "../api/profesoresApi";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { validarProfesor } from "../model/profesor";
import { DatosPersonalesCard } from "@/components/ui/datos-personales-card";
import { DatosLaboralesCard } from "../components/DatosLaboralesCard";
import { CertificacionCard } from "@/components/ui/certificacion-card";

function aDatosDeFormulario(profesor) {
  return {
    nombre: profesor.nombre,
    apellido: profesor.apellido,
    dni: profesor.dni,
    fechaNacimiento: profesor.fechaNacimiento || "",
    calle: profesor.calle || "",
    numeroCalle: profesor.numeroCalle || "",
    codigoArea: profesor.codigoArea || "",
    numeroTelefono: profesor.numeroTelefono || "",
    email: profesor.email,
    tituloHabilitante: profesor.tituloHabilitante || "",
    especialidad: profesor.especialidad || "",
    valorPorHora: profesor.valorPorHora ?? "",
    numeroCertificacion: profesor.certificacion?.numeroCertificacion || "",
    entidadEmisora: profesor.certificacion?.entidadEmisora || "",
    fechaEmision: profesor.certificacion?.fechaEmision || "",
    fechaVencimiento: profesor.certificacion?.fechaVencimiento || "",
    archivoTituloUrl: profesor.certificacion?.archivoTituloUrl || "",
  };
}

const CAMPOS_CERTIFICACION = ["numeroCertificacion", "entidadEmisora", "fechaEmision", "fechaVencimiento", "archivoTituloUrl"];

// La certificación es todo o nada: si cambió algún campo suyo, se manda completa (el backend vuelve a PENDIENTE).
function soloLoQueCambio(datos, original) {
  const cambios = {};
  for (const campo of Object.keys(original)) {
    if (CAMPOS_CERTIFICACION.includes(campo)) continue;
    if (datos[campo] !== original[campo]) cambios[campo] = datos[campo];
  }
  if (cambios.valorPorHora !== undefined) {
    cambios.valorPorHora = cambios.valorPorHora === "" ? null : Number(cambios.valorPorHora);
  }

  const cambioCertificacion = CAMPOS_CERTIFICACION.some((campo) => datos[campo] !== original[campo]);
  if (cambioCertificacion) {
    cambios.certificacion = {
      numeroCertificacion: datos.numeroCertificacion,
      entidadEmisora: datos.entidadEmisora,
      fechaEmision: datos.fechaEmision,
      fechaVencimiento: datos.fechaVencimiento || null,
      archivoTituloUrl: datos.archivoTituloUrl || null,
    };
  }

  return cambios;
}

export default function EditarProfesorPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [cargando, setCargando] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [original, setOriginal] = useState(null);
  const [datos, setDatos] = useState(null);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    obtenerProfesor(id)
      .then((profesor) => {
        const iniciales = aDatosDeFormulario(profesor);
        setOriginal(iniciales);
        setDatos(iniciales);
      })
      .catch(() => setErrorGlobal(true))
      .finally(() => setCargando(false));
  }, [id]);

  const handleChange = (campo) => (e) =>
    setDatos((prev) => ({ ...prev, [campo]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");
    const nuevosErrores = validarProfesor(datos, { esAlta: false });
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    const cambios = soloLoQueCambio(datos, original);
    if (Object.keys(cambios).length === 0) {
      navigate("/profesores");
      return;
    }

    setGuardando(true);
    try {
      await actualizarProfesor(id, cambios);
      navigate("/profesores");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo guardar el profesor."));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">
        Cargando datos...
      </div>
    );
  }

  if (errorGlobal || !datos) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del profesor.</p>
        <button onClick={() => navigate("/profesores")} className="text-primary font-bold hover:underline">
          Volver a profesores
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">
          Editar {datos.nombre} {datos.apellido}
        </h1>
        <VolverButton to="/profesores" />
      </div>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <form onSubmit={guardar} noValidate className="space-y-6">
        <DatosPersonalesCard datos={datos} errores={errores} onChange={handleChange} />

        <DatosLaboralesCard datos={datos} errores={errores} onChange={handleChange} />
        <CertificacionCard datos={datos} errores={errores} onChange={handleChange} />

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <Button type="button" variant="outline" onClick={() => navigate("/profesores")}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : <><Save size={18} strokeWidth={2.5} /> Guardar cambios</>}
          </Button>
        </div>
      </form>
    </div>
  );
}
