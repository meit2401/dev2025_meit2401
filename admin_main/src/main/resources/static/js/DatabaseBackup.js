// DatabaseBackup.js

document.addEventListener('DOMContentLoaded', () => {
	// --- 要素取得 ---
	const backupButton = document.querySelector('button[data-bs-target="#backupModal"]');
	const startYearInput = document.getElementById('startYear');
	const startMonthInput = document.getElementById('startMonth');
	const displayStartYearSpan = document.getElementById('displayStartYear');
	const displayStartMonthSpan = document.getElementById('displayStartMonth');
	
	// 終了年月の要素 (HTMLでは hidden input)
	const endYearInput = document.getElementById('endYear');
	const endMonthInput = document.getElementById('endMonth');

	// モーダル用
	const backupRangeText = document.getElementById('backupRangeText');
	const hiddenStartYear = document.getElementById('hiddenStartYear');
	const hiddenStartMonth = document.getElementById('hiddenStartMonth');
	const hiddenEndYear = document.getElementById('hiddenEndYear');
	const hiddenEndMonth = document.getElementById('hiddenEndMonth');

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
		const startYear = startYearInput.value;
		const startMonth = startMonthInput.value;
		const endYear = endYearInput.value;
		const endMonth = endMonthInput.value;

		// 導出した isHistoryListEmpty を使用
		if (!isHistoryListEmpty && 
			startYear && startYear !== '-1' &&
			startMonth && startMonth !== '-1' &&
			endYear && endMonth) { // endYear/Month も値があることを確認
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
	// (注: 元のHTMLでは endYear/endMonth は hidden input のため、
	// 'change' イベントは実質発生しないが、元のロジックをそのまま移植)
	if(endYearInput) endYearInput.addEventListener('change', toggleBackupButton);
	if(endMonthInput) endMonthInput.addEventListener('change', toggleBackupButton);


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
			const displayStartYear = displayStartYearSpan ? displayStartYearSpan.textContent : '----';
			const displayStartMonth = displayStartMonthSpan ? displayStartMonthSpan.textContent : '--';
			const endYear = endYearInput.value;
			const endMonth = endMonthInput.value;

			// バリデーション
			if (!startYear || startYear === '-1' || !startMonth || startMonth === '-1' || !endYear || !endMonth) {
				alert('期間が正しく設定されていません。');
				event.stopPropagation();
				return;
			}

			// 終了月をフォーマット
			const formattedEndMonth = String(endMonth).padStart(2, '0');

			// モーダル内のテキストを更新
			if (backupRangeText) {
				backupRangeText.innerHTML = `${displayStartYear}/${displayStartMonth} から<br>${endYear}/${formattedEndMonth} までの`;
			}

			// モーダル内の隠しフィールドに値を設定
			if (hiddenStartYear) hiddenStartYear.value = startYear;
			if (hiddenStartMonth) hiddenStartMonth.value = startMonth;
			if (hiddenEndYear) hiddenEndYear.value = endYear;
			if (hiddenEndMonth) hiddenEndMonth.value = endMonth;
		});
	}
});