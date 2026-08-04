export function Banner({ kind = 'info', children }) {
  return <div className={`banner ${kind}`}>{children}</div>
}

export function Empty({ title, children, action }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      <div className="prose" style={{ margin: '0 auto' }}>{children}</div>
      {action && <div style={{ marginTop: 18 }}>{action}</div>}
    </div>
  )
}

export function CardHead({ title, sub, right }) {
  return (
    <div className="card-head">
      <div>
        <h3>{title}</h3>
        {sub && <div className="sub" style={{ marginTop: 2 }}>{sub}</div>}
      </div>
      {right}
    </div>
  )
}
