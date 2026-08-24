(() => {
  "use strict";

  const form = document.getElementById("ai-chat-form");
  const input = document.getElementById("ai-question");
  const sendButton = document.getElementById("ai-send-button");
  const messages = document.getElementById("ai-messages");
  const status = document.getElementById("ai-chat-status");

  if (!form || !input || !sendButton || !messages || !status) return;

  const conversationStorageKey = "stageon-ai-conversation-id";
  const createConversationId = () => globalThis.crypto?.randomUUID?.()
    ?? `stageon-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  let conversationId;
  try {
    conversationId = sessionStorage.getItem(conversationStorageKey) || createConversationId();
    sessionStorage.setItem(conversationStorageKey, conversationId);
  } catch {
    conversationId = createConversationId();
  }
  let pending = false;

  function appendMessage(text, type = "assistant") {
    const message = document.createElement("div");
    message.className = "message";
    if (type === "user") message.classList.add("message--user");
    if (type === "error") message.classList.add("message--error");
    message.textContent = text;
    messages.appendChild(message);
    messages.scrollTop = messages.scrollHeight;
    return message;
  }

  function formatDate(value) {
    if (!value) return "날짜 정보 없음";
    return value.replaceAll("-", ".");
  }

  function appendPerformances(container, performances, dataSource) {
    if (!Array.isArray(performances) || performances.length === 0) return;

    const results = document.createElement("div");
    results.className = "ai-performance-results";

    performances.slice(0, 5).forEach((performance) => {
      const card = document.createElement("article");
      card.className = "ai-performance-result";

      if (performance.posterUrl) {
        const image = document.createElement("img");
        image.src = performance.posterUrl;
        image.alt = `${performance.name ?? "공연"} 포스터`;
        image.loading = "lazy";
        card.appendChild(image);
      } else {
        const placeholder = document.createElement("div");
        placeholder.className = "ai-performance-result__placeholder";
        placeholder.textContent = "NO IMAGE";
        card.appendChild(placeholder);
      }

      const copy = document.createElement("div");
      const badge = document.createElement("span");
      badge.className = `ai-performance-result__badge ${dataSource === "STAGEON" ? "is-stageon" : "is-kopis"}`;
      badge.textContent = dataSource === "STAGEON" ? "StageOn 예매 가능" : "KOPIS 공연정보";
      const title = document.createElement("strong");
      title.textContent = performance.name || "공연명 정보 없음";
      const meta = document.createElement("p");
      meta.textContent = [
        performance.genre,
        performance.venue,
        performance.region,
        `${formatDate(performance.startDate)} ~ ${formatDate(performance.endDate)}`
      ].filter(Boolean).join(" · ");
      copy.append(badge, title, meta);
      if (dataSource === "STAGEON" && performance.bookingUrl) {
        const bookingLink = document.createElement("a");
        bookingLink.className = "ai-performance-result__booking";
        bookingLink.href = performance.bookingUrl;
        bookingLink.textContent = "예매하기";
        copy.appendChild(bookingLink);
      } else if (dataSource === "KOPIS") {
        const notice = document.createElement("small");
        notice.textContent = "StageOn 예매 여부 미확인";
        copy.appendChild(notice);
      }
      card.appendChild(copy);
      results.appendChild(card);
    });

    container.appendChild(results);
    messages.scrollTop = messages.scrollHeight;
  }

  function getErrorMessage(response, data) {
    if (response.status === 429) return "AI가 다른 질문에 답변 중입니다. 잠시 후 다시 시도해 주세요.";
    if (response.status === 503) return "AI 서버가 현재 꺼져 있거나 연결할 수 없습니다.";
    return data?.error || data?.message || "질문을 처리하지 못했습니다.";
  }

  async function ask(question) {
    const trimmed = question.trim();
    if (!trimmed || pending) return;

    appendMessage(trimmed, "user");
    input.value = "";
    pending = true;
    sendButton.disabled = true;
    input.disabled = true;
    status.textContent = "KOPIS 공연 정보를 확인하고 답변을 만들고 있습니다. 첫 질문은 시간이 걸릴 수 있어요.";

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 245_000);
    const headers = {"Content-Type": "application/json", "Accept": "application/json"};
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    try {
      const response = await fetch("/api/ai/chat", {
        method: "POST",
        headers,
        body: JSON.stringify({message: trimmed, conversationId}),
        signal: controller.signal
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(getErrorMessage(response, data));

      const answerMessage = appendMessage(data.answer || "답변을 만들지 못했습니다.");
      appendPerformances(answerMessage, data.performances, data.dataSource);
      status.textContent = data.dataSource
        ? `데이터 출처: ${data.dataSource}${data.dataUpdatedAt ? ` · 기준일: ${data.dataUpdatedAt}` : ""}`
        : "";
    } catch (error) {
      const message = error.name === "AbortError"
        ? "답변 시간이 초과되었습니다. 질문을 줄여 다시 시도해 주세요."
        : error.message;
      appendMessage(message, "error");
      status.textContent = "";
    } finally {
      clearTimeout(timeoutId);
      pending = false;
      sendButton.disabled = false;
      input.disabled = false;
      input.focus();
    }
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    ask(input.value);
  });

  document.querySelectorAll("[data-ai-question]").forEach((button) => {
    button.addEventListener("click", () => ask(button.dataset.aiQuestion || ""));
  });
})();
