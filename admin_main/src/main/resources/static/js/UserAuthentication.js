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
        const passwordStep    = document.getElementById('passwordStep');
         // 入力フォーム要素
        const userIdInput         = document.getElementById('usernameInput');
        const usernameHiddenInput = document.getElementById('username');
        const passwordInput       = document.getElementById('password');
         // ボタン・メッセージ要素
        const nextToPasswordBtn = document.getElementById('nextToPasswordBtn');
        const backToIdInputBtn  = document.getElementById('backToQrScanBtn');
        const loginSubmitBtn    = document.getElementById('loginSubmitBtn');
        const loginError        = document.getElementById('loginError');
        const passwordError     = document.getElementById('passwordError');

         /**
         * モーダル表示時の初期フォーカス設定
         * ユーザーID入力欄にフォーカスを設定する
         */
        loginModalElement.addEventListener('show.bs.modal', function() {
             // 500msの後、ユーザーID入力欄にフォーカス
            if (userIdInput) setTimeout(() => userIdInput.focus(), 500);
        });

         /**
         * モーダル非表示時のリセット処理
         * 各種入力欄とエラーメッセージをクリアし、ステップ表示を初期状態に戻す
         */
        loginModalElement.addEventListener('hidden.bs.modal', function () {
            // ステップ表示のリセット
            if (userIdInputStep) userIdInputStep.style.display = 'block'; // ユーザーID入力ステップを表示
            if (passwordStep) passwordStep.style.display = 'none';        // パスワード入力ステップを非表示
            
            // 入力値とエラーメッセージのクリア
            if (userIdInput) userIdInput.value = '';                 // ユーザーID入力欄をクリア
            if (passwordInput) passwordInput.value = '';             // パスワード入力欄をクリア
            if (usernameHiddenInput) usernameHiddenInput.value = ''; // 隠しフィールドをクリア
            
            // ユーザーIDエラーメッセージを非表示
            if (loginError) loginError.style.display = 'none'; // ユーザーIDエラーメッセージを非表示

             // パスワードエラーメッセージが存在する場合
            if (passwordError) {
                passwordError.style.display = 'none'; // パスワードエラーメッセージを非表示
            }
        });

         /**
         * ユーザーID入力ステップにおいて、「次へ」ボタンを押した場合
         * ユーザーIDの入力チェックを行い、パスワード入力ステップへ遷移する
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
         * ユーザーID入力ステップへ戻る
         */
        if(backToIdInputBtn) {
            backToIdInputBtn.addEventListener('click', function() {
                // 画面遷移
                if (passwordStep) passwordStep.style.display       = 'none';  // パスワード入力ステップを非表示
                if (userIdInputStep) userIdInputStep.style.display = 'block'; // ユーザーID入力ステップを表示
                
                // 入力値をクリア
                if (passwordInput) passwordInput.value = ''; // パスワード入力欄をクリア
                if (userIdInput) userIdInput.value = '';     // ユーザーID入力欄をクリア
                
                 // 500msの後、ユーザーID入力欄にフォーカス
                if (userIdInput) setTimeout(() => userIdInput.focus(), 500);
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
							passwordError.textContent   = 'ユーザーIDまたはパスワードが正しくありません。'; // エラーメッセージを設定
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