(function () {
    const HELP_MODE_STORAGE_KEY = 'museAgentHelpMode.v1';
    const TOOLTIP_OFFSET = 12;
    const VIEWPORT_GAP = 8;

    let helpModeEnabled = false;
    let tooltipElement;
    let titleElement;
    let descElement;
    let activeTarget;

    function ensureTooltip() {
        if (tooltipElement) {
            return;
        }

        tooltipElement = document.createElement('div');
        tooltipElement.className = 'help-tooltip hidden';
        tooltipElement.setAttribute('role', 'tooltip');

        titleElement = document.createElement('div');
        titleElement.className = 'help-tooltip-title';

        descElement = document.createElement('div');
        descElement.className = 'help-tooltip-desc';

        tooltipElement.appendChild(titleElement);
        tooltipElement.appendChild(descElement);
        document.body.appendChild(tooltipElement);
    }

    function readStoredHelpMode() {
        try {
            return localStorage.getItem(HELP_MODE_STORAGE_KEY) === 'true';
        } catch (error) {
            return false;
        }
    }

    function storeHelpMode(enabled) {
        try {
            localStorage.setItem(HELP_MODE_STORAGE_KEY, enabled ? 'true' : 'false');
        } catch (error) {
            // Ignore storage failures.
        }
    }

    function getHelpButtons() {
        return document.querySelectorAll('[data-help-toggle="true"]');
    }

    function updateHelpButtons() {
        getHelpButtons().forEach(function (button) {
            const showLabel = button.dataset.labelShow || 'Show Help';
            const hideLabel = button.dataset.labelHide || 'Hide Help';
            button.textContent = helpModeEnabled ? hideLabel : showLabel;
            button.classList.toggle('active', helpModeEnabled);
            button.setAttribute('aria-pressed', helpModeEnabled ? 'true' : 'false');
        });
    }

    function hideTooltip() {
        if (!tooltipElement) {
            return;
        }

        tooltipElement.classList.add('hidden');
        tooltipElement.removeAttribute('data-placement');
        activeTarget = null;
    }

    function applyTooltipPosition(target) {
        const rect = target.getBoundingClientRect();
        const tooltipWidth = tooltipElement.offsetWidth;
        const tooltipHeight = tooltipElement.offsetHeight;
        let placement = target.dataset.helpPlacement || 'top';
        const isNarrowViewport = window.innerWidth <= 768;
        let top;
        let left;

        // On narrow screens, avoid side placements that are likely to clip.
        if (isNarrowViewport && (placement === 'left' || placement === 'right')) {
            placement = 'bottom';
        }

        if (placement === 'bottom') {
            top = rect.bottom + TOOLTIP_OFFSET;
            if (top + tooltipHeight > window.innerHeight - VIEWPORT_GAP) {
                placement = 'top';
            }
        }

        if (placement === 'left') {
            left = rect.left - tooltipWidth - TOOLTIP_OFFSET;
            if (left < VIEWPORT_GAP) {
                placement = 'right';
            }
        }

        if (placement === 'right') {
            left = rect.right + TOOLTIP_OFFSET;
            if (left + tooltipWidth > window.innerWidth - VIEWPORT_GAP) {
                placement = 'top';
            }
        }

        if (placement === 'top') {
            top = rect.top - tooltipHeight - TOOLTIP_OFFSET;
            if (top < VIEWPORT_GAP) {
                placement = 'bottom';
            }
        }

        if (placement === 'top' || placement === 'bottom') {
            left = rect.left + (rect.width / 2) - (tooltipWidth / 2);
            left = Math.min(Math.max(left, VIEWPORT_GAP), window.innerWidth - tooltipWidth - VIEWPORT_GAP);
            top = placement === 'top'
                ? rect.top - tooltipHeight - TOOLTIP_OFFSET
                : rect.bottom + TOOLTIP_OFFSET;
        } else {
            top = rect.top + (rect.height / 2) - (tooltipHeight / 2);
            top = Math.min(Math.max(top, VIEWPORT_GAP), window.innerHeight - tooltipHeight - VIEWPORT_GAP);
            left = placement === 'left'
                ? rect.left - tooltipWidth - TOOLTIP_OFFSET
                : rect.right + TOOLTIP_OFFSET;
        }

        tooltipElement.style.top = Math.round(top) + 'px';
        tooltipElement.style.left = Math.round(left) + 'px';
        tooltipElement.setAttribute('data-placement', placement);
    }

    function showTooltip(target) {
        if (!helpModeEnabled) {
            return;
        }

        const title = target.dataset.helpTitle || '';
        const desc = target.dataset.helpDesc || '';
        if (!title && !desc) {
            return;
        }

        ensureTooltip();
        titleElement.textContent = title;
        titleElement.style.display = title ? 'block' : 'none';
        descElement.textContent = desc;
        descElement.style.display = desc ? 'block' : 'none';

        // Keep tooltip width within viewport, especially on mobile.
        tooltipElement.style.maxWidth = Math.max(220, window.innerWidth - (VIEWPORT_GAP * 2)) + 'px';
        tooltipElement.classList.remove('hidden');
        applyTooltipPosition(target);
        activeTarget = target;
    }

    function toggleTooltip(target) {
        if (activeTarget === target) {
            hideTooltip();
            return;
        }
        showTooltip(target);
    }

    function setHelpMode(enabled) {
        helpModeEnabled = enabled;
        document.body.classList.toggle('help-mode-enabled', enabled);
        updateHelpButtons();
        hideTooltip();
        storeHelpMode(enabled);
    }

    /**
     * 클릭 대상이 navigation 요소인지 — page 이동을 발생시키는 anchor 또는 button.
     * 도움말 모드가 활성화돼 있더라도 navigation 클릭은 차단하지 않고, 모드를 OFF 한 뒤
     * 브라우저가 default 동작 (페이지 이동) 을 그대로 수행하도록 한다.
     */
    function isNavigationTarget(el) {
        if (!el) return false;
        if (el.tagName === 'A') {
            const href = el.getAttribute('href');
            if (!href) return false;
            if (href.startsWith('#')) return false; // anchor link — 같은 페이지 내 이동, 도움말 의도 유지
            if (href.toLowerCase().startsWith('javascript:')) return false;
            return true;
        }
        if (el.tagName === 'BUTTON') {
            const onclick = el.getAttribute('onclick') || '';
            return /location\.href|location\.assign|window\.open|window\.location/.test(onclick);
        }
        return false;
    }

    function bindHelpTargets() {
        document.querySelectorAll('[data-help-title], [data-help-desc]').forEach(function (target) {
            if (target.dataset.helpBound === 'true') {
                return;
            }

            target.dataset.helpBound = 'true';
            target.addEventListener('mouseenter', function () { showTooltip(target); });
            target.addEventListener('mouseleave', hideTooltip);
            target.addEventListener('focus', function () { showTooltip(target); });
            target.addEventListener('blur', hideTooltip);
            target.addEventListener('click', function (event) {
                if (!helpModeEnabled) {
                    return;
                }

                // navigation 요소 (anchor with href / location.href button) 는 도움말 모드에서도 그대로 이동.
                // 모드는 자동 OFF 처리하여 다음 페이지에서 다른 nav 버튼 클릭 시 같은 함정에 빠지지 않도록.
                const navEl = event.target.closest('a[href], button[onclick]');
                if (navEl && (navEl === target || target.contains(navEl)) && isNavigationTarget(navEl)) {
                    setHelpMode(false);
                    return; // preventDefault 호출 안 함 — 브라우저가 navigation 수행
                }

                // 비-navigation 클릭은 기존 의도 유지: tooltip 표시 + page action 차단.
                event.preventDefault();
                event.stopPropagation();
                toggleTooltip(target);
            });
        });
    }

    function bindHelpButtons() {
        getHelpButtons().forEach(function (button) {
            if (button.dataset.helpToggleBound === 'true') {
                return;
            }

            button.dataset.helpToggleBound = 'true';
            button.addEventListener('click', function () {
                setHelpMode(!helpModeEnabled);
            });
        });
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            hideTooltip();
        }
    });

    document.addEventListener('click', function (event) {
        if (!helpModeEnabled || !tooltipElement || tooltipElement.classList.contains('hidden')) {
            return;
        }

        if (tooltipElement.contains(event.target)) {
            return;
        }

        if (activeTarget && activeTarget.contains(event.target)) {
            return;
        }

        hideTooltip();
    });

    window.addEventListener('scroll', hideTooltip, true);
    window.addEventListener('resize', hideTooltip);

    document.addEventListener('DOMContentLoaded', function () {
        ensureTooltip();
        bindHelpTargets();
        bindHelpButtons();
        setHelpMode(readStoredHelpMode());
    });
})();