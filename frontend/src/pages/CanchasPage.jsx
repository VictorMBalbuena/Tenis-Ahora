import { useState, useEffect, useMemo } from "react";
import { Search, Plus, AlertTriangle, Layers } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { useCanchas } from "@/features/canchas/hooks/useCanchas";
import { EliminarCanchaDialog } from "@/features/canchas/components/EliminarCanchaDialog";
import { CanchasTabla } from "@/features/canchas/components/CanchasTabla";
import { listarTiposCancha } from "@/features/canchas/api/tiposCanchaApi";

const TODOS = "TODOS";

export default function CanchasPage() {
  const navigate = useNavigate();
  const [canchaAEliminar, setCanchaAEliminar] = useState(null);

  const [busqueda, setBusqueda] = useState("");
  const [superficie, setSuperficie] = useState(TODOS);
  const [estadoFiltro, setEstadoFiltro] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [tiposCancha, setTiposCancha] = useState([]);
  const [todasLasSuperficies, setTodasLasSuperficies] = useState(new Set());

  useEffect(() => {
    listarTiposCancha()
      .then((data) => {
        const arr = Array.isArray(data) ? data : [];
        setTiposCancha(arr);
        setTodasLasSuperficies((prev) => {
          const next = new Set(prev);
          arr.forEach((t) => {
            const s = t.superficie || t.tipoSuperficie;
            if (s && s.trim()) next.add(s.trim());
          });
          return next;
        });
      })
      .catch((err) => console.error("Error al cargar tipos de cancha:", err));
  }, []);

  const { canchas, totalPaginas, totalElementos, cargando, error, quitarCancha, recargar } = useCanchas({
    busqueda,
    superficie: superficie === TODOS ? "" : superficie,
    estado: estadoFiltro === TODOS ? "" : estadoFiltro,
    pagina,
  });

  useEffect(() => {
    if (canchas?.length) {
      setTodasLasSuperficies((prev) => {
        let cambio = false;
        const next = new Set(prev);
        canchas.forEach((c) => {
          const s = c.superficie || c.tipoSuperficie;
          if (s && s.trim() && !next.has(s.trim())) {
            next.add(s.trim());
            cambio = true;
          }
        });
        return cambio ? next : prev;
      });
    }
  }, [canchas]);

  const opcionesSuperficie = useMemo(() => {
    const lista = Array.from(todasLasSuperficies).sort((a, b) => a.localeCompare(b));
    return [
      { value: TODOS, label: "Todas las superficies" },
      ...lista.map((s) => ({ value: s, label: s })),
    ];
  }, [todasLasSuperficies]);

  const canchasVisibles = useMemo(() => {
    if (!superficie || superficie === TODOS) return canchas;
    return canchas.filter((c) => {
      const s = (c.superficie || c.tipoSuperficie || "").toLowerCase();
      const filtro = superficie.toLowerCase();
      return s === filtro || s.includes(filtro);
    });
  }, [canchas, superficie]);

  const cambiarBusqueda = (valor) => {
    setBusqueda(valor);
    setPagina(0);
  };

  const cambiarSuperficie = (valor) => {
    setSuperficie(valor);
    setPagina(0);
  };

  const cambiarEstado = (valor) => {
    setEstadoFiltro(valor);
    setPagina(0);
  };

  const alEliminar = (idEliminado) => {
    quitarCancha(idEliminado);
    setCanchaAEliminar(null);
  };

  if (error) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se pudo conectar con el servidor para cargar las canchas.</p>
        <button onClick={recargar} className="font-bold text-primary hover:underline">
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Canchas</h1>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => navigate("/canchas/tipos")}>
            <Layers size={18} />
            <span>Tipos de cancha</span>
          </Button>
          <Button onClick={() => navigate("/canchas/nuevo")}>
            <Plus size={18} strokeWidth={2.5} />
            <span>Nueva cancha</span>
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-sm">
          <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Buscar cancha..."
            value={busqueda}
            onChange={(e) => cambiarBusqueda(e.target.value)}
            className="h-10 rounded-full pl-11"
          />
        </div>
        <div className="flex gap-2">
          <FiltroPill
            valor={superficie}
            onChange={cambiarSuperficie}
            ancho="min-w-[220px] max-w-[320px] w-auto"
            opciones={opcionesSuperficie}
          />
          <FiltroPill
            valor={estadoFiltro}
            onChange={cambiarEstado}
            ancho="w-[200px]"
            opciones={[
              { value: TODOS, label: "Todos los estados" },
              { value: "DISPONIBLE", label: "Disponible" },
              { value: "MANTENIMIENTO", label: "Mantenimiento" },
              { value: "DESHABILITADA", label: "Deshabilitada" },
            ]}
          />
        </div>
      </div>

      {cargando ? (
        <div className="flex min-h-[30vh] items-center justify-center font-medium text-muted-foreground">
          Cargando listado de canchas...
        </div>
      ) : (
        <CanchasTabla
          canchas={canchasVisibles}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(cancha) => navigate(`/canchas/${cancha.id}`)}
          onEditar={(cancha) => navigate(`/canchas/editar/${cancha.id}`)}
          onEliminar={setCanchaAEliminar}
        />
      )}

      <EliminarCanchaDialog
        abierto={!!canchaAEliminar}
        cancha={canchaAEliminar}
        onCerrar={() => setCanchaAEliminar(null)}
        onEliminada={alEliminar}
      />
    </div>
  );
}
