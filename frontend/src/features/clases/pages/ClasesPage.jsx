import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { ErrorBanner } from "@/components/ui/error-banner";
import { useAuthStore } from "@/store/authStore";
import { mensajeDeError } from "@/lib/erroresApi";
import { ETIQUETA_ESTADO, ETIQUETA_NIVEL, EstadoActividad, NivelClase, OPCIONES_DIA, resumirClases } from "../model/clase";
import { useClases } from "../hooks/useClases";
import { imprimirClase } from "../api/clasesApi";
import { ClasesTabla } from "../components/ClasesTabla";
import { EliminarClaseDialog } from "../components/EliminarClaseDialog";
import { ClasesDelSocio } from "../components/ClasesDelSocio";
import { KpiCard } from "@/components/ui/kpi-card";
import { CertificacionPendienteState } from "@/components/ui/certificacion-pendiente-state";

const TODOS = "TODOS";

/**
 * Listado de clases (CU34), con alta, baja, modificación e impresión.
 *
 * Si quien entra es un profesor, la pantalla muestra solo sus clases: es la
 * vista de "mis clases y alumnos" (CU82). Para empleados y administradores
 * muestra todas y habilita programar una nueva.
 */
function ClasesDelPersonal() {
  const navigate = useNavigate();
  const location = useLocation();
  const usuario = useAuthStore((estado) => estado.user);

  const esProfesor = usuario?.rol === "PROFESOR";

  const [nivel, setNivel] = useState(TODOS);
  const [estado, setEstado] = useState(TODOS);
  const [dia, setDia] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState(location.state?.aviso ?? "");
  const [errorImprimir, setErrorImprimir] = useState("");
  const [aEliminar, setAEliminar] = useState(null);

  const { clases, totalPaginas, totalElementos, cargando, error, recargar } = useClases({
    nivel: nivel === TODOS ? "" : nivel,
    estado: estado === TODOS ? "" : estado,
    dia: dia === TODOS ? "" : dia,
    profesorId: esProfesor && usuario?.id ? String(usuario.id) : "",
    pagina,
    esProfesor, 
  });

  const resumen = resumirClases(clases, totalElementos);
  const hayFiltros = nivel !== TODOS || estado !== TODOS || dia !== TODOS;
  const esCertificacionPendiente =
    esProfesor &&
    (error?.response?.status === 422 ||
      error?.response?.data?.codigo === "PROFESOR_NO_VALIDADO");

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
    setNivel(TODOS);
    setEstado(TODOS);
    setDia(TODOS);
    setPagina(0);
  }

  async function imprimir(clase) {
    setErrorImprimir("");
    try {
      const blob = await imprimirClase(clase.id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
      URL.revokeObjectURL(url);
    } catch (e) {
      setErrorImprimir(mensajeDeError(e, "No se pudo generar el PDF de la clase."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
            {esProfesor ? "Mis clases" : "Clases"}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {esProfesor
              ? "Las clases que dictás, con sus alumnos inscriptos."
              : "Programación de clases, con su profesor, su cancha y el cupo de alumnos."}
          </p>
        </div>
        {!esProfesor && (
          <Button onClick={() => navigate("/clases/nueva")}>
            <Plus />
            Nueva clase
          </Button>
        )}
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>
      {errorImprimir && <ErrorBanner>{errorImprimir}</ErrorBanner>}

      {esCertificacionPendiente ? (
        <CertificacionPendienteState
          titulo="Certificación docente en proceso de validación"
          descripcion="Tu cuenta de profesor fue creada y está a la espera de que la secretaría del club valide tu certificación docente habilitante. En cuanto sea aprobada, vas a ver acá tus clases asignadas y podrás tomar asistencia."
        />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <KpiCard etiqueta={esProfesor ? "Mis clases" : "Clases"} valor={resumen.total} />
            <KpiCard etiqueta="Alumnos inscriptos" valor={resumen.inscriptos} detalle="En esta página" />
            <KpiCard etiqueta="Con lugar" valor={resumen.conLugar} detalle="Admiten inscripción" />
            <KpiCard etiqueta="Cupo completo" valor={resumen.completas} />
          </div>

          {/* Filtros */}
          <div className="flex flex-wrap items-end gap-3">
            <FiltroPill
              valor={nivel}
              onChange={filtrar(setNivel)}
              opciones={[
                { value: TODOS, label: "Todos los niveles" },
                ...Object.values(NivelClase).map((v) => ({ value: v, label: ETIQUETA_NIVEL[v] })),
              ]}
            />

            <FiltroPill
              valor={estado}
              onChange={filtrar(setEstado)}
              ancho="w-[190px]"
              opciones={[
                { value: TODOS, label: "Todos los estados" },
                ...Object.values(EstadoActividad).map((v) => ({ value: v, label: ETIQUETA_ESTADO[v] })),
              ]}
            />

            <FiltroPill
              valor={dia}
              onChange={filtrar(setDia)}
              opciones={[{ value: TODOS, label: "Todos los días" }, ...OPCIONES_DIA]}
            />
          </div>

          {/* Listado */}
          {error && <ErrorBanner>{mensajeDeError(error, "No se pudo cargar el listado de clases.")}</ErrorBanner>}

          {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando clases...</p>}

          {!cargando && !error && clases.length === 0 && (
            <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
              <p className="text-sm text-muted-foreground">
                {hayFiltros
                  ? "Ninguna clase coincide con la búsqueda."
                  : esProfesor
                    ? "Todavía no tenés clases asignadas."
                    : "Todavía no hay clases programadas."}
              </p>
              {hayFiltros ? (
                <Button variant="outline" onClick={limpiarFiltros}>
                  Limpiar filtros
                </Button>
              ) : (
                !esProfesor && (
                  <Button onClick={() => navigate("/clases/nueva")}>
                    <Plus />
                    Programar la primera
                  </Button>
                )
              )}
            </div>
          )}

          {!cargando && !error && clases.length > 0 && (
            <ClasesTabla
              clases={clases}
              pagina={pagina}
              totalPaginas={totalPaginas}
              totalElementos={totalElementos}
              esProfesor={esProfesor}
              onCambiarPagina={setPagina}
              onVer={(c) => navigate(`/clases/${c.id}`)}
              onAsistencia={(c) => navigate(`/clases/${c.id}/asistencia`)}
              onImprimir={imprimir}
              onEditar={esProfesor ? null : (c) => navigate(`/clases/editar/${c.id}`)}
              onEliminar={esProfesor ? null : setAEliminar}
            />
          )}

          <EliminarClaseDialog
            key={`baja-${aEliminar?.id ?? "ninguna"}`}
            abierto={aEliminar !== null}
            clase={aEliminar}
            onCerrar={() => setAEliminar(null)}
            onEliminado={(mensaje) => {
              setAEliminar(null);
              setAviso(mensaje);
              recargar();
            }}
          />
        </>
      )}
    </div>
  );
}


export function ClasesPage({ vista }) {
  const rol = useAuthStore((estado) => estado.user?.rol);
  return rol === "SOCIO" ? <ClasesDelSocio vista={vista} /> : <ClasesDelPersonal />;
}
