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
            img.className = 'poster-preview';
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