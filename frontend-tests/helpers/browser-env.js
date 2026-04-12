const fs = require("fs");
const path = require("path");
const { JSDOM } = require("jsdom");
const { createInstrumenter } = require("istanbul-lib-instrument");

/**
 * Kleine Fallback-Implementierung für die Headers-API.
 *
 * Einige Tests laufen in jsdom/Node ohne vollständige Browser-Umgebung.
 * Damit Header-Zugriffe trotzdem wie im Browser funktionieren, wird hier
 * eine schlanke, kompatible Ersatzklasse bereitgestellt.
 */
function createHeadersFallback() {
    return class HeadersFallback {
        constructor(init = {}) {
            this.map = new Map();

            if (init instanceof HeadersFallback) {
                for (const [key, value] of init.entries()) {
                    this.set(key, value);
                }
                return;
            }

            if (Array.isArray(init)) {
                for (const [key, value] of init) {
                    this.set(key, value);
                }
                return;
            }

            for (const [key, value] of Object.entries(init || {})) {
                this.set(key, value);
            }
        }

        set(name, value) {
            this.map.set(String(name).toLowerCase(), String(value));
        }

        get(name) {
            return this.map.get(String(name).toLowerCase()) ?? null;
        }

        has(name) {
            return this.map.has(String(name).toLowerCase());
        }

        entries() {
            return this.map.entries();
        }
    };
}

function normalizePath(value) {
    return String(value).replace(/\\/g, "/");
}

function shouldInstrument(relativePath) {
    return normalizePath(relativePath).startsWith("src/main/resources/static/js/");
}

/**
 * Instrumentiert ausschließlich produktive Frontend-Skripte für die
 * Coverage-Erfassung.
 *
 * Test-Helfer selbst werden bewusst nicht instrumentiert, damit die
 * Abdeckung nur die eigentliche Browserlogik widerspiegelt.
 */
function instrumentSource(source, relativePath) {
    if (!shouldInstrument(relativePath)) {
        return source;
    }

    const instrumenter = createInstrumenter({
        coverageGlobalScope: "window",
        coverageGlobalScopeFunc: false,
        preserveComments: true,
        produceSourceMap: false
    });

    return instrumenter.instrumentSync(source, normalizePath(relativePath));
}

/**
 * Baut eine jsdom-Umgebung auf, in der die produktiven Browser-Skripte
 * direkt ausgeführt werden können.
 *
 * Zusätzlich werden Browser-nahe APIs und globale Coverage-Strukturen
 * ergänzt, damit die Tests sich möglichst wie echte Seitenausführung
 * verhalten.
 */
function createBrowserEnv(
    html = "<!doctype html><html><body></body></html>",
    url = "http://localhost/"
) {
    const dom = new JSDOM(html, {
        url,
        runScripts: "dangerously"
    });

    const HeadersImpl = typeof Headers !== "undefined" ? Headers : createHeadersFallback();

    dom.window.Headers = HeadersImpl;
    dom.window.console = console;

    if (!dom.window.requestAnimationFrame) {
        dom.window.requestAnimationFrame = callback => dom.window.setTimeout(callback, 0);
    }

    if (!dom.window.cancelAnimationFrame) {
        dom.window.cancelAnimationFrame = id => dom.window.clearTimeout(id);
    }

    if (!global.__coverage__) {
        global.__coverage__ = {};
    }

    if (!global.__dummy2proDomRegistry) {
        global.__dummy2proDomRegistry = new Set();
    }

    dom.window.__coverage__ = global.__coverage__;
    global.__dummy2proDomRegistry.add(dom);

    return dom;
}

/**
 * Lädt ein produktives Browser-Skript in die Test-Umgebung und verbindet
 * dessen Coverage-Daten mit dem globalen Sammelobjekt.
 */
function loadBrowserScript(dom, relativePath) {
    const normalizedRelativePath = normalizePath(relativePath);
    const absolutePath = path.join(process.cwd(), relativePath);
    const source = fs.readFileSync(absolutePath, "utf8");
    const preparedSource = instrumentSource(source, normalizedRelativePath);

    dom.window.__coverage__ = global.__coverage__ || {};
    dom.window.eval(`${preparedSource}\n//# sourceURL=${normalizedRelativePath}`);
    global.__coverage__ = dom.window.__coverage__ || global.__coverage__;
}

module.exports = {
    createBrowserEnv,
    loadBrowserScript
};