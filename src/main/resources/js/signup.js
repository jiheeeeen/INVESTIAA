(() => {
    const form = document.getElementById("signupForm");
    const errorBox = document.getElementById("errorBox");
    const okBox = document.getElementById("okBox");
    const goLoginLink = document.getElementById("goLoginLink");

    function showError(msg) {
        errorBox.textContent = msg;
        errorBox.style.display = "block";
        okBox.style.display = "none";
    }

    function showOk(msg) {
        okBox.textContent = msg;
        okBox.style.display = "block";
        errorBox.style.display = "none";
    }

    function getBridge() {
        if (window.app) return window.app;
        if (window.Bridge) return window.Bridge;
        return null;
    }


    function goLogin() {
        const bridge = getBridge();
        if (bridge && typeof bridge.goLogin === "function") {
            bridge.goLogin();
        } else {
            showError("Bridge JavaFX non disponible (goLogin).");
        }
    }

    if (goLoginLink) {
        goLoginLink.addEventListener("click", (e) => {
            e.preventDefault();
            goLogin();
        });
    }

    form.addEventListener("submit", (e) => {

        e.preventDefault();

        errorBox.style.display = "none";
        okBox.style.display = "none";

        if (!form.checkValidity()) {
            showError("Veuillez remplir correctement tous les champs obligatoires.");
            return;
        }

        const fullName = document.getElementById("fullName").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const email = document.getElementById("email").value.trim().toLowerCase();
        const cin = document.getElementById("cin").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const role = document.getElementById("role").value;
        const terms = document.getElementById("terms").checked;
        const faceTemplate = (document.getElementById("faceTemplate")?.value || "").trim();

        if (!terms) {
            showError("Veuillez accepter les conditions d'utilisation.");
            return;
        }

        if (password !== confirmPassword) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        const bridge = getBridge();
        if (!bridge || typeof bridge.register !== "function") {
            showError("Bridge JavaFX non disponible (register).");
            return;
        }

        const res = bridge.register(fullName, email, password, role, phone, cin,faceTemplate);

        if (res === "OK") {
            showOk("Compte cree. Connectez-vous pour completer vos informations.");
            setTimeout(() => goLogin(), 1000);
        } else if (res === "EMAIL_EXISTS") {
            showError("Cet email est deja utilise.");
        } else if (res === "ROLE_INVALID") {
            showError("Role invalide.");
        } else {
            showError("Erreur: " + res);
        }
    });

})();
