import { hoyLocal } from "@/lib/fechas";
import { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import axiosClient from "@/config/axiosClient";
import {
  Calendar,
  CreditCard,
  Eye,
  Loader2,
  Pencil,
  Plus,
  Search,
  Ban,
  Trash2,
  Play,
  CheckCircle2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { DateField } from "@/components/ui/date-field";
import { Input } from "@/components/ui/input";
import { buscarReservas, confirmarCancelacion } from "../api/reservasApi";
import {
  ESTADOS_RESERVA,
  formatearMoneda,
  formatearHorarioTurno,
  extraerMensajeError, formatearFecha } from "../model/reserva";
import { EliminarReservaDialog } from "./EliminarReservaDialog";
import { CancelarReservaDialog } from "./CancelarReservaDialog";
import { IniciarAlquilerDialog } from "./IniciarAlquilerDialog";
import { FinalizarAlquilerDialog } from "./FinalizarAlquilerDialog";

function titularDeReserva(reserva) {
  const titularJugador = reserva.jugadores?.find((j) => j.titular);
  if (titularJugador) {
    const nombre =
      `${titularJugador.nombre || ""} ${titularJugador.apellido || ""}`.trim();
    if (nombre) return nombre;
    if (titularJugador.documento) return `DNI ${titularJugador.documento}`;
  }
  if (reserva.titular) return reserva.titular;
  if (reserva.socioNombre) return reserva.socioNombre;
  if (reserva.usuario?.nombre) {
    return `${reserva.usuario.nombre} ${reserva.usuario.apellido || ""}`.trim();
  }
  return "—";
}

function obtenerIniciales(nombre) {
  if (!nombre || nombre === "—") return "—";
  const partes = nombre.trim().split(/\s+/);
  if (partes.length >= 2) {
    return (partes[0][0] + partes[1][0]).toUpperCase();
  }
  return partes[0].slice(0, 2).toUpperCase();
}

export function GestionReservasView() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  // Helper para leer estado inicial combinando URL y sessionStorage
  const [filtrosIniciales] = useState(() => {
    let guardados = {};
    try {
      const raw = sessionStorage.getItem("filtros_gestion_reservas");
      if (raw) guardados = JSON.parse(raw);
    } catch {
      // Ignorar error al parsear
    }

    const canchaIdParam = searchParams.get("canchaId");
    const fechaParam = searchParams.get("fecha");
    const estadoParam = searchParams.get("estado");
    const qParam =
      searchParams.get("q") ||
      searchParams.get("numero") ||
      searchParams.get("documento");
    const pageParam = searchParams.get("page");

    return {
      canchaId: canchaIdParam || guardados.canchaId || "TODOS",
      fecha: fechaParam !== null ? fechaParam : (guardados.fecha || ""),
      estado: estadoParam || guardados.estado || "TODOS",
      busqueda: qParam !== null ? qParam : (guardados.busqueda || ""),
      terminoBuscado:
        qParam !== null ? qParam : (guardados.terminoBuscado || ""),
      filtroRapido: guardados.filtroRapido || "TODOS",
      pagina: pageParam ? Number(pageParam) : (guardados.pagina || 0),
    };
  });

  const [aviso, setAviso] = useState(location.state?.aviso ?? null);
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState(filtrosIniciales.busqueda);
  const [terminoBuscado, setTerminoBuscado] = useState(filtrosIniciales.terminoBuscado);
  const [fecha, setFecha] = useState(filtrosIniciales.fecha);
  const [canchaId, setCanchaId] = useState(filtrosIniciales.canchaId);
  const [canchas, setCanchas] = useState([]);
  const [estado, setEstado] = useState(filtrosIniciales.estado);
  const [filtroRapido, setFiltroRapido] = useState(filtrosIniciales.filtroRapido);
  const [pagina, setPagina] = useState(filtrosIniciales.pagina);
  const [totalPaginas, setTotalPaginas] = useState(1);

  const [reservaAEliminar, setReservaAEliminar] = useState(null);
  const [modalEliminarAbierto, setModalEliminarAbierto] = useState(false);

  const [reservaACancelar, setReservaACancelar] = useState(null);
  const [modalCancelarAbierto, setModalCancelarAbierto] = useState(false);

  const [reservaAIniciar, setReservaAIniciar] = useState(null);
  const [modalIniciarAbierto, setModalIniciarAbierto] = useState(false);

  const [reservaAFinalizar, setReservaAFinalizar] = useState(null);
  const [modalFinalizarAbierto, setModalFinalizarAbierto] = useState(false);

  useEffect(() => {
    let cancelado = false;
    axiosClient
      .get("/canchas", { params: { size: 50 } })
      .then(({ data }) => {
        if (!cancelado) {
          const lista = Array.isArray(data?.contenido)
            ? data.contenido
            : Array.isArray(data)
              ? data
              : [];
          const activas = lista.filter((c) => c.activa !== false);
          setCanchas(activas.length > 0 ? activas : lista);
        }
      })
      .catch(() => {
        if (!cancelado) {
          setCanchas([]);
        }
      });
    return () => {
      cancelado = true;
    };
  }, []);

  const ejecutarBusqueda = useCallback(() => {
    let montado = true;
    const params = {};
    const q = terminoBuscado.trim();
    if (q) {
      if (/^r-\d+/i.test(q)) {
        params.numero = q.toUpperCase();
      } else {
        params.documento = q;
      }
    }
    if (fecha) {
      params.fecha = fecha;
    }
    if (canchaId && canchaId !== "TODOS") {
      params.canchaId = Number(canchaId);
    }
    if (estado && estado !== "TODOS") {
      params.estado = estado;
    }
    if (pagina > 0) {
      params.page = pagina;
    }

    buscarReservas(params)
      .then((data) => {
        if (!montado) return;
        const lista = Array.isArray(data?.contenido)
          ? data.contenido
          : Array.isArray(data)
            ? data
            : [];
        setReservas(lista);
        setTotalPaginas(data?.totalPaginas ?? 1);
        setError(null);
        setCargando(false);
      })
      .catch((err) => {
        if (!montado) return;
        setError(
          extraerMensajeError(
            err,
            "No se pudieron cargar las reservas de gestión.",
          ),
        );
        setCargando(false);
      });

    return () => {
      montado = false;
    };
  }, [terminoBuscado, fecha, canchaId, estado, pagina]);

  useEffect(() => {
    ejecutarBusqueda();
  }, [ejecutarBusqueda]);

  // Persistir filtros en sessionStorage y sincronizar searchParams en la URL
  useEffect(() => {
    try {
      sessionStorage.setItem(
        "filtros_gestion_reservas",
        JSON.stringify({
          terminoBuscado,
          busqueda,
          fecha,
          canchaId,
          estado,
          filtroRapido,
          pagina,
        }),
      );
    } catch {
      // Ignorar errores de storage
    }

    const nuevosParams = {};
    if (terminoBuscado.trim()) nuevosParams.q = terminoBuscado.trim();
    if (fecha) nuevosParams.fecha = fecha;
    if (canchaId && canchaId !== "TODOS") nuevosParams.canchaId = canchaId;
    if (estado && estado !== "TODOS") nuevosParams.estado = estado;
    if (pagina > 0) nuevosParams.page = String(pagina);

    setSearchParams(nuevosParams, { replace: true });
  }, [
    terminoBuscado,
    busqueda,
    fecha,
    canchaId,
    estado,
    filtroRapido,
    pagina,
    setSearchParams,
  ]);

  const handleBuscar = (e) => {
    e?.preventDefault();
    setCargando(true);
    setTerminoBuscado(busqueda);
    setPagina(0);
  };

  const handleLimpiarFiltros = () => {
    try {
      sessionStorage.removeItem("filtros_gestion_reservas");
    } catch {
      // Ignorar
    }
    setCargando(true);
    setBusqueda("");
    setTerminoBuscado("");
    setFecha("");
    setCanchaId("TODOS");
    setEstado("TODOS");
    setFiltroRapido("TODOS");
    setPagina(0);
    setSearchParams({}, { replace: true });
  };

  const handleSeleccionarFiltroRapido = (idFiltro) => {
    setFiltroRapido(idFiltro);
    setPagina(0);
    setCargando(true);
    if (idFiltro === "TODOS") {
      setEstado("TODOS");
      setFecha("");
    } else if (idFiltro === "HOY") {
      const hoyIso = hoyLocal();
      setFecha(hoyIso);
      setEstado("TODOS");
    } else if (idFiltro === "PROXIMAS") {
      setEstado("CONFIRMADA");
      setFecha("");
    } else if (idFiltro === "PENDIENTE_PAGO") {
      setEstado("PENDIENTE_PAGO");
      setFecha("");
    } else if (idFiltro === "CANCELADA") {
      setEstado("CANCELADA");
      setFecha("");
    }
  };

  const abrirCancelar = (reserva) => {
    setReservaACancelar(reserva);
    setModalCancelarAbierto(true);
  };

  const abrirEliminar = (reserva) => {
    setReservaAEliminar(reserva);
    setModalEliminarAbierto(true);
  };

  const abrirIniciar = (reserva) => {
    setReservaAIniciar(reserva);
    setModalIniciarAbierto(true);
  };

  const abrirFinalizar = (reserva) => {
    setReservaAFinalizar(reserva);
    setModalFinalizarAbierto(true);
  };

  const hayFiltros = Boolean(
    terminoBuscado || fecha || estado !== "TODOS" || canchaId !== "TODOS"
  );

  return (
    <div className="w-full min-w-0 space-y-6">
      {/* Encabezado: Título y botón de acción */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
            Gestión de reservas
          </h1>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <Button
            onClick={() => navigate("/reservas/nuevo")}
            className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-40 bg-primary text-primary-foreground hover:brightness-95 h-10 px-5"
          >
            <Plus className="h-[18px] w-[18px] shrink-0" />
            <span>Nueva reserva</span>
          </Button>
        </div>
      </div>

      {aviso && <SuccessBanner>{aviso}</SuccessBanner>}

      {/* Barra de Filtros Rápidos (Pills) según Mockup reservas-listado.html */}
      <div className="flex gap-1.5 overflow-x-auto scroll-thin">
        {[
          { id: "TODOS", label: "Todas" },
          { id: "HOY", label: "Hoy" },
          { id: "PROXIMAS", label: "Próximas" },
          { id: "PENDIENTE_PAGO", label: "Turnos retenidos" },
          { id: "CANCELADA", label: "Canceladas" },
        ].map((f) => {
          const activo = filtroRapido === f.id;
          return (
            <button
              key={f.id}
              type="button"
              onClick={() => handleSeleccionarFiltroRapido(f.id)}
              className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
                activo
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              {f.label}
            </button>
          );
        })}
      </div>

      {/* Barra de búsqueda y selectores */}
      <form
        onSubmit={handleBuscar}
        className="flex flex-col gap-3 sm:flex-row sm:items-center"
      >
        <div className="relative flex-1 sm:max-w-sm">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground h-[18px] w-[18px] shrink-0" />
          <Input
            id="input-busqueda-reservas"
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por código o DNI de socio..."
            className="h-10 rounded-full pl-11"
          />
        </div>

        <div className="-mx-4 flex gap-2 overflow-x-auto scroll-thin px-4 sm:mx-0 sm:px-0 items-center">
          <div className="flex items-center gap-1.5 shrink-0">
            <label htmlFor="filtro-estado-reservas" className="sr-only">
              Estado
            </label>
            <FiltroPill
              id="filtro-estado-reservas"
              valor={estado}
              onChange={(valor) => {
                setCargando(true);
                setEstado(valor);
                setFiltroRapido(valor);
                setPagina(0);
              }}
              opciones={[{ value: "TODOS", label: "Estado: Todos" }, { value: "CONFIRMADA", label: "Confirmada" }, { value: "PENDIENTE_PAGO", label: "Turno retenido" }, { value: "EN_CURSO", label: "En curso" }, { value: "CANCELADA", label: "Cancelada" }, { value: "FINALIZADA", label: "Finalizada" }]}
            />
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <label htmlFor="filtro-cancha-reservas" className="sr-only">
              Cancha
            </label>
            <FiltroPill
              id="filtro-cancha-reservas"
              valor={canchaId}
              onChange={(valor) => {
                setCargando(true);
                setCanchaId(valor);
                setPagina(0);
              }}
              opciones={[
                { value: "TODOS", label: "Cancha: Todas" },
                ...canchas.map((c) => ({
                  value: String(c.id),
                  label: c.identificador || `Cancha ${c.id}`,
                })),
              ]}
            />
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <label htmlFor="filtro-fecha-reservas" className="sr-only">
              Fecha
            </label>
            <DateField
              id="filtro-fecha-reservas"
              aria-label="Fecha"
              value={fecha}
              onChange={(e) => {
                setCargando(true);
                setFecha(e.target.value);
                setPagina(0);
              }}
              className="h-10 w-auto py-0 text-sm"
            />
          </div>

          <Button
            type="submit"
            className="rounded-full h-10 px-5 font-semibold shrink-0"
          >
            Buscar
          </Button>

          {hayFiltros && (
            <Button
              type="button"
              variant="outline"
              onClick={handleLimpiarFiltros}
              className="rounded-full h-10 px-5 font-medium shrink-0"
            >
              Limpiar
            </Button>
          )}
        </div>
      </form>

      {/* Feedback de error */}
      {error && (
        <div className="p-4 rounded-xl border border-destructive/20 bg-destructive/10 text-destructive text-sm flex items-center justify-between">
          <span>{error}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setCargando(true);
              ejecutarBusqueda();
            }}
          >
            Reintentar
          </Button>
        </div>
      )}

      {/* Contenido principal */}
      {cargando ? (
        <div className="py-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <p className="text-sm">Buscando reservas...</p>
        </div>
      ) : reservas.length === 0 ? (
        <div className="py-16 text-center rounded-2xl border border-dashed border-border bg-card/50 p-8 space-y-4">
          <Calendar className="w-12 h-12 text-muted-foreground mx-auto" />
          <div className="space-y-1">
            <h3 className="text-lg font-semibold text-foreground">
              No se encontraron reservas
            </h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              Probá ajustando el documento, código o los filtros de fecha y
              estado.
            </p>
          </div>
        </div>
      ) : (
        <>
          {/* Tarjetas Mobile conforme a mockup reservas-listado.html (gobernado por CSS puro) */}
          <div className="md:hidden space-y-3">
            {reservas.map((reserva) => {
              const numero =
                reserva.numero ||
                reserva.codigo ||
                `RES-${String(reserva.id).padStart(4, "0")}`;
              const nombreCancha =
                reserva.cancha ||
                reserva.canchaNombre ||
                (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
              const horario = formatearHorarioTurno(
                reserva.horaInicio,
                reserva.horaFin,
              );
              const titular = titularDeReserva(reserva);
              const cantJugadores =
                Array.isArray(reserva.jugadores) && reserva.jugadores.length > 0
                  ? reserva.jugadores.length
                  : reserva.modalidad === "DOBLES"
                    ? 4
                    : 2;

              const estadoInfo = ESTADOS_RESERVA[reserva.estado] || {
                label: reserva.estado || "Desconocido",
                clase: "bg-muted text-muted-foreground",
              };
              const puedeOperar =
                reserva.estado !== "CANCELADA" &&
                reserva.estado !== "FINALIZADA";

              return (
                <div
                  key={reserva.id}
                  className="rounded-xl border border-border bg-card p-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-foreground">
                        {numero} - {titular}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-muted-foreground">
                        {nombreCancha} - {formatearFecha(reserva.fecha)} {horario}
                      </p>
                    </div>
                    <div className="shrink-0">
                      <span
                        className={`inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs ${estadoInfo.clase}`}
                      >
                        {estadoInfo.label}
                      </span>
                    </div>
                  </div>

                  <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
                    <div>
                      <dt className="text-[11px] text-muted-foreground">
                        Jugadores
                      </dt>
                      <dd className="text-sm font-medium font-mono tabular-nums text-foreground">
                        {cantJugadores}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-[11px] text-muted-foreground">
                        Total
                      </dt>
                      <dd className="text-sm font-medium font-mono tabular-nums text-foreground">
                        {formatearMoneda(reserva.montoTotal)}
                      </dd>
                    </div>
                  </dl>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/reservas/${reserva.id}`)}
                      className="flex-1 rounded-full text-xs font-semibold h-8"
                    >
                      <Eye className="h-3.5 w-3.5 mr-1" />
                      <span>Ver detalle</span>
                    </Button>
                    {reserva.estado === "CONFIRMADA" && (
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => abrirIniciar(reserva)}
                        className="flex-1 rounded-full text-xs font-semibold h-8 bg-emerald-600 hover:bg-emerald-700 text-white"
                      >
                        <Play className="h-3.5 w-3.5 mr-1 fill-current" />
                        <span>Iniciar</span>
                      </Button>
                    )}
                    {reserva.estado === "EN_CURSO" && (
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => abrirFinalizar(reserva)}
                        className="flex-1 rounded-full text-xs font-semibold h-8 bg-blue-600 hover:bg-blue-700 text-white"
                      >
                        <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                        <span>Finalizar</span>
                      </Button>
                    )}
                    {reserva.estado === "PENDIENTE_PAGO" && (
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => navigate(`/reservas/${reserva.id}/cobro`)}
                        className="flex-1 rounded-full text-xs font-semibold h-8"
                      >
                        <CreditCard className="h-3.5 w-3.5 mr-1" />
                        <span>Cobrar seña</span>
                      </Button>
                    )}
                    {puedeOperar && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          navigate(`/reservas/editar/${reserva.id}`)
                        }
                        className="flex-1 rounded-full text-xs font-semibold h-8"
                      >
                        <Pencil className="h-3.5 w-3.5 mr-1" />
                        <span>Editar</span>
                      </Button>
                    )}
                    {puedeOperar && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => abrirCancelar(reserva)}
                        className="flex-1 rounded-full text-xs font-semibold h-8 text-destructive border-destructive/30 hover:bg-destructive/10"
                      >
                        <Ban className="h-3.5 w-3.5 mr-1" />
                        <span>Cancelar</span>
                      </Button>
                    )}
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => abrirEliminar(reserva)}
                      className="flex-1 rounded-full text-xs font-semibold h-8 text-destructive border-destructive/30 hover:bg-destructive/10"
                    >
                      <Trash2 className="h-3.5 w-3.5 mr-1" />
                      <span>Eliminar</span>
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Tabla Desktop conforme a mockup reservas-listado.html (gobernado por CSS puro) */}
          <div
            className="hidden md:block rounded-xl border border-border bg-card p-2 overflow-x-auto overflow-y-hidden scroll-thin"
            tabIndex={0}
          >
            <table className="w-full text-sm">
              <caption className="sr-only">Listado de reservas</caption>
              <thead>
                <tr>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground"
                  >
                    Código
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground"
                  >
                    Usuario
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground hidden lg:table-cell"
                  >
                    Cancha
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground"
                  >
                    Fecha y horario
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground hidden lg:table-cell"
                  >
                    Jug.
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground text-right"
                  >
                    Total
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground"
                  >
                    Estado
                  </th>
                  <th
                    scope="col"
                    className="px-4 pb-3 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground text-right"
                  >
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {reservas.map((reserva) => {
                  const numero =
                    reserva.numero ||
                    reserva.codigo ||
                    `RES-${String(reserva.id).padStart(4, "0")}`;
                  const nombreCancha =
                    reserva.cancha ||
                    reserva.canchaNombre ||
                    (reserva.canchaId
                      ? `Cancha ${reserva.canchaId}`
                      : "Cancha");
                  const horario = formatearHorarioTurno(
                    reserva.horaInicio,
                    reserva.horaFin,
                  );
                  const titular = titularDeReserva(reserva);
                  const iniciales = obtenerIniciales(titular);
                  const cantJugadores =
                    Array.isArray(reserva.jugadores) &&
                    reserva.jugadores.length > 0
                      ? reserva.jugadores.length
                      : reserva.modalidad === "DOBLES"
                        ? 4
                        : 2;

                  const estadoInfo = ESTADOS_RESERVA[reserva.estado] || {
                    label: reserva.estado || "Desconocido",
                    clase: "bg-muted text-muted-foreground",
                  };
                  const puedeOperar =
                    reserva.estado !== "CANCELADA" &&
                    reserva.estado !== "FINALIZADA";

                  return (
                    <tr
                      key={reserva.id}
                      className="border-t transition-colors hover:bg-muted/50"
                    >
                      <td className="px-4 py-3.5 align-middle text-sm">
                        <span className="whitespace-nowrap font-medium font-mono tabular-nums text-foreground">
                          {numero}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm">
                        <div className="flex items-center gap-3 min-w-0">
                          <span className="inline-flex h-9 w-9 text-[11px] shrink-0 items-center justify-center rounded-full bg-muted font-bold text-foreground">
                            {iniciales}
                          </span>
                          <div className="min-w-0">
                            <p className="truncate font-semibold text-foreground">
                              {titular}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm hidden lg:table-cell text-foreground">
                        {nombreCancha}
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-foreground">
                        <span className="font-mono tabular-nums whitespace-nowrap">
                          {formatearFecha(reserva.fecha)} {horario}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm hidden lg:table-cell text-foreground">
                        <span className="font-mono tabular-nums">
                          {cantJugadores}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-right text-foreground">
                        <span className="font-mono tabular-nums font-medium">
                          {formatearMoneda(reserva.montoTotal)}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm">
                        <span
                          className={`inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs ${estadoInfo.clase}`}
                        >
                          {estadoInfo.label}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 align-middle text-sm text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            aria-label="Ver detalle"
                            title="Ver detalle"
                            onClick={() => navigate(`/reservas/${reserva.id}`)}
                            className="h-9 w-9 rounded-full text-muted-foreground hover:bg-muted"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>

                          {reserva.estado === "PENDIENTE_PAGO" && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="Cobrar seña"
                              onClick={() => navigate(`/reservas/${reserva.id}/cobro`)}
                              className="h-9 w-9 rounded-full text-muted-foreground hover:bg-muted"
                            >
                              <CreditCard className="h-4 w-4" />
                            </Button>
                          )}

                          {reserva.estado === "CONFIRMADA" && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="Iniciar alquiler"
                              title="Iniciar alquiler (ingreso)"
                              onClick={() => abrirIniciar(reserva)}
                              className="h-9 w-9 rounded-full text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950/40"
                            >
                              <Play className="h-4 w-4 fill-current ml-0.5" />
                            </Button>
                          )}

                          {reserva.estado === "EN_CURSO" && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="Finalizar alquiler"
                              title="Finalizar alquiler (salida)"
                              onClick={() => abrirFinalizar(reserva)}
                              className="h-9 w-9 rounded-full text-blue-600 hover:text-blue-700 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                            >
                              <CheckCircle2 className="h-4 w-4" />
                            </Button>
                          )}

                          {puedeOperar && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="Editar"
                              onClick={() =>
                                navigate(`/reservas/editar/${reserva.id}`)
                              }
                              className="h-9 w-9 rounded-full text-muted-foreground hover:bg-muted"
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                          )}

                          {puedeOperar && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="Cancelar reserva"
                              onClick={() => abrirCancelar(reserva)}
                              className="h-9 w-9 rounded-full text-muted-foreground hover:bg-muted"
                            >
                              <Ban className="h-4 w-4" />
                            </Button>
                          )}

                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            aria-label="Eliminar"
                            onClick={() => abrirEliminar(reserva)}
                            className="h-9 w-9 rounded-full text-muted-foreground hover:text-danger-fg hover:bg-danger-bg"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Paginación conforme a mockup */}
      {!cargando && reservas.length > 0 && (
        <nav
          className="flex items-center justify-between gap-3 pt-2"
          aria-label="Paginación"
        >
          <p className="text-xs text-muted-foreground font-mono tabular-nums">
            Página {pagina + 1} de {Math.max(totalPaginas, 1)}
          </p>
          <div className="flex items-center gap-1.5">
            <Button
              type="button"
              variant="outline"
              size="icon"
              aria-label="Página anterior"
              disabled={pagina <= 0}
              onClick={() => {
                setCargando(true);
                setPagina((p) => Math.max(0, p - 1));
              }}
              className="h-9 w-9 rounded-full border border-input bg-background hover:bg-muted"
            >
              <span className="sr-only">Anterior</span>
              <svg
                aria-hidden="true"
                className="h-[18px] w-[18px] shrink-0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="m15 18-6-6 6-6" />
              </svg>
            </Button>
            <Button
              type="button"
              variant="outline"
              size="icon"
              aria-label="Página siguiente"
              disabled={pagina >= totalPaginas - 1}
              onClick={() => {
                setCargando(true);
                setPagina((p) => p + 1);
              }}
              className="h-9 w-9 rounded-full border border-input bg-background hover:bg-muted"
            >
              <span className="sr-only">Siguiente</span>
              <svg
                aria-hidden="true"
                className="h-[18px] w-[18px] shrink-0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="m9 18 6-6-6-6" />
              </svg>
            </Button>
          </div>
        </nav>
      )}

      {/* Modales de eliminación y cancelación */}
      <EliminarReservaDialog
        open={modalEliminarAbierto}
        reserva={reservaAEliminar}
        onOpenChange={(abierto) => {
          setModalEliminarAbierto(abierto);
          if (!abierto) setReservaAEliminar(null);
        }}
        onConfirm={async (r) => {
          const id = r?.id || reservaAEliminar?.id;
          if (id) {
            await confirmarCancelacion(id);
          }
          setCargando(true);
          ejecutarBusqueda();
        }}
      />

      <CancelarReservaDialog
        abierta={modalCancelarAbierto}
        reserva={reservaACancelar}
        onCerrar={() => {
          setModalCancelarAbierto(false);
          setReservaACancelar(null);
        }}
        onCanceladaExitosamente={() => {
          setAviso("Reserva cancelada.");
          setModalCancelarAbierto(false);
          setReservaACancelar(null);
          setCargando(true);
          ejecutarBusqueda();
        }}
      />

      <IniciarAlquilerDialog
        open={modalIniciarAbierto}
        reserva={reservaAIniciar}
        onOpenChange={(abierto) => {
          setModalIniciarAbierto(abierto);
          if (!abierto) setReservaAIniciar(null);
        }}
        onIniciado={() => {
          setCargando(true);
          ejecutarBusqueda();
        }}
      />

      <FinalizarAlquilerDialog
        open={modalFinalizarAbierto}
        reserva={reservaAFinalizar}
        onOpenChange={(abierto) => {
          setModalFinalizarAbierto(abierto);
          if (!abierto) setReservaAFinalizar(null);
        }}
        onFinalizado={() => {
          setCargando(true);
          ejecutarBusqueda();
        }}
      />
    </div>
  );
}

export default GestionReservasView;
