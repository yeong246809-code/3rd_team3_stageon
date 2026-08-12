document.addEventListener("DOMContentLoaded", () => {

    /* =========================================================
       예매 상세 - 좌석 선택 / 일괄 취소 UI
       실제 취소·환불 API 호출은 담당 팀원이 연결합니다.
    ========================================================== */

    const selectAllCheckbox = document.getElementById("seatSelectAll");
    const selectedCancelButton =
        document.getElementById("selectedSeatCancelButton");

    const seatCheckboxes =
        Array.from(document.querySelectorAll(".seat-cancel-checkbox"));

    const seatCancelButtons =
        Array.from(document.querySelectorAll(".seat-cancel-button"));


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

        const enabledCheckboxes =
            seatCheckboxes.filter(checkbox => !checkbox.disabled);

        const selectedCheckboxes =
            enabledCheckboxes.filter(checkbox => checkbox.checked);

        /*
         * 선택된 좌석이 하나라도 있을 때만
         * '선택 취소/환불' 버튼을 활성화합니다.
         */
        if (selectedCancelButton) {
            selectedCancelButton.disabled =
                selectedCheckboxes.length === 0;
        }

        /*
         * 전체 선택 체크박스 상태
         */
        if (selectAllCheckbox) {

            const allSelected =
                enabledCheckboxes.length > 0
                && selectedCheckboxes.length === enabledCheckboxes.length;

            const partiallySelected =
                selectedCheckboxes.length > 0
                && selectedCheckboxes.length < enabledCheckboxes.length;

            selectAllCheckbox.checked = allSelected;

            /*
             * 일부만 선택되었을 경우 브라우저 기본
             * indeterminate 상태를 사용합니다.
             */
            selectAllCheckbox.indeterminate = partiallySelected;
        }
    }


    /* =========================================================
       전체 선택
    ========================================================== */
    if (selectAllCheckbox) {

        selectAllCheckbox.addEventListener("change", () => {

            seatCheckboxes.forEach(checkbox => {

                /*
                 * 취소 불가능한 좌석은 건드리지 않습니다.
                 */
                if (!checkbox.disabled) {
                    checkbox.checked = selectAllCheckbox.checked;
                }

            });

            updateSelectionState();
        });
    }


    /* =========================================================
       개별 좌석 선택
    ========================================================== */
    seatCheckboxes.forEach(checkbox => {

        checkbox.addEventListener("change", () => {
            updateSelectionState();
        });

    });


    /* =========================================================
       선택 좌석 일괄 취소 버튼
       실제 API 연결 전까지는 ID만 확인합니다.
    ========================================================== */
    if (selectedCancelButton) {

        selectedCancelButton.addEventListener("click", () => {

            const selectedSeatIds = getSelectedSeatIds();

            if (selectedSeatIds.length === 0) {
                return;
            }

            /*
             * 추후 담당 팀원이 이 부분에
             * 부분 취소/환불 API를 연결하면 됩니다.
             */
            console.log(
                "선택 취소/환불 대상 reservationSeatIds:",
                selectedSeatIds
            );

        });
    }


    /* =========================================================
       개별 좌석 취소 버튼
       실제 API 연결 전까지는 ID만 확인합니다.
    ========================================================== */
    seatCancelButtons.forEach(button => {

        button.addEventListener("click", () => {

            const reservationId =
                Number(button.dataset.reservationId);

            const reservationSeatId =
                Number(button.dataset.reservationSeatId);

            /*
             * 추후 담당 팀원이 이 부분에
             * 좌석 1장 취소/환불 API를 연결하면 됩니다.
             */
            console.log(
                "개별 취소/환불 대상:",
                {
                    reservationId,
                    reservationSeatId
                }
            );

        });

    });


    /* =========================================================
       최초 화면 상태 설정
    ========================================================== */
    updateSelectionState();

});