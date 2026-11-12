import { useMemo, useState, useEffect } from "react";
import { Plus, Search, Users, UserCheck, UserX, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthStore } from "@/store/authStore";
import { imprimirFichaUsuario } from "../api/usuariosApi";
import {
  ETIQUETA_TIPO,
  TipoUsuario,
  nombreCompleto,
  resumirUsuarios,
} from "../model/usuario";
import { useUsuarios, useValorDemorado } from "../hooks/useUsuarios";
import { UsuariosTabla } from "../components/UsuariosTabla";
import { UsuarioFormDialog } from "../components/UsuarioFormDialog";
import { UsuarioDetalleDialog } from "../components/UsuarioDetalleDialog";
import { EliminarUsuarioDialog } from "../components/EliminarUsuarioDialog";
import { CambiarRolDialog, EstadoCuentaDialog } from "@/features/permisos";
import { KpiCard } from "@/components/ui/kpi-card";

/**
 * Página principal de gestión de usuarios (ABM - FRONT 03).
 */
export function UsuariosPage() {
  const usuarioActual = useAuthStore((state) => state.user);
  const esAdmin = usuarioActual?.rol === "ADMINISTRADOR";

  // Estado inicial desde sessionStorage si existe
  const [filtrosIniciales] = useState(() => {
    try {
      const guardados = sessionStorage.getItem("filtros_usuarios");
      if (guardados) return JSON.parse(guardados);
    } catch {
      // Ignorar
    }
    return { tipo: TipoUsuario.SOCIO, busqueda: "", pagina: 0 };
  });

  // Estado de navegación por pestañas
  const [tipo, setTipo] = useState(filtrosIniciales.tipo || TipoUsuario.SOCIO);

  // Filtros y paginación
  const [busquedaInput, setBusquedaInput] = useState(filtrosIniciales.busqueda || "");
  const busquedaDemorada = useValorDemorado(busquedaInput, 300);
  const [pagina, setPagina] = useState(filtrosIniciales.pagina || 0);

  // Diálogos modales
  const [formAbierto, setFormAbierto] = useState(false);
  const [enEdicion, setEnEdicion] = useState(null);
  const [detalleId, setDetalleId] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  // CU74 y CU76: las dos operan sobre la cuenta, no sobre el tipo de usuario de la pestaña
  const [aCambiarRol, setACambiarRol] = useState(null);
  const [aCambiarEstado, setACambiarEstado] = useState(null);

  // Avisos de éxito temporales
  const [aviso, setAviso] = useState(null);
  const [errorDescarga, setErrorDescarga] = useState(null);

  useEffect(() => {
    try {
      sessionStorage.setItem(
        "filtros_usuarios",
        JSON.stringify({ tipo, busqueda: busquedaInput, pagina }),
      );
    } catch {
      // Ignorar
    }
  }, [tipo, busquedaInput, pagina]);

  // Petición al hook con debounce y paginado
  const { contenido, totalPaginas, totalElementos, cargando, error, recargar } = useUsuarios({
    tipo,
    busqueda: busquedaDemorada,
    page: pagina,
    size: 20,
  });

  // Métricas calculadas en memoria según react-best-practices
  const resumen = useMemo(() => resumirUsuarios(contenido), [contenido]);

  // Cambio de pestaña
  const cambiarPestana = (nuevoTipo) => {
    setTipo(nuevoTipo);
    setPagina(0);
    setBusquedaInput("");
    setAviso(null);
  };

  const alCompletarAccion = (mensaje) => {
    setFormAbierto(false);
    setEnEdicion(null);
    setAEliminar(null);
    setACambiarRol(null);
    setACambiarEstado(null);
    setAviso(mensaje);
    recargar();
  };

  // Descarga / visualización de ficha PDF (CU05)
  const manejarImprimir = async (usuario) => {
    setErrorDescarga(null);
    try {
      const blob = await imprimirFichaUsuario(tipo, usuario.id);
      const url = window.URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
      window.open(url, "_blank");
    } catch (err) {
      console.error("Error al descargar PDF:", err);
      setErrorDescarga(`No se pudo generar la ficha en PDF de ${nombreCompleto(usuario)}.`);
    }
  };

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 text-left">
      <h1 className="sr-only">Gestión de Usuarios</h1>

      {/* Barra de Acciones y Pestañas de Rol */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4">
        <nav aria-label="Tipos de usuario" className="flex items-center gap-1 p-1 rounded-xl bg-muted/60 border border-border overflow-x-auto max-w-full">
          <button
            type="button"
            onClick={() => cambiarPestana(TipoUsuario.SOCIO)}
            className={`min-h-[40px] px-4 py-2 rounded-lg text-sm font-medium transition-all shrink-0 ${
              tipo === TipoUsuario.SOCIO
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Socios
          </button>

          <button
            type="button"
            onClick={() => cambiarPestana(TipoUsuario.EMPLEADO)}
            className={`min-h-[40px] px-4 py-2 rounded-lg text-sm font-medium transition-all shrink-0 ${
              tipo === TipoUsuario.EMPLEADO
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Empleados
          </button>

          {esAdmin && (
            <button
              type="button"
              onClick={() => cambiarPestana(TipoUsuario.ADMINISTRADOR)}
              className={`min-h-[40px] px-4 py-2 rounded-lg text-sm font-medium transition-all shrink-0 ${
                tipo === TipoUsuario.ADMINISTRADOR
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Administradores
            </button>
          )}
        </nav>

        <Button
          onClick={() => {
            setEnEdicion(null);
            setFormAbierto(true);
          }}
          className="gap-2 shadow-xs min-h-[40px] w-full sm:w-auto justify-center"
        >
          <Plus className="size-4" />
          Nuevo {tipo === "SOCIO" ? "Socio" : tipo === "EMPLEADO" ? "Empleado" : "Administrador"}
        </Button>
      </div>

      {/* Alerta de notificación / feedback accesible */}
      <div aria-live="polite" role="status">
        {aviso && (
          <div className="flex items-center justify-between rounded-lg bg-[var(--estado-ok-bg)] px-4 py-3 text-sm text-[var(--estado-ok-fg)] shadow-xs">
            <span>{aviso}</span>
            <button
              onClick={() => setAviso(null)}
              className="text-xs font-semibold hover:underline ml-4"
            >
              Cerrar
            </button>
          </div>
        )}
        {errorDescarga && (
          <div className="flex items-center justify-between rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive shadow-xs">
            <span>{errorDescarga}</span>
            <button
              onClick={() => setErrorDescarga(null)}
              className="text-xs font-semibold hover:underline ml-4"
            >
              Cerrar
            </button>
          </div>
        )}
      </div>

      {/* Tarjeta Horizontal de Métricas */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <KpiCard
          etiqueta={`Total de ${ETIQUETA_TIPO[tipo]}`}
          valor={totalElementos}
          icono={Users}
        />
        <KpiCard
          etiqueta="Activos (en página)"
          valor={resumen.activos}
          icono={UserCheck}
        />
        <KpiCard
          etiqueta="Inactivos (en página)"
          valor={resumen.inactivos}
          icono={UserX}
        />
      </div>

      {/* Barra de Filtros */}
      <div className="space-y-1.5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="relative w-full max-w-sm">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="filtro-busqueda-usuarios"
              value={busquedaInput}
              placeholder="Buscar por DNI o Nombre..."
              className="pl-9 bg-card min-h-[40px]"
              onChange={(e) => {
                setBusquedaInput(e.target.value);
                setPagina(0);
              }}
            />
          </div>

          {busquedaInput && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setBusquedaInput("");
                setPagina(0);
              }}
            >
              Limpiar búsqueda
            </Button>
          )}
        </div>

        {busquedaInput.trim().length > 0 && /^\d+$/.test(busquedaInput.trim()) && busquedaInput.trim().length < 7 && (
          <p className="text-xs text-muted-foreground pl-1">
            El DNI debe tener 7 u 8 números para realizar la búsqueda.
          </p>
        )}
        {busquedaInput.trim().length === 1 && !/^\d+$/.test(busquedaInput.trim()) && (
          <p className="text-xs text-muted-foreground pl-1">
            Ingresá al menos 2 letras para buscar por nombre o apellido.
          </p>
        )}
      </div>

      {/* Estado de Carga o Error */}
      {error && (
        <div
          role="alert"
          className="rounded-lg bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2"
        >
          <AlertCircle className="size-4 shrink-0" />
          <span>
            No se pudo cargar el listado.{" "}
            {error.response?.data?.mensaje ||
              error.response?.data?.message ||
              error.message}
          </span>
        </div>
      )}

      {cargando && (
        <p className="py-12 text-center text-sm text-muted-foreground">
          Cargando {ETIQUETA_TIPO[tipo].toLowerCase()}...
        </p>
      )}

      {/* Contenido / Tabla */}
      {!cargando && !error && contenido.length === 0 && (
        <div className="rounded-xl border border-dashed border-border py-12 text-center space-y-3 bg-card/40">
          <p className="text-sm text-muted-foreground">
            {busquedaDemorada
              ? "Ningún registro coincide con el criterio de búsqueda."
              : `Todavía no hay registros cargados en ${ETIQUETA_TIPO[tipo]}.`}
          </p>
          {!busquedaDemorada && (
            <Button
              onClick={() => {
                setEnEdicion(null);
                setFormAbierto(true);
              }}
            >
              <Plus className="size-4" />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && contenido.length > 0 && (
        <UsuariosTabla
          usuarios={contenido}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(u) => setDetalleId(u.id)}
          onEditar={(u) => {
            setEnEdicion(u);
            setFormAbierto(true);
          }}
          onImprimir={manejarImprimir}
          onEliminar={(u) => setAEliminar(u)}
          onCambiarRol={(u) => setACambiarRol(u)}
          onCambiarEstado={(u) => setACambiarEstado(u)}
        />
      )}

      {/* Diálogo de Alta y Edición */}
      <UsuarioFormDialog
        key={`form-${formAbierto}-${enEdicion?.id ?? "nuevo"}`}
        abierto={formAbierto}
        tipo={tipo}
        usuario={enEdicion}
        onCerrar={() => {
          setFormAbierto(false);
          setEnEdicion(null);
        }}
        onGuardado={alCompletarAccion}
      />

      {/* Diálogo de Consulta de Detalle */}
      <UsuarioDetalleDialog
        key={`detalle-${detalleId ?? "ninguno"}`}
        abierto={detalleId !== null}
        tipo={tipo}
        usuarioId={detalleId}
        onCerrar={() => setDetalleId(null)}
      />

      {/* Diálogo de Baja */}
      <EliminarUsuarioDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        tipo={tipo}
        usuario={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alCompletarAccion}
      />

      {/* Asignación de rol (CU74) */}
      {aCambiarRol && (
        <CambiarRolDialog
          key={`rol-${aCambiarRol.id}`}
          abierto
          cuenta={aCambiarRol}
          onCerrar={() => setACambiarRol(null)}
          onCambiado={(_, mensaje) => alCompletarAccion(mensaje)}
        />
      )}

      {/* Habilitar o deshabilitar la cuenta (CU76) */}
      {aCambiarEstado && (
        <EstadoCuentaDialog
          key={`estado-${aCambiarEstado.id}`}
          abierto
          cuenta={{ ...aCambiarEstado, activa: aCambiarEstado.activo !== false }}
          onCerrar={() => setACambiarEstado(null)}
          onCambiado={(_, mensaje) => alCompletarAccion(mensaje)}
        />
      )}
    </div>
  );
}
