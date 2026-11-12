import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { Button } from "@/components/ui/button";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import CobroQR from "@/pages/CobroQR";
import { EstadoCobro, MedioPago, validarCobro } from "../model/cobro";
import { cobrarSeniaDeReserva, listarReservasConSaldo } from "../api/cobrosApi";
import { CobroFormCard } from "../components/CobroFormCard";

const VACIO = {
  reservaId: "",
  medioPago: "",
  numeroOperacion: "",
  cuotas: "",
  aprobado: true,
};

function aCobroRequest(datos) {
  const usaTerminal = datos.medioPago === MedioPago.DEBITO || datos.medioPago === MedioPago.CREDITO;
  return {
    medioPago: datos.medioPago,
    numeroOperacion: usaTerminal ? datos.numeroOperacion.trim() : null,
    cuotas: datos.medioPago === MedioPago.CREDITO && datos.cuotas !== "" ? Number(datos.cuotas) : null,
    aprobado: usaTerminal ? datos.aprobado : true,
  };
}

/** Cobrar la seña de una reserva (CU51). El monto y los descuentos los calcula siempre el backend. */
export default function NuevoCobroPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [datos, setDatos] = useState(() => ({ ...VACIO, reservaId: params.get("reserva") ?? "" }));
  const [reservas, setReservas] = useState([]);
  const [errores, setErrores] = useState({});
  const [mensajeError, setMensajeError] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [cobroConQr, setCobroConQr] = useState(null);

  useEffect(() => {
    listarReservasConSaldo().then(setReservas).catch(() => setReservas([]));
  }, []);

  const handleChange = (campo) => (e) => setDatos((prev) => ({ ...prev, [campo]: e.target.value }));
  const handleCambiarAprobado = (aprobado) => setDatos((prev) => ({ ...prev, aprobado }));

  async function guardar(e) {
    e.preventDefault();
    setMensajeError("");
    const nuevosErrores = validarCobro(datos);
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) return;

    setGuardando(true);
    try {
      const cobro = await cobrarSeniaDeReserva(datos.reservaId, aCobroRequest(datos));
      if (cobro.estado === EstadoCobro.RECHAZADO) {
        setDatos((prev) => ({ ...prev, numeroOperacion: "", aprobado: true }));
        setMensajeError("La tarjeta fue rechazada. Elegí otro medio de pago.");
      } else if (datos.medioPago === MedioPago.QR) {
        setCobroConQr(cobro);
      } else {
        navigate("/cobros");
      }
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setMensajeError(mensajeDeError(error, "No se pudo registrar el cobro."));
    } finally {
      setGuardando(false);
    }
  }

  if (cobroConQr) {
    return (
      <CobroQR
        reservaInfo={{ subtitulo: `Reserva #${datos.reservaId}`, importe: Number(cobroConQr.montoFinal ?? 0) }}
        cobroId={cobroConQr.id}
        onCancelar={() => navigate("/cobros")}
        onCambiarMedio={(nuevoMedio) => {
          setDatos((prev) => ({ ...prev, medioPago: nuevoMedio }));
          setCobroConQr(null);
        }}
        onPagoExitoso={() => navigate("/cobros")}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Cobrar seña</h1>
        <VolverButton to="/cobros" />
      </div>

      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <form onSubmit={guardar} noValidate className="space-y-6">
        <CobroFormCard
          datos={datos}
          errores={errores}
          onChange={handleChange}
          onCambiarAprobado={handleCambiarAprobado}
          reservas={reservas}
        />

        <div className="flex justify-end items-center gap-4 pt-4 pb-12">
          <Button type="button" variant="outline" onClick={() => navigate("/cobros")}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : <><Save size={18} strokeWidth={2.5} /> Registrar cobro</>}
          </Button>
        </div>
      </form>
    </div>
  );
}
