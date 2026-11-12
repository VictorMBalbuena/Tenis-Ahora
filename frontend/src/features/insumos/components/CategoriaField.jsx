import { SelectField } from "@/components/ui/select-field";

/** Selector de categoría de insumo. Las categorías se administran en /insumos/categorias. */
export function CategoriaField({ categorias, value, onChange, error }) {
  return (
    <div className="space-y-2">
      <label className="block text-[14px] font-semibold text-black">Categoría</label>
      <SelectField
        name="categoriaId"
        value={value}
        onChange={onChange}
        opciones={categorias.map((c) => ({ value: String(c.id), label: c.nombre }))}
      />
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
