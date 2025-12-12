// DatabaseBackup.js

document.addEventListener('DOMContentLoaded', () => {
	// --- 要素取得 ---
	const backupButton = document.querySelector('button[data-bs-target="#backupModal"]');
	
	// HTMLに追加した隠しフィールドを取得
	const startYearInput = document.getElementById('startYear');
	const startMonthInput = document.getElementById('startMonth');
	const startDayInput = document.getElementById('startDay'); // 追加
	const endYearInput = document.getElementById('endYear');
	const endMonthInput = document.getElementById('endMonth');
	const endDayInput = document.getElementById('endDay'); // 追加

	const displayStartYearSpan = document.getElementById('displayStartYear');
	const displayStartMonthSpan = document.getElementById('displayStartMonth');
	const displayStartDaySpan = document.getElementById('displayStartDay'); // 追加
	
	// モーダル用
	const backupRangeText = document.getElementById('backupRangeText');
	const hiddenStartYear = document.getElementById('hiddenStartYear');
	const hiddenStartMonth = document.getElementById('hiddenStartMonth');
	const hiddenStartDay = document.getElementById('hiddenStartDay'); // 追加
	const hiddenEndYear = document.getElementById('hiddenEndYear');
	const hiddenEndMonth = document.getElementById('hiddenEndMonth');
	const hiddenEndDay = document.getElementById('hiddenEndDay'); // 追加

	// 履歴テーブルの tbody
	const historyTableBody = document.querySelector('.table-responsive tbody.custom-text--short');

	// --- ★ [UserManagement.js から移植] テーブル行調整 ---
	/**
	 * テーブルの行数が最低行数に満たない場合、空行を追加する
	 * @param {number} minRows - 最低限表示したい行数
	 * @returns {number} 調整前のデータ行数
	 */
	const adjustTableRows = (minRows) => {
		if (!historyTableBody) return 0; // データ行数 0
		
		// 1. 既存の空行 (cells[0] が空の行) をすべて削除 (UserManagement.js との互換性)
		const allRows = historyTableBody.querySelectorAll("tr");
		const emptyRows = Array.from(allRows).filter(row => {
			return !row.cells[0] || row.cells[0].textContent.trim() === "" || row.cells[0].textContent.trim() === "&nbsp;";
		});
		emptyRows.forEach(row => row.remove());

		// 2. 現在のデータ行の数をカウント
		const currentRowCount = historyTableBody.querySelectorAll("tr").length;
		
		// 3. 足りない分の空行を追加
		if (currentRowCount < minRows) {
			const cellsInRow = 4; // ★ 修正: 履歴テーブルは4列 (日付, 時間, 氏名, 作業内容)
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = historyTableBody.insertRow(); // 末尾に追加
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
		}
		
		// 4. 調整前のデータ行数を返す
		return currentRowCount;
	};

	// --- ★ [UserManagement.js スタイル] 実行 ---
	// 1. 行を調整し、調整前のデータ行数を取得
	const dataRowCount = adjustTableRows(16); // 16行に設定

	// 2. 調整前のデータ行数を使って「リストが空か」を判断
	const isHistoryListEmpty = dataRowCount === 0;


	// --- バックアップボタンの有効/無効を切り替える関数 ---
	const toggleBackupButton = () => {
		const startYear = startYearInput ? startYearInput.value : null;
		const startMonth = startMonthInput ? startMonthInput.value : null;
		const startDay = startDayInput ? startDayInput.value : null; // 追加
		const endYear = endYearInput ? endYearInput.value : null;
		const endMonth = endMonthInput ? endMonthInput.value : null;
		const endDay = endDayInput ? endDayInput.value : null; // 追加

		// 導出した isHistoryListEmpty を使用
		if (!isHistoryListEmpty && 
			startYear && startYear !== '-1' &&
			startMonth && startMonth !== '-1' &&
			startDay && startDay !== '-1' && // 追加
			endYear && endMonth && endDay) { // endYear/Month/Day も値があることを確認
			backupButton.disabled = false; // 有効化
			// ★ 修正: UserManagement.js に倣い、クラスをトグル
			backupButton.classList.remove("custom-btn-common--notselectable");
		} else {
			backupButton.disabled = true; // 無効化
			// ★ 修正: UserManagement.js に倣い、クラスをトグル
			backupButton.classList.add("custom-btn-common--notselectable");
		}
	};

	// --- 初期状態でボタンの状態をチェック ---
	toggleBackupButton();

	// --- 終了年月ドロップダウンの変更イベントにのみ関数を紐付け ---
	if(endYearInput) endYearInput.addEventListener('change', toggleBackupButton);
	if(endMonthInput) endMonthInput.addEventListener('change', toggleBackupButton);
	if(endDayInput) endDayInput.addEventListener('change', toggleBackupButton); // 追加


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
			const startDay = startDayInput.value; // 追加
			
			const displayStartYear = displayStartYearSpan ? displayStartYearSpan.textContent : '----';
			const displayStartMonth = displayStartMonthSpan ? displayStartMonthSpan.textContent : '--';
			const displayStartDay = displayStartDaySpan ? displayStartDaySpan.textContent : '--'; // 追加
			
			const endYear = endYearInput.value;
			const endMonth = endMonthInput.value;
			const endDay = endDayInput.value; // 追加

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
			const formattedEndDay = String(endDay).padStart(2, '0'); // 追加

			// モーダル内のテキストを更新
			if (backupRangeText) {
				// YYYY/MM/DD 形式に変更
				backupRangeText.innerHTML = `${displayStartYear}/${displayStartMonth}/${displayStartDay} から<br>${endYear}/${formattedEndMonth}/${formattedEndDay} までの`;
			}

			// モーダル内の隠しフィールドに値を設定
			if (hiddenStartYear) hiddenStartYear.value = startYear;
			if (hiddenStartMonth) hiddenStartMonth.value = startMonth;
			if (hiddenStartDay) hiddenStartDay.value = startDay; // 追加
			if (hiddenEndYear) hiddenEndYear.value = endYear;
			if (hiddenEndMonth) hiddenEndMonth.value = endMonth;
			if (hiddenEndDay) hiddenEndDay.value = endDay; // 追加
		});
	}
});