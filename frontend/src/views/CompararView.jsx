import { useEffect, useState } from 'react'
import { useSimulation } from '../state/useSimulation.js'
import { compareScenariosRequest } from "../api/services/simulationService";
import { getScenariosRequest } from "../api/services/scenarioService";
import { TimeSeries } from '../components/Charts.jsx'
import InterventionEditor from '../components/InterventionEditor.jsx'
import { useChartColors } from '../state/useTheme.js'
import { CardHead, Empty, Banner } from '../components/Common.jsx'
import { computeR0 } from '../lib/r0.js'
import { MODEL_FIELDS, HOSPITAL_FIELDS, interventionMeta } from '../lib/fields.js'
import { int, dec2, pct, ratioX } from '../lib/format.js'

const CAMAS = HOSPITAL_FIELDS.find((f) => f.key === 'initialBeds')

function acotarCamas(valor) {
  const n = Math.round(Number(valor))
  if (!Number.isFinite(n)) return CAMAS.min
  return Math.min(CAMAS.max, Math.max(CAMAS.min, n))
}



const RANURAS = ['teal', 'amber', 'slate']
const LETRAS = ['A', 'B', 'C']
const MAXIMO = 3


function merge(results, key) {
  const n = Math.min(...results.map((r) => r.result.series.length))
  const rows = []
  for (let i = 0; i < n; i++) {
    const row = { day: results[0].result.series[i].day }
    results.forEach((r, idx) => { row[`s${idx}`] = r.result.series[i][key] })
    rows.push(row)
  }
  return rows
}

const METRICS = [
  { label: 'R₀', get: (k) => dec2(k.r0) },
  { label: 'Día de saturación', get: (k) => (k.saturationDay != null ? `día ${k.saturationDay}` : 'no satura') },
  { label: 'Presión pico', get: (k) => ratioX(k.peakPressureRatio) },
  { label: 'Muertes por saturación', get: (k) => int(k.deathsFromSaturation) },
  { label: 'Hospitalizaciones', get: (k) => int(k.totalHospitalizations) },
  { label: 'Tasa de ataque final', get: (k) => pct(k.finalAttackRate) },
]


function resumeIntervencion(iv) {
  const meta = interventionMeta(iv.type)
  return `${meta.label} · día ${iv.startDay} · ${iv.magnitude}`
}


function parametrosQueCambian(scenarios) {
  const filas = []
  const revisar = (campos, grupo) => {
    for (const f of campos) {
      const valores = scenarios.map((s) => s.request[grupo]?.[f.key])
      const distintos = new Set(valores.map((v) => Number(v).toFixed(6))).size > 1
      
      const fmt = f.step >= 1 ? int : dec2
      if (distintos) filas.push({ label: f.label, unit: f.unit, valores: valores.map(fmt) })
    }
  }
  revisar(MODEL_FIELDS, 'model')
  revisar(HOSPITAL_FIELDS, 'hospital')

  const intervenciones = scenarios.map((s) => (s.request.interventions ?? []).map(resumeIntervencion))
  const firmas = new Set(intervenciones.map((l) => l.join(' | ')))
  if (firmas.size > 1) {
    filas.push({
      label: 'Intervenciones', unit: '',
      valores: intervenciones.map((l) => (l.length ? l.join(' · ') : 'ninguna')),
    })
  }
  return filas
}


function TarjetaEscenario({ escenario, color, letra, onRename, onPatch, onDuplicar, onQuitar, puedeDuplicar }) {
  const req = escenario.request
  return (
    <div className="cmp-scn">
      <div className="cmp-scn-head">
        <span className="dot" style={{ background: color, boxShadow: 'none' }} />
        <input
          className="num-box cmp-scn-name" value={escenario.name} maxLength={150}
          onChange={(e) => onRename(e.target.value)}
          aria-label={`Nombre del escenario ${letra}`}
        />
        <span className="help mono cmp-scn-meta">
          R₀ {dec2(computeR0(req.model))} · {int(req.horizonDays)} días · {req.stochastic ? 'estocástico' : 'determinista'}
          {escenario.origen && ` · ${escenario.origen}`}
        </span>
        <div className="row gap-8" style={{ marginLeft: 'auto' }}>
          <button className="btn btn-ghost btn-sm" onClick={onDuplicar} disabled={!puedeDuplicar}
            title="Copia este escenario para cambiarle una sola cosa">Duplicar</button>
          <button className="btn btn-danger-ghost btn-sm" onClick={onQuitar}>Quitar</button>
        </div>
      </div>

      <div className="cmp-scn-body">
        <label className="cmp-camas">
          <span className="help">Camas iniciales</span>
          
          <input
            type="number" min={CAMAS.min} max={CAMAS.max} step={CAMAS.step}
            value={req.hospital.initialBeds}
            onChange={(e) => onPatch({ hospital: { ...req.hospital, initialBeds: e.target.value } })}
            onBlur={(e) => onPatch({ hospital: { ...req.hospital, initialBeds: acotarCamas(e.target.value) } })}
          />
        </label>
        <div className="cmp-iv">
          <span className="help" style={{ display: 'block', marginBottom: 6 }}>Intervenciones</span>
          <InterventionEditor
            interventions={req.interventions ?? []}
            horizon={req.horizonDays}
            onChange={(next) => onPatch({ interventions: next })}
          />
        </div>
      </div>
    </div>
  )
}

export default function CompararView() {
  const { buildRequest, model, stochastic } = useSimulation()
  const C = useChartColors()
  const PALETA = RANURAS.map((k) => C[k])
  const [scenarios, setScenarios] = useState([])
  const [results, setResults] = useState(null)
  const [desfasado, setDesfasado] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [guardados, setGuardados] = useState([])

  const liveR0 = computeR0(model)
  const lleno = scenarios.length >= MAXIMO

  
  useEffect(() => {
    let vivo = true
    getScenariosRequest()
      .then((l) => { if (vivo) setGuardados(l ?? []) })
      .catch(() => { /* sin biblioteca disponible: el resto de la vista sigue sirviendo */ })
    return () => { vivo = false }
  }, [])

  const agregar = (name, request, origen) => {
    if (scenarios.length >= MAXIMO) return
    setScenarios((prev) => [...prev, { name, request, origen }])
    setDesfasado(true)
  }

  const capturar = () => agregar(`Escenario ${LETRAS[scenarios.length]}`, buildRequest(), 'configuración actual')

  const cargarGuardado = (id) => {
    const g = guardados.find((s) => s.id === id)
    if (g) agregar(g.name, structuredClone(g.request), 'biblioteca')
  }

  const duplicar = (i) => {
    const base = scenarios[i]
    agregar(`${base.name} (copia)`, structuredClone(base.request), base.origen)
  }

  
  const renombrar = (i, name) => {
    setScenarios((prev) => prev.map((s, j) => (j === i ? { ...s, name } : s)))
    setResults((prev) => (prev ? prev.map((r, j) => (j === i ? { ...r, name } : r)) : prev))
  }

  const parchar = (i, patch) => {
    setScenarios((prev) => prev.map((s, j) => (j === i ? { ...s, request: { ...s.request, ...patch } } : s)))
    setDesfasado(true)
  }

  const quitar = (i) => {
    setScenarios((prev) => prev.filter((_, j) => j !== i))
    setDesfasado(true)
  }

  const limpiar = () => {
    setScenarios([]); setResults(null); setError(null); setDesfasado(false)
  }

  const comparar = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await compareScenariosRequest(scenarios.map((s) => ({ name: s.name, request: s.request })))
      setResults((res.results || []).map((r) => ({ name: r.name, result: r.result })))
      setDesfasado(false)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const overlaySeries = results
    ? results.map((r, idx) => ({ key: `s${idx}`, name: r.name, color: PALETA[idx] }))
    : []
  const cambios = scenarios.length >= 2 ? parametrosQueCambian(scenarios) : []

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">05 · Contrafactuales</span>
        <h1>Comparar</h1>
        <p>
          Enfrenta hasta tres versiones del mismo brote en una sola gráfica. Arma cada versión aquí
          mismo: captura tu configuración actual, cárgala de la Biblioteca o <b>duplica</b> una que ya
          tengas y cámbiale una sola cosa, que es la forma limpia de aislar el efecto de una medida.
        </p>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Escenarios a comparar" sub={`${scenarios.length} de ${MAXIMO} · editables sin salir de esta vista`} />
        <div className="card-pad">
          <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
            <div className="help">
              Configuración actual: R₀ ≈ <b className="mono">{dec2(liveR0)}</b> · población{' '}
              <b className="mono">{int(model.populationHuman)}</b> · modo{' '}
              <b>{stochastic ? 'estocástico' : 'determinista'}</b>
            </div>
            <div className="row gap-8" style={{ flexWrap: 'wrap' }}>
              <button className="btn btn-ghost btn-sm" onClick={capturar} disabled={lleno}>
                + Capturar configuración actual
              </button>
              <select
                className="cmp-lib" value="" disabled={lleno || guardados.length === 0}
                onChange={(e) => { if (e.target.value) cargarGuardado(e.target.value) }}
                aria-label="Cargar escenario de la Biblioteca"
              >
                <option value="">
                  {guardados.length ? '+ Cargar de Biblioteca…' : 'Biblioteca vacía'}
                </option>
                {guardados.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </select>
              {scenarios.length > 0 && (
                <button className="btn btn-ghost btn-sm" onClick={limpiar}>Limpiar</button>
              )}
            </div>
          </div>

          {scenarios.length === 0 ? (
            <div className="help" style={{ marginTop: 14 }}>
              Aún no has agregado escenarios. Necesitas al menos dos para comparar.
            </div>
          ) : (
            <div style={{ marginTop: 14 }}>
              {scenarios.map((s, i) => (
                <TarjetaEscenario
                  key={i}
                  escenario={s}
                  color={PALETA[i]}
                  letra={LETRAS[i]}
                  puedeDuplicar={!lleno}
                  onRename={(name) => renombrar(i, name)}
                  onPatch={(patch) => parchar(i, patch)}
                  onDuplicar={() => duplicar(i)}
                  onQuitar={() => quitar(i)}
                />
              ))}
            </div>
          )}

          <div className="row gap-12" style={{ marginTop: 16, flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={comparar} disabled={scenarios.length < 2 || loading}>
              {loading ? <><span className="spinner" /> Comparando…</> : `Comparar ${scenarios.length} escenarios`}
            </button>
            {scenarios.length < 2 && <span className="help">Necesitas al menos 2 escenarios.</span>}
            {scenarios.length >= 2 && cambios.length === 0 && (
              <span className="help">
                Ojo: los escenarios son idénticos. Cámbiale algo a uno para que la comparación diga algo.
              </span>
            )}
          </div>

          {error && <div style={{ marginTop: 12 }}><Banner kind="err">{error}</Banner></div>}
          {results && desfasado && (
            <div style={{ marginTop: 12 }}>
              <Banner kind="warn">
                Cambiaste los escenarios desde la última comparación: las gráficas de abajo aún
                muestran los valores anteriores. Vuelve a pulsar «Comparar».
              </Banner>
            </div>
          )}
        </div>
      </div>

      {scenarios.length >= 2 && cambios.length > 0 && (
        <div className="card" style={{ marginBottom: 18 }}>
          <CardHead title="Qué cambia entre ellos" sub="solo los parámetros en los que no coinciden" />
          <div className="card-pad" style={{ overflowX: 'auto' }}>
            <table className="data">
              <thead>
                <tr>
                  <th>Parámetro</th>
                  {scenarios.map((s, i) => (
                    <th key={i} className="num">
                      <span className="dot" style={{ background: PALETA[i], boxShadow: 'none', marginRight: 6 }} />
                      {s.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {cambios.map((f) => (
                  <tr key={f.label}>
                    <td>{f.label} {f.unit && <span className="help mono">{f.unit}</span>}</td>
                    {f.valores.map((v, i) => <td key={i} className="num">{v}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!results ? (
        <Empty title="Sin comparación todavía">
          Agrega dos o más escenarios y pulsa <b>Comparar</b> para superponer sus curvas epidémicas
          y su presión hospitalaria.
        </Empty>
      ) : (
        <>
          <div className="card" style={{ marginBottom: 18 }}>
            <CardHead title="Resumen comparativo" sub="indicadores clave por escenario" />
            <div className="card-pad" style={{ overflowX: 'auto' }}>
              <table className="data">
                <thead>
                  <tr>
                    <th>Indicador</th>
                    {results.map((r, i) => (
                      <th key={i} className="num">
                        <span className="dot" style={{ background: PALETA[i], boxShadow: 'none', marginRight: 6 }} />
                        {r.name}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {METRICS.map((m) => (
                    <tr key={m.label}>
                      <td>{m.label}</td>
                      {results.map((r, i) => (
                        <td key={i} className="num">{m.get(r.result.kpis)}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid cols-2">
            <div className="card">
              <CardHead title="Infecciosos humanos" sub="I_h · tamaño del brote" />
              <div className="card-pad">
                <TimeSeries data={merge(results, 'ih')} series={overlaySeries} height={280} yLabel="personas" />
              </div>
            </div>
            <div className="card">
              <CardHead title="Pacientes sin cama" sub="cola · overflow hospitalario" />
              <div className="card-pad">
                <TimeSeries data={merge(results, 'queueLength')} series={overlaySeries} height={280} yLabel="en cola" />
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
