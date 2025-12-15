// DatabaseBackup.js

document.addEventListener('DOMContentLoaded', () => {
	// --- 要素取得 ---
	const backupButton = document.querySelector('button[data-bs-target="#backupModal"]');
	
	// HTMLに追加した隠しフィールドを取得
	const startYearInput = document.getElementById('startYear');
	const startMonthInput = document.getElementById('startMonth');
	const startDayInput = document.getElementById('startDay');
	
	// 修正: select 要素を取得
	const endYearSelect = document.getElementById('endYear');
	const endMonthSelect = document.getElementById('endMonth');
	const endDaySelect = document.getElementById('endDay');

	const displayStartYearSpan = document.getElementById('displayStartYear');
	const displayStartMonthSpan = document.getElementById('displayStartMonth');
	const displayStartDaySpan = document.getElementById('displayStartDay');
	
	// モーダル用
	const backupRangeText = document.getElementById('backupRangeText');
	const hiddenStartYear = document.getElementById('hiddenStartYear');
	const hiddenStartMonth = document.getElementById('hiddenStartMonth');
	const hiddenStartDay = document.getElementById('hiddenStartDay');
	const hiddenEndYear = document.getElementById('hiddenEndYear');
	const hiddenEndMonth = document.getElementById('hiddenEndMonth');
	const hiddenEndDay = document.getElementById('hiddenEndDay');

	// 階層データ(JSON)の取得
	const dateHierarchyJsonInput = document.getElementById('dateHierarchyJson');
	let dateHierarchy = {};
	if (dateHierarchyJsonInput && dateHierarchyJsonInput.value) {
		try {
			dateHierarchy = JSON.parse(dateHierarchyJsonInput.value);
		} catch (e) {
			console.error("JSON parse error:", e);
		}
	}

	// 履歴テーブルの tbody
	const historyTableBody = document.querySelector('.table-responsive tbody.custom-text--short');

	// --- ★ [UserManagement.js から移植] テーブル行調整 ---
	const adjustTableRows = (minRows) => {
		if (!historyTableBody) return 0;
		
		const allRows = historyTableBody.querySelectorAll("tr");
		const emptyRows = Array.from(allRows).filter(row => {
			return !row.cells[0] || row.cells[0].textContent.trim() === "" || row.cells[0].textContent.trim() === "&nbsp;";
		});
		emptyRows.forEach(row => row.remove());

		const currentRowCount = historyTableBody.querySelectorAll("tr").length;
		
		if (currentRowCount < minRows) {
			const cellsInRow = 4;
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = historyTableBody.insertRow();
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
		}
		return currentRowCount;
	};

	const dataRowCount = adjustTableRows(16);
	const isHistoryListEmpty = dataRowCount === 0;

	// --- プルダウン生成ロジック ---

	// 共通: Selectの選択肢をクリア
	const clearSelect = (selectElement) => {
		selectElement.innerHTML = '';
	};

	// 共通: Optionを追加
	const addOption = (selectElement, value, text) => {
		const option = document.createElement('option');
		option.value = value;
		option.textContent = text;
		selectElement.appendChild(option);
	};

	// 1. 年の生成
	const populateEndYear = () => {
		clearSelect(endYearSelect);
		const years = Object.keys(dateHierarchy).sort((a, b) => b - a); // 降順
		years.forEach(year => {
			addOption(endYearSelect, year, year);
		});
		// デフォルトで先頭(最新)を選択
		if (years.length > 0) {
			endYearSelect.value = years[0];
			populateEndMonth(years[0]); // 月を更新
		} else {
			// データがない場合の処理（必要なら）
			addOption(endYearSelect, "", "----");
			clearSelect(endMonthSelect);
			clearSelect(endDaySelect);
		}
	};

	// 2. 月の生成
	const populateEndMonth = (year) => {
		clearSelect(endMonthSelect);
		if (!dateHierarchy[year]) return;

		const months = Object.keys(dateHierarchy[year]).sort((a, b) => b - a); // 降順
		months.forEach(month => {
			// 2桁表示
			addOption(endMonthSelect, month, String(month).padStart(2, '0'));
		});

		if (months.length > 0) {
			endMonthSelect.value = months[0];
			populateEndDay(year, months[0]); // 日を更新
		}
	};

	// 3. 日の生成
	const populateEndDay = (year, month) => {
		clearSelect(endDaySelect);
		if (!dateHierarchy[year] || !dateHierarchy[year][month]) return;

		const days = dateHierarchy[year][month]; // 既にJava側でソート済と仮定、またはここでソート
		// days は配列
		days.forEach(day => {
			addOption(endDaySelect, day, String(day).padStart(2, '0'));
		});

		if (days.length > 0) {
			endDaySelect.value = days[0];
		}
		// 日付が確定したのでボタン状態更新
		toggleBackupButton();
	};

	// --- イベントリスナー設定 ---
	if (endYearSelect) {
		endYearSelect.addEventListener('change', (e) => {
			populateEndMonth(e.target.value);
		});
	}
	if (endMonthSelect) {
		endMonthSelect.addEventListener('change', (e) => {
			populateEndDay(endYearSelect.value, e.target.value);
		});
	}
	if (endDaySelect) {
		endDaySelect.addEventListener('change', () => {
			toggleBackupButton();
		});
	}

	// --- バックアップボタンの有効/無効を切り替える関数 ---
	const toggleBackupButton = () => {
		const startYear = startYearInput ? startYearInput.value : null;
		const startMonth = startMonthInput ? startMonthInput.value : null;
		const startDay = startDayInput ? startDayInput.value : null;
		
		const endYear = endYearSelect ? endYearSelect.value : null;
		const endMonth = endMonthSelect ? endMonthSelect.value : null;
		const endDay = endDaySelect ? endDaySelect.value : null;

		if (!isHistoryListEmpty && 
			startYear && startYear !== '-1' &&
			startMonth && startMonth !== '-1' &&
			startDay && startDay !== '-1' &&
			endYear && endMonth && endDay) {
			
			backupButton.disabled = false;
			backupButton.classList.remove("custom-btn-common--notselectable");
		} else {
			backupButton.disabled = true;
			backupButton.classList.add("custom-btn-common--notselectable");
		}
	};

	// --- 初期化実行 ---
	// JSONデータがあればプルダウンを初期構築
	if (Object.keys(dateHierarchy).length > 0) {
		populateEndYear();
	} else {
		toggleBackupButton();
	}


	// --- バックアップボタンクリック時の処理 ---
	if (backupButton) {
		backupButton.addEventListener('click', (event) => {
			if (backupButton.disabled) {
				 event.stopPropagation();
				 return;
			}

			// 値を取得
			const startYear = startYearInput.value;
			const startMonth = startMonthInput.value;
			const startDay = startDayInput.value;
			
			const displayStartYear = displayStartYearSpan ? displayStartYearSpan.textContent : '----';
			const displayStartMonth = displayStartMonthSpan ? displayStartMonthSpan.textContent : '--';
			const displayStartDay = displayStartDaySpan ? displayStartDaySpan.textContent : '--';
			
			// Selectから値を取得
			const endYear = endYearSelect.value;
			const endMonth = endMonthSelect.value;
			const endDay = endDaySelect.value;

			// バリデーション
			if (!startYear || startYear === '-1' || 
				!startMonth || startMonth === '-1' || 
				!startDay || startDay === '-1' || 
				!endYear || !endMonth || !endDay) {
				alert('期間が正しく設定されていません。');
				event.stopPropagation();
				return;
			}

			// 月・日をフォーマット
			const formattedEndMonth = String(endMonth).padStart(2, '0');
			const formattedEndDay = String(endDay).padStart(2, '0');

			// モーダル内のテキストを更新
			if (backupRangeText) {
				backupRangeText.innerHTML = `${displayStartYear}/${displayStartMonth}/${displayStartDay} から<br>${endYear}/${formattedEndMonth}/${formattedEndDay} までの`;
			}

			// モーダル内の隠しフィールドに値を設定
			if (hiddenStartYear) hiddenStartYear.value = startYear;
			if (hiddenStartMonth) hiddenStartMonth.value = startMonth;
			if (hiddenStartDay) hiddenStartDay.value = startDay;
			if (hiddenEndYear) hiddenEndYear.value = endYear;
			if (hiddenEndMonth) hiddenEndMonth.value = endMonth;
			if (hiddenEndDay) hiddenEndDay.value = endDay;
		});
	}
});