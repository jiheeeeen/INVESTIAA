window.Bridge = {
    goLogin() {
        if (window.app && typeof window.app.goLogin === "function") {
            window.app.goLogin();
        }
    },

    async register(payload) {
        if (!window.app || typeof window.app.register !== "function") return "NO_BRIDGE";
        return window.app.register(payload.fullName, payload.email, payload.password, payload.role);
    }
};
