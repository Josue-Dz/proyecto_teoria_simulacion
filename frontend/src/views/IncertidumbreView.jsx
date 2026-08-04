import { useState } from 'react'
import { useSimulation } from '../state/useSimulation.js'
import { getUncertaintyRequest } from "../api/services/simulationService";
import { BandChart } from '../components/Charts.jsx'
import { useChartColors } from '../state/useTheme.js'
import { Telemetry } from '../components/Telemetry.jsx'
import { CardHead, Empty, Banner } from '../components/Common.jsx'
import { int, dec1, dec2, pct, ratioX } from '../lib/format.js'

const RUN_OPTIONS = [20, 50, 100, 200, 500]


const SIGMA_OPTIONS = [
  { value: 0, label: 'Sin ruido (epidemia fija)' },
  { value: 0.05, label: '± 5 % en la transmisión' },
  { value: 0.10, label: '± 10 % en la transmisión' },
  { value: 0.20, label: '± 20 % en la transmisión' },
]



const BANDS = [
  { key: 'newCases', title: 'Nuevos casos por día', sub: 'incidencia · incertidumbre del brote', tono: 'incidence' },
  { key: 'bedsOccupied', title: 'Camas ocupadas', sub: 'servidores ocupados del sistema', tono: 'teal' },
  { key: 'queueLength', title: 'Pacientes sin cama', sub: 'longitud de la cola', tono: 'amber' },
  { key: 'cumulativeDeaths', title: 'Muertes acumuladas por saturación', sub: 'costo humano del colapso', tono: 'red' },
]

export default function IncertidumbreView({ goTo }) {
  const { buildRequest, day, horizonDays } = useSimulation()
  const C = useChartColors()
  const [runs, setRuns] = useState(50)
  const [sigma, setSigma] = useState(0.10)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const ejecutar = async () => {
    setLoading(true)
    setError(null)
    try {
      
      
      const base = { ...buildRequest(), stochastic: true }
      setData(await getUncertaintyRequest(base, runs, sigma))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const k = data?.medianKpis
  const pSat = data?.saturationProbability

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">04 · Variabilidad</span>
        <h1>Incertidumbre</h1>
        <p>
          Una sola corrida estocástica es una anécdota. Aquí se repite la simulación N veces con
          semillas distintas y se resume el resultado en bandas de percentiles: la mediana marca lo
          típico y el rango p05–p95 marca hasta dónde puede irse el sistema por puro azar.
        </p>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Análisis de Monte Carlo" sub="usa la configuración actual del panel Escenario" />
        <div className="card-pad">
          <div className="row" style={{ gap: 20, flexWrap: 'wrap' }}>
            <div className="field" style={{ margin: 0, minWidth: 220 }}>
              <div className="field-row">
                <label>Número de realizaciones</label>
                <span className="sym">N</span>
              </div>
              <div className="field-control">
                <select className="select" value={runs} onChange={(e) => setRuns(Number(e.target.value))}>
                  {RUN_OPTIONS.map((n) => <option key={n} value={n}>{n} corridas</option>)}
                </select>
              </div>
              <span className="help">Más corridas = bandas más estables, pero más tiempo de cálculo.</span>
            </div>

            <div className="field" style={{ margin: 0, minWidth: 260 }}>
              <div className="field-row">
                <label>Incertidumbre de la transmisión</label>
                <span className="sym">σ</span>
              </div>
              <div className="field-control">
                <select className="select" value={sigma} onChange={(e) => setSigma(Number(e.target.value))}>
                  {SIGMA_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              </div>
              <span className="help">Cuánto se desconoce β. Con 0, el brote es idéntico en todas las corridas.</span>
            </div>

            <div style={{ marginLeft: 'auto' }}>
              <button className="btn btn-primary" onClick={ejecutar} disabled={loading}>
                {loading ? <><span className="spinner" /> Ejecutando {runs} corridas…</> : `Ejecutar ${runs} corridas`}
              </button>
            </div>
          </div>

          <p className="help" style={{ marginTop: 14, marginBottom: 0 }}>
            Cada corrida combina dos fuentes de azar: un multiplicador lognormal sobre la transmisión
            (incertidumbre del <b>brote</b>) y la cola hospitalaria con llegadas de Poisson y servicios
            exponenciales (riesgo <b>operativo</b>). Con σ = 0 solo queda la segunda.
            {sigma > 0 && (
              <> Ojo al leer las bandas de incidencia: los percentiles se calculan día a día, y como el
              ruido en β desplaza <i>cuándo</i> ocurre el pico, la mediana aplana la curva. La p50 no es
              una trayectoria posible, es el valor típico de cada día por separado.</>
            )}
          </p>

          {error && <div style={{ marginTop: 12 }}><Banner kind="err">{error}</Banner></div>}
        </div>
      </div>

      {!data ? (
        <Empty
          title="Sin análisis todavía"
          action={<button className="btn btn-ghost" onClick={() => goTo('escenario')}>Ajustar escenario</button>}
        >
          Ejecuta el análisis para ver las bandas p05–p95 y la probabilidad de que el hospital
          colapse bajo este escenario.
        </Empty>
      ) : (
        <>
          <Telemetry items={[
            {
              label: 'P(saturación)',
              value: pct(pSat, 0),
              tone: pSat >= 0.5 ? 'crit' : pSat > 0 ? 'warn' : '',
              unit: `${data.runs} corridas`,
            },
            {
              label: 'Día de saturación (mediana)',
              value: k.saturationDay != null ? int(k.saturationDay) : 'nunca',
              tone: k.saturated ? 'crit' : '',
            },
            { label: 'Presión pico (mediana)', value: ratioX(k.peakPressureRatio), tone: k.peakPressureRatio > 1 ? 'crit' : '' },
            { label: 'Muertes (mediana)', value: int(k.deathsFromSaturation), tone: k.deathsFromSaturation > 0 ? 'crit' : '' },
          ]} />
          <div style={{ height: 12 }} />
          <Telemetry items={[
            { label: 'Déficit máx. de camas (mediana)', value: int(k.maxBedDeficit), tone: k.maxBedDeficit > 0 ? 'warn' : '' },
            { label: 'Espera media (mediana)', value: dec1(k.avgWaitDays), unit: 'días' },
            { label: 'Hospitalizaciones (mediana)', value: int(k.totalHospitalizations) },
            { label: 'R₀', value: dec2(k.r0) },
          ]} />

          <div className="card" style={{ marginTop: 20 }}>
            <CardHead
              title="Lectura del riesgo"
              sub={`${data.runs} realizaciones · horizonte ${horizonDays} días`}
            />
            <div className="card-pad">
              <p className="prose" style={{ margin: 0 }}>
                {pSat >= 0.99
                  ? <>El hospital se saturó en <b>todas</b> las corridas. Con esta configuración el colapso no es un riesgo: es el resultado esperado, y la única pregunta es cuándo ocurre y cuánto dura.</>
                  : pSat <= 0.01
                    ? <>El hospital <b>no se saturó en ninguna</b> corrida. La capacidad absorbe el brote incluso en las realizaciones más desfavorables.</>
                    : <>El hospital se saturó en <b>{pct(pSat, 0)}</b> de las corridas. El resultado depende del azar en las llegadas: hay escenarios donde el sistema aguanta y otros donde no, así que la decisión de capacidad debe tomarse sobre el peor caso creíble (banda p95), no sobre la mediana.</>}
              </p>
            </div>
          </div>

          <div style={{ marginTop: 18, display: 'grid', gap: 18 }}>
            {BANDS.map((b) => (
              <div className="card" key={b.key}>
                <CardHead title={b.title} sub={`${b.sub} · mediana p50 con rango p05–p95`} />
                <div className="card-pad">
                  <BandChart
                    points={data.bands[b.key] ?? []}
                    color={C[b.tono]}
                    name={`${b.title} (mediana)`}
                    height={260}
                    marker={day}
                  />
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
