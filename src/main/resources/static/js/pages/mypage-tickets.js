document.addEventListener("DOMContentLoaded", () => {

    /* =========================================================
       1. 화면 로딩 시 목록에 작은 QR 코드들 그리기
    ========================================================== */
    document.querySelectorAll(".ticket-qr__button").forEach(button => {
        const status = button.dataset.ticketStatus;
        const qrText = button.dataset.qrImage; // (정적 QR일 때만 그려짐)
        const canvasContainer = button.querySelector(".ticket-qr__list-canvas");

        if (status === "AVAILABLE" && qrText && canvasContainer) {
            new QRCode(canvasContainer, {
                text: qrText,
                width: 90,
                height: 90,
                colorDark : "#000000",
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.M
            });
        }
    });

    /* =========================================================
       2. QR 확대 모달용 전역 변수 세팅
    ========================================================== */
    const qrModal = document.getElementById("ticketQrModal");
    if (!qrModal) return;

    const modalTitle = qrModal.querySelector("[data-modal-title]");
    const modalSeat = qrModal.querySelector("[data-modal-seat]");
    const modalQrView = qrModal.querySelector("[data-modal-qr-view]");
    const modalQrCanvas = document.getElementById("modal-qr-canvas");

    const modalUnavailable = qrModal.querySelector("[data-modal-unavailable]");
    const modalUnavailableTitle = qrModal.querySelector("[data-modal-unavailable-title]");
    const modalUnavailableText = qrModal.querySelector("[data-modal-unavailable-text]");

    const closeButton = qrModal.querySelector(".ticket-modal__close");
    const overlay = qrModal.querySelector(".ticket-modal__overlay");

    // 🚨 타이머 관리를 위한 변수
    let qrRefreshInterval;
    let countdownInterval;
    const timeLeftSpan = document.getElementById("qr-time-left");


    /* =========================================================
       3. API 호출 및 동적 QR + 카운트다운 그리기 함수 (에러 해결!)
    ========================================================== */
    function fetchAndDrawDynamicQr(ticketId) {
        // 백엔드 API 호출
        fetch(`/api/tickets/${ticketId}/qr-token`)
            .then(res => {
                if (!res.ok) throw new Error("토큰 발급 실패");
                return res.json();
            })
            .then(data => {
                // 도화지 초기화 및 새 토큰으로 QR 그리기
                if (modalQrCanvas) {
                    modalQrCanvas.innerHTML = "";
                    new QRCode(modalQrCanvas, {
                        text: data.token,
                        width: 200,
                        height: 200,
                        colorDark : "#000000",
                        colorLight : "#ffffff",
                        correctLevel : QRCode.CorrectLevel.H
                    });
                }

                // 카운트다운 타이머(숫자 줄어드는 효과) 초기화 및 시작
                let timeLeft = data.expiresIn || 30;
                if (timeLeftSpan) timeLeftSpan.textContent = timeLeft;

                clearInterval(countdownInterval); // 기존 카운트다운 멈춤
                countdownInterval = setInterval(() => {
                    timeLeft--;
                    if (timeLeftSpan) timeLeftSpan.textContent = timeLeft;
                    if (timeLeft <= 0) clearInterval(countdownInterval);
                }, 1000);
            })
            .catch(err => {
                console.error("QR 갱신 에러:", err);
                if (modalQrCanvas) {
                    modalQrCanvas.innerHTML = "<p style='color:red; font-size:14px; text-align:center;'>QR 코드를<br>불러오지 못했습니다.</p>";
                }
            });
    }


    /* =========================================================
       4. QR 모달 열기
    ========================================================== */
    function openQrModal(button) {
        const status = button.dataset.ticketStatus;
        const title = button.dataset.ticketTitle;
        const seat = button.dataset.ticketSeat;
        const ticketId = button.dataset.ticketId;

        if (modalTitle) modalTitle.textContent = title || "공연 정보";
        if (modalSeat) modalSeat.textContent = seat || "좌석 정보";

        if (modalQrView) modalQrView.hidden = true;
        if (modalUnavailable) modalUnavailable.hidden = true;

        if (status === "AVAILABLE") {
            if (modalQrView) modalQrView.hidden = false;

            // 모달 열자마자 첫 번째 토큰 받아와서 그리기
            fetchAndDrawDynamicQr(ticketId);

            // 혹시 돌고 있던 이전 타이머 확실히 죽이고 30초 무한 루프 시작
            clearInterval(qrRefreshInterval);
            qrRefreshInterval = setInterval(() => {
                fetchAndDrawDynamicQr(ticketId);
            }, 30000);

        } else if (status === "UPCOMING") {
            if (modalUnavailable) modalUnavailable.hidden = false;
            if (modalUnavailableTitle) modalUnavailableTitle.textContent = "아직 QR 코드를 사용할 수 없습니다.";
            if (modalUnavailableText) modalUnavailableText.textContent = "입장용 QR 코드는 공연 시작 전부터 활성화됩니다.";
        } else if (status === "ENDED") {
            if (modalUnavailable) modalUnavailable.hidden = false;
            if (modalUnavailableTitle) modalUnavailableTitle.textContent = "종료된 공연입니다.";
            if (modalUnavailableText) modalUnavailableText.textContent = "공연 종료 후에는 입장용 QR 코드를 사용할 수 없습니다.";
        } else {
            if (modalUnavailable) modalUnavailable.hidden = false;
            if (modalUnavailableTitle) modalUnavailableTitle.textContent = "현재 QR 코드를 사용할 수 없습니다.";
            if (modalUnavailableText) modalUnavailableText.textContent = "티켓 상태를 확인해 주세요.";
        }

        qrModal.classList.add("is-open");
        qrModal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }


    /* =========================================================
       5. QR 모달 닫기
    ========================================================== */
    function closeQrModal() {
        qrModal.classList.remove("is-open");
        qrModal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";

        if (modalQrCanvas) {
            modalQrCanvas.innerHTML = "";
        }

        // 🚨 모달이 닫히면 무한루프 타이머와 카운트다운을 즉시 폭파!
        clearInterval(qrRefreshInterval);
        clearInterval(countdownInterval);
    }


    /* =========================================================
       6. 이벤트 리스너 등록 (클릭 등)
    ========================================================== */
    document.querySelectorAll(".ticket-qr__button").forEach(button => {
        button.addEventListener("click", () => {
            const status = button.dataset.ticketStatus;
            if (status !== "AVAILABLE") return;
            openQrModal(button);
        });
    });

    if (closeButton) closeButton.addEventListener("click", closeQrModal);
    if (overlay) overlay.addEventListener("click", closeQrModal);

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && qrModal.classList.contains("is-open")) {
            closeQrModal();
        }
    });

});