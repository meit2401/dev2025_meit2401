/**
 * 操作履歴画面用ファイル
 * (OperationHistoryDialog.js の機能を含む)
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
		monthSelect.value = "";	  // 月の選択状態をリセット
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

/*
 * ========================================
 * ビデオ操作関数 (旧 videoControls.js)
 * ========================================
 */

// player要素を取得するヘルパー関数
function getPlayer() {
	return document.getElementById("player");
}

// 表示エリアの要素を取得するヘルパー関数
function getStatusDisplay() {
	return document.getElementById("status-display");
}

/*
 * ========================================
 * history.js 本体の処理
 * ========================================
 */

window.updateMonths = updateMonths; // (こちらはモーダルで使われるため残します)


/**
 * ページが読み込まれたら、イベントリスナーを設定する
 */
document.addEventListener("DOMContentLoaded", () => {
	
	// player要素をここで一度だけ取得し、以降の処理で共有する
	const player = document.getElementById('player'); 

	const historyTable = document.getElementById('historyTable');
	const historyTableBody = historyTable ? historyTable.querySelector('tbody') : null;

	// --- (1) すべての操作ボタンの要素を取得 ---
	const playBtn = document.getElementById("play-btn");
	const pauseBtn = document.getElementById("pause-btn");
	const stopBtn = document.getElementById("stop-btn");
	const skipForwardBtn = document.getElementById("skip-forward-btn");
	const skipBackwardBtn = document.getElementById("skip-backward-btn");

	const speed05Btn = document.getElementById("speed-0.5-btn");
	const speed10Btn = document.getElementById("speed-1.0-btn");
	const speed15Btn = document.getElementById("speed-1.5-btn");
	const speed25Btn = document.getElementById("speed-2.5-btn");
	const speed50Btn = document.getElementById("speed-5.0-btn");
	
	const seekBar = document.getElementById('seek-bar');

	// --- (2) ボタンをグループ化 ---

	// videoControlElements: 有効/無効を切り替える全要素
	const videoControlElements = [
		playBtn, pauseBtn, stopBtn, skipForwardBtn, skipBackwardBtn,
		speed05Btn, speed10Btn, speed15Btn, speed25Btn, speed50Btn,
		seekBar
	].filter(el => el !== null); // 存在しない要素は除外

	// speedButtons: 速度変更ボタン (スタイル切り替え用)
	const speedButtons = [speed05Btn, speed10Btn, speed15Btn, speed25Btn, speed50Btn].filter(el => el !== null);
	
	// statefulButtons: 状態保持ボタン (スタイル切り替え用)
	const statefulButtons = [playBtn, pauseBtn, stopBtn].filter(el => el !== null);

	// --- (3) 状態変数を定義 ---
	let currentSpeedButton = speed10Btn; // 速度のデフォルト
	let currentStateButton = stopBtn;   // 再生状態のデフォルト
	let currentPlaybackRate = 1.0; // 再生速度のデフォルト (1.0x)

	// --- (4) スタイル更新関数を定義 ---

	/**
	 * 再生/一時停止/停止ボタンのスタイルを更新する
	 * @param {HTMLElement} selectedButton - 選択状態にするボタン
	 */
	function updateStateButtonStyles(selectedButton) {
		if (!selectedButton) return;

		// 1. すべての状態保持ボタンのスタイルをリセット
		statefulButtons.forEach(button => {
			button.style.backgroundColor = '';
			button.style.color = '';
		});

		// 2. 選択されたボタンにスタイルを適用
		selectedButton.style.backgroundColor = 'var(--color-blue)';
		selectedButton.style.color = 'var(--color-white)';
		
		// 3. 現在の選択ボタンを更新
		currentStateButton = selectedButton;
	}

	/**
	 * 再生速度ボタンのスタイルを更新する
	 * @param {HTMLElement} selectedButton - 選択状態にするボタン
	 */
	function updateSpeedButtonStyles(selectedButton) {
		if (!selectedButton) return;

		// 1. すべての速度ボタンのスタイルをリセット
		speedButtons.forEach(button => {
			button.style.backgroundColor = '';
			button.style.color = '';
		});

		// 2. 選択されたボタンにスタイルを適用
		selectedButton.style.backgroundColor = 'var(--color-blue)';
		selectedButton.style.color = 'var(--color-white)';
		
		// 3. 現在の選択ボタンを更新
		currentSpeedButton = selectedButton;
	}

	/**
	 * ビデオ操作ボタン群の状態（有効/無効）を更新する
	 * @param {boolean} isPlayable - 再生可能な状態か (true=有効, false=無効)
	 */
	function updateVideoControlsState(isPlayable) {
		videoControlElements.forEach(element => {
			element.disabled = !isPlayable;
			element.style.opacity = isPlayable ? 1.0 : 0.5;
		});

		if (!isPlayable) {
			const statusDisplay = getStatusDisplay();
			if (statusDisplay && statusDisplay.textContent !== '動画なし') {
				statusDisplay.textContent = '動画なし';
			}
			
			// 無効化されるタイミングで、スキップボタンのスタイルを強制リセット
			// (0.5秒のタイマー作動中に無効化されてもスタイルが戻るようにする)
			if (skipForwardBtn) {
				skipForwardBtn.style.backgroundColor = '';
				skipForwardBtn.style.color = '';
			}
			if (skipBackwardBtn) {
				skipBackwardBtn.style.backgroundColor = '';
				skipBackwardBtn.style.color = '';
			}
		}
	}

	// --- (5) ビデオ操作関数をローカルに定義 ---

	/** 再生 */
	function playVideo() {
		const videoPlayer = getPlayer();
		const statusDisplay = getStatusDisplay();
		if (!videoPlayer || !statusDisplay) return;

		videoPlayer.play();
		statusDisplay.textContent = '再生';
		updateStateButtonStyles(playBtn); // スタイル更新
	}

	/** 一時停止 */
	function pauseVideo() {
		const videoPlayer = getPlayer();
		const statusDisplay = getStatusDisplay();
		if (!videoPlayer || !statusDisplay) return;

		videoPlayer.pause();
		statusDisplay.textContent = '一時停止';
		updateStateButtonStyles(pauseBtn); // スタイル更新
	}

	/** 停止 */
	function stopVideo() {
		const videoPlayer = getPlayer();
		const statusDisplay = getStatusDisplay();
		if (!videoPlayer || !statusDisplay) return;

		videoPlayer.pause();
		videoPlayer.currentTime = 0;
		statusDisplay.textContent = '停止';
		updateStateButtonStyles(stopBtn); // スタイル更新
	}

	/**
	 * 再生速度を変更する
	 * @param {number} rate - 設定したい再生速度
	 * @param {HTMLElement} clickedButton - クリックされたボタン要素
	 */
	function changeSpeed(rate, clickedButton) {
		const videoPlayer = getPlayer();
		const statusDisplay = getStatusDisplay();
		if (!videoPlayer || !statusDisplay) return;

		videoPlayer.playbackRate = rate;
		currentPlaybackRate = rate; // 選択された速度を状態変数に保存
		statusDisplay.textContent = `再生速度: ${rate}x`;
		updateSpeedButtonStyles(clickedButton);
	}

	/**
	 * 再生位置を変更（スキップ）する
	 * @param {number} seconds - 進めたい秒数（マイナスで戻る）
	 * @param {HTMLElement} clickedButton - クリックされたボタン要素
	 */
	function skip(seconds, clickedButton) {
		const videoPlayer = getPlayer();
		const statusDisplay = getStatusDisplay();
		if (!videoPlayer || !statusDisplay || !clickedButton) return;

		videoPlayer.currentTime += seconds;
		if (seconds > 0) {
			statusDisplay.textContent = `${seconds}秒進む`;
		} else {
			statusDisplay.textContent = `${Math.abs(seconds)}秒戻る`;
		}

		// 一時的なスタイル適用
		clickedButton.style.backgroundColor = 'var(--color-blue)';
		clickedButton.style.color = 'var(--color-white)';
		
		setTimeout(() => {
			// 0.5秒後にスタイルをリセット (ボタンが有効な場合のみ)
			if (!clickedButton.disabled) {
				clickedButton.style.backgroundColor = '';
				clickedButton.style.color = '';
			}
		}, 500); // 0.5秒
	}


	// --- (6) テーブルクリックイベント ---
	if (historyTable && historyTableBody) {
		historyTable.addEventListener('click', function(event) {
			const clickedRow = event.target.closest('tr');
			if (!clickedRow || !clickedRow.closest('tbody')) {
				return;
			}
			const videoPath = clickedRow.dataset.videoPath;
			
			// (空行クリック時)
			if (videoPath === undefined) { 
				historyTableBody.querySelectorAll('tr.custom-list--selected').forEach(row => {
					row.classList.remove('custom-list--selected');
				});
				updateVideoControlsState(false);
				document.getElementById('detail-category').textContent = '分類：○○○';
				document.getElementById('detail-maker').textContent = 'メーカー：○○○';
				document.getElementById('detail-name').textContent = '型番：○○○';
				document.getElementById('detail-material').textContent = '材質：○○○';
				// document.getElementById('detail-buyer').textContent = '商社：○○○'; // 削除
				player.src = "";
				player.load(); // 'loadstart' が発火
				return; 
			}
			
			// (データ行クリック時)
			historyTableBody.querySelectorAll('tr.custom-list--selected').forEach(row => {
				row.classList.remove('custom-list--selected');
			});
			clickedRow.classList.add('custom-list--selected');

			const category = clickedRow.dataset.category;
			const maker = clickedRow.dataset.maker;
			const name = clickedRow.dataset.name;
			const material = clickedRow.dataset.material;
			// const buyer = clickedRow.dataset.buyer; // 削除

			document.getElementById('detail-category').textContent = '分類：' + (category || '----');
			document.getElementById('detail-maker').textContent = 'メーカー：' + (maker || '----');
			document.getElementById('detail-name').textContent = '型番：' + (name || '----');
			document.getElementById('detail-material').textContent = '材質：' + (material || '----');
			// document.getElementById('detail-buyer').textContent = '商社：' + (buyer || '----'); // 削除

			if (videoPath && videoPath !== "null" && videoPath !== "0") {
				player.src = `/video/${videoPath}`;
				updateVideoControlsState(true); 
			} else {
				player.src = ""; 
				updateVideoControlsState(false);
			}
			player.load(); // 'loadstart' が発火
			
			if (!player.src) {
				// stopVideo() を呼んでスタイルもリセット
				stopVideo(); 
				const statusDisplay = getStatusDisplay();
				if(statusDisplay) statusDisplay.textContent = '動画なし';
			}
		});
	}
	
	// --- (7) 操作ボタンのイベント設定 ---
	if (playBtn) playBtn.addEventListener("click", playVideo);
	if (pauseBtn) pauseBtn.addEventListener("click", pauseVideo);
	if (stopBtn) stopBtn.addEventListener("click", stopVideo);
	
	// スキップ関数にボタン要素を渡す
	if (skipForwardBtn) skipForwardBtn.addEventListener("click", () => skip(10, skipForwardBtn));
	if (skipBackwardBtn) skipBackwardBtn.addEventListener("click", () => skip(-10, skipBackwardBtn));

	// 速度変更関数にボタン要素を渡す
	if (speed05Btn) speed05Btn.addEventListener("click", () => changeSpeed(0.5, speed05Btn));
	if (speed10Btn) speed10Btn.addEventListener("click", () => changeSpeed(1.0, speed10Btn));
	if (speed15Btn) speed15Btn.addEventListener("click", () => changeSpeed(1.5, speed15Btn));
	if (speed25Btn) speed25Btn.addEventListener("click", () => changeSpeed(2.5, speed25Btn));
	if (speed50Btn) speed50Btn.addEventListener("click", () => changeSpeed(5.0, speed50Btn));

	
	// --- (8) プレイヤーのイベント設定 ---
	const currentTimeDisplay = document.getElementById('current-time');
	const totalTimeDisplay = document.getElementById('total-time');

	if (player && seekBar && currentTimeDisplay && totalTimeDisplay) {
		
		player.addEventListener('loadedmetadata', () => {
			seekBar.max = player.duration; 
			totalTimeDisplay.textContent = formatTime(player.duration); 
			
			// 保持している再生速度 (currentPlaybackRate) を新しい動画に適用
			player.playbackRate = currentPlaybackRate;
		});

		player.addEventListener('timeupdate', () => {
			seekBar.value = player.currentTime; 
			currentTimeDisplay.textContent = formatTime(player.currentTime); 
			// シークバーの進捗(青い部分)をCSS変数で更新
			const percentage = (player.duration > 0) ? (player.currentTime / player.duration) * 100 : 0;
			seekBar.style.setProperty('--seek-progress-percentage', `${percentage}%`);
		});

		seekBar.addEventListener('input', () => {
			player.currentTime = seekBar.value; 
			// スライド中も進捗(青い部分)をCSS変数で更新
			const percentage = (player.duration > 0) ? (player.currentTime / player.duration) * 100 : 0;
			seekBar.style.setProperty('--seek-progress-percentage', `${percentage}%`);
		});
		
		player.addEventListener('ended', () => {
			const statusDisplay = getStatusDisplay();
			if(statusDisplay) statusDisplay.textContent = '再生終了';
			updateStateButtonStyles(stopBtn); // 終了時も停止スタイルに
		});

		player.addEventListener('loadstart', () => {
			seekBar.value = 0;
			seekBar.max = 100; 
			currentTimeDisplay.textContent = formatTime(0);
			totalTimeDisplay.textContent = formatTime(0);
			seekBar.style.setProperty('--seek-progress-percentage', '0%');

			// 状態スタイルのみリセット (速度スタイルはリセットしない)
			if (stopBtn) {
				updateStateButtonStyles(stopBtn);
			}

			const statusDisplay = getStatusDisplay();
			if(statusDisplay) {
				if (player.src && player.src !== window.location.href) { 
					statusDisplay.textContent = '停止';
				} else {
					statusDisplay.textContent = '動画なし';
				}
			}
		});
	}

	/**
	 * テーブルの行数が最低行数に満たない場合、空行を追加する
	 */
	function adjustTableRows(minRows, tableBody) {
		if (!tableBody) return;
		
		// 既存の空行をすべて削除
		const allRows = tableBody.querySelectorAll("tr");
		const emptyRows = Array.from(allRows).filter(row => {
			return row.dataset.videoPath === undefined; 
		});
		emptyRows.forEach(row => row.remove());

		// 現在のデータ行の数をカウント
		const dataRows = tableBody.querySelectorAll("tr[data-video-path]");
		const currentRowCount = dataRows.length;
		
		// 足りない分の空行を追加
		if (currentRowCount < minRows) {
			const cellsInRow = 4; 
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = tableBody.insertRow(); 
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
		}
	}

	// --- (9) 初期化処理 ---
	adjustTableRows(5, historyTableBody); 
	
	if (speed10Btn) {
		updateSpeedButtonStyles(speed10Btn); // 速度ボタンの初期スタイル
	}
	if (stopBtn) {
		updateStateButtonStyles(stopBtn); // 状態ボタンの初期スタイル
	}
	
	updateVideoControlsState(false); // 初期状態は全ボタン無効
	
	const statusDisplay = getStatusDisplay();
	if(statusDisplay) statusDisplay.textContent = '動画なし';

}); // DOMContentLoaded 終了

/**
 * 秒数を "分:秒" (m:ss) 形式の文字列にフォーマットする
 * @param {number} timeInSeconds - 秒数
 * @returns {string} フォーマットされた時間
 */
function formatTime(timeInSeconds) {
	if (isNaN(timeInSeconds) || timeInSeconds <= 0) {
		return "0:00";
	}
	const minutes = Math.floor(timeInSeconds / 60);
	const seconds = Math.floor(timeInSeconds % 60);
	return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}