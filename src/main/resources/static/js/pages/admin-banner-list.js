document.addEventListener('DOMContentLoaded', function () {
    var modal = document.getElementById('slideSettingModal');
    var openBtn = document.getElementById('openSlideSettingBtn');
    var closeBtn = document.getElementById('closeSlideSettingBtn');
    var closeBtnX = document.getElementById('closeSlideSettingBtnX');

    if (!modal || !openBtn) {
        return;
    }

    function close() {
        modal.hidden = true;
    }

    openBtn.addEventListener('click', function () {
        modal.hidden = false;
    });

    if (closeBtn) closeBtn.addEventListener('click', close);
    if (closeBtnX) closeBtnX.addEventListener('click', close);

    modal.addEventListener('click', function (e) {
        if (e.target === modal) {
            close();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !modal.hidden) {
            close();
        }
    });
});