const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("ui-feedback.js", () => {
    // Lädt das produktive Skript in eine kleine DOM-Struktur mit einem
    // vorbereiteten Inline-Ziel für Meldungen im Formular-Kontext.
    function setupDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="inlineTarget" class="hidden"></div>
                </body>
            </html>
        `);

        loadBrowserScript(dom, "src/main/resources/static/js/ui-feedback.js");
        return dom;
    }

    test("alert erstellt globale Meldung, leitet Typ korrekt her und escaped HTML", () => {
        const dom = setupDom();

        dom.window.Dummy2ProUI.alert("Erfolgreich gespeichert <b>Test</b>");

        const container = dom.window.document.getElementById("dummy2pro-global-feedback");
        expect(container).not.toBeNull();
        expect(container.textContent).toContain("Erfolg");
        expect(container.innerHTML).toContain("&lt;b&gt;Test&lt;/b&gt;");
        expect(container.innerHTML).not.toContain("<b>Test</b>");
    });

    test("setInlineMessage zeigt Meldung an und clearInlineMessage entfernt sie wieder", () => {
        const dom = setupDom();
        const target = dom.window.document.getElementById("inlineTarget");

        dom.window.Dummy2ProUI.setInlineMessage(target, "Bitte Eingabe prüfen");

        expect(target.classList.contains("hidden")).toBe(false);
        expect(target.textContent).toContain("Hinweis");
        expect(target.textContent).toContain("Bitte Eingabe prüfen");

        dom.window.Dummy2ProUI.clearInlineMessage(target);

        expect(target.className).toBe("hidden");
        expect(target.innerHTML).toBe("");
    });

    test("confirm löst true aus, wenn Bestätigen geklickt wird", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.confirm("Wirklich fortfahren?");
        dom.window.document.querySelector('[data-action="confirm"]').click();

        await expect(promise).resolves.toBe(true);

        const modalRoot = dom.window.document.getElementById("dummy2pro-modal-root");
        expect(modalRoot.classList.contains("hidden")).toBe(true);
        expect(dom.window.document.body.classList.contains("overflow-hidden")).toBe(false);
    });

    test("prompt gibt den eingegebenen Wert zurück", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.prompt("Neuen Namen eingeben", "Alt");
        const input = dom.window.document.querySelector('[data-role="prompt-input"]');

        input.value = "NeuerName";
        dom.window.document.querySelector('[data-action="confirm"]').click();

        await expect(promise).resolves.toBe("NeuerName");
    });

    test("notice schließt per OK", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.notice("Gespeichert");

        dom.window.document.querySelector('[data-action="ok"]').click();

        await expect(promise).resolves.toBe(true);
    });
});