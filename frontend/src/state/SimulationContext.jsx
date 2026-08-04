import { useEffect, useRef, useState } from 'react'
import { getModelDefaultsRequest } from "../api/services/modelService";
import { runSimulationRequest } from "../api/services/simulationService";
import { SimulationContext } from './useSimulation.js'



const FALLBACK = {
  model: {
    populationHuman: 1342329, vectorRatio: 1.5, bitingRate: 0.65,
    betaHuman: 0.4, betaVector: 0.4, incubationHumanDays: 5,
    incubationVectorDays: 10, infectiousHumanDays: 6,
    lifeExpectancyDaysHuman: 25550, mosquitoLifespanDays: 14, initialInfectedHumans: 10,
    initialImmuneFraction: 0.55,
  },
  hospital: {
    initialBeds: 150, hospitalizedFraction: 0.005, admissionDelayDays: 4,
    avgStayDays: 6, deteriorationThresholdDays: 2, graveMortalityPerDay: 0.013,
    maxWaitDays: 14, severeFraction: 0.27,
  },
  interventions: [],
  horizonDays: 365,
  seed: null,
  stochastic: false,
}

export function SimulationProvider({ children }) {
  const [model, setModel] = useState(FALLBACK.model)
  const [hospital, setHospital] = useState(FALLBACK.hospital)
  const [interventions, setInterventions] = useState([])
  const [horizonDays, setHorizonDays] = useState(365)
  const [stochastic, setStochastic] = useState(false)
  const [seed, setSeed] = useState('')

  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [connWarning, setConnWarning] = useState(false)

  const [day, setDay] = useState(0)
  const [playing, setPlaying] = useState(false)
  const rafRef = useRef(null)

  
  useEffect(() => {
    let alive = true
    getModelDefaultsRequest()
      .then((d) => {
        if (!alive || !d) return
        if (d.model) setModel(d.model)
        if (d.hospital) setHospital(d.hospital)
        if (d.horizonDays) setHorizonDays(d.horizonDays)
        setConnWarning(false)
      })
      .catch(() => alive && setConnWarning(true))
    return () => {
      alive = false
    }
  }, [])

  
  useEffect(() => {
    if (!playing || !result) return
    let last = performance.now()
    const maxDay = result.series.length - 1
    const step = (now) => {
      if (now - last > 45) {
        last = now
        setDay((d) => {
          if (d >= maxDay) {
            setPlaying(false)
            return d
          }
          return d + 1
        })
      }
      rafRef.current = requestAnimationFrame(step)
    }
    rafRef.current = requestAnimationFrame(step)
    return () => cancelAnimationFrame(rafRef.current)
  }, [playing, result])

  const buildRequest = () => ({
    model,
    hospital,
    interventions,
    horizonDays,
    stochastic,
    seed: stochastic && seed !== '' ? Number(seed) : null,
  })

  
  
  const applyResult = (res) => {
    setResult(res)
    setPlaying(false)
    setConnWarning(false)
    const focus = res.kpis?.saturationDay ?? res.kpis?.peakPressureDay ?? 0
    setDay(Math.min(Math.max(0, focus), res.series.length - 1))
  }

  const run = async () => {
    setLoading(true)
    setError(null)
    setPlaying(false)
    try {
      const res = await runSimulationRequest(buildRequest())
      applyResult(res)
      return true
    } catch (e) {
      setError(e.message)
      return false
    } finally {
      setLoading(false)
    }
  }

  
  const loadRequest = (req) => {
    if (!req) return
    if (req.model) setModel(req.model)
    if (req.hospital) setHospital(req.hospital)
    setInterventions(req.interventions ?? [])
    if (req.horizonDays) setHorizonDays(req.horizonDays)
    setStochastic(Boolean(req.stochastic))
    setSeed(req.seed != null ? String(req.seed) : '')
  }

  const setModelField = (key, value) => setModel((m) => ({ ...m, [key]: value }))
  const setHospitalField = (key, value) => setHospital((h) => ({ ...h, [key]: value }))

  const value = {
    model, hospital, interventions, horizonDays, stochastic, seed,
    setModel, setHospital, setModelField, setHospitalField,
    setInterventions, setHorizonDays, setStochastic, setSeed,
    result, loading, error, connWarning,
    day, setDay, playing, setPlaying,
    buildRequest, run, loadRequest, applyResult,
    current: result ? result.series[Math.min(day, result.series.length - 1)] : null,
  }

  return <SimulationContext.Provider value={value}>{children}</SimulationContext.Provider>
}