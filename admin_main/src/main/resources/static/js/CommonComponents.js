document.addEventListener("DOMContentLoaded", function () {
	// --- リアルタイム時刻表示の初期化と定期更新 ---
	updateTime();
	setInterval(updateTime, 1000); // 100ミリ秒から1秒（1000ミリ秒）に変更推奨

	// --- パスワード更新処理 ---
	const passwordUpdateForm = document.getElementById('passwordUpdateForm');
	if (passwordUpdateForm) {
		const newPasswordInput = document.getElementById('newPassword');
		const confirmPasswordInput = document.getElementById('confirmPassword');
		const successMessage = document.getElementById('passwordUpdateSuccess');
		const errorMessage = document.getElementById('passwordUpdateError');
		const settingModalElement = document.getElementById('settingModal');
		const settingModal = bootstrap.Modal.getOrCreateInstance(settingModalElement);

		// フォーム送信イベント
		passwordUpdateForm.addEventListener('submit', function (event) {
			event.preventDefault(); // デフォルトのフォーム送信をキャンセル

			// メッセージを非表示に初期化
			successMessage.style.display = 'none';
			errorMessage.style.display = 'none';
			errorMessage.textContent = ''; // テキストもリセット

			const newPassword = newPasswordInput.value;
			const confirmPassword = confirmPasswordInput.value;

			// パスワードが未入力の場合
			if (!newPassword || !confirmPassword) {
				errorMessage.textContent = 'パスワードを入力してください。';
				errorMessage.style.display = 'block';
				return;
			}

			// パスワードが一致しない場合
			if (newPassword !== confirmPassword) {
				errorMessage.textContent = '新しいパスワードと確認用パスワードが一致しません。';
				errorMessage.style.display = 'block';
				return;
			}

			// サーバーに更新リクエストを送信
			fetch('/setting/password/update', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({ password: newPassword }),
			})
			.then(response => {
				if (response.ok) {
					// 成功した場合
					successMessage.style.display = 'block';
					// フォームをリセット
					passwordUpdateForm.reset(); 
				} else {
					// 失敗した場合
					errorMessage.textContent = 'パスワードの更新に失敗しました。';
					errorMessage.style.display = 'block';
				}
			})
			.catch(error => {
				console.error('Error:', error);
				errorMessage.textContent = '通信エラーが発生しました。';
				errorMessage.style.display = 'block';
			});
		});

		// モーダルが閉じられるときにフォームとメッセージをリセット
		settingModalElement.addEventListener('hidden.bs.modal', function () {
			successMessage.style.display = 'none';
			errorMessage.style.display = 'none';
			errorMessage.textContent = '';
			passwordUpdateForm.reset();
		});
	}
	
	// 1. 対象となるクラス名を持つすべてのinput要素を取得
	const inputsToNormalize = document.querySelectorAll('.js-normalize-hankaku');

	// 2. 取得した各input要素に対して処理を実行
	inputsToNormalize.forEach(function(input) {
		// 3. IMEをデフォルトで無効化 (HTMLからstyle属性を削除するため)
		input.style.imeMode = 'disabled';
		
		// 4. 'input' (入力中) イベントが発生したときに、
		//	normalizeInputToHankaku 関数が実行されるように設定
		input.addEventListener('input', normalizeInputToHankaku);
		
		// 5. [追加] 'compositionend' (IME確定時) イベント
		//    日本語入力が確定した際にも明示的に変換処理を実行する
		input.addEventListener('compositionend', normalizeInputToHankaku);
	});
});

/**
 * 全角英数字、記号、スペースを自動で半角に変換し、
 * 不正な文字（'、'など）を削除するイベントハンドラ。
 * @param {Event} event - inputイベントオブジェクト
 */
function normalizeInputToHankaku(event) {
	// IMEによる入力が未確定（変換中）の場合は、処理を中断する
	// これにより、未確定文字が消えたりする不具合を防ぐ
	if (event.isComposing) {
		return;
	}

	const input = event.target;
	let value = input.value;

	// 変換前のカーソル位置を保持
	let selectionStart = null;
	let selectionEnd = null;
	
	// type="password"などでselectionStartがサポートされていない場合を考慮
	if (typeof input.selectionStart === 'number') {
		selectionStart = input.selectionStart;
		selectionEnd = input.selectionEnd;
	}

	// 1. 全角英数字と記号（！～～）を半角に変換
	let convertedValue = value.replace(/[\uFF01-\uFF5E]/g, function(char) {
		return String.fromCharCode(char.charCodeAt(0) - 0xFEE0);
	});

	// 2. 全角スペースを半角スペースに変換
	convertedValue = convertedValue.replace(/\u3000/g, ' ');

	// 3. 半角に変換できない不正な文字（例：、）を削除
	//	(必要に応じて対象文字を追加してください)
	convertedValue = convertedValue.replace(/[\u3001]/g, '');

	// 4. 実際に値が変更された場合のみ、inputの値を更新
	if (input.value !== convertedValue) {
		input.value = convertedValue;

		// カーソル位置を復元
		if (selectionStart !== null && typeof input.setSelectionRange === 'function') {
			// '、' などが削除された場合、カーソル位置が元の文字列長を超える可能性があるため調整
			const newLength = convertedValue.length;
			if (selectionStart > newLength) {
				selectionStart = newLength;
			}
			if (selectionEnd > newLength) {
				selectionEnd = newLength;
			}
			input.setSelectionRange(selectionStart, selectionEnd);
		}
	}
}

// 現在時刻を更新する関数
function updateTime() {
	// 現在の日時を取得
	const now = new Date();
	const year = now.getFullYear();
	const month = String(now.getMonth() + 1).padStart(2, '0');
	const day = String(now.getDate()).padStart(2, '0');
	const hours = String(now.getHours()).padStart(2, '0');
	const minutes = String(now.getMinutes()).padStart(2, '0');

	// フォーマットした時刻文字列を作成
	const formattedTime = `${year}年${month}月${day}日 ${hours}:${minutes}`;
	const display = document.getElementById("real-time-display");
	// 時刻表示要素が存在すれば内容を更新
	if (display) {
		display.textContent = formattedTime;
	}
}

