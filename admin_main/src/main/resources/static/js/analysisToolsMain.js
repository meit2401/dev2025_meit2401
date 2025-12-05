/**
 * サーバー側にCSVファイルを保存するよう非同期リクエストを送信する
 */
function saveCsvToServer() {
	// ユーザーに確認（任意）
	if (!confirm('CSVファイルを保存しますか？')) {
		return; // キャンセルしたら何もしない
	}
	 
	// '/download/csv' エンドポイントにリクエストを送信
	fetch('/stock/download/csv') // GETリクエスト
		.then(response => {
			// サーバーからの応答をテキストとして取得
			return response.text().then(text => ({
				ok: response.ok, // HTTPステータスが 200-299 かどうか
				status: response.status,
				text: text 
			}));
		})
		.then(result => {
			if (result.ok) {
				// 成功した場合 (HTTP 200)
				alert('サーバーにCSVファイルを保存しました。\n');
			} else {
				// 失敗した場合 (HTTP 500 など)
				alert('エラーが発生しました (コード: ' + result.status + ')\n' + result.text);
			}
		})
		.catch(error => {
			// ネットワークエラーなど
			console.error('Fetch error:', error);
			alert('通信エラーが発生しました。');
		});
}