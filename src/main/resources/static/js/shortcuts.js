// Keyboard Shortcuts for VCSM

class KeyboardShortcuts {
    constructor() {
        this.init();
    }

    init() {
        // Listen for keydown events
        document.addEventListener('keydown', (e) => this.handleKeydown(e));
        console.log('✅ Keyboard shortcuts enabled');
    }

    handleKeydown(e) {
        // Ctrl + V - Start Voice Command
        if (e.ctrlKey && e.key === 'v') {
            e.preventDefault();
            this.startVoiceCommand();
            return;
        }

        // Ctrl + N - New Complaint
        if (e.ctrlKey && e.key === 'n') {
            e.preventDefault();
            this.newComplaint();
            return;
        }

        // Ctrl + D - Dashboard
        if (e.ctrlKey && e.key === 'd') {
            e.preventDefault();
            this.goToDashboard();
            return;
        }

        // Ctrl + E - Events
        if (e.ctrlKey && e.key === 'e') {
            e.preventDefault();
            this.goToEvents();
            return;
        }

        // Ctrl + A - Analytics
        if (e.ctrlKey && e.key === 'a') {
            e.preventDefault();
            this.goToAnalytics();
            return;
        }

        // Ctrl + Enter - Submit Form
        if (e.ctrlKey && e.key === 'Enter') {
            e.preventDefault();
            this.submitActiveForm();
            return;
        }

        // Escape - Close Modal
        if (e.key === 'Escape') {
            this.closeAllModals();
            return;
        }

        // ? - Show Help
        if (e.key === '?') {
            e.preventDefault();
            this.showShortcutsHelp();
            return;
        }
    }

    startVoiceCommand() {
        const micBtn = document.getElementById('micBtn');
        if (micBtn) {
            micBtn.click();
            this.showToast('🎤 Voice command started', 'info');
        } else {
            // Try to find voice input
            const voiceInput = document.getElementById('voiceInput');
            if (voiceInput) {
                voiceInput.focus();
                this.showToast('🎤 Speak now...', 'info');
            }
        }
    }

    newComplaint() {
        const newBtn = document.querySelector('[data-bs-target="#fileComplaintModal"]');
        if (newBtn) {
            newBtn.click();
            this.showToast('📝 New complaint modal opened', 'success');
        } else {
            window.location.href = '/complaints';
        }
    }

    goToDashboard() {
        window.location.href = '/';
        this.showToast('📊 Going to Dashboard', 'info');
    }

    goToEvents() {
        window.location.href = '/events';
        this.showToast('📅 Going to Events', 'info');
    }

    goToAnalytics() {
        window.location.href = '/analytics';
        this.showToast('📈 Going to Analytics', 'info');
    }

    submitActiveForm() {
        const activeForm = document.querySelector('form:has(input:focus), form:has(textarea:focus), form:has(select:focus)');
        if (activeForm) {
            const submitBtn = activeForm.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.click();
                this.showToast('📤 Form submitted', 'success');
            }
        } else {
            // Try to find any visible modal form
            const modalForm = document.querySelector('.modal.show form');
            if (modalForm) {
                const submitBtn = modalForm.querySelector('button[type="submit"]');
                if (submitBtn) submitBtn.click();
            }
        }
    }

    closeAllModals() {
        const modals = document.querySelectorAll('.modal.show');
        modals.forEach(modal => {
            const closeBtn = modal.querySelector('.btn-close, [data-bs-dismiss="modal"]');
            if (closeBtn) closeBtn.click();
        });
        if (modals.length > 0) {
            this.showToast('❌ Modals closed', 'info');
        }
    }

    showShortcutsHelp() {
        const helpHtml = `
            <div class="shortcuts-help-modal" id="shortcutsHelp">
                <div class="shortcuts-help-content">
                    <div class="shortcuts-help-header">
                        <h5><i class="fas fa-keyboard"></i> Keyboard Shortcuts</h5>
                        <button class="btn-close" onclick="document.getElementById('shortcutsHelp').remove()"></button>
                    </div>
                    <div class="shortcuts-help-body">
                        <table class="table table-sm">
                            <tr><td><kbd>Ctrl</kbd> + <kbd>V</kbd></td><td>Start Voice Command</td></tr>
                            <tr><td><kbd>Ctrl</kbd> + <kbd>N</kbd></td><td>New Complaint</td></tr>
                            <tr><td><kbd>Ctrl</kbd> + <kbd>D</kbd></td><td>Dashboard</td></tr>
                            <tr><td><kbd>Ctrl</kbd> + <kbd>E</kbd></td><td>Events</td></tr>
                            <tr><td><kbd>Ctrl</kbd> + <kbd>A</kbd></td><td>Analytics</td></tr>
                            <tr><td><kbd>Ctrl</kbd> + <kbd>Enter</kbd></td><td>Submit Form</td></tr>
                            <tr><td><kbd>Esc</kbd></td><td>Close Modal</td></tr>
                            <tr><td><kbd>?</kbd></td><td>Show this help</td></tr>
                        </table>
                    </div>
                </div>
            </div>
        `;
        
        // Remove existing help modal if any
        const existing = document.getElementById('shortcutsHelp');
        if (existing) existing.remove();
        
        document.body.insertAdjacentHTML('beforeend', helpHtml);
        
        // Close on outside click
        setTimeout(() => {
            const modal = document.getElementById('shortcutsHelp');
            if (modal) {
                modal.addEventListener('click', (e) => {
                    if (e.target === modal) modal.remove();
                });
            }
        }, 100);
    }

    showToast(message, type = 'info') {
        // Check if toast already exists
        let toast = document.getElementById('shortcutsToast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'shortcutsToast';
            toast.className = 'shortcuts-toast';
            document.body.appendChild(toast);
        }
        
        toast.textContent = message;
        toast.className = `shortcuts-toast ${type} show`;
        
        setTimeout(() => {
            toast.classList.remove('show');
        }, 2000);
    }
}

// Initialize shortcuts when page loads
document.addEventListener('DOMContentLoaded', () => {
    window.keyboardShortcuts = new KeyboardShortcuts();
});