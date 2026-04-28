// Cleanup Path Management JavaScript

/**
 * Add a new cleanup path
 */
async function addPath() {
    const btn = document.getElementById('btnAdd');
    const pathInput = document.getElementById('newPath');
    const descInput = document.getElementById('newDescription');
    const path = pathInput.value.trim();
    const description = descInput.value.trim();

    if (!path) {
        showResult(false, I18N.noPath);
        return;
    }

    setButtonLoading(btn, true);

    try {
        const response = await fetch('/api/cleanup-paths', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path: path, description: description || null })
        });

        const data = await response.json();

        if (data.success) {
            showResult(true, I18N.pathAdded);
            pathInput.value = '';
            descInput.value = '';
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showResult(false, data.message || I18N.noPath);
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    } finally {
        setButtonLoading(btn, false);
    }
}

/**
 * Delete a cleanup path
 */
async function deletePath(id) {
    if (!confirm(I18N.confirmDelete)) {
        return;
    }

    try {
        const response = await fetch('/api/cleanup-paths/' + id, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (data.success) {
            showResult(true, I18N.pathDeleted);
            setTimeout(() => window.location.reload(), 800);
        } else {
            showResult(false, data.message || I18N.noPath);
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    }
}

/**
 * Toggle cleanup path enabled/disabled
 */
async function togglePath(id) {
    try {
        const response = await fetch('/api/cleanup-paths/' + id + '/toggle', {
            method: 'POST'
        });

        const data = await response.json();

        if (data.success) {
            window.location.reload();
        } else {
            showResult(false, data.message || I18N.noPath);
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    }
}

/**
 * Verify all enabled cleanup paths
 */
async function verifyPaths() {
    const btn = document.getElementById('btnVerify');
    const resultDiv = document.getElementById('verifyResult');

    setButtonLoading(btn, true);
    resultDiv.classList.remove('hidden');

    try {
        const response = await fetch('/api/cleanup-paths/verify', {
            method: 'POST'
        });

        const data = await response.json();

        if (data.success && data.data) {
            const v = data.data;
            let html = '<div class="info-grid">';

            if (v.verifications && v.verifications.length > 0) {
                html += '<table class="path-table"><thead><tr><th>Path</th><th>' + I18N.yes + '</th><th>R</th><th>W</th><th>Type</th></tr></thead><tbody>';
                v.verifications.forEach(p => {
                    html += '<tr>';
                    html += '<td class="path-text">' + escapeHtml(p.path) + '</td>';
                    html += '<td>' + statusBadge(p.exists) + '</td>';
                    html += '<td>' + statusBadge(p.readable) + '</td>';
                    html += '<td>' + statusBadge(p.writable) + '</td>';
                    html += '<td><span class="badge ' + (p.directory ? 'badge-dir' : 'badge-file') + '">' + (p.directory ? 'DIR' : 'FILE') + '</span></td>';
                    html += '</tr>';
                });
                html += '</tbody></table>';
            } else {
                html += '<p style="color: var(--text-secondary);">-</p>';
            }

            html += '</div>';
            resultDiv.innerHTML = html;
        } else {
            resultDiv.innerHTML = '<p style="color: var(--danger-color);">' + (data.message || '-') + '</p>';
        }
    } catch (error) {
        resultDiv.innerHTML = '<p style="color: var(--danger-color);">Error: ' + escapeHtml(error.message) + '</p>';
    } finally {
        setButtonLoading(btn, false);
    }
}

/**
 * Show result message
 */
function showResult(success, message) {
    const section = document.getElementById('resultSection');
    const content = document.getElementById('resultContent');

    section.classList.remove('hidden');
    content.innerHTML = '<p style="color: ' + (success ? 'var(--success-color)' : 'var(--danger-color)') + ';">' + escapeHtml(message) + '</p>';

    section.scrollIntoView({ behavior: 'smooth' });

    if (success) {
        setTimeout(() => section.classList.add('hidden'), 3000);
    }
}

/**
 * Set button loading state
 */
function setButtonLoading(btn, loading) {
    if (loading) {
        btn.disabled = true;
        btn.dataset.originalText = btn.textContent;
        btn.textContent = I18N.processing;
    } else {
        btn.disabled = false;
        btn.textContent = btn.dataset.originalText || btn.textContent;
    }
}

/**
 * Create status badge HTML
 */
function statusBadge(value) {
    return value
        ? `<span class="badge badge-enabled">${I18N.yes}</span>`
        : `<span class="badge badge-disabled">${I18N.no}</span>`;
}

/**
 * Escape HTML entities
 */
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

async function loadTrashSettings() {
    const retentionInput = document.getElementById('trashRetentionDays');
    const rootPathInput = document.getElementById('trashRootPath');
    const purgeBatchInput = document.getElementById('trashPurgeBatchSize');

    if (!retentionInput || !rootPathInput || !purgeBatchInput) {
        return;
    }

    try {
        const response = await fetch('/api/settings/cleanup-trash');
        const data = await response.json();
        if (data.success && data.data) {
            retentionInput.value = data.data.retentionDays ?? 30;
            rootPathInput.value = data.data.rootPath ?? '';
            purgeBatchInput.value = data.data.purgeBatchSize ?? 200;
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    }
}

async function saveTrashSettings() {
    const btn = document.getElementById('btnSaveTrashSettings');
    const retentionInput = document.getElementById('trashRetentionDays');
    const rootPathInput = document.getElementById('trashRootPath');
    const purgeBatchInput = document.getElementById('trashPurgeBatchSize');

    if (!retentionInput || !rootPathInput || !purgeBatchInput) {
        return;
    }

    const payload = {
        retentionDays: Number(retentionInput.value || 0),
        rootPath: String(rootPathInput.value || '').trim(),
        purgeBatchSize: Number(purgeBatchInput.value || 0)
    };

    setButtonLoading(btn, true);
    try {
        const response = await fetch('/api/settings/cleanup-trash', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (data.success) {
            showResult(true, I18N.trashSettingsSaved);
            await loadTrashStats();
        } else {
            showResult(false, data.message || I18N.noPath);
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    } finally {
        setButtonLoading(btn, false);
    }
}

async function loadTrashStats() {
    const statsDiv = document.getElementById('trashStats');
    if (!statsDiv) {
        return;
    }

    try {
        const response = await fetch('/api/trash/stats');
        const data = await response.json();
        if (!data.success || !data.data) {
            statsDiv.innerHTML = '<p style="color: var(--danger-color);">' + escapeHtml(data.message || '-') + '</p>';
            return;
        }

        const s = data.data;
        let html = '<h4 style="margin-bottom: 8px; color: var(--text-primary);">' + escapeHtml(I18N.trashStatsTitle) + '</h4>';
        html += '<table class="path-table"><tbody>';
        html += '<tr><td>' + escapeHtml(I18N.trashMovedCount) + '</td><td>' + (s.movedCount ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashDeletedCount) + '</td><td>' + (s.deletedCount ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashRestoredCount) + '</td><td>' + (s.restoredCount ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashFailedCount) + '</td><td>' + (s.deleteFailedCount ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashPurgeCandidates) + '</td><td>' + (s.purgeCandidateCount ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashRetentionDays) + '</td><td>' + (s.retentionDays ?? '-') + '</td></tr>';
        html += '<tr><td>' + escapeHtml(I18N.trashRootPath) + '</td><td class="path-text">' + escapeHtml(s.trashRootPath ?? '-') + '</td></tr>';
        html += '</tbody></table>';
        statsDiv.innerHTML = html;
    } catch (error) {
        statsDiv.innerHTML = '<p style="color: var(--danger-color);">Error: ' + escapeHtml(error.message) + '</p>';
    }
}

async function purgeTrashNow() {
    const btn = document.getElementById('btnPurgeTrashNow');
    setButtonLoading(btn, true);
    try {
        const response = await fetch('/api/trash/purge-now', {
            method: 'POST'
        });
        const data = await response.json();
        if (data.success) {
            showResult(true, I18N.trashPurgeDone);
            await loadTrashStats();
            await loadTrashItems();
        } else {
            showResult(false, data.message || '-');
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    } finally {
        setButtonLoading(btn, false);
    }
}

function formatDateTime(value) {
    if (!value) {
        return '-';
    }
    try {
        const date = new Date(value);
        return date.toLocaleString();
    } catch (e) {
        return String(value);
    }
}

async function loadTrashItems() {
    const container = document.getElementById('trashItemsContainer');
    const statusFilter = document.getElementById('trashStatusFilter');
    if (!container) {
        return;
    }

    const params = new URLSearchParams();
    params.set('limit', '100');
    const status = statusFilter ? statusFilter.value : '';
    if (status) {
        params.set('status', status);
    }

    try {
        const response = await fetch('/api/trash/items?' + params.toString());
        const data = await response.json();
        if (!data.success) {
            container.innerHTML = '<p style="color: var(--danger-color);">' + escapeHtml(data.message || '-') + '</p>';
            return;
        }

        const rows = Array.isArray(data.data) ? data.data : [];
        if (rows.length === 0) {
            container.innerHTML = '<p style="color: var(--text-secondary);">' + escapeHtml(I18N.trashNoItems) + '</p>';
            return;
        }

        let html = '<h4 style="margin-bottom: 8px; color: var(--text-primary);">' + escapeHtml(I18N.trashItemsTitle) + '</h4>';
        html += '<table class="path-table"><thead><tr>';
        html += '<th>' + escapeHtml(I18N.trashColOriginalPath) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColTrashPath) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColStatus) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColMovedAt) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColExpireAt) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColError) + '</th>';
        html += '<th>' + escapeHtml(I18N.trashColAction) + '</th>';
        html += '</tr></thead><tbody>';

        rows.forEach(function(row) {
            const canRestore = row.status === 'MOVED' || row.status === 'DELETE_FAILED';
            html += '<tr>';
            html += '<td class="path-text">' + escapeHtml(row.originalPath || '-') + '</td>';
            html += '<td class="path-text">' + escapeHtml(row.trashPath || '-') + '</td>';
            html += '<td>' + escapeHtml(row.status || '-') + '</td>';
            html += '<td>' + escapeHtml(formatDateTime(row.movedAt)) + '</td>';
            html += '<td>' + escapeHtml(formatDateTime(row.expireAt)) + '</td>';
            html += '<td>' + escapeHtml(row.lastError || '-') + '</td>';
            html += '<td>';
            if (canRestore) {
                html += '<button type="button" class="btn btn-sm btn-secondary" onclick="restoreTrashItem(' + Number(row.id) + ')">' + escapeHtml(I18N.trashRestore) + '</button>';
            } else {
                html += '-';
            }
            html += '</td>';
            html += '</tr>';
        });

        html += '</tbody></table>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = '<p style="color: var(--danger-color);">Error: ' + escapeHtml(error.message) + '</p>';
    }
}

async function restoreTrashItem(id) {
    if (!confirm(I18N.trashRestoreConfirm)) {
        return;
    }

    try {
        const response = await fetch('/api/trash/' + id + '/restore', {
            method: 'POST'
        });
        const data = await response.json();
        if (data.success) {
            showResult(true, I18N.trashRestoreDone);
            await loadTrashStats();
            await loadTrashItems();
        } else {
            showResult(false, data.message || '-');
        }
    } catch (error) {
        showResult(false, 'Error: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    loadTrashSettings();
    loadTrashStats();
    loadTrashItems();
});
