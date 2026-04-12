const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

function flush(dom, ms = 0) {
    return new Promise(resolve => dom.window.setTimeout(resolve, ms));
}

describe("room-common.js weitere Branches", () => {
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

        // room-common.js arbeitet an mehreren Stellen mit globalen
        // Elementreferenzen aus dem Template. Im Test werden diese Knoten
        // deshalb bewusst zusätzlich auf window gespiegelt.
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

    test("isGapQuestion erkennt normale und GAP-Fragen korrekt", () => {
        const dom = setupDom();

        expect(dom.window.isGapQuestion(null)).toBeNull();
        expect(dom.window.isGapQuestion({ questionType: "MC" })).toBeUndefined();
        expect(dom.window.isGapQuestion({ questionType: "GAP" })).toBe(true);
        expect(dom.window.isGapQuestion({ gapFields: [{}] })).toBe(true);
    });

    test("renderStandardQuestionText zeigt Fallback ohne Inhalte", () => {
        const dom = setupDom();

        dom.window.renderStandardQuestionText({}, "base");

        expect(dom.window.questionText.innerText).toBe("Keine Frage vorhanden.");
    });

    test("renderStandardQuestionText rendert Bild und entfernt es bei onerror", () => {
        const dom = setupDom();

        dom.window.renderStandardQuestionText({
            startText: "Start",
            imageUrl: "/img/test.png",
            endText: "Ende"
        }, "base");

        const image = dom.window.questionText.querySelector("img");
        expect(image).not.toBeNull();

        image.onerror();

        expect(dom.window.questionText.querySelector("img")).toBeNull();
    });

    test("applyQuestionLayout deckelt Standard-Fragen nach oben", async () => {
        const dom = setupDom();

        dom.window.applyQuestionLayout({
            startText: "A ".repeat(220),
            endText: "B ".repeat(220),
            answerOptions: new Array(10).fill({})
        });

        await flush(dom, 0);

        expect(dom.window.questionPanel.style.height).toBe("720px");
        expect(dom.window.gameScene.style.height).toBe("800px");
        expect(dom.window.questionText.style.maxHeight).toBe("320px");
        expect(dom.window.statusPanel.style.height).toBe("720px");
    });

    test("applyQuestionLayout deckelt GAP-Fragen nach oben", async () => {
        const dom = setupDom();

        dom.window.applyQuestionLayout({
            questionType: "GAP",
            startText: "A ".repeat(180),
            endText: "B ".repeat(180),
            gapFields: [
                { textBefore: "1", textAfter: "a", gapOptions: new Array(9).fill({}) },
                { textBefore: "2", textAfter: "b", gapOptions: new Array(9).fill({}) },
                { textBefore: "3", textAfter: "c", gapOptions: new Array(9).fill({}) },
                { textBefore: "4", textAfter: "d", gapOptions: new Array(9).fill({}) }
            ]
        });

        await flush(dom, 0);

        expect(dom.window.questionPanel.style.height).toBe("690px");
        expect(dom.window.gameScene.style.height).toBe("770px");
        expect(dom.window.questionText.style.maxHeight).toBe("230px");
    });

    test("updateSpeechTailPosition setzt Klassen für Spieler und NPC", () => {
        const dom = setupDom();

        dom.window.updateSpeechTailPosition("player");
        expect(dom.window.speechTail.classList.contains("left-[18%]")).toBe(true);
        expect(dom.window.speechPanel.classList.contains("-translate-x-[50%]")).toBe(true);

        dom.window.updateSpeechTailPosition("npc");
        expect(dom.window.speechTail.classList.contains("left-[82%]")).toBe(true);
        expect(dom.window.speechPanel.classList.contains("-translate-x-[34%]")).toBe(true);
    });

    test("fitAnswerTexts setzt Tooltip bei langen Antworten", () => {
        const dom = setupDom();

        dom.window.answerContainer.innerHTML = `
            <button class="quiz-answer-btn" data-full-text="eins zwei drei vier fünf">
                <span class="answer-label">eins zwei drei vier fünf</span>
            </button>
        `;

        const label = dom.window.answerContainer.querySelector(".answer-label");
        const button = dom.window.answerContainer.querySelector(".quiz-answer-btn");

        Object.defineProperty(label, "scrollHeight", { configurable: true, get: () => 80 });
        Object.defineProperty(label, "clientHeight", { configurable: true, get: () => 20 });

        dom.window.fitAnswerTexts();

        expect(button.classList.contains("has-answer-tooltip")).toBe(true);
        expect(button.getAttribute("data-tooltip")).toBe("eins zwei drei vier fünf");
    });

    test("fitAnswerTexts entfernt Tooltip bei kurzen Antworten", () => {
        const dom = setupDom();

        dom.window.answerContainer.innerHTML = `
            <button class="quiz-answer-btn has-answer-tooltip" data-full-text="kurz" data-tooltip="alt">
                <span class="answer-label">kurz</span>
            </button>
        `;

        const label = dom.window.answerContainer.querySelector(".answer-label");
        const button = dom.window.answerContainer.querySelector(".quiz-answer-btn");

        Object.defineProperty(label, "scrollHeight", { configurable: true, get: () => 20 });
        Object.defineProperty(label, "clientHeight", { configurable: true, get: () => 20 });

        dom.window.fitAnswerTexts();

        expect(button.classList.contains("has-answer-tooltip")).toBe(false);
        expect(button.hasAttribute("data-tooltip")).toBe(false);
    });

    test("fitGapOptionTexts setzt und entfernt Tooltip passend", () => {
        const dom = setupDom();

        dom.window.answerContainer.innerHTML = `
            <button class="gap-option-btn" data-option-text="Dies ist eine ziemlich lange GAP-Option für Tooltip">
                <span class="gap-option-label">Lang</span>
            </button>
            <button class="gap-option-btn has-answer-tooltip" data-option-text="Kurz" data-tooltip="alt">
                <span class="gap-option-label">Kurz</span>
            </button>
        `;

        const labels = dom.window.answerContainer.querySelectorAll(".gap-option-label");
        const buttons = dom.window.answerContainer.querySelectorAll(".gap-option-btn");

        Object.defineProperty(labels[0], "scrollHeight", { configurable: true, get: () => 60 });
        Object.defineProperty(labels[0], "clientHeight", { configurable: true, get: () => 20 });

        Object.defineProperty(labels[1], "scrollHeight", { configurable: true, get: () => 20 });
        Object.defineProperty(labels[1], "clientHeight", { configurable: true, get: () => 20 });

        dom.window.fitGapOptionTexts();

        expect(buttons[0].classList.contains("has-answer-tooltip")).toBe(true);
        expect(buttons[0].getAttribute("data-tooltip")).toContain("ziemlich lange GAP-Option");

        expect(buttons[1].classList.contains("has-answer-tooltip")).toBe(false);
        expect(buttons[1].hasAttribute("data-tooltip")).toBe(false);
    });

    test("updateStatus berechnet Prozentwerte und leitet wrongAnswers her", () => {
        const dom = setupDom();

        dom.window.updateStatus({
            earnedPoints: 25,
            totalPoints: 100,
            answeredQuestions: 1,
            totalQuestions: 4,
            correctAnswers: 1,
            medal: "NONE"
        });

        expect(dom.window.document.getElementById("earnedPoints").innerText).toBe("25/100");
        expect(dom.window.document.getElementById("questionProgress").innerText).toBe("1/4");
        expect(dom.window.document.getElementById("themeProgressBar").style.width).toBe("25%");
        expect(dom.window.document.getElementById("questionCorrectBar").style.width).toBe("25%");
        expect(dom.window.document.getElementById("questionWrongBar").style.width).toBe("0%");
        expect(dom.window.document.getElementById("roomStatusBadge").innerText).toBe("IN PROGRESS (25%)");
        expect(dom.window.Dummy2ProSound.playMedal).not.toHaveBeenCalled();
    });

    test("updateStatus spielt Medaillensound erst beim Wechsel", () => {
        const dom = setupDom();

        dom.window.updateStatus({
            earnedPoints: 50,
            totalPoints: 100,
            answeredQuestions: 2,
            totalQuestions: 4,
            correctAnswers: 2,
            wrongAnswers: 0,
            medal: "BRONZE"
        });

        expect(dom.window.Dummy2ProSound.playMedal).not.toHaveBeenCalled();

        dom.window.updateStatus({
            earnedPoints: 100,
            totalPoints: 100,
            answeredQuestions: 4,
            totalQuestions: 4,
            correctAnswers: 4,
            wrongAnswers: 0,
            medal: "GOLD"
        });

        expect(dom.window.Dummy2ProSound.playMedal).toHaveBeenCalledWith("GOLD", "room");
    });

    test("animatePlayerEntrance setzt Zielklassen nach zwei Frames", async () => {
        const dom = setupDom();

        dom.window.animatePlayerEntrance();

        await flush(dom, 0);
        await flush(dom, 0);

        expect(dom.window.playerCharacter.classList.contains("left-[180px]")).toBe(true);
        expect(dom.window.playerCharacter.classList.contains("bottom-[95px]")).toBe(true);
    });
});