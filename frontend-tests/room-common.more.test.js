const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

function flush(dom, ms = 0) {
    return new Promise(resolve => dom.window.setTimeout(resolve, ms));
}

describe("room-common.js weitere Methodenpfade", () => {
    function setupDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="feedbackBox"></div>
                    <div id="questionText"></div>
                    <div id="answerContainer"></div>
                    <div id="gameScene" style="height: 680px;"></div>
                    <div id="questionPanel" style="height: 520px;"></div>
                    <div id="statusPanel"></div>
                    <div id="gameRoomCharacter"></div>
                    <div id="gamePanda"></div>
                    <div id="speechTail"></div>
                    <div id="speechPanel"></div>
                    <div id="playerCharacter"></div>
                    <div id="welcomeContainer"></div>
                    <div id="gameContainer" class="hidden"></div>

                    <div id="earnedPoints"></div>
                    <div id="questionProgress"></div>
                    <div id="themeProgressBar"></div>
                    <div id="themeProgressText"></div>
                    <div id="questionCorrectBar"></div>
                    <div id="questionWrongBar"></div>
                    <div id="correctAnswersText"></div>
                    <div id="wrongAnswersText"></div>
                    <div id="roomStatusBadge"></div>

                    <div id="medalContainer" class="hidden"></div>
                    <div id="medalCoin"></div>

                    <span id="headlineUsername"></span>
                    <span id="headlineAnsweredQuestions"></span>
                    <span id="headlineTotalQuestions"></span>
                    <img id="headlineAvatar" />
                </body>
            </html>
        `);

        const ids = [
            "feedbackBox",
            "questionText",
            "answerContainer",
            "gameScene",
            "questionPanel",
            "statusPanel",
            "gameRoomCharacter",
            "gamePanda",
            "speechTail",
            "speechPanel",
            "playerCharacter",
            "welcomeContainer",
            "gameContainer",
            "medalContainer",
            "medalCoin"
        ];

        // room-common.js greift im echten Raumskript teils direkt auf
        // globale Referenzen zu. Für die Testumgebung werden die relevanten
        // DOM-Knoten daher explizit auf window abgelegt.
        ids.forEach(id => {
            dom.window[id] = dom.window.document.getElementById(id);
        });

        dom.window.sessionId = "sess-1";
        dom.window.roomId = 3;
        dom.window.roomLoaded = false;
        dom.window.currentQuestion = null;
        dom.window.dialogIndex = 0;
        dom.window.introDialog = [];
        dom.window.showCurrentDialogLine = jest.fn();
        dom.window.renderQuestion = jest.fn();
        dom.window.showRoomCompletedState = jest.fn();
        dom.window.alert = jest.fn();
        dom.window.Dummy2ProSound = {
            play: jest.fn(),
            playMedal: jest.fn()
        };
        dom.window.console = {
            ...console,
            error: jest.fn()
        };

        loadBrowserScript(dom, "src/main/resources/static/js/room-common.js");
        return dom;
    }

    test("Feedback-Observer spielt correct und wrong nur bei Änderung", async () => {
        const dom = setupDom();
        const feedbackBox = dom.window.document.getElementById("feedbackBox");

        feedbackBox.textContent = "Start";
        await flush(dom, 0);
        feedbackBox.textContent = "Richtig! Genau so";
        await flush(dom, 0);
        feedbackBox.textContent = "Falsch! Leider nein";
        await flush(dom, 0);

        expect(dom.window.Dummy2ProSound.play).toHaveBeenCalledWith("correct");
        expect(dom.window.Dummy2ProSound.play).toHaveBeenCalledWith("wrong");
    });

    test("refreshHeadlineOverview nutzt Fallback ohne sessionId und bei Fehlern", async () => {
        const dom = setupDom();
        dom.window.sessionStorage.setItem("username", "Jan");
        dom.window.sessionStorage.setItem("avatar", "duck.jpg");

        dom.window.sessionId = "";
        await dom.window.refreshHeadlineOverview();

        expect(dom.window.document.getElementById("headlineUsername").textContent).toBe("Jan");
        expect(dom.window.document.getElementById("headlineAvatar").src).toBe("http://localhost/images/duck.jpg");

        dom.window.sessionId = "sess-1";
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: false,
            json: async () => ({})
        });

        await dom.window.refreshHeadlineOverview();
        expect(dom.window.fetch).toHaveBeenCalledWith("/api/session/sess-1/overview");

        dom.window.fetch = jest.fn().mockRejectedValue(new Error("boom"));
        await dom.window.refreshHeadlineOverview();
        expect(dom.window.console.error).toHaveBeenCalled();
    });

    test("refreshHeadlineOverview übernimmt Daten aus erfolgreicher API-Antwort", async () => {
        const dom = setupDom();
        dom.window.sessionStorage.setItem("username", "Mira");
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                rooms: [
                    { answeredQuestions: 2, totalQuestions: 5 },
                    { answeredQuestions: 4, totalQuestions: 6 }
                ]
            })
        });

        await dom.window.refreshHeadlineOverview();

        expect(dom.window.document.getElementById("headlineAnsweredQuestions").textContent).toBe("6");
        expect(dom.window.document.getElementById("headlineTotalQuestions").textContent).toBe("11");
    });

    test("syncQuestionSideLayout positioniert Status, Charakter und Panda", () => {
        const dom = setupDom();
        dom.window.questionPanel.style.height = "600px";
        dom.window.gameScene.style.height = "800px";

        dom.window.syncQuestionSideLayout();

        expect(dom.window.statusPanel.style.height).toBe("600px");
        expect(dom.window.statusPanel.style.bottom).toBe("30px");
        expect(dom.window.gameRoomCharacter.style.top).toBe("20px");
        expect(dom.window.gamePanda.style.top).toBe("-15px");
    });

    test("nextDialogLine erhöht Index nur bis zum Ende und startQuiz blendet um", () => {
        const dom = setupDom();
        dom.window.introDialog = [{}, {}];
        dom.window.dialogIndex = 0;

        dom.window.nextDialogLine();
        dom.window.nextDialogLine();

        expect(dom.window.dialogIndex).toBe(1);
        expect(dom.window.showCurrentDialogLine).toHaveBeenCalledTimes(1);

        dom.window.startQuiz();
        expect(dom.window.welcomeContainer.classList.contains("hidden")).toBe(true);
        expect(dom.window.gameContainer.classList.contains("hidden")).toBe(false);
    });

    test("fitQuestionText und fitGapQuestionText verkleinern Schrift bis passend", () => {
        const dom = setupDom();

        Object.defineProperty(dom.window.questionText, "scrollHeight", { configurable: true, get: () => 100 });
        Object.defineProperty(dom.window.questionText, "clientHeight", { configurable: true, get: () => 20 });
        dom.window.fitQuestionText();
        expect(parseInt(dom.window.questionText.style.fontSize, 10)).toBe(8);

        dom.window.questionText.innerHTML = "<div>Lang</div>";
        Object.defineProperty(dom.window.questionText, "scrollHeight", { configurable: true, get: () => 100 });
        Object.defineProperty(dom.window.questionText, "clientHeight", { configurable: true, get: () => 20 });
        dom.window.fitGapQuestionText();
        expect(parseInt(dom.window.questionText.firstElementChild.style.fontSize, 10)).toBe(8);
    });

    test("refreshRoomStatus aktualisiert Status und Headline nur bei ok", async () => {
        const dom = setupDom();
        const updateStatusSpy = jest.spyOn(dom.window, "updateStatus").mockImplementation(() => {});
        const refreshHeadlineSpy = jest.spyOn(dom.window, "refreshHeadlineOverview").mockResolvedValue();

        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ earnedPoints: 5, totalPoints: 10 })
        });

        await dom.window.refreshRoomStatus();
        expect(updateStatusSpy).toHaveBeenCalled();
        expect(refreshHeadlineSpy).toHaveBeenCalled();

        updateStatusSpy.mockClear();
        refreshHeadlineSpy.mockClear();
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: false,
            json: async () => ({})
        });

        await dom.window.refreshRoomStatus();
        expect(updateStatusSpy).not.toHaveBeenCalled();
        expect(refreshHeadlineSpy).not.toHaveBeenCalled();

        dom.window.fetch = jest.fn().mockRejectedValue(new Error("kaputt"));
        await dom.window.refreshRoomStatus();
        expect(dom.window.console.error).toHaveBeenCalled();
    });

    test("escapeHtml escaped Sonderzeichen korrekt", () => {
        const dom = setupDom();
        expect(dom.window.escapeHtml(`<a href='x'>&"</a>`)).toBe("&lt;a href=&#039;x&#039;&gt;&amp;&quot;&lt;/a&gt;");
    });

    test("loadRoom zeigt Completed-State wenn keine Frage mehr da ist", async () => {
        const dom = setupDom();
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                firstQuestion: null,
                status: { earnedPoints: 10, totalPoints: 10, answeredQuestions: 4, totalQuestions: 4, correctAnswers: 4, medal: "GOLD" }
            })
        });
        jest.spyOn(dom.window, "refreshHeadlineOverview").mockResolvedValue();

        const result = await dom.window.loadRoom();

        expect(result).toBe(true);
        expect(dom.window.showRoomCompletedState).toHaveBeenCalled();
        expect(dom.window.welcomeContainer.classList.contains("hidden")).toBe(true);
        expect(dom.window.gameContainer.classList.contains("hidden")).toBe(false);
    });

    test("loadRoom blendet direkt ins Spiel wenn bereits Fragen beantwortet wurden", async () => {
        const dom = setupDom();
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                firstQuestion: { questionId: 7 },
                status: { earnedPoints: 5, totalPoints: 10, answeredQuestions: 2, totalQuestions: 4, correctAnswers: 1, medal: "NONE" }
            })
        });
        jest.spyOn(dom.window, "refreshHeadlineOverview").mockResolvedValue();

        const result = await dom.window.loadRoom();

        expect(result).toBe(true);
        expect(dom.window.renderQuestion).toHaveBeenCalledWith({ questionId: 7 });
        expect(dom.window.welcomeContainer.classList.contains("hidden")).toBe(true);
        expect(dom.window.gameContainer.classList.contains("hidden")).toBe(false);
    });

    test("loadRoom startet Intro, wenn noch nichts beantwortet wurde", async () => {
        const dom = setupDom();
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                firstQuestion: { questionId: 8 },
                introDialog: [{ speaker: "npc", text: "Hallo" }],
                status: { earnedPoints: 0, totalPoints: 10, answeredQuestions: 0, totalQuestions: 4, correctAnswers: 0, medal: "NONE" }
            })
        });
        jest.spyOn(dom.window, "refreshHeadlineOverview").mockResolvedValue();
        const animateSpy = jest.spyOn(dom.window, "animatePlayerEntrance").mockImplementation(() => {});
        dom.window.setTimeout = fn => {
            fn();
            return 1;
        };

        const result = await dom.window.loadRoom();

        expect(result).toBe(true);
        expect(animateSpy).toHaveBeenCalled();
        expect(dom.window.showCurrentDialogLine).toHaveBeenCalled();
        expect(dom.window.introDialog).toEqual([{ speaker: "npc", text: "Hallo" }]);
    });

    test("loadRoom behandelt Fetch-Fehler und initWelcomeRoom setzt roomLoaded", async () => {
        const dom = setupDom();
        dom.window.fetch = jest.fn().mockRejectedValue(new Error("netz"));

        const loaded = await dom.window.loadRoom();
        expect(loaded).toBe(false);
        expect(dom.window.alert).toHaveBeenCalledWith("Raum konnte nicht geladen werden.");

        dom.window.loadRoom = jest.fn().mockResolvedValue(true);
        await dom.window.initWelcomeRoom();
        expect(dom.window.roomLoaded).toBe(true);
    });

    test("updateMedalCoin behandelt NONE, BRONZE, SILVER, GOLD und fehlende Elemente", () => {
        const dom = setupDom();

        dom.window.updateMedalCoin("BRONZE");
        expect(dom.window.medalCoin.innerText).toBe("BRONZE");
        expect(dom.window.medalContainer.classList.contains("hidden")).toBe(false);

        dom.window.updateMedalCoin("SILVER");
        expect(dom.window.medalCoin.innerText).toBe("SILBER");

        dom.window.updateMedalCoin("GOLD");
        expect(dom.window.medalCoin.innerText).toBe("GOLD");

        dom.window.updateMedalCoin("NONE");
        expect(dom.window.medalContainer.classList.contains("hidden")).toBe(true);

        dom.window.document.getElementById("medalContainer").remove();
        expect(() => dom.window.updateMedalCoin("GOLD")).not.toThrow();
    });
});