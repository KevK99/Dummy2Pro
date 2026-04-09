const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

function setupDom() {
    const dom = createBrowserEnv(`
        <!doctype html>
        <html>
            <body>
                <div id="inlineTarget" class="hidden"></div>
            </body>
        </html>
    `);

    dom.window.requestAnimationFrame = callback => callback();
    loadBrowserScript(dom, "src/main/resources/static/js/ui-feedback.js");
    return dom;
}

describe("ui-feedback.js zusätzliche Pfade", () => {
    test("globale Meldung nutzt Default-Fehlertyp, ersetzt alte Meldung und entfernt sich automatisch", () => {
        jest.useFakeTimers();
        const dom = setupDom();

        dom.window.Dummy2ProUI.success("Erfolgreich gespeichert", { persistent: true });
        const second = dom.window.Dummy2ProUI.alert("Neutraler Text", { duration: 10 });

        const container = dom.window.document.getElementById("dummy2pro-global-feedback");
        expect(container.children.length).toBe(1);
        expect(container.textContent).toContain("Fehler");
        expect(second.classList.contains("opacity-0")).toBe(false);
        expect(second.classList.contains("translate-y-3")).toBe(false);

        jest.advanceTimersByTime(10);
        expect(second.classList.contains("opacity-0")).toBe(true);
        expect(second.classList.contains("translate-y-3")).toBe(true);

        jest.advanceTimersByTime(220);
        expect(container.children.length).toBe(0);
    });

    test("setInlineMessage per ID nutzt expliziten Typ und clearInlineMessage ignoriert fehlende Ziele", () => {
        const dom = setupDom();

        const result = dom.window.Dummy2ProUI.setInlineMessage("inlineTarget", "Alles gut", "success");

        expect(result).toBe(dom.window.document.getElementById("inlineTarget"));
        expect(result.textContent).toContain("Erfolg");
        expect(result.className).toContain("emerald");

        expect(() => dom.window.Dummy2ProUI.clearInlineMessage("gibt-es-nicht")).not.toThrow();
    });

    test("confirm nutzt danger-Variante und benutzerdefinierte Texte", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.confirm("Endgültig löschen?", {
            title: "Vorsicht",
            confirmText: "Ja, löschen",
            cancelText: "Nein",
            variant: "danger"
        });

        const modalRoot = dom.window.document.getElementById("dummy2pro-modal-root");
        expect(modalRoot.textContent).toContain("Vorsicht");
        expect(modalRoot.textContent).toContain("Ja, löschen");
        expect(modalRoot.textContent).toContain("Nein");
        expect(modalRoot.innerHTML).toContain("from-rose-600 to-red-500");
        expect(modalRoot.innerHTML).toContain(">!<");

        modalRoot.firstElementChild.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));
        expect(modalRoot.classList.contains("flex")).toBe(true);

        modalRoot.querySelector('[data-action="confirm"]').click();
        await expect(promise).resolves.toBe(true);
    });

    test("prompt nutzt benutzerdefinierte Optionen und Overlay-Klick gibt null zurück", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.prompt("Name ändern", "Alt", {
            title: "Benennen",
            confirmText: "Übernehmen",
            cancelText: "Abbruch",
            placeholder: "Neuer Name"
        });

        const modalRoot = dom.window.document.getElementById("dummy2pro-modal-root");
        const input = modalRoot.querySelector('[data-role="prompt-input"]');

        expect(modalRoot.textContent).toContain("Benennen");
        expect(modalRoot.textContent).toContain("Übernehmen");
        expect(modalRoot.textContent).toContain("Abbruch");
        expect(input.getAttribute("placeholder")).toBe("Neuer Name");
        expect(input.value).toBe("Alt");

        modalRoot.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));
        await expect(promise).resolves.toBeNull();
    });

    test("prompt mit Escape gibt null zurück", async () => {
        const dom = setupDom();

        const promise = dom.window.Dummy2ProUI.prompt("Noch mal?");
        dom.window.document.dispatchEvent(new dom.window.KeyboardEvent("keydown", { key: "Escape" }));

        await expect(promise).resolves.toBeNull();
    });

    test("notice deckt warning- und error-Varianten mit Custom-Texten ab", async () => {
        const dom = setupDom();

        const warningPromise = dom.window.Dummy2ProUI.notice("Bitte aufpassen", {
            title: "Achtung",
            okText: "Verstanden",
            type: "warning"
        });

        let modalRoot = dom.window.document.getElementById("dummy2pro-modal-root");
        expect(modalRoot.textContent).toContain("Achtung");
        expect(modalRoot.textContent).toContain("Verstanden");
        expect(modalRoot.innerHTML).toContain("from-amber-500 to-yellow-400");

        modalRoot.querySelector('[data-action="ok"]').click();
        await expect(warningPromise).resolves.toBe(true);

        const errorPromise = dom.window.Dummy2ProUI.notice("Kaputt", {
            title: "Fehlerfall",
            type: "error"
        });

        modalRoot = dom.window.document.getElementById("dummy2pro-modal-root");
        expect(modalRoot.textContent).toContain("Fehlerfall");
        expect(modalRoot.innerHTML).toContain("from-rose-600 to-red-500");

        modalRoot.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));
        await expect(errorPromise).resolves.toBe(true);
    });
});