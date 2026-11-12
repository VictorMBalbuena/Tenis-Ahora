import { useEffect, useMemo, useState } from "react";
import { Plus } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { ErrorBanner } from "@/components/ui/error-banner";
import { useAuthStore } from "@/store/authStore";
import { mensajeDeError } from "@/lib/erroresApi";
import { DIAS_SEMANA, OPCIONES_ESTADO, resumirEntrenamientos } from "../model/entrenamiento";
import { useEntrenamientos } from "../hooks/useEntrenamientos";
import { imprimirEntrenamiento } from "../api/entrenamientosApi";
import { EntrenamientosTabla } from "../components/EntrenamientosTabla";
import { EliminarEntrenamientoDialog } from "../components/EliminarEntrenamientoDialog";
import { EntrenamientosDelSocio } from "../components/EntrenamientosDelSocio";
import { KpiCard } from "@/components/ui/kpi-card";
import { CertificacionPendienteState } from "@/components/ui/certificacion-pendiente-state";

const TODOS = "TODOS";

/**
 * Listado de entrenamientos (CU39), con alta, baja, modificación e impresión.
 *
 * Si quien entra es un entrenador, muestra solo los suyos: es la vista de
 * "mis entrenamientos y alumnos" (CU84).
 */
function EntrenamientosDelPersonal() {
  const navigate = useNavigate();
  const location = useLocation();
  const usuario = useAuthStore((estado) => estado.user);

  const esEntrenador = usuario?.rol === "ENTRENADOR";

  const [estado, setEstado] = useState(TODOS);
  const [dia, setDia] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState(location.state?.aviso ?? "");
  const [errorImprimir, setErrorImprimir] = useState("");
  const [aEliminar, setAEliminar] = useState(null);

  const { entrenamientos, totalPaginas, totalElementos, cargando, error, recargar } = useEntrenamientos({
    estado: estado === TODOS ? "" : estado,
    dia: dia === TODOS ? "" : dia,
    entrenadorId: esEntrenador && usuario?.id ? String(usuario.id) : "",
    pagina,
    size: esEntrenador ? 50 : 20,
  });

  const listaMostrada = useMemo(() => {
    if (!esEntrenador) return entrenamientos;
    return entrenamientos.filter((e) => {
      if (estado !== TODOS && e.estado !== estado) return false;
      if (dia !== TODOS) {
        const dias = e.dias || [];
        if (!dias.includes(dia)) return false;
      }
      return true;
    });
  }, [entrenamientos, esEntrenador, estado, dia]);

  const resumen = resumirEntrenamientos(listaMostrada, esEntrenador ? listaMostrada.length : totalElementos);
  const hayFiltros = estado !== TODOS || dia !== TODOS;
  const esCertificacionPendiente =
    esEntrenador &&
    (error?.response?.status === 422 ||
      error?.response?.data?.codigo === "INSTRUCTOR_NO_VALIDADO");

  useEffect(() => {
    if (location.state?.aviso) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.pathname, location.state, navigate]);

  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setEstado(TODOS);
    setDia(TODOS);
    setPagina(0);
  }

  async function imprimir(entrenamiento) {
    setErrorImprimir("");
    try {
      const blob = await imprimirEntrenamiento(entrenamiento.id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (e) {
      setErrorImprimir(mensajeDeError(e, "No se pudo generar el PDF del entrenamiento."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
            {esEntrenador ? "Mis entrenamientos" : "Entrenamientos"}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {esEntrenador
              ? "Los entrenamientos que dirigís, con sus alumnos y su asistencia."
              : "Programación de entrenamientos, con su entrenador, su objetivo y el cupo."}
          </p>
        </div>
        {!esEntrenador && (
          <Button onClick={() => navigate("/entrenamientos/nuevo")}>
            <Plus />
            Nuevo entrenamiento
          </Button>
        )}
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>
      {errorImprimir && <ErrorBanner>{errorImprimir}</ErrorBanner>}

      {esCertificacionPendiente ? (
        <CertificacionPendienteState />
      ) : (
        <>
          <div className="hidden md:grid grid-cols-2 gap-3 lg:grid-cols-4">
            <KpiCard etiqueta={esEntrenador ? "Míos" : "Entrenamientos"} valor={resumen.total} />
            <KpiCard etiqueta="Alumnos inscriptos" valor={resumen.inscriptos} detalle="En esta página" />
            <KpiCard etiqueta="Con lugar" valor={resumen.conLugar} detalle="Admiten inscripción" />
            <KpiCard etiqueta="Cupo completo" valor={resumen.completos} />
          </div>

          <div className="flex flex-wrap items-end gap-3">
            <FiltroPill
              valor={estado}
              onChange={filtrar(setEstado)}
              ancho="w-[190px]"
              opciones={[{ value: TODOS, label: "Todos los estados" }, ...OPCIONES_ESTADO]}
            />

            <FiltroPill
              valor={dia}
              onChange={filtrar(setDia)}
              opciones={[
                { value: TODOS, label: "Todos los días" },
                ...DIAS_SEMANA.map((d) => ({ value: d.value, label: d.etiqueta })),
              ]}
            />
          </div>

          {error && (
            <ErrorBanner>{mensajeDeError(error, "No se pudo cargar el listado de entrenamientos.")}</ErrorBanner>
          )}

          {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando entrenamientos...</p>}

          {!cargando && !error && listaMostrada.length === 0 && (
            <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
              <p className="text-sm text-muted-foreground">
                {hayFiltros
                  ? "Ningún entrenamiento coincide con la búsqueda."
                  : esEntrenador
                    ? "Todavía no tenés entrenamientos asignados."
                    : "Todavía no hay entrenamientos programados."}
              </p>
              {hayFiltros ? (
                <Button variant="outline" onClick={limpiarFiltros}>
                  Limpiar filtros
                </Button>
              ) : (
                !esEntrenador && (
                  <Button onClick={() => navigate("/entrenamientos/nuevo")}>
                    <Plus />
                    Programar el primero
                  </Button>
                )
              )}
            </div>
          )}

          {!cargando && !error && listaMostrada.length > 0 && (
            <EntrenamientosTabla
              entrenamientos={listaMostrada}
              esEntrenador={esEntrenador}
              pagina={pagina}
              totalPaginas={esEntrenador ? 1 : totalPaginas}
              totalElementos={esEntrenador ? listaMostrada.length : totalElementos}
              onCambiarPagina={setPagina}
              onVer={(e) => navigate(`/entrenamientos/${e.id}`, { state: { from: "/entrenamientos" } })}
              onEditar={esEntrenador ? undefined : (e) => navigate(`/entrenamientos/editar/${e.id}`, { state: { from: "/entrenamientos" } })}
              onAsistencia={(e) => navigate(`/entrenamientos/${e.id}/asistencia`, { state: { from: "/entrenamientos" } })}
              onImprimir={imprimir}
              onEliminar={esEntrenador ? undefined : setAEliminar}
            />
          )}
        </>
      )}

      <EliminarEntrenamientoDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        entrenamiento={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={(mensaje) => {
          setAEliminar(null);
          setAviso(mensaje);
          recargar();
        }}
      />
    </div>
  );
}

export function EntrenamientosPage({ vista }) {
  const rol = useAuthStore((estado) => estado.user?.rol);
  return rol === "SOCIO" ? <EntrenamientosDelSocio vista={vista} /> : <EntrenamientosDelPersonal />;
}
