import { useState } from "react";
import { Save } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { crearProfesor } from "../api/profesoresApi";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { validarProfesor } from "../model/profesor";
import { DatosPersonalesCard } from "@/components/ui/datos-personales-card";
import { DatosLaboralesCard } from "../components/DatosLaboralesCard";
import { CertificacionCard } from "@/components/ui/certificacion-card";

const VACIO = {
  nombre: "",
  apellido: "",
  dni: "",
  fechaNacimiento: "",
  calle: "",
  numeroCalle: "",
  codigoArea: "",
  numeroTelefono: "",
  email: "",
  password: "",
  tituloHabilitante: "",
  especialidad: "",
  valorPorHora: "",
  numeroCertificacion: "",
  entidadEmisora: "",
  fechaEmision: "",
  fechaVencimiento: "",
  archivoTituloUrl: "",
};

function aProfesorRequest(datos) {
  return {
    nombre: datos.nombre,
    apellido: datos.apellido,
    dni: datos.dni,
    fechaNacimiento: datos.fechaNacimiento || null,
    calle: datos.calle,
    numeroCalle: datos.numeroCalle,
    codigoArea: datos.codigoArea,
    numeroTelefono: datos.numeroTelefono,
    email: datos.email,
    password: datos.password,
    tituloHabilitante: datos.tituloHabilitante,
    certificacion: {
      numeroCertificacion: datos.numeroCertificacion,
      entidadEmisora: datos.entidadEmisora,
      fechaEmision: datos.fechaEmision,
      fechaVencimiento: datos.fechaVencimiento || null,
      archivoTituloUrl: datos.archivoTituloUrl || null,
    },
    especialidad: datos.especialidad || null,
    valorPorHora: datos.valorPorHora === "" ? null : Number(datos.valorPorHora),
  };
}

export default function NuevoProfesorPage() {
  const navigate = useNavigate();
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const handleChange = (campo) => (e) =>
    setDatos((prev) => ({ ...prev, [campo]: e.target.value }));

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");
    const nuevosErrores = validarProfesor(datos, { esAlta: true });
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    setGuardando(true);
    try {
      await crearProfesor(aProfesorRequest(datos));
      navigate("/profesores");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo registrar el profesor."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Nuevo profesor</h1>
        <VolverButton to="/profesores" />
      </div>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <form onSubmit={guardar} noValidate className="space-y-6">
        <DatosPersonalesCard datos={datos} errores={errores} onChange={handleChange} incluirPassword />
        <DatosLaboralesCard datos={datos} errores={errores} onChange={handleChange} />
        <CertificacionCard datos={datos} errores={errores} onChange={handleChange} />

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <Button type="button" variant="outline" onClick={() => navigate("/profesores")}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : <><Save size={18} strokeWidth={2.5} /> Registrar profesor</>}
          </Button>
        </div>
      </form>
    </div>
  );
}
