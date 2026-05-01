// Muse Agent Dashboard JavaScript

/**
 * Toggle collapsible card
 * @param {string} cardId - The ID of the card to toggle
 */
function toggleCard(cardId) {
    const card = document.querySelector(`[data-card-id="${cardId}"]`);
    if (card) {
        card.classList.toggle('collapsed');
        // Save collapse state to localStorage
        const collapsedCards = JSON.parse(localStorage.getItem('collapsedCards') || '[]');
        if (card.classList.contains('collapsed')) {
            if (!collapsedCards.includes(cardId)) {
                collapsedCards.push(cardId);
            }
        } else {
            const index = collapsedCards.indexOf(cardId);
            if (index > -1) {
                collapsedCards.splice(index, 1);
            }
        }
        localStorage.setItem('collapsedCards', JSON.stringify(collapsedCards));
        // Update toggle button state
        updateToggleButtonState();
    }
}

/**
 * Load saved collapse state from localStorage.
 *
 * Card-ids the user has never seen before (i.e. introduced by a later
 * release) default to collapsed, matching the first-visit policy. Without
 * this, returning users see new cards as expanded while everything else
 * stays in their saved state — visually inconsistent.
 */
function loadCollapseState() {
    const collapsedSet = new Set(JSON.parse(localStorage.getItem('collapsedCards') || '[]'));
    const seenSet = new Set(JSON.parse(localStorage.getItem('seenCardIds') || '[]'));
    const allCards = document.querySelectorAll('.collapsible-card');
    let collapsedChanged = false;
    let seenChanged = false;

    allCards.forEach(card => {
        const cardId = card.getAttribute('data-card-id');
        if (!cardId) return;
        if (!seenSet.has(cardId)) {
            // New card never rendered for this user before — collapse by default.
            if (!collapsedSet.has(cardId)) {
                collapsedSet.add(cardId);
                collapsedChanged = true;
            }
            seenSet.add(cardId);
            seenChanged = true;
        }
        if (collapsedSet.has(cardId)) {
            card.classList.add('collapsed');
        }
    });

    if (collapsedChanged) {
        localStorage.setItem('collapsedCards', JSON.stringify([...collapsedSet]));
    }
    if (seenChanged) {
        localStorage.setItem('seenCardIds', JSON.stringify([...seenSet]));
    }
}

/**
 * Toggle all cards between collapsed and expanded state
 */
function toggleAllCards() {
    const cards = document.querySelectorAll('.collapsible-card');
    const btn = document.getElementById('btnToggleAll');

    // Check if all cards are collapsed
    const allCollapsed = Array.from(cards).every(card => card.classList.contains('collapsed'));

    if (allCollapsed) {
        // Expand all
        expandAllCards();
        btn.textContent = I18N.collapseAll;
        btn.style.color = 'var(--warning-color)';
        btn.style.borderColor = 'var(--warning-color)';
    } else {
        // Collapse all
        collapseAllCards();
        btn.textContent = I18N.expandAll;
        btn.style.color = 'var(--success-color)';
        btn.style.borderColor = 'var(--success-color)';
    }
}

/**
 * Collapse all cards
 */
function collapseAllCards() {
    const cards = document.querySelectorAll('.collapsible-card');
    cards.forEach(card => {
        if (!card.classList.contains('collapsed')) {
            card.classList.add('collapsed');
        }
    });
    // Save all as collapsed
    const allCardIds = Array.from(cards).map(card => card.getAttribute('data-card-id'));
    localStorage.setItem('collapsedCards', JSON.stringify(allCardIds));
}

/**
 * Expand all cards
 */
function expandAllCards() {
    const cards = document.querySelectorAll('.collapsible-card');
    cards.forEach(card => {
        card.classList.remove('collapsed');
    });
    // Clear collapsed state
    localStorage.setItem('collapsedCards', JSON.stringify([]));
}

// Load collapse state when page loads
document.addEventListener('DOMContentLoaded', function() {
    // Check if first visit (no saved state)
    const savedState = localStorage.getItem('collapsedCards');
    if (savedState === null) {
        // First visit - collapse all cards by default
        const cards = document.querySelectorAll('.collapsible-card');
        const cardIds = Array.from(cards).map(card => card.getAttribute('data-card-id'));
        localStorage.setItem('collapsedCards', JSON.stringify(cardIds));
        // Seed seenCardIds so that future additions are detected as "new"
        // (without this, a first-visit user wouldn't get the new-card collapse default).
        localStorage.setItem('seenCardIds', JSON.stringify(cardIds));
        cards.forEach(card => card.classList.add('collapsed'));
    } else {
        // Load saved state
        loadCollapseState();
    }
    updateToggleButtonState();
    initScrollToTopButton();
});

/**
 * Initialize scroll to top button
 */
function initScrollToTopButton() {
    const btn = document.getElementById('btnTop');

    window.addEventListener('scroll', function() {
        if (window.scrollY > 300) {
            btn.classList.add('show');
        } else {
            btn.classList.remove('show');
        }
    });
}

/**
 * Scroll to top with smooth animation
 */
function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

/**
 * Update toggle button state based on current collapse state
 */
function updateToggleButtonState() {
    const cards = document.querySelectorAll('.collapsible-card');
    const btn = document.getElementById('btnToggleAll');

    if (!btn) return;

    const allCollapsed = Array.from(cards).every(card => card.classList.contains('collapsed'));

    if (allCollapsed) {
        btn.textContent = I18N.expandAll;
        btn.style.color = 'var(--success-color)';
        btn.style.borderColor = 'var(--success-color)';
    } else {
        btn.textContent = I18N.collapseAll;
        btn.style.color = 'var(--warning-color)';
        btn.style.borderColor = 'var(--warning-color)';
    }
}

/**
 * Test all connections to Echo Server
 */
async function testConnection() {
    const btn = document.getElementById('btnTestConnection');
    setButtonLoading(btn, true);
    showResult(I18N.testing);

    // Remove startup badge if exists (manual test overwrites startup result)
    const startupBadge = document.querySelector('.startup-badge');
    if (startupBadge) {
        startupBadge.remove();
    }

    try {
        const response = await fetch('/api/test/connection');
        const data = await response.json();
        showResult(JSON.stringify(data, null, 2));
        updateStatusSection(data);
    } catch (error) {
        showResult('Error: ' + error.message, true);
    } finally {
        setButtonLoading(btn, false);
    }
}

/**
 * Set button loading state
 */
function setButtonLoading(btn, loading) {
    if (loading) {
        btn.disabled = true;
        btn.dataset.originalText = btn.innerHTML;
        btn.innerHTML = '<span class="loading"></span> ' + I18N.testing;
    } else {
        btn.disabled = false;
        btn.innerHTML = btn.dataset.originalText;
    }
}

/**
 * Show result in the result section
 */
function showResult(content, isError = false) {
    const resultDiv = document.getElementById('testResult');
    const resultContent = document.getElementById('resultContent');

    if (!resultDiv || !resultContent) return;

    resultDiv.classList.remove('hidden');
    resultContent.textContent = content;

    if (isError) {
        resultContent.style.color = '#dc3545';
    } else {
        resultContent.style.color = '#e0e0e0';
    }
}

/**
 * Update status section with connection test results
 */
function updateStatusSection(data) {
    const statusContent = document.getElementById('statusContent');
    const statusBadge = document.getElementById('currentStatusBadge');

    if (!data) {
        statusContent.innerHTML = '<p class="status-message">-</p>';
        if (statusBadge) statusBadge.textContent = '-';
        return;
    }

    // Update status badge
    if (statusBadge) {
        if (data.overallSuccess) {
            statusBadge.textContent = I18N.connected;
            statusBadge.style.backgroundColor = 'rgba(40, 167, 69, 0.2)';
            statusBadge.style.color = 'var(--success-color)';
        } else {
            statusBadge.textContent = I18N.failed;
            statusBadge.style.backgroundColor = 'rgba(220, 53, 69, 0.2)';
            statusBadge.style.color = 'var(--danger-color)';
        }
    }

    // Phase 5a: footer Echo Server 상태 동기화
    updateFooterEchoStatus(data.overallSuccess);

    const failureGuideTooltip = `
        <span class="status-tooltip-wrap" tabindex="0">
            <span class="status-tooltip-trigger" aria-label="Echo Server 연결 안내">?</span>
            <span class="status-tooltip-box" role="tooltip">
                Echo Server 연결 실패. 아래 순서로 확인하세요.<br>
                1) URL / Client ID / Client Secret 확인<br>
                2) 저장 후 에이전트 재시작<br>
                - Windows 서비스 실행 시(관리자 CMD):<br>
                &nbsp;&nbsp;sc stop muse-agent &amp;&amp; sc start muse-agent<br>
                - WinSW 사용 시 서비스 폴더에서:<br>
                &nbsp;&nbsp;muse-agent-service.exe restart<br>
                3) 방화벽/프록시/네트워크 접근 확인<br>
                <a href="/echo-config">Echo Server 설정 관리로 이동</a><br>
                <a href="/device/register">디바이스 등록 페이지로 이동</a>
            </span>
        </span>
    `;

    let html = '<div class="status-grid">';

    // Overall status
    html += `
        <div class="status-item">
            <span class="status-label">${I18N.overallStatus}</span>
            <span class="status-value ${data.overallSuccess ? 'success' : 'error'}">
                ${data.overallSuccess ? I18N.connected : I18N.failed + failureGuideTooltip}
            </span>
        </div>
    `;

    // Timestamp
    if (data.timestamp) {
        html += `
            <div class="status-item">
                <span class="status-label">${I18N.lastTest}</span>
                <span class="status-value" style="font-size: 0.9rem; color: #a0a0a0;">
                    ${formatTimestamp(data.timestamp)}
                </span>
            </div>
        `;
    }

    html += '</div>';

    statusContent.innerHTML = html;
}

/**
 * Format timestamp for display
 */
function formatTimestamp(timestamp) {
    try {
        const date = new Date(timestamp);
        return date.toLocaleString();
    } catch (e) {
        return timestamp;
    }
}

/**
 * Load startup connection test results
 * Automatically fetches and displays results from application startup test
 */
async function loadStartupTestResult() {
    const statusContent = document.getElementById('statusContent');

    // Show loading state
    statusContent.innerHTML = '<p class="status-message">' + I18N.testing + '</p>';

    try {
        const response = await fetch('/api/test/startup-result');
        const data = await response.json();

        if (!data.completed) {
            // Test still in progress
            statusContent.innerHTML = `
                <p class="status-message" style="color: #ffa500;">
                    ${data.message}
                </p>
            `;

            // Retry after 2 seconds (max 10 retries = 20 seconds)
            const retryCount = (loadStartupTestResult.retryCount || 0) + 1;
            if (retryCount <= 10) {
                loadStartupTestResult.retryCount = retryCount;
                setTimeout(loadStartupTestResult, 2000);
            } else {
                statusContent.innerHTML = `
                    <p class="status-message" style="color: #dc3545;">
                        ${I18N.failed}
                    </p>
                `;
            }
            return;
        }

        // Reset retry counter on success
        loadStartupTestResult.retryCount = 0;

        // Display results
        showResult(JSON.stringify(data, null, 2));
        updateStatusSection(data);

        // Add startup indicator badge
        if (data.testSource === 'STARTUP') {
            const resultDiv = document.getElementById('testResult');
            if (resultDiv) {
                const startupBadge = document.createElement('div');
                startupBadge.className = 'startup-badge';
                startupBadge.textContent = 'Startup Test Result';
                resultDiv.insertBefore(startupBadge, resultDiv.firstChild);
            }
        }

    } catch (error) {
        statusContent.innerHTML = `
            <p class="status-message" style="color: #dc3545;">
                Error: ${error.message}
            </p>
        `;
    }
}

/**
 * Refresh alive status from Echo Server
 */
async function refreshAliveStatus() {
    const btn = document.getElementById('btnRefreshAlive');
    if (btn) setButtonLoading(btn, true);

    const content = document.getElementById('aliveStatusContent');
    content.innerHTML = '<p class="status-message">' + I18N.testing + '</p>';

    try {
        const response = await fetch('/api/test/alive-status');
        const data = await response.json();

        if (data.success) {
            updateAliveStatusDisplay(data);
        } else {
            content.innerHTML = `<p class="status-message" style="color: var(--danger-color);">${data.message}</p>`;
        }
    } catch (error) {
        content.innerHTML = `<p class="status-message" style="color: var(--danger-color);">Error: ${error.message}</p>`;
    } finally {
        if (btn) setButtonLoading(btn, false);
    }
}

/**
 * Update alive status display
 */
function updateAliveStatusDisplay(data) {
    const content = document.getElementById('aliveStatusContent');
    const statusBadge = document.getElementById('aliveStatusBadge');
    const statusColor = data.status === 'OK' ? 'var(--success-color)'
                       : data.status === 'WARN' ? 'var(--warning-color)'
                       : 'var(--danger-color)';

    // Update alive status badge
    if (statusBadge) {
        statusBadge.textContent = data.status || '-';
        if (data.status === 'OK') {
            statusBadge.style.backgroundColor = 'rgba(40, 167, 69, 0.2)';
            statusBadge.style.color = 'var(--success-color)';
        } else if (data.status === 'WARN') {
            statusBadge.style.backgroundColor = 'rgba(255, 193, 7, 0.2)';
            statusBadge.style.color = 'var(--warning-color)';
        } else {
            statusBadge.style.backgroundColor = 'rgba(220, 53, 69, 0.2)';
            statusBadge.style.color = 'var(--danger-color)';
        }
    }

    let html = '<div class="status-grid">';
    html += `
        <div class="status-item">
            <span class="status-label">${I18N.aliveStatusLabel}</span>
            <span class="status-value" style="color: ${statusColor}; font-weight: bold; font-size: 1.4rem;">
                ${data.status || 'N/A'}
            </span>
        </div>
        <div class="status-item">
            <span class="status-label">${I18N.lastConfirmed}</span>
            <span class="status-value" style="font-size: 0.95rem;">
                ${data.lastConfirmAt ? formatTimestamp(data.lastConfirmAt) : 'Never'}
            </span>
        </div>
        <div class="status-item">
            <span class="status-label">${I18N.nextDeadline}</span>
            <span class="status-value" style="font-size: 0.95rem;">
                ${data.nextDeadline ? formatTimestamp(data.nextDeadline) : 'N/A'}
            </span>
        </div>
        <div class="status-item">
            <span class="status-label">${I18N.ttl}</span>
            <span class="status-value">${data.ttlValue != null ? data.ttlValue + ' ' + (data.ttlUnit || '') : 'N/A'}</span>
        </div>
    `;
    html += '</div>';

    if (data.statusMessage) {
        html += `
            <div style="margin-top: 15px; padding: 12px; background-color: rgba(0,0,0,0.2); border-radius: 8px; border-left: 4px solid ${statusColor};">
                <p style="color: var(--text-secondary); margin: 0;">${data.statusMessage}</p>
            </div>
        `;
    }

    content.innerHTML = html;
}

/**
 * Load alive history from Echo Server
 */
async function loadAliveHistory() {
    const btn = document.getElementById('btnAliveHistory');
    if (btn) setButtonLoading(btn, true);

    const historyDiv = document.getElementById('aliveHistoryContent');
    historyDiv.classList.remove('hidden');
    historyDiv.innerHTML = '<p class="status-message">' + I18N.testing + '</p>';

    try {
        const response = await fetch('/api/test/alive-history?limit=10');
        const data = await response.json();

        if (data.success && data.events && data.events.length > 0) {
            let html = '<div style="background-color: rgba(0,0,0,0.2); border-radius: 8px; padding: 15px;">';
            html += `<h4 style="margin-bottom: 10px; color: #4a90d9;">${I18N.recentEvents}</h4>`;
            html += '<table style="width: 100%; border-collapse: collapse;">';
            html += `<thead><tr style="color: var(--text-secondary); font-size: 0.85rem; text-transform: uppercase;">`;
            html += `<th style="text-align: left; padding: 8px;">${I18N.source}</th>`;
            html += `<th style="text-align: left; padding: 8px;">${I18N.confirmTime}</th>`;
            html += `<th style="text-align: left; padding: 8px;">Request ID</th>`;
            html += '</tr></thead><tbody>';

            data.events.forEach(function(event) {
                html += `<tr style="border-top: 1px solid var(--border-color);">`;
                html += `<td style="padding: 8px; color: var(--text-primary);">${event.source || '-'}</td>`;
                html += `<td style="padding: 8px; color: var(--text-primary);">${event.confirmedAt ? formatTimestamp(event.confirmedAt) : '-'}</td>`;
                html += `<td style="padding: 8px; color: var(--text-secondary); font-size: 0.9rem;">${event.requestId || '-'}</td>`;
                html += `</tr>`;
            });

            html += '</tbody></table>';
            html += `<p style="margin-top: 10px; color: var(--text-secondary); font-size: 0.85rem;">Total: ${data.total || 0} events</p>`;
            html += '</div>';
            historyDiv.innerHTML = html;
        } else if (data.success) {
            historyDiv.innerHTML = `<p class="status-message">${I18N.noEvents}</p>`;
        } else {
            historyDiv.innerHTML = `<p class="status-message" style="color: var(--danger-color);">${data.message}</p>`;
        }
    } catch (error) {
        historyDiv.innerHTML = `<p class="status-message" style="color: var(--danger-color);">Error: ${error.message}</p>`;
    } finally {
        if (btn) setButtonLoading(btn, false);
    }
}

let currentTaskHistoryRows = [];
let taskHistorySummaryMode = 'range';
let taskHistorySummaryDays = 7;

/**
 * Load task execution history from Muse Agent API.
 */
async function loadTaskExecutionHistory() {
    const btn = document.getElementById('btnTaskHistory');
    if (btn) setButtonLoading(btn, true);

    const historyDiv = document.getElementById('taskHistoryContent');
    historyDiv.classList.remove('hidden');
    historyDiv.innerHTML = '<p class="status-message">' + I18N.testing + '</p>';

    const limit = document.getElementById('taskHistoryLimit')?.value || '20';
    const taskGroup = document.getElementById('taskHistoryTaskGroup')?.value || '';
    const taskKey = document.getElementById('taskHistoryTaskKey')?.value || '';
    const status = document.getElementById('taskHistoryStatus')?.value || '';
    const startDate = document.getElementById('taskHistoryStartDate')?.value || '';
    const endDate = document.getElementById('taskHistoryEndDate')?.value || '';
    const sortBy = document.getElementById('taskHistorySortBy')?.value || 'createdAt';
    const sortDir = document.getElementById('taskHistorySortDir')?.value || 'desc';

    const params = new URLSearchParams();
    params.set('limit', limit);
    params.set('sortBy', sortBy);
    params.set('sortDir', sortDir);
    if (taskGroup) params.set('taskGroup', taskGroup);
    if (taskKey) params.set('taskKey', taskKey);
    if (status) params.set('status', status);
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);

    try {
        const response = await fetch('/api/task-executions/history?' + params.toString());
        const data = await response.json();

        if (!data.success) {
            historyDiv.innerHTML = `<p class="status-message" style="color: var(--danger-color);">${data.message || 'Failed to load history'}</p>`;
            return;
        }

        const rows = Array.isArray(data.data) ? data.data : [];
        currentTaskHistoryRows = rows;

        if (rows.length === 0) {
            historyDiv.innerHTML = `<p class="status-message">${I18N.noTaskHistory}</p>`;
            return;
        }

        let html = '<div style="background-color: rgba(0,0,0,0.2); border-radius: 8px; padding: 15px;">';
        html += `<h4 style="margin-bottom: 10px; color: #4a90d9;">${I18N.taskHistoryTitle}</h4>`;
        html += '<div style="overflow-x:auto;">';
        html += '<table style="width: 100%; border-collapse: collapse;">';
        html += '<thead><tr style="color: var(--text-secondary); font-size: 0.85rem; text-transform: uppercase;">';
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColTask}</th>`;
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColStatus}</th>`;
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColRunAt}</th>`;
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColTarget}</th>`;
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColSuccess}</th>`;
        html += `<th style="text-align: left; padding: 8px;">${I18N.taskColFailure}</th>`;
        html += '</tr></thead><tbody>';

        rows.forEach(function(item, index) {
            const statusInfo = normalizeTaskStatus(item.status);
            const taskLabel = item.taskName || item.taskKey || '-';
            html += `<tr class="task-history-row" data-row-index="${index}" style="border-top: 1px solid var(--border-color); cursor: pointer;">`;
            html += `<td style="padding: 8px; color: var(--text-primary);">${escapeHtml(taskLabel)}</td>`;
            html += `<td style="padding: 8px; color: ${statusInfo.color}; font-weight: 600;">${escapeHtml(statusInfo.label)}</td>`;
            html += `<td style="padding: 8px; color: var(--text-primary);">${item.completedAt ? formatTimestamp(item.completedAt) : '-'}</td>`;
            html += `<td style="padding: 8px; color: var(--text-primary);">${item.targetCount != null ? item.targetCount : '-'}</td>`;
            html += `<td style="padding: 8px; color: var(--text-primary);">${item.successCount != null ? item.successCount : '-'}</td>`;
            html += `<td style="padding: 8px; color: var(--text-primary);">${item.failureCount != null ? item.failureCount : '-'}</td>`;
            html += '</tr>';
        });

        html += '</tbody></table></div>';
        html += '</div>';
        historyDiv.innerHTML = html;

        historyDiv.querySelectorAll('.task-history-row').forEach(function(rowEl) {
            rowEl.addEventListener('click', function() {
                const rowIndex = Number(rowEl.dataset.rowIndex || -1);
                if (rowIndex >= 0 && currentTaskHistoryRows[rowIndex]) {
                    openTaskHistoryDetailModal(currentTaskHistoryRows[rowIndex]);
                }
            });
        });
    } catch (error) {
        historyDiv.innerHTML = `<p class="status-message" style="color: var(--danger-color);">Error: ${error.message}</p>`;
    } finally {
        if (btn) setButtonLoading(btn, false);
    }
}

function resetTaskHistoryFilters() {
    const groupEl = document.getElementById('taskHistoryTaskGroup');
    const keyEl = document.getElementById('taskHistoryTaskKey');
    const statusEl = document.getElementById('taskHistoryStatus');
    const startDateEl = document.getElementById('taskHistoryStartDate');
    const endDateEl = document.getElementById('taskHistoryEndDate');
    const sortByEl = document.getElementById('taskHistorySortBy');
    const sortDirEl = document.getElementById('taskHistorySortDir');
    const limitEl = document.getElementById('taskHistoryLimit');

    if (groupEl) groupEl.value = '';
    if (keyEl) keyEl.value = '';
    if (statusEl) statusEl.value = '';
    if (startDateEl) startDateEl.value = '';
    if (endDateEl) endDateEl.value = '';
    if (sortByEl) sortByEl.value = 'createdAt';
    if (sortDirEl) sortDirEl.value = 'desc';
    if (limitEl) limitEl.value = '20';
}

function openTaskHistoryDetailModal(item) {
    const modal = document.getElementById('taskHistoryDetailModal');
    const body = document.getElementById('taskHistoryDetailBody');
    const summaryBadge = document.getElementById('taskHistorySummaryBadge');
    if (!modal || !body || !summaryBadge || !item) {
        return;
    }

    const summary = buildTaskSummaryBadge(item);
    summaryBadge.textContent = summary.label;
    summaryBadge.style.color = summary.color;
    summaryBadge.style.background = summary.background;
    summaryBadge.style.borderColor = summary.borderColor;

    const metadata = prettyMetadata(item.metadataJson);
    const detailText = [
        `${I18N.taskDetailExecutionId}: ${item.executionId || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailTaskGroup}: ${item.taskGroup || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailTaskKey}: ${item.taskKey || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailTaskName}: ${item.taskName || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailStatus}: ${item.status || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailTarget}: ${item.targetCount != null ? item.targetCount : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailSuccess}: ${item.successCount != null ? item.successCount : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailFailure}: ${item.failureCount != null ? item.failureCount : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailStartedAt}: ${item.startedAt ? formatTimestamp(item.startedAt) : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailCompletedAt}: ${item.completedAt ? formatTimestamp(item.completedAt) : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailCreatedAt}: ${item.createdAt ? formatTimestamp(item.createdAt) : I18N.taskDetailEmpty}`,
        `${I18N.taskDetailError}: ${item.errorMessage || I18N.taskDetailEmpty}`,
        `${I18N.taskDetailMetadata}: ${metadata || I18N.taskDetailEmpty}`
    ].join('\n');

    body.textContent = detailText;
    modal.classList.remove('hidden');
    modal.style.display = 'flex';

    modal.onclick = function(event) {
        if (event.target === modal) {
            closeTaskHistoryDetailModal();
        }
    };
}

function closeTaskHistoryDetailModal() {
    const modal = document.getElementById('taskHistoryDetailModal');
    if (!modal) {
        return;
    }
    modal.classList.add('hidden');
    modal.style.display = 'none';
}

function prettyMetadata(metadataJson) {
    if (!metadataJson) {
        return '';
    }
    try {
        return JSON.stringify(JSON.parse(metadataJson), null, 2);
    } catch (e) {
        return metadataJson;
    }
}

function normalizeTaskStatus(status) {
    const normalized = (status || '').toUpperCase();
    if (normalized === 'SUCCESS') {
        return { label: I18N.taskStatusSuccess, color: 'var(--success-color)' };
    }
    if (normalized === 'FAILED') {
        return { label: I18N.taskStatusFailed, color: 'var(--danger-color)' };
    }
    if (normalized === 'SKIPPED') {
        return { label: I18N.taskStatusSkipped, color: 'var(--warning-color)' };
    }
    return { label: status || '-', color: 'var(--text-secondary)' };
}

function buildTaskSummaryBadge(item) {
    const status = (item.status || '').toUpperCase();
    if (status === 'SUCCESS' || item.success === true) {
        return {
            label: I18N.taskDetailSummarySuccess,
            color: 'var(--success-color)',
            background: 'rgba(40, 167, 69, 0.15)',
            borderColor: 'rgba(40, 167, 69, 0.45)'
        };
    }
    if (status === 'FAILED' || item.success === false) {
        return {
            label: I18N.taskDetailSummaryFailed,
            color: 'var(--danger-color)',
            background: 'rgba(220, 53, 69, 0.15)',
            borderColor: 'rgba(220, 53, 69, 0.45)'
        };
    }
    if (status === 'SKIPPED') {
        return {
            label: I18N.taskDetailSummarySkipped,
            color: 'var(--warning-color)',
            background: 'rgba(255, 193, 7, 0.15)',
            borderColor: 'rgba(255, 193, 7, 0.45)'
        };
    }
    return {
        label: I18N.taskDetailSummaryUnknown,
        color: 'var(--text-secondary)',
        background: 'rgba(108, 117, 125, 0.2)',
        borderColor: 'var(--border-color)'
    };
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatDateForQuery(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return year + '-' + month + '-' + day;
}

function updateTaskHistorySummaryPeriodButtons() {
    const buttons = {
        latest: document.getElementById('taskSummaryPeriodLatest'),
        range7: document.getElementById('taskSummaryPeriod7'),
        range30: document.getElementById('taskSummaryPeriod30'),
        range90: document.getElementById('taskSummaryPeriod90'),
        custom: document.getElementById('taskSummaryPeriodCustom')
    };

    let activeKey = 'range7';
    if (taskHistorySummaryMode === 'latest') {
        activeKey = 'latest';
    } else if (taskHistorySummaryMode === 'custom') {
        activeKey = 'custom';
    } else if (taskHistorySummaryDays === 30) {
        activeKey = 'range30';
    } else if (taskHistorySummaryDays === 90) {
        activeKey = 'range90';
    }

    Object.entries(buttons).forEach(function(entry) {
        const key = entry[0];
        const btn = entry[1];
        if (!btn) {
            return;
        }

        const selected = key === activeKey;
        btn.style.backgroundColor = selected ? 'var(--primary-color)' : 'transparent';
        btn.style.color = selected ? '#fff' : 'var(--text-primary)';
    });

    const customRange = document.getElementById('taskHistorySummaryCustomRange');
    if (customRange) {
        customRange.style.display = taskHistorySummaryMode === 'custom' ? 'block' : 'none';
    }
}

function setTaskHistorySummaryMode(mode, days) {
    if (mode === 'latest') {
        taskHistorySummaryMode = 'latest';
    } else if (mode === 'custom') {
        taskHistorySummaryMode = 'custom';
    } else {
        taskHistorySummaryMode = 'range';
        taskHistorySummaryDays = days === 30 || days === 90 ? days : 7;
    }

    updateTaskHistorySummaryPeriodButtons();

    if (taskHistorySummaryMode === 'custom') {
        return;
    }

    loadTaskHistorySummary();
}

function applyCustomTaskHistorySummaryRange() {
    const startDate = document.getElementById('taskSummaryCustomStart')?.value || '';
    const endDate = document.getElementById('taskSummaryCustomEnd')?.value || '';

    if (!startDate || !endDate) {
        alert(I18N.taskSummaryCustomRangeRequired || '사용자 지정 기간은 시작일/종료일을 모두 입력해야 합니다.');
        return;
    }

    if (new Date(startDate) > new Date(endDate)) {
        alert(I18N.taskSummaryCustomRangeInvalid || '시작일은 종료일보다 늦을 수 없습니다.');
        return;
    }

    taskHistorySummaryMode = 'custom';
    updateTaskHistorySummaryPeriodButtons();
    loadTaskHistorySummary();
}

async function loadTaskHistorySummary() {
    const summaryDiv = document.getElementById('taskHistorySummaryContent');
    const rangeEl = document.getElementById('taskHistorySummaryRange');
    if (!summaryDiv) {
        return;
    }

    let rangeText = I18N.taskSummaryPeriod7d;
    let startDate = null;
    let endDate = null;

    if (taskHistorySummaryMode === 'latest') {
        rangeText = I18N.taskSummaryPeriodLatest || '최신 목록';
    } else if (taskHistorySummaryMode === 'custom') {
        const startInput = document.getElementById('taskSummaryCustomStart')?.value || '';
        const endInput = document.getElementById('taskSummaryCustomEnd')?.value || '';

        if (!startInput || !endInput) {
            summaryDiv.innerHTML = '<div class="info-item full-width"><span class="value">' + escapeHtml(I18N.taskSummaryCustomRangeRequired || '사용자 지정 기간은 시작일/종료일을 모두 입력해야 합니다.') + '</span></div>';
            if (rangeEl) {
                rangeEl.textContent = I18N.taskSummaryRangeLabel + ': ' + (I18N.taskSummaryPeriodCustom || '사용자 지정');
            }
            return;
        }

        startDate = new Date(startInput);
        endDate = new Date(endInput);
        rangeText = formatDateForQuery(startDate) + ' ~ ' + formatDateForQuery(endDate);
    } else {
        rangeText = taskHistorySummaryDays === 30
            ? I18N.taskSummaryPeriod30d
            : taskHistorySummaryDays === 90
                ? I18N.taskSummaryPeriod90d
                : I18N.taskSummaryPeriod7d;
        endDate = new Date();
        startDate = new Date(endDate);
        startDate.setDate(startDate.getDate() - taskHistorySummaryDays + 1);
    }

    if (rangeEl) {
        rangeEl.textContent = I18N.taskSummaryRangeLabel + ': ' + rangeText;
    }

    summaryDiv.innerHTML = '<div class="info-item full-width"><span class="value">' + I18N.testing + '</span></div>';

    try {
        const params = new URLSearchParams();
        params.set('limit', '200');
        params.set('sortBy', 'createdAt');
        params.set('sortDir', 'desc');
        if (startDate && endDate) {
            params.set('startDate', formatDateForQuery(startDate));
            params.set('endDate', formatDateForQuery(endDate));
        }

        const response = await fetch('/api/task-executions/history?' + params.toString());
        const data = await response.json();

        if (!data.success || !Array.isArray(data.data) || data.data.length === 0) {
            summaryDiv.innerHTML =
                '<div class="info-item full-width">'
                + '<span class="label">' + I18N.taskHistoryTitle + '</span>'
                + '<span class="value">' + I18N.taskSummaryNoData + '</span>'
                + '</div>';
            return;
        }

        const rows = data.data;
        let success = 0;
        let failed = 0;
        let skipped = 0;

        rows.forEach(function(item) {
            const status = (item.status || '').toUpperCase();
            if (status === 'SUCCESS') {
                success += 1;
            } else if (status === 'FAILED') {
                failed += 1;
            } else if (status === 'SKIPPED') {
                skipped += 1;
            }
        });

        const latest = rows[0];
        const latestAt = latest.completedAt || latest.createdAt || null;

        summaryDiv.innerHTML = [
            '<div class="info-item">',
            '<span class="label">' + I18N.taskSummaryRecentRuns + '</span>',
            '<span class="value">' + rows.length + '</span>',
            '</div>',
            '<div class="info-item">',
            '<span class="label">' + I18N.taskSummarySuccess + '</span>',
            '<span class="value" style="color: var(--success-color);">' + success + '</span>',
            '</div>',
            '<div class="info-item">',
            '<span class="label">' + I18N.taskSummaryFailed + '</span>',
            '<span class="value" style="color: var(--danger-color);">' + failed + '</span>',
            '</div>',
            '<div class="info-item">',
            '<span class="label">' + I18N.taskSummarySkipped + '</span>',
            '<span class="value" style="color: var(--warning-color);">' + skipped + '</span>',
            '</div>',
            '<div class="info-item full-width">',
            '<span class="label">' + I18N.taskSummaryLatestRunAt + '</span>',
            '<span class="value">' + (latestAt ? formatTimestamp(latestAt) : '-') + '</span>',
            '</div>'
        ].join('');
    } catch (error) {
        summaryDiv.innerHTML = '<div class="info-item full-width"><span class="value" style="color: var(--danger-color);">Error: ' + escapeHtml(error.message) + '</span></div>';
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Muse Agent Dashboard loaded');

    // Auto-load startup test results
    loadStartupTestResult();

    // Auto-load alive status
    refreshAliveStatus();

    // Auto-load task history summary
    updateTaskHistorySummaryPeriodButtons();
    loadTaskHistorySummary();
});

// ============================================
// System Panel — Phase 1 (인프라)
// 우측 슬라이드 패널 토글. Phase 2 에서 기존 시스템 카드들이
// 패널 본문으로 이주하면 그대로 활용된다.
// ============================================

function openSystemPanel() {
    const panel = document.getElementById('museSystemPanel');
    const overlay = document.getElementById('museSystemPanelOverlay');
    if (panel) {
        panel.classList.add('open');
        panel.setAttribute('aria-hidden', 'false');
    }
    if (overlay) {
        overlay.classList.add('open');
        overlay.setAttribute('aria-hidden', 'false');
    }
    // 본문 스크롤 잠금 — 패널 내부 스크롤 충돌 방지
    document.body.style.overflow = 'hidden';
}

function closeSystemPanel() {
    const panel = document.getElementById('museSystemPanel');
    const overlay = document.getElementById('museSystemPanelOverlay');
    if (panel) {
        panel.classList.remove('open');
        panel.setAttribute('aria-hidden', 'true');
    }
    if (overlay) {
        overlay.classList.remove('open');
        overlay.setAttribute('aria-hidden', 'true');
    }
    document.body.style.overflow = '';
}

function toggleSystemPanel() {
    const panel = document.getElementById('museSystemPanel');
    if (!panel) return;
    if (panel.classList.contains('open')) {
        closeSystemPanel();
    } else {
        openSystemPanel();
    }
}

// 오버레이 클릭 + ESC 키로 닫기
document.addEventListener('DOMContentLoaded', function() {
    const overlay = document.getElementById('museSystemPanelOverlay');
    if (overlay) {
        overlay.addEventListener('click', closeSystemPanel);
    }
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const panel = document.getElementById('museSystemPanel');
            if (panel && panel.classList.contains('open')) {
                closeSystemPanel();
            }
        }
    });
});

// ============================================
// Footer Echo Server 상태 갱신 — Phase 5a
// 메인 화면 초기 렌더 시 점은 회색 (확인 중). 테스트 결과 수신 시 색·라벨 업데이트.
// ============================================

function updateFooterEchoStatus(connected) {
    const dot = document.getElementById('footerEchoDot');
    const text = document.getElementById('footerEchoStatusText');
    const status = document.querySelector('.muse-footer-status');
    if (!dot || !text) return;

    // I18N 키는 메인 화면에 항상 한 줄로 노출되는 짧은 라벨
    const I = window.MUSE_FOOTER_I18N || {};
    if (connected === true) {
        dot.classList.add('ok');
        dot.classList.remove('error');
        text.textContent = I.echoOk || 'Echo Server 정상';
        if (status) status.classList.remove('is-error');
    } else if (connected === false) {
        dot.classList.add('error');
        dot.classList.remove('ok');
        text.textContent = I.echoError || 'Echo Server 연결 실패';
        if (status) status.classList.add('is-error');
    } else {
        // unknown / 확인 중
        dot.classList.remove('ok', 'error');
        text.textContent = I.echoChecking || 'Echo Server 확인 중…';
    }
}

// ============================================
// 더보기 메뉴 (⋯) — 헤더 우측의 가이드/도움말/테마/언어 묶음
// 시스템 · 설정(⚙️) 은 단독 노출, 나머지는 이 토글 안으로 들어감.
// ============================================

function toggleMoreMenu(e) {
    if (e) { e.preventDefault(); e.stopPropagation(); }
    const dd = document.getElementById('moreMenuDropdown');
    if (!dd) return;
    const isOpen = dd.style.display !== 'none' && dd.style.display !== '';
    dd.style.display = isOpen ? 'none' : 'flex';
}

function closeMoreMenu() {
    const dd = document.getElementById('moreMenuDropdown');
    if (dd) dd.style.display = 'none';
}

// 외부 영역 클릭 시 닫기
document.addEventListener('click', function(e) {
    if (!e.target.closest('.muse-more-menu')) {
        closeMoreMenu();
    }
});
// ESC 로도 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        const dd = document.getElementById('moreMenuDropdown');
        if (dd && dd.style.display === 'flex') closeMoreMenu();
    }
});

// ============================================
// Echo Note Compose Modal — Phase 3
// 메인 hero 의 "+ 새로 보관하기" 버튼 → 모달 → POST /api/echo-note-messages
// 성공 시 페이지 새로고침으로 보관함 갱신.
// ============================================

function openComposeModal() {
    const overlay = document.getElementById('composeModalOverlay');
    if (!overlay) return;
    overlay.classList.add('open');
    overlay.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    // 첫 입력으로 포커스
    setTimeout(function() {
        const recipient = document.getElementById('composeRecipient');
        if (recipient) recipient.focus();
    }, 80);
}

function closeComposeModal() {
    const overlay = document.getElementById('composeModalOverlay');
    if (!overlay) return;
    overlay.classList.remove('open');
    overlay.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    // 입력 비우기 (다음 열림 시 깨끗)
    const recipient = document.getElementById('composeRecipient');
    const message = document.getElementById('composeMessage');
    if (recipient) recipient.value = '';
    if (message) message.value = '';
}

async function submitNewMessage() {
    const recipient = document.getElementById('composeRecipient');
    const message = document.getElementById('composeMessage');
    if (!recipient || !message) return;

    const recipientEmail = recipient.value.trim();
    const originalMessage = message.value.trim();

    if (!recipientEmail || !originalMessage) {
        // 폼 native validation 활용
        if (!recipientEmail) recipient.reportValidity();
        else message.reportValidity();
        return;
    }

    try {
        const response = await fetch('/api/echo-note-messages', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                recipientEmail: recipientEmail,
                originalMessage: originalMessage,
                locale: (window.MUSE_LOCALE || document.documentElement.lang || 'ko')
            })
        });
        if (!response.ok) {
            const text = await response.text();
            alert('보관 실패: ' + text);
            return;
        }
        closeComposeModal();
        // 가장 단순한 갱신 — 페이지 새로고침으로 hero 의 카운트·메시지 카드 재렌더
        window.location.reload();
    } catch (e) {
        alert('보관 실패: ' + e.message);
    }
}

// 모달 오버레이 클릭 + ESC 키로 닫기
document.addEventListener('DOMContentLoaded', function() {
    const overlay = document.getElementById('composeModalOverlay');
    if (overlay) {
        overlay.addEventListener('click', function(e) {
            // 모달 본체가 아니라 오버레이 자체를 클릭한 경우만
            if (e.target === overlay) closeComposeModal();
        });
    }
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const m = document.getElementById('composeModalOverlay');
            if (m && m.classList.contains('open')) closeComposeModal();
        }
    });
});

// ============================================
// 셋업 미완 자동 처리 — Phase 2
// 디바이스 미등록 상태일 때:
//   1. ⚙️ 버튼에 노란 점등 (.has-warning)
//   2. 시스템 패널 내부 device-mgmt 카드 자동 펼침
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    // Phase 2 + 5c: data-device-registered="false" 일 때 자동 펼침 + ⚙️ 노란 점등.
    // 이전 구현은 라벨 텍스트 ("미등록") 매칭이라 i18n 환경에서 깨졌음. 이제 서버가
    // 명시적으로 boolean 을 data-attribute 로 내려보내므로 locale-독립 robust 검출.
    const deviceCard = document.querySelector('[data-card-id="device-mgmt"]');
    if (!deviceCard) return;

    const isUnregistered = deviceCard.getAttribute('data-device-registered') === 'false';
    if (isUnregistered) {
        // 미등록 상태 — 자동 펼침
        deviceCard.classList.remove('collapsed');
        // localStorage 의 collapsedCards 에서도 제거
        const collapsed = JSON.parse(localStorage.getItem('collapsedCards') || '[]');
        const idx = collapsed.indexOf('device-mgmt');
        if (idx > -1) {
            collapsed.splice(idx, 1);
            localStorage.setItem('collapsedCards', JSON.stringify(collapsed));
        }
        // ⚙️ 버튼에 노란 점등
        const sysBtn = document.getElementById('btnSystemPanel');
        if (sysBtn) sysBtn.classList.add('has-warning');
    }
});
