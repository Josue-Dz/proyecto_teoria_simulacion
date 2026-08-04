import { createContext, useContext } from 'react'

export const SimulationContext = createContext(null)

export function useSimulation() {
  const ctx = useContext(SimulationContext)
  if (!ctx) throw new Error('useSimulation debe usarse dentro de SimulationProvider')
  return ctx
}
