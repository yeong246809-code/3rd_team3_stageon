/**
 * alert()를 대체하는 공용 스타일 팝업입니다.
 * 사용법: showAppAlert('메시지', 'error' | 'success' | 'info')
 */
function showAppAlert(message, type, onClose) {
    type = type || 'info';
    var titleMap = { error: '알림', success: '완료', info: '안내' };
    var iconMap = { error: '!', success: '\u2713', info: 'i' };

    var overlay = document.createElement('div');
    overlay.className = 'app-alert-overlay';

    var box = document.createElement('div');
    box.className = 'app-alert-box app-alert-box--' + type;

    var head = document.createElement('div');
    head.className = 'app-alert-box__head';
    head.innerHTML =
        '<span class="app-alert-box__icon">' + iconMap[type] + '</span>' +
        '<span class="app-alert-box__title">' + titleMap[type] + '</span>';

    var body = document.createElement('div');
    body.className = 'app-alert-box__body';
    body.textContent = message;

    var footer = document.createElement('div');
    footer.className = 'app-alert-box__footer';
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'app-alert-box__btn';
    btn.textContent = '확인';
    footer.appendChild(btn);

    box.appendChild(head);
    box.appendChild(body);
    box.appendChild(footer);
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    function close() {
        document.body.removeChild(overlay);
        if (typeof onClose === 'function') onClose();
    }
    btn.addEventListener('click', close);
    overlay.addEventListener('click', function (e) {
        if (e.target === overlay) close();
    });
    btn.focus();
}

/**
 * confirm()을 대체하는 공용 스타일 팝업입니다. 확인 클릭 시에만 onConfirm이 실행됩니다.
 * 사용법: showAppConfirm('메시지', function() { ...확인 시 동작... })
 */
function showAppConfirm(message, onConfirm) {
    var overlay = document.createElement('div');
    overlay.className = 'app-alert-overlay';

    var box = document.createElement('div');
    box.className = 'app-alert-box app-alert-box--error';

    var head = document.createElement('div');
    head.className = 'app-alert-box__head';
    head.innerHTML =
        '<span class="app-alert-box__icon">!</span>' +
        '<span class="app-alert-box__title">확인</span>';

    var body = document.createElement('div');
    body.className = 'app-alert-box__body';
    body.textContent = message;

    var footer = document.createElement('div');
    footer.className = 'app-alert-box__footer';

    var cancelBtn = document.createElement('button');
    cancelBtn.type = 'button';
    cancelBtn.className = 'app-alert-box__btn app-alert-box__btn--cancel';
    cancelBtn.textContent = '취소';

    var confirmBtn = document.createElement('button');
    confirmBtn.type = 'button';
    confirmBtn.className = 'app-alert-box__btn';
    confirmBtn.textContent = '확인';

    footer.appendChild(cancelBtn);
    footer.appendChild(confirmBtn);

    box.appendChild(head);
    box.appendChild(body);
    box.appendChild(footer);
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    function close() {
        document.body.removeChild(overlay);
    }
    cancelBtn.addEventListener('click', close);
    overlay.addEventListener('click', function (e) {
        if (e.target === overlay) close();
    });
    confirmBtn.addEventListener('click', function () {
        close();
        if (typeof onConfirm === 'function') onConfirm();
    });
    confirmBtn.focus();
}