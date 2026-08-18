document.addEventListener("DOMContentLoaded", () => {

    const selectAllCheckbox = document.getElementById("seatSelectAll");
    const selectedCancelButton = document.getElementById("selectedSeatCancelButton");
    const seatCheckboxes = Array.from(document.querySelectorAll(".seat-cancel-checkbox"));
    const seatCancelButtons = Array.from(document.querySelectorAll(".seat-cancel-button"));

    // 가상계좌 환불 모달 관련 요소
    const refundModal = document.getElementById("refundModal");
    const refundForm = document.getElementById("refundForm");
    const closeRefundModalBtn = document.getElementById("closeRefundModalBtn");

    // 취소 사유 모달 관련 요소 (새로 추가됨)
    const cancelReasonModal = document.getElementById("cancelReasonModal");
    const cancelReasonForm = document.getElementById("cancelReasonForm");
    const cancelReasonSelect = document.getElementById("cancelReasonSelect");
    const customReasonWrapper = document.getElementById("customReasonWrapper");
    const customCancelReason = document.getElementById("customCancelReason");
    const closeCancelModalBtn = document.getElementById("closeCancelModalBtn");

    // 취소 타겟 및 결제 상태를 임시 저장할 전역 변수
    let targetReservationId = null;
    let targetSeatIds = [];
    let targetPayMethod = "";
    let targetPayStatus = "";
    let targetCancelReason = ""; // 선택된 취소 사유 저장용

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
       환불 계좌 모달 닫기 버튼 이벤트
    ========================================================== */
    if (closeRefundModalBtn) {
        closeRefundModalBtn.addEventListener("click", () => {
            refundModal.close();
            refundForm.reset();
        });
    }

    /* =========================================================
       🚀 취소 사유 모달 로직 (새로 추가됨)
    ========================================================== */
    // '기타' 선택 시 직접 입력창 노출
    if (cancelReasonSelect) {
        cancelReasonSelect.addEventListener("change", (e) => {
            if (e.target.value === "CUSTOM") {
                customReasonWrapper.style.display = "block";
                customCancelReason.required = true;
            } else {
                customReasonWrapper.style.display = "none";
                customCancelReason.required = false;
                customCancelReason.value = "";
            }
        });
    }

    // 취소 사유 모달 닫기
    if (closeCancelModalBtn) {
        closeCancelModalBtn.addEventListener("click", () => {
            cancelReasonModal.close();
            cancelReasonForm.reset();
            customReasonWrapper.style.display = "none";
        });
    }

    // 취소 사유 모달에서 '취소 진행' 폼 제출 시
    if (cancelReasonForm) {
        cancelReasonForm.addEventListener("submit", (e) => {
            e.preventDefault();

            // 최종 사유 결정
            let finalReason = cancelReasonSelect.value;
            if (finalReason === "CUSTOM") {
                finalReason = customCancelReason.value.trim();
                if (!finalReason) {
                    alert("취소 사유를 입력해 주세요.");
                    return;
                }
            }

            targetCancelReason = finalReason; // 결정된 사유 저장
            cancelReasonModal.close(); // 현재 모달 닫기

            // 다음 흐름 분기 처리 (가상계좌 결제완료 건은 환불 계좌 모달 띄우기)
            if (targetPayMethod === "VBANK" && targetPayStatus === "SUCCESS") {
                refundModal.showModal();
            } else {
                // 일반 결제는 즉시 API 호출
                executeCancelRequest(targetReservationId, targetSeatIds, targetCancelReason, "", "", "");
            }
        });
    }

    /* =========================================================
       🚀 공통 백엔드 API 전송 함수 (Fetch) - 취소 사유 파라미터 추가
    ========================================================== */
    async function executeCancelRequest(reservationId, seatIds, reason, bank, account, holder) {
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
                    cancelReason: reason, // 💡 동적으로 받은 사유를 담아서 보냅니다.
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
       환불 계좌 모달 내부 폼 제출 (가상계좌 입금 완료 환불 요청)
    ========================================================== */
    if (refundForm) {
        refundForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const bank = document.getElementById("refundBank").value;
            const account = document.getElementById("refundAccount").value;
            const holder = document.getElementById("refundHolder").value;

            // 💡 targetCancelReason도 함께 보냅니다!
            await executeCancelRequest(targetReservationId, targetSeatIds, targetCancelReason, bank, account, holder);

            refundModal.close();
            refundForm.reset();
        });
    }

    /* =========================================================
       🚀 선택 좌석 일괄 취소 버튼 클릭 이벤트
    ========================================================== */
    if (selectedCancelButton) {
        selectedCancelButton.addEventListener("click", () => {
            const selectedSeatIds = getSelectedSeatIds();
            if (selectedSeatIds.length === 0) return;

            const firstButton = document.querySelector('.seat-cancel-button');
            if (!firstButton) return;

            // 💡 데이터를 타겟 변수에 세팅하고 사유 모달을 띄웁니다!
            targetReservationId = Number(firstButton.dataset.reservationId);
            targetSeatIds = selectedSeatIds;
            targetPayMethod = selectedCancelButton.dataset.paymentMethod;
            targetPayStatus = selectedCancelButton.dataset.paymentStatus;

            openCancelModalWithPreview();
        });
    }

    /* =========================================================
       🚀 개별 좌석 취소 버튼 클릭 이벤트
    ========================================================== */
    seatCancelButtons.forEach(button => {
        button.addEventListener("click", () => {
            // 💡 데이터를 타겟 변수에 세팅하고 사유 모달을 띄웁니다!
            targetReservationId = Number(button.dataset.reservationId);
            targetSeatIds = [Number(button.dataset.reservationSeatId)];
            targetPayMethod = button.dataset.paymentMethod;
            targetPayStatus = button.dataset.paymentStatus;

            openCancelModalWithPreview();
        });
    });

    /* =========================================================
       🚀 예상 환불 금액 계산 및 모달 띄우기 함수
    ========================================================== */
    function openCancelModalWithPreview() {
        // 1. 선택된 좌석들의 총 금액 합산
        let totalPrice = 0;
        document.querySelectorAll('.seat-cancel-checkbox').forEach(cb => {
            if (targetSeatIds.includes(Number(cb.value))) {
                totalPrice += Number(cb.dataset.price); // HTML에 심어둔 가격 합산
            }
        });

        // 2. 공연 시작일 가져오기 및 남은 일수(D-Day) 계산
        const summaryEl = document.querySelector('.detail-summary');
        const startsAtStr = summaryEl.dataset.startsAt;

        const today = new Date();
        today.setHours(0, 0, 0, 0); // 시간 제외하고 날짜만 비교
        const pDate = new Date(startsAtStr);
        pDate.setHours(0, 0, 0, 0);

        const diffDays = Math.floor((pDate - today) / (1000 * 60 * 60 * 24));

        // 3. 수수료율 결정 (백엔드 CancelFeePolicy와 100% 동일)
        let feeRate = 0.0;
        if (diffDays >= 8) feeRate = 0.0;
        else if (diffDays >= 3) feeRate = 0.1; // 7~3일 전 10%
        else if (diffDays >= 1) feeRate = 0.2; // 2~1일 전 20%
        else feeRate = 1.0;                    // 당일 불가(100%)

        if (feeRate === 1.0) {
            alert("공연 당일은 취소 및 환불이 불가능합니다.");
            return;
        }

        const feeAmount = Math.floor(totalPrice * feeRate);
        const refundAmount = totalPrice - feeAmount;

        // 4. 모달 UI 영수증 텍스트 업데이트
        document.getElementById('previewOriginalAmount').textContent = totalPrice.toLocaleString() + '원';
        document.getElementById('previewFeeRate').textContent = (feeRate * 100) + '%';
        document.getElementById('previewFeeAmount').textContent = '- ' + feeAmount.toLocaleString() + '원';
        document.getElementById('previewRefundAmount').textContent = refundAmount.toLocaleString() + '원';

        // 5. 계산이 끝났으니 짠! 하고 모달 띄우기
        cancelReasonModal.showModal();
    }

    /* =========================================================
       최초 로드 시 화면 상태 세팅
    ========================================================== */
    updateSelectionState();

});