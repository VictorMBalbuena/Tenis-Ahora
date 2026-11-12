// Aviso de éxito tras una operación (alta, baja, edición)par de ErrorBanner.
export function SuccessBanner({ children }) {
  return (
    <div aria-live="polite" role="status">
      {children && (
        <div className="rounded-lg bg-[var(--estado-ok-bg)] px-3.5 py-2.5 text-sm text-[var(--estado-ok-fg)]">
          {children}
        </div>
      )}
    </div>
  );
}
