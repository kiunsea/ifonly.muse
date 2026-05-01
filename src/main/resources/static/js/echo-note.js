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
        if (!res.success) {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
            return;
        }
        hnShowResult(true, HN_I18N.created);
        document.getElementById('hnRecipientEmail').value = '';
        document.getElementById('hnOriginalMessage').value = '';

        // C 도입 — 자동 preview: 보관 직후 가공본 받아 인라인 카드 (정보 모드) 에 표시.
        // [이대로 보관] / [다시 시도] 는 hide, [닫기] 만 노출. [닫기] 가 reload 트리거.
        const savedId = res.data && res.data.id;
        let autoPreviewShown = false;
        if (savedId) {
            try {
                const previewRes = await hnPost(`${HN_API}/${savedId}/preview`, {});
                if (previewRes.success) {
                    const text = (previewRes.data && previewRes.data.aiGeneratedMessage) || '';
                    hnShowAutoPreviewCard(text);
                    autoPreviewShown = true;
                }
            } catch (e) {
                console.warn('auto preview failed:', e);
            }
        }
        if (!autoPreviewShown) {
            setTimeout(() => window.location.reload(), 700);
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    } finally {
        hnSetLoading(btn, false);
    }
}

// 자동 preview 카드 표시 — 정보 모드 (액션 버튼 단순화).
function hnShowAutoPreviewCard(text) {
    const card = document.getElementById('hnPreviewCard');
    const warn = document.getElementById('hnPreviewWarn');
    const warnReason = document.getElementById('hnPreviewWarnReason');
    const textArea = document.getElementById('hnPreviewText');
    textArea.value = text || '';
    // stub heuristic — saved preview 응답엔 stubFallback 플래그가 없어서 텍스트의 prefix 로 추정.
    // EchoNotePreviewGenerator.stubFallback 의 한·영·일 prefix 모두 "[stub" 로 시작.
    if (text && text.trimStart().startsWith('[stub')) {
        warn.classList.remove('hidden');
        warnReason.textContent = '';
    } else {
        warn.classList.add('hidden');
        warnReason.textContent = '';
    }
    // 정보 모드 — [이대로 보관] hide, [다시 시도] hide. [닫기] 가 reload trigger.
    card.dataset.mode = 'auto';
    const acceptBtn = card.querySelector('button[onclick="hnAcceptPreview()"]');
    const retryBtn = card.querySelector('button[onclick="hnPreviewOnly()"]');
    if (acceptBtn) acceptBtn.style.display = 'none';
    if (retryBtn) retryBtn.style.display = 'none';
    card.classList.remove('hidden');
    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// ─── B 도입 — 작성 form 의 인라인 미리보기 ───────────────────────────
//
// hnPreviewOnly: [미리보기 ✨] 버튼. DB 저장 없이 가공본만 받아 인라인 카드에 표시.
// hnAcceptPreview: 미리보기 결과를 보고 [이대로 보관] — 메시지를 DRAFT 로 저장하고 가공본도 같이 반영.
// hnDiscardPreview: 카드만 닫음 (DB 변경 없음).

async function hnPreviewOnly() {
    const message = document.getElementById('hnOriginalMessage').value.trim();
    if (!message) {
        hnShowResult(false, HN_I18N.errorPrefix + (HN_I18N.previewInlineMissing || '메시지 본문을 입력해주세요.'));
        return;
    }
    const btn = document.getElementById('hnBtnPreview');
    hnSetLoading(btn, true);
    const card = document.getElementById('hnPreviewCard');
    const warn = document.getElementById('hnPreviewWarn');
    const warnReason = document.getElementById('hnPreviewWarnReason');
    const text = document.getElementById('hnPreviewText');

    try {
        const res = await hnPost(`${HN_API}/preview-only`, { originalMessage: message, locale: HN_LOCALE });
        if (res.success) {
            text.value = res.preview || '';
            if (res.stubFallback) {
                warn.classList.remove('hidden');
                if (res.fallbackReason) {
                    warnReason.textContent = (HN_I18N.previewInlineFallbackReason || ' (사유: ') + res.fallbackReason + (HN_I18N.previewInlineFallbackReasonSuffix || ')');
                } else {
                    warnReason.textContent = '';
                }
            } else {
                warn.classList.add('hidden');
                warnReason.textContent = '';
            }
            card.classList.remove('hidden');
            card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            hnShowResult(false, HN_I18N.errorPrefix + (res.message || ''));
        }
    } catch (e) {
        hnShowResult(false, HN_I18N.errorPrefix + e.message);
    } finally {
        hnSetLoading(btn, false);
    }
}

async function hnAcceptPreview() {
    // 사용자가 미리보기 결과를 받아들이고 그대로 보관. 표준 hnCreate 흐름 그대로 호출.
    // (가공본 자체는 발송 시점에 echo 가 다시 만들거나 사용자가 보관함의 [수정] 모달에서 직접 편집 가능)
    await hnCreate();
}

function hnDiscardPreview() {
    const card = document.getElementById('hnPreviewCard');
    const isAuto = card.dataset.mode === 'auto';
    card.classList.add('hidden');
    document.getElementById('hnPreviewWarn').classList.add('hidden');
    document.getElementById('hnPreviewText').value = '';
    delete card.dataset.mode;
    // [이대로 보관] / [다시 시도] 버튼 복원 — 다음에 수동 모드로 다시 열 때 정상 표시
    const acceptBtn = card.querySelector('button[onclick="hnAcceptPreview()"]');
    const retryBtn = card.querySelector('button[onclick="hnPreviewOnly()"]');
    if (acceptBtn) acceptBtn.style.display = '';
    if (retryBtn) retryBtn.style.display = '';
    // 자동 모드였으면 reload — 보관함 갱신
    if (isAuto) {
        window.location.reload();
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

// ============================================
// Phase 4: 검색 + 상태 필터 + 정렬 (클라이언트 사이드)
// 모든 필터링·정렬은 이미 렌더된 테이블 행에 직접 적용.
// 서버 변경 없음.
// ============================================

let hnCurrentFilter = 'ALL';
let hnCurrentSort = 'created-desc';

function hnSetFilter(btn, filter) {
    hnCurrentFilter = filter;
    document.querySelectorAll('.en-chip').forEach(c => c.classList.remove('is-active'));
    if (btn) btn.classList.add('is-active');
    hnApplyFilter();
}

function hnUpdateChipCounts() {
    const rows = document.querySelectorAll('.en-table tbody tr');
    let d = 0, r = 0, s = 0;
    rows.forEach(row => {
        const st = row.getAttribute('data-status') || '';
        if (st === 'DRAFT') d++;
        else if (st === 'READY') r++;
        else if (st === 'SENT') s++;
    });
    const eD = document.getElementById('hnCountDraft');
    const eR = document.getElementById('hnCountReady');
    const eS = document.getElementById('hnCountSent');
    if (eD) eD.textContent = d;
    if (eR) eR.textContent = r;
    if (eS) eS.textContent = s;
}

function hnApplyFilter() {
    const search = document.getElementById('hnSearchInput');
    const q = (search ? search.value : '').trim().toLowerCase();
    const sortSel = document.getElementById('hnSortSelect');
    if (sortSel) hnCurrentSort = sortSel.value;

    const tbody = document.querySelector('.en-table tbody');
    if (!tbody) return;
    const rows = Array.from(tbody.querySelectorAll('tr'));

    // 1) Filter (status + search)
    let visibleCount = 0;
    rows.forEach(row => {
        const st = row.getAttribute('data-status') || '';
        const re = (row.getAttribute('data-recipient') || '').toLowerCase();
        const msg = (row.getAttribute('data-message') || '').toLowerCase();
        const matchFilter = (hnCurrentFilter === 'ALL') || (st === hnCurrentFilter);
        const matchSearch = (q === '') || re.includes(q) || msg.includes(q);
        const visible = matchFilter && matchSearch;
        row.style.display = visible ? '' : 'none';
        if (visible) visibleCount++;
    });

    // 2) Sort visible rows
    const visibleRows = rows.filter(r => r.style.display !== 'none');
    visibleRows.sort((a, b) => {
        switch (hnCurrentSort) {
            case 'created-asc':
                return (a.getAttribute('data-created') || '').localeCompare(b.getAttribute('data-created') || '');
            case 'updated-desc':
                return (b.getAttribute('data-updated') || '').localeCompare(a.getAttribute('data-updated') || '');
            case 'recipient':
                return (a.getAttribute('data-recipient') || '').localeCompare(b.getAttribute('data-recipient') || '');
            case 'created-desc':
            default:
                return (b.getAttribute('data-created') || '').localeCompare(a.getAttribute('data-created') || '');
        }
    });
    // 가장 단순한 reorder — DOM 에 다시 append (순서대로)
    visibleRows.forEach(r => tbody.appendChild(r));

    // 3) Update count display
    const countEl = document.getElementById('hnFilteredCount');
    if (countEl) {
        const suffix = (HN_I18N && HN_I18N.countSuffix) ? HN_I18N.countSuffix : '개';
        countEl.textContent = visibleCount > 0
            ? (visibleCount + suffix)
            : (HN_I18N.noResults || '조건에 맞는 메시지가 없어요.');
    }
}

function hnInitToolbar() {
    const search = document.getElementById('hnSearchInput');
    if (!search) return;  // 보관함이 비어있으면 toolbar 자체가 없음

    search.addEventListener('input', hnApplyFilter);

    // URL query params: q (검색어), status (필터), focus (search 자동 포커스)
    const qp = new URLSearchParams(window.location.search);
    const qParam = qp.get('q');
    if (qParam) search.value = qParam;
    const statusParam = qp.get('status');
    if (statusParam) {
        const upper = statusParam.toUpperCase();
        const targetChip = document.querySelector('.en-chip[data-filter="' + upper + '"]');
        if (targetChip) hnSetFilter(targetChip, upper);
    }
    if (qp.get('focus') === 'search') {
        setTimeout(() => search.focus(), 80);
    }

    hnUpdateChipCounts();
    hnApplyFilter();
}

document.addEventListener('DOMContentLoaded', hnInitToolbar);
