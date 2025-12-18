document.addEventListener("DOMContentLoaded", function () {
     // --- ログインモーダル処理 ---
    const loginModalElement = document.getElementById('loginModal');
    if (loginModalElement) {
         // ステップ管理用の要素 (存在しない場合は null になる)
        const qrScanStep      = document.getElementById('qrScanStep');
        const userIdInputStep = document.getElementById('userIdInputStep');
        const passwordStep    = document.getElementById('passwordStep');

         // 入力フォーム要素
        const authCodeInput       = document.getElementById('authCodeInput');
        const userIdInput         = document.getElementById('usernameInput');
        const usernameHiddenInput = document.getElementById('username');
        const passwordInput       = document.getElementById('password');

         // ボタン・メッセージ要素
        const nextToPasswordBtn   = document.getElementById('nextToPasswordBtn');
        const backToFirstStepBtn  = document.getElementById('backToFirstStepBtn');
        const loginSubmitBtn      = document.getElementById('loginSubmitBtn');
        
        const qrError             = document.getElementById('qrError');
        const loginError          = document.getElementById('loginError');
        const passwordError       = document.getElementById('passwordError');

         /**
         * 初期ステップを表示するヘルパー関数
         * QRステップが存在すればそれを、なければID入力ステップを表示する
         */
        function showInitialStep() {
            if (qrScanStep) {
                qrScanStep.style.display = 'block';
                if (userIdInputStep) userIdInputStep.style.display = 'none';
            } else if (userIdInputStep) {
                // QRステップがない（ID入力モード）場合
                userIdInputStep.style.display = 'block';
            }
            if (passwordStep) passwordStep.style.display = 'none';
        }

         /**
         * モーダル表示時の初期フォーカス設定
         */
        loginModalElement.addEventListener('show.bs.modal', function() {
            showInitialStep();

            // フォーカス設定
            if (qrScanStep && authCodeInput) {
                authCodeInput.value = '';
                setTimeout(() => authCodeInput.focus(), 500);
            } else if (userIdInputStep && userIdInput) {
                userIdInput.value = '';
                setTimeout(() => userIdInput.focus(), 500);
            }
        });

         /**
         * モーダル非表示時のリセット処理
         */
        loginModalElement.addEventListener('hidden.bs.modal', function () {
            showInitialStep();
            
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
                        authCodeInput.value = ''; 
                        authCodeInput.focus();    
                    });
                }
            });
            
            authCodeInput.addEventListener('blur', function() {
                if (loginModalElement.classList.contains('show') && qrScanStep.style.display !== 'none') {
                    setTimeout(() => authCodeInput.focus(), 100);
                }
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
                
                if (passwordError) passwordError.style.display = 'none';
                
                if (userIdInputStep) userIdInputStep.style.display = 'none';
                if (passwordStep) passwordStep.style.display = 'block';
                if (passwordInput) setTimeout(() => passwordInput.focus(), 500);
            });
        }
        
        /**
         * パスワード入力ステップにおいて、「戻る」ボタンを押した場合
         */
        if(backToFirstStepBtn) {
            backToFirstStepBtn.addEventListener('click', function() {
                if (passwordStep) passwordStep.style.display = 'none';
                if (passwordInput) passwordInput.value = '';
                if (passwordError) passwordError.style.display = 'none';

                // 直前のステップに戻る（どちらが存在するかで分岐）
                if (qrScanStep) {
                    qrScanStep.style.display = 'block';
                    if (authCodeInput) {
                        authCodeInput.value = '';
                        setTimeout(() => authCodeInput.focus(), 500);
                    }
                } else if (userIdInputStep) {
                    userIdInputStep.style.display = 'block';
                    if (userIdInput) {
                        setTimeout(() => userIdInput.focus(), 500);
                    }
                }
            });
        }
        
        /**
		 * パスワード入力ステップにおいて、「ログイン」ボタンを押した場合
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