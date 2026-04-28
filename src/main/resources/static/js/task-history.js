let currentTaskHistoryRows = [];

function setButtonLoading(btn, loading) {
    if (!btn) {
        return;
    }
    if (loading) {
        btn.disabled = true;
        btn.dataset.originalText = btn.innerHTML;
        btn.innerHTML = '<span class="loading"></span> ' + I18N.testing;
    } else {
        btn.disabled = false;
        btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
    }
}

function formatTimestamp(timestamp) {
    try {
        const date = new Date(timestamp);
        return date.toLocaleString();
    } catch (e) {
        return timestamp;
    }
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\"/g, '&quot;')
        .replace(/'/g, '&#39;');
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

function translateErrorMessageForUser(technicalMessage) {
    if (!technicalMessage) {
        return {
            userMessage: '작업 중 문제가 발생했습니다.',
            technicalDetail: null
        };
    }
    
    const msg = String(technicalMessage).toLowerCase();
    
    // 경로 관련 오류
    if (msg.includes('does not exist') || msg.includes('not found')) {
        return {
            userMessage: '정리 경로가 더 이상 존재하지 않습니다.',
            technicalDetail: technicalMessage
        };
    }
    
    // 권한 관련 오류
    if (msg.includes('permission denied') || msg.includes('access denied') || msg.includes('denied')) {
        return {
            userMessage: '파일/폴더에 접근할 수 있는 권한이 없습니다.',
            technicalDetail: technicalMessage
        };
    }
    
    // 파일 사용 중 오류
    if (msg.includes('in use') || msg.includes('locked') || msg.includes('cannot delete')) {
        return {
            userMessage: '일부 파일이 다른 프로그램에 의해 사용 중입니다.',
            technicalDetail: technicalMessage
        };
    }
    
    // 처리 중 오류
    if (msg.includes('failed') || msg.includes('error') || msg.includes('exception')) {
        return {
            userMessage: '작업 처리 중 오류가 발생했습니다.',
            technicalDetail: technicalMessage
        };
    }
    
    // 연결/네트워크 오류
    if (msg.includes('connection') || msg.includes('timeout') || msg.includes('refused')) {
        return {
            userMessage: '외부 서비스 연결 중 문제가 발생했습니다.',
            technicalDetail: technicalMessage
        };
    }
    
    // 위의 패턴에 맞지 않는 경우
    return {
        userMessage: '작업 실행 중 예기치 않은 문제가 발생했습니다.',
        technicalDetail: technicalMessage
    };
}

async function loadTaskExecutionHistory() {
    const btn = document.getElementById('btnTaskHistory');
    setButtonLoading(btn, true);

    const historyDiv = document.getElementById('taskHistoryContent');
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
            historyDiv.innerHTML = '<p class="status-message" style="color: var(--danger-color);">' + escapeHtml(data.message || 'Failed to load history') + '</p>';
            return;
        }

        const rows = Array.isArray(data.data) ? data.data : [];
        currentTaskHistoryRows = rows;

        if (rows.length === 0) {
            historyDiv.innerHTML = '<p class="status-message">' + I18N.noTaskHistory + '</p>';
            return;
        }

        let html = '<div style="background-color: rgba(0,0,0,0.2); border-radius: 8px; padding: 15px;">';
        html += '<h4 style="margin-bottom: 10px; color: #4a90d9;">' + I18N.taskHistoryTitle + '</h4>';
        html += '<div style="overflow-x:auto;">';
        html += '<table style="width: 100%; border-collapse: collapse;">';
        html += '<thead><tr style="color: var(--text-secondary); font-size: 0.85rem; text-transform: uppercase;">';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColTask + '</th>';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColStatus + '</th>';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColRunAt + '</th>';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColTarget + '</th>';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColSuccess + '</th>';
        html += '<th style="text-align: left; padding: 8px;">' + I18N.taskColFailure + '</th>';
        html += '</tr></thead><tbody>';

        rows.forEach(function(item, index) {
            const statusInfo = normalizeTaskStatus(item.status);
            const taskLabel = item.taskName || item.taskKey || '-';
            html += '<tr class="task-history-row" data-row-index="' + index + '" style="border-top: 1px solid var(--border-color); cursor: pointer;">';
            html += '<td style="padding: 8px; color: var(--text-primary);">' + escapeHtml(taskLabel) + '</td>';
            html += '<td style="padding: 8px; color: ' + statusInfo.color + '; font-weight: 600;">' + escapeHtml(statusInfo.label) + '</td>';
            html += '<td style="padding: 8px; color: var(--text-primary);">' + (item.completedAt ? formatTimestamp(item.completedAt) : '-') + '</td>';
            html += '<td style="padding: 8px; color: var(--text-primary);">' + (item.targetCount != null ? item.targetCount : '-') + '</td>';
            html += '<td style="padding: 8px; color: var(--text-primary);">' + (item.successCount != null ? item.successCount : '-') + '</td>';
            html += '<td style="padding: 8px; color: var(--text-primary);">' + (item.failureCount != null ? item.failureCount : '-') + '</td>';
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
        historyDiv.innerHTML = '<p class="status-message" style="color: var(--danger-color);">Error: ' + escapeHtml(error.message) + '</p>';
    } finally {
        setButtonLoading(btn, false);
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

function applyTaskHistoryPreset(preset) {
    const groupEl = document.getElementById('taskHistoryTaskGroup');
    const keyEl = document.getElementById('taskHistoryTaskKey');
    const statusEl = document.getElementById('taskHistoryStatus');

    if (!groupEl || !keyEl || !statusEl) {
        return;
    }

    groupEl.value = 'SCHEDULED_TASKS';
    statusEl.value = '';

    if (preset === 'allScheduled') {
        keyEl.value = '';
    } else if (preset === 'alivePoll') {
        keyEl.value = 'ALIVE_CHECK_POLL';
    } else if (preset === 'trashMove') {
        keyEl.value = 'FILE_TRASH_MOVE';
    } else if (preset === 'trashPurge') {
        keyEl.value = 'TRASH_PURGE';
    } else if (preset === 'trashRestore') {
        groupEl.value = 'MANUAL_TASKS';
        keyEl.value = 'TRASH_RESTORE';
    }

    loadTaskExecutionHistory();
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
    const isFailed = item.success === false || (item.status || '').toUpperCase() === 'FAILED';
    
    let html = '<div style="display: grid; gap: 12px;">';
    
    // 기본 정보 섹션
    html += '<div style="background: rgba(74, 144, 217, 0.08); border-left: 3px solid var(--primary-color); padding: 12px; border-radius: 6px;">';
    html += '<h4 style="margin: 0 0 8px; color: var(--primary-color); font-size: 0.9rem;">기본 정보</h4>';
    html += '<table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">';
    html += buildDetailRow(I18N.taskDetailExecutionId, item.executionId);
    html += buildDetailRow(I18N.taskDetailTaskGroup, item.taskGroup);
    html += buildDetailRow(I18N.taskDetailTaskKey, item.taskKey);
    html += buildDetailRow(I18N.taskDetailTaskName, item.taskName);
    html += buildDetailRow(I18N.taskDetailStatus, item.status);
    html += '</table>';
    html += '</div>';
    
    // 실행 결과 섹션
    html += '<div style="background: rgba(108, 117, 125, 0.08); border-left: 3px solid var(--border-color); padding: 12px; border-radius: 6px;">';
    html += '<h4 style="margin: 0 0 8px; color: var(--text-secondary); font-size: 0.9rem;">실행 결과</h4>';
    html += '<table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">';
    html += buildDetailRow(I18N.taskDetailTarget, item.targetCount != null ? item.targetCount : '-');
    html += buildDetailRow(I18N.taskDetailSuccess, item.successCount != null ? item.successCount : '-');
    html += buildDetailRow(I18N.taskDetailFailure, item.failureCount != null ? item.failureCount : '-');
    html += '</table>';
    html += '</div>';
    
    // 시간 정보 섹션
    html += '<div style="background: rgba(108, 117, 125, 0.08); border-left: 3px solid var(--border-color); padding: 12px; border-radius: 6px;">';
    html += '<h4 style="margin: 0 0 8px; color: var(--text-secondary); font-size: 0.9rem;">시간 정보</h4>';
    html += '<table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">';
    html += buildDetailRow(I18N.taskDetailStartedAt, item.startedAt ? formatTimestamp(item.startedAt) : '-');
    html += buildDetailRow(I18N.taskDetailCompletedAt, item.completedAt ? formatTimestamp(item.completedAt) : '-');
    html += buildDetailRow(I18N.taskDetailCreatedAt, item.createdAt ? formatTimestamp(item.createdAt) : '-');
    html += '</table>';
    html += '</div>';
    
    // 실패 사유 섹션 (실패 시에만 표시, 사용자 친화적 메시지)
    if (isFailed && item.errorMessage) {
        const errorInfo = translateErrorMessageForUser(item.errorMessage);
        html += '<div style="background: rgba(220, 53, 69, 0.15); border-left: 3px solid var(--danger-color); padding: 12px; border-radius: 6px; border: 1px solid rgba(220, 53, 69, 0.3);">';
        html += '<h4 style="margin: 0 0 10px; color: var(--danger-color); font-size: 0.9rem;">⚠ 실패 사유</h4>';
        html += '<p style="margin: 0 0 8px; color: var(--danger-color); font-size: 0.9rem; line-height: 1.5;">' + escapeHtml(errorInfo.userMessage) + '</p>';
        if (errorInfo.technicalDetail) {
            html += '<details style="margin-top: 8px;">';
            html += '<summary style="cursor: pointer; color: var(--text-secondary); font-size: 0.8rem; padding: 4px 0;">기술 정보 (개발자용)</summary>';
            html += '<pre style="margin: 4px 0 0; white-space: pre-wrap; word-break: break-word; background: rgba(0, 0, 0, 0.2); padding: 8px; border-radius: 4px; color: var(--text-secondary); font-size: 0.75rem; line-height: 1.3;">' + escapeHtml(errorInfo.technicalDetail) + '</pre>';
            html += '</details>';
        }
        html += '</div>';
    } else if (item.errorMessage) {
        const errorInfo = translateErrorMessageForUser(item.errorMessage);
        html += '<div style="background: rgba(108, 117, 125, 0.08); border-left: 3px solid var(--border-color); padding: 12px; border-radius: 6px;">';
        html += '<h4 style="margin: 0 0 8px; color: var(--text-secondary); font-size: 0.9rem;">알림</h4>';
        html += '<p style="margin: 0; color: var(--text-primary); font-size: 0.9rem;">' + escapeHtml(errorInfo.userMessage) + '</p>';
        html += '</div>';
    }
    
    // 메타데이터 섹션
    if (metadata) {
        html += '<div style="background: rgba(108, 117, 125, 0.08); border-left: 3px solid var(--border-color); padding: 12px; border-radius: 6px;">';
        html += '<h4 style="margin: 0 0 8px; color: var(--text-secondary); font-size: 0.9rem;">메타데이터</h4>';
        html += '<pre style="margin: 0; white-space: pre-wrap; word-break: break-all; background: rgba(0, 0, 0, 0.3); padding: 8px; border-radius: 4px; color: var(--text-primary); font-size: 0.8rem; line-height: 1.3;">' + escapeHtml(metadata) + '</pre>';
        html += '</div>';
    }
    
    html += '</div>';
    body.innerHTML = html;
    modal.classList.remove('hidden');
    modal.style.display = 'flex';

    modal.onclick = function(event) {
        if (event.target === modal) {
            closeTaskHistoryDetailModal();
        }
    };
}

function buildDetailRow(label, value) {
    return '<tr style="border-bottom: 1px solid var(--border-color);">' +
        '<td style="padding: 8px; font-weight: 500; color: var(--text-secondary); width: 35%;">' + escapeHtml(label) + ':</td>' +
        '<td style="padding: 8px; color: var(--text-primary);">' + escapeHtml(value || '-') + '</td>' +
        '</tr>';
}

function closeTaskHistoryDetailModal() {
    const modal = document.getElementById('taskHistoryDetailModal');
    if (!modal) {
        return;
    }
    modal.classList.add('hidden');
    modal.style.display = 'none';
}

document.addEventListener('DOMContentLoaded', function() {
    loadTaskExecutionHistory();
});
