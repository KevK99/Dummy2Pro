const { createBrowserEnv, loadBrowserScript } = require("./helpers/browser-env");

describe("room-common.js Avatar-Rahmen und Panda-Sprechblase", () => {
    function setupAvatarDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="avatarHost">
                        <img id="avatarImage" src="/images/duck.jpg" />
                    </div>

                    <div id="feedbackBox"></div>
                </body>
            </html>
        `);

        loadBrowserScript(dom, "src/main/resources/static/js/room-common.js");
        return dom;
    }

    function setupSpeechDom() {
        const dom = createBrowserEnv(`
            <!doctype html>
            <html>
                <body>
                    <div id="feedbackBox"></div>

                    <div id="welcomeContainer">
                        <div id="welcomeCharacterWrapper">
                            <img src="/room-characters/welcome.png" />
                        </div>

                        <div id="speechPanel" class="-translate-x-[34%]">
                            <div id="speechShell">
                                <div class="corner-a"></div>
                                <div id="dialogWrapper">
                                    <div id="dialogText"></div>
                                </div>
                                <div id="speechInnerStroke"></div>
                            </div>
                        </div>

                        <div id="welcomePanda"></div>
                    </div>

                    <div id="gameContainer">
                        <div id="gameScene">
                            <div id="gameCharacterWrapper">
                                <img src="/room-characters/game.png" />
                            </div>

                            <div id="gamePanda"></div>
                            <div id="gameRoomCharacter"></div>
                        </div>
                    </div>

                    <div id="speechTail" class="left-[82%]"></div>
                </body>
            </html>
        `);

        const gameScene = dom.window.document.getElementById("gameScene");
        const gamePanda = dom.window.document.getElementById("gamePanda");

        Object.defineProperty(gameScene, "clientWidth", {
            configurable: true,
            get: () => 1000
        });

        gameScene.getBoundingClientRect = () => ({
            left: 0,
            top: 0,
            width: 1000,
            height: 700,
            right: 1000,
            bottom: 700
        });

        gamePanda.getBoundingClientRect = () => ({
            left: 100,
            top: 200,
            width: 80,
            height: 100,
            right: 180,
            bottom: 300
        });

        loadBrowserScript(dom, "src/main/resources/static/js/room-common.js");
        return dom;
    }

    afterEach(() => {
        jest.useRealTimers();
    });

    test("applyAvatarStyleToElement erzeugt Wrapper, Form und Frame korrekt", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "hexagon");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "blue");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-28 h-28");

        const wrapper = avatarImage.parentElement;

        expect(wrapper).not.toBeNull();
        expect(wrapper.classList.contains("avatar-frame-shell")).toBe(true);
        expect(wrapper.className).toContain("w-28");
        expect(wrapper.className).toContain("h-28");
        expect(wrapper.className).toContain("bg-blue-500");
        expect(wrapper.className).toContain("[clip-path:polygon(25%_6%,75%_6%,100%_50%,75%_94%,25%_94%,0%_50%)]");

        expect(avatarImage.className).toContain("object-cover");
        expect(avatarImage.className).toContain("[clip-path:polygon(25%_6%,75%_6%,100%_50%,75%_94%,25%_94%,0%_50%)]");
    });

    test("applyAvatarStyleToElement erzeugt beim zweiten Aufruf keinen doppelten Wrapper", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "diamond");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "gold");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");
        const firstWrapper = avatarImage.parentElement;

        dom.window.applyAvatarStyleToElement(avatarImage, "w-24 h-24");
        const secondWrapper = avatarImage.parentElement;

        expect(firstWrapper).toBe(secondWrapper);
        expect(dom.window.document.querySelectorAll(".avatar-frame-shell")).toHaveLength(1);
        expect(secondWrapper.className).toContain("w-24");
        expect(secondWrapper.className).toContain("h-24");
        expect(secondWrapper.className).toContain("bg-[#ffd700]");
        expect(secondWrapper.className).toContain("[clip-path:polygon(50%_0%,100%_50%,50%_100%,0%_50%)]");
    });

    test("showPandaGameComment setzt Text, macht die Blase sichtbar und verschiebt sie in die gameScene", () => {
        const dom = setupSpeechDom();

        dom.window.showPandaGameComment("Fast. Beim nächsten Mal klappt’s bestimmt.");

        const speechPanel = dom.window.document.getElementById("speechPanel");
        const speechTail = dom.window.document.getElementById("speechTail");
        const dialogText = dom.window.document.getElementById("dialogText");
        const dialogWrapper = dom.window.document.getElementById("dialogWrapper");
        const speechInnerStroke = dom.window.document.getElementById("speechInnerStroke");
        const gameScene = dom.window.document.getElementById("gameScene");

        expect(dialogText.innerText).toBe("Panda: Fast. Beim nächsten Mal klappt’s bestimmt.");
        expect(speechPanel.style.visibility).toBe("visible");
        expect(speechPanel.style.opacity).toBe("1");
        expect(speechPanel.parentElement).toBe(gameScene);

        expect(speechPanel.style.left).toBeTruthy();
        expect(speechPanel.style.top).toBeTruthy();
        expect(speechPanel.style.width).toBeTruthy();
        expect(speechPanel.style.minHeight).toBe("88px");

        expect(speechTail.style.left).toBe("-18px");
        expect(speechTail.style.transform).toBe("rotate(-90deg)");
        expect(speechTail.style.top).toBeTruthy();

        expect(dialogWrapper.style.padding).toBe("0.5rem 0.8rem 0.5rem 1.8rem");
        expect(speechInnerStroke.style.display).toBe("none");
    });

    test("hidePandaGameComment leert den Text und blendet die Blase aus", () => {
        const dom = setupSpeechDom();

        dom.window.showPandaGameComment("Nicht schlimm, versuch’s einfach nochmal.");
        dom.window.hidePandaGameComment();

        const speechPanel = dom.window.document.getElementById("speechPanel");
        const dialogText = dom.window.document.getElementById("dialogText");

        expect(dialogText.innerText).toBe("");
        expect(speechPanel.style.visibility).toBe("hidden");
        expect(speechPanel.style.opacity).toBe("0");
    });

    test("prepareGameForegroundLayer hebt Panda und Raumcharakter an und versteckt alte Kommentare", () => {
        const dom = setupSpeechDom();

        const speechPanel = dom.window.document.getElementById("speechPanel");
        const dialogText = dom.window.document.getElementById("dialogText");
        const gamePanda = dom.window.document.getElementById("gamePanda");
        const gameRoomCharacter = dom.window.document.getElementById("gameRoomCharacter");
        const welcomePanda = dom.window.document.getElementById("welcomePanda");
        const welcomeCharacterWrapper = dom.window.document.getElementById("welcomeCharacterWrapper");
        const gameCharacterWrapper = dom.window.document.getElementById("gameCharacterWrapper");
        const gameScene = dom.window.document.getElementById("gameScene");

        dialogText.innerText = "Alter Kommentar";
        speechPanel.style.visibility = "visible";
        speechPanel.style.opacity = "1";

        dom.window.prepareGameForegroundLayer();

        expect(speechPanel.parentElement).toBe(gameScene);
        expect(dialogText.innerText).toBe("");
        expect(speechPanel.style.visibility).toBe("hidden");
        expect(speechPanel.style.opacity).toBe("0");

        expect(gamePanda.style.zIndex).toBe("120");
        expect(gamePanda.style.pointerEvents).toBe("none");

        expect(gameRoomCharacter.style.zIndex).toBe("115");
        expect(gameRoomCharacter.style.pointerEvents).toBe("none");

        expect(welcomePanda.style.zIndex).toBe("115");
        expect(welcomePanda.style.pointerEvents).toBe("none");

        expect(welcomeCharacterWrapper.style.zIndex).toBe("115");
        expect(welcomeCharacterWrapper.style.pointerEvents).toBe("none");

        expect(gameCharacterWrapper.style.zIndex).toBe("115");
        expect(gameCharacterWrapper.style.pointerEvents).toBe("none");
    });

    test("showRoomFeedbackBox zeigt eine Erfolgs-Feedbackbox korrekt an", () => {
        const dom = setupAvatarDom();
        const feedbackBox = dom.window.document.getElementById("feedbackBox");

        dom.window.showRoomFeedbackBox(feedbackBox, "Richtig! +5 Punkt(e)", "success");

        expect(feedbackBox.innerText).toBe("Richtig! +5 Punkt(e)");
        expect(feedbackBox.className).toContain("text-center");
        expect(feedbackBox.className).toContain("font-black");
        expect(feedbackBox.style.display).toBe("block");
        expect(feedbackBox.style.position).toBe("relative");
        expect(feedbackBox.style.zIndex).toBe("70");
        expect(feedbackBox.style.borderRadius).toBe("0.9rem");
        expect(feedbackBox.style.background).toBe("rgba(0, 0, 0, 0.72)");
        expect(feedbackBox.style.color).toBe("rgb(52, 211, 153)");
    });

    test("showRoomFeedbackBox zeigt eine Fehler-Feedbackbox korrekt an", () => {
        const dom = setupAvatarDom();
        const feedbackBox = dom.window.document.getElementById("feedbackBox");

        dom.window.showRoomFeedbackBox(feedbackBox, "Falsch! (+0 Punkt(e))", "error");

        expect(feedbackBox.innerText).toBe("Falsch! (+0 Punkt(e))");
        expect(feedbackBox.style.display).toBe("block");
        expect(feedbackBox.style.position).toBe("relative");
        expect(feedbackBox.style.zIndex).toBe("70");
        expect(feedbackBox.style.color).toBe("rgb(248, 113, 113)");
    });

    test("clearRoomFeedbackBox leert und versteckt die Feedbackbox wieder", () => {
        const dom = setupAvatarDom();
        const feedbackBox = dom.window.document.getElementById("feedbackBox");

        dom.window.showRoomFeedbackBox(feedbackBox, "Falsch! (+0 Punkt(e))", "error");
        dom.window.clearRoomFeedbackBox(feedbackBox);

        expect(feedbackBox.innerText).toBe("");
        expect(feedbackBox.className).toBe("hidden");
        expect(feedbackBox.style.display).toBe("none");
        expect(feedbackBox.style.position).toBe("");
        expect(feedbackBox.style.zIndex).toBe("");
        expect(feedbackBox.style.padding).toBe("");
        expect(feedbackBox.style.borderRadius).toBe("");
        expect(feedbackBox.style.background).toBe("");
        expect(feedbackBox.style.boxShadow).toBe("");
        expect(feedbackBox.style.color).toBe("");
    });

    test.each([
        ["circle", "rounded-full"],
        ["square", "rounded-none"],
        ["rounded-square", "rounded-2xl"],
        ["triangle", "[clip-path:polygon(50%_0%,100%_100%,0%_100%)]"],
        ["trapezoid", "[clip-path:polygon(20%_0%,80%_0%,100%_100%,0%_100%)]"],
        ["hexagon", "[clip-path:polygon(25%_6%,75%_6%,100%_50%,75%_94%,25%_94%,0%_50%)]"],
        ["octagon", "[clip-path:polygon(30%_0%,70%_0%,100%_30%,100%_70%,70%_100%,30%_100%,0%_70%,0%_30%)]"],
        ["diamond", "[clip-path:polygon(50%_0%,100%_50%,50%_100%,0%_50%)]"],
        ["star", "[clip-path:polygon(50%_0%,61%_35%,98%_35%,68%_57%,79%_91%,50%_70%,21%_91%,32%_57%,2%_35%,39%_35%)]"]
    ])("applyAvatarStyleToElement setzt die Form %s korrekt", (shape, expectedClass) => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", shape);
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;

        expect(wrapper.className).toContain(expectedClass);
        expect(avatarImage.className).toContain(expectedClass);
    });

    test("applyAvatarStyleToElement fällt bei unbekannter Form auf rounded-full zurück", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "irgendwas-komisches");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;

        expect(wrapper.className).toContain("rounded-full");
        expect(avatarImage.className).toContain("rounded-full");
    });

    test.each([
        ["circle", "rounded-full"],
        ["square", "rounded-none"],
        ["rounded-square", "rounded-2xl"],
        ["triangle", "[clip-path:polygon(50%_0%,100%_100%,0%_100%)]"],
        ["trapezoid", "[clip-path:polygon(20%_0%,80%_0%,100%_100%,0%_100%)]"],
        ["hexagon", "[clip-path:polygon(25%_6%,75%_6%,100%_50%,75%_94%,25%_94%,0%_50%)]"],
        ["octagon", "[clip-path:polygon(30%_0%,70%_0%,100%_30%,100%_70%,70%_100%,30%_100%,0%_70%,0%_30%)]"],
        ["diamond", "[clip-path:polygon(50%_0%,100%_50%,50%_100%,0%_50%)]"],
        ["star", "[clip-path:polygon(50%_0%,61%_35%,98%_35%,68%_57%,79%_91%,50%_70%,21%_91%,32%_57%,2%_35%,39%_35%)]"]
    ])("applyAvatarStyleToElement setzt die Form %s korrekt", (shape, expectedClass) => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", shape);
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;

        expect(wrapper).not.toBeNull();
        expect(wrapper.classList.contains("avatar-frame-shell")).toBe(true);

        expect(wrapper.className).toContain(expectedClass);
        expect(avatarImage.className).toContain(expectedClass);
    });

    test("applyAvatarStyleToElement liest die Form aus sessionStorage", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "octagon");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;

        expect(wrapper.className).toContain("[clip-path:polygon(30%_0%,70%_0%,100%_30%,100%_70%,70%_100%,30%_100%,0%_70%,0%_30%)]");
        expect(avatarImage.className).toContain("[clip-path:polygon(30%_0%,70%_0%,100%_30%,100%_70%,70%_100%,30%_100%,0%_70%,0%_30%)]");
    });

    test("applyAvatarStyleToElement setzt Wrapper und Bild auf dieselbe Form", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "diamond");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;
        const expectedClass = "[clip-path:polygon(50%_0%,100%_50%,50%_100%,0%_50%)]";

        expect(wrapper.className).toContain(expectedClass);
        expect(avatarImage.className).toContain(expectedClass);
    });

    test("applyAvatarStyleToElement fällt bei unbekannter Form auf rounded-full zurück", () => {
        const dom = setupAvatarDom();
        const avatarImage = dom.window.document.getElementById("avatarImage");

        dom.window.sessionStorage.setItem("avatarShape", "irgendwas-komisches");
        dom.window.sessionStorage.setItem("selectedAvatarFrame", "default");

        dom.window.applyAvatarStyleToElement(avatarImage, "w-20 h-20");

        const wrapper = avatarImage.parentElement;

        expect(wrapper.className).toContain("rounded-full");
        expect(avatarImage.className).toContain("rounded-full");
    });
});