document.addEventListener("DOMContentLoaded", () => {

    const selectAllCheckbox = document.getElementById("seatSelectAll");
    const selectedCancelButton = document.getElementById("selectedSeatCancelButton");
    const seatCheckboxes = Array.from(document.querySelectorAll(".seat-cancel-checkbox"));
    const seatCancelButtons = Array.from(document.querySelectorAll(".seat-cancel-button"));

    // 모달 관련 요소
    const refundModal = document.getElementById("refundModal");
    const refundForm = document.getElementById("refundForm");
    const closeRefundModalBtn = document.getElementById("closeRefundModalBtn");

    // 취소 타겟을 임시 저장할 전역 변수
    let targetReservationId = null;
    let targetSeatIds = [];

    /* =========================================================
       선택된 좌석 ID 목록 반환
    ========================================================== */
    function getSelectedSeatIds() {
        return seatCheckboxes
            .filter(checkbox => checkbox.checked)
            .map(checkbox => Number(checkbox.value));
    }

    /* =========================================================
       선택 상태에 따라 상단 버튼 / 전체 선택 상태 갱신
    ========================================================== */
    function updateSelectionState() {
        const enabledCheckboxes = seatCheckboxes.filter(checkbox => !checkbox.disabled);
        const selectedCheckboxes = enabledCheckboxes.filter(checkbox => checkbox.checked);

        if (selectedCancelButton) {
            selectedCancelButton.disabled = selectedCheckboxes.length === 0;
        }

        if (selectAllCheckbox) {
            const allSelected = enabledCheckboxes.length > 0 && selectedCheckboxes.length === enabledCheckboxes.length;
            const partiallySelected = selectedCheckboxes.length > 0 && selectedCheckboxes.length < enabledCheckboxes.length;

            selectAllCheckbox.checked = allSelected;
            selectAllCheckbox.indeterminate = partiallySelected;
        }
    }

    /* =========================================================
       전체 선택 체크박스 변경 이벤트
    ========================================================== */
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener("change", () => {
            seatCheckboxes.forEach(checkbox => {
                if (!checkbox.disabled) {
                    checkbox.checked = selectAllCheckbox.checked;
                }
            });
            updateSelectionState();
        });
    }

    /* =========================================================
       개별 좌석 체크박스 변경 이벤트
    ========================================================== */
    seatCheckboxes.forEach(checkbox => {
        checkbox.addEventListener("change", () => {
            updateSelectionState();
        });
    });

    /* =========================================================
       모달 닫기 버튼 이벤트
    ========================================================== */
    if (closeRefundModalBtn) {
        closeRefundModalBtn.addEventListener("click", () => {
            refundModal.close();
            refundForm.reset();
        });
    }

    /* =========================================================
       🚀 공통 백엔드 API 전송 함수 (Fetch)
    ========================================================== */
    async function executeCancelRequest(reservationId, seatIds, bank, account, holder) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch('/api/reservations/cancel', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    reservationId: reservationId,
                    reservationSeatIds: seatIds,
                    cancelReason: "고객 변심",
                    refundBank: bank,
                    refundAccountNumber: account,
                    refundHolderName: holder
                })
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || "취소 처리에 실패했습니다.");
            }

            alert("성공적으로 취소 및 환불 처리되었습니다.");
            window.location.reload();

        } catch (error) {
            console.error("Cancel Error:", error);
            alert(error.message);
        }
    }

    /* =========================================================
       모달 내부 폼 제출 (가상계좌 입금 완료 환불 요청)
    ========================================================== */
    if (refundForm) {
        refundForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const bank = document.getElementById("refundBank").value;
            const account = document.getElementById("refundAccount").value;
            const holder = document.getElementById("refundHolder").value;

            await executeCancelRequest(targetReservationId, targetSeatIds, bank, account, holder);

            refundModal.close();
            refundForm.reset();
        });
    }

    /* =========================================================
       🚀 선택 좌석 일괄 취소 버튼 클릭 이벤트 (분기 처리 적용)
    ========================================================== */
    if (selectedCancelButton) {
        selectedCancelButton.addEventListener("click", () => {
            const selectedSeatIds = getSelectedSeatIds();
            if (selectedSeatIds.length === 0) return;

            const firstButton = document.querySelector('.seat-cancel-button');
            if (!firstButton) return;

            targetReservationId = Number(firstButton.dataset.reservationId);
            targetSeatIds = selectedSeatIds;

            const payMethod = selectedCancelButton.dataset.paymentMethod;
            const payStatus = selectedCancelButton.dataset.paymentStatus;

            // 가상계좌(VBANK) 이면서 입금 완료(SUCCESS) 상태일 때만 모달 띄우기
            if (payMethod === 'VBANK' && payStatus === 'SUCCESS') {
                refundModal.showModal();
            } else {
                if (confirm(`선택하신 ${targetSeatIds.length}개의 좌석을 정말 취소하시겠습니까?`)) {
                    executeCancelRequest(targetReservationId, targetSeatIds, "", "", "");
                }
            }
        });
    }

    /* =========================================================
       🚀 개별 좌석 취소 버튼 클릭 이벤트 (분기 처리 적용)
    ========================================================== */
    seatCancelButtons.forEach(button => {
        button.addEventListener("click", () => {
            targetReservationId = Number(button.dataset.reservationId);
            targetSeatIds = [Number(button.dataset.reservationSeatId)];

            const payMethod = button.dataset.paymentMethod;
            const payStatus = button.dataset.paymentStatus;

            if (payMethod === 'VBANK' && payStatus === 'SUCCESS') {
                refundModal.showModal();
            } else {
                if (confirm(`해당 좌석을 정말 취소하시겠습니까?`)) {
                    executeCancelRequest(targetReservationId, targetSeatIds, "", "", "");
                }
            }
        });
    });

    /* =========================================================
       최초 로드 시 화면 상태 세팅
    ========================================================== */
    updateSelectionState();

});