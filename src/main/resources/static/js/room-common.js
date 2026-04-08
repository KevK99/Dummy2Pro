function playDummy2ProSound(soundName) {
    window.Dummy2ProSound?.play(soundName);
}

function playDummy2ProMedalSound(medal, variant = "room") {
    window.Dummy2ProSound?.playMedal(medal, variant);
}

function normalizeMedalValue(medal) {
    return String(medal ?? "NONE").toUpperCase();
}

function attachRoomFeedbackSoundObserver() {
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
            showRoomCompletedState();
            return true;
        }

        renderQuestion(currentQuestion);

        const answeredQuestions = data.status?.answeredQuestions ?? 0;

        if (answeredQuestions > 0) {
            welcomeContainer.classList.add("hidden");
            gameContainer.classList.remove("hidden");
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