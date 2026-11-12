import { useEffect, useState } from "react";
import { AlertTriangle, Save } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { useCategoriasInsumo } from "../hooks/useInsumos";
import { actualizarInsumo, obtenerInsumo } from "../api/insumosApi";
import { camposDeError, mensajeDeError } from "../api/errores";
import { validarInsumo } from "../model/insumo";
import { CategoriaField } from "../components/CategoriaField";

export default function EditarInsumoPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { categorias } = useCategoriasInsumo();

  const [isFetching, setIsFetching] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [insumoOriginal, setInsumoOriginal] = useState(null);

  const [formData, setFormData] = useState({
    nombre: "",
    categoriaId: "",
    cantidadDisponible: 0,
    cantidadMinima: 0,
  });
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    obtenerInsumo(id)
      .then((datos) => {
        setInsumoOriginal(datos);
        setFormData({
          nombre: datos.nombre,
          categoriaId: String(datos.categoria?.id ?? ""),
          cantidadDisponible: datos.cantidadDisponible,
          cantidadMinima: datos.cantidadMinima,
        });
      })
      .catch(() => setErrorGlobal(true))
      .finally(() => setIsFetching(false));
  }, [id]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNumero = (campo) => (e) =>
    setFormData((prev) => ({ ...prev, [campo]: Number(e.target.value) }));
  function soloLoQueCambio() {
    const actual = {
      nombre: insumoOriginal.nombre,
      categoriaId: String(insumoOriginal.categoria?.id ?? ""),
      cantidadDisponible: insumoOriginal.cantidadDisponible,
      cantidadMinima: insumoOriginal.cantidadMinima,
    };
    const cambios = {};
    for (const campo of Object.keys(actual)) {
      if (formData[campo] !== actual[campo]) cambios[campo] = formData[campo];
    }
    if (cambios.categoriaId !== undefined) cambios.categoriaId = Number(cambios.categoriaId);
    return cambios;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMensajeError("");
    const nuevosErrores = validarInsumo(formData);
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    const cambios = soloLoQueCambio();
    if (Object.keys(cambios).length === 0) {
      navigate("/insumos");
      return;
    }

    setIsLoading(true);
    try {
      await actualizarInsumo(id, cambios);
      navigate("/insumos");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo guardar el insumo."));
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetching) {
    return (
      <div className="p-8 font-medium text-gray-500 w-full min-h-screen flex items-center justify-center">
        Cargando datos del insumo...
      </div>
    );
  }

  if (errorGlobal) {
    return (
      <div className="w-full min-h-screen bg-background flex flex-col items-center justify-center gap-4 text-gray-500">
        <AlertTriangle size={40} className="text-orange-400" />
        <p>No se encontraron los datos de este insumo o hubo un error de conexión.</p>
        <button onClick={() => navigate("/insumos")} className="text-primary font-bold hover:underline">
          Volver a insumos
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Editar {formData.nombre}</h1>
        <VolverButton to="/insumos" />
      </div>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
          <h2 className="text-lg font-bold text-black mb-6">Datos del insumo</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Nombre</label>
              <input
                type="text"
                name="nombre"
                value={formData.nombre}
                onChange={handleChange}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
              {errores.nombre && <p className="text-xs text-destructive">{errores.nombre}</p>}
            </div>

            <CategoriaField
              categorias={categorias}
              value={formData.categoriaId}
              onChange={handleChange}
              error={errores.categoriaId}
            />

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Stock disponible</label>
              <input
                type="number"
                min="0"
                value={formData.cantidadDisponible}
                onChange={handleNumero("cantidadDisponible")}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
              <p className="text-xs text-muted-foreground">Corregirlo queda en el historial como un ajuste.</p>
              {errores.cantidadDisponible && <p className="text-xs text-destructive">{errores.cantidadDisponible}</p>}
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Stock mínimo</label>
              <input
                type="number"
                min="0"
                value={formData.cantidadMinima}
                onChange={handleNumero("cantidadMinima")}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
              {errores.cantidadMinima && <p className="text-xs text-destructive">{errores.cantidadMinima}</p>}
            </div>
          </div>
        </div>

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <button
            type="button"
            onClick={() => navigate("/insumos")}
            className="px-6 py-3 text-[15px] font-bold text-gray-700 hover:text-black transition-colors"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="bg-primary hover:bg-primary/90 text-white px-8 py-3 rounded-full font-bold text-[15px] transition-colors shadow-md flex items-center gap-2 disabled:opacity-70"
          >
            {isLoading ? "Guardando..." : <><Save size={18} strokeWidth={2.5} /> Guardar cambios</>}
          </button>
        </div>
      </form>
    </div>
  );
}
