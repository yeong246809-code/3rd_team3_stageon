document.addEventListener("DOMContentLoaded", () => {

    const selectAllCheckbox = document.getElementById("seatSelectAll");
    const selectedCancelButton = document.getElementById("selectedSeatCancelButton");
    const seatCheckboxes = Array.from(document.querySelectorAll(".seat-cancel-checkbox"));
    const seatCancelButtons = Array.from(document.querySelectorAll(".seat-cancel-button"));

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
       전체 선택 체크박스 이벤트
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
       개별 좌석 체크박스 이벤트
    ========================================================== */
    seatCheckboxes.forEach(checkbox => {
        checkbox.addEventListener("change", () => {
            updateSelectionState();
        });
    });

    /* =========================================================
       🚀 [백엔드 API 호출] 서버로 취소 요청을 보내는 공통 함수
    ========================================================== */
    async function requestCancel(reservationId, seatIds) {
        if (!confirm(`선택하신 ${seatIds.length}개의 좌석을 정말 취소/환불하시겠습니까?`)) {
            return;
        }

        try {
            // 💡 1. HTML에 숨겨둔 CSRF 암호를 찾아옵니다.
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            // 💡 2. 보낼 헤더(Headers) 봇짐을 만듭니다.
            const headers = {
                'Content-Type': 'application/json'
            };

            // 💡 3. 암호가 있으면 봇짐에 챙겨 넣습니다.
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            // 백엔드의 취소 컨트롤러로 요청 전송
            const response = await fetch('/api/reservations/cancel', {
                method: 'POST',
                headers: headers, // 💡 챙겨둔 봇짐(헤더)을 들고 갑니다.
                body: JSON.stringify({
                    reservationId: reservationId,
                    reservationSeatIds: seatIds,
                    cancelReason: "고객 변심"
                })
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || "취소 처리에 실패했습니다.");
            }

            alert("성공적으로 취소되었습니다.");
            window.location.reload();

        } catch (error) {
            console.error("Cancel Error:", error);
            alert(error.message);
        }
    }

    /* =========================================================
       선택 좌석 일괄 취소 버튼 클릭 이벤트
    ========================================================== */
    if (selectedCancelButton) {
        selectedCancelButton.addEventListener("click", () => {
            const selectedSeatIds = getSelectedSeatIds();

            if (selectedSeatIds.length === 0) {
                return;
            }

            // 첫 번째 개별 취소 버튼에서 공통된 reservationId를 가져옵니다.
            const firstButton = document.querySelector('.seat-cancel-button');
            if (!firstButton) return;

            const reservationId = Number(firstButton.dataset.reservationId);

            // API 호출 함수 실행 (배열 형태로 전송)
            requestCancel(reservationId, selectedSeatIds);
        });
    }

    /* =========================================================
       개별 좌석 취소 버튼 클릭 이벤트
    ========================================================== */
    seatCancelButtons.forEach(button => {
        button.addEventListener("click", () => {
            const reservationId = Number(button.dataset.reservationId);
            const reservationSeatId = Number(button.dataset.reservationSeatId);

            // API 호출 함수 실행 (단건이더라도 배열 형태로 감싸서 전송)
            requestCancel(reservationId, [reservationSeatId]);
        });
    });

    /* =========================================================
       최초 화면 상태 설정
    ========================================================== */
    updateSelectionState();

});