export function PelotaTenisIcon({ className = "size-7" }) {
  return (
    <svg
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`aspect-square shrink-0 ${className}`}
      aria-hidden="true"
    >
      <circle cx="16" cy="16" r="14" fill="#ccdb38" stroke="#aab92a" strokeWidth="1" />
      <path
        d="M6.5 6.5C12 11.5 12 20.5 6.5 25.5"
        stroke="#ffffff"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      <path
        d="M25.5 6.5C20 11.5 20 20.5 25.5 25.5"
        stroke="#ffffff"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
    </svg>
  );
}
