function toggleTheme() {
    var html = document.documentElement;
    var isDark = html.getAttribute('data-theme') === 'dark';
    if (isDark) {
        html.removeAttribute('data-theme');
        localStorage.setItem('stageon-admin-theme', 'light');
    } else {
        html.setAttribute('data-theme', 'dark');
        localStorage.setItem('stageon-admin-theme', 'dark');
    }
    updateThemeIcon();
}

function updateThemeIcon() {
    var btn = document.getElementById('themeToggleBtn');
    if (!btn) return;
    btn.textContent = document.documentElement.getAttribute('data-theme') === 'dark' ? '☀️' : '🌙';
}

document.addEventListener('DOMContentLoaded', updateThemeIcon);