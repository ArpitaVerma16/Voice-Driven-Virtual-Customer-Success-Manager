// ===== VOICE ASSISTANT =====
let recognition = null;
let isRecording = false;
let lastCommandId = null; // Store last command ID for feedback

const STORAGE_KEY = "voiceConversationHistory";

let conversationHistory =
    JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];

    function saveConversation() {
    localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify(conversationHistory)
    );
}

function renderConversation() {
    const container = document.getElementById("conversationHistory");

    if (!container) return;

    container.innerHTML = "";

    conversationHistory.forEach(item => {
        container.innerHTML += `
            <div class="card mb-2">
                <div class="card-body py-2">
                    <strong>You:</strong> ${item.user}<br>
                    <strong>Assistant:</strong> ${item.bot}
                </div>
            </div>
        `;
    });
}

function startVoice() {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
        alert('Voice recognition not supported in this browser. Please use Chrome.');
        return;
    }

    if (isRecording) {
        recognition.stop();
        return;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    recognition = new SpeechRecognition();
    recognition.lang = 'en-IN';
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
        isRecording = true;
        document.getElementById('micBtn').classList.add('btn-danger', 'recording');
        document.getElementById('micBtn').classList.remove('btn-purple');
        document.getElementById('micIcon').className = 'fas fa-stop';
        if (typeof typingIndicator !== 'undefined') {
    typingIndicator.showListening();
}
    };

    recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    document.getElementById('voiceInput').value = transcript;

    if (typeof typingIndicator !== 'undefined') {
        typingIndicator.showProcessing();
    }

    sendCommand();
};

    recognition.onend = () => {
        isRecording = false;
        document.getElementById('micBtn').classList.remove('btn-danger', 'recording');
        document.getElementById('micBtn').classList.add('btn-purple');
        document.getElementById('micIcon').className = 'fas fa-microphone';
    };

    recognition.onerror = (e) => {
        console.error('Voice error:', e);
        isRecording = false;
        if (typeof typingIndicator !== 'undefined') {
            typingIndicator.hide();
        }
    };

    recognition.start();
}

function changePageSize(size) {
    const url = new URL(window.location.href);
    url.searchParams.set('size', size);
    url.searchParams.set('page', 0);
    window.location.href = url.toString();
}

async function sendCommand() {
    const input = document.getElementById('voiceInput');
    const transcript = input.value.trim();
    if (!transcript) return;
    const sendBtn = document.getElementById('sendBtn');
const sendBtnText = sendBtn?.querySelector('.btn-text');

sendBtn.disabled = true;
sendBtn.classList.add('disabled');

if (sendBtnText) {
    sendBtnText.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
}

    // Show typing indicator if available
    if (typeof typingIndicator !== 'undefined') {
        typingIndicator.showProcessing();
    }

    try {
        const res = await fetch('/api/voice/command', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ transcript })
        });
        const data = await res.json();
        
        const responseDiv = document.getElementById('voiceResponse');
        const responseText = document.getElementById('responseText');
        responseText.textContent = data.response || 'Command processed successfully!';
        conversationHistory.push({
    user: transcript,
    bot: data.response || 'Command processed successfully!'
});

saveConversation();
renderConversation();
        responseDiv.classList.remove('d-none');
        
        // Handle offline local navigation matching
        if (data.action) {
            if (data.action === "nav_complaints") {
                setTimeout(() => window.location.href = "/complaints", 1500);
            } else if (data.action === "nav_events") {
                setTimeout(() => window.location.href = "/events", 1500);
            } else if (data.action === "nav_dashboard") {
                setTimeout(() => window.location.href = "/", 1500);
            } else if (data.action === "nav_profile") {
                setTimeout(() => window.location.href = "/profile", 1500);
            }
        }

        // Store command ID for feedback
        if (data.id) {
            lastCommandId = data.id;
        }

        // Show feedback buttons
        const feedbackDiv = document.getElementById('feedbackButtons');
        if (feedbackDiv) {
            feedbackDiv.style.display = 'block';
            feedbackDiv.innerHTML = `
                <span class="text-muted small me-2">Was this helpful?</span>
                <button class="btn btn-sm btn-success" onclick="submitFeedback('UP')">
                    <i class="fas fa-thumbs-up"></i> Yes
                </button>
                <button class="btn btn-sm btn-danger" onclick="submitFeedback('DOWN')">
                    <i class="fas fa-thumbs-down"></i> No
                </button>
            `;
        }

        // Hide typing indicator
        if (typeof typingIndicator !== 'undefined') {
            typingIndicator.hide();
        }
        sendBtn.disabled = false;
sendBtn.classList.remove('disabled');

if (sendBtnText) {
    sendBtnText.innerHTML = '<i class="fas fa-paper-plane"></i> Send';
}

        input.value = '';

    } catch (err) {
    console.error('Error sending command:', err);

    if (typeof typingIndicator !== 'undefined') {
        typingIndicator.hide();
    }

    sendBtn.disabled = false;
    sendBtn.classList.remove('disabled');

    if (sendBtnText) {
        sendBtnText.innerHTML = '<i class="fas fa-paper-plane"></i> Send';
    }

    if (typeof toast !== 'undefined') {
        toast.error('Error processing command', 'Error');
    }
}
// Submit feedback for voice command
function submitFeedback(type) {
    if (!lastCommandId) {
        if (typeof toast !== 'undefined') {
            toast.error('No command to rate. Try a voice command first.', 'Error');
        }
        return;


    }

    const userId = localStorage.getItem('userId') || 1;

    fetch(`/api/voice/feedback?commandId=${lastCommandId}&userId=${userId}&feedback=${type}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            if (typeof toast !== 'undefined') {
                toast.success('Thank you for your feedback!', 'Feedback');
            }
            const feedbackDiv = document.getElementById('feedbackButtons');
            if (feedbackDiv) {
                feedbackDiv.innerHTML = '<span class="text-muted">✅ Feedback submitted</span>';
            }
        } else {
            if (typeof toast !== 'undefined') {
                toast.error('Error submitting feedback', 'Error');
            }
        }
    })
    .catch(err => {
        if (typeof toast !== 'undefined') {
            toast.error('Network error. Please try again.', 'Error');
        }
    });
}

// Submit feedback for voice command
function submitFeedback(type) {
    if (!lastCommandId) {
        if (typeof toast !== 'undefined') {
            toast.error('No command to rate. Try a voice command first.', 'Error');
        }
        return;

    }

    const userId = localStorage.getItem('userId') || 1;

    fetch(`/api/voice/feedback?commandId=${lastCommandId}&userId=${userId}&feedback=${type}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            if (typeof toast !== 'undefined') {
                toast.success('Thank you for your feedback!', 'Feedback');
            }
            const feedbackDiv = document.getElementById('feedbackButtons');
            if (feedbackDiv) {
                feedbackDiv.innerHTML = '<span class="text-muted">✅ Feedback submitted</span>';
            }
        } else {
            if (typeof toast !== 'undefined') {
                toast.error('Error submitting feedback', 'Error');
            }
        }
    })
    .catch(err => {
        if (typeof toast !== 'undefined') {
            toast.error('Network error. Please try again.', 'Error');
        }
    });
}

// Submit feedback for voice command
function submitFeedback(type) {
    if (!lastCommandId) {
        if (typeof toast !== 'undefined') {
            toast.error('No command to rate. Try a voice command first.', 'Error');
        }
        return;
    }

    const userId = localStorage.getItem('userId') || 1;

    fetch(`/api/voice/feedback?commandId=${lastCommandId}&userId=${userId}&feedback=${type}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            if (typeof toast !== 'undefined') {
                toast.success('Thank you for your feedback!', 'Feedback');
            }
            const feedbackDiv = document.getElementById('feedbackButtons');
            if (feedbackDiv) {
                feedbackDiv.innerHTML = '<span class="text-muted">✅ Feedback submitted</span>';
            }
        } else {
            if (typeof toast !== 'undefined') {
                toast.error('Error submitting feedback', 'Error');
            }
        }
    })
    .catch(err => {
        if (typeof toast !== 'undefined') {
            toast.error('Network error. Please try again.', 'Error');
        }
    });
}

// Allow Enter key to send command
document.addEventListener('DOMContentLoaded', () => {
    const voiceInput = document.getElementById('voiceInput');
    if (voiceInput) {
        voiceInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendCommand();
        });
    }
    renderConversation();
});

// ===== QUICK COMPLAINT (Dashboard) =====
function getJwt() {
    return localStorage.getItem('token');
}

function withAuthHeaders(headers = {}) {
    const token = getJwt();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    return headers;
}

async function quickFileComplaint() {
    const name = document.getElementById('qName')?.value?.trim();
    const apt = document.getElementById('qApt')?.value?.trim();
    const category = document.getElementById('qCategory')?.value;
    const desc = document.getElementById('qDesc')?.value?.trim();

    if (!name || !desc) {
        alert('Please fill in required fields (Name and Description).');
        return;
    }

    const complaint = {
        residentName: name,
        apartmentNumber: apt || null,
        category: category || 'OTHER',
        description: desc
    };

    try {
        const res = await fetch('/api/complaints', {
            method: 'POST',
            headers: withAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(complaint)
        });
        if (res.ok) {
            document.getElementById('complaintSuccess').classList.remove('d-none');
            document.getElementById('qName').value = '';
            document.getElementById('qApt').value = '';
            document.getElementById('qDesc').value = '';
            setTimeout(() => {
                document.getElementById('complaintSuccess').classList.add('d-none');
                location.reload();
            }, 2000);
        }
    } catch (err) {
        console.error('Error filing complaint:', err);
    }
}

// ===== COMPLAINT MODAL (Complaints page) =====
async function submitComplaint() {
    const complaint = {
        residentName: document.getElementById('m_name')?.value?.trim(),
        apartmentNumber: document.getElementById('m_apt')?.value?.trim() || null,
        contactEmail: document.getElementById('m_email')?.value?.trim() || null,
        category: document.getElementById('m_category')?.value || 'OTHER',
        description: document.getElementById('m_desc')?.value?.trim()
    };

    if (!complaint.residentName || !complaint.description) {
        alert('Please fill in required fields.');
        return;
    }

    try {
        const res = await fetch('/api/complaints', {
            method: 'POST',
            headers: withAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(complaint)
        });
        if (res.ok) {
            location.reload();
        }
    } catch (err) {
        console.error('Error:', err);
    }
}

async function updateComplaintStatus(id) {
    const status = prompt('Enter new status (OPEN, IN_PROGRESS, RESOLVED, CLOSED):');
    if (!status) return;
    try {
        await fetch(`/api/complaints/${id}/status?status=${status.toUpperCase()}`, { method: 'PUT', headers: withAuthHeaders() });
        location.reload();
    } catch (err) {
        console.error('Error updating status:', err);
    }
}

// ===== EVENT MODAL =====
async function submitEvent() {
    const event = {
        name: document.getElementById('ev_name')?.value?.trim(),
        description: document.getElementById('ev_desc')?.value?.trim(),
        category: document.getElementById('ev_cat')?.value,
        location: document.getElementById('ev_loc')?.value?.trim(),
        eventDate: document.getElementById('ev_date')?.value,
        maxCapacity: parseInt(document.getElementById('ev_cap')?.value || '50'),
        organizer: document.getElementById('ev_org')?.value?.trim(),
        active: true
    };

    if (!event.name) {
        alert('Event name is required.');
        return;
    }

    try {
        const res = await fetch('/api/events', {
            method: 'POST',
            headers: withAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(event)
        });
        if (res.ok) location.reload();
    } catch (err) {
        console.error('Error creating event:', err);
    }
}

async function registerEvent(id) {
    try {
        const res = await fetch(`/api/events/${id}/register`, { method: 'POST', headers: withAuthHeaders() });
        if (res.ok) {
            alert('Successfully registered for the event!');
            location.reload();
        } else {
            const msg = await res.text();
            alert('Registration failed: ' + msg);
        }
    } catch (err) {
        console.error('Error registering:', err);
    }



}



// ===== BULK OPERATIONS =====
let selectedIds = [];

function toggleAllCheckboxes(master) {
    document.querySelectorAll('.complaint-checkbox').forEach(cb => {
        cb.checked = master.checked;
    });
    updateSelectedCount();
}

function updateSelectedCount() {
    selectedIds = [];
    document.querySelectorAll('.complaint-checkbox:checked').forEach(cb => {
        selectedIds.push(parseInt(cb.value));
    });
    const countEl = document.getElementById('selectedCount');
    if (countEl) {
        countEl.textContent = selectedIds.length + ' selected';
    }
}

function selectAll() {
    document.querySelectorAll('.complaint-checkbox').forEach(cb => cb.checked = true);
    updateSelectedCount();
}

function deselectAll() {
    document.querySelectorAll('.complaint-checkbox').forEach(cb => cb.checked = false);
    updateSelectedCount();
}

function bulkUpdateStatus() {
    const status = document.getElementById('bulkStatus').value;
    if (!status) {
        if (typeof toast !== 'undefined') {
            toast.warning('Please select a status', 'Warning');
        }
        return;
    }
    if (selectedIds.length === 0) {
        if (typeof toast !== 'undefined') {
            toast.warning('Please select at least one complaint', 'Warning');
        }
        return;
    }

    fetch('/api/complaints/bulk/status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            complaintIds: selectedIds,
            status: status
        })
    })
    .then(r => r.json())
    .then(data => {
        if (data.success > 0) {
            if (typeof toast !== 'undefined') {
                toast.success(data.message, 'Success');
            }
            setTimeout(() => location.reload(), 1500);
        } else {
            if (typeof toast !== 'undefined') {
                toast.error('Bulk update failed', 'Error');
            }
        }
    })
    .catch(err => {
        if (typeof toast !== 'undefined') {
            toast.error('Network error', 'Error');
        }
    });
}

function bulkResolve() {
    if (selectedIds.length === 0) {
        if (typeof toast !== 'undefined') {
            toast.warning('Please select at least one complaint', 'Warning');
        }
        return;
    }

    if (!confirm('Are you sure you want to resolve ' + selectedIds.length + ' complaints?')) {
        return;
    }

    fetch('/api/complaints/bulk/resolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            complaintIds: selectedIds,
            resolutionNotes: 'Resolved via bulk action'
        })
    })
    .then(r => r.json())
    .then(data => {
        if (data.success > 0) {
            if (typeof toast !== 'undefined') {
                toast.success(data.message, 'Success');
            }
            setTimeout(() => location.reload(), 1500);
        } else {
            if (typeof toast !== 'undefined') {
                toast.error('Bulk resolve failed', 'Error');
            }
        }
    })
    .catch(err => {
        if (typeof toast !== 'undefined') {
            toast.error('Network error', 'Error');
        }
    });
}

// Event listeners for checkboxes
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.complaint-checkbox').forEach(cb => {
        cb.addEventListener('change', updateSelectedCount);
    });


});




// ===== WEBSOCKET NOTIFICATIONS =====
let stompClient = null;

function connectWebSocket() {
    if (typeof SockJS === 'undefined') {
        console.warn('SockJS not loaded. Loading dynamically...');
        loadSockJS();
        return;
    }
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function(frame) {
            console.log('✅ Connected to WebSocket');
            
            // Subscribe to user-specific notifications
            stompClient.subscribe('/user/queue/notifications', function(notification) {
                const data = JSON.parse(notification.body);
                showNotification(data);
                updateNotificationCount();
            });
            
            // Subscribe to global notifications
            stompClient.subscribe('/topic/global', function(notification) {
                const data = JSON.parse(notification.body);
                showNotification(data);
            });
        }, function(error) {
            console.error('WebSocket connection failed:', error);
        });
    } catch (e) {
        console.error('WebSocket error:', e);
    }
}

function loadSockJS() {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/sockjs-client@1.5.1/dist/sockjs.min.js';
    script.onload = function() {
        const stompScript = document.createElement('script');
        stompScript.src = 'https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js';
        stompScript.onload = function() { connectWebSocket(); };
        document.head.appendChild(stompScript);
    };
    document.head.appendChild(script);
}

function showNotification(data) {
    // Show toast
    if (typeof toast !== 'undefined') {
        const type = data.type ? data.type.toLowerCase() : 'info';
        toast[type](data.message || 'New notification', data.title || 'Notification');
    }
    // Update badge
    updateNotificationCount();
}

function updateNotificationCount() {
    fetch('/api/notifications/count')
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById('notificationBadge');
            if (badge) {
                if (data.count > 0) {
                    badge.style.display = 'inline';
                    badge.textContent = data.count > 99 ? '99+' : data.count;
                } else {
                    badge.style.display = 'none';
                }
            }
        })
        .catch(err => console.error('Error fetching notification count:', err));
}

function toggleNotifications() {
    const dropdown = document.getElementById('notificationDropdown');
    if (!dropdown) return;
    if (dropdown.style.display === 'none') {
        loadNotifications();
        dropdown.style.display = 'block';
    } else {
        dropdown.style.display = 'none';
    }
}

function loadNotifications() {
    fetch('/api/notifications')
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('notificationList');
            if (!list) return;
            if (data.length === 0) {
                list.innerHTML = `
                    <div class="text-center text-muted py-4">
                        <i class="fas fa-bell-slash fa-2x mb-2 d-block"></i>
                        <span>No notifications</span>
                    </div>
                `;
                return;
            }
            list.innerHTML = data.map(n => `
                <div class="dropdown-item notification-item ${n.read ? '' : 'unread'}" 
                     style="padding: 10px 12px; border-bottom: 1px solid #f0f0f0; ${n.read ? 'opacity: 0.7;' : 'background: #f8f9fa;'}"
                     onclick="markAsRead(${n.id})">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="flex-grow-1">
                            <div class="fw-bold ${n.read ? '' : 'text-purple'}">${n.title}</div>
                            <div class="small text-muted">${n.message}</div>
                        </div>
                        <small class="text-muted ms-2" style="font-size: 10px; white-space: nowrap;">
                            ${new Date(n.createdAt).toLocaleString()}
                        </small>
                    </div>
                    ${!n.read ? '<span class="badge bg-purple" style="font-size: 8px;">NEW</span>' : ''}
                </div>
            `).join('');
        })
        .catch(err => console.error('Error loading notifications:', err));
}

function markAsRead(id) {
    fetch(`/api/notifications/${id}/read`, { method: 'PUT' })
        .then(() => {
            updateNotificationCount();
            loadNotifications();
        })
        .catch(err => console.error('Error marking as read:', err));
}

function markAllRead() {
    fetch('/api/notifications/read-all', { method: 'PUT' })
        .then(() => {
            updateNotificationCount();
            loadNotifications();
            if (typeof toast !== 'undefined') {
                toast.success('All notifications marked as read', 'Success');
            }
        })
        .catch(err => console.error('Error marking all as read:', err));
}

// Close dropdown when clicking outside
document.addEventListener('click', function(event) {
    const dropdown = document.getElementById('notificationDropdown');
    const bell = document.querySelector('.btn-link .fa-bell')?.closest('.btn-link');
    if (dropdown && dropdown.style.display === 'block') {
        if (!dropdown.contains(event.target) && bell && !bell.contains(event.target)) {
            dropdown.style.display = 'none';
        }
    }
});

// Connect WebSocket on page load (if not already connected)
document.addEventListener('DOMContentLoaded', function() {
    // Connect WebSocket after a small delay
    setTimeout(function() {
        connectWebSocket();
        updateNotificationCount();
    }, 500);


})};





