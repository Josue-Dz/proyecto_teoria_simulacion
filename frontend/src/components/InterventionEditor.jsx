import { INTERVENTION_TYPES, interventionMeta } from '../lib/fields.js'

function sanear(valor, meta) {
  let n = Number(valor)
  if (!Number.isFinite(n)) n = meta.def
  n = Math.min(meta.max, Math.max(meta.min, n))
  return meta.entero ? Math.round(n) : n
}

export default function InterventionEditor({ interventions, onChange, horizon }) {
  const add = () => {
    const t = INTERVENTION_TYPES[0]
    onChange([...interventions, { type: t.value, startDay: 30, magnitude: t.def }])
  }
  const update = (i, patch) => {
    onChange(interventions.map((iv, idx) => (idx === i ? { ...iv, ...patch } : iv)))
  }
  const remove = (i) => onChange(interventions.filter((_, idx) => idx !== i))

  const changeType = (i, value) => {
    update(i, { type: value, magnitude: interventionMeta(value).def })
  }

  const commitDia = (i, valor) => {
    let n = Math.round(Number(valor))
    if (!Number.isFinite(n)) n = 0
    update(i, { startDay: Math.min(horizon, Math.max(0, n)) })
  }
  const commitMagnitud = (i, valor, meta) => update(i, { magnitude: sanear(valor, meta) })

  return (
    <div>
      {interventions.length === 0 && (
        <p className="help" style={{ marginBottom: 12 }}>
          Sin intervenciones: se simula el brote sin respuesta. Agrega una para ver su efecto sobre la saturación.
        </p>
      )}

      {interventions.map((iv, i) => {
        const meta = interventionMeta(iv.type)

        const fueraDeRango = Number(iv.startDay) > horizon
        return (
          <div className="iv-row" key={i}>
            <div>
              <span className="help">Tipo</span>
              <select value={iv.type} onChange={(e) => changeType(i, e.target.value)}>
                {INTERVENTION_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>
            <div>
              <span className="help">Día de inicio</span>
              <input
                type="number" min={0} max={horizon} step={1} value={iv.startDay}
                onChange={(e) => update(i, { startDay: e.target.value })}
                onBlur={(e) => commitDia(i, e.target.value)}
                style={fueraDeRango ? { borderColor: 'var(--red)', color: 'var(--red-ink)' } : undefined}
              />
              {fueraDeRango && (
                <span className="help" style={{ color: 'var(--red-ink)' }}>
                  después del día {horizon}: no se aplica
                </span>
              )}
            </div>
            <div>
              <span className="help">{meta.magLabel} <span className="mono" style={{ color: 'var(--ink-3)' }}>({meta.magUnit})</span></span>
              <input
                type="number" min={meta.min} max={meta.max} step={meta.step} value={iv.magnitude}
                onChange={(e) => update(i, { magnitude: e.target.value })}
                onBlur={(e) => commitMagnitud(i, e.target.value, meta)}
              />
            </div>
            <button className="btn btn-danger-ghost btn-sm" onClick={() => remove(i)} aria-label="Quitar intervención">
              Quitar
            </button>
          </div>
        )
      })}

      <button className="btn btn-ghost btn-sm" onClick={add} style={{ marginTop: 4 }}>+ Agregar intervención</button>
    </div>
  )
}
