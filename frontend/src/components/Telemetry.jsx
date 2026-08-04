
export function Telemetry({ items }) {
  return (
    <div className="telemetry">
      {items.map((it) => (
        <div className="kpi" key={it.label}>
          <div className="k-label">{it.label}</div>
          <div className={`k-value ${it.tone || ''}`}>
            {it.value}
            {it.unit && <span className="k-unit">{it.unit}</span>}
          </div>
        </div>
      ))}
    </div>
  )
}
