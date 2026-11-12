import { useState, useEffect } from "react";
import { Save, AlertTriangle, Plus } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import { Button } from "@/components/ui/button";
import { SelectField } from "@/components/ui/select-field";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { CaracteristicasCard } from "@/features/canchas/components/CaracteristicasCard";
import { listarTiposCancha } from "@/features/canchas/api/tiposCanchaApi";
import { etiquetaSuperficie } from "@/features/canchas/model/cancha";

export default function EditarCanchaPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [mensajeErrorSubmit, setMensajeErrorSubmit] = useState("");
  const [tiposCancha, setTiposCancha] = useState([]);

  const [formData, setFormData] = useState({
    identificador: "",
    tipoCanchaId: "",
    superficie: "CEMENTO",
    capacidad: 4,
    precioPorHora: "",
    activa: true,
    techada: false,
    iluminacion: true,
    tribuna: false,
    torneos: false,
    observaciones: "",
  });

  useEffect(() => {
    const cargarDatos = async () => {
      try {
        const [respCancha, datosTipos] = await Promise.all([
          axiosClient.get(`/canchas/${id}`),
          listarTiposCancha().catch(() => []),
        ]);

        const datos = respCancha.data;
        const listaTipos = datosTipos || [];
        setTiposCancha(listaTipos);
        const tipoAsignado = listaTipos.find((t) => String(t.id) === String(datos.tipoCanchaId));

        setFormData({
          identificador: datos.identificador || "",
          tipoCanchaId: datos.tipoCanchaId ? String(datos.tipoCanchaId) : "",
          superficie: tipoAsignado?.superficie || datos.superficie || datos.tipoSuperficie || "",
          capacidad: tipoAsignado?.capacidad ?? datos.capacidad ?? 4,
          precioPorHora: datos.precioPorHora || "",
          activa: datos.activa !== false,
          techada: datos.techada === true,
          iluminacion: datos.iluminacion !== false,
          tribuna: datos.tribuna === true,
          torneos: datos.aptaTorneos === true,
          observaciones: datos.observaciones || "",
        });
      } catch {
        setErrorGlobal(true);
      } finally {
        setIsFetching(false);
      }
    };

    cargarDatos();
  }, [id]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleTipoCanchaChange = (tipoId) => {
    const tipo = tiposCancha.find((t) => String(t.id) === String(tipoId));
    if (tipo) {
      setFormData((prev) => ({
        ...prev,
        tipoCanchaId: tipoId,
        superficie: tipo.superficie || tipo.tipoSuperficie,
        capacidad: tipo.capacidad,
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        tipoCanchaId: "",
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMensajeErrorSubmit("");

    try {
      const payload = {
        identificador: formData.identificador,
        tipoSuperficie: formData.superficie?.trim() || null,
        superficie: formData.superficie?.trim() || null,
        capacidad: Number(formData.capacidad),
        precioPorHora: Number(formData.precioPorHora),
        activa: formData.activa,
        tipoCanchaId: formData.tipoCanchaId ? Number(formData.tipoCanchaId) : 0,
        techada: formData.techada,
        iluminacion: formData.iluminacion,
        tribuna: formData.tribuna,
        aptaTorneos: formData.torneos,
        observaciones: formData.observaciones?.trim() || null,
      };

      await axiosClient.patch(`/canchas/${id}`, payload);
      navigate("/canchas");
    } catch (error) {
      if (error.response?.status === 409) {
        setMensajeErrorSubmit("Ya existe otra cancha con ese identificador.");
      } else if (error.response) {
        setMensajeErrorSubmit("Ocurrió un error al intentar modificar la cancha.");
      } else {
        console.error("Error al guardar:", error);
        setMensajeErrorSubmit("Error de conexión. Revisá tu red.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetching)
    return (
      <div className="p-8 font-medium text-gray-500 w-full min-h-screen flex items-center justify-center">
        Cargando datos de la cancha...
      </div>
    );

  if (errorGlobal) {
    return (
      <div className="w-full min-h-screen bg-background flex flex-col items-center justify-center gap-4 text-gray-500">
        <AlertTriangle size={40} className="text-orange-400" />
        <p>No se encontraron los datos de esta cancha o hubo un error de conexión.</p>
        <button
          onClick={() => navigate("/canchas")}
          className="text-primary font-bold hover:underline"
        >
          Volver a canchas
        </button>
      </div>
    );
  }

  const handlePrecioKeyDown = (e) => {
    if (e.key === "ArrowUp") {
      e.preventDefault();
      const val = Number(formData.precioPorHora) || 0;
      setFormData((prev) => ({ ...prev, precioPorHora: String(val + 500) }));
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      const val = Number(formData.precioPorHora) || 0;
      setFormData((prev) => ({ ...prev, precioPorHora: String(Math.max(0, val - 500)) }));
    }
  };

  const isTipoFijado = Boolean(formData.tipoCanchaId);

  const opcionesTipos = [
    { value: "", label: "Elija un tipo de cancha" },
    ...tiposCancha.map((t) => ({
      value: String(t.id),
      label: `${t.nombre} (${etiquetaSuperficie(t.tipoSuperficie)} • Cap: ${t.capacidad})`,
    })),
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">
          Editar {formData.identificador}
        </h1>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            onClick={() => navigate("/canchas/tipos/nuevo")}
          >
            <Plus size={18} strokeWidth={2.5} />
            <span>Nuevo tipo</span>
          </Button>
          <VolverButton to="/canchas" />
        </div>
      </div>

      {mensajeErrorSubmit && <ErrorBanner className="mb-6">{mensajeErrorSubmit}</ErrorBanner>}

      <form noValidate onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-bold text-black">Identificación</h2>
            <span className="text-xs text-primary font-medium">
              Superficie y capacidad fijadas por el tipo de cancha seleccionado.
            </span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Nombre / Identificador
              </label>
              <input
                type="text"
                name="identificador"
                value={formData.identificador}
                onChange={handleChange}
                required
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Tipo de cancha
              </label>
              <SelectField
                name="tipoCanchaId"
                value={formData.tipoCanchaId || ""}
                onChange={(e) => handleTipoCanchaChange(e.target.value)}
                placeholder="Elija un tipo de cancha"
                opciones={opcionesTipos}
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Superficie <span className="text-xs text-muted-foreground font-normal">(fijada)</span>
              </label>
              <input
                type="text"
                readOnly
                disabled
                value={formData.superficie ? etiquetaSuperficie(formData.superficie) : "-"}
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] bg-muted/40 text-foreground font-medium text-[15px] cursor-not-allowed select-none"
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Capacidad <span className="text-xs text-muted-foreground font-normal">(fijada)</span>
              </label>
              <input
                type="text"
                readOnly
                disabled
                value={formData.capacidad ? `${formData.capacidad} jugadores` : "-"}
                className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] bg-muted/40 text-foreground font-medium text-[15px] cursor-not-allowed select-none"
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Precio por hora
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 font-semibold text-[15px] pointer-events-none">
                  $
                </span>
                <input
                  type="number"
                  name="precioPorHora"
                  min="0"
                  step="500"
                  value={formData.precioPorHora}
                  onChange={handleChange}
                  onKeyDown={handlePrecioKeyDown}
                  required
                  className="w-full pl-8 pr-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-[14px] font-semibold text-black">
                Disponibilidad de reservas
              </label>
              <SelectField
                name="activa"
                value={String(formData.activa)}
                onChange={(e) =>
                  setFormData({ ...formData, activa: e.target.value === "true" })
                }
                opciones={[
                  { value: "true", label: "Activa" },
                  {
                    value: "false",
                    label: "Deshabilitada (No se muestran nuevos turnos)",
                  },
                ]}
              />
            </div>
          </div>
        </div>

        <CaracteristicasCard formData={formData} onChange={handleChange} />

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <button
            type="button"
            onClick={() => navigate("/canchas")}
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
                <Save size={18} strokeWidth={2.5} /> Guardar cambios
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
