const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

// Testet das gepatchte fetch-Verhalten in einer Browser-ähnlichen Umgebung statt als reine Helferfunktion.
describe("security.js", () => {
    // Lädt security.js so, wie es später auch im Browser window.fetch überschreibt.
    function setupDom(fetchImpl = jest.fn()) {
        const dom = createBrowserEnv();
        dom.window.fetch = fetchImpl;
        loadBrowserScript(dom, "src/main/resources/static/js/security.js");
        return dom;
    }

    test("fügt bei unsafe same-origin Request den CSRF-Header aus Cookie hinzu", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200
        });

        const dom = setupDom(fetchMock);
        dom.window.document.cookie = "XSRF-TOKEN=test-token";

        await dom.window.fetch("/api/profile", {
            method: "POST"
        });

        expect(fetchMock).toHaveBeenCalledTimes(1);

        const [url, options] = fetchMock.mock.calls[0];
        expect(url).toBe("/api/profile");
        expect(options.credentials).toBe("same-origin");
        expect(options.headers.get("X-XSRF-TOKEN")).toBe("test-token");
    });

    test("lädt bei fehlendem Token zuerst /csrf und sendet dann den Header mit", async () => {
        // /csrf setzt den Cookie erst zur Laufzeit. Der Mock braucht daher Zugriff
        // auf dieselbe DOM-Instanz, die anschließend den eigentlichen Request sendet.
        let dom;
        // Der erste Aufruf simuliert den fehlschlagenden Request, der zweite den
        // Retry nach Token-Aktualisierung.
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

        await dom.window.fetch("/api/profile", {
            method: "PUT"
        });

        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(fetchMock.mock.calls[0][0]).toBe("/csrf");
        expect(fetchMock.mock.calls[1][0]).toBe("/api/profile");
        expect(fetchMock.mock.calls[1][1].headers.get("X-XSRF-TOKEN")).toBe("fresh-token");
    });

    test("retry bei 403 lädt neues Token und sendet Request erneut", async () => {
        let dom;
        const fetchMock = jest.fn(async (input) => {
            if (input === "/api/profile") {
                if (!fetchMock.firstProfileCallDone) {
                    fetchMock.firstProfileCallDone = true;
                    return {
                        ok: false,
                        status: 403
                    };
                }

                return {
                    ok: true,
                    status: 200
                };
            }

            if (input === "/csrf") {
                dom.window.document.cookie = "XSRF-TOKEN=fresh-after-403";
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
        dom.window.document.cookie = "XSRF-TOKEN=stale-token";

        const response = await dom.window.fetch("/api/profile", {
            method: "DELETE"
        });

        expect(response.status).toBe(200);
        expect(fetchMock).toHaveBeenCalledTimes(3);
        expect(fetchMock.mock.calls[0][0]).toBe("/api/profile");
        expect(fetchMock.mock.calls[1][0]).toBe("/csrf");
        expect(fetchMock.mock.calls[2][0]).toBe("/api/profile");
        expect(fetchMock.mock.calls[2][1].headers.get("X-XSRF-TOKEN")).toBe("fresh-after-403");
    });

    test("syncCurrentUser speichert userId, username und avatar in sessionStorage", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: true,
            status: 200,
            json: async () => ({
                userId: 42,
                username: "Jan",
                avatar: "duck.jpg"
            })
        });

        const dom = setupDom(fetchMock);

        const result = await dom.window.Dummy2ProSecurity.syncCurrentUser();

        expect(result.userId).toBe(42);
        expect(result.username).toBe("Jan");
        expect(result.avatar).toBe("duck.jpg");
        expect(dom.window.sessionStorage.getItem("userId")).toBe("42");
        expect(dom.window.sessionStorage.getItem("username")).toBe("Jan");
        expect(dom.window.sessionStorage.getItem("avatar")).toBe("duck.jpg");
    });

    test("syncCurrentUser gibt null zurück, wenn /api/user/me nicht ok ist", async () => {
        const fetchMock = jest.fn().mockResolvedValue({
            ok: false,
            status: 401
        });

        const dom = setupDom(fetchMock);

        const result = await dom.window.Dummy2ProSecurity.syncCurrentUser();

        expect(result).toBeNull();
        expect(dom.window.sessionStorage.getItem("userId")).toBeNull();
    });
});