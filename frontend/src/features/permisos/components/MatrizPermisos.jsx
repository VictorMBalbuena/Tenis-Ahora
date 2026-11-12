import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { alternarEspecial, alternarModuloEntero, alternarPermiso, tieneEspecial, tienePermiso } from "../model/permiso";

/**
 * La grilla de permisos de un rol (CU75 / RF75): una fila por módulo con las
 * cuatro acciones, y abajo los permisos especiales.
 *
 * Las columnas y las filas salen del catálogo que da el backend, no de una
 * lista escrita acá: si mañana se agrega un módulo, aparece solo.
 */
export function MatrizPermisos({ catalogo, permisos, onCambiar, soloLectura = false }) {
  const acciones = catalogo?.acciones ?? [];
  const modulos = catalogo?.modulos ?? [];
  const especiales = catalogo?.especiales ?? [];
  const codigosDeAccion = acciones.map((a) => a.codigo);

  function cambiar(siguiente) {
    if (!soloLectura) onCambiar(siguiente);
  }

  return (
    <div className="space-y-6">
      <div className="overflow-x-auto rounded-xl border border-border">
        <table className="w-full text-sm">
          <caption className="sr-only">Permisos por módulo y acción</caption>
          <thead className="bg-muted/50">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-semibold">
                Módulo
              </th>
              {acciones.map((accion) => (
                <th key={accion.codigo} scope="col" className="px-3 py-3 text-center font-semibold">
                  {accion.etiqueta}
                </th>
              ))}
              <th scope="col" className="px-3 py-3 text-center font-semibold">
                Todo
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {modulos.map((modulo) => {
              const marcadas = permisos?.modulos?.[modulo.codigo] ?? [];
              const completo = marcadas.length === codigosDeAccion.length && codigosDeAccion.length > 0;

              return (
                <tr key={modulo.codigo} className="hover:bg-muted/30">
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    {modulo.etiqueta}
                  </th>
                  {acciones.map((accion) => (
                    <td key={accion.codigo} className="px-3 py-3 text-center">
                      <CustomCheckbox
                        checked={tienePermiso(permisos, modulo.codigo, accion.codigo)}
                        disabled={soloLectura}
                        ariaLabel={`${accion.etiqueta} en ${modulo.etiqueta}`}
                        onChange={() => cambiar(alternarPermiso(permisos, modulo.codigo, accion.codigo))}
                      />
                    </td>
                  ))}
                  <td className="px-3 py-3 text-center">
                    <CustomCheckbox
                      checked={completo}
                      disabled={soloLectura}
                      ariaLabel={`Todas las acciones en ${modulo.etiqueta}`}
                      onChange={() => cambiar(alternarModuloEntero(permisos, modulo.codigo, codigosDeAccion))}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <section className="space-y-3">
        <div>
          <h3 className="text-[15px] font-semibold text-foreground">Permisos especiales</h3>
          <p className="text-xs text-muted-foreground">
            Acciones que el club quiere poder dar o quitar por separado, además de lo de arriba.
          </p>
        </div>
        <ul className="grid gap-2 sm:grid-cols-2">
          {especiales.map((especial) => (
            <li key={especial.codigo}>
              <label className="flex items-center gap-3 rounded-xl border border-border px-4 py-3 text-sm hover:bg-muted/30">
                <CustomCheckbox
                  checked={tieneEspecial(permisos, especial.codigo)}
                  disabled={soloLectura}
                  ariaLabel={especial.etiqueta}
                  onChange={() => cambiar(alternarEspecial(permisos, especial.codigo))}
                />
                <span>{especial.etiqueta}</span>
              </label>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
