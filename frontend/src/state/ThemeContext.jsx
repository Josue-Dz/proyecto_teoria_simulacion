import { useCallback, useEffect, useMemo, useState } from 'react'
import { PALETAS } from '../lib/colors.js'
import { ThemeContext } from './themeContextObject.js'

const CLAVE = 'denguesim.tema'


function temaInicial() {
  try {
    const guardado = localStorage.getItem(CLAVE)
    if (guardado === 'light' || guardado === 'dark') return guardado
  } catch {
    /* localStorage bloqueado */
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }) {
  const [tema, setTema] = useState(temaInicial)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    document.documentElement.style.colorScheme = tema
    try {
      localStorage.setItem(CLAVE, tema)
    } catch {
      /* sin persistencia: el tema dura lo que la sesión */
    }
  }, [tema])

  
  useEffect(() => {
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)')
    if (!mq) return undefined
    const alCambiar = (e) => {
      let eligio = false
      try {
        eligio = localStorage.getItem(CLAVE) != null
      } catch { /* sin acceso: se trata como "no eligió" */ }
      if (!eligio) setTema(e.matches ? 'dark' : 'light')
    }
    mq.addEventListener('change', alCambiar)
    return () => mq.removeEventListener('change', alCambiar)
  }, [])

  const alternar = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), [])

  const valor = useMemo(
    () => ({ tema, alternar, colores: PALETAS[tema] }),
    [tema, alternar],
  )

  return <ThemeContext.Provider value={valor}>{children}</ThemeContext.Provider>
}
