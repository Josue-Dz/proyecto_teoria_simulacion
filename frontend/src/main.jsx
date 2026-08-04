import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { SimulationProvider } from './state/SimulationContext.jsx'
import { ThemeProvider } from './state/ThemeContext.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <SimulationProvider>
        <App />
      </SimulationProvider>
    </ThemeProvider>
  </StrictMode>,
)
