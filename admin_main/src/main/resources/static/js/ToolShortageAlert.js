/**
 * ToolShortageAlert.html 専用スクリプト
 * 不足工具の件数とリストを1秒ごとにポーリングして動的更新します。
 */
document.addEventListener('DOMContentLoaded', function() {
	
	// HTML要素を取得
	const countElement = document.getElementById('tool-shortage-count');
	const tbodyElement = document.getElementById('tool-shortage-tbody');
	
	// HTML (mainタグ) に埋め込まれた data-* 属性からAPIのURLを取得
	const mainElement = document.querySelector('main');
	const countApiUrl = mainElement.dataset.countUrl;
	const listApiUrl = mainElement.dataset.listUrl;

	// URLが取得できない場合はエラーを出して停止
	if (!countApiUrl || !listApiUrl) {
		console.error('APIのURLがHTMLのdata属性に見つかりません。');
		if (countElement) {
			countElement.textContent = '（設定エラー）';
		}
		if (tbodyElement) {
			tbodyElement.innerHTML = '<tr><td colspan="7" class="text-center text-danger">ページ設定エラーが発生しました</td></tr>';
		}
		return;
	}

	// 定数を定義
	const minRows = 16; 
	const cellsInRow = 7; // ToolShortageAlert.html の列数

	/**
	 * テーブルの行数が最低行数に満たない場合、空行を追加する
	 * (サーバーサイドレンダリングされた行、またはAPIで挿入された行をデータ行としてカウントする)
	 */
	function adjustTableRows() {
        if (!tbodyElement) return;

		// 1. 既存の「空行」または「メッセージ行」をすべて削除
		//    (データ行（cell[0] に内容があり、colspan がない）は削除しない)
		const allRows = tbodyElement.querySelectorAll("tr");
		const emptyOrMessageRows = Array.from(allRows).filter(row => {
			// cell[0] がない
			if (!row.cells[0]) return true; 
			// cell[0] に colspan 属性がある (メッセージ行)
			if (row.cells[0].hasAttribute("colspan")) return true;
			// cell[0] の中身が空 (空行)
			const text = row.cells[0].textContent.trim();
			if (text === "" || text === "&nbsp;") return true;

			return false; // データ行
		});
		emptyOrMessageRows.forEach(row => row.remove());
        
        // 2. 現在のデータ行の数をカウント
        const currentRowCount = tbodyElement.querySelectorAll("tr").length;
        
        // 3. 足りない分の空行を追加
        if (currentRowCount < minRows) {
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = tbodyElement.insertRow(); // 末尾に追加
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
        }
	}


	// 1秒ごと (Serviceの更新頻度に合わせて) にデータをポーリング
	const pollingInterval = setInterval(updateShortageData, 1000);

	// 不足工具の件数とリストを更新する関数
	async function updateShortageData() {
		try {
			// 1. 件数を取得して更新
			const countResponse = await fetch(countApiUrl);
			if (!countResponse.ok) {
				throw new Error('Count API failed');
			}
			const count = await countResponse.json();
			
			// テキストを更新
			countElement.textContent = '発注が必要な工具が ' + count + ' 件あります';

			// 2. リストを取得してテーブルを更新
			const listResponse = await fetch(listApiUrl);
			if (!listResponse.ok) throw new Error('List API failed');
			const toolList = await listResponse.json();

			// テーブル(tbody)の中身をクリア
			tbodyElement.innerHTML = '';

			// 3. 取得したデータでテーブルを再構築
			toolList.forEach(tool => {
				const row = tbodyElement.insertRow();
				
				// セルにデータを設定 (HTMLのヘッダー順に合わせる)
				row.insertCell(0).textContent = tool.toolCategory;
				row.insertCell(1).textContent = tool.maker;
				row.insertCell(2).textContent = tool.toolName;
				row.insertCell(3).textContent = tool.toolMaterial;
				row.insertCell(4).textContent = tool.buyer;
				
				// 発注点 (右寄せ)
				const ropCell = row.insertCell(5);
				ropCell.textContent = tool.rop;
				ropCell.classList.add('text-end'); 

				// 現在在庫数 (右寄せ、赤字、太字)
				const stockCell = row.insertCell(6);
				stockCell.textContent = tool.currentStock;
				stockCell.classList.add('text-end');
				
				// 'red'の直指定から CSS変数 '--color-red' を参照するように変更
				stockCell.style.setProperty('color', 'var(--color-red)');
				stockCell.style.fontWeight = 'bold';
			});

			// 4. 関数呼び出しに変更
			adjustTableRows();

		} catch (error) {
			console.error('不足工具データの更新に失敗しました:', error);
			// エラー発生時 (例: サーバーダウン時) は表示を更新しないか、エラーメッセージを出す
			tbodyElement.innerHTML = '<tr><td colspan="7" class="text-center text-danger">データ（不足工具リスト）の取得に失敗しました</td></tr>';
			
			// エラー時も最低行数は担保する
			adjustTableRows();
			
			// 件数表示もエラーにする
			countElement.textContent = '（データ取得エラー）';
			
			// エラーが続く場合、ポーリングを停止
			clearInterval(pollingInterval);
		}
	}

    // --- 初期化処理 ---
    // ページ読み込み時に、まず空行でテーブルを埋める
	// (th:each でレンダリングされた行をデータ行として認識し、足りない分を空行で埋める)
    adjustTableRows();
});