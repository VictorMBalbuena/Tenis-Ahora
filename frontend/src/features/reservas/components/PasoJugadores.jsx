import { useState } from "react";
import { AlertCircle, ArrowLeft, ArrowRight } from "lucide-react";
import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";
import { validarAcompaniantes, extraerMensajeError } from "../model/reserva";
import { FormularioJugadores } from "./FormularioJugadores";
import { ErrorBanner } from "@/components/ui/error-banner";

function inicializarAcompaniantes(modalidad, lista) {
  const esperado = modalidad === "DOBLES" ? 3 : 1;
  const base = Array.isArray(lista) ? lista : [];
  const resultado = [];

  for (let i = 0; i < esperado; i++) {
    if (base[i]) {
      const esSocio = base[i].tipo === "SOCIO" || base[i].personaId != null;
      resultado.push({
        tipo: esSocio ? "SOCIO" : "INVITADO",
        personaId: base[i].personaId ?? null,
        nombre: base[i].nombre ?? "",
        apellido: base[i].apellido ?? "",
        documento: base[i].documento ?? "",
      });
    } else {
      resultado.push({
        tipo: "INVITADO",
        personaId: null,
        nombre: "",
        apellido: "",
        documento: "",
      });
    }
  }

  return resultado;
}

/**
 * Paso 2 del Wizard de Reservas: modalidad (Single o Dobles), acompañantes y titular (si es personal).
 * Sigue fielmente reserva-02-jugadores.html.
 */
export function PasoJugadores({
  valor = {},
  onVolver = () => {},
  onSiguiente = () => {},
}) {
  const auth = useAuthStore();
  const rol = auth?.user?.rol || auth?.rol;
  const esPersonal = rol === "EMPLEADO" || rol === "ADMINISTRADOR";

  const [titularDni, setTitularDni] = useState(
    valor?.titularDni || valor?.titularSocio?.dni || ""
  );
  const [titularSocio, setTitularSocio] = useState(
    valor?.titularSocio ||
      (valor?.titularId && valor?.titularNombre
        ? {
            id: valor.titularId,
            nombre: valor.titularNombre,
            apellido: "",
            dni: valor.titularDni || "",
          }
        : null)
  );
  const [buscandoTitular, setBuscandoTitular] = useState(false);
  const [errorTitular, setErrorTitular] = useState(null);

  const [modalidad, setModalidad] = useState(valor?.modalidad || "SINGLE");
  const [acompaniantes, setAcompaniantes] = useState(() =>
    inicializarAcompaniantes(valor?.modalidad || "SINGLE", valor?.acompaniantes)
  );
  const [error, setError] = useState(null);

  const handleBuscarTitular = async (dniInput) => {
    const dniLimpio = String(dniInput || "").trim();
    if (!/^\d{7,8}$/.test(dniLimpio)) {
      setErrorTitular("El DNI debe tener 7 u 8 números.");
      setTitularSocio(null);
      return;
    }

    try {
      setBuscandoTitular(true);
      setErrorTitular(null);
      const res = await axiosClient.get("/socios", { params: { dni: dniLimpio } });
      const lista = res?.data?.contenido || (Array.isArray(res?.data) ? res.data : []);
      const socio = lista.find((s) => s.dni === dniLimpio) || lista[0];
      if (!socio) {
        setErrorTitular("No se encontró ningún socio registrado con el DNI ingresado.");
        setTitularSocio(null);
      } else if (socio.activo === false) {
        setErrorTitular("El socio con este DNI se encuentra inactivo o deshabilitado.");
        setTitularSocio(null);
      } else {
        setTitularSocio(socio);
        setErrorTitular(null);
        if (error) setError(null);
      }
    } catch (err) {
      setErrorTitular(
        extraerMensajeError(err, "No se pudo verificar el DNI del socio.")
      );
      setTitularSocio(null);
    } finally {
      setBuscandoTitular(false);
    }
  };

  const handleCambiarTitularDni = (nuevoDni) => {
    setTitularDni(nuevoDni);
    if (titularSocio && titularSocio.dni !== nuevoDni) {
      setTitularSocio(null);
    }
    if (errorTitular) setErrorTitular(null);
    if (error) setError(null);
  };

  const handleCambiarModalidad = (nuevaModalidad) => {
    setModalidad(nuevaModalidad);
    setAcompaniantes((prev) => inicializarAcompaniantes(nuevaModalidad, prev));
    if (error) setError(null);
  };

  const handleCambiarAcompaniante = (index, campo, nuevoValor) => {
    setAcompaniantes((prev) => {
      const actualizados = [...prev];
      if (typeof campo === "object" && campo !== null) {
        actualizados[index] = {
          ...actualizados[index],
          ...campo,
        };
      } else {
        actualizados[index] = {
          ...actualizados[index],
          [campo]: nuevoValor,
        };
      }
      return actualizados;
    });
    if (error) setError(null);
  };

  const handleContinuar = () => {
    if (esPersonal && !titularSocio?.id) {
      setError("Indicá y verificá el DNI de un socio titular activo para continuar.");
      return;
    }

    for (let i = 0; i < acompaniantes.length; i++) {
      const a = acompaniantes[i];
      if (a.tipo === "SOCIO" && (!a.personaId || Number(a.personaId) <= 0)) {
        setError("Buscá por DNI a cada acompañante socio.");
        return;
      }
    }

    const res = validarAcompaniantes(modalidad, acompaniantes, {
      titularId: esPersonal ? titularSocio?.id : undefined,
    });
    if (!res.valido) {
      const faltaDatosInvitado = acompaniantes.some(
        (a) =>
          a.tipo !== "SOCIO" &&
          a.personaId == null &&
          (!a?.nombre?.trim() || !a?.apellido?.trim())
      );
      if (faltaDatosInvitado || res.error?.includes("nombre y apellido")) {
        setError("Por favor completá nombre y apellido de todos los acompañantes invitados.");
      } else {
        setError(res.error || "Datos de acompañantes inválidos.");
      }
      return;
    }

    setError(null);
    const acompaniantesLimpios = acompaniantes.map((a) => {
      const esSocio = a.tipo === "SOCIO" || a.personaId != null;
      return {
        tipo: esSocio ? "SOCIO" : "INVITADO",
        personaId: esSocio && a.personaId ? Number(a.personaId) : null,
        nombre: a.nombre?.trim() || "",
        apellido: a.apellido?.trim() || "",
        documento: a.documento?.trim() || "",
      };
    });

    const payloadSiguiente = {
      modalidad,
      acompaniantes: acompaniantesLimpios,
    };

    if (esPersonal && titularSocio) {
      payloadSiguiente.titularId = titularSocio.id;
      payloadSiguiente.titularNombre = `${titularSocio.nombre || ""} ${titularSocio.apellido || ""}`.trim();
      payloadSiguiente.titularDni = titularSocio.dni;
      payloadSiguiente.titularSocio = titularSocio;
    }

    onSiguiente(payloadSiguiente);
  };

  return (
    <div className="space-y-5">
      {/* Formulario de selección y datos */}
      <FormularioJugadores
        modalidad={modalidad}
        acompaniantes={acompaniantes}
        titularDni={titularDni}
        titularSocio={titularSocio}
        titularError={errorTitular}
        buscandoTitular={buscandoTitular}
        esPersonal={esPersonal}
        onChangeModalidad={handleCambiarModalidad}
        onChangeAcompaniante={handleCambiarAcompaniante}
        onChangeTitularDni={handleCambiarTitularDni}
        onBuscarTitular={handleBuscarTitular}
      />

      {/* Alerta de validación */}
      {error && (
        <div role="alert"><ErrorBanner>{error}</ErrorBanner></div>
      )}

      {/* Acciones de navegación según mockup reserva-02-jugadores.html L361 */}
      <div className="flex items-center justify-between pt-4 border-t border-border">
        <button
          type="button"
          role="button"
          onClick={onVolver}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors border border-input bg-background hover:bg-muted px-8 py-3 cursor-pointer"
        >
          <ArrowLeft className="h-[18px] w-[18px] shrink-0" />
          <span>Volver</span>
        </button>

        <button
          type="button"
          role="button"
          onClick={handleContinuar}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors bg-primary text-primary-foreground hover:brightness-95 px-8 py-3 cursor-pointer shadow-xs"
        >
          <span>Continuar</span>
          <ArrowRight className="h-[18px] w-[18px] shrink-0" />
        </button>
      </div>
    </div>
  );
}

export default PasoJugadores;
