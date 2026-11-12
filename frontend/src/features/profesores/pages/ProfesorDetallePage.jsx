import { useEffect, useState } from "react";
import { AlertTriangle, BadgeCheck, Edit, Printer, Trash2, TriangleAlert } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import {
  direccionCompleta,
  dniFormateado,
  estaValidado,
  fechaCorta,
  nombreCompleto,
  puedeDictarClases,
  telefonoCompleto,
} from "../model/profesor";
import { decidirCertificacionProfesor, imprimirProfesor, obtenerProfesor } from "../api/profesoresApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { ActivoBadge, ValidacionBadge } from "@/components/ui/estado-badges";
import { RechazarCertificacionDialog } from "@/components/ui/rechazar-certificacion-dialog";
import { EliminarProfesorDialog } from "../components/EliminarProfesorDialog";

export default function ProfesorDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const esAdmin = useAuthStore((s) => s.user?.rol === "ADMINISTRADOR");

  const [profesor, setProfesor] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [validando, setValidando] = useState(false);
  const [rechazando, setRechazando] = useState(false);
  const [error, setError] = useState("");
  const [aEliminar, setAEliminar] = useState(false);
  const [aRechazar, setARechazar] = useState(false);

  useEffect(() => {
    obtenerProfesor(id)
      .then(setProfesor)
      .catch(() => setErrorGlobal(true))
      .finally(() => setCargando(false));
  }, [id]);

  async function decidir(decision) {
    setError("");
    try {
      const actualizado = await decidirCertificacionProfesor(id, decision);
      setProfesor(actualizado);
      return true;
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo decidir la certificación."));
      return false;
    }
  }

  async function aprobar() {
    setValidando(true);
    await decidir({ aprobada: true });
    setValidando(false);
  }

  async function rechazar(motivo) {
    setRechazando(true);
    const ok = await decidir({ aprobada: false, motivo });
    setRechazando(false);
    if (ok) setARechazar(false);
  }

  async function imprimir() {
    try {
      const pdf = await imprimirProfesor(id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo generar la ficha."));
    }
  }

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">
        Cargando datos...
      </div>
    );
  }

  if (errorGlobal || !profesor) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del profesor.</p>
        <button onClick={() => navigate("/profesores")} className="text-primary font-bold hover:underline">
          Volver a profesores
        </button>
      </div>
    );
  }

  const datos = [
    ["DNI", dniFormateado(profesor.dni)],
    ["Correo", profesor.email],
    ["Teléfono", telefonoCompleto(profesor)],
    ["Dirección", direccionCompleta(profesor)],
    ["Fecha de nacimiento", fechaCorta(profesor.fechaNacimiento)],
    ["Alta en el sistema", fechaCorta(profesor.fechaAlta)],
    ["Especialidad", profesor.especialidad || "—"],
    ["Valor por hora", profesor.valorPorHora ? `$ ${profesor.valorPorHora}` : "—"],
  ];

  const sinTitulo = !profesor.tituloHabilitante?.trim();

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">{nombreCompleto(profesor)}</h1>
        <div className="flex items-center gap-3">
          <ValidacionBadge persona={profesor} />
          <ActivoBadge persona={profesor} />
          <VolverButton to="/profesores" />
        </div>
      </div>

      {!puedeDictarClases(profesor) && (
        <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-aviso-bg)] p-3.5 text-sm text-[var(--estado-aviso-fg)]">
          <TriangleAlert className="mt-0.5 size-4 shrink-0" />
          <span>
            {!profesor.activo
              ? "La cuenta está inactiva, así que no se le pueden asignar clases."
              : "Mientras la certificación no esté validada, no se le pueden asignar clases."}
          </span>
        </p>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Datos</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              {datos.map(([etiqueta, valor]) => (
                <div key={etiqueta}>
                  <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
                  <dd className="mt-1 text-sm font-medium">{valor}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Título habilitante</h2>
            <div className="rounded-xl border border-border p-3.5">
              {sinTitulo ? (
                <p className="text-sm text-muted-foreground">
                  Todavía no se cargó el título. Cargalo desde Modificar antes de validar la certificación.
                </p>
              ) : (
                <>
                  <p className="text-sm font-medium">{profesor.tituloHabilitante}</p>
                  {profesor.certificacion && (
                    <dl className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 text-sm">
                      <div>
                        <dt className="text-xs text-muted-foreground">N° de certificación</dt>
                        <dd className="mt-0.5">{profesor.certificacion.numeroCertificacion}</dd>
                      </div>
                      <div>
                        <dt className="text-xs text-muted-foreground">Entidad emisora</dt>
                        <dd className="mt-0.5">{profesor.certificacion.entidadEmisora}</dd>
                      </div>
                      <div>
                        <dt className="text-xs text-muted-foreground">Vencimiento</dt>
                        <dd className="mt-0.5">{fechaCorta(profesor.certificacion.fechaVencimiento)}</dd>
                      </div>
                    </dl>
                  )}
                </>
              )}

              {estaValidado(profesor) && (
                <p className="mt-2.5 flex items-center gap-1.5 text-xs text-[var(--estado-ok-fg)]">
                  <BadgeCheck className="size-3.5" />
                  Validado el {fechaCorta(profesor.fechaValidacion)}
                </p>
              )}
              {profesor.estadoValidacion === "RECHAZADA" && profesor.motivoRechazo && (
                <p className="mt-2.5 text-xs text-[var(--estado-error-fg)]">Motivo del rechazo: {profesor.motivoRechazo}</p>
              )}
            </div>

            {error && (
              <p role="alert" className="mt-3 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
                {error}
              </p>
            )}

            {esAdmin && !estaValidado(profesor) && (
              <div className="mt-4 flex gap-2">
                <Button disabled={validando || sinTitulo} onClick={aprobar} title={sinTitulo ? "Primero cargá el título habilitante" : undefined}>
                  <BadgeCheck />
                  {validando ? "Aprobando..." : "Aprobar certificación"}
                </Button>
                <Button variant="outline" disabled={sinTitulo} onClick={() => setARechazar(true)}>
                  Rechazar certificación
                </Button>
              </div>
            )}
          </div>
        </div>

        <div>
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Acciones</h2>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => navigate(`/profesores/editar/${id}`)}
                className="w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors"
              >
                <Edit size={18} /> Editar
              </button>
              <button
                onClick={imprimir}
                className="w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors"
              >
                <Printer size={18} /> Imprimir ficha
              </button>
              <button
                onClick={() => setAEliminar(true)}
                className="w-full flex items-center justify-center gap-2 h-10 px-5 rounded-full text-sm font-semibold text-[var(--estado-error-fg)] hover:bg-[var(--estado-error-bg)] transition-colors"
              >
                <Trash2 size={18} /> Eliminar
              </button>
            </div>
          </div>
        </div>
      </div>

      <EliminarProfesorDialog
        abierto={aEliminar}
        profesor={profesor}
        onCerrar={() => setAEliminar(false)}
        onEliminado={() => navigate("/profesores")}
      />
      <RechazarCertificacionDialog abierto={aRechazar} enviando={rechazando} onCerrar={() => setARechazar(false)} onConfirmar={rechazar} />
    </div>
  );
}
