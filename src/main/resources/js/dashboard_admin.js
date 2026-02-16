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

// =========================
// DOM
// =========================
const contentArea = document.getElementById("contentArea");
const pageTitle = document.getElementById("pageTitle");
const statsRow = document.getElementById("statsRow");
const addUserBtn = document.getElementById("addUserBtn");

const statPending = document.getElementById("statPending");
const statUsers = document.getElementById("statUsers");
const statProjects = document.getElementById("statProjects");
const statEvents = document.getElementById("statEvents");

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
    statPending.textContent  = pendingAccounts.length;
    statUsers.textContent    = allUsers.length;
    statProjects.textContent = pendingProjects.length;
    statEvents.textContent   = pendingEvents.length;
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
// INLINE EDIT (Users)
// =========================
function enterEditModeUserRow(tr, u) {
    if (tr.dataset.editing === "1") return;
    tr.dataset.editing = "1";
    tr.dataset.orig = JSON.stringify(u);

    const tds = tr.querySelectorAll("td");

    // Colonnes:
    // ID(0) NOM(1) PRENOM(2) EMAIL(3) MDP(4) TEL(5) CIN(6) ROLE(7) STATUT(8) ACTIONS(9)

    tds[1].innerHTML = `<input class="cellInput" data-field="nom" type="text" value="${escapeHtml(u.nom)}">`;
    tds[2].innerHTML = `<input class="cellInput" data-field="prenom" type="text" value="${escapeHtml(u.prenom)}">`;

    const emailVal = String(u.email ?? "");
    tds[3].innerHTML = `<input class="cellInput" data-field="email" type="email" value="${escapeHtml(emailVal)}">`;

    // mot de passe: on ne modifie pas ici (sécurité). On garde masqué.
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

    // mot de passe masqué
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

    // IMPORTANT: en DB c'est statut_verification (snake_case), mais côté JS on garde "statut"
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

// =========================
// Renders
// =========================
function renderHome() {
    addUserBtn.classList.add("isHidden");
    statsRow.classList.remove("isHidden");

    pageTitle.innerHTML = `
    <h1>Bienvenue à Investia 👋</h1>
    <p>Gérez les demandes de comptes, users, projets et événements.</p>
  `;

    contentArea.innerHTML = `
    <div class="welcomeCard enterAnim">
      <h2>Tableau de bord Administrateur</h2>
      <p>Données réelles depuis MySQL ✅</p>
    </div>
  `;
}

// -------------------------
// A) Comptes EN_ATTENTE
// -------------------------
function renderAccounts() {
    addUserBtn.classList.add("isHidden");
    statsRow.classList.remove("isHidden");

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
// B) Users (inline edit + delete + add row)
// -------------------------
function renderUsers() {
    addUserBtn.classList.remove("isHidden");
    statsRow.classList.remove("isHidden");

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

    // Bouton "Ajouter un utilisateur" => insère une ligne editable
    creatingUserRow = false;
    addUserBtn.onclick = () => {
        const tbody = document.getElementById("usersTbody");
        if (!tbody) return;

        // Empêcher plusieurs lignes "new"
        if (creatingUserRow || tbody.querySelector("tr[data-new='1']")) return;

        // Si une ligne est déjà en mode edit, on refuse
        const alreadyEditing = tbody.querySelector("tr[data-editing='1']");
        if (alreadyEditing) return alert("Terminez d'abord l'édition en cours (✓ ou ✕).");

        creatingUserRow = true;
        tbody.insertAdjacentHTML("afterbegin", buildNewUserRowHtml());
        const first = tbody.querySelector("tr[data-new='1'] input[data-field='nom']");
        if (first) first.focus();
    };

    // Event delegation
    const tbody = document.getElementById("usersTbody");
    tbody.onclick = (e) => {
        const btn = e.target.closest("button[data-action]");
        if (!btn) return;

        const tr = e.target.closest("tr");
        const action = btn.dataset.action;

        // ===== CREATE (nouvelle ligne)
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

            // ✅ Appel Java : createUser(nom, prenom, email, telephone, cin, role, statut_verification, mot_de_passe)
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

        // ===== ACTIONS sur user existant
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

// -------------------------
// C) Projets
// -------------------------
function renderProjects() {
    addUserBtn.classList.add("isHidden");
    statsRow.classList.remove("isHidden");
    pageTitle.innerHTML = `<h1>Projets</h1><p>Validation + suppression.</p>`;

    const pendingTable = `
    <h3 style="margin:0 0 10px 0;">Projets EN_ATTENTE</h3>
    <table>
      <thead><tr><th>ID</th><th>Titre</th><th>Secteur</th><th>Statut</th><th style="width:190px;">Actions</th></tr></thead>
      <tbody id="projectsPendingTbody">
        ${pendingProjects.map((p, idx) => `
          <tr class="rowProjPend" data-idx="${idx}">
            <td>${p.id ?? ""}</td>
            <td>${p.titre ?? ""}</td>
            <td>${p.secteur ?? ""}</td>
            <td>${p.statut ?? ""}</td>
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

    contentArea.innerHTML = "";
    const panel = mountPanel({
        title: "Gestion Projets",
        withSearch: true,
        placeholder: "Rechercher un projet...",
        tableHtml: pendingTable + allTable
    });
    contentArea.appendChild(panel);

    filterRows("searchInput", ".rowProjPend, .rowProjAll", (row) => row.innerText);

    const pendBody = document.getElementById("projectsPendingTbody");
    if (pendBody) {
        pendBody.onclick = (e) => {
            const btn = e.target.closest("button[data-action]");
            if (!btn) return;
            const tr = e.target.closest("tr");
            const idx = Number(tr.dataset.idx);
            const p = pendingProjects[idx];
            if (!p) return;

            const res = (btn.dataset.action === "accept")
                ? callAdmin("acceptProject", "ERR_UNDEFINED_RETURN", p.id)
                : callAdmin("rejectProject", "ERR_UNDEFINED_RETURN", p.id);

            if (res === "OK") { loadFromJava(); recomputeStats(); renderProjects(); }
            else alert("Erreur projet: " + res);
        };
    }

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
// D) Events
// -------------------------
function renderEvents() {
    addUserBtn.classList.add("isHidden");
    statsRow.classList.remove("isHidden");
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
    addUserBtn.classList.add("isHidden");
    statsRow.classList.add("isHidden");

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
    setActiveNav(page);

    if (page === "home") renderHome();
    if (page === "accounts") renderAccounts();
    if (page === "users") renderUsers();
    if (page === "projects") renderProjects();
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

// init
waitForAdminBridge();
