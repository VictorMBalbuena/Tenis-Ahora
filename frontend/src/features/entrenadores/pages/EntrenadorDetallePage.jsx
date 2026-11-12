import { useEffect, useState } from "react";
import { AlertTriangle, BadgeCheck, Edit, Printer, Trash2, TriangleAlert } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { ActivoBadge, ValidacionBadge } from "@/components/ui/estado-badges";
import { RechazarCertificacionDialog } from "@/components/ui/rechazar-certificacion-dialog";
import {
  direccionCompleta,
  dniFormateado,
  estaValidado,
  fechaCorta,
  nombreCompleto,
  puedeDirigirEntrenamientos,
  telefonoCompleto,
} from "../model/entrenador";
import { decidirCertificacionEntrenador, imprimirEntrenador, obtenerEntrenador } from "../api/entrenadoresApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { EliminarEntrenadorDialog } from "../components/EliminarEntrenadorDialog";

export default function EntrenadorDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const esAdmin = useAuthStore((s) => s.user?.rol === "ADMINISTRADOR");

  const [entrenador, setEntrenador] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [validando, setValidando] = useState(false);
  const [rechazando, setRechazando] = useState(false);
  const [error, setError] = useState("");
  const [aEliminar, setAEliminar] = useState(false);
  const [aRechazar, setARechazar] = useState(false);

  useEffect(() => {
    obtenerEntrenador(id)
      .then(setEntrenador)
      .catch(() => setErrorGlobal(true))
      .finally(() => setCargando(false));
  }, [id]);

  async function decidir(decision) {
    setError("");
    try {
      const actualizado = await decidirCertificacionEntrenador(id, decision);
      setEntrenador(actualizado);
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
      const pdf = await imprimirEntrenador(id);
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

  if (errorGlobal || !entrenador) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del entrenador.</p>
        <button onClick={() => navigate("/entrenadores")} className="text-primary font-bold hover:underline">
          Volver a entrenadores
        </button>
      </div>
    );
  }

  const datos = [
    ["DNI", dniFormateado(entrenador.dni)],
    ["Correo", entrenador.email],
    ["Teléfono", telefonoCompleto(entrenador)],
    ["Dirección", direccionCompleta(entrenador)],
    ["Fecha de nacimiento", fechaCorta(entrenador.fechaNacimiento)],
    ["Alta en el sistema", fechaCorta(entrenador.fechaAlta)],
    ["Especialidad", entrenador.especialidad || "—"],
    ["Valor por hora", entrenador.valorPorHora ? `$ ${entrenador.valorPorHora}` : "—"],
  ];

  const sinCertificacionDeportiva = !entrenador.certificacionDeportiva?.trim();

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">{nombreCompleto(entrenador)}</h1>
        <div className="flex items-center gap-3">
          <ValidacionBadge persona={entrenador} />
          <ActivoBadge persona={entrenador} />
          <VolverButton to="/entrenadores" />
        </div>
      </div>

      {!puedeDirigirEntrenamientos(entrenador) && (
        <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-aviso-bg)] p-3.5 text-sm text-[var(--estado-aviso-fg)]">
          <TriangleAlert className="mt-0.5 size-4 shrink-0" />
          <span>
            {!entrenador.activo
              ? "La cuenta está inactiva, así que no se le pueden asignar entrenamientos."
              : "Mientras la certificación no esté validada, no se le pueden asignar entrenamientos."}
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
            <h2 className="text-base font-bold tracking-tight mb-5">Certificación deportiva</h2>
            <div className="rounded-xl border border-border p-3.5">
              {sinCertificacionDeportiva ? (
                <p className="text-sm text-muted-foreground">
                  Todavía no se cargó la certificación deportiva. Cargala desde Modificar antes de validar.
                </p>
              ) : (
                <>
                  <p className="text-sm font-medium">{entrenador.certificacionDeportiva}</p>
                  {entrenador.certificacion && (
                    <dl className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 text-sm">
                      <div>
                        <dt className="text-xs text-muted-foreground">N° de certificación</dt>
                        <dd className="mt-0.5">{entrenador.certificacion.numeroCertificacion}</dd>
                      </div>
                      <div>
                        <dt className="text-xs text-muted-foreground">Entidad emisora</dt>
                        <dd className="mt-0.5">{entrenador.certificacion.entidadEmisora}</dd>
                      </div>
                      <div>
                        <dt className="text-xs text-muted-foreground">Vencimiento</dt>
                        <dd className="mt-0.5">{fechaCorta(entrenador.certificacion.fechaVencimiento)}</dd>
                      </div>
                    </dl>
                  )}
                </>
              )}

              {estaValidado(entrenador) && (
                <p className="mt-2.5 flex items-center gap-1.5 text-xs text-[var(--estado-ok-fg)]">
                  <BadgeCheck className="size-3.5" />
                  Validada el {fechaCorta(entrenador.fechaValidacion)}
                </p>
              )}
              {entrenador.estadoValidacion === "RECHAZADA" && entrenador.motivoRechazo && (
                <p className="mt-2.5 text-xs text-[var(--estado-error-fg)]">Motivo del rechazo: {entrenador.motivoRechazo}</p>
              )}
            </div>

            {error && (
              <p role="alert" className="mt-3 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
                {error}
              </p>
            )}

            {esAdmin && !estaValidado(entrenador) && (
              <div className="mt-4 flex gap-2">
                <Button disabled={validando || sinCertificacionDeportiva} onClick={aprobar}>
                  <BadgeCheck />
                  {validando ? "Aprobando..." : "Aprobar certificación"}
                </Button>
                <Button variant="outline" disabled={sinCertificacionDeportiva} onClick={() => setARechazar(true)}>
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
                onClick={() => navigate(`/entrenadores/editar/${id}`)}
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

      <EliminarEntrenadorDialog
        abierto={aEliminar}
        entrenador={entrenador}
        onCerrar={() => setAEliminar(false)}
        onEliminado={() => navigate("/entrenadores")}
      />
      <RechazarCertificacionDialog abierto={aRechazar} enviando={rechazando} onCerrar={() => setARechazar(false)} onConfirmar={rechazar} />
    </div>
  );
}
