document.addEventListener('DOMContentLoaded', () => {

	// 1. グリッドボタン住所テキスト設定
	try {
		const gridButtonsForText = document.querySelectorAll('#toolassignmentModal .grid-button');
		
		gridButtonsForText.forEach(button => {
			const { segment, rowIndex, colIndex, maxRows, maxCols } = button.dataset;

			if (!segment || !rowIndex || !colIndex || !maxRows || !maxCols) {
				console.warn('ボタンのdata属性が不足しています。tools.html の構造を確認してください。', button.dataset);
				return; 
			}

			const i = parseInt(rowIndex, 10); // 1..9 (HTMLの th:each)
			const j = parseInt(colIndex, 10); // 1..8 (HTMLの th:each)
			const rows = parseInt(maxRows, 10); // 9
			const cols = parseInt(maxCols, 10); // 8 (A), 3 (B, C)

			if (isNaN(i) || isNaN(j) || isNaN(rows) || isNaN(cols)) {
				console.error('座標または最大値の数値変換に失敗しました。', button.dataset);
				return;
			}
			const x = cols - j + 1; 
			const y = rows - i + 1;

			button.textContent = `${x}${y}`;
			button.style.fontSize = "1.5rem"; 
			button.style.fontWeight = "bold";
		});
	} catch (e) {
		console.error("グリッドボタンの住所テキスト設定中にエラー:", e);
	}
	// 1. グリッドボタン設定ここまで


	// 2. ツール割当モーダルロジック
	const toolAssignmentModalEl = document.getElementById('toolassignmentModal');

	if (toolAssignmentModalEl) {
		
		const toolAssignmentModal = new bootstrap.Modal(toolAssignmentModalEl);
		
		const okButtonForSetup = toolAssignmentModalEl.querySelector('#toolAssignmentOkButton');
		if (okButtonForSetup) {
			okButtonForSetup.classList.remove('custom-btn-modal');
			okButtonForSetup.classList.remove('btn-outline-danger'); 
			okButtonForSetup.classList.add('custom-btn-common');
			okButtonForSetup.classList.add('custom-btn-common--notselectable');
		}

		// --- 2-1. セグメント切り替えのロジック ---
		const segments = [
			{ id: 'segmentA', title: 'セグメントA' },
			{ id: 'segmentB', title: 'セグメントB' },
			{ id: 'segmentC', title: 'セグメントC' }
		];
		let currentSegment = 0; 
		const segmentTitle = toolAssignmentModalEl.querySelector('#segmentTitle');
		const segmentContainers = segments.map(s => toolAssignmentModalEl.querySelector(`#${s.id}`));
		const prevButton = toolAssignmentModalEl.querySelector('.btn.fs-2:first-of-type'); 
		const nextButton = toolAssignmentModalEl.querySelector('.btn.fs-2:last-of-type');  
		
		const showSegment = (index) => {
			segmentContainers.forEach(container => {
				container.style.display = 'none';
			});
			segmentContainers[index].style.display = 'block';
			segmentTitle.textContent = segments[index].title;
			currentSegment = index; 
		};
		prevButton.addEventListener('click', () => {
			const newIndex = (currentSegment - 1 + segments.length) % segments.length;
			showSegment(newIndex);
		});
		nextButton.addEventListener('click', () => {
			const newIndex = (currentSegment + 1) % segments.length;
			showSegment(newIndex);
		});
		// --- 2-1. セグメント切り替えここまで ---


		// --- 2-2. グリッドボタン選択のロジック ---
		let requiredSelections = 0;   
		let selectedAddresses = new Set(); 
		let selectedButtons = new Map();   
		const allGridButtons = toolAssignmentModalEl.querySelectorAll('.grid-button');
		const messageElement = toolAssignmentModalEl.querySelector('#toolAssignmentMessage');
		const okButton = toolAssignmentModalEl.querySelector('#toolAssignmentOkButton');

		allGridButtons.forEach(button => {
			button.addEventListener('click', () => {
				
				const { segment, rowIndex, colIndex, maxRows, maxCols } = button.dataset;
				const i = parseInt(rowIndex, 10); 
				const j = parseInt(colIndex, 10); 
				const rows = parseInt(maxRows, 10);
				const cols = parseInt(maxCols, 10);

				if (isNaN(i) || isNaN(j) || isNaN(rows) || isNaN(cols)) {
					console.error('座標または最大値の数値変換に失敗しました。', button.dataset);
					return;
				}
				
				const x = cols - j + 1;
				const y = rows - i + 1;
				
				const address = `${segment}${x}${y}`;
				const currentUsage = storageUsageCounts[address] || 0; 

				if (selectedAddresses.has(address)) {
					// 選択解除 (青 -> 元の色(緑/灰) に戻す)
					selectedAddresses.delete(address);
					selectedButtons.delete(address);
					button.classList.remove('active'); 
					
					// スタイルをリセット
					button.style.backgroundColor = '';
					button.style.color = '';
					button.style.borderColor = '';
					button.classList.remove('btn-outline-secondary'); // 念のため削除

					// 元の色を再適用 (show.bs.modal と同じロジック)
					if (currentUsage === 0) {
						// 0個: デフォルト (白背景/黒文字/黒枠線)
						button.style.borderColor = 'var(--color-black)'; 
					} else if (currentUsage === 1) {
						button.style.backgroundColor = 'var(--color-orange)'; // 1個: 橙
						button.style.color = 'var(--color-white)'; 
						button.style.borderColor = 'var(--color-black)';
					}
					// (2個以上(赤)は disabled なので、この分岐には入らない)

				} else {
					// 選択 (緑/灰 -> 青 にする)
					if (currentUsage >= 2) {
						alert(`格納場所 [${address}] は、すでに上限(2個)に達しているため選択できません。`);
						return; 
					}
					if (selectedAddresses.size < requiredSelections) {
						selectedAddresses.add(address);
						selectedButtons.set(address, button);
						
						// スタイルをリセット
						button.style.backgroundColor = '';
						button.style.color = '';
						button.style.borderColor = '';
						button.classList.remove('btn-outline-secondary');
						
						// 青色を適用
						button.style.backgroundColor = 'var(--color-blue)';
						button.style.color = 'var(--color-white)'; 
						button.style.borderColor = 'var(--color-black)';
						button.classList.add('active'); 
						
					} else {
						alert(`選択できるのは ${requiredSelections} 個までです。`);
					}
				}
				
				const remaining = requiredSelections - selectedAddresses.size;
				messageElement.textContent = `コンテナ選択 残り ${remaining}個`;
				
				if (remaining === 0) {
					okButton.disabled = false;
					okButton.classList.remove("custom-btn-common");
					okButton.classList.remove("custom-btn-common--notselectable");
					okButton.classList.add("custom-btn-modal"); 
					okButton.classList.add("btn-outline-danger");
					
					allGridButtons.forEach(btn => {
						const { segment: seg, rowIndex: ri, colIndex: ci, maxRows: mr, maxCols: mc } = btn.dataset;
						const i_ = parseInt(ri, 10);
						const j_ = parseInt(ci, 10);
						const rows_ = parseInt(mr, 10);
						const cols_ = parseInt(mc, 10);

						if (isNaN(i_) || isNaN(j_) || isNaN(rows_) || isNaN(cols_)) {
							return; // スキップ
						}

						const x_ = cols_ - j_ + 1;
						const y_ = rows_ - i_ + 1;

						const btnAddress = `${seg}${x_}${y_}`;

						// 選択されていないボタンは、使用状況に関わらず無効化・フェードする
						if (!selectedAddresses.has(btnAddress)) { 
							btn.disabled = true;
							btn.style.opacity = 0.5; 
						}
					});
					
				} else {
					okButton.disabled = true;
					okButton.classList.remove("custom-btn-modal");
					okButton.classList.remove("btn-outline-danger");
					okButton.classList.add("custom-btn-common");
					okButton.classList.add("custom-btn-common--notselectable");
					
					allGridButtons.forEach(btn => {
						const { segment: seg, rowIndex: ri, colIndex: ci, maxRows: mr, maxCols: mc } = btn.dataset;
						const i_ = parseInt(ri, 10);
						const j_ = parseInt(ci, 10);
						const rows_ = parseInt(mr, 10);
						const cols_ = parseInt(mc, 10);
						
						if (isNaN(i_) || isNaN(j_) || isNaN(rows_) || isNaN(cols_)) {
							return; // スキップ
						}

						const x_ = cols_ - j_ + 1;
						const y_ = rows_ - i_ + 1;

						const btnAddress = `${seg}${x_}${y_}`;
						
						// 選択解除時は、すべてのボタンの opacity を 1.0 に戻す
						btn.style.opacity = 1.0; 
						
						// 使用数が 2 未満のボタンのみ disabled = false にする (赤ボタンは disabled = true のまま)
						if ((storageUsageCounts[btnAddress] || 0) < 2) { 
							btn.disabled = false;
						}
					});
				}
			});
		});


		// --- 2-3. OKボタンのロジック ---
		okButton.addEventListener('click', () => {
			const storageLocationInput = document.getElementById('storageLocation');
			if (storageLocationInput) {
				const addressString = Array.from(selectedAddresses).join(',');
				storageLocationInput.value = addressString;
			} else {
				console.error('ID "storageLocation" の input が見つかりません。');
			}
		});
		// --- 2-3. OKボタンここまで ---


		// --- 2-4. モーダル表示時のリセット処理 ---
		toolAssignmentModalEl.addEventListener('show.bs.modal', () => {
			showSegment(0);
			selectedAddresses.clear();
			selectedButtons.clear();
			
			const countInput = document.getElementById('storageCount');
			requiredSelections = parseInt(countInput.value, 10);
			if (isNaN(requiredSelections) || requiredSelections < 1 || requiredSelections > 5) {
				requiredSelections = 1;
				countInput.value = 1;
			}
			messageElement.textContent = `コンテナ選択 残り ${requiredSelections}個`;
			
			okButton.disabled = true;
			okButton.classList.remove("custom-btn-modal");
			okButton.classList.remove("btn-outline-danger");
			okButton.classList.add("custom-btn-common");
			okButton.classList.add("custom-btn-common--notselectable");
			
			allGridButtons.forEach(button => {
				button.classList.remove('btn-primary');
				button.classList.remove('active');
				button.classList.remove('btn-secondary'); 
				// 色分けクラスを一旦すべて削除
				button.classList.remove('btn-outline-secondary');
				
				button.disabled = false;
				
				button.style.opacity = 1.0; 
				
				// スタイルをリセット (背景色など)
				button.style.backgroundColor = '';
				button.style.color = '';
				button.style.borderColor = '';
				
				const { segment, rowIndex, colIndex, maxRows, maxCols } = button.dataset;
				const i = parseInt(rowIndex, 10);
				const j = parseInt(colIndex, 10);
				const rows = parseInt(maxRows, 10);
				const cols = parseInt(maxCols, 10);

				if (isNaN(i) || isNaN(j) || isNaN(rows) || isNaN(cols)) {
					return; // スキップ
				}
				
				const x = cols - j + 1;
				const y = rows - i + 1;
				
				const address = `${segment}${x}${y}`;

				const currentUsage = storageUsageCounts[address] || 0;

				// 新しい色分けロジック (CSS変数使用)
				if (currentUsage === 0) {
					// 0個: デフォルト (白背景/黒文字/黒枠線)
					button.style.borderColor = 'var(--color-black)';
				} else if (currentUsage === 1) {
					button.style.backgroundColor = 'var(--color-orange)'; // 1個: 橙
					button.style.color = 'var(--color-white)'; 
					button.style.borderColor = 'var(--color-black)';
				} else { // 2個以上
					button.disabled = true;
					button.style.backgroundColor = 'var(--color-red)'; // 2個以上: 赤
					button.style.color = 'var(--color-white)';
					button.style.borderColor = 'var(--color-black)';
				}
			});
		});
		// --- 2-4. モーダル表示リセットここまで ---
	}
	// 2. モーダルロジックここまで


	// 3. 基本工具一覧の行選択・操作ボタン制御
			
	let selectedToolId = (typeof initialSelectedId !== 'undefined' && initialSelectedId !== null) ? initialSelectedId : null;
	// 修正: 装置外保管工具(stc, ropがnull)かどうかのフラグを追加
	let isSelectedToolExternal = false;

	const deleteButton = document.getElementById("deleteButton");
	const editButton = document.getElementById("editButton");
	const individualToolAddButton = document.getElementById("individualToolAddButton");
	const individualToolDetailButton = document.getElementById("individualToolDetailButton");
	// 修正: QRコード再印刷ボタンを取得
	const reprintQrButton = document.getElementById("reprintQrButton");

	const toolsTable = document.getElementById("toolsTable");
	const toolsTableBody = toolsTable ? toolsTable.querySelector("tbody") : null;

	let allToolRows = [];
	if (toolsTableBody) {
		allToolRows = Array.from(toolsTableBody.querySelectorAll("tr[data-tool-id]"));
	}


	/**
	 * ボタンの状態と見た目を更新する
	 */
	function updateButtonStates() {
		const isToolSelected = selectedToolId !== null;
		
		// 装置外保管工具(stc/ropがnull)の場合は、編集・個別登録・詳細・再印刷を不可にする
		// ※ 削除(deleteButton)については指定がないため、選択されていれば有効のままにします
		const isActionAllowed = isToolSelected && !isSelectedToolExternal;

		if (deleteButton) {
			deleteButton.disabled = !isToolSelected;
			deleteButton.classList.toggle("custom-btn-common--notselectable", !isToolSelected);
		}
		if (editButton) {
			editButton.disabled = !isActionAllowed;
			editButton.classList.toggle("custom-btn-common--notselectable", !isActionAllowed);
		}
		if (individualToolAddButton) {
			individualToolAddButton.disabled = !isActionAllowed;
			individualToolAddButton.classList.toggle("custom-btn-common--notselectable", !isActionAllowed);
		}
		if (individualToolDetailButton) {
			individualToolDetailButton.disabled = !isActionAllowed;
			individualToolDetailButton.classList.toggle("custom-btn-common--notselectable", !isActionAllowed);
		}
		// QRコード再印刷ボタン: 未選択時は有効、装置外工具選択時は無効
		if (reprintQrButton) {
			// 「選択されている かつ 装置外工具」の場合のみ無効化
			const shouldDisableQr = isToolSelected && isSelectedToolExternal;
			reprintQrButton.disabled = shouldDisableQr;
			reprintQrButton.classList.toggle("custom-btn-common--notselectable", shouldDisableQr);
		}
	}

	/**
	 * テーブルの行数が最低行数に満たない場合、空行を追加する
	 */
	function adjustTableRows(minRows) {
		if (!toolsTableBody) return;
		
		const existingEmptyRows = toolsTableBody.querySelectorAll("tr:not([data-tool-id])");
		existingEmptyRows.forEach(row => row.remove());

		const currentRowCount = toolsTableBody.querySelectorAll("tr[data-tool-id]").length;
		
		if (currentRowCount < minRows) {
			const cellsInRow = 7; 
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = toolsTableBody.insertRow(); 
				newRow.classList.add("selectable-row"); 
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
		}
	}

	// --- テーブル行のクリックイベント設定 ---
	if (toolsTable) {
		toolsTable.addEventListener("click", function (event) {
			const clickedRow = event.target.closest("tr");
			if (!clickedRow || !clickedRow.closest("tbody")) return;
			
			const toolIdStr = clickedRow.dataset.toolId; 

			// --- 3-1. 空行クリック時 (選択解除) ---
			if (!toolIdStr) {
				if (selectedToolId === null) return; 

				toolsTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
				selectedToolId = null;
				isSelectedToolExternal = false; // フラグ・リセット
				updateButtonStates(); 
				return;
			}

			// --- 3-2. データ行クリック時 ---
			const newSelectedId = parseInt(toolIdStr.trim(), 10);

			if (selectedToolId === newSelectedId) {
				return;
			}

			// --- 3-3. 選択状態の更新 (ちらつき無し) ---
			selectedToolId = newSelectedId;

			// rop(5番目)とstc(6番目)のセルを取得して空文字(=null)判定
			const ropCellText = clickedRow.cells[5].textContent.trim();
			const stcCellText = clickedRow.cells[6].textContent.trim();
			isSelectedToolExternal = (ropCellText === '' && stcCellText === '');

			toolsTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			clickedRow.classList.add("custom-list--selected");

			updateButtonStates();

			// モーダル用のデータを設定
			const cells = clickedRow.cells;
			const rowData = {
				category: cells[0].textContent.trim(),
				maker: cells[1].textContent.trim(),
				toolName: cells[2].textContent.trim(),
				material: cells[3].textContent.trim(),
				rop: cells[5].textContent.trim(),
				stock: cells[6].textContent.trim()
			};
			// (削除モーダル)
			const deleteInput = document.getElementById("deleteBasicToolIdInput");
			if(deleteInput) deleteInput.value = selectedToolId;
			// (編集モーダル)
			const editInput = document.getElementById("editBasicToolIdInput");
			if(editInput) editInput.value = selectedToolId;
			const editCategory = document.getElementById("editModal-category");
			if(editCategory) editCategory.textContent = rowData.category;
			const editMaker = document.getElementById("editModal-maker");
			if(editMaker) editMaker.textContent = rowData.maker;
			const editToolName = document.getElementById("editModal-toolName");
			if(editToolName) editToolName.textContent = rowData.toolName;
			const editMaterial = document.getElementById("editModal-material");
			if(editMaterial) editMaterial.textContent = rowData.material;
			const editStock = document.getElementById("editModal-stock");
			if(editStock) editStock.textContent = rowData.stock;
			
			const editRop = document.getElementById("orderpoint"); 
			if(editRop) editRop.value = rowData.rop;
			
			// (個別登録モーダル)
			const addModal = document.getElementById("toolsaddModal"); 
			if(addModal) addModal.dataset.category = rowData.category; 
			const addInput = document.getElementById("addModalBasicToolIdInput");
			if(addInput) addInput.value = selectedToolId;
			
			const addCategory = document.getElementById("addModal-category");
			if(addCategory) addCategory.textContent = rowData.category;
			const addMaker = document.getElementById("addModal-maker");
			if(addMaker) addMaker.textContent = rowData.maker;
			const addToolName = document.getElementById("addModal-toolName");
			if(addToolName) addToolName.textContent = rowData.toolName;
			const addMaterial = document.getElementById("addModal-material");
			if(addMaterial) addMaterial.textContent = rowData.material;
			
			
			// --- 3-4. Ajaxで個別工具詳細データを非同期取得 ---
			fetch(`/tool/details?basicToolId=${selectedToolId}`)
				.then(response => {
					if (!response.ok) {
						throw new Error('ネットワーク応答が正しくありません');
					}
					return response.json(); 
				})
				.then(individualToolList => {
					const detailBody = document.getElementById("individualToolListBody");
					if(detailBody) {
						detailBody.innerHTML = ""; 
						
						// データ行の追加
						if (individualToolList && individualToolList.length > 0) {
							individualToolList.forEach(indTool => {
								const newRow = detailBody.insertRow();
								newRow.innerHTML = `
									<td class="text-center">${indTool.uniqueToolId}</td>
									<td class="text-center">${indTool.casePackNum}</td>
									<td class="text-center">${indTool.regrindCount}</td>
									<td class="text-center">${indTool.storageCondition}</td>
								`;
							});
						}
						
						// 空行の追加（常に15行になるように調整）
						const currentRowCount = detailBody.rows.length;
						const minRows = 15;
						if (currentRowCount < minRows) {
							for (let i = 0; i < minRows - currentRowCount; i++) {
								const newRow = detailBody.insertRow();
								newRow.innerHTML = `
									<td class="text-center">&nbsp;</td>
									<td class="text-center">&nbsp;</td>
									<td class="text-center">&nbsp;</td>
									<td class="text-center">&nbsp;</td>
								`;
							}
						}
					}
				})
				.catch(error => {
					console.error('個別工具データの取得に失敗しました:', error);
					const detailBody = document.getElementById("individualToolListBody");
					if(detailBody) {
						detailBody.innerHTML = `<tr><td colspan="4" class="text-center text-danger">データの取得に失敗しました</td></tr>`;
					}
				});
		});
	}

	// --- 初期化処理 ---
	// 初期選択IDがある場合の判定ロジック追加
	if (selectedToolId !== null && toolsTableBody) {
		const selectedRow = toolsTableBody.querySelector(`tr[data-tool-id="${selectedToolId}"]`);
		if (selectedRow) {
			const ropCellText = selectedRow.cells[5].textContent.trim();
			const stcCellText = selectedRow.cells[6].textContent.trim();
			isSelectedToolExternal = (ropCellText === '' && stcCellText === '');
		}
	}
	
	updateButtonStates(); 
	adjustTableRows(12);
	
	if (selectedToolId !== null && toolsTableBody) {
		const selectedRow = toolsTableBody.querySelector(`tr[data-tool-id="${selectedToolId}"]`);
		if (selectedRow) {
			selectedRow.classList.add("custom-list--selected");
			
			const cells = selectedRow.cells;
			const rowData = {
				category: cells[0].textContent.trim(),
				maker: cells[1].textContent.trim(),
				toolName: cells[2].textContent.trim(),
				material: cells[3].textContent.trim(),
				trader: cells[4].textContent.trim(),
				rop: cells[5].textContent.trim(),
				stock: cells[6].textContent.trim()
			};

			const deleteInput = document.getElementById("deleteBasicToolIdInput");
			if(deleteInput) deleteInput.value = selectedToolId;
			const editInput = document.getElementById("editBasicToolIdInput");
			if(editInput) editInput.value = selectedToolId;
			const editCategory = document.getElementById("editModal-category");
			if(editCategory) editCategory.textContent = rowData.category;
			const editMaker = document.getElementById("editModal-maker");
			if(editMaker) editMaker.textContent = rowData.maker;
			const editToolName = document.getElementById("editModal-toolName");
			if(editToolName) editToolName.textContent = rowData.toolName;
			const editMaterial = document.getElementById("editModal-material");
			if(editMaterial) editMaterial.textContent = rowData.material;
			const editStock = document.getElementById("editModal-stock");
			if(editStock) editStock.textContent = rowData.stock;
			const editRop = document.getElementById("orderpoint"); 
			if(editRop) editRop.value = rowData.rop;
			
			const addModal = document.getElementById("toolsaddModal"); 
			if(addModal) addModal.dataset.category = rowData.category; 
			
			const addInput = document.getElementById("addModalBasicToolIdInput");
			if(addInput) addInput.value = selectedToolId;
			const addCategory = document.getElementById("addModal-category");
			if(addCategory) addCategory.textContent = rowData.category;
			const addMaker = document.getElementById("addModal-maker");
			if(addMaker) addMaker.textContent = rowData.maker;
			const addToolName = document.getElementById("addModal-toolName");
			if(addToolName) addToolName.textContent = rowData.toolName;
			const addMaterial = document.getElementById("addModal-material");
			if(addMaterial) addMaterial.textContent = rowData.material;
		}
	}
	// 3. 行選択処理ここまで
	
	
	// 4. 型番リアルタイム検索処理
	const searchInput = document.getElementById('toolNameSearchInput');
		
	if (searchInput && toolsTableBody) {
			
		searchInput.addEventListener('keyup', () => {
			const searchText = searchInput.value.toLowerCase().trim();
				
			toolsTableBody.innerHTML = '';
			
			allToolRows.forEach(row => {
				const toolNameCell = row.cells[2];
				const toolName = toolNameCell ? toolNameCell.textContent.toLowerCase() : '';
				
				if (toolName.includes(searchText)) {
					toolsTableBody.appendChild(row);
				}
			});
				
			if (selectedToolId !== null) {
				selectedToolId = null;
				isSelectedToolExternal = false; // 検索時リセット
				updateButtonStates();
			}

			adjustTableRows(12); 
		});
	}
	// 4. 型番検索処理ここまで
	
	// 5. 個別工具登録モーダルロジック (ToolManagementModals.html より移植)
	const toolsAddModal = document.getElementById('toolsaddModal');

	const form = document.getElementById('createIndividualToolForm');
	const submitBtn = document.getElementById('createIndividualToolBtn');
	
	// Ajax 送信 click リスナー
	if (form && submitBtn && toolsAddModal) {
		submitBtn.addEventListener('click', async function(event) {
			event.preventDefault(); // デフォルトのフォーム送信をキャンセル
			
			if (!form.checkValidity()) {
				form.reportValidity();
				return;
			}
			
			submitBtn.disabled = true;
			submitBtn.innerHTML = '登録中...';
			
			const formData = new URLSearchParams(new FormData(form));
	
			try {
				const response = await fetch('/tool/createIndividual', {
					method: 'POST',
					headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
					body: formData
				});
	
				if (!response.ok) {
					// エラーレスポンス (JSON形式を想定) を解析
					const errorData = await response.json().catch(() => ({ error: 'サーバーエラーが発生しました。' }));
					throw new Error(errorData.error || '個別工具の登録に失敗しました。');
				}
	
				const result = await response.json(); // { uniqueToolId: ... }
	
				const modal = bootstrap.Modal.getInstance(toolsAddModal);
				modal.hide();
	
				// 登録成功後、印刷を実行
				if (result.uniqueToolId && typeof printQrCode === 'function') {
					try {
						// サーバーがテストモードなら物理印刷はスキップされ、正常終了する
						await printQrCode('tool', result.uniqueToolId);
						// 印刷成功時はリロード
						location.reload();
					} catch (printErr) {
						console.error("印刷失敗:", printErr);
						
						// 印刷失敗時は常にロールバックを行う
						alert("QRコードの印刷に失敗したため、登録をキャンセルしました。\n" + printErr.message);
						
						await fetch('/tool/deleteIndividual', {
							method: 'POST',
							headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
							body: new URLSearchParams({ uniqueToolId: result.uniqueToolId })
						});
						
						// リロードせず、モーダルも閉じた状態
					}
				} else {
					console.warn('printQrCode関数が見つからないか、uniqueToolIdがレスポンスに含まれていません。');
					location.reload(); // 印刷不要ならそのままリロード
				}
	
			} catch (err) {
				console.error("登録失敗:", err);
				alert("個別工具の登録中にエラーが発生しました。\n" + err.message);
				// エラー時はボタンを元に戻す
				submitBtn.disabled = false;
				submitBtn.innerHTML = 'OK';
			}
		});
	}

	if (toolsAddModal) {
		
		toolsAddModal.addEventListener('show.bs.modal', function () {
			const category = toolsAddModal.dataset.category;
			const insertCountSection = document.getElementById('insertCountSection');
			
			// --- 追加: モーダル表示開始時に見た目をリセット ---
			const numberBox = document.getElementById('numberBox');
			const insertCountInput = document.getElementById('insertCountInput');
			if (numberBox) numberBox.textContent = '1';
			if (insertCountInput) insertCountInput.value = '1';

			if (category === 'インサート' || category === 'チップ') {
				insertCountSection.style.display = 'flex';
			} else {
				insertCountSection.style.display = 'none';
			}
		});


		toolsAddModal.addEventListener('shown.bs.modal', function () {
			
			if (submitBtn) {
				submitBtn.disabled = false;
				submitBtn.innerHTML = 'OK';
			}

			const increaseBtn = document.getElementById('increaseBtn');
			const decreaseBtn = document.getElementById('decreaseBtn');
			const numberBox = document.getElementById('numberBox');
			const insertCountInput = document.getElementById('insertCountInput');

			let count = 1;
			// ここでのリセットは念のため残しておきますが、
			// 見た目はすでに show.bs.modal で 1 になっています。
			numberBox.textContent = count;
			insertCountInput.value = count;
			
			const newIncreaseBtn = increaseBtn.cloneNode(true);
			increaseBtn.parentNode.replaceChild(newIncreaseBtn, increaseBtn);

			const newDecreaseBtn = decreaseBtn.cloneNode(true);
			decreaseBtn.parentNode.replaceChild(newDecreaseBtn, decreaseBtn);

			newIncreaseBtn.addEventListener('click', function() {
				count++;
				numberBox.textContent = count;
				insertCountInput.value = count;
			});

			newDecreaseBtn.addEventListener('click', function() {
				if (count > 1) { 
					count--;
					numberBox.textContent = count;
					insertCountInput.value = count;
				}
			});
		});
	}
	// 5. 個別工具登録モーダルロジックここまで
	
	// 6. QRコード再印刷処理
	const reprintModalEl = document.getElementById('printqrcodeModal');
	const reprintOkButton = document.getElementById('reprintQrOkButton');
	const qrNumberInput = document.getElementById('qrNumber'); // 手入力用
	const qrScanInput = document.getElementById('qrScanInput'); // QRスキャン用
	
	let reprintDebounceTimeout; // QRスキャン用デバウンスタイマー

	if (reprintModalEl && reprintOkButton && qrNumberInput && qrScanInput) {
		
		const reprintModal = new bootstrap.Modal(reprintModalEl);
		
		/**
		 * 9桁の工具コード(16進数)を受け取り、再印刷Ajaxリクエストを実行する共通関数
		 * @param {string} qrNumber - 9桁の工具ID (16進数)
		 * @returns {Promise<boolean>} 処理が成功し印刷が実行された場合は true
		 */
		async function executeReprint(qrNumber) {
			// バリデーション: 9桁の16進数
			if (qrNumber.length !== 9 || !/^[0-9A-Fa-f]{9}$/.test(qrNumber)) {
				alert('9桁の16進数を入力してください。');
				return false;
			}

			// OKボタン（手入力用）があれば無効化
			if (reprintOkButton) {
				reprintOkButton.disabled = true;
				reprintOkButton.textContent = '処理中...';
			}

			try {
				// タイムスタンプを常に更新する設定でリクエスト
				const updateTimestamp = true;
				
				const response = await fetch('/tool/reprintQrAjax', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/x-www-form-urlencoded',
					},
					body: `qrNumber=${encodeURIComponent(qrNumber)}&updateTimestamp=${updateTimestamp}`
				});

				if (response.ok) {
					const result = await response.json();
					if (result.uniqueToolId) {
						
						// 印刷実行
						if (typeof printQrCode === 'function') {
							try {
								// サーバーがテストモードなら物理印刷はスキップされ、正常終了する
								await printQrCode('tool', result.uniqueToolId);
								
								// 印刷成功時はリロード
								location.reload();
								return true; // 成功
							} catch (printErr) {
								console.error("再印刷時の印刷処理に失敗:", printErr);
								
								// 印刷エラー時はアラートを出して終了（ロールバック不要または実装困難なため）
								alert("QRコードの再印刷に失敗したため、更新をキャンセルしました。");
								return false;
							}
						} else {
							alert('QRコード印刷機能の読み込みに失敗しました。');
							return false;
						}
					} else if (result.error) {
						alert(`エラー: ${result.error}`);
					} else {
						alert('不明なエラーが発生しました。');
					}
				} else {
					// サーバーエラー (404, 500など)
					const errorText = await response.text();
					alert(`サーバーエラーが発生しました: ${errorText}`);
				}

			} catch (error) {
				// ネットワークエラーなど
				console.error('QRコード再印刷リクエスト中にエラー:', error);
				alert(`通信エラーが発生しました: ${error.message}`);
			} finally {
				// OKボタン（手入力用）があれば元に戻す
				if (reprintOkButton) {
					reprintOkButton.disabled = false;
					reprintOkButton.textContent = 'OK';
				}
			}
			return false; // 失敗
		}
		
		// --- (A) 手入力用のOKボタンの処理 ---
		reprintOkButton.addEventListener('click', async () => {
			const qrNumber = qrNumberInput.value;
			const success = await executeReprint(qrNumber);
			
			if (success) {
				reprintModal.hide(); // 成功時のみモーダルを閉じる
			} else {
				// 失敗時は手入力欄をクリアし、フォーカスする
				qrNumberInput.value = '';
				qrNumberInput.focus();
			}
		});
		
		// --- (B) QRスキャン用の input イベント処理 ---
		qrScanInput.addEventListener('input', function(e) {
			clearTimeout(reprintDebounceTimeout);
			
			const qrCodeData = qrScanInput.value;
			
			// 工具QRコードの形式 (Tで始まり、-を含む) かチェック
			if (!qrCodeData.startsWith('T') || !qrCodeData.includes('-')) {
				return; // 形式が違う場合は何もしない
			}

			// 300ミリ秒後に入力がなければ、入力完了とみなして処理を開始
			reprintDebounceTimeout = setTimeout(async () => {
				try {
					// QRコードデータ (例: T04995F2D1-2025...) から 9桁のID部分を抽出
					const toolIdentifier = qrCodeData.substring(1).split('-')[0];
					
					if (toolIdentifier) {
						// 抽出した9桁IDで再印刷処理を実行
						const success = await executeReprint(toolIdentifier);
						
						if (success) {
							reprintModal.hide(); // 成功したらモーダルを閉じる
						} else {
							// 失敗した場合 (アラートは executeReprint 内で表示される)
							qrScanInput.value = ''; // スキャン入力をクリアして再スキャンを待つ
							qrScanInput.focus(); // 再度フォーカス
						}

					} else {
						throw new Error('IDの抽出に失敗');
					}
				} catch (err) {
					console.error("QRコード解析エラー:", err);
					alert('QRコードの形式が正しくありません。');
					qrScanInput.value = '';
					qrScanInput.focus();
				}

			}, 300);
		});
		
		// --- (C) モーダル表示時の処理 ---
		reprintModalEl.addEventListener('show.bs.modal', () => {
			// ... 省略 ...
		});
		
		// --- (D) モーダル非表示時の処理 ---
		reprintModalEl.addEventListener('hidden.bs.modal', () => {
			clearTimeout(reprintDebounceTimeout);
			qrNumberInput.value = '';
			qrScanInput.value = '';
		});
	}
	// 6. 再印刷処理ここまで

	// 7. 新規登録モーダル UI制御 (新規追加)
	const modelsAddModalEl = document.getElementById('modelsaddModal');
	if (modelsAddModalEl) {
		const storageCountSelect = document.getElementById('storageCount');
		const assignStorageBtn = document.getElementById('assignStorageBtn');
		const storageLocationInput = document.getElementById('storageLocation');
		const addToolForm = modelsAddModalEl.querySelector('form');
		const addToolSubmitBtn = document.getElementById('addToolSubmitBtn');

		if (storageCountSelect && assignStorageBtn && storageLocationInput && addToolSubmitBtn) {
			
			// 追加ボタンの状態と見た目を制御する関数
			const setAddButtonState = (enable) => {
				addToolSubmitBtn.disabled = !enable;
				if (enable) {
					// 有効化 (赤枠/ホバー可能)
					addToolSubmitBtn.classList.remove('custom-btn-common');
					addToolSubmitBtn.classList.remove('custom-btn-common--notselectable');
					addToolSubmitBtn.classList.add('custom-btn-modal');
					addToolSubmitBtn.classList.add('btn-outline-danger');
				} else {
					// 無効化 (グレー/選択不可)
					addToolSubmitBtn.classList.remove('custom-btn-modal');
					addToolSubmitBtn.classList.remove('btn-outline-danger');
					addToolSubmitBtn.classList.add('custom-btn-common');
					addToolSubmitBtn.classList.add('custom-btn-common--notselectable');
				}
			};

			const updateStorageUI = (shouldClearInput = false) => {
				const val = storageCountSelect.value;
				const ropInput = document.getElementById('rop');
				
				// stc (在庫数) の hidden input 制御
				let stcInput = addToolForm.querySelector('input[name="stc"]');
				if (!stcInput) {
					stcInput = document.createElement('input');
					stcInput.type = 'hidden';
					stcInput.name = 'stc';
					addToolForm.appendChild(stcInput);
				}
				
				if (val === '6') {
					// 装置外保管場所: 割当ボタン無効化、入力可
					assignStorageBtn.disabled = true;
					assignStorageBtn.classList.remove('custom-btn-modal');
					assignStorageBtn.classList.add('custom-btn-common');
					assignStorageBtn.classList.add('custom-btn-common--notselectable');

					ropInput.disabled = true;
					ropInput.value = '';
					ropInput.removeAttribute('required');
					
					// stcをnull (送信しない) に設定
					stcInput.disabled = true;
					stcInput.value = '';
					
					// 切り替え時に既存の自動割当アドレス(A11など)があればクリア
					if (shouldClearInput) {
						const currentVal = storageLocationInput.value;
						if (/^([ABC]\d{2})(,[ABC]\d{2})*$/.test(currentVal)) {
							storageLocationInput.value = "";
						}
					}
					
					storageLocationInput.readOnly = false;
					storageLocationInput.placeholder = "保管場所を入力…";

					// 【予約番号チェック】AXX, BXX, CXX なら追加ボタン無効化
					const locationVal = storageLocationInput.value.trim().toUpperCase();
					const isReserved = /^[ABC]\d{2}$/.test(locationVal);
					
					setAddButtonState(!isReserved); // 予約番号なら無効(false)、そうでなければ有効(true)

				} else {
					// コンテナ (1-5): 割当ボタン有効化、入力不可(readonly)
					assignStorageBtn.disabled = false;
					assignStorageBtn.classList.remove('custom-btn-common');
					assignStorageBtn.classList.remove('custom-btn-common--notselectable');
					assignStorageBtn.classList.add('custom-btn-modal');

					storageLocationInput.readOnly = true;
					storageLocationInput.placeholder = "";

					ropInput.disabled = false;
					ropInput.setAttribute('required', '');
					
					// stcを0で送信設定
					stcInput.disabled = false;
					stcInput.value = '0';
					
					// 装置外などからコンテナ保管へ切り替わった場合、入力値をクリア
					if (shouldClearInput) {
						storageLocationInput.value = "";
					}
					
					// 追加ボタンは常に有効
					setAddButtonState(true);
				}
			};

			// イベントリスナー追加
			storageCountSelect.addEventListener('change', () => {
				updateStorageUI(true); // 切り替え時は入力クリアを許可
			});
			
			// 保管場所入力時のリアルタイム監視
			storageLocationInput.addEventListener('input', () => {
				updateStorageUI(false); // 入力中はクリアしない
			});
			
			// モーダル表示時の初期化処理
			modelsAddModalEl.addEventListener('show.bs.modal', () => {
				updateStorageUI(false);
			});
			
			// フォーム送信時の念押しバリデーション
			if (addToolForm) {
				addToolForm.addEventListener('submit', function(e) {
					const locationVal = storageLocationInput.value.trim();

					if (storageCountSelect.value === '6') { // 6=装置外
						if (/^[ABC]\d{2}$/.test(locationVal.toUpperCase())) {
							e.preventDefault();
							alert("名称が装置内コンテナアドレスと重複しているため登録できません。");
						}
					} else {
						// コンテナ (1-5) の場合、格納場所が必須
						if (!locationVal) {
							e.preventDefault();
							alert("格納場所を選択してください。");
						}
					}
				});
			}
		}
	}
	// 7. 新規登録モーダル UI制御ここまで

	// 8. モーダル閉鎖時のフォームリセット処理 (新規追加)
	// 対象となるモーダルのIDを指定
	const modalsToReset = ['modelsaddModal', 'modelsfilteringModal']; 

	modalsToReset.forEach(modalId => {
		const modalEl = document.getElementById(modalId);
		if (modalEl) {
			modalEl.addEventListener('hidden.bs.modal', () => {
				// モーダル内のフォームを探してリセット
				const form = modalEl.querySelector('form');
				if (form) {
					form.reset();
				}
				// ※ ボタンの活性/非活性などの見た目のリセットは、
				//    各モーダルの show.bs.modal イベント側で処理されています。
			});
		}
	});

	// 9. 基本工具削除処理 (Ajax化)
	const deleteToolForm = document.getElementById('deleteToolForm');
	const executeDeleteBtn = document.getElementById('executeDeleteBtn');
	const deleteModalEl = document.getElementById('modelsdelModal');

	if (deleteToolForm && executeDeleteBtn && deleteModalEl) {
		deleteToolForm.addEventListener('submit', async function(event) {
			event.preventDefault(); // フォーム送信をキャンセル

			executeDeleteBtn.disabled = true;
			executeDeleteBtn.textContent = '削除中...';

			const formData = new FormData(deleteToolForm);

			try {
				const response = await fetch('/tool/disable', {
					method: 'POST',
					body: formData
				});

				if (!response.ok) {
					const errorData = await response.json().catch(() => ({ error: 'サーバーエラーが発生しました。' }));
					throw new Error(errorData.error || '削除に失敗しました。');
				}

				// 成功時
				const modal = bootstrap.Modal.getInstance(deleteModalEl);
				if (modal) modal.hide();
				
				alert('削除しました。');
				location.reload();

			} catch (err) {
				console.error("削除失敗:", err);
				alert("削除中にエラーが発生しました。\n" + err.message);
			} finally {
				executeDeleteBtn.disabled = false;
				executeDeleteBtn.textContent = '削除';
			}
		});
	}
	// 9. 基本工具削除処理ここまで
});