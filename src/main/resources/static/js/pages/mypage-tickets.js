document.addEventListener("DOMContentLoaded", () => {

    // =========================================================
    // QR 확대 모달 관련 요소
    // =========================================================
    const qrModal = document.getElementById("ticketQrModal");

    if (!qrModal) {
        return;
    }

    const modalTitle =
        qrModal.querySelector("[data-modal-title]");

    const modalSeat =
        qrModal.querySelector("[data-modal-seat]");

    const modalQrView =
        qrModal.querySelector("[data-modal-qr-view]");

    const modalQrImage =
        qrModal.querySelector("[data-modal-qr-image]");

    const modalUnavailable =
        qrModal.querySelector("[data-modal-unavailable]");

    const modalUnavailableTitle =
        qrModal.querySelector("[data-modal-unavailable-title]");

    const modalUnavailableMessage =
        qrModal.querySelector("[data-modal-unavailable-message]");


    // =========================================================
    // QR 모달 열기
    // =========================================================
    function openQrModal(button) {

        const status = button.dataset.ticketStatus;
        const title = button.dataset.ticketTitle;
        const seat = button.dataset.ticketSeat;
        const qrImage = button.dataset.qrImage;

        // 선택한 티켓 정보 표시
        modalTitle.textContent = title || "공연명";
        modalSeat.textContent = seat || "좌석 정보";

        // -----------------------------------------------------
        // AVAILABLE
        // 실제 QR 확대 표시
        // -----------------------------------------------------
        if (status === "AVAILABLE") {

            modalQrView.hidden = false;
            modalUnavailable.hidden = true;

            modalQrImage.src = qrImage || "";
        }

            // -----------------------------------------------------
            // UPCOMING
            // QR 오픈 전
        // -----------------------------------------------------
        else if (status === "UPCOMING") {

            modalQrView.hidden = true;
            modalUnavailable.hidden = false;

            modalUnavailableTitle.textContent =
                "아직 QR 코드가 활성화되지 않았습니다.";

            modalUnavailableMessage.textContent =
                "공연 시작 24시간 전부터 입장용 QR 코드가 활성화됩니다.";
        }

            // -----------------------------------------------------
            // ENDED
            // 공연 종료
        // -----------------------------------------------------
        else if (status === "ENDED") {

            modalQrView.hidden = true;
            modalUnavailable.hidden = false;

            modalUnavailableTitle.textContent =
                "사용할 수 없는 티켓입니다.";

            modalUnavailableMessage.textContent =
                "공연이 종료되어 입장용 QR 코드를 사용할 수 없습니다.";
        }

        // 모달 표시
        qrModal.classList.add("is-open");
        qrModal.setAttribute("aria-hidden", "false");

        // 모달이 열린 동안 배경 스크롤 방지
        document.body.style.overflow = "hidden";
    }


    // =========================================================
    // QR 모달 닫기
    // =========================================================
    function closeQrModal() {

        qrModal.classList.remove("is-open");
        qrModal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";

        // 이전 QR 이미지 제거
        modalQrImage.src = "";
    }


    // =========================================================
    // 각 티켓의 QR 버튼 클릭
    // =========================================================
    const qrButtons =
        document.querySelectorAll(".ticket-qr__button");

    qrButtons.forEach((button) => {

        button.addEventListener("click", () => {
            openQrModal(button);
        });

    });


    // =========================================================
    // X 버튼으로 닫기
    // =========================================================
    const closeButton =
        qrModal.querySelector(".ticket-modal__close");

    if (closeButton) {

        closeButton.addEventListener("click", () => {
            closeQrModal();
        });

    }


    // =========================================================
    // 어두운 배경 클릭 시 닫기
    // =========================================================
    const overlay =
        qrModal.querySelector(".ticket-modal__overlay");

    if (overlay) {

        overlay.addEventListener("click", () => {
            closeQrModal();
        });

    }


    // =========================================================
    // ESC 키로 닫기
    // =========================================================
    document.addEventListener("keydown", (event) => {

        if (
            event.key === "Escape"
            && qrModal.classList.contains("is-open")
        ) {
            closeQrModal();
        }

    });

});