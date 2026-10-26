import "./Learn.css";

const LESSONS = [
  { title: "Budgeting on uneven income", meta: "2 min · audio · offline ✓", locked: false },
  { title: "Spotting a scam", meta: "2 min · audio", locked: false },
  { title: "Building an emergency fund", meta: "Unlocks next", locked: true },
];

export default function Learn() {
  return (
    <>
      <h3 className="section-title">📚 Financial Coach</h3>
      {LESSONS.map((l) => (
        <div className={`lesson ${l.locked ? "locked" : ""}`} key={l.title}>
          <span className="play">{l.locked ? "🔒" : "▶"}</span>
          <div>
            <strong>{l.title}</strong>
            <br />
            <span className="hint">{l.meta}</span>
          </div>
        </div>
      ))}
      <p className="demo-note" style={{ marginTop: 12 }}>
        Learning content is a static preview in this demo.
      </p>
    </>
  );
}
