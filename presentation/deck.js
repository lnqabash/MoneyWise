/* MoneyMate pitch deck — navigation, progress, speaker notes, a11y. */
(function () {
  "use strict";

  const slides = Array.from(document.querySelectorAll(".slide"));
  const total = slides.length;
  let current = 0;

  const progress = document.getElementById("progress");
  const count = document.getElementById("count");
  const slideNum = document.getElementById("slideNum");
  const notes = document.getElementById("notes");
  const prevBtn = document.getElementById("prev");
  const nextBtn = document.getElementById("next");
  const toggleNotesBtn = document.getElementById("toggleNotes");

  let notesOpen = false;

  function render() {
    slides.forEach((s, i) => s.classList.toggle("active", i === current));
    const human = `${current + 1} / ${total}`;
    count.textContent = human;
    slideNum.textContent = human;
    progress.style.width = ((current + 1) / total) * 100 + "%";
    prevBtn.disabled = current === 0;
    nextBtn.disabled = current === total - 1;
    if (notesOpen) renderNotes();
    // Move focus to the active slide for screen-reader users.
    slides[current].setAttribute("tabindex", "-1");
    slides[current].focus({ preventScroll: true });
  }

  function renderNotes() {
    const tpl = slides[current].querySelector(".notes-data");
    notes.innerHTML = tpl ? tpl.innerHTML : "<p>No notes for this slide.</p>";
  }

  function go(n) {
    current = Math.max(0, Math.min(total - 1, n));
    render();
  }
  const next = () => go(current + 1);
  const prev = () => go(current - 1);

  function setNotes(open) {
    notesOpen = open;
    notes.classList.toggle("open", open);
    toggleNotesBtn.setAttribute("aria-pressed", String(open));
    if (open) renderNotes();
  }

  nextBtn.addEventListener("click", next);
  prevBtn.addEventListener("click", prev);
  toggleNotesBtn.addEventListener("click", () => setNotes(!notesOpen));

  document.addEventListener("keydown", (e) => {
    switch (e.key) {
      case "ArrowRight":
      case "PageDown":
      case " ":
        e.preventDefault(); next(); break;
      case "ArrowLeft":
      case "PageUp":
        e.preventDefault(); prev(); break;
      case "Home": e.preventDefault(); go(0); break;
      case "End": e.preventDefault(); go(total - 1); break;
      case "n": case "N": setNotes(!notesOpen); break;
      case "f": case "F":
        if (document.fullscreenElement) document.exitFullscreen();
        else document.documentElement.requestFullscreen().catch(() => {});
        break;
      default:
        // Number keys 1-9 jump to that slide.
        if (/^[1-9]$/.test(e.key)) go(parseInt(e.key, 10) - 1);
    }
  });

  // Touch / swipe support.
  let touchX = null;
  document.addEventListener("touchstart", (e) => { touchX = e.touches[0].clientX; }, { passive: true });
  document.addEventListener("touchend", (e) => {
    if (touchX === null) return;
    const dx = e.changedTouches[0].clientX - touchX;
    if (Math.abs(dx) > 50) (dx < 0 ? next : prev)();
    touchX = null;
  }, { passive: true });

  // Deep-link + restore via hash (#7 opens slide 7).
  function fromHash() {
    const n = parseInt(location.hash.replace("#", ""), 10);
    if (!isNaN(n)) current = Math.max(0, Math.min(total - 1, n - 1));
  }
  window.addEventListener("hashchange", () => { fromHash(); render(); });
  fromHash();

  render();
})();
