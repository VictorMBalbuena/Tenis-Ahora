import { Users, User, UserCheck, AlertCircle, Loader2 } from "lucide-react";
import { BuscadorDeAcompaniante } from "./BuscadorDeAcompaniante";

/**
 * Formulario de selección de modalidad y carga de acompañantes para una reserva.
 * Sigue el diseño de `reserva-02-jugadores.html`.
 */
export function FormularioJugadores({
  modalidad = "SINGLE",
  acompaniantes = [],
  titularDni = "",
  titularSocio = null,
  titularError = null,
  buscandoTitular = false,
  esPersonal = false,
  onChangeModalidad = () => {},
  onChangeAcompaniante = () => {},
  onChangeTitularDni = () => {},
  onBuscarTitular = () => {},
}) {
  return (
    <div className="space-y-5">
      {/* Tarjeta 1: Cantidad de jugadores / Modalidad de juego */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Cantidad de jugadores
            </h2>
          </div>
        </div>

        <div className="px-8 pb-8 pt-6">
          <div className="grid grid-cols-2 gap-3">
            {/* Single (2 jugadores) */}
            <button
              type="button"
              role="button"
              aria-pressed={modalidad === "SINGLE"}
              onClick={() => onChangeModalidad("SINGLE")}
              className={`flex cursor-pointer flex-col items-center gap-1.5 rounded-lg border-2 p-5 text-center transition-colors ${
                modalidad === "SINGLE"
                  ? "border-primary bg-[var(--estado-brand-bg)]"
                  : "border-input bg-background hover:bg-secondary"
              }`}
            >
              <span
                className={
                  modalidad === "SINGLE"
                    ? "text-[var(--estado-brand-fg)]"
                    : "text-muted-foreground"
                }
              >
                <User className="h-5 w-5" />
              </span>
              <span className="text-sm font-semibold text-foreground">
                2 jugadores
              </span>
              <span className="text-xs text-muted-foreground">Single</span>
            </button>

            {/* Dobles (4 jugadores) */}
            <button
              type="button"
              role="button"
              aria-pressed={modalidad === "DOBLES"}
              onClick={() => onChangeModalidad("DOBLES")}
              className={`flex cursor-pointer flex-col items-center gap-1.5 rounded-lg border-2 p-5 text-center transition-colors ${
                modalidad === "DOBLES"
                  ? "border-primary bg-[var(--estado-brand-bg)]"
                  : "border-input bg-background hover:bg-secondary"
              }`}
            >
              <span
                className={
                  modalidad === "DOBLES"
                    ? "text-[var(--estado-brand-fg)]"
                    : "text-muted-foreground"
                }
              >
                <Users className="h-5 w-5" />
              </span>
              <span className="text-sm font-semibold text-foreground">
                4 jugadores
              </span>
              <span className="text-xs text-muted-foreground">
                Pareja / dobles
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Tarjeta 2: Datos de los jugadores */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Datos de los jugadores
            </h2>
          </div>
        </div>

        <div className="px-8 pb-8 pt-6 space-y-4">
          {/* Jugador 1 (titular - requerido si quien reserva es personal del club) */}
          {esPersonal && (
            <div className="space-y-3 rounded-lg border border-border p-4 bg-muted/20">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-semibold text-foreground">
                  Jugador 1 (titular)
                </p>
                <span className="inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
                  Responsable del pago
                </span>
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="titular-dni-input"
                  className="text-xs font-medium text-foreground"
                >
                  DNI del socio titular <span className="text-[var(--estado-error-fg)]">*</span>
                </label>
                <div className="flex gap-2">
                  <input
                    id="titular-dni-input"
                    type="text"
                    inputMode="numeric"
                    placeholder="DNI sin puntos (ej: 30123456)"
                    value={titularDni}
                    onChange={(e) => {
                      const soloNumeros = e.target.value.replace(/\D/g, "").slice(0, 8);
                      onChangeTitularDni(soloNumeros);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        onBuscarTitular(titularDni);
                      }
                    }}
                    className="min-w-0 flex-1 rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] text-foreground font-mono focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                  <button
                    type="button"
                    onClick={() => onBuscarTitular(titularDni)}
                    disabled={buscandoTitular || !titularDni}
                    className="inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors bg-primary text-primary-foreground hover:brightness-95 px-8 py-3 disabled:opacity-50 cursor-pointer"
                  >
                    {buscandoTitular && <Loader2 className="w-4 h-4 animate-spin" />}
                    <span>Verificar</span>
                  </button>
                </div>

                {titularError && (
                  <p
                    role="alert"
                    className="text-xs font-semibold text-[var(--estado-error-fg)] flex items-center gap-1.5 pt-1"
                  >
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{titularError}</span>
                  </p>
                )}

                {titularSocio && (
                  <div className="flex items-center justify-between p-3 rounded-lg border border-[var(--estado-ok-border)] bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)] text-xs sm:text-sm mt-2">
                    <span className="font-semibold">
                      {titularSocio.nombre} {titularSocio.apellido} (DNI {titularSocio.dni})
                    </span>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-background/50">
                      Socio Activo
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Lista de Acompañantes según cantidad */}
          {acompaniantes.map((acomp, idx) => {
            const esSocio = acomp.tipo === "SOCIO" || (acomp.tipo == null && acomp.personaId != null);
            const numeroJugador = idx + 2;

            return (
              <div
                key={idx}
                className="space-y-3 rounded-lg border border-border p-4 bg-card"
              >
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <p className="text-sm font-semibold text-foreground">
                    Jugador {numeroJugador}
                  </p>

                  {/* Selector de Tipo: Invitado externo vs Socio del club */}
                  <div className="inline-flex rounded-full border border-input p-0.5 bg-muted/40 text-xs">
                    <button
                      type="button"
                      onClick={() =>
                        esSocio &&
                        onChangeAcompaniante(idx, {
                          tipo: "INVITADO",
                          personaId: null,
                          nombre: "",
                          apellido: "",
                          documento: "",
                        })
                      }
                      className={`px-3 py-1 rounded-full font-medium transition-all cursor-pointer ${
                        !esSocio
                          ? "bg-background text-foreground shadow-xs font-semibold"
                          : "text-muted-foreground hover:text-foreground"
                      }`}
                    >
                      Invitado externo
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        !esSocio &&
                        onChangeAcompaniante(idx, {
                          tipo: "SOCIO",
                          personaId: null,
                          nombre: "",
                          apellido: "",
                          documento: "",
                        })
                      }
                      className={`px-3 py-1 rounded-full font-medium transition-all cursor-pointer ${
                        esSocio
                          ? "bg-background text-foreground shadow-xs font-semibold"
                          : "text-muted-foreground hover:text-foreground"
                      }`}
                    >
                      <span className="flex items-center gap-1">
                        <UserCheck className="w-3.5 h-3.5" />
                        Socio del club
                      </span>
                    </button>
                  </div>
                </div>

                {/* Campos de texto: Nombre, Apellido, DNI/Socio */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {!esSocio && (
                    <>
                  <div className="space-y-1.5">
                    <label
                      htmlFor={`acomp-${idx}-nombre`}
                      className="text-sm font-semibold text-foreground"
                    >
                      Nombre<span className="text-[var(--estado-error-fg)]"> *</span>
                    </label>
                    <input
                      id={`acomp-${idx}-nombre`}
                      type="text"
                      placeholder="Nombre"
                      value={acomp.nombre || ""}
                      onChange={(e) =>
                        onChangeAcompaniante(idx, "nombre", e.target.value)
                      }
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors placeholder:text-muted-foreground/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label
                      htmlFor={`acomp-${idx}-apellido`}
                      className="text-sm font-semibold text-foreground"
                    >
                      Apellido<span className="text-[var(--estado-error-fg)]"> *</span>
                    </label>
                    <input
                      id={`acomp-${idx}-apellido`}
                      type="text"
                      placeholder="Apellido"
                      value={acomp.apellido || ""}
                      onChange={(e) =>
                        onChangeAcompaniante(idx, "apellido", e.target.value)
                      }
                      className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors placeholder:text-muted-foreground/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                  </div>

                    </>
                  )}

                  <div className="space-y-1.5 sm:col-span-2">
                    {esSocio ? (
                      <BuscadorDeAcompaniante
                        idx={idx}
                        acompaniante={acomp}
                        onEncontrado={(socio) =>
                          onChangeAcompaniante(idx, {
                            personaId: socio.id,
                            nombre: socio.nombre,
                            apellido: socio.apellido,
                            documento: "",
                          })
                        }
                        onCambiarDni={() =>
                          onChangeAcompaniante(idx, { personaId: null, nombre: "", apellido: "" })
                        }
                      />
                    ) : (
                      <>
                        <label
                          htmlFor={`acomp-${idx}-documento`}
                          className="text-sm font-semibold text-foreground"
                        >
                          DNI / Documento (opcional)
                        </label>
                        <input
                          id={`acomp-${idx}-documento`}
                          type="text"
                          inputMode="numeric"
                          placeholder="DNI sin puntos (opcional)"
                          value={acomp.documento || ""}
                          onChange={(e) =>
                            onChangeAcompaniante(idx, "documento", e.target.value.replace(/\D/g, "").slice(0, 8))
                          }
                          className="flex w-full rounded-[20px] border border-input bg-background px-5 py-3.5 text-[15px] transition-colors placeholder:text-muted-foreground/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring font-mono"
                        />
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default FormularioJugadores;
