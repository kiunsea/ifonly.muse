// Echo Note 보관함 — Phase 1
// 사용자 PC 에 보관되는 echo-note 메시지 CRUD + 프리뷰/마침 처리.
// Phase 2 에서 echo-server LLM 통신, Phase 3 에서 자동 발송 스케줄러 도입 예정.

const HN_API = '/api/echo-note-messages';
const HN_I18N = window.ECHO_NOTE_I18N || {};
const HN_LOCALE = window.ECHO_NOTE_LOCALE || 'ko';

async function hnCreate() {
    const recipient = document.getElementById('hnRecipientEmail').value.trim();
    const message = document.getElementById('hnOriginalMessage').value.trim();
    if (!recipient || !message) {
        hnShowResult(false, HN_I18N.errorPrefix + 'recipient + message 필수');
        return;
    }
    const btn = document.getElementById('hnBtnCreate');
    hnSetLoading(btn, true);
    try {
        const res = await hnPost(HN_API, { recipientEmail: recipient, originalMessage: message, locale: HN_LOCALE });
        if (res.success) {
            hnShowResult(true, HN_I18N.created);
            document.getElementById('hnRecipientEmail').value = '';
            document.getElementById('hnOriginalMessage').value = '';
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    } finally {
        hnSetLoading(btn, false);
    }
}

async function hnGeneratePreview(id) {
    try {
        const res = await hnPost(`${HN_API}/${id}/preview`, {});
        if (res.success) {
            hnShowResult(true, HN_I18N.previewGenerated);
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

async function hnFinalize(id) {
    if (!confirm(HN_I18N.confirmFinalize)) return;
    try {
        const res = await hnPost(`${HN_API}/${id}/finalize`, {});
        if (res.success) {
            hnShowResult(true, HN_I18N.finalized);
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

async function hnRevert(id) {
    if (!confirm(HN_I18N.confirmRevert)) return;
    try {
        const res = await hnPost(`${HN_API}/${id}/revert`, {});
        if (res.success) {
            hnShowResult(true, HN_I18N.reverted);
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

async function hnSendViaEcho(id) {
    if (!confirm(HN_I18N.confirmSendViaEcho)) return;
    try {
        const res = await hnPost(`${HN_API}/${id}/send-via-echo`, {});
        if (res.success) {
            hnShowResult(true, HN_I18N.sentViaEcho);
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

async function hnDelete(id) {
    if (!confirm(HN_I18N.confirmDelete)) return;
    try {
        const res = await fetch(`${HN_API}/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            hnShowResult(true, HN_I18N.deleted);
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (data.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

async function hnOpenEdit(id) {
    try {
        const res = await fetch(`${HN_API}/${id}`).then(r => r.json());
        if (!res.success) {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
            return;
        }
        const m = res.data;
        document.getElementById('hnEditId').value = m.id;
        document.getElementById('hnEditRecipient').value = m.recipientEmail || '';
        document.getElementById('hnEditOriginal').value = m.originalMessage || '';
        document.getElementById('hnEditPreview').value = m.aiGeneratedMessage || '';
        document.getElementById('hnEditBackdrop').classList.add('open');
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

function hnCloseEdit() {
    document.getElementById('hnEditBackdrop').classList.remove('open');
}

async function hnSaveEdit() {
    const id = document.getElementById('hnEditId').value;
    const payload = {
        recipientEmail: document.getElementById('hnEditRecipient').value.trim(),
        originalMessage: document.getElementById('hnEditOriginal').value.trim(),
        aiGeneratedMessage: document.getElementById('hnEditPreview').value
    };
    try {
        const res = await fetch(`${HN_API}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(r => r.json());
        if (res.success) {
            hnShowResult(true, HN_I18N.saved);
            hnCloseEdit();
            setTimeout(() => window.location.reload(), 700);
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    }
}

// 헬퍼

async function hnPost(url, body) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body || {})
    });
    return await res.json();
}

function hnShowResult(success, message) {
    const section = document.getElementById('hnResultSection');
    const content = document.getElementById('hnResultContent');
    if (!section || !content) return;
    section.classList.remove('hidden');
    content.textContent = message;
    content.style.color = success ? 'var(--success-color)' : 'var(--danger-color)';
    setTimeout(() => section.classList.add('hidden'), 3500);
}

function hnSetLoading(btn, loading) {
    if (!btn) return;
    btn.disabled = !!loading;
    btn.style.opacity = loading ? '0.6' : '';
}

// 모달 backdrop 클릭으로 닫기
document.addEventListener('DOMContentLoaded', () => {
    const backdrop = document.getElementById('hnEditBackdrop');
    if (backdrop) {
        backdrop.addEventListener('click', e => {
            if (e.target === backdrop) hnCloseEdit();
        });
    }
});
