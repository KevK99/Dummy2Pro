const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("room-common.js helper-Funktionen", () => {
    function setupDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <span id="headlineUsername"></span>
                    <span id="headlineAnsweredQuestions"></span>
                    <span id="headlineTotalQuestions"></span>
                    <img id="headlineAvatar" />
                </body>
            </html>
        `);

        loadBrowserScript(dom, "src/main/resources/static/js/room-common.js");
        return dom;
    }

    test("normalizeMedalValue normalisiert Werte korrekt", () => {
        const dom = setupDom();

        expect(dom.window.normalizeMedalValue(null)).toBe("NONE");
        expect(dom.window.normalizeMedalValue("gold")).toBe("GOLD");
        expect(dom.window.normalizeMedalValue("silver")).toBe("SILVER");
    });

    test("countWords zählt Wörter korrekt", () => {
        const dom = setupDom();

        expect(dom.window.countWords("eins zwei drei")).toBe(3);
        expect(dom.window.countWords("  eins   zwei   ")).toBe(2);
        expect(dom.window.countWords("")).toBe(0);
    });

    test("buildAnswerPreviewText baut Vorschautext mit Kürzung", () => {
        const dom = setupDom();

        expect(dom.window.buildAnswerPreviewText(0, "Hallo Welt")).toBe("A) Hallo Welt");
        expect(dom.window.buildAnswerPreviewText(1, "eins zwei drei vier fünf sechs"))
            .toBe("B) eins zwei drei vier ...");
        expect(dom.window.buildAnswerPreviewText(2, "")).toBe("C)");
    });

    test("getStandardQuestionMetrics berechnet Standard-Fragewerte", () => {
        const dom = setupDom();

        const metrics = dom.window.getStandardQuestionMetrics({
            startText: "Was ist Java?",
            endText: "Wähle die richtige Antwort.",
            answerOptions: [{}, {}, {}, {}],
            imageUrl: "/img/test.png"
        });

        expect(metrics.optionCount).toBe(4);
        expect(metrics.rowCount).toBe(2);
        expect(metrics.hasImage).toBe(true);
        expect(metrics.textLength).toBeGreaterThan(0);
    });

    test("getGapMetrics berechnet Gap-Fragewerte", () => {
        const dom = setupDom();

        const metrics = dom.window.getGapMetrics({
            startText: "Fülle aus",
            endText: "bitte",
            gapFields: [
                {
                    textBefore: "Java ist",
                    textAfter: "Programmiersprache",
                    gapOptions: [{}, {}, {}, {}, {}, {}]
                },
                {
                    textBefore: "Spring ist",
                    textAfter: "Framework",
                    gapOptions: [{}, {}]
                }
            ]
        });

        expect(metrics.gapCount).toBe(2);
        expect(metrics.maxOptionCount).toBe(6);
        expect(metrics.rowCount).toBe(3);
        expect(metrics.textLength).toBeGreaterThan(0);
    });

    test("updateHeadlineOverview setzt Username, Avatar und summierte Werte", () => {
        const dom = setupDom();

        dom.window.sessionStorage.setItem("username", "Jan");
        dom.window.sessionStorage.setItem("avatar", "duck.jpg");

        dom.window.updateHeadlineOverview({
            rooms: [
                { answeredQuestions: 2, totalQuestions: 5 },
                { answeredQuestions: 3, totalQuestions: 7 },
                { answeredQuestions: 1, totalQuestions: 4 }
            ]
        });

        expect(dom.window.document.getElementById("headlineUsername").textContent).toBe("Jan");
        expect(dom.window.document.getElementById("headlineAnsweredQuestions").textContent).toBe("6");
        expect(dom.window.document.getElementById("headlineTotalQuestions").textContent).toBe("16");
        expect(dom.window.document.getElementById("headlineAvatar").src).toBe("http://localhost/images/duck.jpg");
    });
});