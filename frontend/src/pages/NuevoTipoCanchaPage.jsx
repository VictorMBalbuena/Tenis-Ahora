import { useState, useEffect } from "react";
import { Save } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SelectField } from "@/components/ui/select-field";
import { SUPERFICIES_SUGERIDAS } from "@/features/canchas/model/cancha";
import { crearTipoCancha, listarTiposCancha } from "@/features/canchas/api/tiposCanchaApi";

export default function NuevoTipoCanchaPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    nombre: "",
    superficie: "",
    capacidad: 4,
    descripcion: "",
  });

  const [superficiesExistentes, setSuperficiesExistentes] = useState([]);
  const [modoSuperficie, setModoSuperficie] = useState("existente"); // "existente" | "nueva"
  const [nuevaSuperficie, setNuevaSuperficie] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [mensajeError, setMensajeError] = useState("");

  useEffect(() => {
    listarTiposCancha()
      .then((tipos) => {
        const delBackend = Array.isArray(tipos)
          ? tipos.map((t) => t.superficie || t.tipoSuperficie).filter(Boolean)
          : [];
        const todas = Array.from(new Set([...delBackend, ...SUPERFICIES_SUGERIDAS]));
        setSuperficiesExistentes(todas);
        if (todas.length > 0 && !formData.superficie) {
          setFormData((prev) => ({ ...prev, superficie: todas[0] }));
        }
      })
      .catch(() => {
        setSuperficiesExistentes(SUPERFICIES_SUGERIDAS);
        if (!formData.superficie) {
          setFormData((prev) => ({ ...prev, superficie: SUPERFICIES_SUGERIDAS[0] }));
        }
      });
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMensajeError("");

    if (!formData.nombre.trim()) {
      setMensajeError("El nombre del tipo de cancha es obligatorio.");
      return;
    }

    if (!formData.superficie?.trim()) {
      setMensajeError("La superficie es obligatoria y no puede estar vacía.");
      return;
    }

    const cap = Number(formData.capacidad);
    if (cap !== 2 && cap !== 4) {
      setMensajeError("La capacidad debe ser de 2 o 4 jugadores.");
      return;
    }

    setIsLoading(true);

    try {
      await crearTipoCancha({
        nombre: formData.nombre.trim(),
        superficie: formData.superficie.trim(),
        tipoSuperficie: formData.superficie.trim(),
        capacidad: cap,
        descripcion: formData.descripcion?.trim() || null,
      });

      navigate("/canchas/tipos");
    } catch (error) {
      if (error.response?.status === 409) {
        setMensajeError("Ya existe un tipo de cancha con ese nombre. Por favor elegí otro.");
      } else if (error.response?.data?.mensaje) {
        setMensajeError(error.response.data.mensaje);
      } else if (error.response) {
        setMensajeError("Verificá los datos ingresados. No se pudo registrar el tipo de cancha.");
      } else {
        console.error("Error al registrar tipo de cancha:", error);
        setMensajeError("Error de conexión. Revisá tu red e intentá nuevamente.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Nuevo tipo de cancha</h1>
        <VolverButton to="/canchas/tipos" />
      </div>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
          <h2 className="text-lg font-bold text-black mb-6">Datos del tipo de cancha</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Nombre</label>
              <input
                type="text"
                name="nombre"
                placeholder="Ladrillo Dobles"
                value={formData.nombre}
                onChange={handleChange}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="block text-[14px] font-semibold text-black">
                  Superficie <span className="text-destructive">*</span>
                </label>
                {modoSuperficie === "nueva" ? (
                  <button
                    type="button"
                    onClick={() => {
                      setModoSuperficie("existente");
                      const defaultSup = superficiesExistentes[0] || "";
                      setFormData((prev) => ({ ...prev, superficie: defaultSup }));
                    }}
                    className="text-xs text-primary font-semibold hover:underline cursor-pointer"
                  >
                    Elegir existente
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      setModoSuperficie("nueva");
                      setFormData((prev) => ({ ...prev, superficie: nuevaSuperficie }));
                    }}
                    className="text-xs text-primary font-semibold hover:underline cursor-pointer"
                  >
                    + Nueva superficie
                  </button>
                )}
              </div>

              {modoSuperficie === "existente" ? (
                <select
                  name="superficieSelect"
                  value={formData.superficie}
                  onChange={(e) => {
                    if (e.target.value === "__OTRA__") {
                      setModoSuperficie("nueva");
                      setFormData((prev) => ({ ...prev, superficie: nuevaSuperficie }));
                    } else {
                      setFormData((prev) => ({ ...prev, superficie: e.target.value }));
                    }
                  }}
                  className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px] bg-white cursor-pointer"
                >
                  <option value="" disabled>Seleccioná una superficie...</option>
                  {superficiesExistentes.map((sup) => (
                    <option key={sup} value={sup}>
                      {sup}
                    </option>
                  ))}
                  <option value="__OTRA__">+ Ingresar otra superficie nueva...</option>
                </select>
              ) : (
                <input
                  type="text"
                  name="superficie"
                  placeholder="Ej. Polvo de ladrillo teñido (azul), Resina multicapa..."
                  value={nuevaSuperficie}
                  onChange={(e) => {
                    setNuevaSuperficie(e.target.value);
                    setFormData((prev) => ({ ...prev, superficie: e.target.value }));
                  }}
                  required
                  autoFocus
                  className="w-full px-5 py-3.5 border border-primary/50 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px] bg-primary/5"
                />
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Capacidad (jugadores)</label>
              <SelectField
                name="capacidad"
                value={formData.capacidad}
                onChange={handleChange}
                opciones={[
                  { value: 2, label: "2 jugadores (Singles)" },
                  { value: 4, label: "4 jugadores (Dobles / Singles)" },
                ]}
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">Descripción (opcional)</label>
              <input
                type="text"
                name="descripcion"
                placeholder="Cancha rápida de cemento para 4 jugadores"
                value={formData.descripcion}
                onChange={handleChange}
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
            </div>
          </div>
        </div>

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-6 py-3 text-[15px] font-bold text-gray-700 hover:text-black transition-colors"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="bg-primary hover:bg-primary/90 text-white px-8 py-3 rounded-full font-bold text-[15px] transition-colors shadow-md flex items-center gap-2 disabled:opacity-70"
          >
            {isLoading ? (
              "Guardando..."
            ) : (
              <>
                <Save size={18} strokeWidth={2.5} /> Registrar tipo de cancha
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}

