(() => {
  "use strict";
  const toggle = document.getElementById("floating-ai-toggle");
  const panel = document.getElementById("floating-ai-panel");
  const closeButton = document.getElementById("floating-ai-close");
  const form = document.getElementById("floating-ai-form");
  const input = document.getElementById("floating-ai-question");
  const sendButton = document.getElementById("floating-ai-send");
  const messages = document.getElementById("floating-ai-messages");
  const status = document.getElementById("floating-ai-status");
  if (!toggle || !panel || !form || !input || !messages || !status) return;

  const storageKey = "stageon-ai-floating-conversation-id";
  const conversationId = (() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) return saved;
      const created = globalThis.crypto?.randomUUID?.() ?? `stageon-${Date.now()}`;
      localStorage.setItem(storageKey, created);
      return created;
    } catch { return `stageon-${Date.now()}`; }
  })();
  let pending = false;
  let historyPromise;
  let csrfToken = "";
  let csrfHeader = "";

  function csrfHeaders() {
    const headers = {"Content-Type": "application/json", "Accept": "application/json"};
    const token = document.querySelector('meta[name="_csrf"]')?.content || csrfToken;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || csrfHeader;
    if (token && header) headers[header] = token;
    return headers;
  }
  function setOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "AI 추천 채팅 닫기" : "AI 추천 채팅 열기");
    if (open) { loadHistory(); setTimeout(() => input.focus(), 30); }
    else toggle.focus();
  }
  function appendMessage(text, type = "assistant") {
    const bubble = document.createElement("div");
    bubble.className = `floating-ai-message floating-ai-message--${type}`;
    const content = document.createElement("span");
    content.textContent = text;
    bubble.appendChild(content);
    messages.appendChild(bubble);
    messages.scrollTop = messages.scrollHeight;
    return bubble;
  }
  function appendPerformances(container, performances) {
    if (!Array.isArray(performances) || performances.length === 0) return;
    const list = document.createElement("div");
    list.className = "floating-ai-results";
    performances.slice(0, 5).forEach((performance) => {
      const card = document.createElement("article");
      card.className = "floating-ai-result";
      const image = document.createElement(performance.posterUrl ? "img" : "div");
      if (performance.posterUrl) {
        image.src = performance.posterUrl;
        image.alt = `${performance.name || "공연"} 포스터`;
        image.loading = "lazy";
      } else { image.className = "floating-ai-result__placeholder"; image.textContent = "STAGE ON"; }
      const body = document.createElement("div");
      const title = document.createElement("strong");
      title.textContent = performance.name || "공연명 정보 없음";
      const meta = document.createElement("small");
      meta.textContent = [performance.genre, performance.venue, performance.price].filter(Boolean).join(" · ");
      body.append(title, meta);
      if (performance.bookingUrl) {
        const link = document.createElement("a");
        link.href = performance.bookingUrl;
        link.textContent = "예매하기";
        body.appendChild(link);
      }
      card.append(image, body);
      list.appendChild(card);
    });
    container.appendChild(list);
    messages.scrollTop = messages.scrollHeight;
  }
  async function loadHistory() {
    if (historyPromise) return historyPromise;
    historyPromise = (async () => { try {
      const response = await fetch("/api/ai/history", {headers: {"Accept": "application/json"}});
      if (!response.ok) return;
      const data = await response.json();
      csrfToken = data.csrfToken || "";
      csrfHeader = data.csrfHeader || "";
      if (!data.authenticated) {
        status.textContent = "로그인하면 예매·찜 기반 추천과 대화 저장을 사용할 수 있어요.";
        return;
      }
      if (!Array.isArray(data.messages) || data.messages.length === 0) {
        status.textContent = `${data.displayName || "회원"}님의 예매·찜 취향을 추천에 반영할 수 있어요.`;
        return;
      }
      messages.replaceChildren();
      data.messages.forEach((item) => { appendMessage(item.question, "user"); appendMessage(item.answer, "assistant"); });
      status.textContent = `${data.displayName || "회원"}님의 저장된 최근 대화입니다.`;
    } catch { status.textContent = "저장된 대화를 불러오지 못했습니다. 페이지를 새로고침해 주세요."; } })();
    return historyPromise;
  }
  async function ask(rawQuestion) {
    const question = rawQuestion.trim();
    if (!question || pending) return;
    await loadHistory();
    appendMessage(question, "user");
    input.value = "";
    pending = true;
    sendButton.disabled = true;
    input.disabled = true;
    status.textContent = "StageOn 예매 가능 공연을 확인하고 있어요…";
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 245000);
    try {
      const response = await fetch("/api/ai/chat", {
        method: "POST", headers: csrfHeaders(),
        body: JSON.stringify({message: question, conversationId}), signal: controller.signal
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || data.message || "질문을 처리하지 못했습니다.");
      const bubble = appendMessage(data.answer || "답변을 만들지 못했습니다.");
      appendPerformances(bubble, data.performances);
      status.textContent = data.dataSource === "STAGEON" ? "StageOn 예매 가능 데이터 기준" : "";
    } catch (error) {
      appendMessage(error.name === "AbortError" ? "답변 시간이 초과되었습니다. 다시 질문해 주세요." : error.message, "error");
      status.textContent = "";
    } finally {
      clearTimeout(timeout);
      pending = false;
      sendButton.disabled = false;
      input.disabled = false;
      input.focus();
    }
  }
  toggle.addEventListener("click", () => setOpen(panel.hidden));
  closeButton?.addEventListener("click", () => setOpen(false));
  form.addEventListener("submit", (event) => { event.preventDefault(); ask(input.value); });
  document.querySelectorAll("[data-floating-ai-question]").forEach((button) => {
    button.addEventListener("click", () => ask(button.dataset.floatingAiQuestion || ""));
  });
  document.addEventListener("keydown", (event) => { if (event.key === "Escape" && !panel.hidden) setOpen(false); });
})();
