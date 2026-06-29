// PWA Install Prompt
let deferredPrompt;

window.addEventListener('beforeinstallprompt', function(e) {
    // Prevent the mini-infobar from appearing on mobile
    e.preventDefault();
    // Stash the event so it can be triggered later
    deferredPrompt = e;
    
    // Show install button
    const installBtn = document.getElementById('installBtn');
    if (installBtn) {
        installBtn.style.display = 'flex';
        installBtn.addEventListener('click', function() {
            showInstallPrompt();
        });
    }
});

function showInstallPrompt() {
    if (deferredPrompt) {
        // Show the install prompt
        deferredPrompt.prompt();
        
        // Wait for the user to respond to the prompt
        deferredPrompt.userChoice.then(function(choiceResult) {
            if (choiceResult.outcome === 'accepted') {
                console.log('✅ User accepted the install prompt');
            } else {
                console.log('❌ User dismissed the install prompt');
            }
            deferredPrompt = null;
        });
    }
}

// Detect if app is installed
window.addEventListener('appinstalled', function() {
    console.log('✅ VCSM installed as PWA');
    // Hide install button
    const installBtn = document.getElementById('installBtn');
    if (installBtn) {
        installBtn.style.display = 'none';
    }
});

// Offline connection loss and recovery monitoring
window.addEventListener('offline', function() {
    console.log('🔌 Network connection lost. Falling back to offline Wasm voice command processor.');
    showOfflineBanner();
});

window.addEventListener('online', function() {
    console.log('🔌 Network connection restored. Back to online mode.');
    hideOfflineBanner();
});

function showOfflineBanner() {
    let banner = document.getElementById('offline-wasm-banner');
    if (!banner) {
        banner = document.createElement('div');
        banner.id = 'offline-wasm-banner';
        banner.style.position = 'fixed';
        banner.style.top = '0';
        banner.style.left = '0';
        banner.style.width = '100%';
        banner.style.background = 'linear-gradient(90deg, #dc2626, #b91c1c)';
        banner.style.color = 'white';
        banner.style.textAlign = 'center';
        banner.style.padding = '8px 12px';
        banner.style.fontWeight = 'bold';
        banner.style.fontSize = '14px';
        banner.style.zIndex = '99999';
        banner.style.boxShadow = '0 2px 10px rgba(0,0,0,0.3)';
        banner.innerHTML = '<i class="fas fa-wifi-slash me-2"></i> Offline Mode: WebAssembly Speech Processing Fallback Active';
        document.body.appendChild(banner);
    }
}

function hideOfflineBanner() {
    const banner = document.getElementById('offline-wasm-banner');
    if (banner) {
        banner.remove();
    }
}

// Check initial status on startup
if (!navigator.onLine) {
    showOfflineBanner();
}