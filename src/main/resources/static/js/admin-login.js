const pwInput = document.getElementById('admin-password');
const pwToggle = document.getElementById('pw-toggle-btn');
const eyeOpen = document.getElementById('eye-open');
const eyeClosed = document.getElementById('eye-closed');

pwToggle.addEventListener('click', () => {
    const showing = pwInput.type === 'text';
    pwInput.type = showing ? 'password' : 'text';
    eyeOpen.style.display = showing ? 'block' : 'none';
    eyeClosed.style.display = showing ? 'none' : 'block';
});

const submitBtn = document.getElementById('login-submit-btn');
document.getElementById('admin-login-form').addEventListener('submit', () => {
    submitBtn.classList.add('is-loading');
    submitBtn.disabled = true;
});