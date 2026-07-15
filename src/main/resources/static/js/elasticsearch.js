// src/main/resources/static/js/elasticsearch.js
class ComplaintSearch {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.searchResults = document.getElementById('searchResults');
        this.autoCompleteList = document.getElementById('autoCompleteList');
        this.statusFilter = document.getElementById('statusFilter');
        this.categoryFilter = document.getElementById('categoryFilter');
        this.currentPage = 0;
        this.pageSize = 20;
        
        this.initEventListeners();
    }

    initEventListeners() {
        // Debounced search
        if (this.searchInput) {
            let debounceTimer;
            this.searchInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                const query = e.target.value;
                
                if (query.length > 2) {
                    debounceTimer = setTimeout(() => {
                        this.performSearch(query);
                    }, 300);
                    
                    // Auto-complete
                    this.getAutoComplete(query);
                } else {
                    this.clearResults();
                }
            });
        }

        // Filter changes
        if (this.statusFilter) {
            this.statusFilter.addEventListener('change', () => {
                this.performSearch(this.searchInput.value);
            });
        }

        if (this.categoryFilter) {
            this.categoryFilter.addEventListener('change', () => {
                this.performSearch(this.searchInput.value);
            });
        }
    }

    async performSearch(query) {
        try {
            const status = this.statusFilter ? this.statusFilter.value : '';
            const category = this.categoryFilter ? this.categoryFilter.value : '';
            
            const url = `/api/elasticsearch/complaints/search/paginated?query=${encodeURIComponent(query)}&page=${this.currentPage}&size=${this.pageSize}`;
            
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`Search failed: ${response.status}`);
            }
            
            const data = await response.json();
            this.renderResults(data);
            
        } catch (error) {
            console.error('Search failed:', error);
            this.showError('Search failed. Please try again.');
        }
    }

    async getAutoComplete(prefix) {
        if (prefix.length < 2) {
            this.autoCompleteList.style.display = 'none';
            return;
        }

        try {
            const response = await fetch(`/api/elasticsearch/complaints/autocomplete?prefix=${encodeURIComponent(prefix)}`);
            
            if (!response.ok) {
                throw new Error('Auto-complete failed');
            }
            
            const suggestions = await response.json();
            this.renderAutoComplete(suggestions);
            
        } catch (error) {
            console.error('Auto-complete failed:', error);
        }
    }

    renderAutoComplete(suggestions) {
        if (!this.autoCompleteList) return;
        
        this.autoCompleteList.innerHTML = '';
        
        if (suggestions.length === 0) {
            this.autoCompleteList.style.display = 'none';
            return;
        }

        suggestions.forEach(suggestion => {
            const item = document.createElement('div');
            item.className = 'autocomplete-item';
            item.textContent = suggestion;
            item.addEventListener('click', () => {
                this.searchInput.value = suggestion;
                this.performSearch(suggestion);
                this.autoCompleteList.style.display = 'none';
            });
            this.autoCompleteList.appendChild(item);
        });

        this.autoCompleteList.style.display = 'block';
    }

    renderResults(data) {
        if (!this.searchResults) return;

        this.searchResults.innerHTML = '';

        if (!data.content || data.content.length === 0) {
            this.searchResults.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-search"></i>
                    <p>No complaints found matching your search</p>
                </div>
            `;
            return;
        }

        data.content.forEach(complaint => {
            const card = this.createComplaintCard(complaint);
            this.searchResults.appendChild(card);
        });

        // Render pagination
        this.renderPagination(data);
    }

    createComplaintCard(complaint) {
        const card = document.createElement('div');
        card.className = 'complaint-card';
        
        const statusColors = {
            'OPEN': 'warning',
            'IN_PROGRESS': 'primary',
            'RESOLVED': 'success',
            'CLOSED': 'secondary'
        };

        card.innerHTML = `
            <div class="card-header">
                <span class="complaint-id">#${complaint.complaintId}</span>
                <span class="badge bg-${statusColors[complaint.status] || 'secondary'}">${complaint.status}</span>
                <span class="category-tag">${complaint.category}</span>
            </div>
            <div class="card-body">
                <h5 class="resident-name">${complaint.residentName}</h5>
                <p class="description">${this.highlightText(complaint.description)}</p>
                <div class="meta-info">
                    <span><i class="fas fa-building"></i> ${complaint.apartmentNumber}</span>
                    <span><i class="fas fa-calendar"></i> ${new Date(complaint.createdAt).toLocaleDateString()}</span>
                </div>
            </div>
            <div class="card-footer">
                <button class="btn btn-sm btn-outline-primary view-details" data-id="${complaint.complaintId}">
                    View Details
                </button>
            </div>
        `;

        // Add event listener for view details
        card.querySelector('.view-details').addEventListener('click', () => {
            this.viewComplaintDetails(complaint.complaintId);
        });

        return card;
    }

    highlightText(text) {
        if (!this.searchInput || !this.searchInput.value) return text;
        
        const query = this.searchInput.value;
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<mark>$1</mark>');
    }

    renderPagination(data) {
        const paginationContainer = document.getElementById('paginationContainer');
        if (!paginationContainer) return;

        paginationContainer.innerHTML = '';

        if (data.totalPages <= 1) return;

        const nav = document.createElement('nav');
        nav.innerHTML = `
            <ul class="pagination">
                <li class="page-item ${data.first ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${data.pageNumber - 1}">Previous</a>
                </li>
                ${this.generatePageItems(data)}
                <li class="page-item ${data.last ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${data.pageNumber + 1}">Next</a>
                </li>
            </ul>
        `;

        nav.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(link.dataset.page);
                if (page >= 0 && page < data.totalPages) {
                    this.currentPage = page;
                    this.performSearch(this.searchInput.value);
                }
            });
        });

        paginationContainer.appendChild(nav);
    }

    generatePageItems(data) {
        let items = '';
        const start = Math.max(0, data.pageNumber - 2);
        const end = Math.min(data.totalPages - 1, data.pageNumber + 2);

        if (start > 0) {
            items += `<li class="page-item"><a class="page-link" href="#" data-page="0">1</a></li>`;
            if (start > 1) {
                items += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        for (let i = start; i <= end; i++) {
            items += `
                <li class="page-item ${i === data.pageNumber ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>
            `;
        }

        if (end < data.totalPages - 1) {
            if (end < data.totalPages - 2) {
                items += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            items += `
                <li class="page-item">
                    <a class="page-link" href="#" data-page="${data.totalPages - 1}">${data.totalPages}</a>
                </li>
            `;
        }

        return items;
    }

    clearResults() {
        if (this.searchResults) {
            this.searchResults.innerHTML = '';
        }
        if (this.autoCompleteList) {
            this.autoCompleteList.style.display = 'none';
        }
    }

    showError(message) {
        if (this.searchResults) {
            this.searchResults.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-circle"></i>
                    ${message}
                </div>
            `;
        }
    }

    async viewComplaintDetails(complaintId) {
        // Navigate to complaint details page or show modal
        window.location.href = `/complaints/${complaintId}`;
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    new ComplaintSearch();
});