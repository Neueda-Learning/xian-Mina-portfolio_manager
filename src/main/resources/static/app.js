const API_URL = "/api/portfolio-items";
const TYPE_ORDER = ["STOCK", "FUND", "CRYPTO", "CASH"];
const TYPE_META = {
    STOCK: { label: "Stocks", color: "#1768d5" },
    FUND: { label: "Funds", color: "#805ad5" },
    CRYPTO: { label: "Crypto", color: "#dc4b58" },
    CASH: { label: "Cash", color: "#eeb547" }
};
const state = { portfolios: [], activePortfolioId: null, items: [], performance: [], marketAssets: [], selectedId: null, marketFilter: "ALL" };
const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

const assetDialogs = [...document.querySelectorAll(".asset-dialog")];
const portfolioDialog = document.querySelector("#portfolioDialog");
const aiDialog = document.querySelector("#aiAnalysisDialog");
const toast = document.querySelector("#toast");
let toastTimer;
let activeAiSource = null;

// 读取所有组合，并恢复上次选择的组合；没有记录时默认选择第一个组合。
async function loadPortfolios() {
    const response = await fetch("/api/portfolios");
    if (!response.ok) throw new Error("Unable to load portfolios");
    state.portfolios = await response.json();
    const storedId = Number(localStorage.getItem("activePortfolioId"));
    const canKeepSelection = state.portfolios.some(portfolio => portfolio.id === state.activePortfolioId);
    const canUseStoredId = state.portfolios.some(portfolio => portfolio.id === storedId);
    state.activePortfolioId = canKeepSelection ? state.activePortfolioId
        : (canUseStoredId ? storedId : state.portfolios[0]?.id ?? null);
    renderPortfolioSelector();
}

function renderPortfolioSelector() {
    const select = document.querySelector("#portfolioSelect");
    select.innerHTML = state.portfolios.map(portfolio =>
        `<option value="${portfolio.id}">${escapeHtml(portfolio.portfolioName)}</option>`).join("");
    select.value = String(state.activePortfolioId ?? "");
    const active = state.portfolios.find(portfolio => portfolio.id === state.activePortfolioId);
    document.querySelector("#dashboardTitle").textContent = active
        ? `${active.portfolioName} Dashboard` : "Portfolio Dashboard";
}

function activePortfolioQuery() {
    return `portfolioId=${encodeURIComponent(state.activePortfolioId)}`;
}

// 从后端读取当前组合的持仓、绩效和市场价格；页面只负责显示和计算。
async function loadItems() {
    if (!state.activePortfolioId) return;
    try {
        const [itemsResponse, performanceResponse, marketResponse] = await Promise.all([
            fetch(`${API_URL}?${activePortfolioQuery()}`),
            fetch(`${API_URL}/performance?${activePortfolioQuery()}`),
            fetch(`${API_URL}/market/assets`)
        ]);
        if (!itemsResponse.ok || !performanceResponse.ok || !marketResponse.ok) throw new Error("Unable to load holdings");
        state.items = await itemsResponse.json();
        state.performance = await performanceResponse.json();
        state.marketAssets = await marketResponse.json();
        renderDashboard();
    } catch (error) {
        showToast("Could not reach the API. Please check Spring Boot is running.");
    }
}

async function initializeDashboard() {
    try {
        await loadPortfolios();
        await loadItems();
    } catch (error) {
        showToast("Could not reach the API. Please check Spring Boot is running.");
    }
}

function decorateItem(item) {
    const quantity = Number(item.quantity);
    const price = Number(item.purchasePrice);
    // currentPrice 由后端从 asset_price_history 最新记录返回。
    const currentPrice = Number(item.currentPrice ?? item.purchasePrice);
    const costBasis = quantity * price;
    const marketValue = quantity * currentPrice;
    return { ...item, quantity, price, currentPrice, costBasis, marketValue, profitLoss: marketValue - costBasis };
}

function getDashboardData() {
    const items = state.items.map(decorateItem);
    const totalCost = items.reduce((sum, item) => sum + item.costBasis, 0);
    const totalValue = items.reduce((sum, item) => sum + item.marketValue, 0);
    const totalReturn = totalValue - totalCost;
    const allocations = TYPE_ORDER.map(type => {
        const value = items.filter(item => item.assetType === type).reduce((sum, item) => sum + item.marketValue, 0);
        return { type, value, percentage: totalValue ? value / totalValue * 100 : 0 };
    });
    return { items, totalCost, totalValue, totalReturn, returnPercent: totalCost ? totalReturn / totalCost * 100 : 0, allocations };
}

function renderDashboard() {
    const data = getDashboardData();
    document.querySelector("#totalValue").textContent = money.format(data.totalValue);
    document.querySelector("#totalCost").textContent = money.format(data.totalCost);
    document.querySelector("#totalReturn").textContent = `${data.totalReturn >= 0 ? "+" : ""}${money.format(data.totalReturn)}`;
    document.querySelector("#totalReturn").className = data.totalReturn >= 0 ? "profit" : "loss";
    document.querySelector("#returnPercent").textContent = `${data.returnPercent >= 0 ? "+" : ""}${data.returnPercent.toFixed(2)}% estimated return`;
    document.querySelector("#holdingCount").textContent = `${data.items.length} holding${data.items.length === 1 ? "" : "s"}`;
    document.querySelector("#performanceReturn").textContent = `${data.returnPercent >= 0 ? "+" : ""}${data.returnPercent.toFixed(2)}%`;
    document.querySelector("#donutTotal").textContent = compactMoney(data.totalValue);
    renderAllocation(data.allocations);
    renderHoldings(data.items);
    renderValueCards(data.allocations);
    drawPerformanceChart(data);
    renderMarketAssets();
}

function renderAllocation(allocations) {
    const donut = document.querySelector("#allocationDonut");
    const legend = document.querySelector("#allocationLegend");
    let current = 0;
    const segments = allocations.filter(item => item.value > 0).map(item => {
        const start = current;
        current += item.percentage;
        return `${TYPE_META[item.type].color} ${start}% ${current}%`;
    });
    donut.style.background = segments.length ? `conic-gradient(${segments.join(",")})` : "#e9edf3";
    legend.innerHTML = allocations.map(item => `
        <div class="legend-row">
            <i class="legend-dot" style="background:${TYPE_META[item.type].color}"></i>
            <span>${TYPE_META[item.type].label}</span>
            <strong>${item.percentage.toFixed(0)}%</strong>
        </div>`).join("");
}

function renderHoldings(items) {
    const body = document.querySelector("#holdingsBody");
    const empty = document.querySelector("#emptyState");
    body.innerHTML = items.map(item => `
        <tr class="${item.id === state.selectedId ? "selected" : ""}" data-id="${item.id}">
            <td><input class="holding-select" type="radio" name="selectedHolding" value="${item.id}"
                aria-label="Select ${escapeHtml(item.ticker)} holding" ${item.id === state.selectedId ? "checked" : ""}></td>
            <td><span class="symbol">${escapeHtml(item.ticker)}</span><br><small>${escapeHtml(item.assetName || "Investment asset")}</small></td>
            <td>${TYPE_META[item.assetType]?.label || item.assetType}</td>
            <td class="number">${item.quantity}</td>
            <td class="number">${money.format(item.currentPrice)}</td>
            <td class="number">${money.format(item.price)}</td>
            <td class="number">${money.format(item.marketValue)}</td>
            <td class="number ${item.profitLoss >= 0 ? "profit" : "loss"}">${item.profitLoss >= 0 ? "+" : ""}${money.format(item.profitLoss)}</td>
            <td><button class="remove-row" data-sell-id="${item.id}">Sell</button></td>
        </tr>`).join("");
    empty.hidden = items.length !== 0;
    body.querySelectorAll("tr").forEach(row => row.addEventListener("click", () => {
        state.selectedId = Number(row.dataset.id);
        renderDashboard();
    }));
    // 单选框让“已选择哪一笔持仓”更直观；点击单选框不触发表格行的重复事件。
    body.querySelectorAll("[name=selectedHolding]").forEach(input => input.addEventListener("click", event => {
        event.stopPropagation();
        state.selectedId = Number(input.value);
        renderDashboard();
    }));
    body.querySelectorAll("[data-sell-id]").forEach(button => button.addEventListener("click", event => {
        event.stopPropagation();
        sellItem(Number(button.dataset.sellId));
    }));
}

function renderValueCards(allocations) {
    document.querySelector("#assetValueCards").innerHTML = allocations.map(item => `
        <article class="value-card" style="--card-color:${TYPE_META[item.type].color}">
            <span>${TYPE_META[item.type].label} Value</span>
            <strong>${money.format(item.value)}</strong>
        </article>`).join("");
}

function renderMarketAssets() {
    const body = document.querySelector("#marketBody");
    const select = document.querySelector("#assetCatalogId");
    const visibleAssets = state.marketFilter === "ALL"
        ? state.marketAssets
        : state.marketAssets.filter(asset => asset.assetType === state.marketFilter);
    body.innerHTML = visibleAssets.map(asset => `
        <tr><td class="symbol">${escapeHtml(asset.ticker)}</td><td>${escapeHtml(asset.assetName)}</td>
        <td>${TYPE_META[asset.assetType]?.label || asset.assetType}</td>
        <td class="number">${money.format(Number(asset.marketPrice))}</td><td>${asset.priceTime}</td></tr>`).join("")
        || '<tr><td class="empty-market-row" colspan="5">No market data is available for this asset type.</td></tr>';
    // The old single Add Asset select no longer exists. Keep this guard so the
    // market table still renders while the three typed dialogs manage their own selects.
    if (select) {
        select.innerHTML = state.marketAssets.map(asset =>
            `<option value="${asset.id}">${escapeHtml(asset.ticker)} — ${escapeHtml(asset.assetName)}</option>`).join("");
        loadPriceOptions();
    }
    const marketDay = state.marketAssets[0]?.priceTime || "—";
    document.querySelector("#marketDay").textContent = marketDay;
    renderOpenAssetDialogs();
}

function getAssetDialogParts(dialog) {
    return {
        type: dialog.dataset.assetType,
        form: dialog.querySelector(".asset-form"),
        search: dialog.querySelector(".asset-search"),
        assetSelect: dialog.querySelector(".asset-catalog-id"),
        quantity: dialog.querySelector(".asset-quantity"),
        priceTime: dialog.querySelector(".price-time")
    };
}

function assetsForType(type, searchText = "") {
    const keyword = searchText.trim().toLowerCase();
    return state.marketAssets.filter(asset => {
        const sameType = String(asset.assetType).toUpperCase() === type;
        const searchable = `${asset.ticker} ${asset.assetName}`.toLowerCase();
        return sameType && (!keyword || searchable.includes(keyword));
    });
}

function renderAssetOptions(dialog) {
    const parts = getAssetDialogParts(dialog);
    const assets = assetsForType(parts.type, parts.search.value);
    if (!assets.length) {
        parts.assetSelect.innerHTML = '<option value="">No matching asset found</option>';
        parts.priceTime.innerHTML = "";
        return;
    }
    parts.assetSelect.innerHTML = assets.map(asset =>
        `<option value="${asset.id}">${escapeHtml(asset.ticker)} — ${escapeHtml(asset.assetName)}</option>`).join("");
    loadDialogPriceOptions(dialog);
}

function openAssetDialog(type) {
    const dialog = document.querySelector(`.asset-dialog[data-asset-type="${type}"]`);
    if (!dialog) return;
    if (!assetsForType(type).length) {
        showToast(`No ${type.toLowerCase()} market data is available yet.`);
        return;
    }
    const parts = getAssetDialogParts(dialog);
    parts.form.reset();
    renderAssetOptions(dialog);
    dialog.showModal();
}

async function loadDialogPriceOptions(dialog) {
    const parts = getAssetDialogParts(dialog);
    const assetId = Number(parts.assetSelect.value);
    if (!assetId) {
        parts.priceTime.innerHTML = "";
        return;
    }
    try {
        const response = await fetch(`${API_URL}/market/assets/${assetId}/prices`);
        if (!response.ok) throw new Error("Unable to load price history");
        const prices = await response.json();
        parts.priceTime.innerHTML = prices.map((item, index) => {
            const time = item.priceTime.replace("T", " ");
            // The API returns newest first. Only the newest option shows its price.
            const label = index === 0 ? `${time} — Latest ${money.format(Number(item.marketPrice))}` : time;
            return `<option value="${item.priceTime}" data-price="${item.marketPrice}" data-latest="${index === 0}">${label}</option>`;
        }).join("");
    } catch (error) {
        parts.priceTime.innerHTML = "";
        showToast("Could not load the selected asset price history.");
    }
}

function renderOpenAssetDialogs() {
    assetDialogs.filter(dialog => dialog.open).forEach(renderAssetOptions);
}

async function saveTypedAsset(event) {
    event.preventDefault();
    const dialog = event.currentTarget.closest(".asset-dialog");
    const parts = getAssetDialogParts(dialog);
    const payload = {
        portfolioId: state.activePortfolioId,
        assetCatalogId: Number(parts.assetSelect.value),
        quantity: Number(parts.quantity.value),
        priceTime: parts.priceTime.value
    };
    const response = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });
    if (!response.ok) {
        showToast("Could not save asset. Please check the values.");
        return;
    }
    // 等待当前组合重新读取并渲染完成，避免用户必须手动刷新页面才能看到新持仓。
    await loadItems();
    dialog.close();
    event.currentTarget.reset();
    showToast("Asset purchased successfully.");
}

// 根据当前选中的股票读取全部五分钟历史价格，供用户选择买入时点。
async function loadPriceOptions() {
    const assetId = Number(document.querySelector("#assetCatalogId").value);
    const priceTimeSelect = document.querySelector("#priceTime");
    if (!assetId) {
        priceTimeSelect.innerHTML = "";
        updateSelectedMarketPrice();
        return;
    }
    try {
        const response = await fetch(`${API_URL}/market/assets/${assetId}/prices`);
        if (!response.ok) throw new Error("Unable to load price history");
        const prices = await response.json();
        priceTimeSelect.innerHTML = prices.map(item => {
            const time = item.priceTime.replace("T", " ");
            return `<option value="${item.priceTime}" data-price="${item.marketPrice}">${time}</option>`;
        }).join("");
        updateSelectedMarketPrice();
    } catch (error) {
        priceTimeSelect.innerHTML = "";
        showToast("Could not load the selected asset price history.");
        updateSelectedMarketPrice();
    }
}

function updateSelectedMarketPrice() {
    const option = document.querySelector("#priceTime").selectedOptions[0];
    document.querySelector("#selectedMarketPrice").textContent = option
        ? money.format(Number(option.dataset.price)) : "—";
}

// 同一天可能有多次市值快照。折线图按天展示，因此只保留当天最后一次快照。
function getDailyPerformancePoints(totalValue) {
    const dailyValues = new Map();
    const sortedHistory = [...state.performance].sort((left, right) =>
        String(left.recordDate ?? left.recordTime).localeCompare(String(right.recordDate ?? right.recordTime))
    );

    sortedHistory.forEach(item => {
        const recordTime = item.recordDate ?? item.recordTime;
        if (!recordTime) return;
        dailyValues.set(String(recordTime).slice(0, 10), Number(item.totalValue));
    });

    const points = [...dailyValues.entries()].map(([day, value]) => ({ day, value }));
    if (points.length) {
        // 为首次买入前补 6 天的 0 市值基线；买入后当天开始出现组合市值。
        // 后续某天没有新快照时，沿用上一天的市值，避免曲线错误地回落到 0。
        const firstDay = points[0].day;
        const lastDay = points.at(-1).day;
        const startDay = addCalendarDays(firstDay, -6);
        const result = [];
        let currentValue = 0;

        for (let day = startDay; day <= lastDay; day = addCalendarDays(day, 1)) {
            if (dailyValues.has(day)) currentValue = dailyValues.get(day);
            result.push({ day, value: currentValue });
        }
        return result;
    }

    // 还没有数据库记录时，使用今天的 0/当前值作为占位平线。
    const today = new Date().toISOString().slice(0, 10);
    return [{ day: today, value: totalValue || 0 }];
}

// 使用本地日历日期计算，避免 toISOString 的时区转换让日期少一天。
function addCalendarDays(day, offset) {
    const [year, month, date] = day.split("-").map(Number);
    const value = new Date(year, month - 1, date + offset);
    const yyyy = value.getFullYear();
    const mm = String(value.getMonth() + 1).padStart(2, "0");
    const dd = String(value.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
}

function renderPerformanceLabels(points) {
    const labels = document.querySelector("#performanceLabels");
    const maxLabels = 7;
    const labelIndexes = points.length <= maxLabels
        ? points.map((_, index) => index)
        : [...new Set(Array.from({ length: maxLabels }, (_, index) =>
            Math.round(index * (points.length - 1) / (maxLabels - 1))
        ))];

    labels.innerHTML = labelIndexes.map(index => {
        const day = points[index].day;
        return `<span>${escapeHtml(day.slice(5))}</span>`;
    }).join("");
}

// 折线图读取 portfolio_value_history，并以数据库记录日期为横轴。
function drawPerformanceChart(data) {
    const canvas = document.querySelector("#performanceChart");
    const box = canvas.getBoundingClientRect();
    const ratio = window.devicePixelRatio || 1;
    canvas.width = box.width * ratio;
    canvas.height = box.height * ratio;
    const ctx = canvas.getContext("2d");
    ctx.scale(ratio, ratio);
    const width = box.width;
    const height = box.height;
    const dailyPoints = getDailyPerformancePoints(data.totalValue);
    const chartPoints = dailyPoints.length >= 2 ? dailyPoints : [dailyPoints[0], dailyPoints[0]];
    const history = chartPoints.map(item => item.value);
    renderPerformanceLabels(dailyPoints);
    const min = Math.min(...history) * .97;
    const max = Math.max(...history) * 1.03;
    const padding = { top: 13, right: 10, bottom: 10, left: 45 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;
    const point = (item, index) => ({ x: padding.left + chartWidth * index / (history.length - 1), y: padding.top + (max - item) / (max - min || 1) * chartHeight });

    ctx.clearRect(0, 0, width, height);
    ctx.font = "11px Segoe UI";
    ctx.fillStyle = "#8795a9";
    ctx.strokeStyle = "#e8edf5";
    for (let line = 0; line < 4; line++) {
        const y = padding.top + chartHeight * line / 3;
        ctx.beginPath(); ctx.moveTo(padding.left, y); ctx.lineTo(width - padding.right, y); ctx.stroke();
        ctx.fillText(compactMoney(max - (max - min) * line / 3), 2, y + 4);
    }
    const gradient = ctx.createLinearGradient(0, padding.top, 0, height);
    gradient.addColorStop(0, "rgba(23, 104, 213, .28)");
    gradient.addColorStop(1, "rgba(23, 104, 213, .01)");
    ctx.beginPath();
    history.forEach((item, index) => { const p = point(item, index); index ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y); });
    const last = point(history.at(-1), history.length - 1);
    ctx.lineTo(last.x, height - padding.bottom); ctx.lineTo(padding.left, height - padding.bottom); ctx.closePath();
    ctx.fillStyle = gradient; ctx.fill();
    ctx.beginPath();
    history.forEach((item, index) => { const p = point(item, index); index ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y); });
    ctx.strokeStyle = "#1768d5"; ctx.lineWidth = 3; ctx.stroke();
}

async function saveAsset(event) {
    event.preventDefault();
    const payload = {
        portfolioId: state.activePortfolioId,
        assetCatalogId: Number(document.querySelector("#assetCatalogId").value),
        quantity: Number(document.querySelector("#quantity").value),
        priceTime: document.querySelector("#priceTime").value
    };
    const response = await fetch(API_URL, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
    if (!response.ok) { showToast("Could not save asset. Please check the values."); return; }
    dialog.close();
    event.target.reset();
    showToast("Asset purchased at the latest market price.");
    loadItems();
}

async function sellItem(id) {
    const holding = state.items.find(item => item.id === id);
    if (!holding) return;
    const input = window.prompt(`Sell quantity (maximum: ${holding.quantity})`, holding.quantity);
    if (input === null) return;
    const quantity = Number(input);
    if (!Number.isFinite(quantity) || quantity <= 0 || quantity > Number(holding.quantity)) {
        showToast("Enter a valid sell quantity.");
        return;
    }
    const response = await fetch(`${API_URL}/${id}/sell?${activePortfolioQuery()}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity })
    });
    if (!response.ok) { showToast("Could not sell the asset."); return; }
    if (state.selectedId === id) state.selectedId = null;
    showToast("Asset sold at the latest market price.");
    loadItems();
}

let aiAnalysisText = "";

// 将模型的固定 Markdown 标题转换为报告卡片，避免直接显示 ## 和原始换行。
function renderAiAnalysis() {
    const output = document.querySelector("#aiAnalysisOutput");
    const text = aiAnalysisText.replace(/\r/g, "").trim();
    if (!text) {
        output.innerHTML = '<div class="ai-empty-state"><strong>正在生成分析</strong><span>DeepSeek 正在整理当前持仓和市场行情…</span></div>';
        return;
    }

    // 某些模型会返回“##持仓分析”（没有空格），两种写法都要识别。
    const sections = text.split(/(?=^##\s*)/m).filter(section => section.trim());
    const report = sections.map((section, index) => {
        const lines = section.trim().split("\n");
        const title = lines.shift().replace(/^##\s*/, "").trim() || "分析内容";
        const content = lines.filter(line => line.trim());
        const bullets = content.filter(line => /^[-*]\s+/.test(line));
        const paragraphs = content.filter(line => !/^[-*]\s+/.test(line));
        const body = [
            paragraphs.map(line => `<p>${formatAiText(line)}</p>`).join(""),
            bullets.length ? `<ul>${bullets.map(line => `<li>${formatAiText(line.replace(/^[-*]\s+/, ""))}</li>`).join("")}</ul>` : ""
        ].join("");
        return `<article class="ai-report-section"><div class="ai-section-heading"><span class="ai-section-index">${String(index + 1).padStart(2, "0")}</span><h3>${escapeHtml(title)}</h3></div><div class="ai-section-content">${body || '<p>正在生成内容…</p>'}</div></article>`;
    });
    output.innerHTML = `<div class="ai-report-grid">${report.join("")}</div>`;
}

// 仅保留粗体标记；其他内容先转义，避免模型返回内容被当成 HTML 执行。
function formatAiText(text) {
    return escapeHtml(text).replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
}

function setAiStatus(text, state = "") {
    const status = document.querySelector("#aiAnalysisStatus");
    status.className = `ai-status${state ? ` is-${state}` : ""}`;
    status.innerHTML = `<i></i>${escapeHtml(text)}`;
}

// EventSource 会持续接收后端转发的 DeepSeek token，不需要等待整段分析生成完毕。
function startAiAnalysis() {
    const button = document.querySelector("#startAiAnalysisButton");
    if (activeAiSource) activeAiSource.close();
    aiDialog.showModal();
    button.disabled = true;
    aiAnalysisText = "";
    setAiStatus("Generating analysis…", "loading");
    renderAiAnalysis();

    const source = new EventSource(`/api/ai-analysis/stream?${activePortfolioQuery()}`);
    activeAiSource = source;
    source.addEventListener("token", event => {
        aiAnalysisText += event.data;
        renderAiAnalysis();
    });
    source.addEventListener("complete", () => {
        source.close();
        activeAiSource = null;
        button.disabled = false;
        setAiStatus("Analysis complete");
    });
    source.addEventListener("ai-error", event => {
        aiAnalysisText = `## 分析失败\n- ${event.data}`;
        renderAiAnalysis();
        source.close();
        activeAiSource = null;
        button.disabled = false;
        setAiStatus("Unable to analyze", "error");
    });
    source.onerror = () => {
        if (source.readyState !== EventSource.CLOSED) {
            aiAnalysisText = "## 连接中断\n- AI 连接中断，请稍后重试。";
            renderAiAnalysis();
            source.close();
            activeAiSource = null;
            button.disabled = false;
            setAiStatus("Connection interrupted", "error");
        }
    };
}

function compactMoney(value) {
    return value >= 1000 ? `$${(value / 1000).toFixed(value >= 100000 ? 0 : 1)}k` : money.format(value);
}
function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[char]); }
function showToast(message) { clearTimeout(toastTimer); toast.textContent = message; toast.classList.add("show"); toastTimer = setTimeout(() => toast.classList.remove("show"), 2600); }

document.querySelectorAll("[data-open-asset-dialog]").forEach(button => {
    button.addEventListener("click", () => openAssetDialog(button.dataset.openAssetDialog));
});
assetDialogs.forEach(dialog => {
    const parts = getAssetDialogParts(dialog);
    parts.form.addEventListener("submit", saveTypedAsset);
    parts.search.addEventListener("input", () => renderAssetOptions(dialog));
    parts.assetSelect.addEventListener("change", () => loadDialogPriceOptions(dialog));
    dialog.querySelectorAll(".close-asset-dialog").forEach(button => {
        button.addEventListener("click", () => dialog.close());
    });
});
document.querySelector("#openPortfolioDialogButton").addEventListener("click", () => portfolioDialog.showModal());
document.querySelector("#closePortfolioDialogButton").addEventListener("click", () => portfolioDialog.close());
document.querySelector("#cancelPortfolioDialogButton").addEventListener("click", () => portfolioDialog.close());
document.querySelector("#portfolioSelect").addEventListener("change", event => {
    state.activePortfolioId = Number(event.target.value);
    state.selectedId = null;
    localStorage.setItem("activePortfolioId", String(state.activePortfolioId));
    renderPortfolioSelector();
    loadItems();
});
document.querySelector("#portfolioForm").addEventListener("submit", async event => {
    event.preventDefault();
    const portfolioName = document.querySelector("#portfolioName").value.trim();
    const response = await fetch("/api/portfolios", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ portfolioName })
    });
    if (!response.ok) {
        showToast("Could not create portfolio. Choose a different name.");
        return;
    }
    const portfolio = await response.json();
    state.portfolios.push(portfolio);
    state.activePortfolioId = portfolio.id;
    state.selectedId = null;
    localStorage.setItem("activePortfolioId", String(portfolio.id));
    renderPortfolioSelector();
    portfolioDialog.close();
    event.target.reset();
    showToast("New portfolio created.");
    loadItems();
});
document.querySelector("#closeAiDialogButton").addEventListener("click", () => {
    if (activeAiSource) activeAiSource.close();
    activeAiSource = null;
    document.querySelector("#startAiAnalysisButton").disabled = false;
    setAiStatus("Analysis closed");
    aiDialog.close();
});
// 用户按 Esc 关闭弹窗时，也停止仍在进行的流式连接。
aiDialog.addEventListener("close", () => {
    if (!activeAiSource) return;
    activeAiSource.close();
    activeAiSource = null;
    document.querySelector("#startAiAnalysisButton").disabled = false;
    setAiStatus("Analysis closed");
});
document.querySelector("#startAiAnalysisButton").addEventListener("click", startAiAnalysis);
document.querySelectorAll("[data-market-filter]").forEach(button => {
    button.addEventListener("click", () => {
        state.marketFilter = button.dataset.marketFilter;
        document.querySelectorAll("[data-market-filter]").forEach(item => {
            item.classList.toggle("is-active", item === button);
        });
        renderMarketAssets();
    });
});
document.querySelectorAll("[data-scroll-target]").forEach(button => button.addEventListener("click", () => {
    const target = document.querySelector(`#${button.dataset.scrollTarget}`);
    if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
    document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));
    button.classList.add("active");
}));
document.querySelectorAll("[data-toast]").forEach(button => button.addEventListener("click", () => showToast(button.dataset.toast)));
window.addEventListener("resize", renderDashboard);
initializeDashboard();
// 前端每分钟只读一次本项目后端；价格 API 的五分钟同步由后端定时任务负责。
setInterval(() => { if (state.activePortfolioId) loadItems(); }, 60_000);
