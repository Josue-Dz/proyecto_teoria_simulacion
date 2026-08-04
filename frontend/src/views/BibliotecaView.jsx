import { useCallback, useEffect, useState } from 'react'
import { useSimulation } from '../state/useSimulation.js'
import {
  getScenariosRequest,
  createScenarioRequest,
  deleteScenarioRequest,
  runScenarioRequest,
} from "../api/services/scenarioService";
import { getRunsRequest } from "../api/services/runService";
import { CardHead, Banner } from '../components/Common.jsx'
import { computeR0 } from '../lib/r0.js'
import { int, dec2, fechaHora } from '../lib/format.js'


export default function BibliotecaView({ goTo }) {
  const { buildRequest, loadRequest, applyResult, model, hospital, horizonDays, stochastic } = useSimulation()

  const [escenarios, setEscenarios] = useState([])
  const [corridas, setCorridas] = useState([])
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [error, setError] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [ocupadoId, setOcupadoId] = useState(null)

  const leerTodo = () => Promise.all([getScenariosRequest(), getRunsRequest()])

  const aplicar = useCallback(([scs, rns]) => {
    setEscenarios(scs ?? [])
    setCorridas(rns ?? [])
    setError(null)
  }, [])

  const recargar = useCallback(async () => {
    try {
      aplicar(await leerTodo())
    } catch (e) {
      setError(`No se pudo leer la base de datos: ${e.message}`)
    }
  }, [aplicar])

  
  
  useEffect(() => {
    let vivo = true
    leerTodo()
      .then((datos) => { if (vivo) aplicar(datos) })
      .catch((e) => { if (vivo) setError(`No se pudo leer la base de datos: ${e.message}`) })
    return () => { vivo = false }
  }, [aplicar])

  const guardar = async () => {
    if (!nombre.trim()) return
    setGuardando(true)
    setError(null)
    try {
      await createScenarioRequest({ name: nombre.trim(), description: descripcion.trim() || null, request: buildRequest() })
      setNombre('')
      setDescripcion('')
      setAviso('Escenario guardado.')
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setGuardando(false)
    }
  }

  const cargar = (esc) => {
    loadRequest(esc.request)
    goTo('escenario')
  }

  const ejecutar = async (esc) => {
    setOcupadoId(esc.id)
    setError(null)
    try {
      const res = await runScenarioRequest(esc.id)
      applyResult(res)
      await recargar()
      goTo('simulacion')
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupadoId(null)
    }
  }

  const borrar = async (esc) => {
    if (!window.confirm(`¿Borrar el escenario "${esc.name}"? Esta acción no se puede deshacer.`)) return
    setOcupadoId(esc.id)
    setError(null)
    try {
      await deleteScenarioRequest(esc.id)
      setAviso(`Escenario "${esc.name}" borrado.`)
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupadoId(null)
    }
  }

  const nombrePorId = Object.fromEntries(escenarios.map((e) => [e.id, e.name]))
  const r0Actual = computeR0(model)

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">06 · Persistencia</span>
        <h1>Biblioteca</h1>
        <p>
          Los escenarios se guardan en la base de datos con todos sus parámetros e intervenciones.
          Un escenario guardado más su semilla reproduce la corrida exacta, y cada ejecución deja
          su ficha estadística en el historial de abajo.
        </p>
      </div>

      {error && <Banner kind="err">{error}</Banner>}
      {aviso && !error && <div style={{ marginBottom: 14 }}><Banner kind="info">{aviso}</Banner></div>}

      <div className="card" style={{ margin: '16px 0 18px' }}>
        <CardHead title="Guardar la configuración actual" sub="lo que tengas ahora mismo en el panel Escenario" />
        <div className="card-pad">
          <div className="help" style={{ marginBottom: 14 }}>
            R₀ ≈ <b className="mono">{dec2(r0Actual)}</b> · {int(hospital.initialBeds)} camas ·
            horizonte <b className="mono">{horizonDays}</b> días · modo <b>{stochastic ? 'estocástico' : 'determinista'}</b>
          </div>
          <div className="iv-row" style={{ gridTemplateColumns: '1fr 1.6fr auto' }}>
            <div>
              <span className="help">Nombre</span>
              <input
                value={nombre} onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej. Fumigación en el día 40" maxLength={150}
              />
            </div>
            <div>
              <span className="help">Descripción (opcional)</span>
              <input
                value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
                placeholder="Qué hipótesis estás probando"
              />
            </div>
            <button className="btn btn-primary btn-sm" onClick={guardar} disabled={guardando || !nombre.trim()}>
              {guardando ? <><span className="spinner" /> Guardando…</> : 'Guardar escenario'}
            </button>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Escenarios guardados" sub={`${escenarios.length} en la base de datos`} />
        <div className="card-pad">
          {escenarios.length === 0 ? (
            <p className="help" style={{ margin: 0 }}>
              Todavía no hay escenarios guardados. Configura uno en <b>Escenario</b> y guárdalo aquí
              para reutilizarlo después.
            </p>
          ) : (
            <table className="data">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th className="num">R₀</th>
                  <th className="num">Camas</th>
                  <th className="num">Horizonte</th>
                  <th className="num">Modo</th>
                  <th className="num">Creado</th>
                  <th className="num">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {escenarios.map((e) => (
                  <tr key={e.id}>
                    <td>
                      {e.name}
                      {e.description && <div className="help">{e.description}</div>}
                      {e.request.interventions?.length > 0 && (
                        <div className="help">{e.request.interventions.length} intervención(es)</div>
                      )}
                    </td>
                    <td className="num">{dec2(computeR0(e.request.model))}</td>
                    <td className="num">{int(e.request.hospital.initialBeds)}</td>
                    <td className="num">{int(e.request.horizonDays)} d</td>
                    <td className="num">{e.request.stochastic ? 'estoc.' : 'determ.'}</td>
                    <td className="num">{fechaHora(e.createdAt)}</td>
                    <td className="num">
                      <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => cargar(e)}>cargar</button>
                        <button className="btn btn-ghost btn-sm" onClick={() => ejecutar(e)} disabled={ocupadoId === e.id}>
                          {ocupadoId === e.id ? '…' : 'ejecutar'}
                        </button>
                        <button className="btn btn-danger-ghost btn-sm" onClick={() => borrar(e)} disabled={ocupadoId === e.id}>
                          borrar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="card">
        <CardHead title="Historial de corridas" sub={`${corridas.length} ejecuciones capturadas · más recientes primero`} />
        <div className="card-pad">
          {corridas.length === 0 ? (
            <p className="help" style={{ margin: 0 }}>
              Aún no hay corridas registradas. Cada simulación que ejecutes queda aquí con sus
              indicadores y su semilla.
            </p>
          ) : (
            <table className="data">
              <thead>
                <tr>
                  <th className="num">Fecha</th>
                  <th>Escenario</th>
                  <th className="num">Modo</th>
                  <th className="num">Semilla</th>
                  <th className="num">R₀</th>
                  <th className="num">Saturación</th>
                  <th className="num">Presión pico</th>
                  <th className="num">Muertes</th>
                </tr>
              </thead>
              <tbody>
                {corridas.slice(0, 50).map((c) => (
                  <tr key={c.id}>
                    <td className="num">{fechaHora(c.createdAt)}</td>
                    <td>{c.scenarioId ? (nombrePorId[c.scenarioId] ?? '(borrado)') : <span className="help">ad-hoc</span>}</td>
                    <td className="num">{c.stochastic ? 'estoc.' : 'determ.'}</td>
                    <td className="num">{c.seed ?? '—'}</td>
                    <td className="num">{dec2(c.kpis.r0)}</td>
                    <td className="num" style={{ color: c.kpis.saturated ? 'var(--red-ink)' : undefined }}>
                      {c.kpis.saturated ? `día ${c.kpis.saturationDay}` : 'no satura'}
                    </td>
                    <td className="num">{dec2(c.kpis.peakPressureRatio)}×</td>
                    <td className="num">{int(c.kpis.deathsFromSaturation)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
