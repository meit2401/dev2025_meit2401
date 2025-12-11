/**
 * ユーザー認証画面（ログインモーダル）の制御スクリプト
 * * 主な機能:
 * - ユーザーID入力とパスワード入力のステップ切り替え
 * - 入力値の検証とエラー表示
 * - 非同期通信によるログイン処理
 */
document.addEventListener("DOMContentLoaded", function () {
    // --- ログインモーダル処理 ---
    const loginModalElement = document.getElementById('loginModal');
    if (loginModalElement) {
        // ステップ管理用の要素
        const userIdInputStep = document.getElementById('qrScanStep');
        const passwordStep = document.getElementById('passwordStep');
        // 入力フォーム要素
        const userIdInput = document.getElementById('usernameInput');
        const usernameHiddenInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        // ボタン・メッセージ要素
        const nextToPasswordBtn = document.getElementById('nextToPasswordBtn');
        const backToIdInputBtn = document.getElementById('backToQrScanBtn');
        const loginSubmitBtn = document.getElementById('loginSubmitBtn');
        const loginError = document.getElementById('loginError');
        const passwordError = document.getElementById('passwordError');

        // === ユーザーID入力ステップ（次へボタン） ===
        if (nextToPasswordBtn) {
            nextToPasswordBtn.addEventListener('click', function() {
                // 要素が取得できていない場合は処理を中断
                if (!userIdInput) return;

                const userId = userIdInput.value;
                
                // 入力値チェック
                if (!userId || userId.trim() === '') {
                    if (loginError) {
                        loginError.textContent = 'ユーザーIDを入力してください。';
                        loginError.style.display = 'block';
                    }
                    return;
                }
                
                if (loginError) loginError.style.display = 'none';
                if (usernameHiddenInput) usernameHiddenInput.value = userId; // ユーザーIDを隠しフィールドに設定
                
                // 画面遷移
                if (userIdInputStep) userIdInputStep.style.display = 'none';
                if (passwordStep) passwordStep.style.display = 'block';
                
                // パスワード入力欄にフォーカス
                if (passwordInput) setTimeout(() => passwordInput.focus(), 500);
            });
        }
        
        // === パスワード入力ステップ（戻るボタン） ===
        if(backToIdInputBtn) {
            backToIdInputBtn.addEventListener('click', function() {
                if (passwordStep) passwordStep.style.display = 'none';
                if (userIdInputStep) userIdInputStep.style.display = 'block';
                
                // 入力値をクリア
                if (passwordInput) passwordInput.value = '';
                if (userIdInput) userIdInput.value = '';
                
                // ユーザーID入力欄にフォーカス
                if (userIdInput) setTimeout(() => userIdInput.focus(), 500);
            });
        }

        // === モーダル表示時のフォーカス制御 ===
        loginModalElement.addEventListener('show.bs.modal', function() {
            if (userIdInput) setTimeout(() => userIdInput.focus(), 500);
        });
        
        // === モーダル閉鎖時のリセット処理 ===
        loginModalElement.addEventListener('hidden.bs.modal', function () {
            // ステップ表示のリセット
            if (userIdInputStep) userIdInputStep.style.display = 'block';
            if (passwordStep) passwordStep.style.display = 'none';
            
            // 入力値とエラーメッセージのクリア
            if (userIdInput) userIdInput.value = '';
            if (passwordInput) passwordInput.value = '';
            if (usernameHiddenInput) usernameHiddenInput.value = '';
            
            if (loginError) loginError.style.display = 'none';
            if (passwordError) {
                passwordError.style.display = 'none';
            }
        });
        
        // === ログイン実行処理 ===
        if (loginSubmitBtn) {
            loginSubmitBtn.addEventListener('click', function() {
                const username = usernameHiddenInput ? usernameHiddenInput.value : '';
                const password = passwordInput ? passwordInput.value : '';

                // エラーメッセージを一旦非表示
                if (passwordError) {
                    passwordError.style.display = 'none';
                    passwordError.textContent = ''; // テキストもクリア
                }

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
                        if (passwordError) {
                            passwordError.textContent = 'ユーザーIDまたはパスワードが正しくありません。';
                            passwordError.style.display = 'block';
                        }
                        // パスワード入力欄をクリアしてフォーカス
                        if (passwordInput) {
                            passwordInput.value = '';
                            passwordInput.focus();
                        }
                    } else {
                        // ログイン成功
                        window.location.href = '/alert';
                    }
                })
                .catch(error => {
                    console.error('Login request failed:', error);
                    // 通信エラー等のメッセージを設定
                    if (passwordError) {
                        passwordError.textContent = 'ログイン処理中にエラーが発生しました。';
                        passwordError.style.display = 'block';
                    }
                });
            });
        }
    }
});