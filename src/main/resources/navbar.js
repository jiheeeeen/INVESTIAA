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
        <a href="${path("demandesAnnulation.html")}" data-key="demandes">Mes demandes</a>
        <a href="${path("web/financement/financements-list.html")}" data-key="financements" style="display:none;">Financements</a>
        <a href="${path("web/remboursement/remboursements-list.html")}" data-key="remboursements" style="display:none;">Remboursements</a>
      </div>
      <div class="nav-user">
        <button class="icon-btn" type="button" aria-label="Notifications">
          <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5"/>
            <path d="M9.5 17a2.5 2.5 0 0 0 5 0"/>
          </svg>
        </button>
        <button class="user-menu-btn" type="button" id="userMenuBtn" aria-haspopup="true" aria-expanded="false">
          <span class="avatar" id="navAvatar">U</span>
          <span id="navUserName">Utilisateur</span>
          <span class="caret">v</span>
        </button>
        <div class="user-dropdown" id="userDropdown">
          <button type="button" id="profileBtn">Mon profil</button>
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
  else if (file === "projet_view.html" || file === "projet_view_investisseur.html") activeKey = "projets";
  else if (file === "detailsEntrepreneur.html" || file === "detailsInvestisseur.html" || file === "businessPlan.html" || file === "modifierProjet.html") activeKey = "projets";
  else if (file === "financements-list.html" || file === "financement-add.html" || file === "financement-edit.html" || file === "financement-detail.html") activeKey = "financements";
  else if (file === "remboursements-list.html" || file === "remboursements-financement.html" || file === "remboursements-investisseur.html" || file === "remboursements-mes.html" || file === "remboursement-add.html" || file === "remboursement-edit.html" || file === "remboursement-detail.html" || file === "remboursement-pay.html" || file === "remboursement-detail-investisseur.html") activeKey = "remboursements";
  const active = links.find(a => a.dataset.key === activeKey);
  if (active) active.classList.add("active");

  const brandLink = nav.querySelector(".brand");
  const accueilLink = nav.querySelector('a[data-key="accueil"]');
  const demandesLink = nav.querySelector('a[data-key="demandes"]');
  const userMenuBtn = nav.querySelector("#userMenuBtn");
  const userDropdown = nav.querySelector("#userDropdown");
  const profileBtn = nav.querySelector("#profileBtn");
  const editProfileBtn = nav.querySelector("#editProfileBtn");
  const logoutBtn = nav.querySelector("#logoutBtn");
  const navUserName = nav.querySelector("#navUserName");
  const navAvatar = nav.querySelector("#navAvatar");
  const projectsLink = nav.querySelector('a[data-key="projets"]');
  const financementsLink = nav.querySelector('a[data-key="financements"]');
  const remboursementsLink = nav.querySelector('a[data-key="remboursements"]');
  let currentRole = "";
  const getBridge = () => (window.javaBridge || window.javaBridgeInvest || window.javaBridgeInvestissement || null);

  function applyRoleBasedProjectsLink(role) {
    if (!projectsLink) return;
    if (role === "INVESTISSEUR") {
      projectsLink.href = path("projet_view_investisseur.html");
      projectsLink.textContent = "Projets";
    } else {
      projectsLink.href = path("projet_view.html");
      projectsLink.textContent = "Mes projets";
    }
  }

  function applyRoleBasedNavigation(role) {
    currentRole = role || "";
    applyRoleBasedProjectsLink(role);
    if (role === "INVESTISSEUR") {
      if (brandLink) brandLink.href = path("accueil_investisseur.html");
      if (accueilLink) accueilLink.href = path("accueil_investisseur.html");
      if (demandesLink) demandesLink.style.display = "none";
      if (financementsLink) financementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.href = path("web/remboursement/remboursements-investisseur.html");
      if (profileBtn) profileBtn.style.display = "";
      if (editProfileBtn) editProfileBtn.style.display = "";
    } else if (role === "ENTREPRENEUR") {
      if (brandLink) brandLink.href = path("projet_view.html");
      if (accueilLink) accueilLink.href = path("accueil.html");
      if (demandesLink) demandesLink.style.display = "";
      if (financementsLink) financementsLink.style.display = "none";
      if (remboursementsLink) remboursementsLink.style.display = "";
      if (remboursementsLink) remboursementsLink.href = path("web/remboursement/remboursements-financement.html");
      if (profileBtn) profileBtn.style.display = "";
      if (editProfileBtn) editProfileBtn.style.display = "";
    } else {
      if (brandLink) brandLink.href = path("projet_view.html");
      if (accueilLink) accueilLink.href = path("accueil.html");
      if (demandesLink) demandesLink.style.display = "";
      if (financementsLink) financementsLink.style.display = "none";
      if (remboursementsLink) remboursementsLink.style.display = "none";
      if (profileBtn) profileBtn.style.display = "";
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
  window.__refreshNavbarUser = () => loadCurrentUserName(0);
  window.addEventListener("investia:user-updated", () => loadCurrentUserName(0));

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
      if (currentRole !== "INVESTISSEUR") return;
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

  hookInvestorNav(brandLink, "goAccueilInvestisseur", path("accueil_investisseur.html"));
  hookInvestorNav(accueilLink, "goAccueilInvestisseur", path("accueil_investisseur.html"));
  hookInvestorNav(projectsLink, "openProjetsInvestisseur", path("projet_view_investisseur.html"));

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
})();
