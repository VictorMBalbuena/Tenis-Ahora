import { useState, useEffect } from "react";
import { Save, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import { Button } from "@/components/ui/button";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { IdentificacionCard } from "@/features/canchas/components/IdentificacionCard";
import { DisponibilidadPorDefectoCard } from "@/features/canchas/components/DisponibilidadPorDefectoCard";
import { CaracteristicasCard } from "@/features/canchas/components/CaracteristicasCard";
import { listarTiposCancha } from "@/features/canchas/api/tiposCanchaApi";

export default function NuevaCanchaPage() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [mensajeError, setMensajeError] = useState("");
  const [tiposCancha, setTiposCancha] = useState([]);

  const [formData, setFormData] = useState({
    identificador: "",
    tipoCanchaId: "",
    superficie: "LADRILLO",
    capacidad: 4,
    precioPorHora: "",
    iluminacion: true,
    techada: false,
    tribuna: true,
    torneos: true,
    observaciones: "",
    apertura: "08:00",
    cierre: "23:00",
    duracion: "1",
    estado: "Disponible",
  });

  const cargarTipos = () => {
    listarTiposCancha()
      .then((data) => {
        const lista = data || [];
        setTiposCancha(lista);
        if (lista.length > 0) {
          setFormData((prev) => {
            if (!prev.tipoCanchaId) {
              const primero = lista[0];
              return {
                ...prev,
                tipoCanchaId: String(primero.id),
                superficie: primero.superficie || primero.tipoSuperficie,
                capacidad: primero.capacidad,
              };
            }
            return prev;
          });
        }
      })
      .catch((err) => console.error("Error al cargar tipos de cancha:", err));
  };

  useEffect(() => {
    cargarTipos();
  }, []);

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

  const errorHorario =
    formData.apertura && formData.cierre && formData.cierre <= formData.apertura
      ? "La hora de cierre debe ser posterior a la de apertura."
      : "";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMensajeError("");

    if (!formData.tipoCanchaId) {
      setMensajeError("Por favor seleccione un tipo de cancha.");
      return;
    }

    if (!formData.precioPorHora || Number(formData.precioPorHora) <= 0) {
      setMensajeError("El precio por hora debe ser mayor a 0.");
      return;
    }

    if (formData.cierre <= formData.apertura) {
      setMensajeError("La hora de cierre debe ser posterior a la hora de apertura.");
      return;
    }

    setIsLoading(true);

    try {
      const payload = {
        identificador: formData.identificador,
        tipoSuperficie: formData.superficie?.trim() || null,
        superficie: formData.superficie?.trim() || null,
        capacidad: Number(formData.capacidad),
        precioPorHora: Number(formData.precioPorHora),
        tipoCanchaId: formData.tipoCanchaId ? Number(formData.tipoCanchaId) : null,
        techada: Boolean(formData.techada),
        iluminacion: Boolean(formData.iluminacion),
        tribuna: Boolean(formData.tribuna),
        aptaTorneos: Boolean(formData.torneos),
        observaciones: formData.observaciones?.trim() || null,
      };

      const { data: canchaCreada } = await axiosClient.post("/canchas", payload);

      // Registrar disponibilidad semanal por defecto (lunes a domingo en la franja elegida)
      if (canchaCreada?.id && formData.apertura && formData.cierre) {
        try {
          const desde = formData.apertura.length === 5 ? `${formData.apertura}:00` : formData.apertura;
          const hasta = formData.cierre.length === 5 ? `${formData.cierre}:00` : formData.cierre;
          await axiosClient.post(`/canchas/${canchaCreada.id}/horarios`, {
            dias: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
            desde,
            hasta,
          });
        } catch (errHorario) {
          console.warn("No se pudo registrar el horario semanal inicial:", errHorario);
        }
      }

      navigate("/canchas");
    } catch (error) {
      if (error.response?.status === 409) {
        setMensajeError("Ya existe una cancha con ese identificador. Por favor elegí otro.");
      } else if (error.response?.data?.mensaje) {
        setMensajeError(error.response.data.mensaje);
      } else if (error.response) {
        setMensajeError("Verificá los datos ingresados. No se pudo crear la cancha.");
      } else {
        console.error("Error de conexión al crear cancha:", error);
        setMensajeError("Error de conexión. Revisá tu red e intentá nuevamente.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between pb-6">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Nueva cancha</h1>
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

      {mensajeError && <ErrorBanner className="mb-6">{mensajeError}</ErrorBanner>}

      <form noValidate onSubmit={handleSubmit} className="flex flex-1 flex-col gap-6 min-h-0">
        <IdentificacionCard
          formData={formData}
          onChange={handleChange}
          tiposCancha={tiposCancha}
          onTipoCanchaChange={handleTipoCanchaChange}
        />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <DisponibilidadPorDefectoCard
            formData={formData}
            onChange={handleChange}
            errorHorario={errorHorario}
          />
          <CaracteristicasCard formData={formData} onChange={handleChange} />
        </div>

        <div className="mt-auto flex items-center justify-end gap-4 border-t border-border pt-4">
          <button
            type="button"
            onClick={() => navigate("/canchas")}
            className="px-6 py-3 text-[15px] font-bold text-gray-700 hover:text-black transition-colors"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isLoading || Boolean(errorHorario)}
            className="bg-primary hover:bg-primary/90 text-white px-8 py-3 rounded-full font-bold text-[15px] transition-colors shadow-md flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              "Guardando..."
            ) : (
              <>
                <Save size={18} strokeWidth={2.5} /> Guardar cancha
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
