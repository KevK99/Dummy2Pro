const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("darkmode.js", () => {
    function setupDom() {
        return createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <button id="darkmodeBtn"></button>
                </body>
            </html>
        `);
    }

    test("setzt Darkmode aus localStorage beim Laden", () => {
        const dom = setupDom();
        dom.window.localStorage.setItem("theme", "dark");

        loadBrowserScript(dom, "src/main/resources/static/js/darkmode.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        expect(dom.window.document.documentElement.classList.contains("dark")).toBe(true);
        expect(dom.window.document.getElementById("darkmodeBtn").textContent).toBe("Light Mode ☀️");
    });

    test("setzt Lightmode beim Laden, wenn theme nicht dark ist", () => {
        const dom = setupDom();
        dom.window.localStorage.setItem("theme", "light");
        dom.window.document.documentElement.classList.add("dark");

        loadBrowserScript(dom, "src/main/resources/static/js/darkmode.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        expect(dom.window.document.documentElement.classList.contains("dark")).toBe(false);
        expect(dom.window.document.getElementById("darkmodeBtn").textContent)
            .toBe("Hello Dark Mode, my old friend 🌙");
    });

    test("Button toggelt von hell auf dunkel", () => {
        const dom = setupDom();

        loadBrowserScript(dom, "src/main/resources/static/js/darkmode.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        const button = dom.window.document.getElementById("darkmodeBtn");
        button.click();

        expect(dom.window.document.documentElement.classList.contains("dark")).toBe(true);
        expect(dom.window.localStorage.getItem("theme")).toBe("dark");
        expect(button.textContent).toBe("Light Mode ☀️");
    });

    test("Button toggelt von dunkel auf hell", () => {
        const dom = setupDom();
        dom.window.localStorage.setItem("theme", "dark");

        loadBrowserScript(dom, "src/main/resources/static/js/darkmode.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        const button = dom.window.document.getElementById("darkmodeBtn");
        button.click();

        expect(dom.window.document.documentElement.classList.contains("dark")).toBe(false);
        expect(dom.window.localStorage.getItem("theme")).toBe("light");
        expect(button.textContent).toBe("Hello Dark Mode, my old friend 🌙");
    });
});