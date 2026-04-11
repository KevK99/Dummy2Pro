function playDummy2ProSound(soundName) {
    window.Dummy2ProSound?.play(soundName);
}

function playDummy2ProMedalSound(medal, variant = "room") {
    window.Dummy2ProSound?.playMedal(medal, variant);
}

function normalizeMedalValue(medal) {
    return String(medal ?? "NONE").toUpperCase();
}

function getStoredAvatarShape() {
    return sessionStorage.getItem("avatarShape") || "circle";
}

function getStoredAvatarFrame() {
    return sessionStorage.getItem("selectedAvatarFrame") || "default";
}

function getAvatarShapeClasses(shape) {
    if (shape === "circle") {
        return ["rounded-full"];
    }

    if (shape === "square") {
        return ["rounded-none"];
    }

    if (shape === "rounded-square") {
        return ["rounded-2xl"];
    }

    if (shape === "triangle") {
        return ["[clip-path:polygon(50%_0%,100%_100%,0%_100%)]"];
    }

    if (shape === "trapezoid") {
        return ["[clip-path:polygon(20%_0%,80%_0%,100%_100%,0%_100%)]"];
    }

    if (shape === "hexagon") {
        return ["[clip-path:polygon(25%_6%,75%_6%,100%_50%,75%_94%,25%_94%,0%_50%)]"];
    }

    if (shape === "octagon") {
        return ["[clip-path:polygon(30%_0%,70%_0%,100%_30%,100%_70%,70%_100%,30%_100%,0%_70%,0%_30%)]"];
    }

    if (shape === "diamond") {
        return ["[clip-path:polygon(50%_0%,100%_50%,50%_100%,0%_50%)]"];
    }

    if (shape === "star") {
        return ["[clip-path:polygon(50%_0%,61%_35%,98%_35%,68%_57%,79%_91%,50%_70%,21%_91%,32%_57%,2%_35%,39%_35%)]"];
    }

    return ["rounded-full"];
}

function getAvatarFrameClass(frame) {
    const frameClassMap = {
        default: "bg-slate-500",
        red: "bg-red-500",
        orange: "bg-orange-500",
        amber: "bg-amber-500",
        yellow: "bg-yellow-400",
        lime: "bg-lime-500",
        green: "bg-green-500",
        emerald: "bg-emerald-500",
        teal: "bg-teal-500",
        cyan: "bg-cyan-500",
        sky: "bg-sky-500",
        blue: "bg-blue-500",
        indigo: "bg-indigo-500",
        violet: "bg-violet-500",
        purple: "bg-purple-500",
        fuchsia: "bg-fuchsia-500",
        pink: "bg-pink-500",
        rose: "bg-rose-500",
        slate: "bg-slate-400",
        gray: "bg-gray-400",
        black: "bg-black",
        bronze: "bg-[#cd7f32]",
        silver: "bg-[#c0c0c0]",
        gold: "bg-[#ffd700]"
    };

    return frameClassMap[frame] || "bg-slate-500";
}

function applyAvatarStyleToElement(element, sizeClasses = "w-20 h-20") {
    if (!element) {
        return;
    }

    const shape = getStoredAvatarShape();
    const frame = getStoredAvatarFrame();

    const shapeClasses = getAvatarShapeClasses(shape);
    const frameClass = getAvatarFrameClass(frame);

    let wrapper = element.parentElement;

    if (!wrapper || !wrapper.classList.contains("avatar-frame-shell")) {
        wrapper = document.createElement("div");
        wrapper.className = "avatar-frame-shell inline-flex items-center justify-center p-[4px] overflow-hidden transition-all duration-200";
        element.parentNode.insertBefore(wrapper, element);
        wrapper.appendChild(element);
    }

    wrapper.className = `avatar-frame-shell inline-flex items-center justify-center p-[4px] overflow-hidden transition-all duration-200 ${sizeClasses} ${frameClass}`;
    wrapper.classList.add(...shapeClasses);

    element.className = "block w-full h-full object-cover transition-all duration-200";
    element.classList.add(...shapeClasses);
}

const roomWrongFeedbackState = {};

function getNextRotatingRoomLine(roomId, lines) {
    if (!Array.isArray(lines) || lines.length === 0) {
        return "Das war leider falsch.";
    }

    const key = String(roomId ?? "default");
    const currentIndex = Number(roomWrongFeedbackState[key] ?? 0);

    roomWrongFeedbackState[key] = currentIndex + 1;

    return lines[currentIndex % lines.length];
}

function getRoomWrongFeedbackLine(roomId) {
    const numericRoomId = Number(roomId ?? 0);

    if (numericRoomId === 1) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Nicht schlimm, versuch’s einfach nochmal.",
            "Fast. Beim nächsten Mal klappt’s bestimmt.",
            "Das war noch nicht richtig, aber du schaffst das.",
            "Kein Problem, wir gehen’s einfach nochmal an.",
            "Noch ein Versuch. Ich glaub an dich."
        ]);
    }

    if (numericRoomId === 2) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Hm. Das war jetzt nicht ganz richtig.",
            "Nein, das war’s noch nicht.",
            "Langsam solltest du das besser treffen.",
            "Das war leider daneben.",
            "Nicht richtig. Nochmal konzentrieren."
        ]);
    }

    if (numericRoomId === 3) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Nein. Das war nichts.",
            "Daneben. Versuch’s sauberer.",
            "Nicht richtig.",
            "Das war der falsche Weg.",
            "So wird das noch nichts."
        ]);
    }

    if (numericRoomId === 4) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Schon wieder daneben.",
            "Nein. Konzentration.",
            "Das war leider falsch.",
            "So langsam wird’s unnötig holprig.",
            "Nicht richtig. Reiß dich zusammen."
        ]);
    }

    if (numericRoomId === 5) {
        return "...";
    }

    if (numericRoomId >= 6 && numericRoomId <= 9) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Schon wieder falsch.",
            "Nein. Schon wieder daneben.",
            "Das war erneut nichts.",
            "Wir drehen uns im Kreis.",
            "Nicht schon wieder.",
            "Das war wieder der falsche Griff.",
            "Schon wieder daneben.",
            "Das war jetzt wirklich derselbe Fehler.",
            "Nein, wieder nicht.",
            "Das wird langsam Gewohnheit.",
            "Erneut falsch.",
            "Wir hatten das doch gerade schon.",
            "Noch ein Fehltritt.",
            "Schon wieder am Ziel vorbei.",
            "Das kippt gerade in eine richtige Serie.",
            "Leider wieder falsch.",
            "Das war die nächste Bauchlandung.",
            "Und wieder daneben.",
            "Wieder nicht richtig.",
            "Das Muster gefällt mir gar nicht."
        ]);
    }

    if (numericRoomId >= 10 && numericRoomId <= 12) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Das war schon erstaunlich daneben.",
            "Du kämpfst heute wirklich gegen die richtige Antwort.",
            "Selbst mein Bambus hätte das präziser hinbekommen.",
            "Kreativ war’s. Richtig leider nicht.",
            "Du weichst der Lösung mit bemerkenswertem Talent aus.",
            "Das war fast schon beeindruckend falsch.",
            "Nein. So schief muss man erst mal liegen.",
            "Die richtige Antwort stand offenbar auf deiner Blockliste.",
            "Du verfehlst das Ziel gerade mit Konstanz.",
            "Das war fachlich eher ein Sturzflug."
        ]);
    }

    if (numericRoomId >= 13 && numericRoomId <= 14) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Das war düster daneben.",
            "Hier zerbröselt gerade jede Hoffnung auf Präzision.",
            "Das war nicht nur falsch, das war ein Rückschritt.",
            "Die richtige Antwort rückt so eher weiter weg.",
            "Ich verliere langsam den Glauben an diese Runde.",
            "Nein. Das war unerquicklich falsch.",
            "Das sieht gerade wirklich nicht gut aus.",
            "Mit solchen Antworten wird das hier finster.",
            "Das war unerquicklich weit weg von richtig.",
            "Wir bewegen uns gerade in die falsche Richtung."
        ]);
    }

    if (numericRoomId === 15) {
        return getNextRotatingRoomLine(numericRoomId, [
            "Was zum *** war das denn?",
            "Das war komplett *** daneben.",
            "Diese Antwort war ja völlig ***.",
            "Ich fasse es nicht. Einfach nur ***.",
            "Nein. Absolut ***.",
            "Das war so *** falsch, dass es fast Kunst ist.",
            "Ganz ehrlich? ***.",
            "Das war ein einziges ***.",
            "So hart daneben? ***.",
            "Ich sag’s zensiert: kompletter ***-Treffer. Nur leider auf das Falsche."
        ]);
    }

    return "Das war leider falsch.";
}

function buildRoomWrongFeedbackText(roomId, pointsEarned) {
    const numericPoints = Number(pointsEarned ?? 0);
    return `Falsch! (+${numericPoints} Punkt(e))`;
}

function clearRoomFeedbackBox(feedbackBox) {
    if (!feedbackBox) {
        return;
    }

    feedbackBox.innerText = "";
    feedbackBox.className = "hidden";
    feedbackBox.style.display = "none";
    feedbackBox.style.position = "";
    feedbackBox.style.zIndex = "";
    feedbackBox.style.padding = "";
    feedbackBox.style.borderRadius = "";
    feedbackBox.style.background = "";
    feedbackBox.style.backdropFilter = "";
    feedbackBox.style.webkitBackdropFilter = "";
    feedbackBox.style.boxShadow = "";
    feedbackBox.style.width = "";
    feedbackBox.style.maxWidth = "";
    feedbackBox.style.marginLeft = "";
    feedbackBox.style.marginRight = "";
    feedbackBox.style.color = "";
}

function showRoomFeedbackBox(feedbackBox, text, type = "error") {
    if (!feedbackBox) {
        return;
    }

    feedbackBox.innerText = text;
    feedbackBox.className = "mt-4 text-center text-lg font-black min-h-[32px] shrink-0";
    feedbackBox.style.display = "block";
    feedbackBox.style.position = "relative";
    feedbackBox.style.zIndex = "70";
    feedbackBox.style.padding = "0.65rem 1rem";
    feedbackBox.style.borderRadius = "0.9rem";
    feedbackBox.style.background = "rgba(0, 0, 0, 0.72)";
    feedbackBox.style.backdropFilter = "blur(6px)";
    feedbackBox.style.webkitBackdropFilter = "blur(6px)";
    feedbackBox.style.boxShadow = "0 10px 30px rgba(0, 0, 0, 0.35)";
    feedbackBox.style.width = "fit-content";
    feedbackBox.style.maxWidth = "100%";
    feedbackBox.style.marginLeft = "auto";
    feedbackBox.style.marginRight = "auto";
    feedbackBox.style.color = type === "success" ? "#34d399" : "#f87171";
}

function elevateRoomFeedbackBox() {
    const roomFeedbackBox = document.getElementById("feedbackBox");

    if (!roomFeedbackBox) {
        return;
    }

    roomFeedbackBox.style.position = "relative";
    roomFeedbackBox.style.zIndex = "70";
    roomFeedbackBox.style.padding = "0.65rem 1rem";
    roomFeedbackBox.style.borderRadius = "0.9rem";
    roomFeedbackBox.style.background = "rgba(0, 0, 0, 0.72)";
    roomFeedbackBox.style.backdropFilter = "blur(6px)";
    roomFeedbackBox.style.webkitBackdropFilter = "blur(6px)";
    roomFeedbackBox.style.boxShadow = "0 10px 30px rgba(0, 0, 0, 0.35)";
    roomFeedbackBox.style.width = "fit-content";
    roomFeedbackBox.style.maxWidth = "100%";
    roomFeedbackBox.style.marginLeft = "auto";
    roomFeedbackBox.style.marginRight = "auto";
}

let pandaCommentTimeoutId = null;

function elevateRoomForegroundActors() {
    const gamePandaElement = document.getElementById("gamePanda");
    const gameRoomCharacterElement = document.getElementById("gameRoomCharacter");
    const welcomePandaElement = document.getElementById("welcomePanda");

    if (gamePandaElement) {
        gamePandaElement.style.zIndex = "120";
        gamePandaElement.style.pointerEvents = "none";
    }

    if (gameRoomCharacterElement) {
        gameRoomCharacterElement.style.zIndex = "115";
        gameRoomCharacterElement.style.pointerEvents = "none";
    }

    if (welcomePandaElement) {
        welcomePandaElement.style.zIndex = "115";
        welcomePandaElement.style.pointerEvents = "none";
    }

    document
        .querySelectorAll('#welcomeContainer img[src^="/room-characters/"], #gameContainer img[src^="/room-characters/"]')
        .forEach(image => {
            const wrapper = image.closest("div");
            if (!wrapper) {
                return;
            }

            wrapper.style.zIndex = "115";
            wrapper.style.pointerEvents = "none";
        });
}

function moveSpeechPanelToGameScene() {
    const speechPanelElement = document.getElementById("speechPanel");
    const gameSceneElement = document.getElementById("gameScene");

    if (!speechPanelElement || !gameSceneElement) {
        return;
    }

    if (speechPanelElement.parentElement !== gameSceneElement) {
        gameSceneElement.appendChild(speechPanelElement);
    }

    speechPanelElement.style.zIndex = "140";
    speechPanelElement.style.pointerEvents = "none";
}

function positionSpeechPanelForPandaComment() {
    const speechPanelElement = document.getElementById("speechPanel");
    const speechTailElement = document.getElementById("speechTail");
    const dialogTextElement = document.getElementById("dialogText");
    const gamePandaElement = document.getElementById("gamePanda");
    const gameSceneElement = document.getElementById("gameScene");

    if (!speechPanelElement || !speechTailElement || !dialogTextElement || !gamePandaElement || !gameSceneElement) {
        return;
    }

    const bubbleShell = speechPanelElement.firstElementChild;
    const bubbleInnerStroke = bubbleShell?.children?.[2];

    const pandaRect = gamePandaElement.getBoundingClientRect();
    const sceneRect = gameSceneElement.getBoundingClientRect();

    const pandaMouthX = pandaRect.left - sceneRect.left + (pandaRect.width * 0.70);
    const pandaMouthY = pandaRect.top - sceneRect.top + (pandaRect.height * 0.42);

    const bubbleWidth = Math.min(390, Math.max(250, Math.round(gameSceneElement.clientWidth * 0.29)));
    const bubbleLeft = Math.max(
        120,
        Math.min(
            gameSceneElement.clientWidth - bubbleWidth - 18,
            Math.round(pandaMouthX + 42)
        )
    );
    const bubbleTop = Math.max(8, Math.round(pandaMouthY - 44));

    speechPanelElement.classList.remove("-translate-x-[42%]", "-translate-x-[50%]", "-translate-x-[34%]");
    speechTailElement.classList.remove("left-[82%]", "left-[18%]");

    speechPanelElement.style.left = `${bubbleLeft}px`;
    speechPanelElement.style.top = `${bubbleTop}px`;
    speechPanelElement.style.transform = "none";
    speechPanelElement.style.width = `${bubbleWidth}px`;
    speechPanelElement.style.maxWidth = `${bubbleWidth}px`;
    speechPanelElement.style.minWidth = "250px";
    speechPanelElement.style.height = "auto";
    speechPanelElement.style.minHeight = "88px";
    speechPanelElement.style.pointerEvents = "none";
    speechPanelElement.style.zIndex = "140";

    if (bubbleShell) {
        bubbleShell.style.minHeight = "88px";
    }

    if (bubbleInnerStroke) {
        bubbleInnerStroke.style.display = "none";
    }

    const tailTop = Math.max(16, Math.round(pandaMouthY - bubbleTop - 18));

    speechTailElement.style.left = "-18px";
    speechTailElement.style.right = "auto";
    speechTailElement.style.top = `${tailTop}px`;
    speechTailElement.style.bottom = "auto";
    speechTailElement.style.transform = "rotate(-90deg)";

    dialogTextElement.style.margin = "0";
    dialogTextElement.style.padding = "0";
    dialogTextElement.style.lineHeight = "1.04";
    dialogTextElement.style.fontSize = "clamp(16px, 1.25vw, 22px)";
    dialogTextElement.style.textAlign = "left";
    dialogTextElement.style.maxWidth = "100%";

    const dialogInnerWrapper = dialogTextElement.parentElement;
    if (dialogInnerWrapper) {
        dialogInnerWrapper.style.padding = "0.5rem 0.8rem 0.5rem 1.8rem";
        dialogInnerWrapper.style.minHeight = "88px";
        dialogInnerWrapper.style.height = "auto";
        dialogInnerWrapper.style.display = "flex";
        dialogInnerWrapper.style.alignItems = "center";
    }
}

function hidePandaGameComment() {
    const speechPanelElement = document.getElementById("speechPanel");
    const dialogTextElement = document.getElementById("dialogText");

    if (!speechPanelElement || !dialogTextElement) {
        return;
    }

    if (pandaCommentTimeoutId) {
        clearTimeout(pandaCommentTimeoutId);
        pandaCommentTimeoutId = null;
    }

    dialogTextElement.innerText = "";
    speechPanelElement.style.opacity = "0";
    speechPanelElement.style.visibility = "hidden";
}

function showPandaGameComment(text) {
    const speechPanelElement = document.getElementById("speechPanel");
    const dialogTextElement = document.getElementById("dialogText");

    if (!speechPanelElement || !dialogTextElement || !text) {
        return;
    }

    moveSpeechPanelToGameScene();
    elevateRoomForegroundActors();

    dialogTextElement.innerText = `Panda: ${text}`;
    positionSpeechPanelForPandaComment();

    speechPanelElement.style.opacity = "1";
    speechPanelElement.style.visibility = "visible";

    if (pandaCommentTimeoutId) {
        clearTimeout(pandaCommentTimeoutId);
    }

    pandaCommentTimeoutId = window.setTimeout(() => {
        hidePandaGameComment();
    }, 4200);
}

function prepareGameForegroundLayer() {
    moveSpeechPanelToGameScene();
    elevateRoomForegroundActors();
    hidePandaGameComment();
}

window.addEventListener("resize", () => {
    const speechPanelElement = document.getElementById("speechPanel");

    if (!speechPanelElement) {
        return;
    }

    if (speechPanelElement.style.visibility === "visible" && speechPanelElement.style.opacity === "1") {
        positionSpeechPanelForPandaComment();
    }
});

function attachRoomFeedbackSoundObserver() {
    elevateRoomFeedbackBox();
    elevateRoomForegroundActors();

    const roomFeedbackBox = document.getElementById("feedbackBox");

    if (!roomFeedbackBox || roomFeedbackBox.dataset.soundObserverAttached === "true") {
        return;
    }

    roomFeedbackBox.dataset.soundObserverAttached = "true";

    let lastFeedbackSignature = roomFeedbackBox.textContent.trim();

    const playFeedbackSound = () => {
        const currentText = roomFeedbackBox.textContent.trim();

        if (!currentText || currentText === lastFeedbackSignature) {
            return;
        }

        lastFeedbackSignature = currentText;

        if (currentText.startsWith("Richtig!")) {
            playDummy2ProSound("correct");
            return;
        }

        if (currentText.startsWith("Falsch!")) {
            playDummy2ProSound("wrong");
        }
    };

    const observer = new MutationObserver(playFeedbackSound);
    observer.observe(roomFeedbackBox, {
        characterData: true,
        childList: true,
        subtree: true
    });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", attachRoomFeedbackSoundObserver);
} else {
    attachRoomFeedbackSoundObserver();
}

function updateHeadlineOverview(overview = null) {
    const usernameElement = document.getElementById("headlineUsername");
    const answeredElement = document.getElementById("headlineAnsweredQuestions");
    const totalElement = document.getElementById("headlineTotalQuestions");
    const avatarElement = document.getElementById("headlineAvatar");

    if (usernameElement) {
        usernameElement.textContent = sessionStorage.getItem("username") ?? "NutzerName";
    }

    if (avatarElement) {
        const avatarFromStorage = sessionStorage.getItem("avatar");
        if (avatarFromStorage) {
            avatarElement.src = `/images/${avatarFromStorage}`;
        }
        applyAvatarStyleToElement(avatarElement, "w-20 h-20");
    }

    const playerAvatarElement = document.getElementById("playerAvatar");
    if (playerAvatarElement) {
        const avatarFromStorage = sessionStorage.getItem("avatar");
        if (avatarFromStorage) {
            playerAvatarElement.src = `/images/${avatarFromStorage}`;
        }
        applyAvatarStyleToElement(playerAvatarElement, "w-28 h-28");
    }

    if (!overview) {
        return;
    }

    const rooms = Array.isArray(overview.rooms) ? overview.rooms : [];

    const totalAnsweredQuestions = rooms.reduce((sum, room) => {
        return sum + Number(room.answeredQuestions ?? 0);
    }, 0);

    const totalQuestions = rooms.reduce((sum, room) => {
        return sum + Number(room.totalQuestions ?? 0);
    }, 0);

    if (answeredElement) {
        answeredElement.textContent = totalAnsweredQuestions;
    }

    if (totalElement) {
        totalElement.textContent = totalQuestions;
    }
}

async function refreshHeadlineOverview() {
    if (!sessionId) {
        updateHeadlineOverview();
        return;
    }

    try {
        const response = await fetch(`/api/session/${sessionId}/overview`);

        if (!response.ok) {
            updateHeadlineOverview();
            return;
        }

        const overview = await response.json();
        updateHeadlineOverview(overview);
    } catch (error) {
        console.error("Fehler beim Aktualisieren des Headline-Headers:", error);
        updateHeadlineOverview();
    }
}

function isGapQuestion(question) {
    return question &&
        (question.questionType === "GAP" ||
            (question.gapFields && question.gapFields.length > 0));
}

function countWords(text) {
    return String(text ?? "")
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .length;
}

function buildAnswerPreviewText(index, fullText, maxWords = 4) {
    const normalizedText = String(fullText ?? "").trim();

    if (!normalizedText) {
        return `${String.fromCharCode(65 + index)})`;
    }

    const words = normalizedText.split(/\s+/).filter(Boolean);
    const preview = words.slice(0, maxWords).join(" ");
    const suffix = words.length > maxWords ? " ..." : "";

    return `${String.fromCharCode(65 + index)}) ${preview}${suffix}`;
}

function getStandardQuestionMetrics(question) {
    const textLength = [question?.startText, question?.endText]
        .filter(Boolean)
        .join(" ")
        .length;

    const optionCount = Array.isArray(question?.answerOptions)
        ? question.answerOptions.length
        : 0;

    const rowCount = Math.max(1, Math.ceil(optionCount / 2));

    return {
        textLength,
        optionCount,
        rowCount,
        hasImage: Boolean(question?.imageUrl)
    };
}

function renderStandardQuestionText(question, baseClassName) {
    if (!questionText) {
        return;
    }

    questionText.className = baseClassName;
    questionText.innerHTML = "";
    questionText.style.wordBreak = "break-word";

    const hasStartText = Boolean(question?.startText?.trim());
    const hasEndText = Boolean(question?.endText?.trim());
    const hasImage = Boolean(question?.imageUrl);

    if (!hasStartText && !hasEndText && !hasImage) {
        questionText.innerText = "Keine Frage vorhanden.";
        return;
    }

    const wrapper = document.createElement("div");
    wrapper.className = "flex h-full w-full flex-col items-center justify-center gap-3 whitespace-pre-line";

    if (hasStartText) {
        const startBlock = document.createElement("div");
        startBlock.className = "w-full";
        startBlock.textContent = question.startText;
        wrapper.appendChild(startBlock);
    }

    if (hasImage) {
        const image = document.createElement("img");
        image.src = question.imageUrl;
        image.alt = "Fragebild";
        image.className = "mx-auto max-h-[150px] w-auto max-w-full object-contain rounded-lg border border-white/20";
        image.loading = "lazy";
        image.onerror = () => {
            image.remove();
        };
        wrapper.appendChild(image);
    }

    if (hasEndText) {
        const endBlock = document.createElement("div");
        endBlock.className = "w-full";
        endBlock.textContent = question.endText;
        wrapper.appendChild(endBlock);
    }

    questionText.appendChild(wrapper);
}

function getGapMetrics(question) {
    const gapFields = [...(question?.gapFields || [])];

    const textLength = [
        question?.startText,
        question?.endText,
        ...gapFields.flatMap(gapField => [gapField.textBefore, gapField.textAfter])
    ]
        .filter(Boolean)
        .join(" ")
        .length;

    const maxOptionCount = Math.max(
        0,
        ...gapFields.map(gapField => (gapField.gapOptions || []).length)
    );

    const columnCount = maxOptionCount >= 7 ? 3 : 2;
    const rowCount = Math.max(1, Math.ceil(maxOptionCount / columnCount));

    return {
        gapCount: gapFields.length,
        textLength,
        maxOptionCount,
        rowCount
    };
}

function syncQuestionSideLayout() {
    if (!questionPanel || !gameScene) {
        return;
    }

    const panelBottom = 30;
    const panelHeight = questionPanel.offsetHeight || parseInt(questionPanel.style.height || "520", 10);
    const sceneHeight = gameScene.offsetHeight || parseInt(gameScene.style.height || "680", 10);
    const questionPanelTop = sceneHeight - panelHeight - panelBottom;

    if (statusPanel) {
        statusPanel.style.height = `${panelHeight}px`;
        statusPanel.style.top = "auto";
        statusPanel.style.bottom = `${panelBottom}px`;
    }

    if (gameRoomCharacter) {
        const roomCharacterTop = Math.max(10, questionPanelTop - 150);
        gameRoomCharacter.style.bottom = "auto";
        gameRoomCharacter.style.top = `${roomCharacterTop}px`;
    }

    if (gamePanda) {
        const pandaTop = Math.max(-42, questionPanelTop - 185);
        gamePanda.style.top = `${pandaTop}px`;
    }
}

function applyQuestionLayout(question) {
    if (!gameScene || !questionPanel) {
        return;
    }

    if (!isGapQuestion(question)) {
        const { textLength, optionCount, rowCount } = getStandardQuestionMetrics(question);

        let panelHeight = 520;
        let sceneHeight = 680;
        let questionTextMaxHeight = 250;

        if (textLength > 180) {
            panelHeight += 30;
            questionTextMaxHeight += 20;
        }
        if (textLength > 260) {
            panelHeight += 40;
            questionTextMaxHeight += 30;
        }
        if (textLength > 360) {
            panelHeight += 35;
            questionTextMaxHeight += 20;
        }

        if (optionCount > 4) {
            panelHeight += 70;
        }
        if (optionCount > 6) {
            panelHeight += 35;
        }
        if (optionCount > 8) {
            panelHeight += 35;
        }

        if (rowCount >= 3) {
            panelHeight += 30;
        }
        if (rowCount >= 4) {
            panelHeight += 30;
        }

        panelHeight = Math.min(panelHeight, 720);
        sceneHeight = Math.max(680, panelHeight + 80);
        questionTextMaxHeight = Math.min(questionTextMaxHeight, 320);

        gameScene.style.height = `${sceneHeight}px`;
        questionPanel.style.height = `${panelHeight}px`;
        questionText.style.maxHeight = `${questionTextMaxHeight}px`;

        requestAnimationFrame(() => {
            syncQuestionSideLayout();
        });
        return;
    }

    const { gapCount, textLength, maxOptionCount, rowCount } = getGapMetrics(question);

    let panelHeight = 520;

    if (gapCount >= 3) {
        panelHeight += 25;
    }
    if (gapCount >= 4) {
        panelHeight += 25;
    }

    if (textLength > 140) {
        panelHeight += 35;
    }
    if (textLength > 220) {
        panelHeight += 35;
    }
    if (textLength > 320) {
        panelHeight += 25;
    }

    if (maxOptionCount >= 5) {
        panelHeight += 35;
    }
    if (maxOptionCount >= 7) {
        panelHeight += 35;
    }
    if (maxOptionCount >= 9) {
        panelHeight += 35;
    }

    if (rowCount >= 3) {
        panelHeight += 20;
    }
    if (rowCount >= 4) {
        panelHeight += 20;
    }

    panelHeight = Math.min(panelHeight, 690);

    const sceneHeight = Math.max(680, panelHeight + 80);

    questionPanel.style.height = `${panelHeight}px`;
    gameScene.style.height = `${sceneHeight}px`;

    let questionTextMaxHeight = 180;

    if (textLength > 220) {
        questionTextMaxHeight += 30;
    }
    if (gapCount >= 4) {
        questionTextMaxHeight += 20;
    }

    questionTextMaxHeight = Math.min(questionTextMaxHeight, 290);
    questionText.style.maxHeight = `${questionTextMaxHeight}px`;

    requestAnimationFrame(() => {
        syncQuestionSideLayout();
    });
}

function updateSpeechTailPosition(speaker) {
    if (speechTail) {
        speechTail.classList.remove("left-[82%]", "left-[18%]");

        if (speaker === "player") {
            speechTail.classList.add("left-[18%]");
        } else {
            speechTail.classList.add("left-[82%]");
        }
    }

    if (speechPanel) {
        speechPanel.classList.remove("-translate-x-[42%]", "-translate-x-[50%]", "-translate-x-[34%]");

        if (speaker === "player") {
            speechPanel.classList.add("-translate-x-[50%]");
        } else {
            speechPanel.classList.add("-translate-x-[34%]");
        }
    }
}

function nextDialogLine() {
    if (dialogIndex < introDialog.length - 1) {
        dialogIndex++;
        showCurrentDialogLine();
    }
}

function startQuiz() {
    welcomeContainer.classList.add("hidden");
    gameContainer.classList.remove("hidden");
    prepareGameForegroundLayer();
}

function fitQuestionText() {
    if (!questionText) {
        return;
    }

    let fontSize = 18;
    const minFontSize = 8;

    questionText.style.fontSize = fontSize + "px";
    questionText.style.lineHeight = "1.08";

    while (fontSize > minFontSize && questionText.scrollHeight > questionText.clientHeight) {
        fontSize--;
        questionText.style.fontSize = fontSize + "px";
    }
}

function fitAnswerTexts() {
    const buttons = answerContainer.querySelectorAll(".quiz-answer-btn");

    buttons.forEach(button => {
        const label = button.querySelector(".answer-label");
        if (!label) {
            return;
        }

        let fontSize = 22;
        const minFontSize = 14;

        label.style.fontSize = fontSize + "px";

        while (fontSize > minFontSize && label.scrollHeight > label.clientHeight) {
            fontSize--;
            label.style.fontSize = fontSize + "px";
        }

        const isLong =
            label.scrollHeight > label.clientHeight ||
            countWords(button.dataset.fullText) > 4;

        if (isLong) {
            button.classList.add("has-answer-tooltip");
            button.setAttribute("data-tooltip", button.dataset.fullText);
        } else {
            button.classList.remove("has-answer-tooltip");
            button.removeAttribute("data-tooltip");
        }
    });
}

function fitGapQuestionText() {
    const textFlow = questionText.firstElementChild;
    if (!textFlow) {
        return;
    }

    let fontSize = 14;
    const minFontSize = 8;

    textFlow.style.fontSize = fontSize + "px";

    while (fontSize > minFontSize && questionText.scrollHeight > questionText.clientHeight) {
        fontSize--;
        textFlow.style.fontSize = fontSize + "px";
    }
}

function fitGapOptionTexts() {
    const buttons = answerContainer.querySelectorAll(".gap-option-btn");

    buttons.forEach(button => {
        const label = button.querySelector(".gap-option-label");
        if (!label) {
            return;
        }

        let fontSize = 16;
        const minFontSize = 9;

        label.style.fontSize = fontSize + "px";

        while (fontSize > minFontSize && label.scrollHeight > label.clientHeight) {
            fontSize--;
            label.style.fontSize = fontSize + "px";
        }

        const isLong =
            label.scrollHeight > label.clientHeight ||
            (button.dataset.optionText && button.dataset.optionText.length > 36);

        if (isLong) {
            button.classList.add("has-answer-tooltip");
            button.setAttribute("data-tooltip", button.dataset.optionText);
        } else {
            button.classList.remove("has-answer-tooltip");
            button.removeAttribute("data-tooltip");
        }
    });
}

async function refreshRoomStatus() {
    try {
        const response = await fetch(`/api/session/${sessionId}/room/${roomId}/status`);
        const status = await response.json();

        if (!response.ok) {
            return;
        }

        updateStatus(status);
        await refreshHeadlineOverview();
    } catch (error) {
        console.error("Fehler beim Aktualisieren des Status:", error);
    }
}

function updateStatus(status) {
    document.getElementById("earnedPoints").innerText = `${status.earnedPoints}/${status.totalPoints}`;
    document.getElementById("questionProgress").innerText = `${status.answeredQuestions}/${status.totalQuestions}`;

    // Thema-Fortschritt = Fortschritt innerhalb dieses Raums
    const roomPercent = status.totalQuestions > 0
        ? Math.round((status.answeredQuestions / status.totalQuestions) * 1000) / 10
        : 0;

    document.getElementById("themeProgressBar").style.width = `${roomPercent}%`;
    document.getElementById("themeProgressText").innerText = `${roomPercent}%`;

    // Fragen-Fortschritt = richtig / falsch
    const correctAnswers =
        status.correctAnswers ??
        0;

    const wrongAnswers =
        status.wrongAnswers ??
        Math.max(0, (status.answeredQuestions ?? 0) - correctAnswers);

    const totalQuestions = status.totalQuestions ?? 0;

    const correctPercent = totalQuestions > 0
        ? (correctAnswers / totalQuestions) * 100
        : 0;

    const wrongPercent = totalQuestions > 0
        ? (wrongAnswers / totalQuestions) * 100
        : 0;

    document.getElementById("questionCorrectBar").style.width = `${correctPercent}%`;
    document.getElementById("questionWrongBar").style.width = `${wrongPercent}%`;

    document.getElementById("correctAnswersText").innerText = correctAnswers;
    document.getElementById("wrongAnswersText").innerText = wrongAnswers;

    const medalText = !status.medal || status.medal === "NONE"
        ? "IN PROGRESS"
        : status.medal;

    document.getElementById("roomStatusBadge").innerText =
        `${medalText} (${roomPercent}%)`;

    const normalizedMedal = normalizeMedalValue(status.medal);
    const previousMedal = window.__dummy2proLastRoomMedal;

    if (previousMedal !== undefined && previousMedal !== normalizedMedal && normalizedMedal !== "NONE") {
        playDummy2ProMedalSound(normalizedMedal, "room");
    }

    window.__dummy2proLastRoomMedal = normalizedMedal;

    updateMedalCoin(status.medal);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function animatePlayerEntrance() {
    if (!playerCharacter) {
        return;
    }

    playerCharacter.classList.remove("left-[140px]", "-translate-x-0");
    playerCharacter.classList.add("left-1/2", "-translate-x-1/2", "bottom-[-170px]");

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            playerCharacter.classList.remove("left-1/2", "-translate-x-1/2", "bottom-[-170px]");
            playerCharacter.classList.add("left-[180px]", "-translate-x-0", "bottom-[95px]");
        });
    });
}

async function loadRoom() {
    try {
        const response = await fetch(`/api/session/${sessionId}/room/${roomId}`);
        const data = await response.json();

        if (!response.ok) {
            sessionStorage.removeItem("sessionId");
            alert(data.message || "Raum konnte nicht geladen werden.");
            window.location.href = "/dashboard";
            return false;
        }

        currentQuestion = data.firstQuestion;
        updateStatus(data.status);
        await refreshHeadlineOverview();

        if (!currentQuestion) {
            welcomeContainer.classList.add("hidden");
            gameContainer.classList.remove("hidden");
            prepareGameForegroundLayer();
            showRoomCompletedState();
            return true;
        }

        renderQuestion(currentQuestion);

        const answeredQuestions = data.status?.answeredQuestions ?? 0;

        if (answeredQuestions > 0) {
            welcomeContainer.classList.add("hidden");
            gameContainer.classList.remove("hidden");
            prepareGameForegroundLayer();
            return true;
        }

        introDialog = data.introDialog || [];
        dialogIndex = 0;

        animatePlayerEntrance();

        setTimeout(() => {
            showCurrentDialogLine();
        }, 1850);

        return true;
    } catch (error) {
        console.error("Fehler beim Laden des Raums:", error);
        alert("Raum konnte nicht geladen werden.");
        return false;
    }
}

async function initWelcomeRoom() {
    const loaded = await loadRoom();
    roomLoaded = loaded === true;
}

function updateMedalCoin(medal) {
    const medalContainer = document.getElementById("medalContainer");
    const medalCoin = document.getElementById("medalCoin");

    if (!medalContainer || !medalCoin) {
        return;
    }

    medalContainer.classList.add("hidden");

    medalCoin.className =
        "w-20 h-20 rounded-full border-4 flex items-center justify-center font-black text-xs tracking-wide shadow-lg";

    medalCoin.innerText = "---";

    if (!medal || medal === "NONE") {
        return;
    }

    medalContainer.classList.remove("hidden");

    if (medal === "BRONZE") {
        medalCoin.classList.add("bg-amber-700", "border-amber-300", "text-white");
        medalCoin.innerText = "BRONZE";
        return;
    }

    if (medal === "SILVER") {
        medalCoin.classList.add("bg-slate-300", "border-white", "text-slate-900");
        medalCoin.innerText = "SILBER";
        return;
    }

    if (medal === "GOLD") {
        medalCoin.classList.add("bg-yellow-400", "border-yellow-100", "text-slate-900");
        medalCoin.innerText = "GOLD";
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        updateHeadlineOverview();
    });
} else {
    updateHeadlineOverview();
}