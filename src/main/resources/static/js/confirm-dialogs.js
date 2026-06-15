// Confirmation Dialogs for Destructive Actions

// SweetAlert or custom confirm dialog
function showConfirmDialog(options) {
    // Use browser's native confirm (simple)
    if (options.simple) {
        return confirm(options.message || 'Are you sure?');
    }
    
    // Custom modal dialog (better UI)
    return new Promise((resolve) => {
        const modalHtml = `
            <div class="confirm-modal-overlay" id="confirmModal">
                <div class="confirm-modal">
                    <div class="confirm-modal-header">
                        <h5>${options.title || 'Confirm Action'}</h5>
                    </div>
                    <div class="confirm-modal-body">
                        <p>${options.message || 'Are you sure you want to proceed?'}</p>
                    </div>
                    <div class="confirm-modal-footer">
                        <button class="btn btn-secondary" onclick="closeConfirmModal(false)">Cancel</button>
                        <button class="btn btn-danger" onclick="closeConfirmModal(true)">${options.confirmText || 'Confirm'}</button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        window.confirmResolve = resolve;
    });
}

function closeConfirmModal(result) {
    const modal = document.getElementById('confirmModal');
    if (modal) modal.remove();
    if (window.confirmResolve) window.confirmResolve(result);
}

// Delete confirmation
function confirmDelete(itemType, itemId, deleteFunction) {
    showConfirmDialog({
        title: `Delete ${itemType}?`,
        message: `Are you sure you want to delete this ${itemType}? This action cannot be undone.`,
        confirmText: 'Delete',
        simple: false
    }).then((confirmed) => {
        if (confirmed) {
            deleteFunction(itemId);
        }
    });
}

// Form unsaved changes warning
let formHasChanges = false;

function trackFormChanges() {
    document.querySelectorAll('form input, form textarea, form select').forEach(field => {
        field.addEventListener('change', () => {
            formHasChanges = true;
        });
    });
    
    window.addEventListener('beforeunload', (e) => {
        if (formHasChanges) {
            e.preventDefault();
            e.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
            return e.returnValue;
        }
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    trackFormChanges();
});