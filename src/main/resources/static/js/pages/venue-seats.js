// 공연장·좌석 관리(admin/venue-seats.html) 스크립트입니다.
// 1) 상단 탭("공연장 구조" / "좌석 보기") 전환
// 2) "좌석 보기" 탭의 공연장(홀) 목록 클릭 -> AdminVenueController#hallSeatMap 호출 -> 조회 전용 배치도 모달 렌더링

document.addEventListener('DOMContentLoaded', function () {
    initTabs();
    initSeatViewList();
});

function initTabs() {
    var tabs = document.querySelectorAll('.page-tab');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            var targetId = tab.dataset.tabTarget;

            document.querySelectorAll('.page-tab').forEach(function (t) { t.classList.remove('is-active'); });
            tab.classList.add('is-active');

            document.querySelectorAll('.page-tab-panel').forEach(function (panel) {
                panel.classList.toggle('is-active', panel.id === targetId);
            });
        });
    });
}

function initSeatViewList() {
    document.querySelectorAll('.js-seat-view-item').forEach(function (item) {
        item.addEventListener('click', function () {
            var hallId = item.dataset.hallId;
            var hallName = item.dataset.hallName;
            openSeatViewModal(hallId, hallName);
        });
    });
}

function openSeatViewModal(hallId, hallName) {
    document.getElementById('seatViewTitle').textContent = hallName + ' · 좌석 배치도';
    document.getElementById('seatViewSections').innerHTML = '';
    document.getElementById('seatViewEmpty').style.display = 'none';

    fetch('/admin/venues/halls/' + hallId + '/seats/map')
        .then(function (res) { return res.json(); })
        .then(function (seats) {
            renderSeatViewMap(seats);
            document.getElementById('seatViewModal').style.display = 'flex';
        })
        .catch(function (err) {
            alert('좌석 정보를 불러오지 못했습니다.');
            console.error(err);
        });
}

function closeSeatViewModal() {
    document.getElementById('seatViewModal').style.display = 'none';
}

function renderSeatViewMap(seats) {
    var container = document.getElementById('seatViewSections');
    var emptyEl = document.getElementById('seatViewEmpty');
    container.innerHTML = '';

    if (!seats || seats.length === 0) {
        emptyEl.style.display = 'block';
        return;
    }
    emptyEl.style.display = 'none';

    // 구역(section) -> 등급(grade) 순으로 그룹핑
    var bySection = {};
    seats.forEach(function (s) {
        var key = s.sectionName || '구역 미지정';
        if (!bySection[key]) bySection[key] = [];
        bySection[key].push(s);
    });

    Object.keys(bySection).forEach(function (sectionName) {
        var byGrade = {};
        bySection[sectionName].forEach(function (s) {
            if (!byGrade[s.gradeId]) byGrade[s.gradeId] = { name: s.gradeName, color: s.displayColor, seats: [] };
            byGrade[s.gradeId].seats.push(s);
        });

        Object.keys(byGrade).forEach(function (gradeId) {
            var group = byGrade[gradeId];

            var titleRow = document.createElement('div');
            titleRow.style.cssText = 'display:flex;align-items:center;justify-content:center;gap:8px;margin:14px 0 8px;';

            var swatch = document.createElement('span');
            swatch.style.cssText = 'display:inline-block;width:10px;height:10px;border-radius:3px;background:' + group.color + ';';

            var title = document.createElement('span');
            title.style.cssText = 'font-weight:600;font-size:13px;';
            title.textContent = sectionName + ' · ' + group.name + ' (' + group.seats.length + '석)';

            titleRow.appendChild(swatch);
            titleRow.appendChild(title);
            container.appendChild(titleRow);

            var byRow = {};
            group.seats.forEach(function (s) {
                var rowKey = s.rowLabel || '';
                if (!byRow[rowKey]) byRow[rowKey] = [];
                byRow[rowKey].push(s);
            });

            Object.keys(byRow)
                .sort(function (a, b) { return a.localeCompare(b, undefined, { numeric: true }); })
                .forEach(function (rowKey) {
                    var rowDiv = document.createElement('div');
                    rowDiv.style.cssText = 'display:flex;justify-content:center;gap:4px;margin-bottom:4px;';
                    byRow[rowKey].forEach(function (s) {
                        var dot = document.createElement('span');
                        dot.title = sectionName + ' ' + s.rowLabel + '열 ' + s.seatNumber + '번 (' + s.gradeName + ')';
                        dot.style.cssText = 'display:inline-block;width:14px;height:14px;border-radius:3px;background:' + s.displayColor + ';';
                        rowDiv.appendChild(dot);
                    });
                    container.appendChild(rowDiv);
                });
        });
    });
}