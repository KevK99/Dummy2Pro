const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

function flush(dom, ms = 0) {
    return new Promise(resolve => dom.window.setTimeout(resolve, ms));
}

describe("sound-manager.js weitere Branches", () => {
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
                    <div id="answerContainer"></div>
                </body>
            </html>
        `, url);

        dom.window.localStorage.clear();
        let currentTime = 0;
        // Der künstlich ansteigende Zeitwert macht die Cooldown-Logik
        // deterministisch testbar, ohne echte Wartezeiten einzubauen.
        dom.window.performance.now = () => {
            currentTime += 120;
            return currentTime;
        };
        dom.window.Audio = AudioMock;
        dom.window.console = { ...console, error: jest.fn() };

        loadBrowserScript(dom, "src/main/resources/static/js/sound-manager.js");
        dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));

        clearAudioHistory();
        return dom;
    }

    test("getVolume nutzt 1 bei NaN und setVolume nutzt 0.55 als Fallback", () => {
        const dom = setupDom();

        dom.window.localStorage.setItem("dummy2proSoundVolume", "abc");
        expect(dom.window.Dummy2ProSound.getVolume()).toBe(1);

        dom.window.Dummy2ProSound.setVolume("kaputt");
        expect(dom.window.Dummy2ProSound.getVolume()).toBe(0.55);
    });

    test("playRoomEnter normalisiert ungültige roomIds auf Raum 1", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.playRoomEnter(0);
        dom.window.Dummy2ProSound.playRoomTransition("abc");

        expect(playedSources()).toContain("/sounds/rooms/room-enter-01.wav");
    });

    test("wiederholtes play desselben Sounds wird über Cooldown geblockt", () => {
        const dom = setupDom();
        let now = 1000;
        dom.window.performance.now = () => now;

        dom.window.Dummy2ProSound.play("home");
        dom.window.Dummy2ProSound.play("home");

        expect(playedSources().filter(src => src === "/sounds/ui/home.wav").length).toBe(1);
    });

    test("unknown sound name macht nichts", () => {
        const dom = setupDom();

        dom.window.Dummy2ProSound.play("gibt-es-nicht");

        expect(playedSources()).toEqual([]);
    });

    test("unlock setzt Probe nach erfolgreichem Play zurück", async () => {
        const dom = setupDom();
        const firstAudio = AudioMock.instances.find(instance => instance.src === "/sounds/ui/answer-select.wav");

        await dom.window.Dummy2ProSound.unlock();
        await Promise.resolve();

        expect(firstAudio.pause).toHaveBeenCalled();
        expect(firstAudio.currentTime).toBe(0);
    });

    test("Klicks klassifizieren dialog, review, profile, avatar, name, password, save und delete", async () => {
        const dom = setupDom();
        await dom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        // Die Klickklassifikation im Sound-Manager hängt stark an IDs,
        // Texten und Attributen. Der Helper baut dafür gezielt kleine,
        // variierbare DOM-Buttons auf.
        const makeButton = (attrs = {}, text = "") => {
            const button = dom.window.document.createElement("button");
            Object.entries(attrs).forEach(([key, value]) => {
                if (key === "className") {
                    button.className = value;
                } else if (key === "textContent") {
                    button.textContent = value;
                } else {
                    button.setAttribute(key, value);
                }
            });
            button.textContent = text || button.textContent;
            dom.window.document.body.appendChild(button);
            return button;
        };

        makeButton({ id: "dialogNextBtn" }, "Weiter").click();
        makeButton({ id: "submitAnswerBtn" }, "Antwort prüfen").click();

        const avatarHeader = dom.window.document.createElement("img");
        avatarHeader.id = "headlineAvatar";
        dom.window.document.body.appendChild(avatarHeader);
        avatarHeader.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));

        makeButton({ id: "profilePicBtn" }, "Profilbild").click();
        makeButton({ id: "newNameButton" }, "Umbenennen").click();
        makeButton({ id: "pwFormEnable" }, "Passwort").click();
        makeButton({ id: "createRunBtn" }, "Spielstand hinzufügen").click();
        makeButton({}, "Account löschen").click();

        await flush(dom, 0);

        expect(playedSources()).toEqual(expect.arrayContaining([
            "/sounds/ui/dialog-next.wav",
            "/sounds/ui/check-results.wav",
            "/sounds/ui/profile.wav",
            "/sounds/ui/avatar.wav",
            "/sounds/ui/name-edit.wav",
            "/sounds/ui/password.wav",
            "/sounds/ui/save.wav",
            "/sounds/ui/delete.wav"
        ]));
    });

    test("nextQuestion-Button erkennt Raumwechsel, Dashboard und normale nächste Frage", async () => {
        const dom = setupDom();
        await dom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const toRoom = dom.window.document.createElement("button");
        toRoom.id = "nextQuestionBtn";
        toRoom.textContent = "Weiter zu Raum 8";
        dom.window.document.body.appendChild(toRoom);

        const toDashboard = dom.window.document.createElement("button");
        toDashboard.id = "nextQuestionBtn";
        toDashboard.textContent = "Zurück zum Dashboard";
        dom.window.document.body.appendChild(toDashboard);

        const next = dom.window.document.createElement("button");
        next.id = "nextQuestionBtn";
        next.textContent = "Nächste Frage";
        dom.window.document.body.appendChild(next);

        toRoom.click();
        toDashboard.click();
        next.click();
        await flush(dom, 0);

        expect(playedSources()).toEqual(expect.arrayContaining([
            "/sounds/rooms/room-enter-08.wav",
            "/sounds/ui/home.wav",
            "/sounds/ui/next-question.wav"
        ]));
    });

    test("Form submit ohne Username oder Passwort spielt nichts", async () => {
        const dom = setupDom();
        await dom.window.Dummy2ProSound.unlock();
        clearAudioHistory();

        const form = dom.window.document.createElement("form");
        form.innerHTML = `<input id="username" />`;
        dom.window.document.body.appendChild(form);
        form.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));
        await flush(dom, 0);

        expect(playedSources()).toEqual([]);
    });

    test("Feedback- und Medaillen-Observer ignorieren leere oder gleiche Werte", async () => {
        const dom = setupDom();

        const feedback = dom.window.document.getElementById("feedbackBox");
        const badge = dom.window.document.getElementById("roomStatusBadge");
        const answered = dom.window.document.getElementById("answeredQuestionsText");
        const correct = dom.window.document.getElementById("correctAnswersText");

        feedback.textContent = "";
        badge.textContent = "NONE";
        answered.textContent = "keine Zahl";
        correct.textContent = "x";
        await flush(dom, 0);

        feedback.textContent = "Richtig";
        await flush(dom, 0);
        feedback.textContent = "Richtig";
        await flush(dom, 0);

        badge.textContent = "SILVER";
        await flush(dom, 0);
        badge.textContent = "SILVER";
        await flush(dom, 0);

        answered.textContent = "2/4";
        correct.textContent = "2";
        await flush(dom, 0);
        answered.textContent = "2/4";
        correct.textContent = "2";
        await flush(dom, 0);

        expect(playedSources()).toContain("/sounds/ui/answer-correct.wav");
        expect(playedSources().filter(src => src === "/sounds/ui/answer-correct.wav").length).toBe(1);
        expect(playedSources()).toContain("/sounds/medals/medal-silver.wav");
        expect(playedSources().filter(src => src === "/sounds/medals/medal-silver.wav").length).toBe(1);
        expect(playedSources()).not.toContain("/sounds/medals/endscreen-gold.wav");
    });
});