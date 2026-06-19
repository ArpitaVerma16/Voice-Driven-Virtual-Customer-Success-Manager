// Toast Notification System
class ToastManager {
    constructor() {
        this.container = null;
        this.init();
    }

    init() {
        // Create container if not exists
        if (!document.querySelector('.toast-container')) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        } else {
            this.container = document.querySelector('.toast-container');
        }
    }

    show(options) {
        const {
            title,
            message,
            type = 'info',
            duration = 3000,
            closable = true
        } = options;

        // Create toast element
        const toast = document.createElement('div');
        toast.className = `toast-notification toast-${type}`;
        
        // Set icon based on type
        let icon = 'fa-info-circle';
        if (type === 'success') icon = 'fa-check-circle';
        if (type === 'error') icon = 'fa-exclamation-circle';
        if (type === 'warning') icon = 'fa-exclamation-triangle';
        
        toast.innerHTML = `
            <div class="toast-icon">
                <i class="fas ${icon}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            ${closable ? '<button class="toast-close">&times;</button>' : ''}
            <div class="toast-progress">
                <div class="toast-progress-bar"></div>
            </div>
        `;
        
        // Add to container
        this.container.appendChild(toast);
        
        // Add close button event
        const closeBtn = toast.querySelector('.toast-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                this.removeToast(toast);
            });
        }
        
        // Auto remove after duration
        const timeout = setTimeout(() => {
            this.removeToast(toast);
        }, duration);
        
        // Click anywhere to dismiss
        toast.addEventListener('click', (e) => {
            if (e.target !== closeBtn) {
                this.removeToast(toast);
                clearTimeout(timeout);
            }
        });
        
        return toast;
    }
    
    success(message, title = 'Success') {
        return this.show({ title, message, type: 'success', duration: 3000 });
    }
    
    error(message, title = 'Error') {
        return this.show({ title, message, type: 'error', duration: 4000 });
    }
    
    info(message, title = 'Info') {
        return this.show({ title, message, type: 'info', duration: 3000 });
    }
    
    warning(message, title = 'Warning') {
        return this.show({ title, message, type: 'warning', duration: 3000 });
    }
    
    removeToast(toast) {
        toast.classList.add('toast-exit');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }
}

// Initialize
const toast = new ToastManager();

// Replace alert with toast (optional - uncomment if needed)
/*
window.originalAlert = window.alert;
window.alert = function(message) {
    toast.info(message, 'Notice');
};
*/