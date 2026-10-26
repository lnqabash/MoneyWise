/* MoneyMate web demo — talks to the backend on the same origin (no CORS). */
(function () {
  "use strict";

  const API = "/v1";
  let token = null;
  let challengeId = null;
  let currentGoalId = null;

  // ---- tiny helpers ----
  const $ = (id) => document.getElementById(id);
  const show = (el) => el.classList.remove("hidden");
  const hide = (el) => el.classList.add("hidden");

  function fmtMoney(minor, currency) {
    const sym = { ZAR: "R", KES: "KSh ", USD: "$" }[currency] || currency + " ";
    return sym + (minor / 100).toFixed(2);
  }

  async function api(path, { method = "GET", body, auth = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    if (auth && token) headers["Authorization"] = "Bearer " + token;
    const res = await fetch(API + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
      const msg = data && data.error ? data.error.message : "Request failed";
      throw new Error(msg);
    }
    return data;
  }

  // ============ AUTH ============
  $("btn-request-otp").addEventListener("click", async () => {
    $("auth-error").textContent = "";
    const phone = $("phone").value.trim();
    try {
      const r = await api("/auth/otp/request", {
        method: "POST",
        auth: false,
        body: { phone, channel: "sms" },
      });
      challengeId = r.challengeId;
      show($("otp-step"));
      if (r.devCode) {
        $("dev-code-hint").textContent = "Demo mode: your code is " + r.devCode;
        $("otp-code").value = r.devCode; // prefill for a smooth demo
      }
    } catch (e) {
      $("auth-error").textContent = e.message;
    }
  });

  $("btn-verify-otp").addEventListener("click", async () => {
    $("auth-error").textContent = "";
    try {
      const r = await api("/auth/otp/verify", {
        method: "POST",
        auth: false,
        body: { challengeId, code: $("otp-code").value.trim() },
      });
      token = r.accessToken;
      enterApp();
    } catch (e) {
      $("auth-error").textContent = e.message;
    }
  });

  $("btn-logout").addEventListener("click", () => {
    token = null;
    challengeId = null;
    $("view-app").classList.remove("active");
    $("view-auth").classList.add("active");
  });

  async function enterApp() {
    $("view-auth").classList.remove("active");
    $("view-app").classList.add("active");
    $("today-date").textContent = new Date().toLocaleDateString(undefined, {
      weekday: "long", day: "numeric", month: "short",
    });
    await loadProfile();
    await loadBudget();
    await loadHealthScore();
    await loadForecast();
    await loadCredit();
    await loadGoals();
    await loadCircles();
  }

  // ============ FINANCIAL HEALTH SCORE ============
  let lastHealth = null;

  async function loadHealthScore() {
    try {
      const h = await api("/health-score");
      lastHealth = h;
      $("health-num").textContent = h.overallScore;
      $("health-band").textContent = h.band;
      $("health-summary").textContent = h.summary;

      // Colour + fill the ring by score.
      const colour = h.overallScore >= 80 ? "var(--mm-safe)"
        : h.overallScore >= 60 ? "var(--mm-primary)"
        : h.overallScore >= 40 ? "var(--mm-warn)" : "var(--mm-danger)";
      const deg = Math.round((h.overallScore / 100) * 360);
      $("health-ring").style.background =
        `conic-gradient(${colour} ${deg}deg, var(--mm-line) ${deg}deg)`;

      const pillars = $("health-pillars");
      pillars.innerHTML = "";
      h.pillars.forEach((p) => {
        const c = p.score >= 70 ? "var(--mm-safe)" : p.score >= 40 ? "var(--mm-warn)" : "var(--mm-danger)";
        const div = document.createElement("div");
        div.className = "pillar";
        div.innerHTML =
          `<div class="pillar-top"><span>${p.name}</span><span class="pillar-score" style="color:${c}">${p.score}</span></div>` +
          `<div class="pillar-bar"><div class="pillar-fill" style="width:${p.score}%;background:${c}"></div></div>` +
          `<div class="pillar-reason">${p.reason}</div>`;
        pillars.appendChild(div);
      });
    } catch (e) { /* non-fatal */ }
  }

  $("btn-health-detail").addEventListener("click", () => {
    $("health-pillars").classList.toggle("hidden");
  });

  async function loadProfile() {
    try {
      const p = await api("/profile");
      const name = p.displayName || "there";
      const hi = p.preferredLanguage === "zu" ? "Sawubona" : "Hello";
      $("greeting").textContent = `${hi}, ${name} 👋`;
      $("btn-logout").textContent = name.charAt(0).toUpperCase();
    } catch (e) { /* non-fatal in demo */ }
  }

  // ============ DASHBOARD ============
  let lastExplanation = null;

  async function loadBudget() {
    const b = await api("/budget/current");
    $("sts-amount").textContent = fmtMoney(b.safeToSpendTodayMinor, b.currency);
    $("sts-conf").textContent = "confidence " + Math.round(b.confidence * 100) + "%";

    lastExplanation = b.explanation;
    const reasons = $("why-reasons");
    reasons.innerHTML = "";
    (b.explanation.reasons || []).forEach((r) => {
      const li = document.createElement("li");
      li.textContent = r;
      reasons.appendChild(li);
    });
    $("why-model").textContent = "Model: " + b.explanation.modelVersion;

    const env = $("envelopes");
    env.innerHTML = "";
    if (!b.envelopes.length) {
      env.innerHTML = '<p class="hint">No spending recorded yet.</p>';
    }
    b.envelopes.forEach((e) => {
      const pct = e.allocatedMinor ? Math.min(100, (e.spentMinor / e.allocatedMinor) * 100) : 0;
      const div = document.createElement("div");
      div.className = "env";
      div.innerHTML =
        `<div class="env-top"><span>${capitalize(e.name)}</span>` +
        `<span>${fmtMoney(e.spentMinor, b.currency)} / ${fmtMoney(e.allocatedMinor, b.currency)}</span></div>` +
        `<div class="env-bar"><div class="env-fill" style="width:${pct}%"></div></div>`;
      env.appendChild(div);
    });
  }

  $("btn-why").addEventListener("click", () => {
    const panel = $("why-panel");
    panel.classList.toggle("hidden");
  });

  // ============ SAVINGS ============
  async function loadGoals() {
    const goals = await api("/goals");
    // Dashboard shows the first goal
    const goalCard = $("goal-card");
    if (!goals.length) {
      goalCard.innerHTML = '<p class="hint">No goals yet.</p>';
    } else {
      goalCard.innerHTML = goalHtml(goals[0]);
      currentGoalId = goals[0].goalId;
    }
    // Save tab lists all goals
    const list = $("goals-list");
    list.innerHTML = "";
    goals.forEach((g) => {
      const div = document.createElement("div");
      div.className = "goal-item";
      div.innerHTML = goalHtml(g);
      list.appendChild(div);
    });
  }

  function goalHtml(g) {
    const pct = Math.round((g.progress || 0) * 100);
    return (
      `<div class="goal-head"><strong>🎯 ${g.name}</strong>` +
      `<span class="goal-pct">${pct}%</span></div>` +
      `<div class="goal-bar"><div class="goal-fill" style="width:${pct}%"></div></div>` +
      `<div class="env-top"><span>${fmtMoney(g.savedMinor, g.currency)} of ${fmtMoney(g.targetMinor, g.currency)}</span>` +
      (g.status === "reached" ? `<span class="streak">reached 🎉</span>` : `<span class="streak">${g.goalType}</span>`) +
      `</div>`
    );
  }

  $("contrib-amount").addEventListener("input", (e) => {
    $("contrib-label").textContent = e.target.value || "0";
  });

  $("btn-contribute").addEventListener("click", async () => {
    if (!currentGoalId) return;
    const rands = parseInt($("contrib-amount").value, 10) || 0;
    if (rands <= 0) return;
    const btn = $("btn-contribute");
    btn.disabled = true;
    try {
      const r = await api(`/goals/${currentGoalId}/contributions`, {
        method: "POST",
        body: { amountMinor: rands * 100, source: "good_day" },
      });
      const toast = $("save-toast");
      toast.textContent = `✓ Saved! New balance ${fmtMoney(r.savedMinor, "ZAR")}.`;
      show(toast);
      await loadGoals();
      setTimeout(() => hide(toast), 4000);
    } catch (e) {
      const toast = $("save-toast");
      toast.textContent = e.message;
      show(toast);
    } finally {
      btn.disabled = false;
    }
  });

  // ============ SCAM SHIELD ============
  $("btn-scan").addEventListener("click", async () => {
    const content = $("scam-input").value.trim();
    if (!content) return;
    const btn = $("btn-scan");
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';
    try {
      const r = await api("/scam/scan", {
        method: "POST",
        body: { channel: "whatsapp", content, language: "en" },
      });
      renderScam(r);
    } catch (e) {
      $("scam-result").innerHTML = `<p class="error">${e.message}</p>`;
      show($("scam-result"));
    } finally {
      btn.disabled = false;
      btn.textContent = "Check this message";
    }
  });

  function renderScam(r) {
    const icon = { high: "🚫", medium: "⚠️", low: "✅" }[r.riskLevel];
    const label = { high: "High risk", medium: "Be careful", low: "Looks okay" }[r.riskLevel];
    const flags = (r.redFlags || [])
      .map(
        (f) =>
          `<div class="flag"><div class="flag-code ${r.riskLevel}">${f.code}</div>${f.explanation}</div>`
      )
      .join("");
    const actions = (r.advice.actions || [])
      .map((a) => `<button class="action-btn" data-action="${a}">${labelAction(a)}</button>`)
      .join("");

    $("scam-result").innerHTML =
      `<div class="verdict ${r.riskLevel}">` +
      `<div class="verdict-badge">${icon} ${label} · score ${Math.round(r.riskScore * 100)}%</div>` +
      `<div class="verdict-summary">${r.advice.summary}</div></div>` +
      (flags ? `<div class="flags">${flags}</div>` : "") +
      (actions ? `<div class="actions">${actions}</div>` : "") +
      `<p class="model-tag" style="margin-top:8px">Model: ${r.modelVersion}</p>`;
    show($("scam-result"));

    // Wire the action buttons to record the user's action
    $("scam-result").querySelectorAll(".action-btn").forEach((b) => {
      b.addEventListener("click", async () => {
        try {
          await api(`/scam/${r.reportId}/action`, {
            method: "POST",
            body: { action: b.dataset.action },
          });
          b.classList.add("done");
          b.textContent = "✓ " + b.textContent;
        } catch (e) { /* ignore in demo */ }
      });
    });
  }

  function labelAction(a) {
    return {
      block_sender: "Block sender",
      report_to_telco: "Report",
      warn_contacts: "Warn contacts",
      verify_sender: "Verify sender",
      do_not_share_details: "Don't share details",
      stay_alert: "Stay alert",
    }[a] || a;
  }

  // ============ NAV + misc ============
  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
      document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
      btn.classList.add("active");
      $("tab-" + btn.dataset.tab).classList.add("active");
    });
  });

  // Simulate the offline/online toggle to show the "works offline" story
  $("conn-indicator").addEventListener("click", (e) => {
    const el = e.target;
    const offline = el.classList.toggle("offline");
    el.textContent = offline ? "● offline" : "● online";
  });

  function capitalize(s) {
    return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
  }

  // ============ VOICE-FIRST ASSISTANT ============
  // Uses the browser's built-in Web Speech API (SpeechRecognition +
  // speechSynthesis) so it needs no cloud and works in the demo. In the
  // product this maps to Azure AI Speech + the /voice/intent endpoint
  // (docs/07) with full multilingual support.
  const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
  let recognizer = null;

  $("btn-voice").addEventListener("click", () => show($("voice-panel")));
  $("voice-close").addEventListener("click", () => {
    hide($("voice-panel"));
    stopListening();
  });

  // Example chips run the intent directly (and are great for a reliable demo
  // if the room is noisy or the browser lacks speech support).
  document.querySelectorAll(".voice-eg").forEach((b) => {
    b.addEventListener("click", () => handleUtterance(b.textContent));
  });

  $("voice-mic").addEventListener("click", () => {
    if (!SpeechRec) {
      $("voice-status").textContent =
        "This browser doesn't support voice input. Tap an example below instead.";
      return;
    }
    startListening();
  });

  function startListening() {
    recognizer = new SpeechRec();
    recognizer.lang = "en-ZA";
    recognizer.interimResults = false;
    recognizer.maxAlternatives = 1;
    $("voice-orb").classList.add("listening");
    $("voice-status").textContent = "Listening… ask me about your money";
    $("voice-heard").textContent = "";
    $("voice-reply").textContent = "";
    recognizer.onresult = (ev) => {
      const said = ev.results[0][0].transcript;
      handleUtterance(said);
    };
    recognizer.onerror = () => {
      $("voice-status").textContent = "I didn't catch that. Try again or tap an example.";
      stopListening();
    };
    recognizer.onend = () => stopListening();
    recognizer.start();
  }

  function stopListening() {
    if (recognizer) { try { recognizer.stop(); } catch (e) {} recognizer = null; }
    $("voice-orb").classList.remove("listening");
  }

  // Tiny on-device intent matcher (the product uses NLU). Maps speech to a
  // real API call and speaks the answer back.
  async function handleUtterance(text) {
    stopListening();
    $("voice-heard").textContent = "You said: \u201C" + text + "\u201D";
    const t = text.toLowerCase();
    let reply;

    try {
      if (/(spend|afford|safe to spend|how much)/.test(t)) {
        const b = await api("/budget/current");
        reply = b.explanation.summary + " " + (b.explanation.reasons[0] || "");
      } else if (/(health|score|how am i doing)/.test(t)) {
        const h = lastHealth || (await api("/health-score"));
        reply = `Your financial health score is ${h.overallScore} out of 100 — ${h.band}. ${h.summary}`;
      } else if (/(scam|suspicious|fraud|check)/.test(t)) {
        reply = "Sure — switch to the Shield tab and paste the message, and I'll check it for you.";
        switchTab("scam");
      } else if (/(save|saving|goal|jar)/.test(t)) {
        const goals = await api("/goals");
        if (goals.length) {
          const g = goals[0];
          reply = `You've saved ${fmtMoney(g.savedMinor, g.currency)} of ${fmtMoney(g.targetMinor, g.currency)} towards ${g.name}. You're doing well!`;
        } else {
          reply = "You don't have a savings goal yet. Want to set one up?";
        }
        switchTab("save");
      } else {
        reply = "I can help with your budget, savings, scam checks, and your health score. Try asking how much you can spend today.";
      }
    } catch (e) {
      reply = "Sorry, I couldn't reach your data just now.";
    }

    $("voice-reply").textContent = reply;
    speak(reply);
  }

  function speak(text) {
    if (!("speechSynthesis" in window)) return;
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "en-ZA";
    u.rate = 1.0;
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(u);
  }

  function switchTab(tab) {
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
    document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
    const navBtn = document.querySelector(`.nav-item[data-tab="${tab}"]`);
    if (navBtn) navBtn.classList.add("active");
    const tabEl = $("tab-" + tab);
    if (tabEl) tabEl.classList.add("active");
  }

  // ============ SMART SPENDING PREDICTIONS ============
  async function loadForecast() {
    try {
      const f = await api("/budget/forecast?horizonDays=14");
      $("fc-in").textContent = fmtMoney(f.predictedIncomeMinor, f.currency);
      $("fc-out").textContent = fmtMoney(f.predictedExpenseMinor, f.currency);
      $("fc-summary").textContent = f.explanation.summary;
      const lean = $("fc-lean");
      if (f.leanPeriods && f.leanPeriods.length) {
        lean.textContent = "⚠️ " + f.leanPeriods.length +
          " lean week(s) ahead — spending may outpace income. A small buffer now helps.";
        show(lean);
      } else {
        hide(lean);
      }
    } catch (e) { /* non-fatal */ }
  }

  // ============ CREDIT READINESS ============
  async function loadCredit() {
    try {
      const c = await api("/credit-readiness");
      $("credit-tier").textContent = c.tier;
      $("credit-score").textContent = c.readinessScore + "/100";
      $("credit-fill").style.width = c.readinessScore + "%";
      $("credit-disclaimer").textContent = c.disclaimer;

      const detail = $("credit-detail");
      let html = "";
      if (c.strengths && c.strengths.length) {
        html += "<h5 class='good'>What's working</h5><ul>" +
          c.strengths.map((s) => `<li class='good'>✓ ${s}</li>`).join("") + "</ul>";
      }
      if (c.improvements && c.improvements.length) {
        html += "<h5 class='improve'>What to improve</h5><ul>" +
          c.improvements.map((s) => `<li class='improve'>→ ${s}</li>`).join("") + "</ul>";
      }
      detail.innerHTML = html;
    } catch (e) { /* non-fatal */ }
  }

  const creditBtn = $("btn-credit-detail");
  if (creditBtn) creditBtn.addEventListener("click", () => $("credit-detail").classList.toggle("hidden"));

  // ============ SAVINGS CIRCLES ============
  let activeCircleId = null;

  $("circle-amount").addEventListener("input", (e) => {
    $("circle-amount-label").textContent = e.target.value || "0";
  });

  $("btn-create-circle").addEventListener("click", async () => {
    const name = $("circle-name").value.trim() || "My Circle";
    const rands = parseInt($("circle-amount").value, 10) || 0;
    if (rands <= 0) return;
    const btn = $("btn-create-circle");
    btn.disabled = true;
    try {
      const circle = await api("/circles", {
        method: "POST",
        body: { name, type: "rosca", contributionMinor: rands * 100, currency: "ZAR", cadence: "weekly", memberLimit: 3 },
      });
      // Demo convenience: add the two seeded members and start the circle.
      await addDemoMembersAndStart(circle.circleId);
      showCircleToast("✓ Circle created with 3 members and started!");
      await loadCircles();
      await openCircle(circle.circleId);
    } catch (e) {
      showCircleToast(e.message);
    } finally {
      btn.disabled = false;
    }
  });

  // The two extra demo users' ids aren't known client-side, so we resolve them
  // by signing in as them once to capture their userIds (demo-only shortcut).
  async function addDemoMembersAndStart(circleId) {
    const grace = await demoUserId("+27830000002");
    const maria = await demoUserId("+27830000003");
    if (grace) await api(`/circles/${circleId}/members`, { method: "POST", body: { userId: grace } });
    if (maria) await api(`/circles/${circleId}/members`, { method: "POST", body: { userId: maria } });
    await api(`/circles/${circleId}/start`, { method: "POST", body: {} });
  }

  async function demoUserId(phone) {
    try {
      const otp = await api("/auth/otp/request", { method: "POST", auth: false, body: { phone } });
      const auth = await api("/auth/otp/verify", { method: "POST", auth: false, body: { challengeId: otp.challengeId, code: otp.devCode } });
      return auth.userId;
    } catch (e) {
      return null;
    }
  }

  async function loadCircles() {
    try {
      const circles = await api("/circles");
      const list = $("circles-list");
      list.innerHTML = "";
      if (!circles.length) {
        list.innerHTML = '<p class="hint">No circles yet. Start one below.</p>';
        return;
      }
      circles.forEach((c) => {
        const div = document.createElement("div");
        div.className = "circle-item";
        div.innerHTML =
          `<div class="circle-head"><strong>👥 ${c.name}</strong>` +
          `<span class="circle-badge">${c.status}</span></div>` +
          `<p class="hint">${fmtMoney(c.contributionMinor, c.currency)}/${c.cadence} · ${c.memberCount} members · cycle ${c.currentCycleNo}</p>`;
        div.addEventListener("click", () => openCircle(c.circleId));
        list.appendChild(div);
      });
    } catch (e) { /* non-fatal */ }
  }

  async function openCircle(circleId) {
    activeCircleId = circleId;
    const c = await api("/circles/" + circleId);
    const cur = c.currentCycle;
    const detail = $("circle-detail");

    const members = c.members.map((m) => {
      const paid = m.contributedThisCycle;
      const initial = m.userId.substring(0, 1).toUpperCase();
      return `<div class="member-row"><span class="member-dot">${m.payoutPosition}</span>` +
        `<span>Member ${m.payoutPosition}${m.userId === c.organiserUserId ? " (organiser)" : ""}</span>` +
        `<span class="member-paid ${paid ? "yes" : "no"}">${paid ? "✓ paid" : "waiting"}</span></div>`;
    }).join("");

    let potBlock = "";
    let actions = "";
    if (cur) {
      potBlock = `<div class="circle-pot"><div class="amt">${fmtMoney(cur.potMinor, c.currency)}</div>` +
        `<div class="lbl">in the pot · cycle ${cur.cycleNumber} · ${cur.contributionsIn}/${cur.totalMembers} paid</div></div>`;
      const allPaid = cur.contributionsIn >= cur.totalMembers;
      actions =
        `<div class="circle-actions">` +
        `<button class="btn btn-primary" id="btn-circle-contribute">I'll pay my ${fmtMoney(c.contributionMinor, c.currency)}</button>` +
        `<button class="btn btn-primary" id="btn-circle-payout" ${allPaid ? "" : "disabled"}>Pay out the pot</button>` +
        `</div>` +
        (allPaid ? "" : `<div class="payout-blocked">🔒 Payout is locked until everyone has paid in — no one can take the pot early.</div>`);
    }

    detail.innerHTML =
      `<div class="card"><div class="circle-head"><strong>${c.name}</strong>` +
      `<span class="circle-badge">${c.status}</span></div>` +
      potBlock + members + actions + `</div>`;

    const cBtn = $("btn-circle-contribute");
    if (cBtn) cBtn.addEventListener("click", async () => {
      try {
        // Thandi (the signed-in user) pays her share.
        await api(`/circles/${circleId}/contributions`, { method: "POST", body: { amountMinor: c.contributionMinor } });
        // Demo: the other members pay in too, each from their own session,
        // so you can see the pot fill and the payout unlock.
        await othersContribute(circleId, c.contributionMinor);
        await openCircle(circleId);
        await loadCircles();
      } catch (e) { showCircleToast(e.message); }
    });

    const pBtn = $("btn-circle-payout");
    if (pBtn) pBtn.addEventListener("click", async () => {
      try {
        const r = await api(`/circles/${circleId}/payout`, { method: "POST", body: {} });
        showCircleToast(`🎉 Pot of ${fmtMoney(r.potMinor, c.currency)} paid out! Now on cycle ${r.nextCycleNo}.`);
        await openCircle(circleId);
        await loadCircles();
      } catch (e) { showCircleToast(e.message); }
    });
  }

  // ============ LUMP-SUM SMOOTHING ============
  $("btn-smooth").addEventListener("click", async () => {
    const rands = parseInt($("smooth-amount").value, 10) || 0;
    const weeks = parseInt($("smooth-weeks").value, 10) || 1;
    if (rands <= 0) return;
    const btn = $("btn-smooth");
    btn.disabled = true;
    try {
      const s = await api("/budget/smooth", {
        method: "POST",
        body: { amountMinor: rands * 100, currency: "ZAR", weeks },
      });
      const el = $("smooth-result");
      el.innerHTML =
        `<div class="smooth-salary"><div class="amt">${fmtMoney(s.perWeekMinor, s.currency)}</div>` +
        `<div class="lbl">per week for ${s.weeks} weeks — your personal salary</div></div>` +
        `<div class="smooth-breakdown">` +
        `<div><div class="b-val">${fmtMoney(s.toEmergencyMinor, s.currency)}</div><div class="b-lbl">Emergency buffer</div></div>` +
        `<div><div class="b-val">${fmtMoney(s.toGoalsMinor, s.currency)}</div><div class="b-lbl">To your goals</div></div>` +
        `</div>` +
        `<p class="smooth-reason">${s.explanation.reasons[s.explanation.reasons.length - 1]}</p>`;
      show(el);
    } catch (e) {
      const el = $("smooth-result");
      el.innerHTML = `<p class="error">${e.message}</p>`;
      show(el);
    } finally {
      btn.disabled = false;
    }
  });

  // ============ ADD EXPENSE (auto-categorisation) ============
  $("btn-add-expense").addEventListener("click", async () => {
    const note = $("exp-note").value.trim();
    const rands = parseInt($("exp-amount").value, 10) || 0;
    if (rands <= 0) return;
    const btn = $("btn-add-expense");
    btn.disabled = true;
    try {
      // No category sent -> backend auto-categorises from the note.
      const r = await api("/expenses", {
        method: "POST",
        body: { amountMinor: rands * 100, currency: "ZAR", note, spentOn: new Date().toISOString().slice(0, 10) },
      });
      const toast = $("exp-toast");
      toast.textContent = r.autoCategorised
        ? `✓ Added. We filed this under "${r.category}" for you.`
        : `✓ Added under "${r.category}".`;
      show(toast);
      setTimeout(() => hide(toast), 4000);
      $("exp-note").value = "";
      // Refresh the parts of the dashboard this affects.
      await loadBudget();
      await loadForecast();
      await loadHealthScore();
    } catch (e) {
      const toast = $("exp-toast");
      toast.textContent = e.message;
      show(toast);
    } finally {
      btn.disabled = false;
    }
  });

  // Demo helper: Grace & Maria contribute from their own tokens so a full
  // round can be shown. Real app: each member pays from their own device.
  async function othersContribute(circleId, amountMinor) {
    for (const phone of ["+27830000002", "+27830000003"]) {
      try {
        const otp = await api("/auth/otp/request", { method: "POST", auth: false, body: { phone } });
        const auth = await api("/auth/otp/verify", { method: "POST", auth: false, body: { challengeId: otp.challengeId, code: otp.devCode } });
        await fetch(API + `/circles/${circleId}/contributions`, {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": "Bearer " + auth.accessToken },
          body: JSON.stringify({ amountMinor }),
        });
      } catch (e) { /* member may have already paid; ignore in demo */ }
    }
  }

  function showCircleToast(msg) {
    const t = $("circle-toast");
    t.textContent = msg;
    show(t);
    setTimeout(() => hide(t), 5000);
  }
})();
