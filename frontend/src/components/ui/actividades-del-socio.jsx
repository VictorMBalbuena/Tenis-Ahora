import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown } from "lucide-react";
import { ActividadSocioCard } from "@/components/ui/actividad-socio-card";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SuccessBanner } from "@/components/ui/success-banner";
import { listarMisInscripciones } from "@/features/inscripciones/api/inscripcionesApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { cn } from "@/lib/utils";

// Vista del socio para clases y entrenamientos (CU80, RNF08): lo suyo y lo disponible, cada uno en su pantalla.
const TAMANIO_DE_PAGINA = 50;

function Seccion({ vacio, children }) {
  if (children.length === 0) {
    return <p className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">{vacio}</p>;
  }
  return <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">{children}</div>;
}

function Pestana({ to, activa, children }) {
  return (
    <Link
      to={to}
      className={cn(
        "inline-flex h-9 items-center rounded-full px-4 text-sm font-semibold transition-colors",
        activa ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
      )}
    >
      {children}
    </Link>
  );
}

export function ActividadesDelSocio({ titulo, tipo, ruta, plural, Icono, describir, listar, inscribirme, vista }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [actividades, setActividades] = useState([]);
  const [inscripciones, setInscripciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  // No es lo mismo que no haya actividades que no haber podido traerlas: sin esto la pantalla decía
  // "por ahora no hay más" justo debajo del cartel de que no tenés permisos.
  const [sinListado, setSinListado] = useState(false);
  const [aviso, setAviso] = useState(location.state?.aviso ?? "");
  const [procesandoId, setProcesandoId] = useState(null);

  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelado = false;
    // Las dos consultas se resuelven por separado a propósito: son de módulos distintos y el administrador
    // puede haber dejado permiso sobre una y no sobre la otra. Con Promise.all, que una fallara dejaba la
    // pantalla entera vacía aunque la otra hubiera traído datos.
    Promise.allSettled([listar({ size: TAMANIO_DE_PAGINA }), listarMisInscripciones()])
      .then(([listado, propias]) => {
        if (cancelado) return;

        if (listado.status === "fulfilled") {
          setActividades(listado.value.contenido || []);
        }
        if (propias.status === "fulfilled") {
          setInscripciones(propias.value.filter((i) => i.tipo === tipo));
        }

        const fallo = listado.status === "rejected" ? listado.reason : propias.reason;
        setError(fallo ? mensajeDeError(fallo, `No se pudieron cargar ${plural}.`) : "");
        setSinListado(listado.status === "rejected");
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
  }, [listar, tipo, plural, version]);

  const recargar = () => setVersion((v) => v + 1);

  useEffect(() => {
    if (location.state?.aviso) navigate(location.pathname, { replace: true, state: {} });
  }, [location.pathname, location.state, navigate]);

  const vigentes = useMemo(() => new Map(inscripciones.filter((i) => i.activa).map((i) => [i.actividadId, i])), [inscripciones]);
  const mias = actividades.filter((a) => vigentes.has(a.id));
  const disponibles = actividades.filter((a) => !vigentes.has(a.id) && a.admiteInscripcion);
  const historial = inscripciones.filter((i) => !i.activa);

  const enMias = vista !== "disponibles";
  const tituloMias = tipo === "CLASE" ? "Mis clases" : "Mis entrenamientos";
  const tituloDisponibles = `${titulo} disponibles`;
  const rutaMias = tipo === "CLASE" ? "/clases/mias" : "/entrenamientos/mios";

  const inscribir = async (actividad) => {
    setError("");
    setAviso("");
    setProcesandoId(actividad.id);
    try {
      const inscripcion = await inscribirme(actividad.id);
      if (inscripcion.estado === "PENDIENTE_PAGO") {
        navigate(`/inscripciones/${inscripcion.id}/cobro`);
        return;
      }
      setAviso(`Te inscribiste a "${actividad.nombre}".`);
      recargar();
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo completar la inscripción."));
    } finally {
      setProcesandoId(null);
    }
  };

  const tarjeta = (actividad) => (
    <ActividadSocioCard
      key={actividad.id}
      actividad={actividad}
      inscripcion={vigentes.get(actividad.id)}
      datos={describir(actividad)}
      Icono={Icono}
      procesando={procesandoId === actividad.id}
      onInscribirme={inscribir}
      ruta={ruta}
    />
  );

  return (
    <div className="w-full min-w-0 space-y-6">
      <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">{enMias ? tituloMias : tituloDisponibles}</h1>

      <div className="flex flex-wrap gap-1">
        <Pestana to={rutaMias} activa={enMias}>{tituloMias}{sinListado ? "" : ` (${mias.length})`}</Pestana>
        <Pestana to={`${ruta}/disponibles`} activa={!enMias}>Disponibles{sinListado ? "" : ` (${disponibles.length})`}</Pestana>
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>
      {error && <ErrorBanner>{error}</ErrorBanner>}

      {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando...</p>}

      {!cargando && !sinListado && (
        <Seccion vacio={enMias ? `Todavía no te inscribiste. Mirá lo que está disponible en "${tituloDisponibles}".` : `Por ahora no hay más ${plural} con inscripción abierta.`}>
          {(enMias ? mias : disponibles).map(tarjeta)}
        </Seccion>
      )}

      {!cargando && enMias && !sinListado && historial.length > 0 && (
        <details className="group rounded-xl border border-border bg-card p-5">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 font-semibold text-foreground">
            Historial de bajas ({historial.length})
            <ChevronDown className="size-4 text-muted-foreground transition-transform group-open:rotate-180" />
          </summary>
          <ul className="mt-3 divide-y divide-border">
            {historial.map((i) => (
              <li key={i.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                <span className="min-w-0 truncate text-foreground">{i.actividad}</span>
                <span className="shrink-0 text-xs text-muted-foreground">{i.fechaInscripcion?.split("-").reverse().join("/")}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}
