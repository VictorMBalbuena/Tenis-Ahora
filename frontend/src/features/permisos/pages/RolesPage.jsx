import { useCallback, useEffect, useRef, useState } from "react";
import { Lock, Pencil, Plus, Shield, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SuccessBanner } from "@/components/ui/success-banner";
import { mensajeDeError } from "@/lib/erroresApi";
import { eliminarRol, guardarPermisos, listarRoles, obtenerCatalogo } from "../api/permisosApi";
import { contarPermisos, etiquetaDeRolBase, mismosPermisos, rolesEditables } from "../model/permiso";
import { MatrizPermisos } from "../components/MatrizPermisos";
import { RolFormDialog } from "../components/RolFormDialog";

/**
 * Roles y permisos (CU75 Configurar permisos por rol, CU85 Crear rol).
 *
 * A la izquierda, los roles: primero los base (menos Administrador), que no se borran, y abajo
 * los que creó el club. A la derecha, la matriz del rol elegido.
 *
 * Los cambios en la matriz no se guardan solos: se marca lo que haga falta y
 * recién al confirmar se manda entera. Así una sesión de edición es una sola
 * decisión y no veinte pedidos sueltos.
 */
export function RolesPage() {
  const [catalogo, setCatalogo] = useState(null);
  const [roles, setRoles] = useState([]);
  const [elegidoId, setElegidoId] = useState(null);
  const [borrador, setBorrador] = useState(null);

  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [dialogo, setDialogo] = useState(null); // { modo: "nuevo" | "editar", rol }

  const pedido = useRef(0);
  // Cuál estaba elegido, en una referencia: recargar() lo necesita sin volver a crearse en cada cambio
  const elegidoRef = useRef(null);

  /**
   * Trae el catálogo y los roles. No toca el estado de forma sincrónica: todo
   * pasa dentro del `then`, como en el resto de los listados del sistema, para
   * no encadenar renders desde el cuerpo de un efecto.
   */
  const recargar = useCallback((idPreferido = null) => {
    const miPedido = ++pedido.current;

    return Promise.all([obtenerCatalogo(), listarRoles()])
      .then(([cat, todos]) => {
        if (miPedido !== pedido.current) return;
        const lista = rolesEditables(todos);
        const buscado = idPreferido ?? elegidoRef.current;
        const elegido = lista.find((r) => r.id === buscado) ?? lista[0] ?? null;

        elegidoRef.current = elegido?.id ?? null;
        setCatalogo(cat);
        setRoles(lista);
        setElegidoId(elegido?.id ?? null);
        setBorrador(elegido?.permisos ?? null);
        setCargando(false);
        setError("");
      })
      .catch((e) => {
        if (miPedido !== pedido.current) return;
        setCargando(false);
        setError(mensajeDeError(e, "No se pudieron cargar los roles."));
      });
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  const elegido = roles.find((r) => r.id === elegidoId) ?? null;
  const hayCambios = elegido && borrador && !mismosPermisos(elegido.permisos, borrador);

  function elegir(rol) {
    elegidoRef.current = rol.id;
    setElegidoId(rol.id);
    setBorrador(rol.permisos);
    setAviso("");
    setError("");
  }

  async function guardar() {
    if (!elegido || !borrador) return;
    setGuardando(true);
    setError("");
    try {
      const actualizado = await guardarPermisos(elegido.id, borrador);
      setRoles((previos) => previos.map((r) => (r.id === actualizado.id ? actualizado : r)));
      setBorrador(actualizado.permisos);
      setAviso(`Se guardaron los permisos de "${actualizado.nombre}".`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudieron guardar los permisos."));
    } finally {
      setGuardando(false);
    }
  }

  async function borrar(rol) {
    setError("");
    try {
      await eliminarRol(rol.id);
      setAviso(`Se eliminó el rol "${rol.nombre}".`);
      elegidoRef.current = null;
      setCargando(true);
      recargar();
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el rol."));
    }
  }

  return (
    <>
      <div className="space-y-6">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Roles y permisos</h1>
            <p className="text-sm text-muted-foreground">
              Qué puede hacer cada rol en cada módulo del sistema.
            </p>
          </div>
          <Button onClick={() => setDialogo({ modo: "nuevo" })}>
            <Plus size={18} /> Nuevo rol
          </Button>
        </header>

        {error && <ErrorBanner>{error}</ErrorBanner>}
        {aviso && <SuccessBanner>{aviso}</SuccessBanner>}

        {cargando ? (
          <p className="text-sm text-muted-foreground">Cargando roles…</p>
        ) : (
          <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
            <nav aria-label="Roles" className="space-y-2">
              {roles.map((rol) => (
                <div
                  key={rol.id}
                  className={`rounded-2xl border p-4 transition-colors ${
                    rol.id === elegidoId ? "border-primary bg-primary/5" : "border-border hover:bg-muted/30"
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => elegir(rol)}
                    aria-current={rol.id === elegidoId ? "true" : undefined}
                    className="w-full text-left"
                  >
                    <div className="flex items-center gap-2">
                      {rol.base ? <Lock size={15} className="text-muted-foreground" /> : <Shield size={15} />}
                      <span className="font-semibold">{rol.nombre}</span>
                    </div>
                    {rol.descripcion && (
                      <p className="mt-1 text-xs text-muted-foreground">{rol.descripcion}</p>
                    )}
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                      <Badge variant="secondary">{contarPermisos(rol.permisos)} permisos</Badge>
                      <span>
                        {rol.cuentas} {rol.cuentas === 1 ? "cuenta" : "cuentas"}
                      </span>
                      {!rol.base && <span>· sobre {etiquetaDeRolBase(rol.rolBase)}</span>}
                    </div>
                  </button>

                  {!rol.base && (
                    <div className="mt-3 flex gap-2">
                      <Button variant="ghost" size="sm" onClick={() => setDialogo({ modo: "editar", rol })}>
                        <Pencil size={14} /> Editar
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => borrar(rol)}>
                        <Trash2 size={14} /> Eliminar
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </nav>

            <section className="space-y-5 rounded-2xl border border-border p-5">
              {elegido ? (
                <>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="text-lg font-semibold">Permisos de {elegido.nombre}</h2>
                      <p className="text-xs text-muted-foreground">
                        {elegido.base
                          ? "Rol base del sistema: se le cambian los permisos, pero no se elimina."
                          : `Se apoya en ${etiquetaDeRolBase(elegido.rolBase)}: no puede tener permisos que ese rol no tenga.`}
                      </p>
                    </div>
                    <Button onClick={guardar} disabled={!hayCambios || guardando}>
                      {guardando ? "Guardando…" : "Guardar permisos"}
                    </Button>
                  </div>

                  <MatrizPermisos
                    catalogo={catalogo}
                    permisos={borrador}
                    onCambiar={setBorrador}
                  />

                  {hayCambios && (
                    <p className="text-xs text-muted-foreground">
                      Hay cambios sin guardar. Los permisos nuevos valen en el pedido siguiente de cada persona:
                      no hace falta que vuelvan a entrar.
                    </p>
                  )}
                </>
              ) : (
                <p className="text-sm text-muted-foreground">Elegí un rol para ver sus permisos.</p>
              )}
            </section>
          </div>
        )}
      </div>

      {dialogo && (
        <RolFormDialog
          key={dialogo.rol?.id ?? "nuevo"}
          abierto
          rol={dialogo.rol}
          onCerrar={() => setDialogo(null)}
          onGuardado={(rol, mensaje) => {
            setDialogo(null);
            setAviso(mensaje);
            recargar(rol.id);
          }}
        />
      )}
    </>
  );
}
