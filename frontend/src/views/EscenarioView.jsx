import { useSimulation } from '../state/useSimulation.js'
import { MODEL_FIELDS, HOSPITAL_FIELDS } from '../lib/fields.js'
import { computeR0, computeRe0, herdImmunityThreshold } from '../lib/r0.js'
import { dec2, pct } from '../lib/format.js'
import Field from '../components/Field.jsx'
import InterventionEditor from '../components/InterventionEditor.jsx'
import { Banner, CardHead } from '../components/Common.jsx'

function acotarSemilla(valor) {
  if (valor === '') return ''
  const n = Math.trunc(Number(valor))
  if (!Number.isFinite(n)) return ''
  return String(Math.min(Number.MAX_SAFE_INTEGER, Math.max(0, n)))
}

export default function EscenarioView({ goTo }) {
  const s = useSimulation()
  const r0 = computeR0(s.model)
  const re0 = computeRe0(s.model)
  const umbral = herdImmunityThreshold(s.model)
  const r0Tone = Number.isNaN(r0) ? '' : r0 >= 1 ? 'crit' : 'ok'

  const handleRun = async () => {
    const ok = await s.run()
    if (ok) goTo('simulacion')
  }

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">01 · Configuración</span>
        <h1>Escenario</h1>
        <p>
          Ajusta cualquier variable del modelo y de la respuesta hospitalaria, define las intervenciones
          en la línea de tiempo y ejecuta la corrida. Todo lo que ves aquí es manipulable.
        </p>
      </div>

      {s.connWarning && (
        <Banner kind="err">
          No hay conexión con el backend (localhost:8080). Puedes editar parámetros, pero necesitas
          Spring Boot corriendo para ejecutar la simulación.
        </Banner>
      )}
      {s.error && <Banner kind="err">{s.error}</Banner>}

      <div className="card card-pad" style={{ margin: '16px 0 20px', display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
        <div className="stat-chip">
          <span className="label">R₀ estimado</span>
          <span className="value" style={{ color: r0Tone === 'crit' ? 'var(--red-ink)' : 'var(--green)', fontSize: 26 }}>
            {Number.isNaN(r0) ? '—' : dec2(r0)}
          </span>
        </div>
        <div className="stat-chip">
          <span className="label">R efectivo · día 0</span>
          <span className="value" style={{ color: re0 >= 1 ? 'var(--red-ink)' : 'var(--green)', fontSize: 26 }}>
            {Number.isNaN(re0) ? '—' : dec2(re0)}
          </span>
        </div>
        <p className="help" style={{ maxWidth: 360, margin: 0 }}>
          {Number.isNaN(re0)
            ? 'Revisa los parámetros.'
            : re0 >= 1
              ? <>R_e &gt; 1: el brote crece. Con esta inmunidad previa, haría falta
                  llegar al <b>{pct(umbral)}</b> de inmunes para frenarlo.</>
              : <>R_e &lt; 1: el brote no despega. La inmunidad previa ya supera el
                  umbral de <b>{pct(umbral)}</b>, así que se apaga solo.</>}
        </p>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 10 }}>
          <label className="toggle">
            <input type="checkbox" checked={s.stochastic} onChange={(e) => s.setStochastic(e.target.checked)} />
            <span className="track" />
            <span style={{ fontSize: 13 }}>Estocástico (cola DES)</span>
          </label>
          <button className="btn btn-primary" onClick={handleRun} disabled={s.loading}>
            {s.loading ? <><span className="spinner" /> Ejecutando…</> : 'Ejecutar simulación'}
          </button>
        </div>
      </div>

      <div className="grid cols-2">
        <div className="card">
          <CardHead title="Modelo epidemiológico" sub="SEIR-SEI · 7 compartimentos" />
          <div className="card-pad">
            {MODEL_FIELDS.map((meta) => (
              <Field key={meta.key} meta={meta} value={s.model[meta.key]}
                onChange={(v) => s.setModelField(meta.key, v)} />
            ))}
          </div>
        </div>

        <div className="card">
          <CardHead title="Capacidad hospitalaria" sub="Cola de camas finitas" />
          <div className="card-pad">
            {HOSPITAL_FIELDS.map((meta) => (
              <Field key={meta.key} meta={meta} value={s.hospital[meta.key]}
                onChange={(v) => s.setHospitalField(meta.key, v)} />
            ))}
          </div>
        </div>
      </div>

      <div className="grid cols-2" style={{ marginTop: 18 }}>
        <div className="card">
          <CardHead title="Corrida" sub="Horizonte y aleatoriedad" />
          <div className="card-pad">
            <Field
              meta={{ label: 'Horizonte de simulación', sym: 'T', unit: 'días', min: 30, max: 1095, step: 5,
                help: 'Duración total de la simulación.' }}
              value={s.horizonDays} onChange={s.setHorizonDays}
            />
            <div className="field">
              <div className="field-row">
                <label>Semilla</label>
                <span className="sym">seed</span>
              </div>
              <div className="field-control">
                <div className="num-box" style={{ minWidth: 160 }}>
                  <input
                    type="number" placeholder="aleatoria" value={s.seed}
                    min={0} max={Number.MAX_SAFE_INTEGER} step={1}
                    onChange={(e) => s.setSeed(e.target.value)}
                    onBlur={(e) => s.setSeed(acotarSemilla(e.target.value))}
                    disabled={!s.stochastic}
                    aria-label="Semilla"
                  />
                </div>
              </div>
              <span className="help">
                Solo aplica en modo estocástico. Fija la semilla para reproducir exactamente una corrida;
                déjala vacía para una semilla aleatoria.
              </span>
            </div>
          </div>
        </div>

        <div className="card">
          <CardHead title="Intervenciones" sub="Línea de tiempo de respuesta" />
          <div className="card-pad">
            <InterventionEditor
              interventions={s.interventions}
              onChange={s.setInterventions}
              horizon={s.horizonDays}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
