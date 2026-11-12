import { useState } from "react";
import { Pencil, Plus, Tags, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VolverButton } from "@/components/ui/volver-button";
import { SuccessBanner } from "@/components/ui/success-banner";
import { useCategoriasInsumo } from "../hooks/useInsumos";
import { CategoriaDialog } from "../components/CategoriaDialog";
import { EliminarCategoriaDialog } from "../components/EliminarCategoriaDialog";

/** Alta, modificación y baja de categorías de insumo (Raqueta, Pelota, Red, ...). */
export default function CategoriasInsumoPage() {
  const { categorias, cargando, recargarCategorias } = useCategoriasInsumo();
  const [dialogo, setDialogo] = useState(null); // null | "nueva" | categoría a editar
  const [aEliminar, setAEliminar] = useState(null);
  const [aviso, setAviso] = useState("");

  async function alGuardar() {
    await recargarCategorias();
    setDialogo(null);
  }

  async function alEliminar(mensaje) {
    await recargarCategorias();
    setAEliminar(null);
    setAviso(mensaje);
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Categorías de insumo</h1>
        <VolverButton to="/insumos" />
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>

      <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-black">Todas las categorías</h2>
          <Button onClick={() => setDialogo("nueva")}>
            <Plus size={18} strokeWidth={2.5} />
            Nueva categoría
          </Button>
        </div>

        {cargando ? (
          <p className="py-10 text-center text-sm text-muted-foreground">Cargando categorías...</p>
        ) : categorias.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-12 text-center text-muted-foreground">
            <Tags size={32} />
            <p className="text-sm">Todavía no hay categorías cargadas.</p>
          </div>
        ) : (
          <ul className="divide-y divide-border">
            {categorias.map((categoria) => (
              <li key={categoria.id} className="flex items-center justify-between py-3.5">
                <span className="text-[15px] font-medium">{categoria.nombre}</span>
                <div className="flex items-center gap-0.5">
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label={`Modificar ${categoria.nombre}`}
                    onClick={() => setDialogo(categoria)}
                  >
                    <Pencil />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    className="text-destructive hover:bg-destructive/10"
                    aria-label={`Eliminar ${categoria.nombre}`}
                    onClick={() => setAEliminar(categoria)}
                  >
                    <Trash2 />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <CategoriaDialog
        key={dialogo === "nueva" ? "nueva" : `editar-${dialogo?.id ?? "ninguna"}`}
        abierto={dialogo !== null}
        categoria={dialogo === "nueva" ? null : dialogo}
        onCerrar={() => setDialogo(null)}
        onGuardada={alGuardar}
      />

      <EliminarCategoriaDialog
        key={`baja-${aEliminar?.id ?? "ninguna"}`}
        abierto={aEliminar !== null}
        categoria={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminada={alEliminar}
      />
    </div>
  );
}
