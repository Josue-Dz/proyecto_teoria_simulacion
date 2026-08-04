import katex from 'katex'
import 'katex/dist/katex.min.css'

function render(tex, displayMode) {
  return katex.renderToString(tex, {
    displayMode,
    throwOnError: false,
    strict: false,
  })
}

export function Eq({ children, note }) {
  return (
    <div className="eq">
      <div
        className="eq-math"
        dangerouslySetInnerHTML={{ __html: render(children, true) }}
      />
      {note && <div className="eq-note">{note}</div>}
    </div>
  )
}

export function Tex({ children }) {
  return <span dangerouslySetInnerHTML={{ __html: render(children, false) }} />
}
