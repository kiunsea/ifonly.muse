// Device Registration JavaScript

/**
 * Submit device registration
 */
async function submitRegistration() {
    const btn = document.getElementById('btnRegister');
    const deviceName = document.getElementById('deviceName').value.trim();

    if (!deviceName) {
        alert(I18N.noDeviceName);
        return;
    }

    setButtonLoading(btn, true);

    try {
        const response = await fetch('/api/device/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ deviceName: deviceName })
        });

        const data = await response.json();

        if (data.success) {
            showSuccess(data);
        } else {
            showError(data.message || I18N.troubleshoot);
        }
    } catch (error) {
        showError('Error: ' + error.message);
    } finally {
        setButtonLoading(btn, false);
    }
}

/**
 * Submit device re-registration
 */
async function submitReregistration() {
    const btn = document.getElementById('btnReregister');
    const deviceName = document.getElementById('newDeviceName').value.trim();

    if (!deviceName) {
        alert(I18N.noDeviceName);
        return;
    }

    // Confirmation dialog
    if (!confirm(I18N.confirmReregister)) {
        return;
    }

    setButtonLoading(btn, true);

    try {
        const response = await fetch('/api/device/reregister', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ deviceName: deviceName })
        });

        const data = await response.json();

        if (data.success) {
            showSuccess(data);
        } else {
            showError(data.message || I18N.troubleshoot);
        }
    } catch (error) {
        showError('Error: ' + error.message);
    } finally {
        setButtonLoading(btn, false);
    }
}

/**
 * Submit device unregistration
 */
async function submitUnregistration() {
    if (!confirm(I18N.confirmUnregister)) {
        return;
    }

    try {
        const response = await fetch('/api/device/unregister', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({})
        });

        const data = await response.json();
        if (data.success) {
            const resultSection = document.getElementById('resultSection');
            const resultContent = document.getElementById('resultContent');

            let html = '<div class="result-success">';
            html += `<h3 style="color: var(--success-color); margin-bottom: 15px;">${I18N.unregisterSuccessTitle}</h3>`;
            html += `<p style="margin-bottom: 0;">${data.message || I18N.unregisterSuccessMessage}</p>`;
            html += '</div>';

            resultContent.innerHTML = html;
            document.querySelectorAll('.card').forEach(card => {
                if (card.id !== 'resultSection') {
                    card.classList.add('hidden');
                }
            });
            resultSection.classList.remove('hidden');
            return;
        }

        showError(data.message || I18N.troubleshoot);
    } catch (error) {
        showError('Error: ' + error.message);
    }
}

/**
 * Show success result
 */
function showSuccess(data) {
    const resultSection = document.getElementById('resultSection');
    const resultContent = document.getElementById('resultContent');

    let html = '<div class="result-success">';
    html += `<h3 style="color: var(--success-color); margin-bottom: 15px;">${I18N.successTitle}</h3>`;

    if (data.data) {
        html += '<div class="result-item">';
        html += `<span class="label">${I18N.deviceIdLabel}</span>`;
        html += '<span class="value">' + data.data.id + '</span>';
        html += '</div>';

        html += '<div class="result-item">';
        html += `<span class="label">${I18N.deviceNameLabel}</span>`;
        html += '<span class="value">' + data.data.deviceName + '</span>';
        html += '</div>';

        html += '<div class="result-item">';
        html += `<span class="label">${I18N.identifierLabel}</span>`;
        html += '<span class="value">' + data.data.deviceIdentifier + '</span>';
        html += '</div>';

        html += '<div class="result-item">';
        html += `<span class="label">${I18N.osLabel}</span>`;
        html += '<span class="value">' + (data.data.osInfo || 'N/A') + '</span>';
        html += '</div>';

        if (data.data.registeredAt) {
            html += '<div class="result-item">';
            html += `<span class="label">${I18N.registeredAtLabel}</span>`;
            html += '<span class="value">' + data.data.registeredAt + '</span>';
            html += '</div>';
        }
    }

    html += '</div>';

    resultContent.innerHTML = html;

    // Hide form sections and show result
    document.querySelectorAll('.card').forEach(card => {
        if (card.id !== 'resultSection') {
            card.classList.add('hidden');
        }
    });
    resultSection.classList.remove('hidden');
}

/**
 * Show error result
 */
function showError(message) {
    const resultSection = document.getElementById('resultSection');
    const resultContent = document.getElementById('resultContent');

    let html = '<div class="result-error">';
    html += `<h3 style="color: var(--danger-color); margin-bottom: 15px;">${I18N.troubleshoot}</h3>`;
    html += `<p style="margin-bottom: 20px; color: var(--danger-color);">${message}</p>`;
    html += '<ul style="margin-left: 20px; margin-top: 10px;">';
    html += `<li>${I18N.checkEcho}</li>`;
    html += `<li>${I18N.checkClient}</li>`;
    html += `<li>${I18N.checkNetwork}</li>`;
    html += '</ul>';
    html += '</div>';

    resultContent.innerHTML = html;

    // Hide form sections and show result
    document.querySelectorAll('.card').forEach(card => {
        if (card.id !== 'resultSection') {
            card.classList.add('hidden');
        }
    });
    resultSection.classList.remove('hidden');
}

/**
 * Show re-register form
 */
function showReregisterForm() {
    if (!confirm(I18N.confirmReregister)) {
        return;
    }

    // Hide current registration status
    document.querySelectorAll('.card').forEach(card => {
        if (card.id !== 'reregisterSection') {
            card.classList.add('hidden');
        }
    });

    // Show re-register form
    document.getElementById('reregisterSection').classList.remove('hidden');
}

/**
 * Cancel re-registration
 */
function cancelReregistration() {
    // Reload page to show original state
    window.location.reload();
}

/**
 * Cancel registration and go back
 */
function cancelRegistration() {
    window.location.href = '/';
}

/**
 * Set button loading state
 */
function setButtonLoading(btn, loading) {
    if (loading) {
        btn.disabled = true;
        btn.dataset.originalText = btn.innerHTML;
        btn.innerHTML = '<span class="loading"></span> ' + I18N.processing;
    } else {
        btn.disabled = false;
        btn.innerHTML = btn.dataset.originalText;
    }
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

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Device Registration page loaded');
});
