document.addEventListener("DOMContentLoaded", function () {
	// --- ログインモーダル処理 ---
	const loginModalElement = document.getElementById('loginModal');
	if (loginModalElement) {
		const loginModal = new bootstrap.Modal(loginModalElement);
		const qrScanStep = document.getElementById('qrScanStep');
		const passwordStep = document.getElementById('passwordStep');
		const authCodeInput = document.getElementById('authCodeInput');
		const loginError = document.getElementById('loginError');
		const passwordInput = document.getElementById('password');
		const backToQrScanBtn = document.getElementById('backToQrScanBtn');
		const usernameHiddenInput = document.getElementById('username');
		
		// === テスト用 フォーム要素 ===
		const usernameInput = document.getElementById('usernameInput');
		const nextToPasswordBtn = document.getElementById('nextToPasswordBtn');
		// === テスト用 フォーム要素 終了 ===
		
		// デバウンス処理用のタイマー変数を定義
		let debounceTimeout;

		//* === テスト用 ユーザーID入力処理（「次へ」ボタン） ===
		if (nextToPasswordBtn) {
			nextToPasswordBtn.addEventListener('click', function() {
				const userId = usernameInput.value;
				
				// 入力値が空かどうかをチェック
				if (!userId || userId.trim() === '') {
					loginError.textContent = 'ユーザーIDを入力してください。';
					loginError.style.display = 'block';
					return;
				}
				
				loginError.style.display = 'none';
				usernameHiddenInput.value = userId; // ユーザーIDを隠しフィールドに設定
				qrScanStep.style.display = 'none'; // ユーザーID入力画面を非表示
				passwordStep.style.display = 'block'; // パスワード入力画面を表示
				// 少し待ってからパスワード入力欄にフォーカスを当てる
				setTimeout(() => passwordInput.focus(), 500);
			});
		}
		// === テスト用 処理 終了 ===
		
		// 戻るボタンがクリックされたときの処理
		if(backToQrScanBtn) {
			backToQrScanBtn.addEventListener('click', function() {
				passwordStep.style.display = 'none'; // パスワード入力画面を非表示
				qrScanStep.style.display = 'block'; // QRスキャン画面を表示
				authCodeInput.value = ''; // 認証コードの入力値をクリア
				if (usernameInput) {
					usernameInput.value = ''; // (テスト用) ユーザーID入力値をクリア
				}
				// 少し待ってから認証コード入力欄にフォーカスを戻す
				// setTimeout(() => authCodeInput.focus(), 500); // (QR用)
				if (usernameInput) {
					setTimeout(() => usernameInput.focus(), 500); // (テスト用) ユーザーID入力欄にフォーカス
				}
			});
		}

		// モーダルが表示される直前に、非表示の入力欄にフォーカスを当てる
		loginModalElement.addEventListener('show.bs.modal', function() {
			// setTimeout(() => authCodeInput.focus(), 500); // (QR用)
			if (usernameInput) {
				setTimeout(() => usernameInput.focus(), 500); // (テスト用) ユーザーID入力欄にフォーカス
			}
		});
		
		// モーダルが閉じられたときに各ステップと入力値をリセット
		loginModalElement.addEventListener('hidden.bs.modal', function () {
			// 既存のタイマーをクリア
			clearTimeout(debounceTimeout);
			
			// パスワード入力欄のエラーメッセージを非表示にする
			const passwordError = passwordStep.querySelector('.alert-danger');
			if (passwordError) {
				passwordError.style.display = 'none';
			}

			qrScanStep.style.display = 'block';
			passwordStep.style.display = 'none';
			authCodeInput.value = '';
			if (usernameInput) {
				usernameInput.value = ''; // (テスト用) ユーザーID入力値をクリア
			}
			passwordInput.value = '';
			usernameHiddenInput.value = '';
			loginError.style.display = 'none';
		});
		
		const loginSubmitBtn = document.getElementById('loginSubmitBtn');
		const passwordError = document.getElementById('passwordError');

		// ログインボタンクリック時の非同期ログイン処理
		loginSubmitBtn.addEventListener('click', function() {
			const username = usernameHiddenInput.value;
			const password = passwordInput.value;

			// エラーメッセージを一旦非表示にする
			passwordError.style.display = 'none';

			// Spring Securityへ送信するためのフォームデータを作成
			const formData = new URLSearchParams();
			formData.append('username', username);
			formData.append('password', password);

			// /loginエンドポイントへfetch APIを使用してPOSTリクエストを送信
			fetch('/login', {
				method: 'POST',
				body: formData
			})
			.then(response => {
				// Spring Securityは失敗時にリダイレクトで応答する
				// そのリダイレクト先のURLに"?error"が含まれているかで成否を判断
				if (response.redirected && response.url.includes('?error')) {
					// ログイン失敗
					passwordError.style.display = 'block'; // エラーメッセージ表示
					passwordInput.value = ''; // パスワード欄を空にする
					passwordInput.focus(); // 再度パスワード欄にフォーカスを当てる
				} else {
					// ログイン成功
					window.location.href = '/alert'; // 成功ページへ遷移
				}
			})
			.catch(error => {
				console.error('Login request failed:', error);
				passwordError.textContent = 'ログイン処理中にエラーが発生しました。';
				passwordError.style.display = 'block';
			});
		});
	}
});