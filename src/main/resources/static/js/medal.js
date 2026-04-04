function updateMedalCoin(medal) {
    const medalContainer = document.getElementById("medalContainer");
    const medalCoin = document.getElementById("medalCoin");

    if (!medalContainer || !medalCoin) {
        return;
    }

    medalContainer.classList.add("hidden");
    medalCoin.classList.add("hidden");
    medalCoin.textContent = "";
    medalCoin.setAttribute("aria-label", "Medaille");

    if (!medal || medal === "NONE") {
        return;
    }

    if (medal === "BRONZE") {
        medalCoin.textContent = "🥉";
        medalCoin.setAttribute("aria-label", "Bronze Medaille");
    }
    else if (medal === "SILVER") {
        medalCoin.textContent = "🥈";
        medalCoin.setAttribute("aria-label", "Silber Medaille");
    }
    else if (medal === "GOLD") {
        medalCoin.textContent = "🥇";
        medalCoin.setAttribute("aria-label", "Gold Medaille");
    }
    else {
        return;
    }

    medalContainer.classList.remove("hidden");
    medalCoin.classList.remove("hidden");
}

window.updateMedalCoin = updateMedalCoin;