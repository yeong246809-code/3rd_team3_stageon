(() => {
  "use strict";

  const form = document.getElementById("ai-chat-form");
  const input = document.getElementById("ai-question");
  const sendButton = document.getElementById("ai-send-button");
  const messages = document.getElementById("ai-messages");
  const status = document.getElementById("ai-chat-status");
  const roomList = document.getElementById("ai-room-list");
  const roomTitle = document.getElementById("ai-conversation-title");
  const roomLimit = document.getElementById("ai-room-limit");
  const memberSummary = document.getElementById("ai-member-summary");
  const newChatButton = document.getElementById("ai-new-chat");
  const clearButton = document.getElementById("ai-clear-history");
  const sidebar = document.getElementById("ai-room-sidebar");
  const sidebarToggle = document.getElementById("ai-room-toggle");
  const sidebarClose = document.getElementById("ai-room-sidebar-close");
  const overlay = document.getElementById("ai-room-overlay");

  if (!form || !input || !sendButton || !messages || !status || !roomList) return;

  const storageKey = "stageon-ai-active-room-id";
  const createConversationId = () => globalThis.crypto?.randomUUID?.()
    ?? `stageon-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  let conversationId = createConversationId();
  let rooms = [];
  let authenticated = false;
  let pending = false;
  let csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  let csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "";

  try { conversationId = sessionStorage.getItem(storageKey) || conversationId; } catch { /* 저장 불가 환경 */ }

  function rememberActiveRoom() {
    try { sessionStorage.setItem(storageKey, conversationId); } catch { /* 저장 불가 환경 */ }
  }

  function requestHeaders() {
    const headers = {"Content-Type": "application/json", "Accept": "application/json"};
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    return headers;
  }

  function redirectToLogin() {
    try { sessionStorage.setItem("stageon-ai-draft", input.value); } catch { /* 저장 불가 환경 */ }
    window.location.assign("/login?required=1&ai=1");
  }

  function setSidebar(open) {
    sidebar.classList.toggle("is-open", open);
    overlay.hidden = !open;
    sidebarToggle?.setAttribute("aria-expanded", String(open));
    if (open) sidebarClose?.focus();
  }

  function scrollToLatest() {
    messages.scrollTop = messages.scrollHeight;
  }

  function appendMessage(text, type = "assistant") {
    const row = document.createElement("div");
    row.className = `ai-message ai-message--${type}`;
    if (type !== "user") {
      const avatar = document.createElement("span");
      avatar.className = "ai-message__avatar";
      avatar.textContent = type === "error" ? "!" : "S";
      avatar.setAttribute("aria-hidden", "true");
      row.appendChild(avatar);
    }
    const body = document.createElement("div");
    body.className = "ai-message__body";
    const copy = document.createElement("p");
    copy.textContent = text;
    body.appendChild(copy);
    row.appendChild(body);
    messages.appendChild(row);
    scrollToLatest();
    return body;
  }

  function appendLoading() {
    const row = document.createElement("div");
    row.className = "ai-message ai-message--assistant ai-message--loading";
    row.setAttribute("role", "status");
    row.setAttribute("aria-live", "polite");
    const avatar = document.createElement("span");
    avatar.className = "ai-message__avatar";
    avatar.textContent = "S";
    avatar.setAttribute("aria-hidden", "true");
    const body = document.createElement("div");
    body.className = "ai-message__body ai-loading-bubble";
    const spinner = document.createElement("span");
    spinner.className = "ai-loading-spinner";
    spinner.setAttribute("aria-hidden", "true");
    const copy = document.createElement("span");
    copy.textContent = "답변을 생성 중입니다";
    body.append(spinner, copy);
    row.append(avatar, body);
    messages.appendChild(row);
    scrollToLatest();
    return row;
  }

  function showWelcome() {
    messages.replaceChildren();
    const body = appendMessage(
      authenticated
        ? "새 채팅방입니다. 최근 예매와 찜한 공연의 장르를 반영해 추천해 드릴게요."
        : "새 채팅방입니다. 로그인하면 예매·찜 기반 추천과 대화방 저장을 사용할 수 있어요."
    );
    const heading = document.createElement("b");
    heading.textContent = "StageOn AI";
    body.prepend(heading);
  }

  function formatRoomDate(value) {
    if (!value) return "저장됨";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "저장됨";
    const today = new Date();
    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString("ko-KR", {hour: "2-digit", minute: "2-digit"});
    }
    return date.toLocaleDateString("ko-KR", {month: "short", day: "numeric"});
  }

  function renderRoomList() {
    roomList.replaceChildren();
    if (!authenticated) {
      const empty = document.createElement("p");
      empty.className = "ai-room-list__empty";
      empty.textContent = "로그인하면 채팅방이 여기에 저장됩니다.";
      roomList.appendChild(empty);
      clearButton.hidden = true;
      return;
    }
    clearButton.hidden = rooms.length === 0;
    if (rooms.length === 0) {
      const empty = document.createElement("p");
      empty.className = "ai-room-list__empty";
      empty.textContent = "아직 저장된 채팅방이 없습니다.";
      roomList.appendChild(empty);
      return;
    }
    rooms.forEach((room) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "ai-room-item";
      button.dataset.conversationId = room.conversationId;
      if (room.conversationId === conversationId) {
        button.classList.add("is-active");
        button.setAttribute("aria-current", "page");
      }
      const icon = document.createElement("span");
      icon.className = "ai-room-item__icon";
      icon.textContent = "S";
      icon.setAttribute("aria-hidden", "true");
      const copy = document.createElement("span");
      copy.className = "ai-room-item__copy";
      const title = document.createElement("strong");
      title.textContent = room.title || "공연 추천 대화";
      const meta = document.createElement("small");
      meta.textContent = `${formatRoomDate(room.updatedAt)} · 대화 ${(room.messages || []).length}개`;
      copy.append(title, meta);
      button.append(icon, copy);
      button.addEventListener("click", () => selectRoom(room));
      roomList.appendChild(button);
    });
  }

  function selectRoom(room) {
    if (pending) return;
    conversationId = room.conversationId;
    rememberActiveRoom();
    roomTitle.textContent = room.title || "공연 추천 대화";
    messages.replaceChildren();
    (room.messages || []).forEach((item) => {
      appendMessage(item.question, "user");
      appendMessage(item.answer, "assistant");
    });
    if (!room.messages?.length) showWelcome();
    renderRoomList();
    setSidebar(false);
    input.focus();
  }

  function startNewChat() {
    if (pending) return;
    conversationId = createConversationId();
    rememberActiveRoom();
    roomTitle.textContent = "새 공연 추천 대화";
    status.textContent = "";
    showWelcome();
    renderRoomList();
    setSidebar(false);
    input.focus();
  }

  function formatDate(value) {
    return value ? value.replaceAll("-", ".") : "날짜 정보 없음";
  }

  function appendPerformances(container, performances) {
    if (!Array.isArray(performances) || performances.length === 0) return;
    const results = document.createElement("div");
    results.className = "ai-performance-results";
    performances.slice(0, 5).forEach((performance) => {
      const card = document.createElement("article");
      card.className = "ai-performance-result";
      const image = document.createElement(performance.posterUrl ? "img" : "div");
      if (performance.posterUrl) {
        image.src = performance.posterUrl;
        image.alt = `${performance.name || "공연"} 포스터`;
        image.loading = "lazy";
      } else {
        image.className = "ai-performance-result__placeholder";
        image.textContent = "STAGE ON";
      }
      const copy = document.createElement("div");
      const badge = document.createElement("span");
      badge.className = "ai-performance-result__badge is-stageon";
      badge.textContent = "StageOn 예매 가능";
      const title = document.createElement("strong");
      title.textContent = performance.name || "공연명 정보 없음";
      const meta = document.createElement("p");
      meta.textContent = [
        performance.genre,
        performance.venue,
        performance.region,
        `${formatDate(performance.startDate)} ~ ${formatDate(performance.endDate)}`,
        performance.price
      ].filter(Boolean).join(" · ");
      copy.append(badge, title, meta);
      if (performance.bookingUrl) {
        const bookingLink = document.createElement("a");
        bookingLink.className = "ai-performance-result__booking";
        bookingLink.href = performance.bookingUrl;
        bookingLink.textContent = "공연 상세보기";
        bookingLink.setAttribute(
          "aria-label",
          `${performance.name || "공연"} 공연 상세보기`
        );
        copy.appendChild(bookingLink);
      }
      card.append(image, copy);
      results.appendChild(card);
    });
    container.appendChild(results);
    scrollToLatest();
  }

  function getErrorMessage(response, data) {
    if (response.status === 429) return "AI가 다른 질문에 답변 중입니다. 잠시 후 다시 시도해 주세요.";
    if (response.status === 503) return "AI 서버가 현재 꺼져 있거나 연결할 수 없습니다.";
    return data?.error || data?.message || "질문을 처리하지 못했습니다.";
  }

  async function fetchHistory(renderMessages = true) {
    try {
      const response = await fetch("/api/ai/history", {headers: {"Accept": "application/json"}});
      if (response.status === 401) {
        redirectToLogin();
        return;
      }
      if (!response.ok) return;
      const data = await response.json();
      authenticated = Boolean(data.authenticated);
      rooms = Array.isArray(data.rooms) ? data.rooms : [];
      csrfToken = data.csrfToken || csrfToken;
      csrfHeader = data.csrfHeader || csrfHeader;
      roomLimit.innerHTML = authenticated
        ? `최근 채팅방은 최대 ${data.roomLimit || 10}개까지 저장됩니다.<br>초과하면 오래된 채팅방부터 자동 정리됩니다.`
        : "로그인하면 채팅방 최대 10개와 맞춤 추천이 저장됩니다.";
      memberSummary.textContent = authenticated
        ? `${data.displayName || "회원"}님의 최근 예매·찜 취향을 안전하게 반영합니다`
        : "로그인하면 예매·찜 기반 맞춤 추천을 받을 수 있습니다";
      renderRoomList();
      if (!renderMessages) return;
      const selected = rooms.find((room) => room.conversationId === conversationId) || rooms[0];
      if (selected) selectRoom(selected);
      else startNewChat();
    } catch {
      status.textContent = "저장된 채팅방을 불러오지 못했습니다. 새 대화는 계속 사용할 수 있어요.";
      showWelcome();
    }
  }

  async function ask(question) {
    const trimmed = question.trim();
    if (!trimmed || pending) return;
    appendMessage(trimmed, "user");
    const loadingRow = appendLoading();
    input.value = "";
    input.style.height = "auto";
    pending = true;
    sendButton.disabled = true;
    input.disabled = true;
    status.textContent = "StageOn 예매 가능 공연을 확인하고 있습니다. 첫 답변은 시간이 걸릴 수 있어요.";

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 245_000);
    try {
      const response = await fetch("/api/ai/chat", {
        method: "POST",
        headers: requestHeaders(),
        body: JSON.stringify({message: trimmed, conversationId}),
        signal: controller.signal
      });
      if (response.status === 401) {
        redirectToLogin();
        return;
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(getErrorMessage(response, data));
      loadingRow.remove();
      const answer = appendMessage(data.answer || "답변을 만들지 못했습니다.");
      appendPerformances(answer, data.performances);
      status.textContent = "StageOn 예매 가능 데이터 기준";
      await fetchHistory(false);
      const current = rooms.find((room) => room.conversationId === conversationId);
      if (current) roomTitle.textContent = current.title;
    } catch (error) {
      loadingRow.remove();
      appendMessage(
        error.name === "AbortError"
          ? "답변 시간이 초과되었습니다. 질문을 줄여 다시 시도해 주세요."
          : error.message,
        "error"
      );
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
  input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });
  input.addEventListener("input", () => {
    input.style.height = "auto";
    input.style.height = `${Math.min(input.scrollHeight, 120)}px`;
  });
  document.querySelectorAll("[data-ai-question]").forEach((button) => {
    button.addEventListener("click", () => ask(button.dataset.aiQuestion || ""));
  });
  newChatButton?.addEventListener("click", startNewChat);
  sidebarToggle?.addEventListener("click", () => setSidebar(!sidebar.classList.contains("is-open")));
  sidebarClose?.addEventListener("click", () => setSidebar(false));
  overlay?.addEventListener("click", () => setSidebar(false));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") setSidebar(false);
  });
  clearButton?.addEventListener("click", async () => {
    if (!authenticated || !confirm("저장된 AI 채팅방을 모두 삭제할까요?")) return;
    const response = await fetch("/api/ai/history", {method: "DELETE", headers: requestHeaders()});
    if (response.ok) {
      rooms = [];
      startNewChat();
      status.textContent = "저장된 채팅방을 모두 삭제했습니다.";
    } else status.textContent = "대화 기록을 삭제하지 못했습니다.";
  });

  fetchHistory();
})();
