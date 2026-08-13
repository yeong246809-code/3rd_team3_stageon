document.addEventListener("DOMContentLoaded", () => {

    // =========================================================
    // 본인 인증 화면 요소
    // =========================================================

    const sendButton =
        document.getElementById("profile-auth-send-button");

    const codeArea =
        document.getElementById("profile-auth-code-area");

    const codeInput =
        document.getElementById("profile-auth-code");

    const verifyButton =
        document.getElementById("profile-auth-verify-button");

    const timerElement =
        document.getElementById("profile-auth-timer");

    const messageElement =
        document.getElementById("profile-auth-message");


    /*
     * 인증 화면에 필요한 요소가 하나라도 없다면
     * 이후 JavaScript를 실행하지 않습니다.
     */
    if (
        !sendButton
        || !codeArea
        || !codeInput
        || !verifyButton
        || !timerElement
        || !messageElement
    ) {
        return;
    }


    // =========================================================
    // Spring Security CSRF 정보
    // =========================================================

    /*
     * mypage-profile-auth.html <head>에 넣어둔
     *
     * <meta name="_csrf">
     * <meta name="_csrf_header">
     *
     * 값을 가져옵니다.
     */
    const csrfToken =
        document
            .querySelector('meta[name="_csrf"]')
            ?.getAttribute("content");

    const csrfHeader =
        document
            .querySelector('meta[name="_csrf_header"]')
            ?.getAttribute("content");


    /**
     * AJAX POST 요청에 사용할 공통 헤더를 만듭니다.
     */
    function createHeaders(
        includeJsonContentType = false
    ) {

        const headers = {};


        // JSON 데이터를 전송하는 요청인 경우
        if (includeJsonContentType) {

            headers["Content-Type"] =
                "application/json";
        }


        // Spring Security CSRF 사용 중이라면 토큰 추가
        if (csrfToken && csrfHeader) {

            headers[csrfHeader] =
                csrfToken;
        }


        return headers;
    }


    // =========================================================
    // 인증 상태
    // =========================================================

    // 인증번호 유효시간 3분
    let remainingSeconds = 180;

    // setInterval ID
    let timerId = null;


    // =========================================================
    // 인증 메시지 출력
    // =========================================================

    /**
     * 인증 결과 메시지를 화면에 출력합니다.
     *
     * type
     * normal  : 일반 안내
     * success : 인증 성공
     * error   : 오류
     */
    function showMessage(
        message,
        type = "normal"
    ) {

        messageElement.textContent =
            message;


        // 기존 상태 제거
        messageElement.classList.remove(
            "is-success",
            "is-error"
        );


        // 성공 상태
        if (type === "success") {

            messageElement.classList.add(
                "is-success"
            );
        }


        // 오류 상태
        if (type === "error") {

            messageElement.classList.add(
                "is-error"
            );
        }
    }


    // =========================================================
    // 인증 타이머
    // =========================================================

    /**
     * 남은 시간을 03:00 형식으로 표시합니다.
     */
    function renderTimer() {

        const minutes =
            String(
                Math.floor(
                    remainingSeconds / 60
                )
            ).padStart(2, "0");


        const seconds =
            String(
                remainingSeconds % 60
            ).padStart(2, "0");


        timerElement.textContent =
            `${minutes}:${seconds}`;
    }


    /**
     * 실행 중인 타이머를 중지합니다.
     */
    function stopTimer() {

        if (timerId !== null) {

            clearInterval(timerId);

            timerId = null;
        }
    }


    /**
     * 인증번호 유효시간 3분을 시작합니다.
     */
    function startTimer() {

        // 기존 타이머 제거
        stopTimer();


        remainingSeconds = 180;


        // 만료 스타일 제거
        timerElement.classList.remove(
            "is-expired"
        );


        renderTimer();


        timerId =
            setInterval(() => {

                remainingSeconds -= 1;


                renderTimer();


                // =============================================
                // 인증시간 만료
                // =============================================

                if (remainingSeconds <= 0) {

                    stopTimer();


                    timerElement.textContent =
                        "00:00";


                    timerElement.classList.add(
                        "is-expired"
                    );


                    // 인증번호 입력 비활성화
                    codeInput.disabled = true;


                    // 인증 확인 버튼 비활성화
                    verifyButton.disabled = true;


                    /*
                     * 다시 인증번호를 받을 수 있도록
                     * 발송 버튼은 활성 상태를 유지합니다.
                     */
                    sendButton.disabled = false;


                    showMessage(
                        "인증 시간이 만료되었습니다. 인증번호를 다시 받아 주세요.",
                        "error"
                    );
                }

            }, 1000);
    }


    // =========================================================
    // 인증번호 입력 영역 활성화
    // =========================================================

    /**
     * 인증번호를 발송한 뒤
     * 입력 영역을 활성화합니다.
     */
    function enableCodeArea() {

        codeArea.classList.remove(
            "is-disabled"
        );


        codeInput.disabled = false;


        codeInput.value = "";


        verifyButton.disabled = true;


        codeInput.focus();
    }


    // =========================================================
    // 인증번호 발송
    // =========================================================

    sendButton.addEventListener(
        "click",
        async () => {

            // 중복 클릭 방지
            sendButton.disabled = true;


            sendButton.textContent =
                "발송 중...";


            try {

                /*
                 * MyPageProfileController
                 *
                 * POST
                 * /mypage/profile/verification/send
                 */
                const response =
                    await fetch(
                        "/mypage/profile/verification/send",
                        {
                            method: "POST",

                            headers:
                                createHeaders(false)
                        }
                    );


                const result =
                    await response.json();


                /*
                 * 서버에서 오류 응답이 왔거나
                 * success가 false인 경우
                 */
                if (
                    !response.ok
                    || !result.success
                ) {

                    throw new Error(
                        result.message
                        || "인증번호 발송에 실패했습니다."
                    );
                }


                // 인증번호 입력 영역 활성화
                enableCodeArea();


                // 3분 타이머 시작
                startTimer();


                showMessage(
                    result.message
                    || "현재 계정 이메일로 인증번호를 발송했습니다.",
                    "success"
                );


                /*
                 * 한 번 발송한 후에는
                 * 버튼 문구를 재발송으로 변경합니다.
                 */
                sendButton.textContent =
                    "인증번호 재발송";


            } catch (error) {

                console.error(
                    "본인 인증번호 발송 오류",
                    error
                );


                showMessage(
                    error.message
                    || "인증번호 발송 중 오류가 발생했습니다.",
                    "error"
                );


                sendButton.textContent =
                    "인증번호 받기";


            } finally {

                // 다시 클릭 가능
                sendButton.disabled = false;
            }
        }
    );


    // =========================================================
    // 인증번호 숫자 입력 처리
    // =========================================================

    codeInput.addEventListener(
        "input",
        () => {

            /*
             * 숫자가 아닌 문자는 제거하고
             * 최대 6자리까지만 입력합니다.
             */
            codeInput.value =
                codeInput.value
                    .replace(/\D/g, "")
                    .slice(0, 6);


            /*
             * 6자리를 모두 입력했을 때만
             * 인증 확인 버튼을 활성화합니다.
             */
            verifyButton.disabled =
                codeInput.value.length !== 6;
        }
    );


    // =========================================================
    // 인증번호 붙여넣기
    // =========================================================

    codeInput.addEventListener(
        "paste",
        (event) => {

            /*
             * 사용자가 이메일의 인증번호를
             * 통째로 복사해 붙여넣을 수 있도록 합니다.
             */
            event.preventDefault();


            const pastedValue =
                event.clipboardData
                    .getData("text")
                    .replace(/\D/g, "")
                    .slice(0, 6);


            codeInput.value =
                pastedValue;


            verifyButton.disabled =
                pastedValue.length !== 6;
        }
    );


    // =========================================================
    // Enter 키로 인증 확인
    // =========================================================

    codeInput.addEventListener(
        "keydown",
        (event) => {

            if (
                event.key === "Enter"
                && codeInput.value.length === 6
                && !verifyButton.disabled
            ) {

                event.preventDefault();


                verifyButton.click();
            }
        }
    );


    // =========================================================
    // 인증번호 확인
    // =========================================================

    verifyButton.addEventListener(
        "click",
        async () => {

            const code =
                codeInput.value.trim();


            // 6자리 여부 재확인
            if (code.length !== 6) {

                showMessage(
                    "인증번호 6자리를 입력해 주세요.",
                    "error"
                );


                codeInput.focus();


                return;
            }


            // 중복 요청 방지
            verifyButton.disabled = true;


            verifyButton.textContent =
                "확인 중...";


            try {

                /*
                 * MyPageProfileController
                 *
                 * POST
                 * /mypage/profile/verification/verify
                 */
                const response =
                    await fetch(
                        "/mypage/profile/verification/verify",
                        {
                            method: "POST",

                            headers:
                                createHeaders(true),

                            body:
                                JSON.stringify({
                                    code: code
                                })
                        }
                    );


                const result =
                    await response.json();


                // 인증 실패
                if (
                    !response.ok
                    || !result.success
                ) {

                    throw new Error(
                        result.message
                        || "인증번호 확인에 실패했습니다."
                    );
                }


                // =================================================
                // 인증 성공
                // =================================================

                stopTimer();


                codeInput.disabled =
                    true;


                verifyButton.disabled =
                    true;


                sendButton.disabled =
                    true;


                showMessage(
                    result.message
                    || "본인 인증이 완료되었습니다.",
                    "success"
                );


                /*
                 * Controller가 이미 세션에
                 * PROFILE_VERIFIED_EMAIL
                 * PROFILE_VERIFIED_AT
                 *
                 * 값을 저장한 상태입니다.
                 *
                 * 따라서 /mypage/profile을 다시 요청하면
                 * 이번에는 인증 화면이 아니라
                 * 실제 회원정보 수정 화면이 열립니다.
                 */
                setTimeout(() => {

                    window.location.href =
                        "/mypage/profile";

                }, 700);


            } catch (error) {

                console.error(
                    "본인 인증번호 확인 오류",
                    error
                );


                showMessage(
                    error.message
                    || "인증번호 확인 중 오류가 발생했습니다.",
                    "error"
                );


                /*
                 * 틀린 인증번호를 다시 입력할 수 있도록
                 * 버튼을 활성화합니다.
                 */
                verifyButton.disabled =
                    codeInput.value.length !== 6;


            } finally {

                verifyButton.textContent =
                    "인증 확인";
            }
        }
    );


    // =========================================================
    // 초기 상태
    // =========================================================

    /*
     * 페이지 최초 진입 시에는
     * 아직 인증번호를 발송하지 않았으므로
     * 인증번호 입력 영역을 비활성 상태로 둡니다.
     */
    codeInput.disabled = true;

    verifyButton.disabled = true;

    renderTimer();

});