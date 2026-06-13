const state = {
    game: null,
    busy: false,
    modalOffsetX: 0,
    modalOffsetY: 0,
    modalDragging: false,
    soundEnabled: true,
    playedSounds: new Set()
};

const amountGrid = document.querySelector("#amountGrid");
const caseGrid = document.querySelector("#caseGrid");
const statusText = document.querySelector("#statusText");
const roundText = document.querySelector("#roundText");
const leftText = document.querySelector("#leftText");
const message = document.querySelector("#message");
const finalPanel = document.querySelector("#finalPanel");
const modalOverlay = document.querySelector("#modalOverlay");
const resultModal = document.querySelector("#resultModal");
const modalAnimation = document.querySelector("#modalAnimation");
const modalLabel = document.querySelector("#modalLabel");
const modalTitle = document.querySelector("#modalTitle");
const modalBody = document.querySelector("#modalBody");
const modalAnalysis = document.querySelector("#modalAnalysis");
const modalOfferActions = document.querySelector("#modalOfferActions");
const modalResultActions = document.querySelector("#modalResultActions");
const soundButton = document.querySelector("#soundButton");

document.querySelector("#newGameButton").addEventListener("click", createGame);
document.querySelector("#dealButton").addEventListener("click", () => postAction(`/api/games/${state.game.id}/deal`));
document.querySelector("#noDealButton").addEventListener("click", () => postAction(`/api/games/${state.game.id}/no-deal`));
document.querySelector("#keepButton").addEventListener("click", () => postAction(`/api/games/${state.game.id}/keep`));
document.querySelector("#switchButton").addEventListener("click", () => postAction(`/api/games/${state.game.id}/switch`));
document.querySelector("#modalNewGameButton").addEventListener("click", createGame);
document.querySelector("#modalAnalysisButton").addEventListener("click", () => {
    modalAnalysis.hidden = !modalAnalysis.hidden;
});
soundButton.addEventListener("click", () => {
    state.soundEnabled = !state.soundEnabled;
    soundButton.classList.toggle("muted", !state.soundEnabled);
    soundButton.textContent = state.soundEnabled ? "♪" : "×";
    if (state.soundEnabled) {
        playSound("select");
    }
});

resultModal.addEventListener("pointerdown", startModalDrag);
resultModal.addEventListener("pointermove", dragModal);
resultModal.addEventListener("pointerup", stopModalDrag);
resultModal.addEventListener("pointercancel", stopModalDrag);

createGame();

async function createGame() {
    state.playedSounds.clear();
    await postAction("/api/games", "新游戏已开始。先选择一个专属箱子。");
}

async function postAction(url, successMessage, afterUpdate) {
    if (state.busy) {
        return;
    }
    state.busy = true;
    hideModal();
    setMessage("处理中...");
    try {
        const response = await fetch(url, {method: "POST"});
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "操作失败。");
        }
        state.game = data;
        state.busy = false;
        if (afterUpdate) {
            afterUpdate(data);
        }
        render(data);
        if (successMessage) {
            setMessage(successMessage);
        }
    } catch (error) {
        setMessage(error.message);
    } finally {
        state.busy = false;
    }
}

function render(game) {
    hideModal();
    renderStatus(game);
    renderAmounts(game);
    renderCases(game);
    renderOffer(game);
    renderFinalChoice(game);
    renderTerminalMessage(game);
}

function renderStatus(game) {
    statusText.textContent = statusName(game.status);
    roundText.textContent = game.round;
    leftText.textContent = game.status === "OPENING_CASES" ? game.casesLeftToOpen : "-";
}

function renderAmounts(game) {
    const openedValues = new Set(game.openedAmounts.map(String));
    const allAmounts = [...game.openedAmounts, ...game.remainingAmounts]
        .map(Number)
        .sort((a, b) => a - b);

    amountGrid.innerHTML = "";
    allAmounts.forEach(amount => {
        const item = document.createElement("div");
        item.className = `amount tier-${amountTier(amount)}`;
        if (openedValues.has(String(amount))) {
            item.classList.add("opened");
        }
        item.textContent = formatMoney(amount);
        amountGrid.appendChild(item);
    });
}

function renderCases(game) {
    caseGrid.innerHTML = "";
    game.cases.forEach(briefcase => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "case-button";
        if (briefcase.selected) {
            button.classList.add("selected");
        }
        if (briefcase.opened) {
            button.classList.add("opened");
        }
        if (briefcase.amount !== null) {
            button.classList.add(`tier-${amountTier(briefcase.amount)}`);
        }
        button.disabled = !canClickCase(game, briefcase);
        button.innerHTML = `
            <span class="case-number">${briefcase.number}</span>
            ${briefcase.amount !== null ? `<span class="case-amount">${formatMoney(briefcase.amount)}</span>` : ""}
        `;
        button.addEventListener("click", () => handleCaseClick(game, briefcase));
        caseGrid.appendChild(button);
    });
}

function renderOffer(game) {
    const visible = game.status === "BANKER_OFFER" && game.currentOffer;
    if (visible) {
        playSoundOnce(`offer-${game.id}-${game.round}`, "offer");
        showOfferModal(game);
        setMessage(`银行第 ${game.round} 轮报价来了：接受就立刻带走，拒绝则继续开箱。`);
    }
}

function renderFinalChoice(game) {
    finalPanel.hidden = game.status !== "FINAL_CHOICE";
    if (game.status === "FINAL_CHOICE") {
        setMessage("只剩你的专属箱子和最后一个未开箱子。现在做最终选择。");
    }
}

function renderTerminalMessage(game) {
    if (game.status === "DEAL_ACCEPTED") {
        const selectedCase = game.cases.find(briefcase => briefcase.selected);
        const selectedAmount = selectedCase ? Number(selectedCase.amount) : 0;
        const dealDiff = Number(game.acceptedOffer) - selectedAmount;
        const round = game.currentOffer ? game.currentOffer.round : game.round;
        playSoundOnce(`result-${game.id}-${game.status}`, dealDiff >= 0 ? "success" : "failure");
        showResultModal({
            title: formatMoney(game.acceptedOffer),
            body: `你的专属箱子里是 ${formatMoney(selectedAmount)}，${profitText(dealDiff)}。`,
            analysis: `你在第 ${round} 轮接受银行报价。银行给了 ${formatMoney(game.acceptedOffer)}，而你原本选中的箱子是 ${formatMoney(selectedAmount)}，所以这次成交${dealDiff >= 0 ? "更划算" : "不划算"}，差额为 ${formatMoney(Math.abs(dealDiff))}。`,
            celebration: dealDiff >= 0
        });
        setMessage(`你选择成交，奖金为 ${formatMoney(game.acceptedOffer)}。你的专属箱子里是 ${formatMoney(selectedAmount)}，${profitText(dealDiff)}。`);
    }
    if (game.status === "FINISHED") {
        const action = game.switchedAtFinal ? "交换了最后一个箱子" : "保留了自己的箱子";
        const finalPrize = Number(game.finalPrize);
        const selectedCase = game.cases.find(briefcase => briefcase.selected);
        const selectedAmount = selectedCase ? Number(selectedCase.amount) : finalPrize;
        playSoundOnce(`result-${game.id}-${game.status}`, finalPrize >= 100000 ? "success" : "failure");
        showResultModal({
            title: formatMoney(finalPrize),
            body: `你${action}，最终奖金为 ${formatMoney(finalPrize)}。`,
            analysis: game.switchedAtFinal
                ? `你最后选择交换，拿到 ${formatMoney(finalPrize)}。如果保留原来的专属箱子，金额是 ${formatMoney(selectedAmount)}，差额为 ${formatMoney(Math.abs(finalPrize - selectedAmount))}。`
                : `你最后选择保留专属箱子，拿到 ${formatMoney(finalPrize)}。这局没有接受银行报价，结果完全由最后箱子金额决定。`,
            celebration: finalPrize >= 100000
        });
        setMessage(`你${action}，最终奖金为 ${formatMoney(game.finalPrize)}。`);
    }
}

function showOfferModal(game) {
    resultModal.classList.remove("celebration", "failure");
    modalAnimation.innerHTML = `<span class="banker-pulse"></span>`;
    modalAnalysis.hidden = true;
    modalAnalysis.textContent = "";
    modalLabel.textContent = `第 ${game.round} 轮银行报价`;
    modalTitle.textContent = formatMoney(game.currentOffer.amount);
    modalBody.textContent = `已经打开 ${game.currentOffer.openedCount} 个箱子，剩余 ${game.currentOffer.remainingCount} 个金额。`;
    modalOfferActions.hidden = false;
    modalResultActions.hidden = true;
    modalOverlay.hidden = false;
}

function showResultModal({title, body, analysis, celebration}) {
    resultModal.classList.toggle("celebration", celebration);
    resultModal.classList.toggle("failure", !celebration);
    modalAnimation.innerHTML = celebration ? celebrationMarkup() : failureMarkup();
    modalLabel.textContent = celebration ? "结果不错" : "结果失利";
    modalTitle.textContent = title;
    modalBody.textContent = body;
    modalAnalysis.textContent = analysis;
    modalAnalysis.hidden = true;
    modalOfferActions.hidden = true;
    modalResultActions.hidden = false;
    modalOverlay.hidden = false;
}

function hideModal() {
    modalOverlay.hidden = true;
}

function startModalDrag(event) {
    if (event.target.closest("button")) {
        return;
    }
    state.modalDragging = true;
    state.dragStartX = event.clientX;
    state.dragStartY = event.clientY;
    state.dragOriginX = state.modalOffsetX;
    state.dragOriginY = state.modalOffsetY;
    resultModal.classList.add("dragging");
    resultModal.setPointerCapture(event.pointerId);
}

function dragModal(event) {
    if (!state.modalDragging) {
        return;
    }
    state.modalOffsetX = state.dragOriginX + event.clientX - state.dragStartX;
    state.modalOffsetY = state.dragOriginY + event.clientY - state.dragStartY;
    resultModal.style.translate = `${state.modalOffsetX}px ${state.modalOffsetY}px`;
}

function stopModalDrag(event) {
    if (!state.modalDragging) {
        return;
    }
    state.modalDragging = false;
    resultModal.classList.remove("dragging");
    if (resultModal.hasPointerCapture(event.pointerId)) {
        resultModal.releasePointerCapture(event.pointerId);
    }
}

function celebrationMarkup() {
    return Array.from({length: 18}, (_, index) => `<span style="--i:${index}"></span>`).join("");
}

function failureMarkup() {
    return `<span></span><span></span><span></span>`;
}

function playSoundOnce(key, name) {
    if (state.playedSounds.has(key)) {
        return;
    }
    state.playedSounds.add(key);
    playSound(name);
}

function playSound(name) {
    if (!state.soundEnabled) {
        return;
    }
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) {
        return;
    }
    if (!state.audioContext) {
        state.audioContext = new AudioContextClass();
    }
    if (state.audioContext.state === "suspended") {
        state.audioContext.resume();
    }

    const patterns = {
        select: [[523, 0, 0.07], [659, 0.08, 0.08]],
        open: [[180, 0, 0.08], [360, 0.08, 0.06]],
        offer: [[392, 0, 0.09], [523, 0.1, 0.09], [784, 0.2, 0.12]],
        success: [[523, 0, 0.08], [659, 0.09, 0.08], [784, 0.18, 0.1], [1046, 0.3, 0.18]],
        failure: [[330, 0, 0.12], [277, 0.14, 0.12], [220, 0.28, 0.18]]
    };

    const notes = patterns[name] || patterns.select;
    notes.forEach(([frequency, delay, duration]) => {
        playTone(frequency, delay, duration, name === "open" ? "triangle" : "sine");
    });
}

function playTone(frequency, delay, duration, type) {
    const context = state.audioContext;
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    const start = context.currentTime + delay;
    const end = start + duration;

    oscillator.type = type;
    oscillator.frequency.setValueAtTime(frequency, start);
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(0.12, start + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, end);

    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start(start);
    oscillator.stop(end + 0.02);
}

function handleCaseClick(game, briefcase) {
    if (game.status === "SELECTING_PERSONAL_CASE") {
        playSound("select");
        postAction(`/api/games/${game.id}/select/${briefcase.number}`, `你选择了 ${briefcase.number} 号箱作为专属箱子。`);
        return;
    }
    if (game.status === "OPENING_CASES") {
        postAction(`/api/games/${game.id}/open/${briefcase.number}`, null, updatedGame => {
            const openedCase = updatedGame.cases.find(item => item.number === briefcase.number);
            if (openedCase && openedCase.amount !== null) {
                playSound(isGoodOpenedAmount(openedCase.amount) ? "success" : "failure");
            }
        });
    }
}

function canClickCase(game, briefcase) {
    if (state.busy || briefcase.opened) {
        return false;
    }
    if (game.status === "SELECTING_PERSONAL_CASE") {
        return true;
    }
    return game.status === "OPENING_CASES" && !briefcase.selected;
}

function statusName(status) {
    const names = {
        SELECTING_PERSONAL_CASE: "选择专属箱子",
        OPENING_CASES: "开箱阶段",
        BANKER_OFFER: "银行报价",
        FINAL_CHOICE: "最终选择",
        DEAL_ACCEPTED: "已成交",
        FINISHED: "游戏结束"
    };
    return names[status] || status;
}

function formatMoney(value) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: Number(value) < 1 ? 2 : 0
    }).format(Number(value));
}

function profitText(diff) {
    if (diff > 0) {
        return `这笔成交赚了 ${formatMoney(diff)}`;
    }
    if (diff < 0) {
        return `这笔成交亏了 ${formatMoney(Math.abs(diff))}`;
    }
    return "这笔成交不赚不亏";
}

function amountTier(value) {
    const amount = Number(value);
    if (amount <= 75) {
        return "gray";
    }
    if (amount <= 750) {
        return "green";
    }
    if (amount <= 25000) {
        return "blue";
    }
    if (amount <= 100000) {
        return "purple";
    }
    if (amount <= 500000) {
        return "gold";
    }
    return "red";
}

function isGoodOpenedAmount(value) {
    return Number(value) <= 25000;
}

function setMessage(text) {
    message.textContent = text;
}
