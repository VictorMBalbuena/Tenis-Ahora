import { useState } from "react";
import { Save } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { useCategoriasInsumo } from "../hooks/useInsumos";
import { crearInsumo } from "../api/insumosApi";
import { camposDeError, mensajeDeError } from "../api/errores";
import { validarInsumo } from "../model/insumo";
import { CategoriaField } from "../components/CategoriaField";

export default function NuevoInsumoPage() {
  const navigate = useNavigate();
  const { categorias } = useCategoriasInsumo();

  const [formData, setFormData] = useState({
    nombre: "",
    categoriaId: "",
    cantidadDisponible: 0,
    cantidadMinima: 0,
  });
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const categoriaId = formData.categoriaId || (categorias[0] ? String(categorias[0].id) : "");

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNumero = (campo) => (e) =>
    setFormData((prev) => ({ ...prev, [campo]: Number(e.target.value) }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMensajeError("");
    const nuevosErrores = validarInsumo({ ...formData, categoriaId });
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    setIsLoading(true);
    try {
      await crearInsumo({ ...formData, categoriaId: Number(categoriaId) });
      navigate("/insumos");
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo registrar el insumo."));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Nuevo insumo</h1>
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
                placeholder="Raquetas"
                value={formData.nombre}
                onChange={handleChange}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
              {errores.nombre && <p className="text-xs text-destructive">{errores.nombre}</p>}
            </div>

            <CategoriaField
              categorias={categorias}
              value={categoriaId}
              onChange={handleChange}
              error={errores.categoriaId}
            />

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Stock inicial</label>
              <input
                type="number"
                min="0"
                value={formData.cantidadDisponible}
                onChange={handleNumero("cantidadDisponible")}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
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
            {isLoading ? "Guardando..." : <><Save size={18} strokeWidth={2.5} /> Registrar insumo</>}
          </button>
        </div>
      </form>
    </div>
  );
}
