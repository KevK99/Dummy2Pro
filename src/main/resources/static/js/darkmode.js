document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("darkmodeBtn");
    const html = document.documentElement;

    if (!button) {
        return;
    }

    function updateButtonText() {
        if (html.classList.contains("dark")) {
            button.textContent = "Light Mode ☀️";
        } else {
            button.textContent = "Hello Dark Mode, my old friend 🌙";
        }
    }

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