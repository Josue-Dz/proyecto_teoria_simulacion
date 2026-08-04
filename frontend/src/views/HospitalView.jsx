import { useSimulation } from '../state/useSimulation.js'
import { TimeSeries } from '../components/Charts.jsx'
import { useChartColors } from '../state/useTheme.js'
import { Empty, CardHead } from '../components/Common.jsx'
import { int, dec2, pct, dec1 } from '../lib/format.js'

const MAX_CELLS = 480

function BedGrid({ capacity, occupancyRatio }) {
  const cap = Math.max(0, Math.round(capacity))
  const scaled = cap > MAX_CELLS
  const cells = scaled ? MAX_CELLS : cap
  const bedsPerCell = scaled ? cap / MAX_CELLS : 1
  const occCells = Math.round((occupancyRatio || 0) * cells)

  return (
    <div>
      <div className="bedgrid" role="img" aria-label={`${pct(occupancyRatio)} de camas ocupadas`}>
        {Array.from({ length: cells }).map((_, i) => (
          <span key={i} className={`bed ${i < occCells ? 'occ' : 'free'}`} />
        ))}
      </div>
      <div className="legend">
        <span><i style={{ background: 'var(--teal)' }} /> Ocupada</span>
        <span><i style={{ background: 'var(--surface-2)', border: '1px solid var(--line-strong)' }} /> Libre</span>
        {scaled && <span className="help">cada celda ≈ {dec1(bedsPerCell)} camas</span>}
      </div>
    </div>
  )
}

export default function HospitalView({ goTo }) {
  const { result, day, current } = useSimulation()
  const C = useChartColors()

  if (!result) {
    return (
      <Empty title="Sin datos hospitalarios"
        action={<button className="btn btn-primary" onClick={() => goTo('escenario')}>Ir a Escenario</button>}>
        Ejecuta una simulación para inspeccionar la ocupación de camas, la cola y el diagnóstico de colas día a día.
      </Empty>
    )
  }

  const data = result.series
  const saturated = current && (current.rho >= 1 || current.waitDaysQueue < 0)

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">03 · Sala</span>
        <h1>Hospital</h1>
        <p>La rejilla y el diagnóstico reflejan el día seleccionado en el scrubber. Mueve el control para ver cómo se llena la sala.</p>
      </div>

      <div className="grid" style={{ gridTemplateColumns: '1.5fr 1fr' }}>
        <div className="card">
          <CardHead title={`Camas · día ${day}`}
            sub={current ? `${int(current.bedsOccupied)} / ${int(current.capacity)} ocupadas` : ''}
            right={<span className="mono" style={{ fontSize: 13, color: saturated ? 'var(--red-ink)' : 'var(--ink-2)' }}>
              {current ? pct(current.occupancyRatio) : ''}
            </span>} />
          <div className="card-pad">
            {current && <BedGrid capacity={current.capacity} occupancyRatio={current.occupancyRatio} />}
          </div>
        </div>

        <div className="card">
          <CardHead title="Diagnóstico de colas" sub="Erlang-C (M/M/c) · overlay analítico" />
          <div className="card-pad">
            {current && (
              <table className="data">
                <tbody>
                  <tr><td>En cola ahora</td><td className="num">{int(current.queueLength)}</td></tr>
                  <tr><td>Demanda del día</td><td className="num">{int(current.bedDemand)}</td></tr>
                  <tr><td>Ingresos / altas</td><td className="num">{int(current.admissions)} / {int(current.discharges)}</td></tr>
                  <tr>
                    <td>Se fueron sin cama<div className="help">superaron la espera máxima</div></td>
                    <td className="num" style={{ color: current.leftWithoutBed > 0 ? 'var(--amber)' : undefined }}>
                      {int(current.leftWithoutBed)}
                    </td>
                  </tr>
                  <tr><td>Carga ofrecida (a)</td><td className="num">{dec1(current.offeredLoad)} Erlang</td></tr>
                  <tr><td>Utilización (ρ)</td>
                    <td className="num" style={{ color: current.rho >= 1 ? 'var(--red-ink)' : undefined }}>{dec2(current.rho)}</td></tr>
                  <tr><td>Prob. de esperar</td><td className="num">{pct(current.probWait)}</td></tr>
                  <tr><td>Espera media (W_q)</td>
                    <td className="num">{current.waitDaysQueue < 0 ? 'SATURADO' : `${dec1(current.waitDaysQueue)} días`}</td></tr>
                </tbody>
              </table>
            )}
            {saturated && (
              <div className="banner err" style={{ marginTop: 12 }}>
                ρ ≥ 1: la cola no se estabiliza. El sistema está saturado en este día.
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="grid cols-2" style={{ marginTop: 18 }}>
        <div className="card">
          <CardHead title="Cola de espera" sub="pacientes que aguardan una cama" />
          <div className="card-pad">
            <TimeSeries
              data={data} height={260}
              yLabel="pacientes"
              series={[{ key: 'queueLength', name: 'En cola', color: C.amber, area: true }]}
              marker={day}
            />
          </div>
        </div>

        <div className="card">
          <CardHead title="Utilización del sistema" sub="ρ = carga / capacidad · línea crítica en 1" />
          <div className="card-pad">
            <TimeSeries
              data={data} height={260}
              series={[{ key: 'rho', name: 'ρ (utilización)', color: C.slate, area: true }]}
              refs={[{ x: result.kpis.saturationDay, color: C.red, label: 'saturación' }].filter((r) => r.x != null)}
              marker={day}
            />
          </div>
        </div>

        <div className="card">
          <CardHead title="Movimiento de camas" sub="el hospital funcionando: quién entra y quién sale" />
          <div className="card-pad">
            <TimeSeries
              data={data} height={230}
              yLabel="por día"
              series={[
                { key: 'admissions', name: 'Ingresos', color: C.teal },
                { key: 'discharges', name: 'Altas', color: C.green },
              ]}
              marker={day}
            />
          </div>
        </div>

        <div className="card">
          <CardHead title="Salidas sin atención" sub="el costo de la saturación" />
          <div className="card-pad">
            <TimeSeries
              data={data} height={230}
              yLabel="por día"
              series={[
                { key: 'deaths', name: 'Muertes en cola', color: C.red },
                { key: 'leftWithoutBed', name: 'Se van sin cama', color: C.amber, dashed: true },
              ]}
              marker={day}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
