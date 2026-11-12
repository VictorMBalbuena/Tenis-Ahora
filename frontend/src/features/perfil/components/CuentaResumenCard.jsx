import { Badge } from "@/components/ui/badge";
import { ETIQUETA_ROL, dniFormateado, fechaCorta } from "../model/perfil";

/** Lo que define la cuenta y no se edita desde acá: rol, DNI, antigüedad, número de socio. */
export function CuentaResumenCard({ perfil }) {
  const esSocio = perfil.rol === "SOCIO";

  const datos = [
    ["Rol", ETIQUETA_ROL[perfil.rol] ?? perfil.rol],
    ["DNI", dniFormateado(perfil.dni)],
    [esSocio ? "Socio desde" : "Miembro desde", fechaCorta(perfil.fechaAlta)],
  ];

  if (perfil.numeroSocio) datos.splice(2, 0, ["N° de socio", perfil.numeroSocio]);

  return (
    <div className="flex h-full flex-col rounded-xl border border-border bg-card p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-base font-bold tracking-tight">Mi cuenta</h2>
        <Badge
          className={
            perfil.activo
              ? "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
              : "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"
          }
        >
          {perfil.activo ? "Activa" : "Inactiva"}
        </Badge>
      </div>

      <dl className="divide-y divide-border">
        {datos.map(([etiqueta, valor]) => (
          <div key={etiqueta} className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
            <dt className="text-sm text-muted-foreground">{etiqueta}</dt>
            <dd className="text-sm font-semibold tabular-nums">{valor}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
