// 더보기 (⋯) 메뉴 — 헤더 우측의 테마/언어/그 외 묶음.
// index.html 외 다른 페이지들에서 공유 사용. index.html 은 동일 함수가 app.js 에 정의돼 있어
// 본 파일을 추가로 로드하지 않는다 (중복 정의는 brower 가 마지막 정의로 덮어쓰므로 functional 영향은 없으나,
// 불필요한 중복 회피).
//
// app.js 와 동일 시그니처:
//   - toggleMoreMenu(e)   : 토글
//   - closeMoreMenu()     : 닫기
//   - 외부 클릭 / ESC 로 자동 닫기

(function () {
    if (typeof window.toggleMoreMenu === 'function') {
        // 이미 정의돼 있으면 (예: index.html 의 app.js) 재정의 회피
        return;
    }

    function toggleMoreMenu(e) {
        if (e) { e.preventDefault(); e.stopPropagation(); }
        var dd = document.getElementById('moreMenuDropdown');
        if (!dd) return;
        var isOpen = dd.style.display !== 'none' && dd.style.display !== '';
        dd.style.display = isOpen ? 'none' : 'flex';
    }

    function closeMoreMenu() {
        var dd = document.getElementById('moreMenuDropdown');
        if (dd) dd.style.display = 'none';
    }

    window.toggleMoreMenu = toggleMoreMenu;
    window.closeMoreMenu = closeMoreMenu;

    // 외부 영역 클릭 시 닫기
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.muse-more-menu')) {
            closeMoreMenu();
        }
    });

    // ESC 로도 닫기
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            var dd = document.getElementById('moreMenuDropdown');
            if (dd && dd.style.display === 'flex') closeMoreMenu();
        }
    });
})();
