import { DateField } from "@/components/ui/date-field";
import { useReglasDeReserva } from "../hooks/useReglasDeReserva";
import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Check,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Search,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthStore } from "@/store/authStore";
import {
  crearReserva,
  modificarReserva,
  obtenerReserva,
} from "../api/reservasApi";
import { calcularHorasDuracion } from "../model/reserva";
import axiosClient from "@/config/axiosClient";

export function ReservaFormPage() {
  const reglas = useReglasDeReserva();
  const { id } = useParams();
  const esEdicion = Boolean(id);
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const esPersonal = user?.rol === "ADMINISTRADOR" || user?.rol === "EMPLEADO";
  const esSocio = !esPersonal;

  // Turno
  const [canchaId, setCanchaId] = useState("4");
  const [fecha, setFecha] = useState("2026-09-22");
  const [horaInicio, setHoraInicio] = useState("19:00");
  const [horaFin, setHoraFin] = useState("20:00");

  // Jugadores
  const [modalidad, setModalidad] = useState("SINGLE"); // "SINGLE" | "DOBLES"
  const [jugador1Nombre, setJugador1Nombre] = useState(
    esSocio
      ? user?.nombre && user?.apellido
        ? `${user.nombre} ${user.apellido}`
        : user?.nombreCompleto || user?.nombre || ""
      : "",
  );
  const [jugador1Dni, setJugador1Dni] = useState(
    esSocio ? user?.dni || "" : "",
  );
  const [buscandoSocio, setBuscandoSocio] = useState(false);
  const [socioEncontrado, setSocioEncontrado] = useState(null);
  const [errorSocio, setErrorSocio] = useState(null);

  const [jugador2Nombre, setJugador2Nombre] = useState("");
  const [jugador2Dni, setJugador2Dni] = useState("");

  const [jugador3Nombre, setJugador3Nombre] = useState("");
  const [jugador3Dni, setJugador3Dni] = useState("");

  const [jugador4Nombre, setJugador4Nombre] = useState("");
  const [jugador4Dni, setJugador4Dni] = useState("");

  // Insumos
  const [insumos, setInsumos] = useState({
    red: true,
    pelotas: true,
    raquetas: true,
    conos: false,
  });

  const [guardando, setGuardando] = useState(false);
  const [cargandoInicial, setCargandoInicial] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState(null);

  // Si es edición, cargar datos
  useEffect(() => {
    if (!esEdicion) {
      if (esSocio && user) {
        const nombreComp =
          user.nombre && user.apellido
            ? `${user.nombre} ${user.apellido}`
            : user.nombreCompleto || user.nombre || "";
        setJugador1Nombre(nombreComp);
        setJugador1Dni(user.dni || "");
      }
      return;
    }

    let cancelado = false;
    async function cargar() {
      try {
        setCargandoInicial(true);
        const data = await obtenerReserva(id);
        if (cancelado || !data) return;
        if (data.canchaId) setCanchaId(String(data.canchaId));
        if (data.fecha) setFecha(data.fecha);
        if (data.horaInicio) setHoraInicio(data.horaInicio);
        if (data.horaFin) setHoraFin(data.horaFin);
        if (data.modalidad) setModalidad(data.modalidad);

        const tit =
          data.jugadores?.find((j) => j.titular) || data.socioTitular || {};
        setJugador1Nombre(
          tit.nombre && tit.apellido
            ? `${tit.nombre} ${tit.apellido}`
            : tit.nombreCompleto || tit.nombre || "",
        );
        setJugador1Dni(tit.documento || tit.dni || "");

        const acomps = data.jugadores
          ? data.jugadores.filter((j) => !j.titular)
          : Array.isArray(data.acompaniantes)
            ? data.acompaniantes
            : [];
        if (acomps[0]) {
          setJugador2Nombre(
            acomps[0].nombre && acomps[0].apellido
              ? `${acomps[0].nombre} ${acomps[0].apellido}`
              : acomps[0].nombreCompleto || acomps[0].nombre || "",
          );
          setJugador2Dni(acomps[0].documento || acomps[0].dni || "");
        }
        if (acomps[1]) {
          setJugador3Nombre(
            acomps[1].nombre && acomps[1].apellido
              ? `${acomps[1].nombre} ${acomps[1].apellido}`
              : acomps[1].nombreCompleto || acomps[1].nombre || "",
          );
          setJugador3Dni(acomps[1].documento || acomps[1].dni || "");
        }
        if (acomps[2]) {
          setJugador4Nombre(
            acomps[2].nombre && acomps[2].apellido
              ? `${acomps[2].nombre} ${acomps[2].apellido}`
              : acomps[2].nombreCompleto || acomps[2].nombre || "",
          );
          setJugador4Dni(acomps[2].documento || acomps[2].dni || "");
        }
      } catch (err) {
        if (!cancelado)
          setErrorEnvio(err?.message || "Error al cargar la reserva");
      } finally {
        if (!cancelado) setCargandoInicial(false);
      }
    }
    cargar();
    return () => {
      cancelado = true;
    };
  }, [id, esEdicion, esSocio, user]);

  // Consulta /socios cuando el operador tipea el DNI
  const buscarSocioPorDni = async (dni) => {
    const limpio = (dni || "").trim();
    if (!limpio || limpio.length < 6) return;
    try {
      setBuscandoSocio(true);
      setErrorSocio(null);
      const res = await axiosClient.get("/socios", { params: { dni: limpio } });
      const data = res?.data;
      const lista = Array.isArray(data) ? data : data?.contenido || [];
      const encontrado = lista.find(
        (s) => String(s.dni).trim() === limpio && s.activo !== false,
      );

      if (encontrado) {
        const nom =
          encontrado.nombre && encontrado.apellido
            ? `${encontrado.nombre} ${encontrado.apellido}`
            : encontrado.nombreCompleto || encontrado.nombre || "";
        setJugador1Nombre(nom);
        setSocioEncontrado(encontrado);
      } else {
        setSocioEncontrado(null);
        setErrorSocio("No se encontró socio activo con ese DNI");
      }
    } catch {
      setErrorSocio("Error al consultar socio por DNI");
    } finally {
      setBuscandoSocio(false);
    }
  };

  const handleDniJugador1Change = (e) => {
    setJugador1Dni(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setGuardando(true);
      setErrorEnvio(null);

      const duracionHoras = calcularHorasDuracion(horaInicio, horaFin);
      const titularId = socioEncontrado?.id || (esSocio ? user?.id : undefined);

      const payload = {
        canchaId: Number(canchaId),
        fecha,
        horaInicio,
        horaFin,
        duracionHoras,
        modalidad,
        titularId,
        jugador1Nombre,
        jugador1Dni,
        acompaniantes: [
          { nombre: jugador2Nombre, dni: jugador2Dni, documento: jugador2Dni },
          ...(modalidad === "DOBLES"
            ? [
                {
                  nombre: jugador3Nombre,
                  dni: jugador3Dni,
                  documento: jugador3Dni,
                },
                {
                  nombre: jugador4Nombre,
                  dni: jugador4Dni,
                  documento: jugador4Dni,
                },
              ]
            : []),
        ],
        insumos,
      };

      if (esEdicion) {
        await modificarReserva(id, payload);
      } else {
        await crearReserva(payload);
      }
      navigate("/reservas", { state: { aviso: esEdicion ? "Reserva modificada." : "Turno retenido: falta pagar la seña para confirmarlo." } });
    } catch (err) {
      setErrorEnvio(
        err?.response?.data?.mensaje ||
          err?.response?.data?.message ||
          err?.message ||
          "Error al guardar la reserva",
      );
    } finally {
      setGuardando(false);
    }
  };

  if (cargandoInicial) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div
      data-testid="reserva-form-page"
      className="w-full min-w-0 space-y-6 pb-10"
    >
      {/* Encabezado y Breadcrumbs */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight sm:text-3xl text-foreground">
            {esEdicion ? "Editar reserva" : "Nuevo reserva"}
          </h1>
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            className="rounded-full"
            onClick={() => navigate("/reservas")}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            <span>Volver</span>
          </Button>
        </div>
      </div>

      {errorEnvio && (
        <div className="w-full rounded-xl border border-danger-bg bg-danger-bg/20 p-4 text-danger-fg">
          <p className="font-semibold text-sm">{errorEnvio}</p>
        </div>
      )}

            <form onSubmit={handleSubmit} className="w-full space-y-6">
        {/* Tarjeta 1: Turno */}
        <div className="rounded-xl border border-border bg-card shadow-sm">
          <div className="px-8 pt-8">
            <h2 className="text-lg font-bold text-foreground">
              Turno
            </h2>
          </div>
          <div className="px-8 pb-8 pt-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div className="space-y-1.5">
                <label htmlFor="form-cancha" className="text-sm font-semibold">
                  Cancha
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <div className="relative">
                  <select
                    id="form-cancha"
                    name="form-cancha"
                    value={canchaId}
                    onChange={(e) => setCanchaId(e.target.value)}
                    required
                    className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring appearance-none pr-9 cursor-pointer"
                  >
                    <option value="1">Cancha 1 - Ladrillo</option>
                    <option value="2">Cancha 2 - Ladrillo</option>
                    <option value="4">Cancha 4 - Cemento</option>
                    <option value="5">Cancha 5 - Pasto</option>
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <label htmlFor="form-fecha" className="text-sm font-semibold">
                  Fecha
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <DateField
                  id="form-fecha"
                  name="form-fecha"
                  value={fecha}
                  onChange={(e) => setFecha(e.target.value)}
                  aria-describedby="form-fecha-hint"
                />
                <p
                  id="form-fecha-hint"
                  className="text-xs text-muted-foreground"
                >
                  {reglas ? `Máximo ${reglas.antelacionMaximaDias} días de antelación` : ""}
                </p>
              </div>

              <div className="space-y-1.5">
                <label
                  htmlFor="form-hora-inicio"
                  className="text-sm font-semibold"
                >
                  Hora de inicio
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <input
                  id="form-hora-inicio"
                  name="form-hora-inicio"
                  type="time"
                  value={horaInicio}
                  onChange={(e) => setHoraInicio(e.target.value)}
                  required
                  className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>

              <div className="space-y-1.5">
                <label
                  htmlFor="form-hora-fin"
                  className="text-sm font-semibold"
                >
                  Hora de fin
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <input
                  id="form-hora-fin"
                  name="form-hora-fin"
                  type="time"
                  value={horaFin}
                  onChange={(e) => setHoraFin(e.target.value)}
                  required
                  aria-describedby="form-hora-fin-hint"
                  className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
                <p
                  id="form-hora-fin-hint"
                  className="text-xs text-muted-foreground"
                >
                  {reglas ? `Duración máxima ${reglas.duracionMaximaHoras} horas` : ""}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Tarjeta 2: Jugadores */}
        <div className="rounded-xl border border-border bg-card shadow-sm">
          <div className="px-8 pt-8">
            <h2 className="text-lg font-bold text-foreground">
              Jugadores
            </h2>
          </div>
          <div className="px-8 pb-8 pt-6 space-y-5">
            {/* Modalidad Radio */}
            <div className="flex flex-wrap gap-6">
              <label
                htmlFor="radio-single"
                className="flex items-center gap-2 cursor-pointer text-sm"
              >
                <input
                  type="radio"
                  id="radio-single"
                  name="modalidad"
                  checked={modalidad === "SINGLE"}
                  onChange={() => setModalidad("SINGLE")}
                  className="h-4 w-4 text-primary focus:ring-primary"
                />
                <span>2 jugadores (single)</span>
              </label>

              <label
                htmlFor="radio-dobles"
                className="flex items-center gap-2 cursor-pointer text-sm"
              >
                <input
                  type="radio"
                  id="radio-dobles"
                  name="modalidad"
                  checked={modalidad === "DOBLES"}
                  onChange={() => setModalidad("DOBLES")}
                  className="h-4 w-4 text-primary focus:ring-primary"
                />
                <span>4 jugadores (pareja)</span>
              </label>
            </div>

            {/* Inputs Jugadores */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Jugador 1 (titular) */}
              <div className="space-y-1.5">
                <label
                  htmlFor="form-jugador-1"
                  className="text-sm font-semibold"
                >
                  Jugador 1 (titular)
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <input
                  id="form-jugador-1"
                  name="form-jugador-1"
                  type="text"
                  value={jugador1Nombre}
                  onChange={(e) => setJugador1Nombre(e.target.value)}
                  readOnly={esSocio}
                  required
                  className={`flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                    esSocio
                      ? "bg-muted text-muted-foreground cursor-not-allowed"
                      : ""
                  }`}
                />
              </div>

              {/* DNI Jugador 1 */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label
                    htmlFor="form-dni-jugador-1"
                    className="text-sm font-semibold"
                  >
                    DNI jugador 1
                    <span aria-hidden="true" className="text-danger-fg">
                      {" "}
                      *
                    </span>
                  </label>
                  {buscandoSocio && (
                    <Loader2 className="h-3.5 w-3.5 animate-spin text-muted-foreground" />
                  )}
                </div>
                <div className="flex gap-2">
                  <input
                    id="form-dni-jugador-1"
                    name="form-dni-jugador-1"
                    type="text"
                    value={jugador1Dni}
                    onChange={handleDniJugador1Change}
                    onBlur={() => {
                      if (esPersonal && jugador1Dni.trim().length >= 6) {
                        buscarSocioPorDni(jugador1Dni);
                      }
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        if (esPersonal) buscarSocioPorDni(jugador1Dni);
                      }
                    }}
                    readOnly={esSocio}
                    required
                    className={`flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
                      esSocio
                        ? "bg-muted text-muted-foreground cursor-not-allowed"
                        : ""
                    }`}
                  />
                  {esPersonal && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => buscarSocioPorDni(jugador1Dni)}
                      className="shrink-0"
                    >
                      Verificar
                    </Button>
                  )}
                </div>
                {errorSocio && (
                  <p className="text-xs text-danger-fg flex items-center gap-1 mt-1">
                    <AlertCircle className="h-3 w-3 shrink-0" /> {errorSocio}
                  </p>
                )}
                {socioEncontrado && (
                  <p className="text-xs text-success-fg flex items-center gap-1 mt-1">
                    <CheckCircle2 className="h-3 w-3 shrink-0" /> Socio
                    verificado y activo
                  </p>
                )}
              </div>

              {/* Jugador 2 */}
              <div className="space-y-1.5">
                <label
                  htmlFor="form-jugador-2"
                  className="text-sm font-semibold"
                >
                  Jugador 2
                  <span aria-hidden="true" className="text-danger-fg">
                    {" "}
                    *
                  </span>
                </label>
                <input
                  id="form-jugador-2"
                  name="form-jugador-2"
                  type="text"
                  value={jugador2Nombre}
                  onChange={(e) => setJugador2Nombre(e.target.value)}
                  required
                  className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>

              {/* DNI Jugador 2 */}
              <div className="space-y-1.5">
                <label
                  htmlFor="form-dni-jugador-2"
                  className="text-sm font-semibold"
                >
                  DNI jugador 2
                  <span className="font-normal text-muted-foreground"> (opcional)</span>
                </label>
                <input
                  id="form-dni-jugador-2"
                  name="form-dni-jugador-2"
                  type="text"
                  value={jugador2Dni}
                  onChange={(e) => setJugador2Dni(e.target.value)}
                  className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>

              {/* Jugador 3 y 4 en modo Dobles */}
              {modalidad === "DOBLES" && (
                <>
                  <div className="space-y-1.5">
                    <label
                      htmlFor="form-jugador-3"
                      className="text-sm font-semibold"
                    >
                      Jugador 3
                      <span aria-hidden="true" className="text-danger-fg">
                        {" "}
                        *
                      </span>
                    </label>
                    <input
                      id="form-jugador-3"
                      name="form-jugador-3"
                      type="text"
                      value={jugador3Nombre}
                      onChange={(e) => setJugador3Nombre(e.target.value)}
                      required
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label
                      htmlFor="form-dni-jugador-3"
                      className="text-sm font-semibold"
                    >
                      DNI jugador 3
                      <span className="font-normal text-muted-foreground"> (opcional)</span>
                    </label>
                    <input
                      id="form-dni-jugador-3"
                      name="form-dni-jugador-3"
                      type="text"
                      value={jugador3Dni}
                      onChange={(e) => setJugador3Dni(e.target.value)}
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label
                      htmlFor="form-jugador-4"
                      className="text-sm font-semibold"
                    >
                      Jugador 4
                      <span aria-hidden="true" className="text-danger-fg">
                        {" "}
                        *
                      </span>
                    </label>
                    <input
                      id="form-jugador-4"
                      name="form-jugador-4"
                      type="text"
                      value={jugador4Nombre}
                      onChange={(e) => setJugador4Nombre(e.target.value)}
                      required
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label
                      htmlFor="form-dni-jugador-4"
                      className="text-sm font-semibold"
                    >
                      DNI jugador 4
                      <span className="font-normal text-muted-foreground"> (opcional)</span>
                    </label>
                    <input
                      id="form-dni-jugador-4"
                      name="form-dni-jugador-4"
                      type="text"
                      value={jugador4Dni}
                      onChange={(e) => setJugador4Dni(e.target.value)}
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Tarjeta 3: Insumos asignados */}
        <div className="rounded-xl border border-border bg-card shadow-sm">
          <div className="px-8 pt-8">
            <h2 className="text-lg font-bold text-foreground">
              Insumos asignados
            </h2>
          </div>
          <div className="px-8 pb-8 pt-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <label
                htmlFor="check-red"
                className="flex items-start gap-3 cursor-pointer"
              >
                <input
                  type="checkbox"
                  id="check-red"
                  checked={insumos.red}
                  onChange={(e) =>
                    setInsumos((prev) => ({ ...prev, red: e.target.checked }))
                  }
                  className="h-4 w-4 rounded border-input text-primary focus:ring-primary mt-0.5"
                />
                <span className="text-sm leading-snug">Red (1 unidad)</span>
              </label>

              <label
                htmlFor="check-pelotas"
                className="flex items-start gap-3 cursor-pointer"
              >
                <input
                  type="checkbox"
                  id="check-pelotas"
                  checked={insumos.pelotas}
                  onChange={(e) =>
                    setInsumos((prev) => ({
                      ...prev,
                      pelotas: e.target.checked,
                    }))
                  }
                  className="h-4 w-4 rounded border-input text-primary focus:ring-primary mt-0.5"
                />
                <span className="text-sm leading-snug">
                  Pelotas de tenis (1 tubo)
                </span>
              </label>

              <label
                htmlFor="check-raquetas"
                className="flex items-start gap-3 cursor-pointer"
              >
                <input
                  type="checkbox"
                  id="check-raquetas"
                  checked={insumos.raquetas}
                  onChange={(e) =>
                    setInsumos((prev) => ({
                      ...prev,
                      raquetas: e.target.checked,
                    }))
                  }
                  className="h-4 w-4 rounded border-input text-primary focus:ring-primary mt-0.5"
                />
                <span className="text-sm leading-snug">
                  Raquetas (2 unidades)
                </span>
              </label>

              <label
                htmlFor="check-conos"
                className="flex items-start gap-3 cursor-pointer"
              >
                <input
                  type="checkbox"
                  id="check-conos"
                  checked={insumos.conos}
                  onChange={(e) =>
                    setInsumos((prev) => ({ ...prev, conos: e.target.checked }))
                  }
                  className="h-4 w-4 rounded border-input text-primary focus:ring-primary mt-0.5"
                />
                <span className="text-sm leading-snug">
                  Conos de entrenamiento
                </span>
              </label>
            </div>
          </div>
        </div>

        {/* Acciones */}
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="ghost"
            className="rounded-full"
            onClick={() => navigate("/reservas")}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            disabled={guardando}
            className="rounded-full gap-2"
          >
            {guardando ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Check className="h-4 w-4" />
            )}
            <span>Guardar</span>
          </Button>
        </div>
      </form>
    </div>
  );
}

export default ReservaFormPage;
