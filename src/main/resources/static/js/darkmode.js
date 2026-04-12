document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("darkmodeBtn");
    const html = document.documentElement;

    if (!button) {
        return;
    }

    // Der Buttontext richtet sich ausschließlich nach der aktiven HTML-Klasse,
    // damit Anzeige und tatsächlicher Zustand immer zusammenpassen.
    function updateButtonText() {
        if (html.classList.contains("dark")) {
            button.textContent = "Light Mode ☀️";
        } else {
            button.textContent = "Hello Dark Mode, my old friend 🌙";
        }
    }

    // Beim Laden wird nur der gespeicherte Theme-Zustand wiederhergestellt.
    if (localStorage.theme === "dark") {
        html.classList.add("dark");
    } else {
        html.classList.remove("dark");
    }

    updateButtonText();

    button.addEventListener("click", () => {
        if (html.classList.contains("dark")) {
            html.classList.remove("dark");
            localStorage.theme = "light";
        } else {
            html.classList.add("dark");
            localStorage.theme = "dark";
        }

        updateButtonText();
    });
});