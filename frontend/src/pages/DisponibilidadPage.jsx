import { hoyLocal } from "@/lib/fechas";
import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Wrench, AlertTriangle, Check, Calendar, ArrowRight, X } from "lucide-react";
import axiosClient from "@/config/axiosClient";
import { DateField } from "@/components/ui/date-field";
import { useDisponibilidad } from "@/features/canchas/hooks/useDisponibilidad";
import { GrillaDisponibilidad, HORARIOS, datosDeCelda } from "@/features/canchas/components/GrillaDisponibilidad";
import { BloqueoMantenimientoDialog } from "@/features/canchas/components/BloqueoMantenimientoDialog";
import { DesbloqueoMantenimientoDialog } from "@/features/canchas/components/DesbloqueoMantenimientoDialog";
import { KpiCard } from "@/components/ui/kpi-card";

export default function DisponibilidadPage() {
  const navigate = useNavigate();
  const [fechaActual, setFechaActual] = useState(hoyLocal);
  const { canchas, grillaData, cargando, error: errorGlobal, recargar } = useDisponibilidad(fechaActual);

  const [showModalMantenimiento, setShowModalMantenimiento] = useState(false);
  const [showModalDesbloqueo, setShowModalDesbloqueo] = useState(false);
  const [bloqueoADesbloquear, setBloqueoADesbloquear] = useState(null);
  const [turnoLibreSeleccionado, setTurnoLibreSeleccionado] = useState(null);
  const [isDesbloqueando, setIsDesbloqueando] = useState(false);
  const [errorDesbloqueo, setErrorDesbloqueo] = useState("");

  const [estadoValidacion, setEstadoValidacion] = useState(null);
  const [reservasConflicto, setReservasConflicto] = useState([]);

  const [bloqueoData, setBloqueoData] = useState({
    canchaId: "",
    motivo: "Mantenimiento general",
    fechaDesde: fechaActual,
    fechaHasta: fechaActual,
    horaInicio: "08:00",
    horaFin: "23:00",
    observaciones: "",
    notificarUsuarios: true
  });

  const handleBloqueoChange = (e) => {
    const { name, value, type, checked } = e.target;
    setBloqueoData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setEstadoValidacion(null);
  };

  const abrirModalMantenimiento = () => {
    setEstadoValidacion(null);
    setReservasConflicto([]);
    if (canchas.length > 0) {
      setBloqueoData(prev => ({ ...prev, canchaId: canchas[0].id }));
    }
    setShowModalMantenimiento(true);
  };

  const handleAplicarBloqueo = async () => {
    try {
      await axiosClient.post(`/canchas/${bloqueoData.canchaId}/bloqueos`, {
        motivo: bloqueoData.motivo,
        fechaDesde: bloqueoData.fechaDesde,
        fechaHasta: bloqueoData.fechaHasta,
        horaDesde: bloqueoData.horaInicio,
        horaHasta: bloqueoData.horaFin
      });
      setShowModalMantenimiento(false);
      recargar();
    } catch (error) {
      if (error.response?.status === 409) {
        setEstadoValidacion('error');
        const errorData = error.response.data;
        setReservasConflicto(errorData.conflictos || errorData.reservas || []);
      } else {
        console.error("Error de conexión:", error);
      }
    }
  };

  const handleTurnoClick = async ({ cancha, hora, estado }) => {
    if (estado === "Libre") {
      setTurnoLibreSeleccionado({ cancha, hora });
      return;
    }

    if (estado === "Clase") {
      navigate("/clases");
      return;
    }

    if (estado === "Reservada") {
      try {
        const resp = await axiosClient.get("/reservas", {
          params: { canchaId: cancha.id, fecha: fechaActual, page: 0, size: 50 },
        });
        const reservas = resp.data?.contenido || resp.data || [];
        const coincidente = reservas.find((r) => r.horaInicio && r.horaInicio.startsWith(hora));
        if (coincidente?.id) {
          navigate(`/reservas/${coincidente.id}`);
        } else {
          navigate(`/reservas?fecha=${fechaActual}&canchaId=${cancha.id}`);
        }
      } catch {
        navigate(`/reservas?fecha=${fechaActual}&canchaId=${cancha.id}`);
      }
      return;
    }

    if (estado === "Mantenimiento") {
      try {
        const resp = await axiosClient.get(`/canchas/${cancha.id}/bloqueos`, {
          params: { desde: fechaActual, hasta: fechaActual },
        });
        const bloqueos = Array.isArray(resp.data) ? resp.data : [];
        const b =
          bloqueos.find((bl) => {
            const desdeStr = String(bl.desde || "");
            const hastaStr = String(bl.hasta || "");
            return desdeStr.startsWith(hora) || (desdeStr <= hora && hastaStr > hora);
          }) || bloqueos[0];

        setBloqueoADesbloquear({
          canchaId: cancha.id,
          canchaNombre: `${cancha.identificador} - ${cancha.superficie || "Cancha"}`,
          bloqueoId: b?.id,
          motivo: b?.motivo || "Mantenimiento general",
          fechaDesde: b?.fecha || fechaActual,
          fechaHasta: b?.fecha || fechaActual,
          horaInicio: b?.desde ? String(b.desde).slice(0, 5) : hora,
          horaFin: b?.hasta ? String(b.hasta).slice(0, 5) : "23:00",
          observaciones: "Bloqueo registrado para mantenimiento de cancha.",
        });
        setErrorDesbloqueo("");
        setShowModalDesbloqueo(true);
      } catch (err) {
        console.error("Error al buscar bloqueos:", err);
        setBloqueoADesbloquear({
          canchaId: cancha.id,
          canchaNombre: cancha.identificador,
          fechaDesde: fechaActual,
          fechaHasta: fechaActual,
          horaInicio: hora,
          horaFin: "23:00",
          motivo: "Mantenimiento general",
        });
        setErrorDesbloqueo("");
        setShowModalDesbloqueo(true);
      }
    }
  };

  const handleCrearReservaDesdeTurno = () => {
    if (!turnoLibreSeleccionado) return;
    const { cancha, hora } = turnoLibreSeleccionado;
    setTurnoLibreSeleccionado(null);
    navigate(
      `/reservas/nuevo?canchaId=${cancha.id}&fecha=${fechaActual}&hora=${hora}&canchaNombre=${encodeURIComponent(
        cancha.identificador
      )}`,
      {
        state: {
          canchaId: cancha.id,
          canchaNombre: cancha.identificador,
          fecha: fechaActual,
          horaInicio: hora,
          precioPorHora: cancha.precioPorHora,
        },
      }
    );
  };

  const handleBloquearDesdeTurno = () => {
    if (!turnoLibreSeleccionado) return;
    const { cancha, hora } = turnoLibreSeleccionado;
    const horaNum = parseInt(hora.split(":")[0], 10);
    const finNum = Math.min(horaNum + 1, 23);
    const horaFin = `${finNum.toString().padStart(2, "0")}:00`;

    setBloqueoData({
      canchaId: String(cancha.id),
      motivo: "Mantenimiento general",
      fechaDesde: fechaActual,
      fechaHasta: fechaActual,
      horaInicio: hora,
      horaFin: horaFin,
      observaciones: "",
      notificarUsuarios: true,
    });
    setTurnoLibreSeleccionado(null);
    setEstadoValidacion(null);
    setReservasConflicto([]);
    setShowModalMantenimiento(true);
  };

  const handleEjecutarDesbloqueo = async () => {
    if (!bloqueoADesbloquear?.canchaId) return;
    setIsDesbloqueando(true);
    setErrorDesbloqueo("");
    try {
      if (bloqueoADesbloquear.bloqueoId) {
        await axiosClient.delete(
          `/canchas/${bloqueoADesbloquear.canchaId}/bloqueos/${bloqueoADesbloquear.bloqueoId}`
        );
      }
      setShowModalDesbloqueo(false);
      setBloqueoADesbloquear(null);
      recargar();
    } catch (err) {
      setErrorDesbloqueo(
        err.response?.data?.mensaje || "Error al intentar levantar el bloqueo."
      );
    } finally {
      setIsDesbloqueando(false);
    }
  };

  const resumen = useMemo(() => {
    let disponibles = 0;
    let reservados = 0;
    let clases = 0;
    let bloqueados = 0;

    canchas.forEach((cancha) => {
      HORARIOS.forEach((hora) => {
        const { estado } = datosDeCelda(grillaData, cancha.id, hora);
        if (estado === "Mantenimiento") {
          bloqueados++;
        } else if (estado === "Clase") {
          clases++;
        } else if (estado === "Reservada") {
          reservados++;
        } else {
          disponibles++;
        }
      });
    });

    return { disponibles, reservados, clases, bloqueados };
  }, [canchas, grillaData]);

  if (cargando) return <div className="w-full min-h-screen bg-background flex items-center justify-center text-gray-500 font-medium">Cargando grilla...</div>;

  if (errorGlobal) {
    return (
      <div className="w-full min-h-screen bg-background flex flex-col items-center justify-center gap-4 text-gray-500">
        <AlertTriangle size={40} className="text-orange-400" />
        <p>No se pudo cargar la disponibilidad de las canchas.</p>
        <button onClick={recargar} className="text-primary font-bold hover:underline">Reintentar</button>
      </div>
    );
  }

  return (
    <div className="space-y-6 relative">
      <div className="flex justify-between items-center">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Disponibilidad de canchas</h1>
        <div className="flex gap-3">
          <button onClick={abrirModalMantenimiento} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-5 py-2.5 rounded-full font-medium transition-colors shadow-sm">
            <Wrench size={18} /> Bloquear por mantenimiento
          </button>
          <button
            onClick={() => navigate(`/reservas/nuevo?fecha=${fechaActual}`)}
            className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-white px-5 py-2.5 rounded-full font-medium transition-colors shadow-sm cursor-pointer"
          >
            <Plus size={18} strokeWidth={2.5} /> Nueva reserva
          </button>
        </div>
      </div>

      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-4">
          <DateField
            name="fechaActual"
            value={fechaActual}
            onChange={(e) => setFechaActual(e.target.value)}
            className="w-auto"
          />
          <span onClick={() => setFechaActual(hoyLocal())} className="font-medium text-gray-500 ml-2 cursor-pointer hover:text-black transition-colors">Ir a Hoy</span>
        </div>
        <div className="flex gap-4 text-sm font-medium text-gray-600">
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full border border-gray-200 bg-white"></div> Libre</div>
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-emerald-100"></div> Reservada</div>
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-blue-100"></div> Clase</div>
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-orange-100"></div> Mantenimiento</div>
        </div>
      </div>

      <GrillaDisponibilidad
        canchas={canchas}
        grillaData={grillaData}
        onTurnoClick={handleTurnoClick}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 rounded-xl border border-border bg-card p-8 shadow-sm">
          <h3 className="font-bold text-lg mb-6">Resumen del dia</h3>
          <div className="grid grid-cols-4 gap-4">
            <KpiCard etiqueta="Turnos disponibles" valor={resumen.disponibles} />
            <KpiCard etiqueta="Turnos reservados" valor={resumen.reservados} />
            <KpiCard etiqueta="Ocupados por clases" valor={resumen.clases} />
            <KpiCard etiqueta="Bloqueados" valor={resumen.bloqueados} />
          </div>
        </div>
        <div className="rounded-xl border border-border bg-muted p-8 shadow-sm">
          <h3 className="font-bold text-lg mb-6">Reglas de la grilla</h3>
          <ul className="space-y-4 text-sm text-gray-600 font-medium">
            <li className="flex items-start gap-3"><Check size={18} className="text-primary mt-0.5 flex-shrink-0" /> Las canchas bloqueadas no se ofrecen.</li>
            <li className="flex items-start gap-3"><Check size={18} className="text-primary mt-0.5 flex-shrink-0" /> Fechas dentro de los proximos 30 dias.</li>
            <li className="flex items-start gap-3"><Check size={18} className="text-primary mt-0.5 flex-shrink-0" /> Turno maximo 2 horas consecutivas.</li>
          </ul>
        </div>
      </div>

      {turnoLibreSeleccionado && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-card rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative border border-border">
            <button
              type="button"
              onClick={() => setTurnoLibreSeleccionado(null)}
              className="absolute right-5 top-5 text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
            >
              <X size={20} />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-11 h-11 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold">
                <Calendar size={22} />
              </div>
              <div>
                <h3 className="text-xl font-bold text-foreground">
                  {turnoLibreSeleccionado.cancha.identificador}
                </h3>
                <p className="text-xs text-muted-foreground font-medium">
                  {fechaActual} • Turno {turnoLibreSeleccionado.hora} hs ({turnoLibreSeleccionado.cancha.superficie || "Cancha"})
                </p>
              </div>
            </div>

            <p className="text-sm text-muted-foreground mb-6">
              Este turno se encuentra libre. Elige la acción que deseas realizar:
            </p>

            <div className="space-y-3">
              <button
                type="button"
                onClick={handleCrearReservaDesdeTurno}
                className="w-full flex items-center justify-between p-4 rounded-2xl border-2 border-primary/20 bg-primary/5 hover:bg-primary/10 hover:border-primary transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-primary text-white">
                    <Plus size={20} strokeWidth={2.5} />
                  </div>
                  <div>
                    <div className="font-bold text-foreground group-hover:text-primary transition-colors">
                      Nueva reserva
                    </div>
                    <div className="text-xs text-muted-foreground">
                      Ir al asistente con cancha, fecha y horario precargados
                    </div>
                  </div>
                </div>
                <ArrowRight size={18} className="text-muted-foreground group-hover:text-primary transition-transform group-hover:translate-x-1" />
              </button>

              <button
                type="button"
                onClick={handleBloquearDesdeTurno}
                className="w-full flex items-center justify-between p-4 rounded-2xl border-2 border-orange-200 bg-orange-50/50 hover:bg-orange-100/60 hover:border-orange-400 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-orange-500 text-white">
                    <Wrench size={20} />
                  </div>
                  <div>
                    <div className="font-bold text-foreground group-hover:text-orange-700 transition-colors">
                      Bloquear por mantenimiento
                    </div>
                    <div className="text-xs text-muted-foreground">
                      Bloquear este horario para reparaciones o cuidado
                    </div>
                  </div>
                </div>
                <ArrowRight size={18} className="text-muted-foreground group-hover:text-orange-700 transition-transform group-hover:translate-x-1" />
              </button>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                type="button"
                onClick={() => setTurnoLibreSeleccionado(null)}
                className="text-sm font-semibold text-muted-foreground hover:text-foreground px-4 py-2 cursor-pointer"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      <BloqueoMantenimientoDialog
        abierto={showModalMantenimiento}
        canchas={canchas}
        bloqueoData={bloqueoData}
        onChange={handleBloqueoChange}
        estadoValidacion={estadoValidacion}
        reservasConflicto={reservasConflicto}
        onCerrar={() => setShowModalMantenimiento(false)}
        onAplicar={handleAplicarBloqueo}
      />

      <DesbloqueoMantenimientoDialog
        abierto={showModalDesbloqueo}
        bloqueoInfo={bloqueoADesbloquear}
        isLoading={isDesbloqueando}
        error={errorDesbloqueo}
        onCerrar={() => {
          setShowModalDesbloqueo(false);
          setBloqueoADesbloquear(null);
        }}
        onDesbloquear={handleEjecutarDesbloqueo}
      />
    </div>
  );
}
