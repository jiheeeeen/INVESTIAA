// =========================
// Fetch polyfill (JavaFX WebView safe)
// =========================
(function ensureFetch(){
    if (window.fetch) return;

    function makeResponse(xhr) {
        return {
            ok: xhr.status >= 200 && xhr.status < 300,
            status: xhr.status,
            text: () => Promise.resolve(xhr.responseText),
            json: () => Promise.resolve().then(() => JSON.parse(xhr.responseText))
        };
    }

    window.fetch = function(url, opts){
        opts = opts || {};
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open(opts.method || "GET", url, true);

            const headers = opts.headers || {};
            Object.keys(headers).forEach(k => xhr.setRequestHeader(k, headers[k]));

            xhr.onload = () => resolve(makeResponse(xhr));
            xhr.onerror = () => reject(new Error("Network error"));
            xhr.ontimeout = () => reject(new Error("Timeout"));
            xhr.timeout = opts.timeout || 15000;

            xhr.send(opts.body || null);
        });
    };
})();

// =========================
// Utils (inline edit safe)
// =========================
function v(el, fallback = "") {
    if (!el) return (fallback ?? "");
    const val = String(el.value ?? "").trim();
    return val === "" ? (fallback ?? "") : val;
}

function safeEmail(newEmail, oldEmail) {
    const e = String(newEmail ?? "").trim();
    if (!e) return String(oldEmail ?? "");
    return e.toLowerCase();
}

function escapeHtml(s) {
    return String(s ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function safeParse(json, fallback) {
    try { return JSON.parse(json); } catch { return fallback; }
}

function isValidEmail(email) {
    const e = String(email ?? "").trim();
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e);
}

// =========================
// Data (DB via Java Bridge)
// =========================
let pendingAccounts = [];
let allUsers = [];
let pendingProjects = [];
let allProjects = [];
let pendingEvents = [];
let allEvents = [];
let historyLog = [];

// Analyse Projet (BI)
let currentAnalysisProjectId = null;
let analysisCharts = [];

// =========================
// DOM
// =========================
const contentArea = document.getElementById("contentArea");
const pageTitle   = document.getElementById("pageTitle");
const statsRow    = document.getElementById("statsRow");
const addUserBtn  = document.getElementById("addUserBtn");

const statPending  = document.getElementById("statPending");
const statUsers    = document.getElementById("statUsers");
const statProjects = document.getElementById("statProjects");
const statEvents   = document.getElementById("statEvents");

// =========================
// Bridge call (supports args)
// =========================
function callAdmin(fnName, fallback = "[]", ...args) {
    try {
        if (!window.admin) return fallback;
        const fn = window.admin[fnName];
        if (typeof fn !== "function") return fallback;
        const res = fn.apply(window.admin, args);
        return String(res ?? fallback);
    } catch (e) {
        console.warn("Admin call failed:", fnName, e);
        return fallback;
    }
}

// =========================
// Load data from Java
// =========================
function loadFromJava() {
    pendingAccounts = safeParse(callAdmin("getPendingAccountsJson", "[]"), []);
    allUsers        = safeParse(callAdmin("getAllUsersJson", "[]"), []);

    pendingProjects = safeParse(callAdmin("getPendingProjectsJson", "[]"), []);
    allProjects     = safeParse(callAdmin("getAllProjectsJson", "[]"), []);

    pendingEvents   = safeParse(callAdmin("getPendingEventsJson", "[]"), []);
    allEvents       = safeParse(callAdmin("getAllEventsJson", "[]"), []);

    historyLog      = safeParse(callAdmin("getHistoryJson", "[]"), []);
}

function recomputeStats() {
    if (statPending)  statPending.textContent  = pendingAccounts.length;
    if (statUsers)    statUsers.textContent    = allUsers.length;
    if (statProjects) statProjects.textContent = pendingProjects.length;
    if (statEvents)   statEvents.textContent   = pendingEvents.length;
}

function setActiveNav(page) {
    document.querySelectorAll(".nav button").forEach((b) => {
        b.classList.toggle("active", b.dataset.page === page);
    });
}

function mountPanel({ title, withSearch = true, placeholder = "Search...", tableHtml = "" }) {
    const panel = document.createElement("section");
    panel.className = "panel enterAnim";
    panel.innerHTML = `
    <div class="panelHeader">
      <h2 class="panelTitle">${title}</h2>
      ${withSearch ? `<div class="search"><input id="searchInput" type="text" placeholder="${placeholder}"></div>` : `<div></div>`}
    </div>
    <div class="tableWrap">${tableHtml}</div>
  `;
    return panel;
}

function filterRows(inputId, rowSelector, getText) {
    const inp = document.getElementById(inputId);
    if (!inp) return;
    inp.addEventListener("input", () => {
        const q = (inp.value || "").trim().toLowerCase();
        document.querySelectorAll(rowSelector).forEach((row) => {
            const t = getText(row).toLowerCase();
            row.style.display = t.includes(q) ? "" : "none";
        });
    });
}

// =========================
// Analyse Projet (BI) helpers
// =========================
function openProjectAnalysis(projectId) {
    currentAnalysisProjectId = projectId;
    go("projectAnalysis");
}

function destroyAnalysisCharts() {
    try {
        analysisCharts.forEach(c => c && typeof c.destroy === "function" && c.destroy());
    } catch {}
    analysisCharts = [];
}

// =========================
// INLINE EDIT (Users)
// =========================
function enterEditModeUserRow(tr, u) {
    if (tr.dataset.editing === "1") return;
    tr.dataset.editing = "1";
    tr.dataset.orig = JSON.stringify(u);

    const tds = tr.querySelectorAll("td");

    tds[1].innerHTML = `<input class="cellInput" data-field="nom" type="text" value="${escapeHtml(u.nom)}">`;
    tds[2].innerHTML = `<input class="cellInput" data-field="prenom" type="text" value="${escapeHtml(u.prenom)}">`;

    const emailVal = String(u.email ?? "");
    tds[3].innerHTML = `<input class="cellInput" data-field="email" type="email" value="${escapeHtml(emailVal)}">`;

    tds[4].textContent = "••••••";

    tds[5].innerHTML = `<input class="cellInput" data-field="telephone" type="text" value="${escapeHtml(u.telephone)}">`;
    tds[6].innerHTML = `<input class="cellInput" data-field="cin" type="text" value="${escapeHtml(u.cin)}">`;

    tds[7].innerHTML = `
    <select class="cellInput" data-field="role">
      ${["ADMIN","ENTREPRENEUR","INVESTISSEUR"].map(r =>
        `<option value="${r}" ${String(u.role) === r ? "selected" : ""}>${r}</option>`
    ).join("")}
    </select>
  `;

    tds[8].innerHTML = `
    <select class="cellInput" data-field="statut">
      ${["NON_VERIFIE","EN_ATTENTE","VERIFIE","REFUSE"].map(s =>
        `<option value="${s}" ${String(u.statutVerification) === s ? "selected" : ""}>${s}</option>`
    ).join("")}
    </select>
  `;

    tds[9].innerHTML = `
    <div class="actions">
      <button class="btnIcon btnAccept" data-action="save">✓</button>
      <button class="btnIcon btnReject" data-action="cancel">✕</button>
    </div>
  `;
}

function exitEditModeUserRow(tr, uRestored) {
    tr.dataset.editing = "0";
    const tds = tr.querySelectorAll("td");

    tds[1].textContent = uRestored.nom ?? "";
    tds[2].textContent = uRestored.prenom ?? "";
    tds[3].textContent = uRestored.email ?? "";
    tds[4].textContent = "••••••";

    tds[5].textContent = uRestored.telephone ?? "";
    tds[6].textContent = uRestored.cin ?? "";
    tds[7].textContent = uRestored.role ?? "";
    tds[8].textContent = uRestored.statutVerification ?? "";

    tds[9].innerHTML = `
    <div class="actions">
      <button class="btnIcon" data-action="edit">✎</button>
      <button class="btnIcon btnReject" data-action="delete">🗑</button>
    </div>
  `;
}

// =========================
// INLINE CREATE (Users)
// =========================
let creatingUserRow = false;

function buildNewUserRowHtml() {
    return `
    <tr class="rowUser" data-new="1" data-editing="1">
      <td>—</td>
      <td><input class="cellInput" data-field="nom" type="text" placeholder="Nom"></td>
      <td><input class="cellInput" data-field="prenom" type="text" placeholder="Prénom"></td>
      <td><input class="cellInput" data-field="email" type="email" placeholder="Email"></td>
      <td><input class="cellInput" data-field="mot_de_passe" type="password" placeholder="Mot de passe"></td>
      <td><input class="cellInput" data-field="telephone" type="text" placeholder="Téléphone"></td>
      <td><input class="cellInput" data-field="cin" type="text" placeholder="CIN"></td>
      <td>
        <select class="cellInput" data-field="role">
          ${["INVESTISSEUR","ENTREPRENEUR","ADMIN"].map(r => `<option value="${r}">${r}</option>`).join("")}
        </select>
      </td>
      <td>
        <select class="cellInput" data-field="statut">
          ${["NON_VERIFIE","EN_ATTENTE","VERIFIE","REFUSE"].map(s => `<option value="${s}">${s}</option>`).join("")}
        </select>
      </td>
      <td>
        <div class="actions">
          <button class="btnIcon btnAccept" data-action="create-save">✓</button>
          <button class="btnIcon btnReject" data-action="create-cancel">✕</button>
        </div>
      </td>
    </tr>
  `;
}

function readNewUserRow(tr) {
    const nom = v(tr.querySelector("input[data-field='nom']"), "");
    const prenom = v(tr.querySelector("input[data-field='prenom']"), "");
    const email = safeEmail(v(tr.querySelector("input[data-field='email']"), ""), "");
    const mot_de_passe = v(tr.querySelector("input[data-field='mot_de_passe']"), "");
    const telephone = v(tr.querySelector("input[data-field='telephone']"), "");
    const cin = v(tr.querySelector("input[data-field='cin']"), "");
    const role = v(tr.querySelector("select[data-field='role']"), "INVESTISSEUR");
    const statut_verification = v(tr.querySelector("select[data-field='statut']"), "NON_VERIFIE");
    return { nom, prenom, email, mot_de_passe, telephone, cin, role, statut_verification };
}

function validateNewUser(p) {
    if (!p.nom) return "Nom obligatoire.";
    if (!p.email) return "Email obligatoire.";
    if (!isValidEmail(p.email)) return "Email invalide.";
    if (!p.mot_de_passe) return "Mot de passe obligatoire.";
    if (p.mot_de_passe.length < 6) return "Mot de passe: minimum 6 caractères.";
    if (!p.role) return "Rôle obligatoire.";
    if (!p.statut_verification) return "Statut obligatoire.";
    return "";
}

// =========================================================
// HOME — Weather + Calendar (Open-Meteo) ✅ sans API key
// =========================================================

// Date helpers
function parseDateAny(s) {
    if (!s) return null;
    const iso = String(s).trim().replace(" ", "T");
    const d = new Date(iso);
    return isNaN(d) ? null : d;
}

function ymd(d) {
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function fmtFR(d) {
    return d.toLocaleString("fr-FR", {
        weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit"
    }).replace(",", "");
}

function parseYmd(s) {
    const [y, m, d] = String(s).split("-").map(Number);
    if (!y || !m || !d) return null;
    const dt = new Date(y, m - 1, d);
    return isNaN(dt) ? null : dt;
}

function isBeforeToday(dateStr) {
    const d = parseYmd(dateStr);
    if (!d) return false;
    const t = new Date();
    t.setHours(0,0,0,0);
    d.setHours(0,0,0,0);
    return d.getTime() < t.getTime();
}

function isAfter(dateA, dateB) {
    return String(dateA) > String(dateB);
}

function weatherLabel(code) {
    const m = {
        0: "Ciel dégagé",
        1: "Principalement dégagé",
        2: "Partiellement nuageux",
        3: "Couvert",
        45: "Brouillard",
        48: "Brouillard givrant",
        51: "Bruine légère",
        53: "Bruine",
        55: "Bruine forte",
        61: "Pluie faible",
        63: "Pluie",
        65: "Pluie forte",
        71: "Neige faible",
        73: "Neige",
        75: "Neige forte",
        80: "Averses faibles",
        81: "Averses",
        82: "Averses fortes",
        95: "Orage",
        96: "Orage + grêle",
        99: "Orage violent + grêle"
    };
    return m[code] || `Météo (code ${code})`;
}

function weatherEmoji(code) {
    if (code === 0) return "☀️";
    if (code === 1) return "🌤️";
    if (code === 2) return "⛅";
    if (code === 3) return "☁️";
    if (code === 45 || code === 48) return "🌫️";
    if ([51,53,55].includes(code)) return "🌦️";
    if ([61,63,65,80,81,82].includes(code)) return "🌧️";
    if ([71,73,75].includes(code)) return "❄️";
    if ([95,96,99].includes(code)) return "⛈️";
    return "🌡️";
}

// Weather State (ONE source of truth)
const WX = {
    city: "Tunis",
    place: null,            // {name,country,lat,lon}
    lastUpdated: null,      // Date
    forecastLastDate: null, // YYYY-MM-DD (limit forecast)
    selectedDate: ymd(new Date()),
    dailyMap: new Map(),    // date => {code,tmin,tmax,prec,wind}
    current: null           // {temp, wind, code, time, humid, feels}
};

// Calendar State
let __calYear = new Date().getFullYear();
let __calMonth = new Date().getMonth(); // 0..11
let __calEvents = [];

function initHomeWeatherAndCalendar() {
    // Weather UI events
    const refreshBtn = document.getElementById("weatherRefresh");
    if (refreshBtn) refreshBtn.onclick = () => loadWeather();

    const cityInput = document.getElementById("weatherCity");
    if (cityInput) {
        cityInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") loadWeather();
        });
    }

    // Events + calendar
    loadEventsForHome();

    // Load weather
    loadWeather();
}

// -------------------------
// Events -> Home widgets
// -------------------------
function loadEventsForHome() {
    const evs = Array.isArray(allEvents) ? allEvents : [];
    __calEvents = evs;

    initCalendarWidget(evs);
    renderUpcoming(evs);
}

function buildEventsIndex(events) {
    const map = new Map();
    for (const ev of (events || [])) {
        const d = parseDateAny(ev.dateDebut);
        if (!d) continue;
        if (String(ev.statut || "").toUpperCase() === "REFUSE") continue;

        const key = ymd(d);
        if (!map.has(key)) map.set(key, []);
        map.get(key).push(ev);
    }
    return map;
}

function renderUpcoming(events) {
    const wrap = document.getElementById("upcomingList");
    if (!wrap) return;

    const now = new Date();
    const upcoming = (events || [])
        .map(e => ({ e, d: parseDateAny(e.dateDebut) }))
        .filter(x => x.d && x.d.getTime() >= now.getTime())
        .filter(x => String(x.e.statut || "").toUpperCase() !== "REFUSE")
        .sort((a, b) => a.d - b.d)
        .slice(0, 4);

    if (upcoming.length === 0) {
        wrap.innerHTML = `<div class="warnBox">Aucun événement à venir.</div>`;
        return;
    }

    wrap.innerHTML = upcoming.map(x => `
    <div class="upItem">
      <div><strong>${escapeHtml(x.e.titre || "Événement")}</strong></div>
      <span>${fmtFR(x.d)}</span>
    </div>
  `).join("");
}

// -------------------------
// Calendar widget (click => météo du jour)
// -------------------------
function initCalendarWidget(events) {
    const prev = document.getElementById("calPrev");
    const next = document.getElementById("calNext");
    const grid = document.getElementById("calGrid");

    if (prev) prev.onclick = () => { __calMonth--; normalizeCal(); renderCalendar(events); };
    if (next) next.onclick = () => { __calMonth++; normalizeCal(); renderCalendar(events); };

    if (grid) {
        grid.onclick = (e) => {
            const cell = e.target.closest(".calCell[data-date]");
            if (!cell) return;
            if (cell.classList.contains("muted")) return;

            const dateStr = cell.dataset.date;
            if (!dateStr) return;

            WX.selectedDate = dateStr;
            renderCalendar(events);

            // ✅ météo principale change avec le jour sélectionné
            loadWeatherForSelectedDay(dateStr);
        };
    }

    renderCalendar(events);
}

function normalizeCal() {
    while (__calMonth < 0) { __calMonth += 12; __calYear--; }
    while (__calMonth > 11) { __calMonth -= 12; __calYear++; }
}

function renderCalendar(events) {
    const label = document.getElementById("calLabel");
    const grid  = document.getElementById("calGrid");
    if (!label || !grid) return;

    const monthNames = ["Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"];
    label.textContent = `${monthNames[__calMonth]} ${__calYear}`;

    const idx = buildEventsIndex(events);

    const first = new Date(__calYear, __calMonth, 1);
    const last  = new Date(__calYear, __calMonth + 1, 0);

    // Monday-based: JS getDay() => Sun=0..Sat=6
    const mondayIndex = (d) => (d.getDay() + 6) % 7;

    const startPad  = mondayIndex(first);
    const totalDays = last.getDate();
    const todayKey  = ymd(new Date());

    const cellsCount = Math.ceil((startPad + totalDays) / 7) * 7;

    let html = "";
    for (let i = 0; i < cellsCount; i++) {
        const dayNum = i - startPad + 1;
        const d = new Date(__calYear, __calMonth, dayNum);

        const inMonth = dayNum >= 1 && dayNum <= totalDays;
        const key = ymd(d);

        const cls = [
            "calCell",
            inMonth ? "" : "muted",
            key === todayKey ? "today" : "",
            key === WX.selectedDate ? "selected" : "",
            idx.has(key) ? "hasEvent" : ""
        ].filter(Boolean).join(" ");

        html += `<div class="${cls}" data-date="${key}" title="${idx.has(key) ? "Événements" : ""}">${d.getDate()}</div>`;
    }

    grid.innerHTML = html;
}

// -------------------------
// Weather (Open-Meteo)
// -------------------------
async function geocodeCity(city) {
    const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(city)}&count=1&language=fr&format=json`;
    const r = await fetch(url, { timeout: 15000 });
    if (!r.ok) throw new Error("GEO_HTTP_" + r.status);
    const j = await r.json();
    if (!j || !j.results || !j.results.length) throw new Error("CITY_NOT_FOUND");

    const it = j.results[0];
    return {
        name: it.name || city,
        country: it.country || "",
        lat: it.latitude,
        lon: it.longitude
    };
}

async function fetchForecast(lat, lon) {
    const url =
        `https://api.open-meteo.com/v1/forecast?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}` +
        `&current_weather=true` +
        `&hourly=relativehumidity_2m,apparent_temperature` +
        `&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max` +
        `&timezone=auto&forecast_days=16`;

    const r = await fetch(url, { timeout: 15000 });
    if (!r.ok) throw new Error("WX_HTTP_" + r.status);
    return await r.json();
}

async function fetchArchive(lat, lon, dateStr) {
    const url =
        `https://archive-api.open-meteo.com/v1/archive?latitude=${encodeURIComponent(lat)}&longitude=${encodeURIComponent(lon)}` +
        `&start_date=${encodeURIComponent(dateStr)}&end_date=${encodeURIComponent(dateStr)}` +
        `&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max` +
        `&timezone=auto`;

    const r = await fetch(url, { timeout: 15000 });
    if (!r.ok) throw new Error("ARCH_HTTP_" + r.status);
    return await r.json();
}

function pickCurrentFromHourly(hourly, currentIso) {
    if (!hourly || !hourly.time || !hourly.time.length) return { humid: null, feels: null };
    const idx = hourly.time.indexOf(currentIso);
    if (idx < 0) return { humid: null, feels: null };

    const humid = hourly.relativehumidity_2m ? hourly.relativehumidity_2m[idx] : null;
    const feels = hourly.apparent_temperature ? hourly.apparent_temperature[idx] : null;
    return { humid, feels };
}

function loadDailyIntoMap(daily) {
    if (!daily || !daily.time) return;
    for (let i = 0; i < daily.time.length; i++) {
        const date = daily.time[i];
        WX.dailyMap.set(date, {
            code: daily.weathercode ? daily.weathercode[i] : null,
            tmax: daily.temperature_2m_max ? daily.temperature_2m_max[i] : null,
            tmin: daily.temperature_2m_min ? daily.temperature_2m_min[i] : null,
            prec: daily.precipitation_sum ? daily.precipitation_sum[i] : null,
            wind: daily.windspeed_10m_max ? daily.windspeed_10m_max[i] : null
        });
    }
}

function renderWeatherMainForDate(dateStr) {
    const wb = document.getElementById("weatherBody");
    if (!wb) return;

    const item = WX.dailyMap.get(dateStr);
    const p = WX.place;

    if (!item || !p) {
        wb.innerHTML = `<div class="warnBox">Météo indisponible pour ${escapeHtml(dateStr)}.</div>`;
        return;
    }

    const placeLabel = `${escapeHtml(p.name)}${p.country ? " • " + escapeHtml(p.country) : ""}`;
    const desc = weatherLabel(item.code);
    const em   = weatherEmoji(item.code);

    const tmin = (item.tmin == null) ? "—" : `${Math.round(item.tmin)}°`;
    const tmax = (item.tmax == null) ? "—" : `${Math.round(item.tmax)}°`;
    const prec = (item.prec == null) ? "—" : `${Number(item.prec).toFixed(1)} mm`;
    const wind = (item.wind == null) ? "—" : `${Math.round(item.wind)} km/h`;

    const updTxt = WX.lastUpdated ? fmtFR(WX.lastUpdated) : "";

    const currentLine = (WX.current && WX.current.temp != null)
        ? `Actuellement: <b>${weatherEmoji(WX.current.code)} ${Math.round(WX.current.temp)}°</b> • Vent ${Math.round(WX.current.wind ?? 0)} km/h`
        : `Actuellement: —`;

    wb.innerHTML = `
    <div class="weatherNow">
      <div>
        <div class="miniStat" style="display:inline-flex; margin-bottom:10px;">📍 ${placeLabel}</div>
        <div class="weatherTemp">${em} ${tmin} → ${tmax}</div>
        <div class="weatherDesc">${escapeHtml(desc)}</div>
      </div>
    </div>

    <div class="weatherMeta">
      <div class="miniStat">🌧️ Pluie: ${escapeHtml(prec)}</div>
      <div class="miniStat">💨 Vent max: ${escapeHtml(wind)}</div>
      <div class="miniStat">📅 ${escapeHtml(dateStr)}</div>
    </div>

    <div class="weatherDayCard">
      <div class="weatherDayTitle">Détails</div>
      <div class="weatherDayDesc">
        ${currentLine}<br>
        Mise à jour: ${escapeHtml(updTxt)}
      </div>
    </div>
  `;
}

async function loadWeather() {
    const wb = document.getElementById("weatherBody");
    const cityInput = document.getElementById("weatherCity");
    const city = v(cityInput, "Tunis");
    WX.city = city;

    if (wb) wb.innerHTML = `<div class="warnBox">Chargement de la météo…</div>`;

    try {
        WX.place = await geocodeCity(city);

        const data = await fetchForecast(WX.place.lat, WX.place.lon);

        // current
        const cw = data.current_weather;
        const curIso = cw && cw.time ? cw.time : null;
        const extra = pickCurrentFromHourly(data.hourly, curIso);

        WX.current = {
            temp: cw ? cw.temperature : null,
            wind: cw ? cw.windspeed : null,
            code: cw ? cw.weathercode : null,
            time: cw ? cw.time : null,
            humid: extra.humid,
            feels: extra.feels
        };

        WX.lastUpdated = new Date();

        // daily
        if (data.daily && data.daily.time && data.daily.time.length) {
            loadDailyIntoMap(data.daily);
            WX.forecastLastDate = data.daily.time[data.daily.time.length - 1];
        } else {
            WX.forecastLastDate = null;
        }

        // si selectedDate vide -> aujourd'hui
        if (!WX.selectedDate) WX.selectedDate = ymd(new Date());

        // ✅ afficher météo du jour sélectionné
        renderWeatherMainForDate(WX.selectedDate);
    } catch (e) {
        console.error("Weather load error:", e);
        if (wb) wb.innerHTML = `<div class="warnBox"><b>Erreur météo.</b><br>${escapeHtml(String(e.message || e))}</div>`;
    }
}

async function loadWeatherForSelectedDay(dateStr) {
    // déjà dans forecast
    if (WX.dailyMap.has(dateStr)) {
        renderWeatherMainForDate(dateStr);
        return;
    }

    // si météo pas chargée encore
    if (!WX.place) {
        await loadWeather();
        if (WX.dailyMap.has(dateStr)) {
            renderWeatherMainForDate(dateStr);
            return;
        }
    }

    // passé => archive
    if (isBeforeToday(dateStr)) {
        const wb = document.getElementById("weatherBody");
        if (wb) wb.innerHTML = `<div class="warnBox">Chargement historique…</div>`;

        try {
            const arch = await fetchArchive(WX.place.lat, WX.place.lon, dateStr);
            if (arch && arch.daily && arch.daily.time && arch.daily.time.length) {
                loadDailyIntoMap(arch.daily);
            }
            renderWeatherMainForDate(dateStr);
            return;
        } catch (e) {
            console.error("Archive error:", e);
            const wb2 = document.getElementById("weatherBody");
            if (wb2) wb2.innerHTML = `<div class="warnBox">Historique indisponible pour ${escapeHtml(dateStr)}.</div>`;
            return;
        }
    }

    // futur trop loin
    if (WX.forecastLastDate && isAfter(dateStr, WX.forecastLastDate)) {
        const wb = document.getElementById("weatherBody");
        if (wb) wb.innerHTML = `
      <div class="warnBox">
        Prévision indisponible pour <b>${escapeHtml(dateStr)}</b> (limite ~16 jours).<br>
        Choisis une date plus proche.
      </div>
    `;
        return;
    }

    // sinon recharger forecast
    await loadWeather();
    renderWeatherMainForDate(dateStr);
}

// =========================================================
// Renders
// =========================================================
function renderHome() {
    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.add("isHidden");

    pageTitle.innerHTML = `
    <h1>Bienvenue à Investia 👋</h1>
    <p>Utilise la sidebar pour gérer les comptes, users, projets et événements.</p>
  `;

    contentArea.innerHTML = `
    <section class="welcomeCard enterAnim">
      <div class="welcomeTop">
        <div>
          <span class="welcomeBadge">Dashboard Admin</span>
          <h2 class="welcomeTitle" style="margin:10px 0 6px;">Accueil</h2>
          <p class="welcomeSub" style="margin:0; color: var(--mutedDark); font-weight: 750;">
            Météo locale + calendrier + prochains événements .
          </p>
        </div>

        <div class="welcomeActions">
          <button class="btnPrimary" id="goUsers">👥 Users</button>
          <button class="btnGhost" id="goAccounts">🧾 Comptes</button>
          <button class="btnGhost" id="goProjects">📁 Projets</button>
          <button class="btnGhost" id="goEvents">📅 Événements</button>
        </div>
      </div>

      <div class="homeWidgets">
        <section class="widgetCard">
          <div class="widgetHeader">
            <h3>Météo</h3>
            <div class="weatherSearch">
              <input id="weatherCity" type="text" value="${escapeHtml(WX.city || "Tunis")}" placeholder="Ville..." />
              <button class="btnIcon btnAnalyze" id="weatherRefresh" title="Actualiser">↻</button>
            </div>
          </div>
          <div id="weatherBody" class="weatherBody">
            <div class="warnBox">Chargement de la météo…</div>
          </div>
        </section>

        <section class="widgetCard">
          <div class="widgetHeader">
            <h3>Calendrier</h3>
            <div class="calControls">
              <button class="btnIcon" id="calPrev" title="Mois précédent">‹</button>
              <div class="calLabel" id="calLabel"></div>
              <button class="btnIcon" id="calNext" title="Mois suivant">›</button>
            </div>
          </div>

          <div class="calWeekdays">
            <div>Lun</div><div>Mar</div><div>Mer</div><div>Jeu</div><div>Ven</div><div>Sam</div><div>Dim</div>
          </div>

          <div id="calGrid" class="calGrid"></div>

          <div class="upNext">
            <div class="upTitle">À venir</div>
            <div id="upcomingList" class="upList"></div>
          </div>
        </section>
      </div>
    </section>
  `;

    document.getElementById("goUsers").onclick    = () => go("users");
    document.getElementById("goAccounts").onclick = () => go("accounts");
    document.getElementById("goProjects").onclick = () => go("projects");
    document.getElementById("goEvents").onclick   = () => go("events");

    // ✅ init widgets après injection DOM
    setTimeout(() => initHomeWeatherAndCalendar(), 0);
}

// -------------------------
// A) Comptes EN_ATTENTE
// -------------------------
function renderAccounts() {
    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.remove("isHidden");

    pageTitle.innerHTML = `
    <h1>Gestion des Comptes</h1>
    <p>Acceptez/refusez les comptes EN_ATTENTE.</p>
  `;

    const tableHtml = `
    <table>
      <thead>
        <tr>
          <th>ID</th><th>Email</th><th>Rôle</th><th>Statut</th>
          <th style="width:190px;">Actions</th>
        </tr>
      </thead>
      <tbody id="accountsTbody">
        ${pendingAccounts.map((a, idx) => `
          <tr class="rowAccount" data-idx="${idx}">
            <td>${a.id ?? ""}</td>
            <td>${a.email ?? ""}</td>
            <td>${a.role ?? ""}</td>
            <td>${a.statutVerification ?? ""}</td>
            <td>
              <div class="actions">
                <button class="btnIcon btnAccept" data-action="accept">✓</button>
                <button class="btnIcon btnReject" data-action="reject">✕</button>
              </div>
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    contentArea.innerHTML = "";
    const panel = mountPanel({
        title: "Demandes de comptes",
        withSearch: true,
        placeholder: "Rechercher un compte...",
        tableHtml
    });
    contentArea.appendChild(panel);

    filterRows("searchInput", ".rowAccount", (row) => row.innerText);

    const body = document.getElementById("accountsTbody");
    body.onclick = (e) => {
        const btn = e.target.closest("button[data-action]");
        if (!btn) return;

        const tr = e.target.closest("tr");
        const idx = Number(tr.dataset.idx);
        const action = btn.dataset.action;
        const acc = pendingAccounts[idx];

        if (!acc || acc.id == null) return alert("Erreur: id manquant.");

        const res = (action === "accept")
            ? callAdmin("acceptAccount", "ERR_UNDEFINED_RETURN", acc.id)
            : callAdmin("rejectAccount", "ERR_UNDEFINED_RETURN", acc.id);

        if (res === "OK") {
            loadFromJava();
            recomputeStats();
            go("users");
        } else {
            alert("Erreur action compte: " + res);
        }
    };
}

// -------------------------
// B) Users
// -------------------------
function renderUsers() {
    addUserBtn?.classList.remove("isHidden");
    statsRow?.classList.remove("isHidden");

    pageTitle.innerHTML = `<h1>Users</h1><p>Tous les utilisateurs + modifier / supprimer.</p>`;

    const tableHtml = `
    <table>
      <thead>
        <tr>
          <th>ID</th><th>Nom</th><th>Prénom</th><th>Email</th>
          <th>Mot de passe</th>
          <th>Tel</th><th>CIN</th>
          <th>Rôle</th><th>Statut</th>
          <th style="width:210px;">Actions</th>
        </tr>
      </thead>
      <tbody id="usersTbody">
        ${allUsers.map((u) => `
          <tr class="rowUser" data-id="${u.id}">
            <td>${u.id ?? ""}</td>
            <td>${u.nom ?? ""}</td>
            <td>${u.prenom ?? ""}</td>
            <td>${u.email ?? ""}</td>
            <td>••••••</td>
            <td>${u.telephone ?? ""}</td>
            <td>${u.cin ?? ""}</td>
            <td>${u.role ?? ""}</td>
            <td>${u.statutVerification ?? ""}</td>
            <td>
              <div class="actions">
                <button class="btnIcon" data-action="edit">✎</button>
                <button class="btnIcon btnReject" data-action="delete">🗑</button>
              </div>
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    contentArea.innerHTML = "";
    const panel = mountPanel({
        title: "Liste des Users",
        withSearch: true,
        placeholder: "Rechercher un user...",
        tableHtml
    });
    contentArea.appendChild(panel);

    filterRows("searchInput", ".rowUser", (row) => row.innerText);

    creatingUserRow = false;
    addUserBtn.onclick = () => {
        const tbody = document.getElementById("usersTbody");
        if (!tbody) return;

        if (creatingUserRow || tbody.querySelector("tr[data-new='1']")) return;

        const alreadyEditing = tbody.querySelector("tr[data-editing='1']");
        if (alreadyEditing) return alert("Terminez d'abord l'édition en cours (✓ ou ✕).");

        creatingUserRow = true;
        tbody.insertAdjacentHTML("afterbegin", buildNewUserRowHtml());
        const first = tbody.querySelector("tr[data-new='1'] input[data-field='nom']");
        if (first) first.focus();
    };

    const tbody = document.getElementById("usersTbody");
    tbody.onclick = (e) => {
        const btn = e.target.closest("button[data-action]");
        if (!btn) return;

        const tr = e.target.closest("tr");
        const action = btn.dataset.action;

        if (action === "create-cancel") {
            const newRow = tr.closest("tr[data-new='1']");
            if (newRow) newRow.remove();
            creatingUserRow = false;
            return;
        }

        if (action === "create-save") {
            const newRow = tr.closest("tr[data-new='1']");
            if (!newRow) return;

            const payload = readNewUserRow(newRow);
            const err = validateNewUser(payload);
            if (err) return alert(err);

            const res = callAdmin(
                "createUser",
                "ERR_UNDEFINED_RETURN",
                payload.nom,
                payload.prenom,
                payload.email,
                payload.telephone,
                payload.cin,
                payload.role,
                payload.statut_verification,
                payload.mot_de_passe
            );

            if (res === "OK") {
                creatingUserRow = false;
                loadFromJava();
                recomputeStats();
                renderUsers();
            } else {
                alert("Erreur ajout user: " + res);
            }
            return;
        }

        const userId = Number(tr.dataset.id);
        const u = allUsers.find(x => Number(x.id) === userId);
        if (!u) return alert("User introuvable.");

        if (action === "delete") {
            if (!confirm("Supprimer cet utilisateur ?")) return;
            const res = callAdmin("deleteUser", "ERR_UNDEFINED_RETURN", u.id);
            if (res === "OK") { loadFromJava(); recomputeStats(); renderUsers(); }
            else alert("Erreur deleteUser: " + res);
            return;
        }

        if (action === "edit") {
            if (tbody.querySelector("tr[data-new='1']")) {
                return alert("Terminez d'abord l'ajout en cours (✓ ou ✕).");
            }
            enterEditModeUserRow(tr, u);
            return;
        }

        if (action === "cancel") {
            const orig = safeParse(tr.dataset.orig || "{}", {});
            exitEditModeUserRow(tr, orig);
            return;
        }

        if (action === "save") {
            const old = u;

            const nom = v(tr.querySelector("input[data-field='nom']"), old.nom);
            const prenom = v(tr.querySelector("input[data-field='prenom']"), old.prenom);

            const emailInput = tr.querySelector("input[data-field='email']");
            const email = safeEmail(v(emailInput, old.email), old.email);

            const telephone = v(tr.querySelector("input[data-field='telephone']"), old.telephone);
            const cin = v(tr.querySelector("input[data-field='cin']"), old.cin);

            const role = v(tr.querySelector("select[data-field='role']"), old.role);
            const statut = v(tr.querySelector("select[data-field='statut']"), old.statutVerification);

            if (!email) return alert("Email obligatoire.");
            if (!isValidEmail(email)) return alert("Email invalide.");

            const res = callAdmin(
                "updateUser",
                "ERR_UNDEFINED_RETURN",
                old.id, nom, prenom, email, telephone, cin, role, statut
            );

            if (res === "OK") {
                loadFromJava();
                recomputeStats();
                renderUsers();
            } else {
                alert("Erreur updateUser: " + res);
            }
        }
    };
}
// -------------------------------------
// ✅ AJOUTS GLOBAUX (une seule fois)
// -------------------------------------
let cancelRequests = []; // demandes d’annulation EN_ATTENTE

/**
 * Recharge les demandes d’annulation depuis le Bridge
 * (ne touche pas aux autres loadFromJava existants)
 */
function refreshCancelRequestsInMemory() {
    try {
        // ✅ utiliser la même passerelle que le reste (callAdmin)
        const raw = (typeof callAdmin === "function")
            ? callAdmin("getDemandesAnnulationEnAttente", "ERR_UNDEFINED_RETURN")
            : "{\"ok\":false,\"data\":[]}";

        // si callAdmin te renvoie une erreur texte
        if (typeof raw === "string" && raw.startsWith("ERR_")) {
            console.error("getDemandesAnnulationEnAttente:", raw);
            cancelRequests = [];
            return;
        }

        const res = (typeof safeParse === "function")
            ? safeParse(raw, { ok: false, data: [] })
            : JSON.parse(raw);

        cancelRequests = (res && res.ok && Array.isArray(res.data)) ? res.data : [];
    } catch (e) {
        console.error(e);
        cancelRequests = [];
    }
}

// -------------------------
// C) Projets (✅ avec demandes d’annulation)
// -------------------------
function renderProjects() {
    // ✅ on recharge les demandes à chaque render
    refreshCancelRequestsInMemory();

    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.remove("isHidden");
    pageTitle.innerHTML = `<h1>Projets</h1><p>Validation + suppression + demandes d’annulation.</p>`;

    // =========================
    // 1) Demandes d’annulation EN_ATTENTE (NOUVEAU)
    // =========================
    const cancelTable = `
    <h3 style="margin:0 0 10px 0;">Demandes d’annulation EN_ATTENTE</h3>
    <table>
      <thead>
        <tr>
          <th>ID Dem.</th>
          <th>ID Projet</th>
          <th>Titre</th>
          <th>Raison</th>
          <th>Date</th>
          <th style="width:240px;">Actions</th>
        </tr>
      </thead>
      <tbody id="cancelRequestsTbody">
        ${
        (cancelRequests.length === 0)
            ? `<tr><td colspan="6" style="opacity:.7;">Aucune demande d’annulation.</td></tr>`
            : cancelRequests.map((d, idx) => `
              <tr class="rowCancelReq" data-idx="${idx}">
                <td>${d.id ?? ""}</td>
                <td>${d.projetId ?? ""}</td>
                <td>${d.titre ?? ""}</td>
                <td>${d.raison ?? ""}</td>
                <td>${d.createdAt ?? ""}</td>
                <td>
                  <div class="actions">
                    <button class="btnIcon btnAnalyze" data-action="analyze" title="Analyser projet">📊</button>
                    <button class="btnIcon btnAccept" data-action="acceptCancel" title="Accepter annulation (supprime projet)">✓</button>
                    <button class="btnIcon btnReject" data-action="rejectCancel" title="Refuser annulation (projet reste)">✕</button>
                  </div>
                </td>
              </tr>
            `).join("")
    }
      </tbody>
    </table>
  `;

    // =========================
    // 2) Projets EN_ATTENTE (inchangé)
    // =========================
    const pendingTable = `
    <h3 style="margin:18px 0 10px 0;">Projets EN_ATTENTE</h3>
    <table>
      <thead><tr><th>ID</th><th>Titre</th><th>Secteur</th><th>Statut</th><th style="width:240px;">Actions</th></tr></thead>
      <tbody id="projectsPendingTbody">
        ${pendingProjects.map((p, idx) => `
          <tr class="rowProjPend" data-idx="${idx}">
            <td>${p.id ?? ""}</td>
            <td>${p.titre ?? ""}</td>
            <td>${p.secteur ?? ""}</td>
            <td>${p.statut ?? ""}</td>
            <td>
              <div class="actions">
                <button class="btnIcon btnAnalyze" data-action="analyze" title="Analyser">📊</button>
                <button class="btnIcon btnAccept" data-action="accept" title="Valider">✓</button>
                <button class="btnIcon btnReject" data-action="reject" title="Refuser">✕</button>
              </div>
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    // =========================
    // 3) Autres projets (inchangé)
    // =========================
    const otherProjects = allProjects.filter(p => (p.statut ?? "") !== "EN_ATTENTE");
    const allTable = `
    <h3 style="margin:18px 0 10px 0;">Autres projets</h3>
    <table>
      <thead><tr><th>ID</th><th>Titre</th><th>Secteur</th><th>Statut</th><th style="width:120px;">Action</th></tr></thead>
      <tbody id="projectsAllTbody">
        ${otherProjects.map((p) => `
          <tr class="rowProjAll" data-id="${p.id}">
            <td>${p.id ?? ""}</td>
            <td>${p.titre ?? ""}</td>
            <td>${p.secteur ?? ""}</td>
            <td>${p.statut ?? ""}</td>
            <td><button class="btnIcon btnReject" data-action="delete">🗑</button></td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    // =========================
    // Mount panel (HTML ensemble)
    // =========================
    contentArea.innerHTML = "";
    const panel = mountPanel({
        title: "Gestion Projets",
        withSearch: true,
        placeholder: "Rechercher un projet...",
        tableHtml: cancelTable + pendingTable + allTable
    });
    contentArea.appendChild(panel);

    // ✅ recherche inclut aussi les demandes d’annulation
    filterRows("searchInput", ".rowCancelReq, .rowProjPend, .rowProjAll", (row) => row.innerText);

    // =========================
    // Click handlers - Demandes Annulation (NOUVEAU)
    // =========================
    const cancelBody = document.getElementById("cancelRequestsTbody");
    if (cancelBody) {
        cancelBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action]");
            if (!btn) return;

            const tr = e.target.closest("tr");
            const idx = Number(tr.dataset.idx);
            const d = cancelRequests[idx];
            if (!d) return;

            const action = btn.dataset.action;

            if (action === "analyze") {
                // analyse du projet concerné
                openProjectAnalysis(Number(d.projetId));
                return;
            }

            if (action === "acceptCancel") {
                const raw = callAdmin("accepterDemandeAnnulation", "ERR_UNDEFINED_RETURN", Number(d.id));
                if (typeof raw === "string" && raw.startsWith("ERR_")) { alert("Erreur annulation: " + raw); return; }

                const res = safeParse(raw, { ok:false });
                if (res.ok) { loadFromJava(); recomputeStats(); renderProjects(); }
                else alert("Erreur annulation: ACCEPT_FAILED");
                return;
            }

            if (action === "rejectCancel") {
                const raw = callAdmin("refuserDemandeAnnulation", "ERR_UNDEFINED_RETURN", Number(d.id));
                if (typeof raw === "string" && raw.startsWith("ERR_")) { alert("Erreur annulation: " + raw); return; }

                const res = safeParse(raw, { ok:false });
                if (res.ok) { loadFromJava(); recomputeStats(); renderProjects(); }
                else alert("Erreur annulation: REFUSE_FAILED");
                return;
            }
        };
    }

    // =========================
    // Click handlers - Projets EN_ATTENTE (inchangé)
    // =========================
    const pendBody = document.getElementById("projectsPendingTbody");
    if (pendBody) {
        pendBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action]");
            if (!btn) return;

            const tr = e.target.closest("tr");
            const idx = Number(tr.dataset.idx);
            const p = pendingProjects[idx];
            if (!p) return;

            const action = btn.dataset.action;

            if (action === "analyze") {
                openProjectAnalysis(p.id);
                return;
            }

            const res = (action === "accept")
                ? callAdmin("acceptProject", "ERR_UNDEFINED_RETURN", p.id)
                : callAdmin("rejectProject", "ERR_UNDEFINED_RETURN", p.id);

            if (res === "OK") { loadFromJava(); recomputeStats(); renderProjects(); }
            else alert("Erreur projet: " + res);
        };
    }

    // =========================
    // Click handlers - Autres projets (inchangé)
    // =========================
    const allBody = document.getElementById("projectsAllTbody");
    if (allBody) {
        allBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action='delete']");
            if (!btn) return;
            const tr = e.target.closest("tr");
            const id = Number(tr.dataset.id);
            if (!confirm("Supprimer ce projet ?")) return;

            const res = callAdmin("deleteProject", "ERR_UNDEFINED_RETURN", id);
            if (res === "OK") { loadFromJava(); recomputeStats(); renderProjects(); }
            else alert("Erreur suppression projet: " + res);
        };
    }
}

// -------------------------
// Analyse Projet (BI) — inchangé
// -------------------------
function renderProjectAnalysis() {
    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.add("isHidden");

    const projectId = Number(currentAnalysisProjectId);
    pageTitle.innerHTML = `
    <h1>Analyse Projet</h1>
    <p>Dashboard décisionnel (graphes + %). Projet #${projectId}</p>
  `;

    contentArea.innerHTML = "";
    const panel = document.createElement("section");
    panel.className = "panel enterAnim";
    panel.innerHTML = `
    <div class="panelHeader">
      <div>
        <h2 class="panelTitle">Analyse BI — Décision en pourcentage</h2>
        <div class="panelSub">Bénéfice vs Risque + Confiance + facteurs</div>
      </div>
      <div class="actions">
        <button class="btnPrimary" id="btnBackProjects">← Retour</button>
      </div>
    </div>

    <div id="analysisRoot">
      <div class="analysisGridTop">
        <div class="chartCard">
          <div class="chartTitle">Décision (Bénéfice vs Risque)</div>
          <canvas id="chartDecisionDonut" height="160"></canvas>
          <div class="decisionText" id="decisionText"></div>
        </div>

        <div class="chartCard">
          <div class="chartTitle">Bénéfice (%) — Gauge</div>
          <canvas id="chartGauge" height="160"></canvas>
          <div class="decisionText" id="gaugeText"></div>
        </div>

        <div class="chartCard">
          <div class="chartTitle">Profil (Radar)</div>
          <canvas id="chartRadar" height="170"></canvas>
        </div>
      </div>

      <div class="analysisGridBottom">
        <div class="chartCard">
          <div class="chartTitle">Projection (12 mois) — Net vs Mensualité</div>
          <canvas id="chartLine12" height="170"></canvas>
        </div>

        <div class="chartCard">
          <div class="chartTitle">Ce qui fait monter/descendre le score</div>
          <canvas id="chartContrib" height="170"></canvas>
          <div class="miniExplain" id="miniExplain"></div>
        </div>
      </div>
    </div>
  `;
    contentArea.appendChild(panel);

    document.getElementById("btnBackProjects").onclick = () => {
        destroyAnalysisCharts();
        go("projects");
    };

    if (!window.Chart) {
        document.getElementById("analysisRoot").innerHTML = `
      <div class="warnBox">
        <b>Chart.js non chargé.</b><br>
        Ajoute <code>chart.umd.min.js</code> dans <code>../js/vendor/</code> et inclus-le dans le HTML.
      </div>
    `;
        return;
    }

    destroyAnalysisCharts();
    const json = callAdmin("getProjectAnalysisJson", "{}", projectId);
    const data = safeParse(json, null);

    if (!data || !data.decision) {
        document.getElementById("analysisRoot").innerHTML = `
      <div class="warnBox">
        <b>Analyse indisponible.</b><br>
        Vérifie côté Java que <code>getProjectAnalysisJson</code> existe et retourne un JSON.
      </div>
    `;
        return;
    }

    const d = data.decision;
    const badge = d.recommandation === "BENEFIQUE" ? "✅ BÉNÉFIQUE"
        : d.recommandation === "MITIGE" ? "⚠️ MITIGÉ"
            : "❌ RISQUÉ";

    document.getElementById("decisionText").innerHTML = `
    <div class="bigLine">${badge}</div>
    <div class="smallLine">
      Bénéfice: <b>${d.benefice_pct}%</b> • Risque: <b>${d.risque_pct}%</b> • Confiance: <b>${d.confiance_pct}%</b>
    </div>
  `;

    document.getElementById("gaugeText").innerHTML = `
    <div class="bigLine"><b>${d.benefice_pct}%</b> bénéfice estimé</div>
    <div class="smallLine">Confiance: <b>${d.confiance_pct}%</b></div>
  `;

    const c1 = new Chart(document.getElementById("chartDecisionDonut"), {
        type: "doughnut",
        data: { labels: ["Bénéfice", "Risque"], datasets: [{ data: [d.benefice_pct, d.risque_pct] }] },
        options: { responsive: true, plugins: { legend: { position: "bottom" } } }
    });
    analysisCharts.push(c1);

    const c2 = new Chart(document.getElementById("chartGauge"), {
        type: "doughnut",
        data: { labels: ["Bénéfice", "Reste"], datasets: [{ data: [d.benefice_pct, 100 - d.benefice_pct] }] },
        options: { rotation: -90, circumference: 180, cutout: "70%", plugins: { legend: { display: false } } }
    });
    analysisCharts.push(c2);

    const r = data.radar;
    const c3 = new Chart(document.getElementById("chartRadar"), {
        type: "radar",
        data: { labels: r.labels, datasets: [{ label: "Score (0-100)", data: r.values }] },
        options: { responsive: true, scales: { r: { suggestedMin: 0, suggestedMax: 100 } } }
    });
    analysisCharts.push(c3);

    const s = data.series;
    const c4 = new Chart(document.getElementById("chartLine12"), {
        type: "line",
        data: { labels: s.labels, datasets: [
                { label: "Net mensuel estimé (TND)", data: s.netMensuel },
                { label: "Mensualité (TND)", data: s.mensualite }
            ]},
        options: { responsive: true, plugins: { legend: { position: "bottom" } } }
    });
    analysisCharts.push(c4);

    const contrib = data.contributions;
    const c5 = new Chart(document.getElementById("chartContrib"), {
        type: "bar",
        data: { labels: contrib.map(x => x.label), datasets: [{ label: "Impact (points)", data: contrib.map(x => x.value) }] },
        options: { responsive: true, plugins: { legend: { display: false } } }
    });
    analysisCharts.push(c5);

    if (Array.isArray(data.explain)) {
        document.getElementById("miniExplain").innerHTML =
            data.explain.slice(0, 3).map(t => `<div class="pill">• ${escapeHtml(t)}</div>`).join("");
    }
}

// -------------------------
// D) Events
// -------------------------
function renderEvents() {
    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.remove("isHidden");
    pageTitle.innerHTML = `<h1>Événements</h1><p>Validation + suppression.</p>`;

    const pendingTable = `
    <h3 style="margin:0 0 10px 0;">Événements EN_ATTENTE</h3>
    <table>
      <thead><tr><th>ID</th><th>Titre</th><th>Mode</th><th>Début</th><th>Statut</th><th style="width:190px;">Actions</th></tr></thead>
      <tbody id="eventsPendingTbody">
        ${pendingEvents.map((ev, idx) => `
          <tr class="rowEvPend" data-idx="${idx}">
            <td>${ev.id ?? ""}</td>
            <td>${ev.titre ?? ""}</td>
            <td>${ev.mode ?? ""}</td>
            <td>${ev.dateDebut ?? ""}</td>
            <td>${ev.statut ?? ""}</td>
            <td>
              <div class="actions">
                <button class="btnIcon btnAccept" data-action="accept">✓</button>
                <button class="btnIcon btnReject" data-action="reject">✕</button>
              </div>
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    const otherEvents = allEvents.filter(ev => (ev.statut ?? "") !== "EN_ATTENTE");
    const allTable = `
    <h3 style="margin:18px 0 10px 0;">Autres événements</h3>
    <table>
      <thead><tr><th>ID</th><th>Titre</th><th>Mode</th><th>Début</th><th>Statut</th><th style="width:120px;">Action</th></tr></thead>
      <tbody id="eventsAllTbody">
        ${otherEvents.map(ev => `
          <tr class="rowEvAll" data-id="${ev.id}">
            <td>${ev.id ?? ""}</td>
            <td>${ev.titre ?? ""}</td>
            <td>${ev.mode ?? ""}</td>
            <td>${ev.dateDebut ?? ""}</td>
            <td>${ev.statut ?? ""}</td>
            <td><button class="btnIcon btnReject" data-action="delete">🗑</button></td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    contentArea.innerHTML = "";
    const panel = mountPanel({
        title: "Gestion Événements",
        withSearch: true,
        placeholder: "Rechercher un événement...",
        tableHtml: pendingTable + allTable
    });
    contentArea.appendChild(panel);

    filterRows("searchInput", ".rowEvPend, .rowEvAll", (row) => row.innerText);

    const pendBody = document.getElementById("eventsPendingTbody");
    if (pendBody) {
        pendBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action]");
            if (!btn) return;
            const tr = e.target.closest("tr");
            const idx = Number(tr.dataset.idx);
            const ev = pendingEvents[idx];
            if (!ev) return;

            const res = (btn.dataset.action === "accept")
                ? callAdmin("acceptEvent", "ERR_UNDEFINED_RETURN", ev.id)
                : callAdmin("rejectEvent", "ERR_UNDEFINED_RETURN", ev.id);

            if (res === "OK") { loadFromJava(); recomputeStats(); renderEvents(); }
            else alert("Erreur événement: " + res);
        };
    }

    const allBody = document.getElementById("eventsAllTbody");
    if (allBody) {
        allBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action='delete']");
            if (!btn) return;
            const tr = e.target.closest("tr");
            const id = Number(tr.dataset.id);
            if (!confirm("Supprimer cet événement ?")) return;

            const res = callAdmin("deleteEvent", "ERR_UNDEFINED_RETURN", id);
            if (res === "OK") { loadFromJava(); recomputeStats(); renderEvents(); }
            else alert("Erreur suppression événement: " + res);
        };
    }
}

// -------------------------
// Historique
// -------------------------
function renderHistory() {
    addUserBtn?.classList.add("isHidden");
    statsRow?.classList.add("isHidden");

    pageTitle.innerHTML = `<h1>Historique</h1><p>Actions admin.</p>`;

    const tableHtml = `
    <table>
      <thead><tr><th>Date</th><th>Action</th><th>Détails</th></tr></thead>
      <tbody>
        ${historyLog.length ? historyLog.map(h => `
          <tr class="rowHist">
            <td>${h.createdAt ?? ""}</td>
            <td>${h.action ?? ""}</td>
            <td>${h.details ?? ""}</td>
          </tr>
        `).join("") : `<tr><td colspan="3">Aucun historique.</td></tr>`}
      </tbody>
    </table>
  `;

    contentArea.innerHTML = "";
    const panel = mountPanel({ title: "Historique", withSearch: true, placeholder: "Search...", tableHtml });
    contentArea.appendChild(panel);

    filterRows("searchInput", ".rowHist", (row) => row.innerText);
}

// =========================
// Navigation
// =========================
function go(page) {
    setActiveNav(page === "projectAnalysis" ? "projects" : page);

    if (page !== "projectAnalysis") destroyAnalysisCharts();

    if (page === "home") renderHome();
    if (page === "accounts") renderAccounts();
    if (page === "users") renderUsers();
    if (page === "projects") renderProjects();
    if (page === "projectAnalysis") renderProjectAnalysis();
    if (page === "events") renderEvents();
    if (page === "history") renderHistory();

    recomputeStats();
}

document.querySelectorAll(".nav button").forEach((btn) => {
    btn.addEventListener("click", () => go(btn.dataset.page));
});

document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("logoutBtn");
    if (btn) {
        btn.addEventListener("click", () => {
            if (window.admin && typeof window.admin.logout === "function") {
                window.admin.logout();
            } else {
                alert("Bridge admin non disponible (logout).");
            }
        });
    }
});

// =========================
// Sidebar hover expand
// =========================
(function initSidebarHover(){
    const app = document.querySelector(".app");
    const sidebar = document.querySelector(".sidebar");
    if (!app || !sidebar) return;

    app.classList.remove("sb-open"); // fermé par défaut
    sidebar.addEventListener("mouseenter", () => app.classList.add("sb-open"));
    sidebar.addEventListener("mouseleave", () => app.classList.remove("sb-open"));
})();

// =========================
// Wait bridge
// =========================
function waitForAdminBridge(tries = 60) {
    if (window.admin) {
        loadFromJava();
        recomputeStats();
        go("home");
        return;
    }

    if (tries <= 0) {
        alert("❌ Bridge admin introuvable. Vérifie l’injection Java.");
        return;
    }

    setTimeout(() => waitForAdminBridge(tries - 1), 100);
}

// =========================
// init
// =========================
waitForAdminBridge();