(function () {
    // Einheitliche UI-Meldungen für globale Hinweise, Inline-Fehler und modale Dialoge.
    const GLOBAL_ID = "dummy2pro-global-feedback";
    const MODAL_ID = "dummy2pro-modal-root";

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/\"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Der globale Container wird bei Bedarf lazily erzeugt, damit Seiten ohne
    // vorbereitete Feedback-Struktur dieselben Hilfsfunktionen nutzen können.
    function ensureGlobalContainer() {
        let container = document.getElementById(GLOBAL_ID);
        if (!container) {
            container = document.createElement("div");
            container.id = GLOBAL_ID;
            container.className = "pointer-events-none fixed inset-x-0 bottom-5 z-[9998] flex justify-center px-4";
            document.body.appendChild(container);
        }
        return container;
    }

    function ensureModalRoot() {
        let modalRoot = document.getElementById(MODAL_ID);
        if (!modalRoot) {
            modalRoot = document.createElement("div");
            modalRoot.id = MODAL_ID;
            modalRoot.className = "fixed inset-0 z-[9999] hidden items-center justify-center bg-slate-950/80 px-4 py-8 backdrop-blur-sm";
            document.body.appendChild(modalRoot);
        }
        return modalRoot;
    }

    function getTypeConfig(type) {
        switch (type) {
            case "success":
                return {
                    wrapper: "border-emerald-400/60 bg-emerald-950/95 text-emerald-50 shadow-emerald-950/50",
                    badge: "border-emerald-300/40 bg-emerald-400/15 text-emerald-100",
                    label: "Erfolg"
                };
            case "warning":
                return {
                    wrapper: "border-amber-300/60 bg-amber-950/95 text-amber-50 shadow-amber-950/50",
                    badge: "border-amber-200/40 bg-amber-300/15 text-amber-100",
                    label: "Hinweis"
                };
            case "error":
            default:
                return {
                    wrapper: "border-rose-400/60 bg-rose-950/95 text-rose-50 shadow-rose-950/50",
                    badge: "border-rose-300/40 bg-rose-400/15 text-rose-100",
                    label: "Fehler"
                };
        }
    }

    function inferType(message) {
        const text = String(message ?? "").toLowerCase();

        if (/fehler|fehl|nicht geladen|konnte nicht|ungültig|bereits|nicht gefunden|abgelehnt/.test(text)) {
            return "error";
        }

        if (/erfolgreich|gespeichert|angelegt|aktualisiert/.test(text)) {
            return "success";
        }

        if (/bitte|achtung|warn/.test(text)) {
            return "warning";
        }

        return "error";
    }

    // Es wird immer nur eine globale Meldung gleichzeitig angezeigt, damit sich
    // kurzlebige Hinweise nicht stapeln und gegenseitig verdecken.
    function showGlobalMessage(message, type = inferType(message), options = {}) {
        const container = ensureGlobalContainer();
        const config = getTypeConfig(type);
        const element = document.createElement("div");
        const duration = options.persistent ? 0 : (options.duration ?? 4200);

        element.className = [
            "pointer-events-auto w-full max-w-2xl rounded-2xl border px-4 py-3 shadow-2xl transition duration-300 ease-out",
            "opacity-0 translate-y-3",
            config.wrapper
        ].join(" ");

        element.innerHTML = `
            <div class="flex items-start gap-3">
                <span class="shrink-0 rounded-full border px-3 py-1 text-[11px] font-extrabold uppercase tracking-[0.28em] ${config.badge}">${config.label}</span>
                <p class="min-w-0 flex-1 pt-0.5 text-sm font-medium leading-6">${escapeHtml(message)}</p>
                <button type="button" class="rounded-lg px-2 py-1 text-lg leading-none text-white/70 transition hover:bg-white/10 hover:text-white" aria-label="Meldung schließen">×</button>
            </div>
        `;

        const closeButton = element.querySelector("button");
        const remove = () => {
            element.classList.add("opacity-0", "translate-y-3");
            window.setTimeout(() => element.remove(), 220);
        };

        closeButton.addEventListener("click", remove);
        container.innerHTML = "";
        container.appendChild(element);

        requestAnimationFrame(() => {
            element.classList.remove("opacity-0", "translate-y-3");
        });

        if (duration > 0) {
            window.setTimeout(remove, duration);
        }

        return element;
    }

    function setInlineMessage(targetOrId, message, type = inferType(message)) {
        const element = typeof targetOrId === "string"
            ? document.getElementById(targetOrId)
            : targetOrId;

        if (!element) {
            return null;
        }

        const config = getTypeConfig(type);
        element.className = [
            "rounded-2xl border px-4 py-3 text-sm font-medium leading-6 shadow-lg",
            config.wrapper
        ].join(" ");
        element.innerHTML = `
            <div class="flex items-start gap-3">
                <span class="shrink-0 rounded-full border px-3 py-1 text-[11px] font-extrabold uppercase tracking-[0.28em] ${config.badge}">${config.label}</span>
                <p class="min-w-0 flex-1">${escapeHtml(message)}</p>
            </div>
        `;
        element.classList.remove("hidden");
        return element;
    }

    function clearInlineMessage(targetOrId) {
        const element = typeof targetOrId === "string"
            ? document.getElementById(targetOrId)
            : targetOrId;

        if (!element) {
            return;
        }

        element.innerHTML = "";
        element.className = "hidden";
    }

    function showConfirm(message, options = {}) {
        return new Promise(resolve => {
            const modalRoot = ensureModalRoot();
            const title = escapeHtml(options.title || "Bitte bestätigen");
            const confirmText = escapeHtml(options.confirmText || "Bestätigen");
            const cancelText = escapeHtml(options.cancelText || "Abbrechen");
            const isDanger = options.variant === "danger";
            const iconWrapper = isDanger
                ? "bg-rose-500/20 text-rose-100 border border-rose-300/30"
                : "bg-indigo-500/20 text-indigo-100 border border-indigo-300/30";
            const confirmButton = isDanger
                ? "bg-gradient-to-r from-rose-600 to-red-500 hover:from-rose-500 hover:to-red-400 shadow-rose-950/50"
                : "bg-gradient-to-r from-indigo-600 to-violet-500 hover:from-indigo-500 hover:to-violet-400 shadow-indigo-950/50";

            modalRoot.innerHTML = `
                <div class="w-full max-w-lg rounded-[2rem] border border-white/10 bg-slate-900/95 p-6 shadow-2xl shadow-black/60">
                    <div class="flex items-start gap-4">
                        <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl ${iconWrapper}">
                            <span class="text-2xl font-black">${isDanger ? "!" : "?"}</span>
                        </div>
                        <div class="min-w-0 flex-1">
                            <h2 class="text-2xl font-extrabold tracking-tight text-white">${title}</h2>
                            <p class="mt-3 text-sm leading-6 text-slate-200">${escapeHtml(message)}</p>
                        </div>
                    </div>
                    <div class="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                        <button type="button" data-action="cancel" class="rounded-2xl border border-slate-600 bg-slate-800 px-5 py-3 font-semibold text-white transition hover:bg-slate-700">${cancelText}</button>
                        <button type="button" data-action="confirm" class="rounded-2xl px-5 py-3 font-semibold text-white shadow-xl transition ${confirmButton}">${confirmText}</button>
                    </div>
                </div>
            `;

            modalRoot.classList.remove("hidden");
            modalRoot.classList.add("flex");
            document.body.classList.add("overflow-hidden");

            // cleanup schließt den Dialog zentral und liefert den semantischen
            // Rückgabewert an den aufrufenden Code zurück.
            const cleanup = (result) => {
                modalRoot.innerHTML = "";
                modalRoot.classList.add("hidden");
                modalRoot.classList.remove("flex");
                document.body.classList.remove("overflow-hidden");
                resolve(result);
            };

            modalRoot.querySelector('[data-action="cancel"]').addEventListener("click", () => cleanup(false));
            modalRoot.querySelector('[data-action="confirm"]').addEventListener("click", () => cleanup(true));
            modalRoot.addEventListener("click", (event) => {
                if (event.target === modalRoot) {
                    cleanup(false);
                }
            }, { once: true });

            modalRoot.querySelector('[data-action="confirm"]').focus();

            const onKeyDown = (event) => {
                if (event.key === "Escape") {
                    document.removeEventListener("keydown", onKeyDown);
                    cleanup(false);
                }
            };
            document.addEventListener("keydown", onKeyDown, { once: true });
        });
    }

    // Prompt und Confirm teilen sich denselben Modal-Root, damit nie mehrere
    // blockierende Overlays gleichzeitig konkurrieren.
    function showPrompt(message, defaultValue = "", options = {}) {
        return new Promise(resolve => {
            const modalRoot = ensureModalRoot();
            const title = escapeHtml(options.title || "Eingabe");
            const confirmText = escapeHtml(options.confirmText || "Speichern");
            const cancelText = escapeHtml(options.cancelText || "Abbrechen");
            const placeholder = escapeHtml(options.placeholder || "Bitte eingeben");

            modalRoot.innerHTML = `
                <div class="w-full max-w-xl rounded-[2rem] border border-white/10 bg-slate-900/95 p-6 shadow-2xl shadow-black/60">
                    <div class="flex items-start gap-4">
                        <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-indigo-300/30 bg-indigo-500/20 text-2xl font-black text-indigo-100">✎</div>
                        <div class="min-w-0 flex-1">
                            <h2 class="text-2xl font-extrabold tracking-tight text-white">${title}</h2>
                            <p class="mt-3 text-sm leading-6 text-slate-200">${escapeHtml(message)}</p>
                        </div>
                    </div>

                    <label class="mt-6 block">
                        <span class="mb-2 block text-sm font-medium text-slate-300">Neuer Wert</span>
                        <input data-role="prompt-input" type="text" placeholder="${placeholder}" class="w-full rounded-2xl border border-slate-600 bg-slate-800 px-4 py-3 text-white outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500/40" />
                    </label>

                    <div class="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                        <button type="button" data-action="cancel" class="rounded-2xl border border-slate-600 bg-slate-800 px-5 py-3 font-semibold text-white transition hover:bg-slate-700">${cancelText}</button>
                        <button type="button" data-action="confirm" class="rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-500 px-5 py-3 font-semibold text-white shadow-xl shadow-indigo-950/50 transition hover:from-indigo-500 hover:to-violet-400">${confirmText}</button>
                    </div>
                </div>
            `;

            modalRoot.classList.remove("hidden");
            modalRoot.classList.add("flex");
            document.body.classList.add("overflow-hidden");

            const input = modalRoot.querySelector('[data-role="prompt-input"]');
            input.value = defaultValue ?? "";
            input.focus();
            input.select();

            // Null steht hier bewusst für Abbruch, ein String für die
            // bestätigte Eingabe. So bleibt der Aufrufer API-seitig eindeutig.
            const cleanup = (result) => {
                modalRoot.innerHTML = "";
                modalRoot.classList.add("hidden");
                modalRoot.classList.remove("flex");
                document.body.classList.remove("overflow-hidden");
                resolve(result);
            };

            modalRoot.querySelector('[data-action="cancel"]').addEventListener("click", () => cleanup(null));
            modalRoot.querySelector('[data-action="confirm"]').addEventListener("click", () => cleanup(input.value));
            modalRoot.addEventListener("click", (event) => {
                if (event.target === modalRoot) {
                    cleanup(null);
                }
            }, { once: true });

            input.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    cleanup(input.value);
                }
            });

            const onKeyDown = (event) => {
                if (event.key === "Escape") {
                    document.removeEventListener("keydown", onKeyDown);
                    cleanup(null);
                }
            };
            document.addEventListener("keydown", onKeyDown, { once: true });
        });
    }

    // Notice ist für blockierende Einzelhinweise gedacht, bei denen der Nutzer
    // aktiv bestätigen soll, bevor es im Ablauf weitergeht.
    function showNotice(message, options = {}) {
        return new Promise(resolve => {
            const modalRoot = ensureModalRoot();
            const title = escapeHtml(options.title || "Hinweis");
            const okText = escapeHtml(options.okText || "OK");
            const type = options.type || inferType(message);
            const isError = type === "error";
            const isWarning = type === "warning";

            const iconWrapper = isError
                ? "bg-rose-500/20 text-rose-100 border border-rose-300/30"
                : isWarning
                    ? "bg-amber-500/20 text-amber-100 border border-amber-300/30"
                    : "bg-emerald-500/20 text-emerald-100 border border-emerald-300/30";

            const buttonClass = isError
                ? "bg-gradient-to-r from-rose-600 to-red-500 hover:from-rose-500 hover:to-red-400 shadow-rose-950/50"
                : isWarning
                    ? "bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 shadow-amber-950/50 text-slate-950"
                    : "bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 shadow-emerald-950/50";

            const icon = isError ? "!" : (isWarning ? "!" : "✓");

            modalRoot.innerHTML = `
            <div class="w-full max-w-lg rounded-[2rem] border border-white/10 bg-slate-900/95 p-6 shadow-2xl shadow-black/60">
                <div class="flex items-start gap-4">
                    <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl ${iconWrapper}">
                        <span class="text-2xl font-black">${icon}</span>
                    </div>
                    <div class="min-w-0 flex-1">
                        <h2 class="text-2xl font-extrabold tracking-tight text-white">${title}</h2>
                        <p class="mt-3 text-sm leading-6 text-slate-200">${escapeHtml(message)}</p>
                    </div>
                </div>
                <div class="mt-7 flex justify-end">
                    <button type="button" data-action="ok" class="rounded-2xl px-5 py-3 font-semibold text-white shadow-xl transition ${buttonClass}">
                        ${okText}
                    </button>
                </div>
            </div>
        `;

            modalRoot.classList.remove("hidden");
            modalRoot.classList.add("flex");
            document.body.classList.add("overflow-hidden");

            const cleanup = () => {
                modalRoot.innerHTML = "";
                modalRoot.classList.add("hidden");
                modalRoot.classList.remove("flex");
                document.body.classList.remove("overflow-hidden");
                resolve(true);
            };

            modalRoot.querySelector('[data-action="ok"]').addEventListener("click", cleanup);
            modalRoot.addEventListener("click", (event) => {
                if (event.target === modalRoot) {
                    cleanup();
                }
            }, { once: true });

            modalRoot.querySelector('[data-action="ok"]').focus();

            const onKeyDown = (event) => {
                if (event.key === "Escape" || event.key === "Enter") {
                    document.removeEventListener("keydown", onKeyDown);
                    cleanup();
                }
            };
            document.addEventListener("keydown", onKeyDown, { once: true });
        });
    }

    // Die API bündelt absichtlich alle UI-Rückmeldungen unter einem globalen
    // Namespace, damit statische Seiten und Thymeleaf-Views dieselben Helfer nutzen.
    window.Dummy2ProUI = {
        alert(message, options = {}) {
            return showGlobalMessage(message, options.type || inferType(message), options);
        },
        success(message, options = {}) {
            return showGlobalMessage(message, "success", options);
        },
        error(message, options = {}) {
            return showGlobalMessage(message, "error", options);
        },
        warning(message, options = {}) {
            return showGlobalMessage(message, "warning", options);
        },
        setInlineMessage,
        clearInlineMessage,
        confirm: showConfirm,
        prompt: showPrompt,
        notice: showNotice
    };

    window.alert = function (message) {
        showGlobalMessage(message, inferType(message));
    };
})();