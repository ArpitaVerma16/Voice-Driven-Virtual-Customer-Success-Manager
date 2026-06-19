// Dark Mode Toggle
class DarkModeManager {
    constructor() {
        this.isDarkMode = false;
        this.init();
    }

    init() {
        // Check saved preference
        const saved = localStorage.getItem('darkMode');
        if (saved === 'true') {
            this.enable();
        }

        // Create toggle button if not exists
        this.createToggleButton();
    }

    createToggleButton() {
        // Check if toggle already exists
        if (document.getElementById('darkModeToggle')) return;

        const toggle = document.createElement('button');
        toggle.id = 'darkModeToggle';
        toggle.className = 'dark-mode-toggle';
        toggle.innerHTML = '<i class="fas fa-moon"></i>';
        toggle.title = 'Toggle Dark Mode';
        toggle.onclick = () => this.toggle();

        // Find navbar or append to body
        const navbar = document.querySelector('.navbar .container');
        if (navbar) {
            const wrapper = document.createElement('div');
            wrapper.className = 'ms-auto d-flex align-items-center';
            wrapper.appendChild(toggle);
            navbar.appendChild(wrapper);
        } else {
            document.body.appendChild(toggle);
        }
    }

    toggle() {
        if (this.isDarkMode) {
            this.disable();
        } else {
            this.enable();
        }
    }

    enable() {
        document.documentElement.classList.add('dark-mode');
        this.isDarkMode = true;
        localStorage.setItem('darkMode', 'true');
        
        const toggle = document.getElementById('darkModeToggle');
        if (toggle) {
            toggle.innerHTML = '<i class="fas fa-sun"></i>';
            toggle.title = 'Switch to Light Mode';
        }

        // Show toast notification
        if (typeof toast !== 'undefined') {
            toast.info('Dark mode enabled 🌙', 'Theme');
        }
    }

    disable() {
        document.documentElement.classList.remove('dark-mode');
        this.isDarkMode = false;
        localStorage.setItem('darkMode', 'false');
        
        const toggle = document.getElementById('darkModeToggle');
        if (toggle) {
            toggle.innerHTML = '<i class="fas fa-moon"></i>';
            toggle.title = 'Switch to Dark Mode';
        }

        // Show toast notification
        if (typeof toast !== 'undefined') {
            toast.info('Light mode enabled ☀️', 'Theme');
        }
    }

    getCurrentTheme() {
        return this.isDarkMode ? 'dark' : 'light';
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    window.darkModeManager = new DarkModeManager();
});