import { useContext } from 'react'
import { ThemeContext } from './themeContextObject.js'
import { PALETAS } from '../lib/colors.js'


export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme debe usarse dentro de <ThemeProvider>')
  return ctx
}


export function useChartColors() {
  const ctx = useContext(ThemeContext)
  return ctx ? ctx.colores : PALETAS.light
}
