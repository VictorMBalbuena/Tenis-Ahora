import { hoyLocal } from "@/lib/fechas";
import {
  ETIQUETA_CALCULO,
  beneficio,
  formaDeCalculo,
} from "../model/descuento";

/**
 * Ficha del descuento para imprimir (CU66).
 *
 * Esta siempre en el DOM y oculta por CSS; solo se ve en la hoja impresa.
 * Las reglas estan en descuentos.css.
 *
 * Usa estilos propios en vez de los tokens del tema porque al imprimir no
 * hay modo oscuro ni fondo de color: tiene que salir negro sobre blanco.
 */
export function FichaImpresion({ descuento }) {
  if (!descuento) return null;

  const forma = formaDeCalculo(descuento);

  const filas = [
    ["Beneficio", beneficio(descuento)],
    ["Forma de calculo", forma ? ETIQUETA_CALCULO[forma] : "Sin definir"],
    ["Condiciones", descuento.condiciones || "Sin condiciones cargadas"],
    ["Estado", descuento.activo ? "Activo" : "Inactivo"],
    ["Veces aplicado", String(descuento.vecesAplicado)],
  ];

  const hoy = hoyLocal().split("-").reverse().join("/");

  return (
    <section className="ficha-impresion" aria-hidden="true">
      <header style={{ borderBottom: "2px solid #000", paddingBottom: "8px" }}>
        <h1 style={{ fontSize: "20px", margin: 0 }}>Tenis Ahora</h1>
        <p style={{ fontSize: "13px", margin: "2px 0 0" }}>
          Ficha de descuento
        </p>
      </header>

      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          marginTop: "16px",
          fontSize: "12px",
        }}
      >
        <tbody>
          {filas.map(([clave, valor]) => (
            <tr key={clave}>
              <th
                style={{
                  textAlign: "left",
                  width: "35%",
                  padding: "6px 8px 6px 0",
                  borderBottom: "1px solid #ccc",
                  fontWeight: 600,
                  verticalAlign: "top",
                }}
              >
                {clave}
              </th>
              <td style={{ padding: "6px 0", borderBottom: "1px solid #ccc" }}>
                {valor}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <footer style={{ marginTop: "20px", fontSize: "11px", color: "#444" }}>
        Emitida el {hoy} por el sistema Tenis Ahora.
      </footer>
    </section>
  );
}
