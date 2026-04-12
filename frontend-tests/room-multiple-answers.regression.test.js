const fs = require("fs");
const path = require("path");
const { createBrowserEnv } = require("./helpers/browser-env");

function extractFunction(relativePath, functionSignature) {
    const absolutePath = path.join(process.cwd(), relativePath);
    const source = fs.readFileSync(absolutePath, "utf8");
    const start = source.indexOf(functionSignature);

    if (start === -1) {
        throw new Error(`Function not found: ${functionSignature}`);
    }

    let depth = 0;
    let startedBody = false;

    for (let index = start; index < source.length; index++) {
        const char = source[index];

        if (char === "{") {
            depth++;
            startedBody = true;
        } else if (char === "}") {
            depth--;
            if (startedBody && depth === 0) {
                return source.slice(start, index + 1);
            }
        }
    }

    throw new Error(`Could not extract function body: ${functionSignature}`);
}

describe("room1 Mehrfachantworten Regression", () => {
    function setupDom(answerResponse) {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="questionText"></div>
                    <div id="answerContainer"></div>
                    <div id="feedbackBox"></div>
                    <button id="submitAnswerBtn" class="hidden"></button>
                    <button id="nextQuestionBtn" class="hidden"></button>
                </body>
            </html>
        `);

        dom.window.console = { ...console, error: jest.fn() };
        dom.window.alert = jest.fn();
        dom.window.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => answerResponse
        });

        const renderQuestion = extractFunction(
            "src/main/resources/templates/room1.html",
            "function renderQuestion(question)"
        );
        const toggleAnswer = extractFunction(
            "src/main/resources/templates/room1.html",
            "function toggleAnswer(answerId, buttonElement, allowsMultiple)"
        );
        const submitAnswer = extractFunction(
            "src/main/resources/templates/room1.html",
            "async function submitAnswer()"
        );

        dom.window.eval(`
            var sessionId = "sess-1";
            var roomId = 1;
            var currentQuestion = null;
            var selectedAnswerIds = [];
            var selectedGapAnswers = [];
            var selectedGapAnswersMap = {};
            var currentGapStep = 0;
            var questionText = document.getElementById("questionText");
            var answerContainer = document.getElementById("answerContainer");
            var feedbackBox = document.getElementById("feedbackBox");
            var submitAnswerBtn = document.getElementById("submitAnswerBtn");
            var nextQuestionBtn = document.getElementById("nextQuestionBtn");
            const gameContainer = {
                classList: {
                    contains(className) {
                        return className === "hidden";
                    }
                }
            };

        function isGapQuestion() { return false; }
        function applyQuestionLayout() {}
        function renderStandardQuestionText() {}
        function buildAnswerPreviewText(index, fullText) { return String.fromCharCode(65 + index) + ") " + fullText; }
        function fitQuestionText() {}
        function fitAnswerTexts() {}
        function clearRoomFeedbackBox(box) {
            if (!box) {
                return;
            }
            box.innerText = "";
            box.className = "hidden";
            box.style.display = "none";
        }
        function showRoomFeedbackBox(box, text, type) {
            if (!box) {
                return;
            }
            box.innerText = text;
            box.className = type === "success"
                ? "feedback-success"
                : "feedback-error";
            box.style.display = "block";
        }
        function buildRoomWrongFeedbackText(roomId, pointsEarned) {
            return \`Falsch! (+\${Number(pointsEarned ?? 0)} Punkt(e))\`;
        }
        function getRoomWrongFeedbackLine() {
            return "Test-Kommentar";
        }
        function hidePandaGameComment() {}
        function showPandaGameComment() {}
        async function refreshRoomStatus() {}
        ${renderQuestion}
        ${toggleAnswer}
        ${submitAnswer}
            `);

            return dom;
    }

    test("Mehrfachauswahl markiert nur die exakt gewählten Buttons als selektiert", () => {
        const dom = setupDom({
            correct: false,
            pointsEarned: 0,
            correctAnswerIds: [1, 2]
        });

        const question = {
            questionId: 77,
            allowsMultiple: true,
            startText: "Welche Antworten sind richtig?",
            answerOptions: [
                { answerId: 1, optionText: "Antwort A" },
                { answerId: 2, optionText: "Antwort B" },
                { answerId: 3, optionText: "Antwort C" }
            ]
        };

        dom.window.currentQuestion = question;
        dom.window.renderQuestion(question);

        const buttons = dom.window.document.querySelectorAll(".quiz-answer-btn");
        buttons[0].click();
        buttons[1].click();

        expect(buttons[0].className).toContain("bg-yellow-400");
        expect(buttons[1].className).toContain("bg-yellow-400");
        expect(buttons[2].className).not.toContain("bg-yellow-400");
        expect(dom.window.document.getElementById("submitAnswerBtn").classList.contains("hidden")).toBe(false);
    });

    test("Antwortprüfung wertet Mehrfachantworten als richtig, sobald mindestens eine richtige Antwort gewählt wurde", async () => {
        const dom = setupDom({
            correct: true,
            pointsEarned: 5,
            correctAnswerIds: [1, 2]
        });

        const question = {
            questionId: 88,
            allowsMultiple: true,
            startText: "Welche Antworten sind richtig?",
            points: 5,
            answerOptions: [
                { answerId: 1, optionText: "Antwort A" },
                { answerId: 2, optionText: "Antwort B" },
                { answerId: 3, optionText: "Antwort C" }
            ]
        };

        dom.window.currentQuestion = question;
        dom.window.renderQuestion(question);

        const buttons = [...dom.window.document.querySelectorAll(".quiz-answer-btn")];
        buttons[0].click();
        buttons[2].click();

        await dom.window.submitAnswer();

        expect(dom.window.fetch).toHaveBeenCalledTimes(1);
        expect(dom.window.document.getElementById("feedbackBox").innerText).toContain("Richtig!");
        expect(dom.window.document.getElementById("nextQuestionBtn").classList.contains("hidden")).toBe(false);
    });
});