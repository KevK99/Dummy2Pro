(() => {
    // Zentrale Audio-Steuerung für Klicks, Quiz-Feedback, Raumwechsel und Medaillen.
    const SOUND_PATH = "/sounds";
    const STORAGE_KEY_ENABLED = "dummy2proSoundEnabled";
    const STORAGE_KEY_VOLUME = "dummy2proSoundVolume";

    const FILES = {
        answerSelect: `${SOUND_PATH}/ui/answer-select.wav`,
        correct: `${SOUND_PATH}/ui/answer-correct.wav`,
        wrong: `${SOUND_PATH}/ui/answer-wrong.wav`,
        home: `${SOUND_PATH}/ui/home.wav`,
        profile: `${SOUND_PATH}/ui/profile.wav`,
        name: `${SOUND_PATH}/ui/name-edit.wav`,
        password: `${SOUND_PATH}/ui/password.wav`,
        avatar: `${SOUND_PATH}/ui/avatar.wav`,
        save: `${SOUND_PATH}/ui/save.wav`,
        delete: `${SOUND_PATH}/ui/delete.wav`,
        dialogNext: `${SOUND_PATH}/ui/dialog-next.wav`,
        register: `${SOUND_PATH}/ui/register.wav`,
        login: `${SOUND_PATH}/ui/login.wav`,
        nextQuestion: `${SOUND_PATH}/ui/next-question.wav`,
        colorMode: `${SOUND_PATH}/ui/theme-toggle.wav`,
        checkResults: `${SOUND_PATH}/ui/check-results.wav`,
        medalBronze: `${SOUND_PATH}/medals/medal-bronze.wav`,
        medalSilver: `${SOUND_PATH}/medals/medal-silver.wav`,
        medalGold: `${SOUND_PATH}/medals/medal-gold.wav`,
        endscreenBronze: `${SOUND_PATH}/medals/endscreen-bronze.wav`,
        endscreenSilver: `${SOUND_PATH}/medals/endscreen-silver.wav`,
        endscreenGold: `${SOUND_PATH}/medals/endscreen-gold.wav`
    };

    // Vorgehaltene Audio-Objekte vermeiden wiederholte Initialisierung derselben Datei.
    const audioCache = new Map();
    // Cooldowns verhindern Dopplerauslösung bei schnellen Mehrfachklicks oder Mutation-Spitzen.
    const playCooldowns = new Map();

    let unlocked = false;
    let feedbackObserverAttached = false;
    let roomMedalObserverAttached = false;
    let endscreenObserverAttached = false;
    let lastFeedbackText = "";
    let lastRoomMedal = "NONE";
    let lastEndscreenMedal = "NONE";

    function isEnabled() {
        const raw = localStorage.getItem(STORAGE_KEY_ENABLED);
        return raw == null ? true : raw === "true";
    }

    function setEnabled(value) {
        localStorage.setItem(STORAGE_KEY_ENABLED, String(Boolean(value)));
    }

    function getVolume() {
        const raw = Number(localStorage.getItem(STORAGE_KEY_VOLUME));

        if (!Number.isFinite(raw)) {
            return 1;
        }

        return Math.min(1, Math.max(0.15, raw));
    }

    function setVolume(value) {
        const normalized = Math.min(1, Math.max(0, Number(value) || 0.55));
        localStorage.setItem(STORAGE_KEY_VOLUME, String(normalized));

        for (const audio of audioCache.values()) {
            audio.volume = normalized;
        }
    }

    function withCooldown(key, minMs = 90) {
        const current = performance.now();
        const last = playCooldowns.get(key) ?? 0;

        if (current - last < minMs) {
            return false;
        }

        playCooldowns.set(key, current);
        return true;
    }

    function preloadFile(src) {
        if (audioCache.has(src)) {
            return audioCache.get(src);
        }

        const audio = new Audio(src);
        audio.preload = "auto";
        audio.volume = getVolume();
        audioCache.set(src, audio);
        return audio;
    }

    // Browser blockieren Audio oft bis zur ersten echten Benutzerinteraktion.
    // Der Probe-Play entsperrt die Wiedergabe so früh wie möglich.
    function unlock() {
        if (unlocked) {
            return;
        }

        unlocked = true;

        const firstSrc = FILES.answerSelect;
        const probe = preloadFile(firstSrc);
        const playPromise = probe.play();

        if (playPromise && typeof playPromise.then === "function") {
            playPromise
                .then(() => {
                    probe.pause();
                    probe.currentTime = 0;
                })
                .catch(() => {
                    unlocked = false;
                });
        }
    }

    // Für jede Wiedergabe wird aus dem gecachten Audio ein Klon erzeugt,
    // damit parallel ausgelöste Sounds sich nicht gegenseitig überschreiben.
    function playFile(src, cooldownKey = src) {
        if (!isEnabled()) {
            return;
        }

        if (!withCooldown(cooldownKey)) {
            return;
        }

        const original = preloadFile(src);
        const instance = original.cloneNode(true);
        instance.volume = getVolume();

        const playPromise = instance.play();
        if (playPromise && typeof playPromise.catch === "function") {
            playPromise.catch(() => {});
        }
    }

    function roomFile(roomId) {
        const parsed = Number(roomId);
        const normalized = Number.isFinite(parsed) && parsed >= 1 ? parsed : 1;
        const padded = String(normalized).padStart(2, "0");
        return `${SOUND_PATH}/rooms/room-enter-${padded}.wav`;
    }

    function normalizeMedal(value) {
        return String(value ?? "NONE").trim().toUpperCase();
    }

    function determineOverallMedal(correctAnswers, totalQuestions) {
        const correct = Number(correctAnswers) || 0;
        const total = Number(totalQuestions) || 0;

        if (total <= 0) {
            return "NONE";
        }

        const ratio = correct / total;

        if (ratio >= 1) {
            return "GOLD";
        }

        if (ratio >= 0.75) {
            return "SILVER";
        }

        if (ratio >= 0.5) {
            return "BRONZE";
        }

        return "NONE";
    }

    function playMedal(medal, variant = "room") {
        const normalized = normalizeMedal(medal);

        if (normalized === "NONE") {
            return;
        }

        if (String(variant).toLowerCase() === "endscreen") {
            if (normalized === "BRONZE") {
                playFile(FILES.endscreenBronze, "endscreenBronze");
                return;
            }

            if (normalized === "SILVER") {
                playFile(FILES.endscreenSilver, "endscreenSilver");
                return;
            }

            if (normalized === "GOLD") {
                playFile(FILES.endscreenGold, "endscreenGold");
            }
            return;
        }

        if (normalized === "BRONZE") {
            playFile(FILES.medalBronze, "medalBronze");
            return;
        }

        if (normalized === "SILVER") {
            playFile(FILES.medalSilver, "medalSilver");
            return;
        }

        if (normalized === "GOLD") {
            playFile(FILES.medalGold, "medalGold");
        }
    }

    function play(name, payload = {}) {
        switch (name) {
            case "answerSelect":
                playFile(FILES.answerSelect, "answerSelect");
                return;
            case "correct":
                playFile(FILES.correct, "correct");
                return;
            case "wrong":
                playFile(FILES.wrong, "wrong");
                return;
            case "home":
                playFile(FILES.home, "home");
                return;
            case "profile":
                playFile(FILES.profile, "profile");
                return;
            case "name":
                playFile(FILES.name, "name");
                return;
            case "password":
                playFile(FILES.password, "password");
                return;
            case "avatar":
                playFile(FILES.avatar, "avatar");
                return;
            case "save":
                playFile(FILES.save, "save");
                return;
            case "delete":
                playFile(FILES.delete, "delete");
                return;
            case "dialogNext":
                playFile(FILES.dialogNext, "dialogNext");
                return;
            case "register":
                playFile(FILES.register, "register");
                return;
            case "login":
                playFile(FILES.login, "login");
                return;
            case "nextQuestion":
                playFile(FILES.nextQuestion, "nextQuestion");
                return;
            case "colorMode":
                playFile(FILES.colorMode, "colorMode");
                return;
            case "checkResults":
                playFile(FILES.checkResults, "checkResults");
                return;
            case "roomEnter":
                playFile(roomFile(payload.roomId), `roomEnter:${payload.roomId}`);
                return;
            case "roomTransition":
                playFile(roomFile(payload.roomId), `roomTransition:${payload.roomId}`);
                return;
            default:
                break;
        }
    }

    function textOfElement(element) {
        if (!element) {
            return "";
        }

        const aria = element.getAttribute?.("aria-label") || "";
        const title = element.getAttribute?.("title") || "";
        const text = element.textContent || "";
        return `${aria} ${title} ${text}`.replace(/\s+/g, " ").trim().toLowerCase();
    }

    function getClickableTarget(eventTarget) {
        return eventTarget?.closest?.("button, a, img, [role='button'], .room-link, .quiz-answer-btn, .gap-option-btn, [id^='gapPreview-']");
    }

    // Ordnet UI-Elemente einer Sound-Kategorie zu, auch wenn die Erkennung
    // nur über Texte, IDs, Hrefs oder bekannte onclick-Muster möglich ist.
    function classifyClick(element) {
        if (!element) {
            return null;
        }

        const id = (element.id || "").toLowerCase();
        const href = (element.getAttribute?.("href") || "").toLowerCase();
        const onclick = (element.getAttribute?.("onclick") || "").toLowerCase();
        const text = textOfElement(element);

        if (
            element.matches?.(".quiz-answer-btn, .gap-option-btn, [id^='gapPreview-']") ||
            element.closest?.("#answerContainer")
        ) {
            return { name: "answerSelect" };
        }

        if (element.classList?.contains("room-link") || element.dataset?.roomId) {
            return {
                name: "roomEnter",
                payload: { roomId: element.dataset.roomId || "1" }
            };
        }

        if (id === "dialognextbtn" || text === "weiter" || text.includes(" weiter ")) {
            return { name: "dialogNext" };
        }

        if (id === "submitanswerbtn" || text.includes("antwort prüfen")) {
            return { name: "checkResults" };
        }

        if (id === "nextquestionbtn") {
            const roomMatch = text.match(/zu raum\s+(\d+)/i);

            if (roomMatch) {
                return {
                    name: "roomTransition",
                    payload: { roomId: roomMatch[1] }
                };
            }

            if (text.includes("abschlussraum")) {
                return {
                    name: "roomTransition",
                    payload: { roomId: "16" }
                };
            }

            if (text.includes("dashboard")) {
                return { name: "home" };
            }

            return { name: "nextQuestion" };
        }

        if (
            id === "darkmodebtn" ||
            text.includes("darkmode") ||
            text.includes("dark mode") ||
            text.includes("light mode") ||
            text.includes("bunt edition")
        ) {
            return { name: "colorMode" };
        }

        if (
            href.includes("/review") ||
            text.includes("antworten prüfen")
        ) {
            return { name: "checkResults" };
        }

        if (
            href.includes("/dashboard") ||
            element.classList?.contains("room-home-button") ||
            text.includes("zum dashboard")
        ) {
            return { name: "home" };
        }

        if (
            href.includes("/profile") ||
            id === "headlineavatar"
        ) {
            return { name: "profile" };
        }

        if (
            id === "currentprofileimage" ||
            id === "profilepicbtn" ||
            id === "savenewprofilepic" ||
            element.closest?.("#profilePicGrid") ||
            onclick.includes("setprofile(") ||
            onclick.includes("openprofilepicedit") ||
            onclick.includes("closeprofilepicedit") ||
            onclick.includes("saveprofilepic") ||
            text.includes("profilbild")
        ) {
            return { name: "avatar" };
        }

        if (
            id === "currentnameinput" ||
            id === "newnamebutton" ||
            id === "button" ||
            onclick.includes("closenameedit") ||
            onclick.includes("renamerun") ||
            text.includes("name ändern") ||
            text.includes("neuen namen bestätigen") ||
            text.includes("umbenennen")
        ) {
            return { name: "name" };
        }

        if (
            id === "pwformenable" ||
            onclick.includes("showpwform") ||
            onclick.includes("savenewpassword") ||
            onclick.includes("closepwform") ||
            text.includes("passwort")
        ) {
            return { name: "password" };
        }

        if (
            id === "createrunbtn" ||
            onclick.includes("createnewrun") ||
            onclick.includes("loadrun(") ||
            text.includes("spielstand hinzufügen") ||
            text.includes("auswählen")
        ) {
            return { name: "save" };
        }

        if (
            onclick.includes("deleteuser") ||
            onclick.includes("deleterun(") ||
            text.includes("account löschen") ||
            text.includes("spielstand löschen") ||
            text === "löschen" ||
            text.includes(" löschen")
        ) {
            return { name: "delete" };
        }

        if (text.includes("registrieren")) {
            return { name: "register" };
        }

        if (text.includes("einloggen") || text.includes("anmelden")) {
            return { name: "login" };
        }

        return null;
    }

    function handleDocumentClick(event) {
        unlock();

        const element = getClickableTarget(event.target);
        if (!element) {
            return;
        }

        const result = classifyClick(element);
        if (!result) {
            return;
        }

        play(result.name, result.payload || {});
    }

    function handleFormSubmit(event) {
        unlock();

        const form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        const text = textOfElement(form);
        const hasPassword = form.querySelector("input[type='password']") != null;
        const hasUsername = form.querySelector("#username, input[name='username']") != null;

        if (!hasPassword || !hasUsername) {
            return;
        }

        if (location.pathname.includes("register") || text.includes("registrieren")) {
            play("register");
            return;
        }

        play("login");
    }

    // Quiz-Feedback wird nicht direkt hier ausgelöst, sondern über DOM-Änderungen
    // an der vorhandenen Feedback-Box beobachtet.
    function attachFeedbackObserver() {
        if (feedbackObserverAttached) {
            return;
        }

        const feedbackBox = document.getElementById("feedbackBox");
        if (!feedbackBox) {
            return;
        }

        feedbackObserverAttached = true;
        lastFeedbackText = feedbackBox.textContent.trim();

        const observer = new MutationObserver(() => {
            const currentText = feedbackBox.textContent.trim();

            if (!currentText || currentText === lastFeedbackText) {
                return;
            }

            lastFeedbackText = currentText;

            if (currentText.startsWith("Richtig")) {
                play("correct");
                return;
            }

            if (currentText.startsWith("Falsch")) {
                play("wrong");
            }
        });

        observer.observe(feedbackBox, {
            childList: true,
            characterData: true,
            subtree: true
        });
    }

    function extractMedalFromText(text) {
        const normalized = String(text || "").toUpperCase();

        if (normalized.includes("GOLD")) {
            return "GOLD";
        }

        if (normalized.includes("SILVER")) {
            return "SILVER";
        }

        if (normalized.includes("BRONZE")) {
            return "BRONZE";
        }

        return "NONE";
    }

    // Auf Raumseiten wird die Medaille aus dem Status-Badge gelesen, weil der
    // endgültige Wert erst nach der UI-Aktualisierung zuverlässig vorliegt.
    function attachRoomMedalObserver() {
        if (roomMedalObserverAttached) {
            return;
        }

        const badge = document.getElementById("roomStatusBadge");
        if (!badge) {
            return;
        }

        roomMedalObserverAttached = true;
        lastRoomMedal = extractMedalFromText(badge.textContent);

        const observer = new MutationObserver(() => {
            const currentMedal = extractMedalFromText(badge.textContent);

            if (currentMedal === "NONE" || currentMedal === lastRoomMedal) {
                return;
            }

            lastRoomMedal = currentMedal;
            playMedal(currentMedal, "room");
        });

        observer.observe(badge, {
            childList: true,
            characterData: true,
            subtree: true
        });
    }

    function parseTotalsPair(text) {
        const match = String(text || "").match(/(\d+)\s*\/\s*(\d+)/);
        if (!match) {
            return null;
        }

        return {
            first: Number(match[1]),
            second: Number(match[2])
        };
    }

    function evaluateEndscreenMedalFromDom() {
        const correctElement = document.getElementById("correctAnswersText");
        const answeredElement = document.getElementById("answeredQuestionsText");

        if (!correctElement || !answeredElement) {
            return "NONE";
        }

        const correct = Number((correctElement.textContent || "").replace(/[^\d]/g, ""));
        const pair = parseTotalsPair(answeredElement.textContent);

        if (!pair) {
            return "NONE";
        }

        return determineOverallMedal(correct, pair.second);
    }

    // Auf dem Endscreen wird die Gesamtmedaille aus den angezeigten Summen
    // rekonstruiert, damit der passende Abschluss-Sound exakt einmal startet.
    function attachEndscreenObserver() {
        if (endscreenObserverAttached) {
            return;
        }

        const correctElement = document.getElementById("correctAnswersText");
        const answeredElement = document.getElementById("answeredQuestionsText");

        if (!correctElement || !answeredElement) {
            return;
        }

        endscreenObserverAttached = true;
        lastEndscreenMedal = evaluateEndscreenMedalFromDom();

        const observer = new MutationObserver(() => {
            const currentMedal = evaluateEndscreenMedalFromDom();

            if (currentMedal === "NONE" || currentMedal === lastEndscreenMedal) {
                return;
            }

            lastEndscreenMedal = currentMedal;
            playMedal(currentMedal, "endscreen");
        });

        observer.observe(correctElement, {
            childList: true,
            characterData: true,
            subtree: true
        });

        observer.observe(answeredElement, {
            childList: true,
            characterData: true,
            subtree: true
        });
    }

    function initObservers() {
        attachFeedbackObserver();
        attachRoomMedalObserver();
        attachEndscreenObserver();
    }

    // Zusätzlich zu den fest definierten UI-Sounds werden die Raum-Sounds
    // bewusst vorab geladen, damit Übergänge ohne hörbare Verzögerung starten.
    function preloadAll() {
        Object.values(FILES).forEach(preloadFile);
        for (let roomId = 1; roomId <= 16; roomId++) {
            preloadFile(roomFile(roomId));
        }
    }

    window.Dummy2ProSound = {
        play,
        playRoomEnter(roomId) {
            play("roomEnter", { roomId });
        },
        playRoomTransition(roomId) {
            play("roomTransition", { roomId });
        },
        playMedal,
        determineOverallMedal,
        unlock,
        setEnabled,
        isEnabled,
        setVolume,
        getVolume
    };

    // Capture-Phase stellt sicher, dass der Sound auch dann erkannt wird,
    // wenn spätere Handler Navigation oder DOM-Änderungen sofort auslösen.
    document.addEventListener("click", handleDocumentClick, true);
    document.addEventListener("submit", handleFormSubmit, true);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => {
            preloadAll();
            initObservers();
        });
    } else {
        preloadAll();
        initObservers();
    }

    // Einige Raum- und Endscreen-Elemente erscheinen verzögert. Die gestaffelten
    // Re-Initialisierungen hängen Observer deshalb auch nach spätem Rendering an.
    window.addEventListener("load", () => {
        setTimeout(initObservers, 150);
        setTimeout(initObservers, 700);
    });

    window.addEventListener("pointerdown", unlock, { passive: true });
    window.addEventListener("keydown", unlock, { passive: true });
    window.addEventListener("touchstart", unlock, { passive: true });
})();