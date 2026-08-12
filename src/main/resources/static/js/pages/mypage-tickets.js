document.addEventListener("DOMContentLoaded", () => {

    /* =========================================================
       QR 확대 모달
    ========================================================== */

    const qrModal = document.getElementById("ticketQrModal");

    if (!qrModal) {
        return;
    }

    const modalTitle = qrModal.querySelector("[data-modal-title]");
    const modalSeat = qrModal.querySelector("[data-modal-seat]");

    const modalQrView = qrModal.querySelector("[data-modal-qr-view]");
    const modalQrImage = qrModal.querySelector("[data-modal-qr-image]");

    const modalUnavailable = qrModal.querySelector("[data-modal-unavailable]");
    const modalUnavailableTitle =
        qrModal.querySelector("[data-modal-unavailable-title]");
    const modalUnavailableText =
        qrModal.querySelector("[data-modal-unavailable-text]");

    const closeButton = qrModal.querySelector(".ticket-modal__close");
    const overlay = qrModal.querySelector(".ticket-modal__overlay");


    /* =========================================================
       QR 모달 열기
    ========================================================== */

    function openQrModal(button) {

        const status = button.dataset.ticketStatus;
        const title = button.dataset.ticketTitle;
        const seat = button.dataset.ticketSeat;
        const qrImage = button.dataset.qrImage;

        modalTitle.textContent = title || "공연 정보";
        modalSeat.textContent = seat || "좌석 정보";

        /*
         * 먼저 모든 상태 영역을 숨긴 뒤
         * 티켓 상태에 따라 필요한 영역만 표시합니다.
         */
        modalQrView.hidden = true;
        modalUnavailable.hidden = true;

        if (status === "AVAILABLE") {

            /* QR 사용 가능 */
            modalQrView.hidden = false;

            if (qrImage) {
                modalQrImage.src = qrImage;
            }

        } else if (status === "UPCOMING") {

            /* 공연 시작 24시간 전 */
            modalUnavailable.hidden = false;

            modalUnavailableTitle.textContent =
                "아직 QR 코드를 사용할 수 없습니다.";

            modalUnavailableText.textContent =
                "입장용 QR 코드는 공연 시작 24시간 전부터 활성화됩니다.";

        } else if (status === "ENDED") {

            /* 공연 종료 */
            modalUnavailable.hidden = false;

            modalUnavailableTitle.textContent =
                "종료된 공연입니다.";

            modalUnavailableText.textContent =
                "공연 종료 후에는 입장용 QR 코드를 사용할 수 없습니다.";

        } else {

            /* 예외 상태 */
            modalUnavailable.hidden = false;

            modalUnavailableTitle.textContent =
                "현재 QR 코드를 사용할 수 없습니다.";

            modalUnavailableText.textContent =
                "티켓 상태를 확인해 주세요.";
        }

        qrModal.classList.add("is-open");
        qrModal.setAttribute("aria-hidden", "false");

        document.body.style.overflow = "hidden";
    }


    /* =========================================================
       QR 모달 닫기
    ========================================================== */

    function closeQrModal() {

        qrModal.classList.remove("is-open");
        qrModal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";

        /*
         * 이전 티켓 QR 이미지가 다음 모달에 남지 않도록 초기화합니다.
         */
        modalQrImage.src = "";
    }


    /* =========================================================
       티켓 QR 버튼 이벤트
    ========================================================== */

    document.querySelectorAll(".ticket-qr__button")
        .forEach(button => {

            button.addEventListener("click", () => {
                openQrModal(button);
            });

        });


    /* =========================================================
       닫기 버튼
    ========================================================== */

    if (closeButton) {
        closeButton.addEventListener("click", closeQrModal);
    }


    /* =========================================================
       바깥 영역 클릭 시 닫기
    ========================================================== */

    if (overlay) {
        overlay.addEventListener("click", closeQrModal);
    }


    /* =========================================================
       ESC 키로 닫기
    ========================================================== */

    document.addEventListener("keydown", event => {

        if (
            event.key === "Escape"
            && qrModal.classList.contains("is-open")
        ) {
            closeQrModal();
        }

    });

});