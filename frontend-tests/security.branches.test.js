const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("security.js weitere Branches", () => {
    // Lädt security.js mit einem kontrollierten Fetch-Mock, damit die
    // Wrapper- und Retry-Logik gegen echte Browser-Globals getestet wird.
    function setupDom(fetchImpl = jest.fn()) {
        const dom = createBrowserEnv();
        dom.window.fetch = fetchImpl;
        loadBrowserScript(dom, "src/main/resources/static/js/security.js");
        return dom;
    }

    test("GET same-origin setzt credentials, aber keinen CSRF-Header", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);

        await dom.window.fetch("/api/user/me");

        const [, options] = fetchMock.mock.calls[0];
        expect(options.credentials).toBe("same-origin");
        expect(options.headers).toBeUndefined();
    });

    test("externe URL bekommt keinen same-origin CSRF-Zusatz", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);

        await dom.window.fetch("https://example.org/api/test", {
            method: "POST"
        });

        const [url, options] = fetchMock.mock.calls[0];
        expect(url).toBe("https://example.org/api/test");
        expect(options.credentials).toBeUndefined();
        expect(options.headers).toBeUndefined();
    });

    test("login ohne Token lädt nicht erst /csrf", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: false,
            status: 401
        });

        const dom = setupDom(fetchMock);

        await dom.window.fetch("/api/login", {
            method: "POST"
        });

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe("/api/login");
    });

    test("register ohne Token lädt nicht erst /csrf", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);

        await dom.window.fetch("/api/register", {
            method: "POST"
        });

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe("/api/register");
    });

    test("vorhandener X-XSRF-TOKEN Header wird nicht überschrieben", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);
        dom.window.document.cookie = "XSRF-TOKEN=cookie-token";

        await dom.window.fetch("/api/profile", {
            method: "PATCH",
            headers: {
                "X-XSRF-TOKEN": "custom-token"
            }
        });

        const [, options] = fetchMock.mock.calls[0];
        expect(options.headers.get("X-XSRF-TOKEN")).toBe("custom-token");
    });

    test("ensureCsrfToken verwendet vorhandenes Cookie ohne /csrf", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);
        dom.window.document.cookie = "XSRF-TOKEN=already-there";

        const token = await dom.window.Dummy2ProSecurity.ensureCsrfToken();

        expect(token).toBe("already-there");
        expect(fetchMock).not.toHaveBeenCalled();
    });

    test("ensureCsrfToken lädt /csrf nach, wenn kein Token vorhanden ist", async () => {
        let dom;
        const fetchMock = jest.fn(async (input) => {
            if (input === "/csrf") {
                dom.window.document.cookie = "XSRF-TOKEN=fresh-token";
                return {
                    ok: true,
                    status: 200
                };
            }

            return {
                ok: true,
                status: 200
            };
        });

        dom = setupDom(fetchMock);

        const token = await dom.window.Dummy2ProSecurity.ensureCsrfToken();

        expect(token).toBe("fresh-token");
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe("/csrf");
    });

    test("refreshCsrfToken wirft Fehler bei nicht-ok Antwort", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: false,
            status: 500
        });

        const dom = setupDom(fetchMock);

        await expect(dom.window.Dummy2ProSecurity.refreshCsrfToken())
            .rejects
            .toThrow("CSRF-Token konnte nicht geladen werden.");
    });

    test("bei zweitem 403 wird nicht endlos weiter versucht", async () => {
        let dom;
        const fetchMock = jest.fn(async (input) => {
            if (input === "/csrf") {
                dom.window.document.cookie = "XSRF-TOKEN=fresh-again";
                return {
                    ok: true,
                    status: 200
                };
            }

            return {
                ok: false,
                status: 403
            };
        });

        dom = setupDom(fetchMock);
        dom.window.document.cookie = "XSRF-TOKEN=stale-token";

        // Der Wrapper darf genau einen Refresh- und genau einen Retry-Pfad
        // ausführen, danach muss der zweite 403 direkt zurückgegeben werden.
        const response = await dom.window.fetch("/api/profile", {
            method: "DELETE"
        });

        expect(response.status).toBe(403);
        expect(fetchMock).toHaveBeenCalledTimes(3);
        expect(fetchMock.mock.calls[0][0]).toBe("/api/profile");
        expect(fetchMock.mock.calls[1][0]).toBe("/csrf");
        expect(fetchMock.mock.calls[2][0]).toBe("/api/profile");
    });

    test("403 bei /api/login wird nicht erneut versucht", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: false,
            status: 403
        });

        const dom = setupDom(fetchMock);

        const response = await dom.window.fetch("/api/login", {
            method: "POST"
        });

        expect(response.status).toBe(403);
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });
});