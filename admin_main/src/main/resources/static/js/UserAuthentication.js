document.addEventListener("DOMContentLoaded", function () {
	// --- ログインモーダル処理 ---
	const loginModalElement = document.getElementById('loginModal');
	if (loginModalElement) {
		// Bootstrapモーダルインスタンスの作成
		const loginModal = new bootstrap.Modal(loginModalElement);

		// ステップ管理用の要素
		const userIdInputStep = document.getElementById('userIdInputStep');
		const passwordStep = document.getElementById('passwordStep');
		
		// 入力フォーム要素
		const userIdInput = document.getElementById('userIdInput');
		const usernameHiddenInput = document.getElementById('username');
		const passwordInput = document.getElementById('password');
		
		// ボタン・メッセージ要素
		const nextToPasswordBtn = document.getElementById('nextToPasswordBtn');
		const backToIdInputBtn = document.getElementById('backToIdInputBtn');
		const loginSubmitBtn = document.getElementById('loginSubmitBtn');
		const loginError = document.getElementById('loginError');
		const passwordError = document.getElementById('passwordError');

		// === ユーザーID入力ステップ（次へボタン） ===
		if (nextToPasswordBtn) {
			nextToPasswordBtn.addEventListener('click', function() {
				const userId = userIdInput.value;
				
				// 入力値チェック
				if (!userId || userId.trim() === '') {
					loginError.textContent = 'ユーザーIDを入力してください。';
					loginError.style.display = 'block';
					return;
				}
				
				loginError.style.display = 'none';
				usernameHiddenInput.value = userId; // ユーザーIDを隠しフィールドに設定
				
				// 画面遷移
				userIdInputStep.style.display = 'none';
				passwordStep.style.display = 'block';
				
				// パスワード入力欄にフォーカス
				setTimeout(() => passwordInput.focus(), 500);
			});
		}
		
		// === パスワード入力ステップ（戻るボタン） ===
		if(backToIdInputBtn) {
			backToIdInputBtn.addEventListener('click', function() {
				passwordStep.style.display = 'none';
				userIdInputStep.style.display = 'block';
				
				// 入力値をクリア
				passwordInput.value = '';
				userIdInput.value = '';
				
				// ユーザーID入力欄にフォーカス
				setTimeout(() => userIdInput.focus(), 500);
			});
		}

		// === モーダル表示時のフォーカス制御 ===
		loginModalElement.addEventListener('show.bs.modal', function() {
			setTimeout(() => userIdInput.focus(), 500);
		});
		
		// === モーダル閉鎖時のリセット処理 ===
		loginModalElement.addEventListener('hidden.bs.modal', function () {
			// ステップ表示のリセット
			userIdInputStep.style.display = 'block';
			passwordStep.style.display = 'none';
			
			// 入力値とエラーメッセージのクリア
			userIdInput.value = '';
			passwordInput.value = '';
			usernameHiddenInput.value = '';
			
			loginError.style.display = 'none';
			if (passwordError) {
				passwordError.style.display = 'none';
			}
		});
		
		// === ログイン実行処理 ===
		loginSubmitBtn.addEventListener('click', function() {
			const username = usernameHiddenInput.value;
			const password = passwordInput.value;

			// エラーメッセージを一旦非表示
			passwordError.style.display = 'none';
			passwordError.textContent = ''; // テキストもクリア

			// Spring Security認証用データの作成
			const formData = new URLSearchParams();
			formData.append('username', username);
			formData.append('password', password);

			// ログインリクエスト送信
			fetch('/login', {
				method: 'POST',
				body: formData
			})
			.then(response => {
				// ログイン失敗時はURLに?errorが含まれるリダイレクトが発生する
				if (response.redirected && response.url.includes('?error')) {
					// ログイン失敗
                    // 修正: エラーメッセージをJSで設定
					passwordError.textContent = 'ユーザーIDまたはパスワードが正しくありません。';
					passwordError.style.display = 'block';
					
					passwordInput.value = '';
					passwordInput.focus();
				} else {
					// ログイン成功
					window.location.href = '/alert';
				}
			})
			.catch(error => {
				console.error('Login request failed:', error);
                // 修正: 通信エラー等のメッセージを設定
				passwordError.textContent = 'ログイン処理中にエラーが発生しました。';
				passwordError.style.display = 'block';
			});
		});
	}
});