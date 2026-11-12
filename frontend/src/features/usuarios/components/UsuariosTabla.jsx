import { ChevronLeft, ChevronRight, Eye, Pencil, Printer, ShieldUser, Trash2, UserCheck, UserX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { dniFormateado, nombreCompleto } from "../model/usuario";

/**
 * Badge semántico para el estado de la cuenta (activo / inactivo).
 */
function EstadoBadge({ activo }) {
  if (activo) {
    return (
      <span className="inline-flex items-center rounded-full bg-[var(--estado-ok-bg)] px-2.5 py-0.5 text-xs font-medium text-[var(--estado-ok-fg)]">
        Activo
      </span>
    );
  }

  return (
    <span className="inline-flex items-center rounded-full bg-[var(--estado-neutro-bg)] px-2.5 py-0.5 text-xs font-medium text-[var(--estado-neutro-fg)]">
      Inactivo
    </span>
  );
}

/**
 * Grupo de botones de acción por cada fila o tarjeta.
 * En móviles expone botones grandes (>=44px de área táctil) con etiquetas legibles.
 */
function AccionesDeFila({ usuario, onVer, onEditar, onImprimir, onEliminar, onCambiarRol, onCambiarEstado, esMobile = false }) {
  const nombre = nombreCompleto(usuario);
  const activa = usuario.activo !== false;

  if (esMobile) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 w-full pt-1">
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
          aria-label={`Ver el detalle de ${nombre}`}
          title={`Ver el detalle de ${nombre}`}
          onClick={() => onVer(usuario)}
        >
          <Eye className="size-4 shrink-0 text-muted-foreground" />
          <span>Ver</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
          aria-label={`Modificar a ${nombre}`}
          title={`Modificar a ${nombre}`}
          onClick={() => onEditar(usuario)}
        >
          <Pencil className="size-4 shrink-0 text-muted-foreground" />
          <span>Editar</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
          aria-label={`Imprimir la ficha de ${nombre}`}
          title={`Imprimir la ficha de ${nombre}`}
          onClick={() => onImprimir(usuario)}
        >
          <Printer className="size-4 shrink-0 text-muted-foreground" />
          <span>Ficha</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
          aria-label={`Cambiar el rol de ${nombre}`}
          title={`Cambiar el rol de ${nombre}`}
          onClick={() => onCambiarRol(usuario)}
        >
          <ShieldUser className="size-4 shrink-0 text-muted-foreground" />
          <span>Rol</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium"
          aria-label={activa ? `Deshabilitar la cuenta de ${nombre}` : `Habilitar la cuenta de ${nombre}`}
          title={activa ? `Deshabilitar la cuenta de ${nombre}` : `Habilitar la cuenta de ${nombre}`}
          onClick={() => onCambiarEstado(usuario)}
        >
          {activa ? <UserX className="size-4 shrink-0 text-muted-foreground" /> : <UserCheck className="size-4 shrink-0 text-muted-foreground" />}
          <span>{activa ? "Baja lógica" : "Habilitar"}</span>
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="min-h-[44px] justify-center gap-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 hover:text-destructive border-destructive/20"
          aria-label={`Eliminar a ${nombre}`}
          title={`Eliminar a ${nombre}`}
          onClick={() => onEliminar(usuario)}
        >
          <Trash2 className="size-4 shrink-0" />
          <span>Baja</span>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-end gap-1">
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={`Ver el detalle de ${nombre}`}
        title={`Ver el detalle de ${nombre}`}
        onClick={() => onVer(usuario)}
      >
        <Eye className="size-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={`Modificar a ${nombre}`}
        title={`Modificar a ${nombre}`}
        onClick={() => onEditar(usuario)}
      >
        <Pencil className="size-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={`Imprimir la ficha de ${nombre}`}
        title={`Imprimir la ficha de ${nombre}`}
        onClick={() => onImprimir(usuario)}
      >
        <Printer className="size-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={`Cambiar el rol de ${nombre}`}
        title={`Cambiar el rol de ${nombre}`}
        onClick={() => onCambiarRol(usuario)}
      >
        <ShieldUser className="size-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={activa ? `Deshabilitar la cuenta de ${nombre}` : `Habilitar la cuenta de ${nombre}`}
        title={activa ? `Deshabilitar la cuenta de ${nombre}` : `Habilitar la cuenta de ${nombre}`}
        onClick={() => onCambiarEstado(usuario)}
      >
        {activa ? <UserX className="size-4" /> : <UserCheck className="size-4" />}
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        className="text-destructive hover:bg-destructive/10"
        aria-label={`Eliminar a ${nombre}`}
        title={`Eliminar a ${nombre}`}
        onClick={() => onEliminar(usuario)}
      >
        <Trash2 className="size-4" />
      </Button>
    </div>
  );
}

/**
 * Celda con nombre completo y correo electrónico.
 */
function CeldaPersona({ usuario }) {
  return (
    <div className="min-w-0">
      <p className="truncate font-medium text-foreground">{nombreCompleto(usuario)}</p>
      <p className="truncate text-xs text-muted-foreground">{usuario.email}</p>
    </div>
  );
}

/**
 * Listado de usuarios con soporte responsive (tabla en escritorio, tarjetas en móviles)
 * y controles de paginación accesibles.
 */
export function UsuariosTabla({
  usuarios = [],
  pagina = 0,
  totalPaginas = 1,
  totalElementos = 0,
  onCambiarPagina,
  onVer,
  onEditar,
  onImprimir,
  onEliminar,
  onCambiarRol,
  onCambiarEstado,
}) {
  const acciones = { onVer, onEditar, onImprimir, onEliminar, onCambiarRol, onCambiarEstado };

  return (
    <div className="space-y-4">
      {/* Vista de Escritorio y Tablets */}
      <div className="hidden rounded-xl border border-border bg-card shadow-xs md:block overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[30%]">Persona</TableHead>
              <TableHead className="w-[18%]">DNI</TableHead>
              <TableHead className="w-[18%]">Rol</TableHead>
              <TableHead className="w-[14%]">Estado</TableHead>
              <TableHead className="w-[20%] text-right">
                <span className="sr-only">Acciones</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {usuarios.map((usuario) => (
              <TableRow key={usuario.id}>
                <TableCell>
                  <CeldaPersona usuario={usuario} />
                </TableCell>
                <TableCell className="tabular-nums font-mono text-sm">
                  {dniFormateado(usuario.dni)}
                </TableCell>
                <TableCell>
                  <Badge variant="outline" className="font-normal">
                    {usuario.rol}
                  </Badge>
                </TableCell>
                <TableCell>
                  <EstadoBadge activo={usuario.activo} />
                </TableCell>
                <TableCell className="text-right">
                  <AccionesDeFila usuario={usuario} {...acciones} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Vista Móvil (Tarjetas accesibles) */}
      <ul className="space-y-3 md:hidden">
        {usuarios.map((usuario) => (
          <li
            key={usuario.id}
            className="rounded-xl border border-border bg-card p-5 sm:p-6 shadow-xs space-y-3.5"
          >
            <div className="flex items-start justify-between gap-3">
              <CeldaPersona usuario={usuario} />
              <EstadoBadge activo={usuario.activo} />
            </div>

            <dl className="grid grid-cols-2 gap-3 text-sm border-t border-border pt-3">
              <div>
                <dt className="text-xs text-muted-foreground">DNI</dt>
                <dd className="font-mono tabular-nums">{dniFormateado(usuario.dni)}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Rol</dt>
                <dd>
                  <Badge variant="outline" className="font-normal text-xs">
                    {usuario.rol}
                  </Badge>
                </dd>
              </div>
            </dl>

            <div className="border-t border-border pt-3">
              <AccionesDeFila usuario={usuario} {...acciones} esMobile={true} />
            </div>
          </li>
        ))}
      </ul>

      {/* Barra de Paginación */}
      {totalPaginas > 1 && (
        <nav
          aria-label="Paginación de usuarios"
          className="flex flex-wrap items-center justify-between gap-3 pt-2 text-sm text-muted-foreground"
        >
          <p>
            Mostrando página <strong className="text-foreground">{pagina + 1}</strong> de{" "}
            <strong className="text-foreground">{totalPaginas}</strong> ({totalElementos} en total)
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={pagina <= 0}
              onClick={() => onCambiarPagina(pagina - 1)}
              aria-label="Ir a la página anterior"
            >
              <ChevronLeft className="size-4" />
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={pagina >= totalPaginas - 1}
              onClick={() => onCambiarPagina(pagina + 1)}
              aria-label="Ir a la página siguiente"
            >
              Siguiente
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </nav>
      )}
    </div>
  );
}
