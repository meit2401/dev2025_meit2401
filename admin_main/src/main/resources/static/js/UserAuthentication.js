// テスト用モード設定：trueの場合、QRコードによるユーザー認証を行わない
const TEST_MODE_QR_AUTH = false;

document.addEventListener("DOMContentLoaded", function () {
     // --- ログインモーダル処理 ---
    const loginModalElement = document.getElementById('loginModal');
    if (loginModalElement) {
         // ステップ管理用の要素
        const qrScanStep      = document.getElementById('qrScanStep');      // 追加: QRスキャンステップ
        const userIdInputStep = document.getElementById('userIdInputStep'); // 変更: ID入力ステップ
        const passwordStep    = document.getElementById('passwordStep');

         // 入力フォーム要素
        const authCodeInput       = document.getElementById('authCodeInput');   // 追加: QRコード入力欄
        const userIdInput         = document.getElementById('usernameInput');
        const usernameHiddenInput = document.getElementById('username');
        const passwordInput       = document.getElementById('password');

         // ボタン・メッセージ要素
        const switchToManualBtn   = document.getElementById('switchToManualBtn'); // 追加: 手動切替ボタン
        const switchToQrBtn       = document.getElementById('switchToQrBtn');     // 追加: QR切替ボタン
        const nextToPasswordBtn   = document.getElementById('nextToPasswordBtn');
        const backToFirstStepBtn  = document.getElementById('backToFirstStepBtn'); // 変更: 戻るボタン
        const loginSubmitBtn      = document.getElementById('loginSubmitBtn');
        
        const qrError             = document.getElementById('qrError');    // 追加: QRエラー
        const loginError          = document.getElementById('loginError');
        const passwordError       = document.getElementById('passwordError');

         /**
         * モーダル表示時の初期フォーカス設定
         * デフォルトでQRコード入力欄にフォーカスを設定する
         */
        loginModalElement.addEventListener('show.bs.modal', function() {
             // 初期表示はQRスキャンステップ
            if (qrScanStep) qrScanStep.style.display = 'block';
            if (userIdInputStep) userIdInputStep.style.display = 'none';
            if (passwordStep) passwordStep.style.display = 'none';

             // 500msの後、QRコード入力欄にフォーカス
            if (authCodeInput) {
                authCodeInput.value = '';
                setTimeout(() => authCodeInput.focus(), 500);
            }
        });

         /**
         * モーダル非表示時のリセット処理
         */
        loginModalElement.addEventListener('hidden.bs.modal', function () {
            // ステップ表示のリセット（QRスキャンをデフォルトにする）
            if (qrScanStep) qrScanStep.style.display = 'block';
            if (userIdInputStep) userIdInputStep.style.display = 'none';
            if (passwordStep) passwordStep.style.display = 'none';
            
            // 入力値とエラーメッセージのクリア
            if (authCodeInput) authCodeInput.value = '';
            if (userIdInput) userIdInput.value = '';
            if (passwordInput) passwordInput.value = '';
            if (usernameHiddenInput) usernameHiddenInput.value = '';
            
            // 各種エラーメッセージを非表示
            if (qrError) qrError.style.display = 'none';
            if (loginError) loginError.style.display = 'none';
            if (passwordError) passwordError.style.display = 'none';
        });

        /**
         * QRコード入力欄のイベントリスナー
         * スキャナーからの入力を検知（Enterキー押下）して認証APIを呼び出す
         */
        if (authCodeInput) {
            authCodeInput.addEventListener('keydown', function(event) {
                if (event.key === 'Enter') {
                    event.preventDefault(); // フォーム送信を防ぐ
                    const authCode = authCodeInput.value;

                    if (!authCode || authCode.trim() === '') return;

                    if (TEST_MODE_QR_AUTH) {
                        console.log("Test Mode: QR Scanned", authCode);
                    }

                    // 認証APIの呼び出し
                    fetch('/api/verify-auth-code', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ authCode: authCode })
                    })
                    .then(response => {
                        if (response.ok) {
                            return response.json();
                        } else {
                            throw new Error('認証に失敗しました');
                        }
                    })
                    .then(data => {
                        // 認証成功: ユーザーIDを設定してパスワード入力へ遷移
                        if (usernameHiddenInput) usernameHiddenInput.value = data.userId;
                        if (qrError) qrError.style.display = 'none';

                        if (qrScanStep) qrScanStep.style.display = 'none';
                        if (passwordStep) passwordStep.style.display = 'block';
                        if (passwordInput) setTimeout(() => passwordInput.focus(), 500);
                    })
                    .catch(error => {
                        // 認証失敗
                        if (qrError) {
                            qrError.textContent = 'QRコードが無効か、有効期限切れです。';
                            qrError.style.display = 'block';
                        }
                        authCodeInput.value = ''; // 入力欄クリア
                        authCodeInput.focus();    // 再フォーカス
                    });
                }
            });
            
            // フォーカスが外れないように監視（モーダル表示中かつQRステップ表示中のみ）
            authCodeInput.addEventListener('blur', function() {
                if (loginModalElement.classList.contains('show') && qrScanStep.style.display !== 'none') {
                    setTimeout(() => authCodeInput.focus(), 100);
                }
            });
        }

        /**
         * 「IDを手動で入力する」ボタン押下時の処理
         */
        if (switchToManualBtn) {
            switchToManualBtn.addEventListener('click', function() {
                if (qrScanStep) qrScanStep.style.display = 'none';
                if (userIdInputStep) userIdInputStep.style.display = 'block';
                if (userIdInput) setTimeout(() => userIdInput.focus(), 500);
                if (qrError) qrError.style.display = 'none'; // エラー消去
            });
        }

        /**
         * 「QRコードでログイン」ボタン押下時の処理
         */
        if (switchToQrBtn) {
            switchToQrBtn.addEventListener('click', function() {
                if (userIdInputStep) userIdInputStep.style.display = 'none';
                if (qrScanStep) qrScanStep.style.display = 'block';
                if (authCodeInput) {
                    authCodeInput.value = '';
                    setTimeout(() => authCodeInput.focus(), 500);
                }
                if (loginError) loginError.style.display = 'none'; // エラー消去
            });
        }

         /**
         * ユーザーID入力ステップにおいて、「次へ」ボタンを押した場合
         */
        if (nextToPasswordBtn) {
            nextToPasswordBtn.addEventListener('click', function() {
                 // 要素が取得できない場合、処理を中断
                if (!userIdInput) return;

                 // ユーザーIDの取得
                const userId = userIdInput.value;
                
                 // 入力値が空の場合
                if (!userId || userId.trim() === '') {
                    if (loginError) {
                        loginError.textContent   = 'ユーザーIDを入力してください。'; // エラーメッセージを設定
                        loginError.style.display = 'block';            // エラーメッセージを表示
                    }
                    return;
                }
                
                // エラーメッセージのクリアと隠しフィールドへの設定
                if (loginError) loginError.style.display = 'none';           // エラーメッセージを非表示
                if (usernameHiddenInput) usernameHiddenInput.value = userId; // ユーザーIDを隠しフィールドに設定
                
                // 画面遷移
                if (userIdInputStep) userIdInputStep.style.display = 'none'; // ユーザーID入力ステップを非表示
                if (passwordStep) passwordStep.style.display = 'block';      // パスワード入力ステップを表示
                
                 // 500msの後、パスワード入力欄にフォーカス
                if (passwordInput) setTimeout(() => passwordInput.focus(), 500);
            });
        }
        
         /**
         * パスワード入力ステップにおいて、「戻る」ボタンを押した場合
         * ユーザーID入力ステップ（またはQRステップ）へ戻る
         */
        if(backToFirstStepBtn) {
            backToFirstStepBtn.addEventListener('click', function() {
                // パスワードステップを非表示
                if (passwordStep) passwordStep.style.display = 'none';

                // 入力値をクリア
                if (passwordInput) passwordInput.value = '';

                // 直前のステップに戻るべきだが、デフォルトでQRステップに戻す
                if (qrScanStep) qrScanStep.style.display = 'block';
                if (authCodeInput) {
                    authCodeInput.value = '';
                    setTimeout(() => authCodeInput.focus(), 500);
                }
                
                // もし手動入力ステップの状態を保持したい場合はここで制御可能
            });
        }
        
         /**
		 * パスワード入力ステップにおいて、「ログイン」ボタンを押した場合
         * 入力されたパスワードの検証を行い、非同期通信でログイン処理を実行する
		 */
		if (loginSubmitBtn) {
			loginSubmitBtn.addEventListener('click', function() {
				// 要素が取得できない場合、処理を中断
				const username = usernameHiddenInput ? usernameHiddenInput.value : ''; // 隠しフィールドからユーザーIDを取得
				const password = passwordInput ? passwordInput.value : '';             // パスワード入力欄からパスワードを取得

				 // エラーメッセージを一旦非表示
				if (passwordError) {
                    passwordError.style.display = 'none'; // パスワードエラーメッセージを非表示
                    passwordError.textContent   = '';     // メッセージ内容をクリア
				}

				 // 入力値が空の場合
				if (!password || password.trim() === '') {
                     // パスワードが未入力の場合の処理
					if (passwordError) {
						passwordError.textContent   = 'パスワードを入力してください。'; // エラーメッセージを設定
						passwordError.style.display = 'block';           // エラーメッセージを表示
					}
					if (passwordInput) {
						passwordInput.focus(); // パスワード入力欄にフォーカス
					}
					return;
				}

				// Spring Security認証用データの作成
				const           formData = new URLSearchParams(); // フォームデータオブジェクトを作成
				formData.append('username', username);            // ユーザーIDを追加
				formData.append('password', password);            // パスワードを追加

				 // ログインリクエスト送信
				fetch('/login', {
					method: 'POST',
					body  : formData
				})
				.then(response => {
					 // ログイン失敗時はURLに?errorが含まれるリダイレクトが発生する
					if (response.redirected && response.url.includes('?error')) {
						 // ログイン失敗
						if (passwordError) {
							passwordError.textContent   = 'ユーザーIDもしくはパスワードが正しくありません。'; // エラーメッセージを設定
							passwordError.style.display = 'block';                    // エラーメッセージを表示
						}
						 // パスワード入力欄をクリアしてフォーカス
						if (passwordInput) {
							passwordInput.value = ''; // パスワード入力欄をクリア
							passwordInput.focus();    // パスワード入力欄にフォーカス
						}
					} else {
						 // ログイン成功
						window.location.href = '/alert';
					}
				})
				.catch(error => {
					console.error('Login request failed:', error); // エラー内容をコンソールに出力
					// 通信エラー等のメッセージを設定
					if (passwordError) {
						passwordError.textContent   = 'ログイン処理中にエラーが発生しました。'; // エラーメッセージを設定
						passwordError.style.display = 'block';               // エラーメッセージを表示
					}
				});
			});
		}
    }
});