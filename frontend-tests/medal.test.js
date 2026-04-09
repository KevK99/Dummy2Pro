const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("medal.js", () => {
    function setupDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="medalContainer" class="hidden">
                        <span id="medalCoin" class="hidden"></span>
                    </div>
                </body>
            </html>
        `);

        loadBrowserScript(dom, "src/main/resources/static/js/medal.js");
        return dom;
    }

    test("NONE versteckt Medaillenanzeige", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("NONE");

        const container = dom.window.document.getElementById("medalContainer");
        const coin = dom.window.document.getElementById("medalCoin");

        expect(container.classList.contains("hidden")).toBe(true);
        expect(coin.classList.contains("hidden")).toBe(true);
        expect(coin.textContent).toBe("");
        expect(coin.getAttribute("aria-label")).toBe("Medaille");
    });

    test("BRONZE zeigt Bronze-Medaille", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("BRONZE");

        const container = dom.window.document.getElementById("medalContainer");
        const coin = dom.window.document.getElementById("medalCoin");

        expect(container.classList.contains("hidden")).toBe(false);
        expect(coin.classList.contains("hidden")).toBe(false);
        expect(coin.textContent).toBe("🥉");
        expect(coin.getAttribute("aria-label")).toBe("Bronze Medaille");
    });

    test("SILVER zeigt Silber-Medaille", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("SILVER");

        const coin = dom.window.document.getElementById("medalCoin");

        expect(coin.textContent).toBe("🥈");
        expect(coin.getAttribute("aria-label")).toBe("Silber Medaille");
    });

    test("GOLD zeigt Gold-Medaille", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("GOLD");

        const coin = dom.window.document.getElementById("medalCoin");

        expect(coin.textContent).toBe("🥇");
        expect(coin.getAttribute("aria-label")).toBe("Gold Medaille");
    });

    test("unbekannter Wert ändert nichts sichtbar", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("PLATIN");

        const container = dom.window.document.getElementById("medalContainer");
        const coin = dom.window.document.getElementById("medalCoin");

        expect(container.classList.contains("hidden")).toBe(true);
        expect(coin.classList.contains("hidden")).toBe(true);
        expect(coin.textContent).toBe("");
    });
});