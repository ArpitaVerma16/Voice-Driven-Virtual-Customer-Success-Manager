// Global Loading Spinner Controller
class SpinnerManager {
    constructor() {
        this.activeRequests = 0;
        this.init();
    }

    init() {
        // Create overlay if not exists
        if (!document.querySelector('.spinner-overlay')) {
            const overlay = document.createElement('div');
            overlay.className = 'spinner-overlay';
            overlay.innerHTML = '<div class="spinner"></div>';
            document.body.appendChild(overlay);
        }
        this.overlay = document.querySelector('.spinner-overlay');
        
        // Add spinner to all buttons
        this.addButtonSpinners();
        
        // Intercept form submissions
        this.interceptForms();
        
        // Intercept fetch requests
        this.interceptFetch();
    }

    addButtonSpinners() {
        const buttons = document.querySelectorAll('button[type="submit"], .btn-submit');
        buttons.forEach(btn => {
            if (!btn.querySelector('.btn-spinner')) {
                const spinner = document.createElement('span');
                spinner.className = 'btn-spinner';
                const text = document.createElement('span');
                text.className = 'btn-text';
                text.textContent = btn.textContent;
                btn.innerHTML = '';
                btn.appendChild(spinner);
                btn.appendChild(text);
            }
            
            btn.addEventListener('click', (e) => {
                if (!btn.classList.contains('no-spinner')) {
                    btn.classList.add('loading');
                }
            });
        });
    }

    interceptForms() {
        document.querySelectorAll('form').forEach(form => {
            form.addEventListener('submit', () => {
                this.showOverlay();
            });
        });
    }

    interceptFetch() {
        const originalFetch = window.fetch;
        window.fetch = async (...args) => {
            this.showOverlay();
            try {
                const response = await originalFetch(...args);
                this.hideOverlay();
                return response;
            } catch (error) {
                this.hideOverlay();
                throw error;
            }
        };
    }

    showOverlay() {
        this.activeRequests++;
        if (this.overlay) {
            this.overlay.classList.add('active');
        }
    }

    hideOverlay() {
        this.activeRequests--;
        if (this.activeRequests <= 0 && this.overlay) {
            this.activeRequests = 0;
            this.overlay.classList.remove('active');
            
            // Remove loading class from all buttons
            document.querySelectorAll('.btn.loading').forEach(btn => {
                btn.classList.remove('loading');
            });
        }
    }

    showSkeleton(containerId, type = 'card') {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const skeletonHtml = type === 'card' ? 
            '<div class="skeleton skeleton-card"></div>' :
            '<div class="skeleton skeleton-title"></div><div class="skeleton skeleton-text"></div><div class="skeleton skeleton-text"></div>';
        
        container.innerHTML = skeletonHtml;
    }
}

// Voice Processing Indicator
class VoiceIndicator {
    constructor() {
        this.init();
    }

    init() {
        const voiceBtn = document.querySelector('#voiceBtn');
        if (voiceBtn) {
            voiceBtn.addEventListener('click', () => this.showListening());
        }
    }

    showListening() {
        const indicator = document.querySelector('.voice-indicator');
        if (indicator) {
            indicator.style.display = 'inline-flex';
        }
    }

    hideListening() {
        const indicator = document.querySelector('.voice-indicator');
        if (indicator) {
            indicator.style.display = 'none';
        }
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    window.spinnerManager = new SpinnerManager();
    window.voiceIndicator = new VoiceIndicator();
});

// Show spinner for AJAX calls
function showLoading() {
    if (window.spinnerManager) {
        window.spinnerManager.showOverlay();
    }
}

function hideLoading() {
    if (window.spinnerManager) {
        window.spinnerManager.hideOverlay();
    }
}