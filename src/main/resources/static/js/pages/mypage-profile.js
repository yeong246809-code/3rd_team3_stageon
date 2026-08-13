document.addEventListener("DOMContentLoaded", () => {

    // =========================================================
    // 회원정보 수정 화면 요소
    // =========================================================

    const profileForm =
        document.getElementById("profile-form");

    const phoneInput =
        document.getElementById("profile-phone");

    const currentPasswordInput =
        document.getElementById("profile-current-password");

    const newPasswordInput =
        document.getElementById("profile-password");

    const newPasswordConfirmInput =
        document.getElementById("profile-password-confirm");


    /*
     * 회원정보 수정 화면이 아닌 경우
     * JavaScript 실행을 종료합니다.
     */
    if (!profileForm) {
        return;
    }


    // =========================================================
    // 휴대전화 입력 처리
    // =========================================================

    /**
     * 휴대전화 번호의 숫자 이외 문자를 제거합니다.
     */
    function normalizePhone(value) {

        return (value || "")
            .replace(/\D/g, "");
    }


    /**
     * 국내 휴대전화 기본 형식인지 확인합니다.
     *
     * 현재 StageOn에서는
     * 010 + 숫자 8자리 형식으로 검증합니다.
     */
    function isValidPhone(value) {

        const normalizedPhone =
            normalizePhone(value);

        return /^010\d{8}$/
            .test(normalizedPhone);
    }


    /**
     * 휴대전화 입력 시
     * 숫자와 하이픈만 입력할 수 있도록 정리합니다.
     */
    if (phoneInput) {

        phoneInput.addEventListener(
            "input",
            () => {

                phoneInput.value =
                    phoneInput.value.replace(
                        /[^0-9-]/g,
                        ""
                    );
            }
        );
    }


    // =========================================================
    // 비밀번호 변경 여부 확인
    // =========================================================

    /**
     * 새 비밀번호가 입력되어 있는지 확인합니다.
     */
    function hasNewPassword() {

        if (!newPasswordInput) {
            return false;
        }

        return newPasswordInput
            .value
            .trim() !== "";
    }


    // =========================================================
    // 비밀번호 검증
    // =========================================================

    /**
     * 비밀번호를 변경하려는 경우
     * 필요한 항목을 확인합니다.
     */
    function validatePasswordChange() {

        /*
         * 새 비밀번호를 입력하지 않았다면
         * 비밀번호 변경을 요청하지 않은 것이므로 통과합니다.
         */
        if (!hasNewPassword()) {
            return true;
        }


        const currentPassword =
            currentPasswordInput
                ? currentPasswordInput.value
                : "";


        const newPassword =
            newPasswordInput
                ? newPasswordInput.value
                : "";


        const newPasswordConfirm =
            newPasswordConfirmInput
                ? newPasswordConfirmInput.value
                : "";


        // -----------------------------------------------------
        // 현재 비밀번호 필수
        // -----------------------------------------------------

        if (!currentPassword.trim()) {

            alert(
                "비밀번호를 변경하려면 현재 비밀번호를 입력해 주세요."
            );


            if (currentPasswordInput) {
                currentPasswordInput.focus();
            }


            return false;
        }


        // -----------------------------------------------------
        // 새 비밀번호 확인값 필수
        // -----------------------------------------------------

        if (!newPasswordConfirm.trim()) {

            alert(
                "새 비밀번호 확인을 입력해 주세요."
            );


            if (newPasswordConfirmInput) {
                newPasswordConfirmInput.focus();
            }


            return false;
        }


        // -----------------------------------------------------
        // 새 비밀번호 일치 확인
        // -----------------------------------------------------

        if (
            newPassword
            !== newPasswordConfirm
        ) {

            alert(
                "새 비밀번호가 일치하지 않습니다."
            );


            if (newPasswordConfirmInput) {
                newPasswordConfirmInput.focus();
            }


            return false;
        }


        return true;
    }


    // =========================================================
    // 회원정보 저장 전 검증
    // =========================================================

    profileForm.addEventListener(
        "submit",
        (event) => {

            // -------------------------------------------------
            // 휴대전화 번호 확인
            // -------------------------------------------------

            if (
                phoneInput
                && !isValidPhone(
                    phoneInput.value
                )
            ) {

                event.preventDefault();


                alert(
                    "휴대전화 번호를 올바르게 입력해 주세요.\n예: 010-1234-5678"
                );


                phoneInput.focus();


                return;
            }


            // -------------------------------------------------
            // 비밀번호 변경 항목 확인
            // -------------------------------------------------

            if (!validatePasswordChange()) {

                event.preventDefault();


                return;
            }


            /*
             * 여기까지 통과하면 정상적으로
             * POST /mypage/profile 요청이 전송됩니다.
             *
             * 실제 현재 비밀번호 일치 여부,
             * 휴대전화 중복 여부,
             * 본인 인증 세션 유효 여부는
             * 서버에서 다시 검증합니다.
             */
        }
    );

});