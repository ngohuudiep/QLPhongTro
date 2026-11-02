/**
 * QLPhongTro - Global JavaScript Helper
 * Dark mode, Toast system, Password Strength Meter, View Switcher, Delete Modal
 */

// ==========================================================================
// 1. Theme Management (Dark Mode)
// ==========================================================================
(function initTheme() {
    const savedTheme = localStorage.getItem('qlphongtro_theme');
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const theme = savedTheme || (prefersDark ? 'dark' : 'light');
    document.documentElement.setAttribute('data-bs-theme', theme);
})();

function toggleDarkMode() {
    const currentTheme = document.documentElement.getAttribute('data-bs-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-bs-theme', newTheme);
    localStorage.setItem('qlphongtro_theme', newTheme);
    updateThemeIcon(newTheme);
}

function updateThemeIcon(theme) {
    const icon = document.getElementById('themeIcon');
    if (icon) {
        if (theme === 'dark') {
            icon.className = 'bi bi-sun-fill text-warning';
        } else {
            icon.className = 'bi bi-moon-stars-fill text-muted';
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
    updateThemeIcon(currentTheme);
});

// ==========================================================================
// 2. Toast Notification System
// ==========================================================================
function showToast(message, type = 'info', duration = 4000) {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;

    let iconClass = 'bi-info-circle-fill text-primary';
    if (type === 'success') iconClass = 'bi-check-circle-fill text-success';
    if (type === 'error')   iconClass = 'bi-x-circle-fill text-danger';
    if (type === 'warning') iconClass = 'bi-exclamation-triangle-fill text-warning';

    toast.innerHTML = `
        <i class="bi ${iconClass} fs-5"></i>
        <div class="flex-grow-1 fw-medium" style="font-size: 0.92rem;">${message}</div>
        <button type="button" class="btn-close btn-sm" aria-label="Close" onclick="this.parentElement.remove()"></button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1) reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ==========================================================================
// 3. Password Visibility Toggle
// ==========================================================================
function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        if (icon) icon.className = 'bi bi-eye-slash';
    } else {
        input.type = 'password';
        if (icon) icon.className = 'bi bi-eye';
    }
}

// ==========================================================================
// 4. Password Strength Meter
// ==========================================================================
function checkPasswordStrength(password, fillBarId, labelId) {
    const fillBar = document.getElementById(fillBarId);
    const label = document.getElementById(labelId);
    if (!fillBar) return;

    let strength = 0;
    if (password.length >= 8) strength += 25;
    if (/[A-Z]/.test(password)) strength += 25;
    if (/[0-9]/.test(password)) strength += 25;
    if (/[^A-Za-z0-9]/.test(password)) strength += 25;

    fillBar.style.width = strength + '%';

    let color = '#ef4444'; // Yếu
    let text = 'Rất yếu';
    if (strength >= 50) { color = '#f59e0b'; text = 'Trung bình'; }
    if (strength >= 75) { color = '#06b6d4'; text = 'Khá'; }
    if (strength === 100) { color = '#10b981'; text = 'Rất mạnh'; }

    fillBar.style.backgroundColor = color;
    if (label) {
        label.textContent = text;
        label.style.color = color;
    }
}

// ==========================================================================
// 5. Grid / Table View Switcher
// ==========================================================================
function switchRoomView(mode) {
    const gridView = document.getElementById('roomGridView');
    const tableView = document.getElementById('roomTableView');
    const btnGrid = document.getElementById('btnViewGrid');
    const btnTable = document.getElementById('btnViewTable');

    if (!gridView || !tableView) return;

    if (mode === 'table') {
        gridView.classList.add('d-none');
        tableView.classList.remove('d-none');
        if (btnTable) btnTable.classList.add('active', 'btn-primary');
        if (btnTable) btnTable.classList.remove('btn-light');
        if (btnGrid) btnGrid.classList.remove('active', 'btn-primary');
        if (btnGrid) btnGrid.classList.add('btn-light');
        localStorage.setItem('qlphongtro_room_view', 'table');
    } else {
        gridView.classList.remove('d-none');
        tableView.classList.add('d-none');
        if (btnGrid) btnGrid.classList.add('active', 'btn-primary');
        if (btnGrid) btnGrid.classList.remove('btn-light');
        if (btnTable) btnTable.classList.remove('active', 'btn-primary');
        if (btnTable) btnTable.classList.add('btn-light');
        localStorage.setItem('qlphongtro_room_view', 'grid');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const savedView = localStorage.getItem('qlphongtro_room_view');
    if (savedView === 'table') {
        switchRoomView('table');
    }
});
