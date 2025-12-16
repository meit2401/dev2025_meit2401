// 設定値を保持する変数 (デフォルトは安全のためfalse)
let isTestModeQrAuth = false;

document.addEventListener("DOMContentLoaded", function () {
     // --- ログインモーダル処理 ---
    const loginModalElement = document.getElementById('loginModal');
    if (loginModalElement) {
         // ステップ管理用の要素
        const qrScanStep      = document.getElementById('qrScanStep');
        const userIdInputStep = document.getElementById('userIdInputStep');
        const passwordStep    = document.getElementById('passwordStep');

         // 入力フォーム要素
        const authCodeInput       = document.getElementById('authCodeInput');
        const userIdInput         = document.getElementById('usernameInput');
        const usernameHiddenInput = document.getElementById('username');
        const passwordInput       = document.getElementById('password');

         // ボタン・メッセージ要素
        const switchToManualBtn   = document.getElementById('switchToManualBtn');
        const switchToQrBtn       = document.getElementById('switchToQrBtn');
        const nextToPasswordBtn   = document.getElementById('nextToPasswordBtn');
        const backToFirstStepBtn  = document.getElementById('backToFirstStepBtn');
        const loginSubmitBtn      = document.getElementById('loginSubmitBtn');
        
        const qrError             = document.getElementById('qrError');
        const loginError          = document.getElementById('loginError');
        const passwordError       = document.getElementById('passwordError');

        // 追加: サーバーから設定値を取得してUIを制御
        fetch('/api/auth-config')
            .then(response => {
                // レスポンスがJSONかどうかを確認
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.indexOf("application/json") !== -1) {
                    return response.json();
                } else {
                    // JSONでない場合（ログイン画面のHTMLなど）はエラーとする
                    throw new Error("Response is not JSON");
                }
            })
            .then(config => {
                isTestModeQrAuth = config.testModeQrAuth;
                // テストモードが有効な場合のみ手動入力ボタンを表示
                if (switchToManualBtn) {
                    switchToManualBtn.style.display = isTestModeQrAuth ? 'block' : 'none';
                }
            })
            .catch(error => {
                console.warn('Auth config load failed (using default=false):', error);
                // エラー時は安全側に倒してボタンを非表示
                if (switchToManualBtn) switchToManualBtn.style.display = 'none';
            });

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

                    // 修正: 変数を使用するように変更
                    if (isTestModeQrAuth) {
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

                        // パスワード入力ステップへ遷移する前にエラー表示をクリア
                        if (passwordError) passwordError.style.display = 'none';

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
            // 修正: 初期表示はCSSまたはAPIコールバックで制御するため、ここでの即時非表示ロジックは削除
            // 代わりにロード直後は非表示にしておく（チラつき防止）
            switchToManualBtn.style.display = 'none';

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
                if (!userIdInput) return;
                const userId = userIdInput.value;
                if (!userId || userId.trim() === '') {
                    if (loginError) {
                        loginError.textContent   = 'ユーザーIDを入力してください。';
                        loginError.style.display = 'block';
                    }
                    return;
                }
                if (loginError) loginError.style.display = 'none';
                if (usernameHiddenInput) usernameHiddenInput.value = userId;
                
                // パスワード入力ステップへ遷移する前にエラー表示をクリア
                if (passwordError) passwordError.style.display = 'none';
                
                if (userIdInputStep) userIdInputStep.style.display = 'none';
                if (passwordStep) passwordStep.style.display = 'block';
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

                // エラー表示をクリア
                if (passwordError) passwordError.style.display = 'none';

                // 直前のステップに戻るべきだが、デフォルトでQRステップに戻す
                if (qrScanStep) qrScanStep.style.display = 'block';
                if (authCodeInput) {
                    authCodeInput.value = '';
                    setTimeout(() => authCodeInput.focus(), 500);
                }
            });
        }
        
        /**
		 * パスワード入力ステップにおいて、「ログイン」ボタンを押した場合
         * 修正不要: 省略
		 */
		if (loginSubmitBtn) {
            // 省略: 既存ロジックそのまま
			loginSubmitBtn.addEventListener('click', function() {
				const username = usernameHiddenInput ? usernameHiddenInput.value : '';
				const password = passwordInput ? passwordInput.value : '';

				if (passwordError) {
                    passwordError.style.display = 'none';
                    passwordError.textContent   = '';
				}

				if (!password || password.trim() === '') {
					if (passwordError) {
						passwordError.textContent   = 'パスワードを入力してください。';
						passwordError.style.display = 'block';
					}
					if (passwordInput) {
						passwordInput.focus();
					}
					return;
				}

				const formData = new URLSearchParams();
				formData.append('username', username);
				formData.append('password', password);

				fetch('/login', {
					method: 'POST',
					body  : formData
				})
				.then(response => {
					if (response.redirected && response.url.includes('?error')) {
						if (passwordError) {
							passwordError.textContent   = 'ユーザーIDもしくはパスワードが正しくありません。';
							passwordError.style.display = 'block';
						}
						if (passwordInput) {
							passwordInput.value = '';
							passwordInput.focus();
						}
					} else {
						window.location.href = '/alert';
					}
				})
				.catch(error => {
					console.error('Login request failed:', error);
					if (passwordError) {
						passwordError.textContent   = 'ログイン処理中にエラーが発生しました。';
						passwordError.style.display = 'block';
					}
				});
			});
		}
    }
});