/**
 * 分析工具ダイアログで年が選択されたら対応する月を候補として表示するjavascript
 */
/**
 * 年度が選択されたときに月のドロップダウンを更新する関数
 * (※この関数は HTML 側で定義された 'yearMonthData' グローバル変数を参照します)
 * @param {string} selectedYear - 選択された年度 (例: "2024")
 */
function updateMonths(selectedYear) {
    
    // 2. 月の <select> タグ（id="monthSelect"）を取得
    const monthSelect = document.getElementById('monthSelect');
    
    // 3. 既存の月の選択肢を (「必ず選択...」以外) すべて削除
    // (オプションの数が 1 より多い間、最後の子要素を削除し続ける)
    while (monthSelect.options.length > 1) {
        monthSelect.remove(monthSelect.options.length - 1);
    }
    
    // 4. 年度も「必ず選択...」に戻された場合
    if (!selectedYear) {
        monthSelect.disabled = true; // 月を選択不可（無効化）にする
        monthSelect.value = "";      // 月の選択状態をリセット
        return;
    }

    // 5. 選択された年度に対応する月のリストを yearMonthData (グローバル変数) から探す
    // (find を使って、ym.year が selectedYear と一致する最初の要素を探す)
    const yearData = yearMonthData.find(ym => ym.year === selectedYear);
    
    if (yearData && yearData.months) {
        // 6. 見つかった月のリスト (yearData.months) でループ
        yearData.months.forEach(month => {
            // <option> タグを新しく作成
            const option = new Option(month + "月", month); // (表示テキスト, 送信される値)
            // 月の <select> タグに追加
            monthSelect.add(option);
        });
        
        // 7. 月を選択可能（有効化）にする
        monthSelect.disabled = false;
    } else {
        // (データが見つからない場合 - 基本的に発生しないはず)
        monthSelect.disabled = true;
        monthSelect.value = "";
    }
}