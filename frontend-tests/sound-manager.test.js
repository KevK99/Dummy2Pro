const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

function flush(dom, ms = 0) {
    return new Promise(resolve => dom.window.setTimeout(resolve, ms));
}

describe("sound-manager.js", () => {
    class AudioMock {
        static instances = [];

        constructor(src) {
            this.src = src;
            this.preload = "";
            this.volume = 1;
            this.currentTime = 0;
            this.play = jest.fn(() => Promise.resolve());
            this.pause = jest.fn();
            AudioMock.instances.push(this);
        }

        cloneNode() {
            const clone = new AudioMock(this.src);
            clone.volume = this.volume;
            return clone;
        }
    }

    function playedSources() {
        return AudioMock.instances
            .filter(instance => instance.play.mock.calls.length > 0)
            .map(instance => instance.src);
    }

    function clearAudioHistory() {
        AudioMock.instances.forEach(instance => {
            instance.play.mockClear();
            instance.pause.mockClear();
        });
    }

    /**
     * Baut eine kleine Browserumgebung für den Sound-Manager auf.
     *
     * Dabei werden Audio, localStorage und Zeitmessung kontrolliert
     * nachgebildet, damit Sound-Auswahl und Cooldowns deterministisch
     * testbar bleiben.
     */
    function setupDom(url = "http://localhost/") {
        AudioMock.instances = [];

        const dom = createBrowserEnv(`
        <!doctype html>
        <html>
            <body>
                <div id="feedbackBox"></div>
                <div id="roomStatusBadge"></div>
                <div id="correctAnswersText"></div>
                <div id="answeredQuestionsText"></div>
            </body>
        </html>
    `, url);

        dom.window.localStorage.clear();
        dom.window.localStorage.removeItem("dummy2proSoundEnabled");
        dom.window.localStorage.removeItem("dummy2proSoundVolume");

        let currentTime = 0;

        // Der Sound-Manager nutzt performance.now() für interne Sperrzeiten.
        // Mit dem künstlich steigenden Zeitwert werden diese Pfade ohne
        // echte Wartezeiten reproduzierbar testbar.
        dom.window.performance.now = () => {
            currentTime += 120;
            return currentTime;
        };

        dom.window.Audio = AudioMock;
        dom.window.console = {
            ...console,
            error: jest.fn()
        };

        loadBrowserScript(dom, "src/main/resources/static/js/sound-manager.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        clearAudioHistory();

        return dom;
    }

    test("Standardwerte und Clamping für enabled/volume funktionieren", () => {
        const dom = setupDom();

        expect(dom.window.Dummy2ProSound.isEnabled()).toBe(true);
        expect(dom.window.Dummy2ProSound.getVolume()).toBe(0.15);

        dom.window.localStorage.setItem("dummy2proSoundEnabled", "false");
        expect(dom.window.Dummy2ProSound.isEnabled()).toBe(false);

        dom.window.localStorage.setItem("dummy2proSoundVolume", "0.1");
        expect(dom.window.Dummy2ProSound.getVolume()).toBe(0.15);

        dom.window.localStorage.setItem("dummy2proSoundVolume", "5");
        expect(dom.window.Dummy2ProSound.getVolume()).toBe(1);
    });

    test("setVolume aktualisiert auch bereits vorgeladene Audios", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.setVolume(0.33);

        expect(dom.window.Dummy2ProSound.getVolume()).toBe(0.33);
        expect(AudioMock.instances.every(instance => instance.volume === 0.33)).toBe(true);
    });

    test("play spielt Datei nur wenn Sound aktiviert ist", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.play("home");
        expect(playedSources()).toContain("/sounds/ui/home.wav");

        clearAudioHistory();
        dom.window.Dummy2ProSound.setEnabled(false);
        dom.window.Dummy2ProSound.play("profile");

        expect(playedSources()).toEqual([]);
    });

    test("playRoomEnter und playRoomTransition verwenden die richtige Raumdatei", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.playRoomEnter(7);
        dom.window.Dummy2ProSound.playRoomTransition(16);

        expect(playedSources()).toContain("/sounds/rooms/room-enter-07.wav");
        expect(playedSources()).toContain("/sounds/rooms/room-enter-16.wav");
    });

    test("determineOverallMedal berechnet NONE, BRONZE, SILVER und GOLD", () => {
        const dom = setupDom();

        expect(dom.window.Dummy2ProSound.determineOverallMedal(0, 0)).toBe("NONE");
        expect(dom.window.Dummy2ProSound.determineOverallMedal(2, 4)).toBe("BRONZE");
        expect(dom.window.Dummy2ProSound.determineOverallMedal(3, 4)).toBe("SILVER");
        expect(dom.window.Dummy2ProSound.determineOverallMedal(4, 4)).toBe("GOLD");
    });

    test("playMedal nutzt room- und endscreen-Dateien korrekt", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.playMedal("BRONZE", "room");
        dom.window.Dummy2ProSound.playMedal("SILVER", "endscreen");
        dom.window.Dummy2ProSound.playMedal("NONE", "room");

        expect(playedSources()).toContain("/sounds/medals/medal-bronze.wav");
        expect(playedSources()).toContain("/sounds/medals/endscreen-silver.wav");
        expect(playedSources()).not.toContain("/sounds/medals/medal-gold.wav");
    });

    test("Klick auf room-link wird als Raumsound erkannt", async () => {
        const dom = setupDom();

        await dom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const roomButton = dom.window.document.createElement("button");
        roomButton.className = "room-link";
        roomButton.dataset.roomId = "4";
        roomButton.textContent = "Raum 4";
        dom.window.document.body.appendChild(roomButton);

        roomButton.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));

        await flush(dom, 0);

        expect(playedSources()).toContain("/sounds/rooms/room-enter-04.wav");
    });

    test("Klick auf Dashboard-Button und Darkmode-Button wird erkannt", async () => {
        const dom = setupDom();

        await dom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const homeButton = dom.window.document.createElement("button");
        homeButton.className = "room-home-button";
        homeButton.textContent = "Zum Dashboard";
        dom.window.document.body.appendChild(homeButton);

        const darkButton = dom.window.document.createElement("button");
        darkButton.id = "darkmodeBtn";
        darkButton.textContent = "Light Mode";
        dom.window.document.body.appendChild(darkButton);

        homeButton.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));
        darkButton.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));

        await flush(dom, 0);

        expect(playedSources()).toContain("/sounds/ui/home.wav");
        expect(playedSources()).toContain("/sounds/ui/theme-toggle.wav");
    });

    test("Form submit erkennt Registrierung und Login", async () => {
        const registerDom = setupDom("http://localhost/register.html");
        await registerDom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const registerForm = registerDom.window.document.createElement("form");
        registerForm.innerHTML = `
            <input id="username" />
            <input type="password" />
            <button type="submit">Registrieren</button>
        `;
        registerDom.window.document.body.appendChild(registerForm);

        registerForm.dispatchEvent(new registerDom.window.Event("submit", { bubbles: true, cancelable: true }));
        await flush(registerDom, 0);

        expect(playedSources()).toContain("/sounds/ui/register.wav");

        const loginDom = setupDom("http://localhost/");
        await loginDom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const loginForm = loginDom.window.document.createElement("form");
        loginForm.innerHTML = `
            <input id="username" />
            <input type="password" />
            <button type="submit">Einloggen</button>
        `;
        loginDom.window.document.body.appendChild(loginForm);

        loginForm.dispatchEvent(new loginDom.window.Event("submit", { bubbles: true, cancelable: true }));
        await flush(loginDom, 0);

        expect(playedSources()).toContain("/sounds/ui/login.wav");
    });

    test("Feedback-, Raum-Medaille- und Endscreen-Observer reagieren", async () => {
        const dom = setupDom();

        dom.window.document.getElementById("feedbackBox").textContent = "Richtig! Sehr gut";
        await flush(dom, 0);

        dom.window.document.getElementById("roomStatusBadge").textContent = "GOLD (100%)";
        await flush(dom, 0);

        dom.window.document.getElementById("answeredQuestionsText").textContent = "4/4";
        dom.window.document.getElementById("correctAnswersText").textContent = "4";
        await flush(dom, 0);

        expect(playedSources()).toContain("/sounds/ui/answer-correct.wav");
        expect(playedSources()).toContain("/sounds/medals/medal-gold.wav");
        expect(playedSources()).toContain("/sounds/medals/endscreen-gold.wav");
    });
});