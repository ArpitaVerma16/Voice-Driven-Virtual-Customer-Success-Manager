class TypingIndicatorManager {
    constructor() {
        this.indicator = null;
        this.timeout = null;
        this.init();
    }

    init() {
        if (!document.getElementById('typingIndicator')) {
            const indicator = document.createElement('div');
            indicator.className = 'typing-indicator';
            indicator.id = 'typingIndicator';
            indicator.innerHTML = `
                <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <span class="typing-text" id="typingText">Processing</span>
            `;
            document.body.appendChild(indicator);
            this.indicator = indicator;
        } else {
            this.indicator = document.getElementById('typingIndicator');
        }
    }

    show(message = 'Processing', type = 'processing') {
        this.clearTimeout();
        const textSpan = document.getElementById('typingText');
        if (textSpan) textSpan.textContent = message;
        if (this.indicator) {
            this.indicator.classList.remove('listening', 'processing', 'success');
            this.indicator.classList.add(type);
            this.indicator.style.display = 'flex';
        }
    }

    hide() {
        if (this.indicator) {
            this.indicator.style.display = 'none';
        }
        this.clearTimeout();
    }

    showListening() {
        this.show('Listening', 'listening');
        this.setAutoHide(10000);
    }

    showProcessing() {
        this.show('Processing', 'processing');
    }

    showSuccess(message = 'Done!') {
        this.show(message, 'success');
        this.setAutoHide(2000);
    }

    setAutoHide(delay) {
        this.clearTimeout();
        this.timeout = setTimeout(() => this.hide(), delay);
    }

    clearTimeout() {
        if (this.timeout) {
            clearTimeout(this.timeout);
            this.timeout = null;
        }
    }
}

const typingIndicator = new TypingIndicatorManager();