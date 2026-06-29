// Push Notification Service
class PushNotificationManager {
    constructor() {
        this.isSupported = 'Notification' in window && 'serviceWorker' in navigator;
        this.subscription = null;
        this.publicKey = 'YOUR_VAPID_PUBLIC_KEY';
        this.init();
    }

    async init() {
        if (!this.isSupported) {
            console.warn('Push notifications not supported');
            return;
        }

        // Request permission
        if (Notification.permission === 'default') {
            await this.requestPermission();
        }

        // Subscribe if allowed
        if (Notification.permission === 'granted') {
            await this.subscribeToPush();
        }

        this.showNotificationPrompt();
    }

    async requestPermission() {
        try {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                console.log('✅ Notification permission granted');
                await this.subscribeToPush();
            }
        } catch (error) {
            console.error('Error requesting permission:', error);
        }
    }

    async subscribeToPush() {
        try {
            const registration = await navigator.serviceWorker.ready;
            this.subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: this.publicKey
            });
            
            // Send subscription to server
            await this.sendSubscriptionToServer(this.subscription);
            console.log('✅ Subscribed to push notifications');
            
        } catch (error) {
            console.error('Error subscribing to push:', error);
        }
    }

    async sendSubscriptionToServer(subscription) {
        try {
            const response = await fetch('/api/notifications/subscribe', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(subscription)
            });
            
            if (!response.ok) {
                throw new Error('Failed to send subscription to server');
            }
            
            console.log('✅ Subscription sent to server');
            
        } catch (error) {
            console.error('Error sending subscription:', error);
        }
    }

    async unsubscribe() {
        try {
            if (this.subscription) {
                await this.subscription.unsubscribe();
                this.subscription = null;
                console.log('✅ Unsubscribed from push notifications');
            }
        } catch (error) {
            console.error('Error unsubscribing:', error);
        }
    }

    showNotificationPrompt() {
        // Show prompt on first visit
        if (!localStorage.getItem('notificationPromptShown')) {
            setTimeout(() => {
                if (Notification.permission === 'default') {
                    const enableBtn = confirm('Enable notifications for VCSM? You will receive real-time updates.');
                    if (enableBtn) {
                        this.requestPermission();
                    }
                }
                localStorage.setItem('notificationPromptShown', 'true');
            }, 3000);
        }
    }

    // Send a test notification
    async sendTestNotification(title = 'Hello from VCSM!', body = 'This is a test notification.') {
        if (Notification.permission === 'granted') {
            const registration = await navigator.serviceWorker.ready;
            registration.showNotification(title, {
                body: body,
                icon: '/icons/icon-192x192.png',
                badge: '/icons/icon-72x72.png',
                vibrate: [200, 100, 200],
                data: {
                    url: '/'
                }
            });
        }
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    window.pushManager = new PushNotificationManager();
});

// Test notification button (add to UI)
function testNotification() {
    if (window.pushManager) {
        window.pushManager.sendTestNotification();
    }
}