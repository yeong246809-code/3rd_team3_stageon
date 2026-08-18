function previewBanner(input) {
    if (!input.files || !input.files[0]) {
        return;
    }
    var reader = new FileReader();
    reader.onload = function (e) {
        var empty = document.getElementById('bannerPreviewEmpty');
        var img = document.getElementById('bannerPreview');
        if (!img) {
            img = document.createElement('img');
            img.id = 'bannerPreview';
            img.className = 'poster-preview poster-preview--wide';
            empty.parentNode.insertBefore(img, empty);
        }
        img.src = e.target.result;
        img.style.display = 'block';
        if (empty) {
            empty.style.display = 'none';
        }
    };
    reader.readAsDataURL(input.files[0]);
}

/** 버튼 링크 종류 선택에 따라 직접입력 URL 필드를 보여주거나 숨깁니다. */
function toggleCustomUrl(prefix) {
    var select = document.querySelector('select[name="' + prefix + 'LinkType"]');
    var input = document.getElementById(prefix + 'UrlInput');
    if (!select || !input) {
        return;
    }
    input.style.display = select.value === 'CUSTOM' ? 'block' : 'none';
}

document.addEventListener('DOMContentLoaded', function () {
    toggleCustomUrl('button1');
    toggleCustomUrl('button2');
});