import { useState } from "react";
import { Calendar as CalendarIcon, ChevronDown, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "cn";

const DIAS_SEMANA = ["D", "L", "M", "M", "J", "V", "S"];
const MESES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
];

const aIso = (fecha) => {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
};

const desdeIso = (iso) => {
  if (!iso) return null;
  const [anio, mes, dia] = iso.split("-").map(Number);
  return new Date(anio, mes - 1, dia);
};

/** 100 años para atrás (alcanza para una fecha de nacimiento) y 10 para adelante (vencimientos de certificación). */
function generarAnios(min, max) {
  const actual = new Date().getFullYear();
  const hasta = max ? Number(max.slice(0, 4)) : actual + 10;
  const desde = min ? Number(min.slice(0, 4)) : actual - 100;
  return Array.from({ length: hasta - desde + 1 }, (_, i) => hasta - i);
}

function BotonDePeriodo({ abierto, onClick, etiqueta, children }) {
  return (
    <button
      type="button"
      aria-label={etiqueta}
      aria-expanded={abierto}
      onClick={onClick}
      className="flex items-center gap-1 rounded-md px-1.5 py-1 text-sm font-semibold transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
    >
      {children}
      <ChevronDown size={14} className={cn("transition-transform", abierto && "rotate-180")} />
    </button>
  );
}

function OpcionDePeriodo({ activa, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "rounded-lg px-2 py-2 text-sm transition-colors",
        activa ? "bg-primary font-semibold text-primary-foreground" : "hover:bg-muted"
      )}
    >
      {children}
    </button>
  );
}

function generarGrilla(mesVisible) {
  const primero = new Date(mesVisible.getFullYear(), mesVisible.getMonth(), 1);
  const inicio = new Date(primero);
  inicio.setDate(inicio.getDate() - primero.getDay());

  return Array.from({ length: 42 }, (_, i) => {
    const dia = new Date(inicio);
    dia.setDate(inicio.getDate() + i);
    return dia;
  });
}

/**
 * Selector de fecha de la app: TODAS las pantallas deben usar este componente, nunca `<input type="date">` ni
 * un calendario propio. Si hay que cambiar algo del calendario, se cambia acá, no en cada pantalla.
 *
 * Selector de fecha con nuestro propio estilo (bordes redondeados, acento verde).
 * El calendario nativo de `<input type="date">` lo dibuja el navegador (en inglés,
 * con azul) y no se puede restylear, por eso arma el suyo con Popover en vez de usarlo.
 *
 * API de input controlado estándar: value/onChange con fecha en formato "YYYY-MM-DD",
 * onChange dispara un evento sintético target.name/target.value igual que un input nativo.
 */
export function DateField({ name, value, onChange, className, min, max, ...props }) {
  const seleccionado = desdeIso(value);
  const [mesVisible, setMesVisible] = useState(() => seleccionado || new Date());
  const [abierto, setAbierto] = useState(false);
  const [vista, setVista] = useState("dias");

  const alternarVista = (destino) => setVista((actual) => (actual === destino ? "dias" : destino));

  const hoyIso = aIso(new Date());

  const elegir = (dia) => {
    onChange({ target: { name, value: aIso(dia), type: "date" } });
    setAbierto(false);
  };

  const cambiarMes = (delta) => {
    setMesVisible((prev) => new Date(prev.getFullYear(), prev.getMonth() + delta, 1));
  };

  const cambiarAMes = (mes) => {
    setMesVisible((prev) => new Date(prev.getFullYear(), mes, 1));
  };

  const cambiarAAnio = (anio) => {
    setMesVisible((prev) => new Date(anio, prev.getMonth(), 1));
  };

  const formatoVisible = seleccionado
    ? seleccionado.toLocaleDateString("es-AR", { day: "2-digit", month: "2-digit", year: "numeric" })
    : "dd/mm/aaaa";

  return (
    <Popover
      open={abierto}
      onOpenChange={(ab) => {
        setAbierto(ab);
        if (ab) {
          setMesVisible(seleccionado || new Date());
          setVista("dias");
        }
      }}
    >
      <PopoverTrigger
        render={
          <Button
            {...props}
            data-min={min}
            data-max={max}
            type="button"
            variant="outline"
            className={cn("w-full justify-between rounded-full px-5 py-3.5 h-auto font-normal text-[15px]", className)}
          >
            <span>{formatoVisible}</span>
            <CalendarIcon size={16} className="text-muted-foreground shrink-0" />
          </Button>
        }
      />
      <PopoverContent className="w-[280px]">
        <div className="mb-3 flex items-center justify-between gap-1">
          <Button type="button" variant="ghost" size="icon-sm" onClick={() => cambiarMes(-1)} aria-label="Mes anterior">
            <ChevronLeft size={16} />
          </Button>
          <div className="flex min-w-0 flex-1 items-center justify-center gap-1">
            <BotonDePeriodo abierto={vista === "meses"} onClick={() => alternarVista("meses")} etiqueta="Mes">
              {MESES[mesVisible.getMonth()]}
            </BotonDePeriodo>
            <BotonDePeriodo abierto={vista === "anios"} onClick={() => alternarVista("anios")} etiqueta="Año">
              {mesVisible.getFullYear()}
            </BotonDePeriodo>
          </div>
          <Button type="button" variant="ghost" size="icon-sm" onClick={() => cambiarMes(1)} aria-label="Mes siguiente">
            <ChevronRight size={16} />
          </Button>
        </div>

        {vista === "meses" && (
          <div className="grid grid-cols-3 gap-1">
            {MESES.map((mes, i) => (
              <OpcionDePeriodo key={mes} activa={i === mesVisible.getMonth()} onClick={() => { cambiarAMes(i); setVista("dias"); }}>
                {mes}
              </OpcionDePeriodo>
            ))}
          </div>
        )}

        {vista === "anios" && (
          <div className="grid max-h-64 grid-cols-3 gap-1 overflow-y-auto">
            {generarAnios(min, max).map((anio) => (
              <OpcionDePeriodo key={anio} activa={anio === mesVisible.getFullYear()} onClick={() => { cambiarAAnio(anio); setVista("dias"); }}>
                {anio}
              </OpcionDePeriodo>
            ))}
          </div>
        )}

        {vista === "dias" && (
          <>
        <div className="mb-1 grid grid-cols-7 gap-1">
          {DIAS_SEMANA.map((d, i) => (
            <div key={i} className="flex h-7 items-center justify-center text-[11px] font-semibold text-muted-foreground">
              {d}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-1">
          {generarGrilla(mesVisible).map((dia) => {
            const iso = aIso(dia);
            const delMesVisible = dia.getMonth() === mesVisible.getMonth();
            const esSeleccionado = iso === value;
            const esHoy = iso === hoyIso;
            const fueraDeRango = (min && iso < min) || (max && iso > max);
            return (
              <button
                key={iso}
                type="button"
                disabled={Boolean(fueraDeRango)}
                onClick={() => elegir(dia)}
                className={cn(
                  fueraDeRango && "cursor-not-allowed opacity-30",
                  "h-8 w-8 rounded-full text-sm transition-colors",
                  !delMesVisible && "text-muted-foreground/40",
                  delMesVisible && !esSeleccionado && "hover:bg-muted",
                  esSeleccionado && "bg-primary text-primary-foreground font-semibold",
                  !esSeleccionado && esHoy && "border border-primary"
                )}
              >
                {dia.getDate()}
              </button>
            );
          })}
        </div>
          </>
        )}
      </PopoverContent>
    </Popover>
  );
}
