(() => {
  const css = `
.navbar{
  position: sticky;
  top: 0;
  z-index: 999;
  background: #fff;
  border-bottom: 1px solid rgba(15,23,42,.08);
  box-shadow:0 8px 24px rgba(15,23,42,.06);
}
.nav-inner{
  max-width:100%;
  margin:0;
  padding:12px 20px 12px 8px;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:16px;
}
.brand{
  display:flex;
  align-items:center;
  gap:12px;
  letter-spacing:.2px;
  text-decoration:none;
}
.brand img{
  width:52px;
  height:52px;
  object-fit:contain;
}
.brand span{
  font-size:26px;
  font-weight:900;
  color:#1e3a8a;
  letter-spacing:.2px;
  text-decoration:none;
}
.brand .ia{
  color:#d97706;
  font-weight:900;
}
.nav-links{
  display:flex;
  gap:18px;
  flex-wrap:wrap;
  align-items:center;
  font-weight:700;
  font-size:14px;
  color:#1e3a8a;
}
.nav-links a{
  padding:6px 0 8px;
  border-bottom:none;
  color:#1e3a8a;
  font-weight:800;
  letter-spacing:.2px;
  text-decoration:none;
}
.nav-links a::before{
  content:none;
}
.nav-links a:hover{
  color:#d97706;
}
.nav-links a.active{
  color:#d97706;
}
.nav-user{
  position:relative;
  display:flex;
  align-items:center;
  justify-content:flex-end;
  gap:10px;
  font-size:14px;
  color:#334155;
  font-weight:700;
}
.icon-btn{
  width:36px;
  height:36px;
  border-radius:999px;
  border:1px solid rgba(15,23,42,.10);
  background:#fff;
  display:inline-flex;
  align-items:center;
  justify-content:center;
  padding:0;
  cursor:pointer;
}
.icon-btn svg{
  width:18px;
  height:18px;
  stroke:#334155;
  display:block;
}
.icon-btn:hover{
  background: rgba(15,23,42,.04);
}
.notif-wrap{
  position:relative;
}
.msg-wrap{
  position:relative;
}
.fav-wrap{
  position:relative;
  width:36px;
  height:36px;
  display:inline-flex;
  align-items:center;
  justify-content:center;
}
.fav-badge{
  position:absolute;
  top:-4px;
  right:-4px;
  background:#f59e0b;
  color:#fff;
  font-size:10px;
  font-weight:800;
  padding:2px 6px;
  min-width:18px;
  text-align:center;
  border-radius:999px;
  line-height:1;
  display:none;
  z-index:2;
}
.fav-dropdown{
  position:absolute;
  right:0;
  top:46px;
  min-width:300px;
  max-width:380px;
  background:#fff;
  border:1px solid rgba(15,23,42,.10);
  border-radius:12px;
  box-shadow:0 14px 34px rgba(15,23,42,.14);
  padding:6px;
  display:none;
  z-index:1000;
}
.fav-dropdown.open{
  display:block;
}
.fav-item{
  padding:10px 12px;
  border-radius:10px;
  cursor:pointer;
  font-size:13px;
  font-weight:700;
  color:#0f172a;
}
.fav-item:hover{
  background:rgba(15,23,42,.05);
}
.fav-item .muted{
  font-weight:600;
  color:#64748b;
  font-size:12px;
}
.fav-footer{
  padding:8px 10px 6px;
  display:flex;
  gap:8px;
}
.fav-footer button{
  flex:1;
  border:1px solid rgba(15,23,42,.10);
  background:#fff;
  border-radius:10px;
  padding:8px 10px;
  font-weight:800;
  cursor:pointer;
  font-size:13px;
}
.fav-footer button:hover{
  background:rgba(15,23,42,.04);
}
.notif-badge{
  position:absolute;
  top:-4px;
  right:-4px;
  background:#ef4444;
  color:#fff;
  font-size:10px;
  font-weight:800;
  padding:2px 6px;
  border-radius:999px;
  line-height:1;
  display:none;
}
.msg-badge{
  position:absolute;
  top:-4px;
  right:-4px;
  background:#ef4444;
  color:#fff;
  font-size:10px;
  font-weight:800;
  padding:2px 6px;
  min-width:18px;
  text-align:center;
  border-radius:999px;
  line-height:1;
  display:none;
  z-index:2;
}
.msg-dropdown{
  position:absolute;
  right:0;
  top:46px;
  min-width:280px;
  max-width:360px;
  background:#fff;
  border:1px solid rgba(15,23,42,.10);
  border-radius:12px;
  box-shadow:0 14px 34px rgba(15,23,42,.14);
  padding:6px;
  display:none;
  z-index:1000;
}
.msg-dropdown.open{
  display:block;
}
.msg-item{
  padding:10px 12px;
  border-radius:10px;
  cursor:pointer;
  font-size:13px;
  font-weight:700;
  color:#0f172a;
}
.msg-item:hover{
  background:rgba(15,23,42,.05);
}
.msg-item .muted{
  font-weight:600;
  color:#64748b;
  font-size:12px;
}
.notif-dropdown{
  position:absolute;
  right:0;
  top:46px;
  min-width:260px;
  background:#fff;
  border:1px solid rgba(15,23,42,.10);
  border-radius:12px;
  box-shadow:0 14px 34px rgba(15,23,42,.14);
  padding:6px;
  display:none;
  z-index:1000;
}
.notif-dropdown.open{
  display:block;
}
.notif-item{
  padding:10px 12px;
  border-radius:10px;
  cursor:pointer;
  font-size:13px;
  font-weight:700;
  color:#0f172a;
}
.notif-item .muted{
  font-weight:600;
  color:#64748b;
  font-size:12px;
}
.notif-item.unread{
  background:rgba(250,204,21,.15);
  border:1px solid rgba(245,158,11,.35);
}
.notif-footer{
  padding:8px 10px 6px;
}
.notif-footer button{
  width:100%;
  border:1px solid rgba(15,23,42,.10);
  background:#fff;
  border-radius:10px;
  padding:8px 10px;
  font-weight:800;
  cursor:pointer;
  font-size:13px;
}
.notif-footer button:hover{
  background:rgba(15,23,42,.04);
}
.notif-item:hover{
  background: rgba(15,23,42,.05);
}
.notif-empty{
  padding:10px 12px;
  font-size:13px;
  color:#64748b;
}
.user-menu-btn{
  border:1px solid rgba(15,23,42,.10);
  background:#fff;
  border-radius:999px;
  padding:4px 10px 4px 4px;
  display:inline-flex;
  align-items:center;
  gap:8px;
  cursor:pointer;
  font-weight:700;
  color:#334155;
}
.user-menu-btn:hover{
  background: rgba(15,23,42,.04);
}
.avatar{
  width:30px;
  height:30px;
  border-radius:50%;
  background:#e2e8f0;
  display:inline-flex;
  align-items:center;
  justify-content:center;
  font-weight:800;
  color:#0f172a;
}
.caret{
  font-size:11px;
  color:#64748b;
}
.user-dropdown{
  position:absolute;
  right:0;
  top:46px;
  min-width:180px;
  background:#fff;
  border:1px solid rgba(15,23,42,.10);
  border-radius:12px;
  box-shadow:0 14px 34px rgba(15,23,42,.14);
  padding:6px;
  display:none;
}
.user-dropdown.open{
  display:block;
}
.user-dropdown button{
  width:100%;
  border:0;
  background:transparent;
  text-align:left;
  border-radius:8px;
  padding:10px 12px;
  cursor:pointer;
  font-size:14px;
  font-weight:700;
  color:#0f172a;
}
.user-dropdown button:hover{
  background: rgba(239,68,68,.08);
  color:#b91c1c;
}
`;

  const style = document.createElement("style");
  style.textContent = css;
  document.head.appendChild(style);

  const hrefNoHash = (window.location.href.split("#")[0].split("?")[0] || "");
  function computeRootPrefix(url) {
    const idx = url.indexOf("/web/");
    if (idx === -1) return "";
    const rest = url.substring(idx + "/web/".length);
    const parts = rest.split("/").filter(Boolean);
    const folderDepth = Math.max(0, parts.length - 1);
    return "../".repeat(folderDepth + 1);
  }
  const root = computeRootPrefix(hrefNoHash);
  const path = (rel) => root + rel;

  const nav = document.createElement("nav");
  nav.className = "navbar";
  nav.innerHTML = `
    <div class="nav-inner">
      <a class="brand" href="${path("projet_view.html")}">
        <img src="${path("logo.png")}" alt="Logo">
        <span>Invest<span class="ia">ia</span></span>
      </a>
      <div class="nav-links">
        <a href="${path("accueil.html")}" data-key="accueil">Accueil</a>
        <a href="${path("projet_view.html")}" data-key="projets">Mes projets</a>
        <a href="${path("web/mes_investissements_view.html")}" data-key="mes-investissements" style="display:none;">Mes investissements</a>
        <a href="${path("web/ajoutEvenement.html")}" data-key="evenement" style="display:none;">Ã‰vÃ©nements</a>
        <a href="${path("web/financement/financements-list.html")}" data-key="financements" style="display:none;">Financements</a>
        <a href="${path("web/remboursement/remboursements-list.html")}" data-key="remboursements" style="display:none;">Remboursements</a>
        <a href="${path("chatbot.html")}" data-key="chatbot">Assistant IA</a>
        <a href="${path("contact.html")}" data-key="contact">Contact</a>
      </div>
      <div class="nav-user">
        <div class="fav-wrap">
          <button class="icon-btn" type="button" aria-label="Favoris" id="favBtn" style="visibility:hidden;pointer-events:none;">
            <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m12 3 2.9 5.9 6.5 1-4.7 4.6 1.1 6.5L12 18l-5.8 3 1.1-6.5L2.6 9.9l6.5-1z"/>
            </svg>
          </button>
          <span class="fav-badge" id="favBadge">0</span>
          <div class="fav-dropdown" id="favDropdown"></div>
        </div>
        <div class="msg-wrap">
          <button class="icon-btn" type="button" aria-label="Messagerie" id="msgBtn">
            <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </button>
          <span class="msg-badge" id="msgBadge">0</span>
          <div class="msg-dropdown" id="msgDropdown"></div>
        </div>
        <div class="notif-wrap">
          <button class="icon-btn" type="button" aria-label="Notifications" id="notifBtn">
            <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5"/>
              <path d="M9.5 17a2.5 2.5 0 0 0 5 0"/>
            </svg>
          </button>
          <span class="notif-badge" id="notifBadge">0</span>
          <div class="notif-dropdown" id="notifDropdown"></div>
        </div>
        <button class="user-menu-btn" type="button" id="userMenuBtn" aria-haspopup="true" aria-expanded="false">
          <span class="avatar" id="navAvatar">U</span>
          <span id="navUserName">Utilisateur</span>
          <span class="caret">v</span>
        </button>
        <div class="user-dropdown" id="userDropdown">
          <button type="button" id="profileBtn">Mon profil</button>
          <button type="button" id="walletBtn" style="display:none;">My wallet</button>
          <button type="button" id="editProfileBtn">Modifier profil</button>
          <button type="button" id="logoutBtn">Se deconnecter</button>
        </div>
      </div>
    </div>
  `;
  document.body.insertBefore(nav, document.body.firstChild);

  const file = (window.location.href.split("#")[0].split("?")[0].split("/").pop() || "");
  const links = Array.from(nav.querySelectorAll(".nav-links a"));
  let activeKey = "";
  if (file === "accueil.html" || file === "accueil_investisseur.html") activeKey = "accueil";
  else if (file === "ajoutProjet.html") activeKey = "projets";
  else if (file === "monProfil.html") activeKey = "profil";
  else if (file === "demandesAnnulation.html") activeKey = "demandes";
  else if (file === "mes_investissements_view.html") activeKey = "mes-investissements";
  else if (file === "projet_view.html" || file === "projet_view_investisseur.html") activeKey = "projets";
  else if (file === "chatbot.html") activeKey = "chatbot";
  else if (file === "contact.html" || file === "contact_investisseur.html") activeKey = "contact";
  else if (file === "detailsEntrepreneur.html" || file === "detailsInvestisseur.html" || file === "businessPlan.html" || file === "modifierProjet.html") activeKey = "projets";
  else if (file === "ajoutEvenement.html" || file === "Modifierevenement.html") activeKey = "evenement";
  else if (file === "financements-list.html" || file === "financement-add.html" || file === "financement-edit.html" || file === "financement-detail.html") activeKey = "financements";
  else if (file === "remboursements-list.html" || file === "remboursements-financement.html" || file === "remboursements-investisseur.html" || file === "remboursements-mes.html" || file === "remboursement-add.html" || file === "remboursement-edit.html" || file === "remboursement-detail.html" || file === "remboursement-pay.html" || file === "remboursement-detail-investisseur.html") activeKey = "remboursements";
  const active = links.find(a => a.dataset.key === activeKey);
  if (active) active.classList.add("active");

  const brandLink = nav.querySelector(".brand");
  const accueilLink = nav.querySelector('a[data-key="accueil"]');
  const demandesLink = nav.querySelector('a[data-key="demandes"]');
  const userMenuBtn = nav.querySelector("#userMenuBtn");
  const favBtn = nav.querySelector("#favBtn");
  const favBadge = nav.querySelector("#favBadge");
  const favDropdown = nav.querySelector("#favDropdown");
  const userDropdown = nav.querySelector("#userDropdown");
  const profileBtn = nav.querySelector("#profileBtn");
  const walletBtn = nav.querySelector("#walletBtn");
  const editProfileBtn = nav.querySelector("#editProfileBtn");
  const logoutBtn = nav.querySelector("#logoutBtn");
  const navUserName = nav.querySelector("#navUserName");
  const navAvatar = nav.querySelector("#navAvatar");
  const notifBtn = nav.querySelector("#notifBtn");
  const notifBadge = nav.querySelector("#notifBadge");
  const notifDropdown = nav.querySelector("#notifDropdown");
  const msgBtn = nav.querySelector("#msgBtn");
  const msgBadge = nav.querySelector("#msgBadge");
  const msgDropdown = nav.querySelector("#msgDropdown");
  const projectsLink = nav.querySelector('a[data-key="projets"]');
  const mesInvestissementsLink = nav.querySelector('a[data-key="mes-investissements"]');
  const evenementLink = nav.querySelector('a[data-key="evenement"]');
  const financementsLink = nav.querySelector('a[data-key="financements"]');
  const remboursementsLink = nav.querySelector('a[data-key="remboursements"]');
  const contactLink = nav.querySelector('a[data-key="contact"]');
  let currentRole = "";
  const FAV_KEY = "investia_favs";
  const getBridge = () => (window.javaBridge || window.navBridge || window.javaBridgeInvest || window.javaBridgeInvestissement || null);
  const setSelectedProjetId = (pid) => {
    try {
      const b = getBridge();
      if (b && typeof b.setSelectedProjetId === "function") {
        b.setSelectedProjetId(String(pid));
      }
    } catch (e) {}
  };

  function applyRoleBasedProjectsLink(role) {
    if (!projectsLink) return;
    if (role === "INVESTISSEUR") {
      if (favBtn) {
        favBtn.style.visibility = "visible";
        favBtn.style.pointerEvents = "auto";
      }
      projectsLink.href = path("projet_view_investisseur.html");
      projectsLink.textContent = "Projets";
      if (mesInvestissementsLink) mesInvestissementsLink.style.display = "";
    } else {
      if (favBtn) {
        favBtn.style.visibility = "hidden";
        favBtn.style.pointerEvents = "none";
      }
      projectsLink.href = path("projet_view.html");
      projectsLink.textContent = "Mes projets";
      if (mesInvestissementsLink) mesInvestissementsLink.style.display = "none";
    }
  }

  function applyRoleBasedNavigation(role) {
    currentRole = role || "";
    applyRoleBasedProjectsLink(role);
    if (role === "INVESTISSEUR") {
      if (brandLink) brandLink.href = path("web/accueil_investisseur.html");
      if (accueilLink) accueilLink.href = path("web/accueil_investisseur.html");
      if (demandesLink) demandesLink.style.display = "none";
      if (evenementLink) evenementLink.style.display = "none";
      if (contactLink) {
        contactLink.style.display = "";
        contactLink.href = path("contact_investisseur.html");
      }
      if (financementsLink) financementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.href = path("web/remboursement/remboursements-investisseur.html");
      if (profileBtn) profileBtn.style.display = "";
      if (walletBtn) walletBtn.style.display = "";
      if (editProfileBtn) editProfileBtn.style.display = "none";
      loadInvestorNotifications();
    } else if (role === "ENTREPRENEUR") {
      if (favBtn) {
        favBtn.style.visibility = "hidden";
        favBtn.style.pointerEvents = "none";
      }
      if (brandLink) brandLink.href = path("projet_view.html");
      if (accueilLink) accueilLink.href = path("accueil.html");
      if (demandesLink) demandesLink.style.display = "";
      if (evenementLink) evenementLink.style.display = "";
      if (contactLink) {
        contactLink.style.display = "";
        contactLink.href = path("contact.html");
      }
      if (financementsLink) financementsLink.style.display = "none";
      if (remboursementsLink) remboursementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.href = path("web/remboursement/remboursements-financement.html");
      if (profileBtn) profileBtn.style.display = "";
      if (walletBtn) walletBtn.style.display = "none";
      if (editProfileBtn) editProfileBtn.style.display = "";
      loadEntrepreneurNotifications();
    } else {
      if (favBtn) {
        favBtn.style.visibility = "hidden";
        favBtn.style.pointerEvents = "none";
      }
      if (brandLink) brandLink.href = path("projet_view.html");
      if (accueilLink) accueilLink.href = path("accueil.html");
      if (demandesLink) demandesLink.style.display = "";
      if (evenementLink) evenementLink.style.display = "none";
      if (contactLink) {
        contactLink.style.display = "";
        contactLink.href = path("contact.html");
      }
      if (financementsLink) financementsLink.style.display = "none";
      if (remboursementsLink) remboursementsLink.style.display = "none";
      if (profileBtn) profileBtn.style.display = "";
      if (walletBtn) walletBtn.style.display = "none";
      if (editProfileBtn) editProfileBtn.style.display = "";
    }
  }

  function applyUserDisplay(name) {
    const cleanName = (name || "").trim();
    const display = cleanName || "Utilisateur";
    if (navUserName) navUserName.textContent = display;
    if (navAvatar) {
      const first = display.charAt(0).toUpperCase();
      navAvatar.textContent = first || "U";
    }
  }

  function refreshMessagesBadge() {
    if (!msgBadge) return;
    const b = getBridge();
    try {
      let count = 0;
      if (b && typeof b.getUnreadMessagesCount === "function") {
        count = Number(b.getUnreadMessagesCount() || "0");
      }
      if ((!Number.isFinite(count) || count <= 0) && b && typeof b.getMessagerieContactsJson === "function") {
        const arr = JSON.parse(b.getMessagerieContactsJson() || "[]");
        count = (Array.isArray(arr) ? arr : []).reduce((s, x) => s + Number(x.unread_count || 0), 0);
      }
      if (count > 0) {
        msgBadge.textContent = count > 99 ? "99+" : String(count);
        msgBadge.style.display = "inline-flex";
      } else {
        msgBadge.style.display = "none";
      }
    } catch (e) {
      msgBadge.style.display = "none";
    }
  }

  function scheduleMessagesBadgeBoot() {
    // Bridge Java peut arriver aprÃ¨s le rendu initial de la navbar.
    const delays = [150, 400, 800, 1500, 2500];
    delays.forEach((ms) => {
      setTimeout(() => {
        refreshMessagesBadge();
      }, ms);
    });
  }

  function loadMessagesEnum(limit = 8) {
    if (!msgDropdown) return;
    const b = getBridge();
    if (!b || typeof b.getMessagerieContactsJson !== "function") {
      msgDropdown.innerHTML = `<div class="notif-empty">Aucun message.</div>`;
      return;
    }
    try {
      const all = JSON.parse(b.getMessagerieContactsJson() || "[]");
      const unread = (Array.isArray(all) ? all : [])
        .filter(x => Number(x.unread_count || 0) > 0)
        .slice(0, Math.max(1, limit));

      if (!unread.length) {
        msgDropdown.innerHTML = `<div class="notif-empty">Aucun nouveau message.</div>`;
        return;
      }

      msgDropdown.innerHTML = unread.map((it) => {
        const name = it.nom || "Contact";
        const count = Number(it.unread_count || 0);
        const preview = it.last_message || "";
        const uid = Number(it.user_id || 0);
        return `<div class="msg-item" data-user="${uid}">
          ${name} (${count})<br><span class="muted">${preview}</span>
        </div>`;
      }).join("");

      Array.from(msgDropdown.querySelectorAll(".msg-item")).forEach(el => {
        el.addEventListener("click", () => {
          const uid = Number(el.getAttribute("data-user") || 0);
          const b = getBridge();
          if (uid > 0 && b && typeof b.setSelectedContactUserId === "function") {
            try { b.setSelectedContactUserId(String(uid)); } catch (e) {}
          }
          window.location.href = path("messagerie.html");
        });
      });
    } catch (e) {
      msgDropdown.innerHTML = `<div class="notif-empty">Aucun nouveau message.</div>`;
    }
  }

  function loadEntrepreneurNotifications(limit = 5) {
    if (!notifBadge || !notifDropdown) return;
    const b = getBridge();
    if (!b || typeof b.getEntrepreneurInvestmentNotificationsJson !== "function") {
      notifBadge.style.display = "none";
      notifDropdown.innerHTML = "";
      return;
    }
    try {
      const unreadJson = b.getEntrepreneurInvestmentNotificationsJson();
      const unreadItems = JSON.parse(unreadJson || "[]");
      if (Array.isArray(unreadItems) && unreadItems.length > 0) {
        notifBadge.textContent = String(unreadItems.length);
        notifBadge.style.display = "inline-flex";
      } else {
        notifBadge.style.display = "none";
      }

      if (typeof b.getEntrepreneurNotificationHistoryJson !== "function") {
        notifDropdown.innerHTML = `<div class="notif-empty">Aucune notification.</div>`;
        return;
      }
      const historyJson = b.getEntrepreneurNotificationHistoryJson(String(limit));
      const payload = JSON.parse(historyJson || "{}");
      const items = Array.isArray(payload.items) ? payload.items : [];
      const total = Number(payload.total || items.length || 0);

      if (!items.length) {
        notifDropdown.innerHTML = `<div class="notif-empty">Aucune notification.</div>`;
        return;
      }
      notifDropdown.innerHTML = items.map(it => {
        const title = it.titre || "Projet";
        const date = it.date || "";
        const notifId = it.id;
        const unreadCls = String(it.is_read || "0") === "0" ? " unread" : "";
        return `<div class="notif-item${unreadCls}" data-id="${it.id_projet}" data-notif="${notifId}">
          Nouveau investissement pour ${title}<br><span class="muted">${date}</span>
        </div>`;
      }).join("");

      notifDropdown.innerHTML += `<div class="notif-footer"><button type="button" id="notifViewAll">Voir tout</button></div>`;

      Array.from(notifDropdown.querySelectorAll(".notif-item")).forEach(el => {
        el.addEventListener("click", () => {
          const pid = el.getAttribute("data-id");
          const nid = el.getAttribute("data-notif");
          const b = getBridge();
          if (nid && b && typeof b.markEntrepreneurNotificationRead === "function") {
            try { b.markEntrepreneurNotificationRead(String(nid)); } catch (e) {}
          }
          if (pid) window.location.href = path("detailsEntrepreneur_encours.html?id=" + encodeURIComponent(pid));
        });
      });

      const viewAll = notifDropdown.querySelector("#notifViewAll");
      if (viewAll) {
        viewAll.addEventListener("click", () => {
          window.location.href = path("notifications_entrepreneur.html");
        });
      }
    } catch (e) {
      notifBadge.style.display = "none";
      notifDropdown.innerHTML = "";
    }
  }

  function loadInvestorNotifications(limit = 5) {
    if (!notifBadge || !notifDropdown) return;
    const b = getBridge();
    if (!b || typeof b.getInvestorProjectNotificationsJson !== "function") {
      notifBadge.style.display = "none";
      notifDropdown.innerHTML = "";
      return;
    }
    try {
      const unreadJson = b.getInvestorProjectNotificationsJson();
      const unreadItems = JSON.parse(unreadJson || "[]");
      if (Array.isArray(unreadItems) && unreadItems.length > 0) {
        notifBadge.textContent = String(unreadItems.length);
        notifBadge.style.display = "inline-flex";
      } else {
        notifBadge.style.display = "none";
      }

      if (typeof b.getInvestorNotificationHistoryJson !== "function") {
        notifDropdown.innerHTML = `<div class="notif-empty">Aucune notification.</div>`;
        return;
      }
      const historyJson = b.getInvestorNotificationHistoryJson(String(limit));
      const payload = JSON.parse(historyJson || "{}");
      const items = Array.isArray(payload.items) ? payload.items : [];

      if (!items.length) {
        notifDropdown.innerHTML = `<div class="notif-empty">Aucune notification.</div>`;
        return;
      }
      notifDropdown.innerHTML = items.map(it => {
        const title = it.titre || "Projet";
        const date = it.date || "";
        const notifId = it.id;
        const unreadCls = String(it.is_read || "0") === "0" ? " unread" : "";
        return `<div class="notif-item${unreadCls}" data-id="${it.id_projet}" data-notif="${notifId}">
          Nouvel evenement sur projet: ${title}<br><span class="muted">${date}</span>
        </div>`;
      }).join("");

      notifDropdown.innerHTML += `<div class="notif-footer"><button type="button" id="notifViewAll">Voir tout</button></div>`;

      Array.from(notifDropdown.querySelectorAll(".notif-item")).forEach(el => {
        el.addEventListener("click", () => {
          const pid = el.getAttribute("data-id");
          const nid = el.getAttribute("data-notif");
          const b = getBridge();
          if (nid && b && typeof b.markInvestorNotificationRead === "function") {
            try { b.markInvestorNotificationRead(String(nid)); } catch (e) {}
          }
          if (pid) {
            setSelectedProjetId(pid);
            window.location.href = path("detailsInvestisseur.html?id=" + encodeURIComponent(pid));
          }
        });
      });

      const viewAll = notifDropdown.querySelector("#notifViewAll");
      if (viewAll) {
        viewAll.addEventListener("click", () => {
          window.location.href = path("notifications_investisseur.html");
        });
      }
    } catch (e) {
      notifBadge.style.display = "none";
      notifDropdown.innerHTML = "";
    }
  }

  function loadCurrentUserName(retry = 0) {
    try {
      if (window.javaBridge && typeof window.javaBridge.getCurrentUserName === "function") {
        applyUserDisplay(window.javaBridge.getCurrentUserName());
        if (typeof window.javaBridge.getCurrentUserRole === "function") {
          applyRoleBasedNavigation(window.javaBridge.getCurrentUserRole());
        } else {
          applyRoleBasedNavigation("");
        }
        return;
      }
    } catch (e) {
      // ignore and fallback below
    }

    if (retry < 10) {
      setTimeout(() => loadCurrentUserName(retry + 1), 120);
    } else {
      applyUserDisplay("");
    }
  }

  loadCurrentUserName();
  scheduleMessagesBadgeBoot();
  window.__refreshNavbarUser = () => loadCurrentUserName(0);
  window.addEventListener("investia:user-updated", () => {
    loadCurrentUserName(0);
    refreshMessagesBadge();
    refreshFavBadge();
  });

  if (userMenuBtn && userDropdown) {
    userMenuBtn.addEventListener("click", () => {
      const opened = userDropdown.classList.toggle("open");
      userMenuBtn.setAttribute("aria-expanded", opened ? "true" : "false");
    });

    document.addEventListener("click", (e) => {
      if (!nav.contains(e.target)) {
        userDropdown.classList.remove("open");
        userMenuBtn.setAttribute("aria-expanded", "false");
      }
    });
  }

  if (notifBtn && notifDropdown) {
    notifBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      const opened = notifDropdown.classList.toggle("open");
      if (opened && userDropdown) {
        userDropdown.classList.remove("open");
      }
    });
    document.addEventListener("click", (e) => {
      if (!nav.contains(e.target)) {
        notifDropdown.classList.remove("open");
      }
    });
  }

  if (msgBtn) {
    msgBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      if (!msgDropdown) {
        window.location.href = path("messagerie.html");
        return;
      }
      const opened = msgDropdown.classList.toggle("open");
      if (opened) {
        loadMessagesEnum();
        if (notifDropdown) notifDropdown.classList.remove("open");
        if (userDropdown) userDropdown.classList.remove("open");
      }
    });
  }
  document.addEventListener("click", (e) => {
    if (!nav.contains(e.target) && msgDropdown) {
      msgDropdown.classList.remove("open");
    }
  });

  refreshMessagesBadge();
  refreshFavBadge();
  setInterval(refreshMessagesBadge, 5000);
  setInterval(refreshFavBadge, 2000);

  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      try {
        if (window.javaBridge && typeof window.javaBridge.logout === "function") {
          const res = window.javaBridge.logout();
          if (res === "OK") return;
        }
      } catch (e) {
        // fallback web redirect
      }
      window.location.href = path("html/login.html");
    });
  }

  function hookInvestorNav(linkEl, fnName, fallbackHref) {
    if (!linkEl) return;
    linkEl.addEventListener("click", (e) => {
      if (currentRole && currentRole !== "INVESTISSEUR") return;
      const b = getBridge();
      if (b && typeof b[fnName] === "function") {
        e.preventDefault();
        b[fnName]();
        return;
      }
      if (fallbackHref) {
        e.preventDefault();
        window.location.href = fallbackHref;
      }
    });
  }

  function hookEntrepreneurNav(linkEl, fnName, fallbackHref) {
    if (!linkEl) return;
    linkEl.addEventListener("click", (e) => {
      if (currentRole !== "ENTREPRENEUR") return;
      const b = getBridge();
      if (b && typeof b[fnName] === "function") {
        e.preventDefault();
        b[fnName]();
        return;
      }
      if (fallbackHref) {
        e.preventDefault();
        window.location.href = fallbackHref;
      }
    });
  }

  hookInvestorNav(brandLink, "goAccueilInvestisseur", path("web/accueil_investisseur.html"));
  hookInvestorNav(accueilLink, "goAccueilInvestisseur", path("web/accueil_investisseur.html"));
  hookInvestorNav(projectsLink, "openProjetsInvestisseur", path("projet_view_investisseur.html"));
  hookInvestorNav(mesInvestissementsLink, "openMesInvestissements", path("web/mes_investissements_view.html"));
  hookInvestorNav(contactLink, "openContactInvestisseur", path("contact_investisseur.html"));

  if (favBtn) {
    favBtn.addEventListener("click", () => {
      try {
        if (currentRole === "INVESTISSEUR") {
          if (!favDropdown) {
            window.location.href = path("favoris_investisseurs.html");
            return;
          }
          const opened = favDropdown.classList.toggle("open");
          if (opened) {
            renderFavDropdown();
            if (msgDropdown) msgDropdown.classList.remove("open");
            if (notifDropdown) notifDropdown.classList.remove("open");
            if (userDropdown) userDropdown.classList.remove("open");
          }
        }
      } catch (e) {}
    });
  }
  document.addEventListener("click", (e) => {
    if (!nav.contains(e.target) && favDropdown) {
      favDropdown.classList.remove("open");
    }
  });

  refreshFavBadge();

  function readFavItems() {
    try {
      const arr = JSON.parse(localStorage.getItem(FAV_KEY) || "[]");
      return Array.isArray(arr) ? arr.filter(x => x && typeof x === "object") : [];
    } catch (e) {
      return [];
    }
  }

  function refreshFavBadge() {
    if (!favBadge) return;
    favBadge.textContent = "";
    favBadge.style.display = "none";
  }

  function renderFavDropdown(limit = 8) {
    if (!favDropdown) return;
    const favs = readFavItems();
    if (!favs.length) {
      favDropdown.innerHTML = `<div class="notif-empty">Aucun favori.</div><div class="fav-footer"><button type="button" id="favViewAll">Voir tout</button></div>`;
      const viewBtn = favDropdown.querySelector("#favViewAll");
      if (viewBtn) viewBtn.addEventListener("click", () => { window.location.href = path("favoris_investisseurs.html"); });
      return;
    }
    const top = favs.slice(0, Math.max(1, limit));
    favDropdown.innerHTML = top.map((item, idx) => {
      const title = item.title || "Article";
      const source = item.source || "";
      return `<div class="fav-item" data-idx="${idx}">${title}<br><span class="muted">${source}</span></div>`;
    }).join("");
    favDropdown.innerHTML += `<div class="fav-footer"><button type="button" id="favClear">Vider</button><button type="button" id="favViewAll">Voir tout</button></div>`;

    Array.from(favDropdown.querySelectorAll(".fav-item")).forEach(el => {
      el.addEventListener("click", () => {
        const idx = Number(el.getAttribute("data-idx") || -1);
        const item = top[idx];
        const url = item && item.url ? String(item.url) : "";
        if (url) window.location.href = url;
      });
    });
    const clearBtn = favDropdown.querySelector("#favClear");
    if (clearBtn) {
      clearBtn.addEventListener("click", () => {
        try { localStorage.setItem(FAV_KEY, "[]"); } catch (e) {}
        refreshFavBadge();
        renderFavDropdown(limit);
      });
    }
    const viewBtn = favDropdown.querySelector("#favViewAll");
    if (viewBtn) viewBtn.addEventListener("click", () => { window.location.href = path("favoris_investisseurs.html"); });
  }
  hookEntrepreneurNav(projectsLink, "openProjetsEntrepreneur", path("projet_view.html"));
  hookEntrepreneurNav(contactLink, "openContactEntrepreneur", path("contact.html"));

  if (evenementLink) {
    evenementLink.addEventListener("click", (e) => {
      if (currentRole !== "ENTREPRENEUR") return;
      const b = getBridge();
      if (b && typeof b.goEvenements === "function") {
        e.preventDefault();
        b.goEvenements();
        return;
      }
    });
  }


  if (profileBtn) {
    profileBtn.addEventListener("click", () => {
      try {
        if (currentRole === "INVESTISSEUR") {
          if (window.javaBridge && typeof window.javaBridge.openProfilInvestisseur === "function") {
            window.javaBridge.openProfilInvestisseur();
            return;
          }
          window.location.href = path("monProfil_investisseur.html");
          return;
        }
      } catch (e) {}
      window.location.href = path("monProfil.html");
    });
  }

  if (editProfileBtn) {
    editProfileBtn.addEventListener("click", () => {
      try {
        if (currentRole === "INVESTISSEUR") {
          if (window.javaBridge && typeof window.javaBridge.openEditProfilInvestisseur === "function") {
            window.javaBridge.openEditProfilInvestisseur();
            return;
          }
          window.location.href = path("profil_investisseur_edit.html");
          return;
        }
      } catch (e) {}
      try {
        sessionStorage.setItem("openProfileEdit", "1");
      } catch (e) {}
      if (file === "monProfil.html") {
        const editBtn = document.getElementById("editBtn");
        if (editBtn) editBtn.click();
      } else {
        window.location.href = path("monProfil.html");
      }
    });
  }

  if (walletBtn) {
    walletBtn.addEventListener("click", () => {
      try {
        if (currentRole === "INVESTISSEUR") {
          const b = getBridge();
          if (b && typeof b.openWalletInvestisseur === "function") {
            b.openWalletInvestisseur();
            return;
          }
          window.location.href = path("web/wallet_investisseur.html");
        }
      } catch (e) {}
    });
  }
})();



