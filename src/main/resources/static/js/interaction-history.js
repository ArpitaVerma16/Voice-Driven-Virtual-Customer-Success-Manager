// Interaction History JavaScript

let currentPage = 0;
let currentPageSize = 10;
let totalPages = 1;

// Initialize the page
document.addEventListener('DOMContentLoaded', function () {
    loadStatistics();
    loadInteractions();
});

/**
 * Load interaction statistics
 */
function loadStatistics() {
    fetch('/api/interactions/statistics/summary')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load statistics');
            return response.json();
        })
        .then(data => {
            updateStatistics(data);
        })
        .catch(error => {
            console.error('Error loading statistics:', error);
            showToast('Error loading statistics', 'danger');
        });
}

/**
 * Update statistics display
 */
function updateStatistics(stats) {
    document.getElementById('totalInteractions').textContent = stats.total || 0;
    document.getElementById('completedCount').textContent = stats.completed || 0;
    document.getElementById('pendingCount').textContent = stats.pending || 0;
    document.getElementById('positiveCount').textContent = stats.positive || 0;
}

/**
 * Load interactions with current filters
 */
function loadInteractions(page = 0) {
    currentPage = page;
    const searchTerm = document.getElementById('searchInput').value || '';
    const status = document.getElementById('statusFilter').value || '';
    const sentiment = document.getElementById('sentimentFilter').value || '';
    const category = document.getElementById('categoryFilter').value || '';

    showLoadingSpinner();

    let url = '/api/interactions?page=' + page + '&size=' + currentPageSize + '&sortBy=createdAt&sortDirection=DESC';

    if (searchTerm || status || sentiment || category) {
        url = '/api/interactions/search?page=' + page + '&size=' + currentPageSize +
              '&sortBy=createdAt&sortDirection=DESC';
        if (searchTerm) url += '&searchTerm=' + encodeURIComponent(searchTerm);
        if (status) url += '&status=' + status;
        if (sentiment) url += '&sentiment=' + sentiment;
        if (category) url += '&category=' + category;
    }

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Failed to load interactions');
            return response.json();
        })
        .then(data => {
            displayInteractions(data);
            setupPagination(data);
            hideLoadingSpinner();
        })
        .catch(error => {
            console.error('Error loading interactions:', error);
            showErrorState();
            hideLoadingSpinner();
        });
}

/**
 * Display interactions in the UI
 */
function displayInteractions(data) {
    const container = document.getElementById('interactionsContainer');
    
    if (data.content.length === 0) {
        container.innerHTML = `
            <div class="no-results">
                <i class="fas fa-inbox"></i>
                <h5>No Interactions Found</h5>
                <p>Try adjusting your search or filter criteria</p>
            </div>
        `;
        return;
    }

    let html = '';
    data.content.forEach(interaction => {
        const sentimentBadgeClass = getSentimentBadgeClass(interaction.sentiment);
        const statusBadgeClass = getStatusBadgeClass(interaction.status);
        const timestamp = formatDate(interaction.createdAt);
        const interactionIcon = getInteractionIcon(interaction.interactionType);

        html += `
            <div class="interaction-card">
                <div class="interaction-header">
                    <div class="customer-info">
                        <h5>
                            <i class="${interactionIcon}"></i> ${escapeHtml(interaction.customerName)}
                        </h5>
                        <p>${escapeHtml(interaction.interactionType)} • ${timestamp}</p>
                    </div>
                    <div>
                        <span class="badge ${sentimentBadgeClass}">${interaction.sentiment}</span>
                        <span class="badge ${statusBadgeClass}">${interaction.status}</span>
                        ${interaction.followUpRequired ? '<span class="badge follow-up-badge"><i class="fas fa-flag"></i> Follow-up</span>' : ''}
                    </div>
                </div>

                <div class="interaction-summary">
                    <h6>Summary</h6>
                    <p>${escapeHtml(interaction.summary)}</p>
                </div>

                <div class="interaction-details">
                    ${interaction.category ? `
                        <div class="detail-item">
                            <span class="detail-label"><i class="fas fa-tag"></i> Category</span>
                            <span class="detail-value">${escapeHtml(interaction.category)}</span>
                        </div>
                    ` : ''}
                    ${interaction.outcome ? `
                        <div class="detail-item">
                            <span class="detail-label"><i class="fas fa-check-circle"></i> Outcome</span>
                            <span class="detail-value">${escapeHtml(interaction.outcome)}</span>
                        </div>
                    ` : ''}
                    ${interaction.agentName ? `
                        <div class="detail-item">
                            <span class="detail-label"><i class="fas fa-user"></i> Agent</span>
                            <span class="detail-value">${escapeHtml(interaction.agentName)}</span>
                        </div>
                    ` : ''}
                    ${interaction.duration ? `
                        <div class="detail-item">
                            <span class="detail-label"><i class="fas fa-clock"></i> Duration</span>
                            <span class="detail-value">${escapeHtml(interaction.duration)}</span>
                        </div>
                    ` : ''}
                    ${interaction.customerEmail ? `
                        <div class="detail-item">
                            <span class="detail-label"><i class="fas fa-envelope"></i> Email</span>
                            <span class="detail-value">${escapeHtml(interaction.customerEmail)}</span>
                        </div>
                    ` : ''}
                </div>

                ${interaction.details ? `
                    <div class="interaction-summary" style="margin-top: 1rem; border-left-color: #d1d5db;">
                        <h6>Details</h6>
                        <p>${escapeHtml(interaction.details)}</p>
                    </div>
                ` : ''}

                <div class="timestamp">
                    <i class="fas fa-calendar-alt"></i> Created: ${timestamp}
                    ${interaction.updatedAt && interaction.updatedAt !== interaction.createdAt ? `
                        | Updated: ${formatDate(interaction.updatedAt)}
                    ` : ''}
                </div>

                <div class="interaction-actions">
                    <button class="btn btn-small btn-view" onclick="viewInteractionDetails(${interaction.id})">
                        <i class="fas fa-eye"></i> View Details
                    </button>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

/**
 * Setup pagination controls
 */
function setupPagination(data) {
    const paginationContainer = document.getElementById('paginationContainer');
    const paginationList = document.getElementById('paginationList');

    if (data.totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }

    paginationContainer.style.display = 'block';
    paginationList.innerHTML = '';

    // Previous button
    const prevLi = document.createElement('li');
    prevLi.className = 'page-item ' + (data.first ? 'disabled' : '');
    prevLi.innerHTML = '<a class="page-link" href="#" onclick="loadInteractions(' + (currentPage - 1) + '); return false;"><i class="fas fa-chevron-left"></i> Previous</a>';
    paginationList.appendChild(prevLi);

    // Page numbers
    for (let i = 0; i < data.totalPages; i++) {
        const li = document.createElement('li');
        li.className = 'page-item ' + (i === data.number ? 'active' : '');
        li.innerHTML = '<a class="page-link" href="#" onclick="loadInteractions(' + i + '); return false;">' + (i + 1) + '</a>';
        paginationList.appendChild(li);
    }

    // Next button
    const nextLi = document.createElement('li');
    nextLi.className = 'page-item ' + (data.last ? 'disabled' : '');
    nextLi.innerHTML = '<a class="page-link" href="#" onclick="loadInteractions(' + (currentPage + 1) + '); return false;">Next <i class="fas fa-chevron-right"></i></a>';
    paginationList.appendChild(nextLi);
}

/**
 * Apply filters and search
 */
function applyFilters() {
    currentPage = 0;
    loadInteractions(0);
}

/**
 * Reset filters
 */
function resetFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('statusFilter').value = '';
    document.getElementById('sentimentFilter').value = '';
    document.getElementById('categoryFilter').value = '';
    currentPage = 0;
    loadInteractions(0);
}

/**
 * View interaction details in modal
 */
function viewInteractionDetails(interactionId) {
    const modal = new bootstrap.Modal(document.getElementById('interactionDetailModal'));
    const body = document.getElementById('interactionDetailBody');
    body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';
    modal.show();

    fetch('/api/interactions/' + interactionId)
        .then(response => {
            if (!response.ok) throw new Error('Failed to load interaction');
            return response.json();
        })
        .then(interaction => {
            const sentimentBadgeClass = getSentimentBadgeClass(interaction.sentiment);
            const statusBadgeClass = getStatusBadgeClass(interaction.status);
            body.innerHTML = `
                <div class="mb-3 d-flex gap-2 flex-wrap">
                    <span class="badge ${sentimentBadgeClass}">${interaction.sentiment}</span>
                    <span class="badge ${statusBadgeClass}">${interaction.status}</span>
                    ${interaction.followUpRequired ? '<span class="badge follow-up-badge"><i class="fas fa-flag"></i> Follow-up Required</span>' : ''}
                </div>
                <table class="table table-bordered table-sm">
                    <tbody>
                        <tr><th style="width:35%">Customer Name</th><td>${escapeHtml(interaction.customerName)}</td></tr>
                        <tr><th>Interaction Type</th><td>${escapeHtml(interaction.interactionType)}</td></tr>
                        ${interaction.category ? `<tr><th>Category</th><td>${escapeHtml(interaction.category)}</td></tr>` : ''}
                        ${interaction.agentName ? `<tr><th>Agent</th><td>${escapeHtml(interaction.agentName)}</td></tr>` : ''}
                        ${interaction.duration ? `<tr><th>Duration</th><td>${escapeHtml(interaction.duration)}</td></tr>` : ''}
                        ${interaction.outcome ? `<tr><th>Outcome</th><td>${escapeHtml(interaction.outcome)}</td></tr>` : ''}
                        ${interaction.customerEmail ? `<tr><th>Customer Email</th><td>${escapeHtml(interaction.customerEmail)}</td></tr>` : ''}
                        ${interaction.customerPhone ? `<tr><th>Customer Phone</th><td>${escapeHtml(interaction.customerPhone)}</td></tr>` : ''}
                        <tr><th>Created At</th><td>${formatDate(interaction.createdAt)}</td></tr>
                        ${interaction.updatedAt && interaction.updatedAt !== interaction.createdAt ? `<tr><th>Updated At</th><td>${formatDate(interaction.updatedAt)}</td></tr>` : ''}
                    </tbody>
                </table>
                <div class="interaction-summary">
                    <h6>Summary</h6>
                    <p>${escapeHtml(interaction.summary)}</p>
                </div>
                ${interaction.details ? `
                    <div class="interaction-summary mt-3" style="border-left-color: #d1d5db;">
                        <h6>Details</h6>
                        <p>${escapeHtml(interaction.details)}</p>
                    </div>` : ''}
            `;
        })
        .catch(error => {
            console.error('Error loading interaction details:', error);
            body.innerHTML = '<div class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><p>Failed to load interaction details.</p></div>';
        });
}

/**
 * Export data as CSV
 */
function exportData() {
    const searchTerm = document.getElementById('searchInput').value || '';
    const status = document.getElementById('statusFilter').value || '';
    const sentiment = document.getElementById('sentimentFilter').value || '';
    const category = document.getElementById('categoryFilter').value || '';

    let url = '/api/interactions?page=0&size=1000&sortBy=createdAt&sortDirection=DESC';

    if (searchTerm || status || sentiment || category) {
        url = '/api/interactions/search?page=0&size=1000&sortBy=createdAt&sortDirection=DESC';
        if (searchTerm) url += '&searchTerm=' + encodeURIComponent(searchTerm);
        if (status) url += '&status=' + status;
        if (sentiment) url += '&sentiment=' + sentiment;
        if (category) url += '&category=' + category;
    }

    fetch(url)
        .then(response => response.json())
        .then(data => {
            const csv = convertToCSV(data.content);
            downloadCSV(csv, 'interaction-history.csv');
            showToast('Data exported successfully', 'success');
        })
        .catch(error => {
            console.error('Error exporting data:', error);
            showToast('Error exporting data', 'danger');
        });
}

/**
 * Convert data to CSV format
 */
function convertToCSV(interactions) {
    const headers = ['ID', 'Customer Name', 'Type', 'Category', 'Summary', 'Status', 'Sentiment', 'Outcome', 'Agent', 'Created At'];
    const rows = interactions.map(i => [
        i.id,
        i.customerName,
        i.interactionType,
        i.category || '',
        i.summary.replace(/,/g, ''),
        i.status,
        i.sentiment,
        i.outcome || '',
        i.agentName || '',
        i.createdAt
    ]);

    let csv = headers.join(',') + '\n';
    rows.forEach(row => {
        csv += row.map(cell => `"${cell}"`).join(',') + '\n';
    });

    return csv;
}

/**
 * Download CSV file
 */
function downloadCSV(csv, filename) {
    const link = document.createElement('a');
    link.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    link.download = filename;
    link.click();
}

/**
 * Helper: Get sentiment badge class
 */
function getSentimentBadgeClass(sentiment) {
    const map = {
        'POSITIVE': 'badge-success',
        'NEGATIVE': 'badge-danger',
        'NEUTRAL': 'badge-secondary'
    };
    return map[sentiment] || 'badge-secondary';
}

/**
 * Helper: Get status badge class
 */
function getStatusBadgeClass(status) {
    const map = {
        'COMPLETED': 'badge-success',
        'IN_PROGRESS': 'badge-info',
        'PENDING': 'badge-warning',
        'CANCELLED': 'badge-danger'
    };
    return map[status] || 'badge-secondary';
}

/**
 * Helper: Get interaction icon
 */
function getInteractionIcon(type) {
    const map = {
        'VOICE_CALL': 'fas fa-phone',
        'TEXT_CHAT': 'fas fa-comment',
        'EMAIL': 'fas fa-envelope',
        'VIDEO': 'fas fa-video'
    };
    return map[type] || 'fas fa-circle';
}

/**
 * Helper: Format date
 */
function formatDate(dateString) {
    const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString('en-US', options);
}

/**
 * Helper: Escape HTML special characters
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text ? text.replace(/[&<>"']/g, m => map[m]) : '';
}

/**
 * Show loading spinner
 */
function showLoadingSpinner() {
    const spinner = document.querySelector('.loading-spinner');
    if (spinner) {
        spinner.classList.add('active');
        spinner.style.display = 'block';
    }
}

/**
 * Hide loading spinner
 */
function hideLoadingSpinner() {
    const spinner = document.querySelector('.loading-spinner');
    if (spinner) {
        spinner.classList.remove('active');
        spinner.style.display = 'none';
    }
}

/**
 * Show error state
 */
function showErrorState() {
    const container = document.getElementById('interactionsContainer');
    container.innerHTML = `
        <div class="no-results">
            <i class="fas fa-exclamation-triangle"></i>
            <h5>Error Loading Interactions</h5>
            <p>Please try again later or contact support</p>
        </div>
    `;
}

/**
 * Show toast notification
 */
function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toastContainer');
    const toastId = 'toast-' + Date.now();

    const toastHTML = `
        <div id="${toastId}" class="toast show" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header bg-${type} text-white">
                <strong class="me-auto">Notification</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;

    toastContainer.innerHTML += toastHTML;

    const toastElement = document.getElementById(toastId);
    setTimeout(() => {
        toastElement.remove();
    }, 3000);
}

// Allow Enter key to trigger search
document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                applyFilters();
            }
        });
    }
});
