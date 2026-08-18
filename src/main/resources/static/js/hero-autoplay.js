/**
 * 히어로 배너 슬라이더 자동 전환입니다.
 * 슬라이드 전환 로직 자체는 기존 home.js의 prev/next 버튼 핸들러를 그대로 재사용합니다.
 * (data-carousel-next 버튼을 주기적으로 클릭하는 방식이라 home.js 내부 구현과 무관하게 동작합니다.)
 * 전환 간격은 관리자 화면(/admin/banners/settings)에서 설정한 값을
 * carousel-shell의 data-autoplay-interval(ms) 속성으로 서버에서 내려줍니다.
 */
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        var shell = document.querySelector('[data-carousel]');
        if (!shell) {
            return;
        }

        var nextButton = shell.querySelector('[data-carousel-next]');
        if (!nextButton) {
            return;
        }

        var intervalMs = parseInt(shell.getAttribute('data-autoplay-interval'), 10);
        if (!intervalMs || isNaN(intervalMs) || intervalMs <= 0) {
            intervalMs = 3000;
        }

        var timer = null;

        function start() {
            stop();
            timer = setInterval(function () {
                nextButton.click();
            }, intervalMs);
        }

        function stop() {
            if (timer) {
                clearInterval(timer);
                timer = null;
            }
        }

        // 마우스를 올리면 잠시 멈추고, 벗어나면 다시 재생합니다.
        shell.addEventListener('mouseenter', stop);
        shell.addEventListener('mouseleave', start);

        // 사용자가 직접 화살표/인디케이터를 눌렀을 때도 타이머를 리셋해서
        // 클릭 직후 바로 또 넘어가는 어색함을 없앱니다.
        shell.addEventListener('click', function (e) {
            if (e.target.closest('[data-carousel-next], [data-carousel-prev], [data-slide-index]')) {
                start();
            }
        });

        start();
    });
})();