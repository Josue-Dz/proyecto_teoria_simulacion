
const DASH = '—';

export function int(v) {
  if (v == null || Number.isNaN(v)) return DASH;
  return Math.round(v).toLocaleString('es-HN');
}

export function dec1(v) {
  if (v == null || Number.isNaN(v)) return DASH;
  return Number(v).toLocaleString('es-HN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
}

export function dec2(v) {
  if (v == null || Number.isNaN(v)) return DASH;
  return Number(v).toLocaleString('es-HN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function pct(v, decimals = 0) {
  if (v == null || Number.isNaN(v)) return DASH;
  return `${(v * 100).toFixed(decimals)}%`;
}

export function ratioX(v) {
  if (v == null || Number.isNaN(v)) return DASH;
  return `${Number(v).toFixed(1)}×`;
}

export function fechaHora(iso) {
  if (!iso) return DASH;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return DASH;
  return d.toLocaleString('es-HN', {
    day: '2-digit', month: '2-digit', year: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
}
