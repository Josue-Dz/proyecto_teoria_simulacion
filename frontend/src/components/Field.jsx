export default function Field({ meta, value, onChange }) {
  const handle = (raw) => {
    if (raw === '' || raw === '-') return onChange(raw)
    const n = Number(raw)
    if (!Number.isNaN(n)) onChange(n)
  }
  const commit = () => {
    let n = Number(value)
    if (Number.isNaN(n)) n = meta.min
    n = Math.min(meta.max, Math.max(meta.min, n))
    onChange(n)
  }
  const sliderVal = Number.isNaN(Number(value)) ? meta.min : Number(value)

  return (
    <div className="field">
      <div className="field-row">
        <label>{meta.label}</label>
        <span className="sym">{meta.sym}</span>
      </div>
      <div className="field-control">
        <input
          type="range" min={meta.min} max={meta.max} step={meta.step}
          value={Math.min(meta.max, Math.max(meta.min, sliderVal))}
          onChange={(e) => onChange(Number(e.target.value))}
          aria-label={meta.label}
        />
        <div className="num-box">
          <input
            type="number" value={value} step={meta.step}
            onChange={(e) => handle(e.target.value)} onBlur={commit}
            aria-label={`${meta.label} (valor)`}
          />
          {meta.unit && <span className="unit">{meta.unit}</span>}
        </div>
      </div>
      {meta.help && <span className="help">{meta.help}</span>}
    </div>
  )
}
