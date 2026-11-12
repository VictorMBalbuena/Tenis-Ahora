import { useEffect, useState } from "react";
import { AlertCircle, User, Phone, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { obtenerUsuario } from "../api/usuariosApi";
import {
  dniFormateado,
  domicilioFormateado,
  fechaCorta,
  nombreCompleto,
  telefonoFormateado,
} from "../model/usuario";

function FilaDato({ etiqueta, valor }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="font-medium text-sm text-foreground">{valor || "—"}</dd>
    </div>
  );
}

/**
 * Modal de Consulta Detallada de Usuario (CU03).
 */
export function UsuarioDetalleDialog({
  abierto = false,
  tipo = "SOCIO",
  usuarioId = null,
  onCerrar,
}) {
  const [detalle, setDetalle] = useState(null);
  const [cargando, setCargando] = useState(Boolean(abierto && usuarioId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!usuarioId) return;

    let vigente = true;
    obtenerUsuario(tipo, usuarioId)
      .then((data) => {
        if (!vigente) return;
        setDetalle(data);
      })
      .catch((err) => {
        if (!vigente) return;
        console.error("Error al cargar detalle:", err);
        setError("No se pudo obtener la información completa del usuario.");
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [usuarioId, tipo]);

  return (
    <Dialog open={abierto} onOpenChange={(ab) => !ab && onCerrar()}>
      <DialogContent className="sm:max-w-xl max-h-[90vh] overflow-y-auto p-6 sm:p-8">
        <DialogHeader className="pb-2">
          <DialogTitle>Detalle del Usuario</DialogTitle>
          <DialogDescription>
            Información registrada en el sistema de gestión del club.
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div
            role="alert"
            className="my-3 flex items-start gap-2.5 rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive"
          >
            <AlertCircle className="size-4 mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {cargando && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            Cargando ficha completa...
          </p>
        )}

        {!cargando && detalle && (
          <div className="space-y-6 py-2">
            {/* Cabecera resumen */}
            <div className="flex items-center justify-between border-b border-border pb-3.5">
              <div>
                <h3 className="text-lg font-semibold">{nombreCompleto(detalle)}</h3>
                <p className="text-sm text-muted-foreground">{detalle.email}</p>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="outline">{detalle.rol}</Badge>
                {detalle.activo ? (
                  <span className="inline-flex items-center rounded-full bg-[var(--estado-ok-bg)] px-2.5 py-0.5 text-xs font-medium text-[var(--estado-ok-fg)]">
                    Activo
                  </span>
                ) : (
                  <span className="inline-flex items-center rounded-full bg-[var(--estado-neutro-bg)] px-2.5 py-0.5 text-xs font-medium text-[var(--estado-neutro-fg)]">
                    Inactivo
                  </span>
                )}
              </div>
            </div>

            {/* Datos Personales */}
            <section className="space-y-2.5">
              <h4 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <User className="size-3.5" />
                Datos Personales
              </h4>
              <dl className="grid grid-cols-2 gap-3.5 rounded-xl border border-border bg-muted/20 p-4 sm:p-5 sm:grid-cols-3">
                <FilaDato etiqueta="DNI" valor={dniFormateado(detalle.dni)} />
                <FilaDato
                  etiqueta="Fecha de Nacimiento"
                  valor={fechaCorta(detalle.fechaNacimiento)}
                />
                <FilaDato
                  etiqueta="Edad"
                  valor={detalle.edad ? `${detalle.edad} años` : "—"}
                />
              </dl>
            </section>

            {/* Contacto y Domicilio */}
            <section className="space-y-2.5">
              <h4 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Phone className="size-3.5" />
                Contacto y Domicilio
              </h4>
              <dl className="grid grid-cols-2 gap-3.5 rounded-xl border border-border bg-muted/20 p-4 sm:p-5">
                <FilaDato
                  etiqueta="Teléfono"
                  valor={telefonoFormateado(detalle.codigoArea, detalle.numeroTelefono)}
                />
                <FilaDato
                  etiqueta="Domicilio"
                  valor={domicilioFormateado(detalle.calle, detalle.numeroCalle)}
                />
              </dl>
            </section>

            {/* Registro en el Club */}
            <section className="space-y-2.5">
              <h4 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Shield className="size-3.5" />
                Cuenta y Membresía
              </h4>
              <dl className="grid grid-cols-2 gap-3.5 rounded-xl border border-border bg-muted/20 p-4 sm:p-5 sm:grid-cols-3">
                <FilaDato etiqueta="Fecha de Alta" valor={fechaCorta(detalle.fechaAlta)} />
                {detalle.numeroSocio && (
                  <FilaDato etiqueta="N° de Socio" valor={detalle.numeroSocio} />
                )}
                <FilaDato etiqueta="ID de Sistema" valor={`#${detalle.id}`} />
              </dl>
            </section>
          </div>
        )}

        <DialogFooter className="mt-6 pt-4 border-t border-border">
          <Button type="button" variant="outline" onClick={onCerrar}>
            Cerrar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
