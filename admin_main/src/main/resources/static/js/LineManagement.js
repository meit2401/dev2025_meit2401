// LineManagement.js

document.addEventListener("DOMContentLoaded", function () {
	let selectedLineId = null;
	let selectedLineName = null;
	let selectedProgramId = null;
	let selectedToolId = null;
	let selectedBasicToolId = null; // モーダル内での選択工具ID

	const deleteLineButton = document.getElementById("delete-line-btn");
	const addProgramButton = document.getElementById("add-program-btn");
	const deleteProgramButton = document.getElementById("delete-program-btn");
	const allocateToolButton = document.getElementById("allocate-tool-btn");
	const deallocateToolButton = document.getElementById("deallocate-tool-btn");
	const reprintQrButton = document.getElementById("reprint-qr-btn");

	const lineTable = document.getElementById("lines-table");
	const programTable = document.getElementById("programs-table");
	const toolTable = document.getElementById("tools-table");
	const programTableBody = document.getElementById("programs-table-body");

	/**
	 * プログラムテーブルを更新し、最低7行を表示する
	 * @param {Array} programs - 表示するプログラムの配列 (DTOのリスト)
	 */
	function updateProgramTable(programs = []) {
		programTableBody.innerHTML = '';

		programs.forEach(program => {
			const row = programTableBody.insertRow();
			row.dataset.programId = program.programId;
			row.dataset.lineId = program.lineId;
			const cell = row.insertCell(0);
			cell.textContent = program.programName;
		});

		const rowsToPad = 7 - programs.length;
		if (rowsToPad > 0) {
			for (let i = 0; i < rowsToPad; i++) {
				const row = programTableBody.insertRow();
				row.insertCell(0).innerHTML = '&nbsp;';
			}
		}
	}
	
	/**
	 * ツール割当テーブルを更新する
	 * @param {Array} assignments - 表示する工具割当情報の配列
	 */
	function updateToolTable(assignments = []) {
		const toolTableBody = toolTable.querySelector("tbody");
		const rows = toolTableBody.querySelectorAll("tr");

		// [修正] 取得した工具情報を、ツール番号(TNN)をキーにしたMapに変換する
		// (DBの tool_num "T01", "T02" ... がキーになる)
		const assignmentMap = new Map(assignments.map(a => [a.toolNum, a]));

		rows.forEach(row => {
			const toolNumCell = row.cells[0];
			if (!toolNumCell) return;
			
			// [修正] HTMLの表示 ("T01", "T02") をDBのキー ("T01", "T02") として使用する
			const toolNumText = toolNumCell.textContent.trim(); // "T01", "T02" などを取得
			
			// [修正] N-1 変換を削除
			const lookupKey = toolNumText; 
			
			// [修正] 調整したキー ("T01", "T02"...) で Map からデータを検索
			const assignment = assignmentMap.get(lookupKey); 

			if (assignment) {
				row.cells[1].textContent = assignment.toolCategory || '';
				row.cells[2].textContent = assignment.maker || '';
				row.cells[3].textContent = assignment.toolName || '';
				row.cells[4].textContent = assignment.toolMaterial || '';
			} else {
				// 対応するデータがない場合はセルをクリアする
				row.cells[1].innerHTML = '&nbsp;';
				row.cells[2].innerHTML = '&nbsp;';
				row.cells[3].innerHTML = '&nbsp;';
				row.cells[4].innerHTML = '&nbsp;';
			}
		});
	}

	/**
	 * 工具割当モーダル内の候補テーブルを更新する
	 * @param {Array} tools - 表示する工具(Toolエンティティ)の配列
	 */
	function updateToolCandidatesTable(tools = []) {
		const toolCandidatesBody = document.getElementById("tool-candidates-body");
		if (!toolCandidatesBody) return;
		toolCandidatesBody.innerHTML = '';

		tools.forEach(tool => {
			const row = toolCandidatesBody.insertRow();
			// 選択時にIDを取得できるようdata属性に設定
			row.dataset.basicToolId = tool.basicToolId; 
			
			row.insertCell(0).textContent = tool.toolCategory || '';
			row.insertCell(1).textContent = tool.maker || '';
			row.insertCell(2).textContent = tool.toolName || '';
			row.insertCell(3).textContent = tool.toolMaterial || '';
		});

		// モーダルのHTMLは5行表示 (LineManagementModals.html 参照)
		const rowsToPad = 5 - tools.length; 
		if (rowsToPad > 0) {
			for (let i = 0; i < rowsToPad; i++) {
				const row = toolCandidatesBody.insertRow();
				row.insertCell(0).innerHTML = '&nbsp;';
				row.insertCell(1).innerHTML = ' '; // HTML の定義に合わせる
				row.insertCell(2).innerHTML = ' ';
				row.insertCell(3).innerHTML = ' ';
			}
		}
		
		// 選択状態をリセット
		selectedBasicToolId = null;
		const toolCandidatesTable = document.getElementById("tool-candidates-table");
		if (toolCandidatesTable) {
			toolCandidatesTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
		}
	}

	/**
	 * 選択されたプログラムの工具割当リストを再取得・再描画する
	 * @param {string | number | null} programId - 工具割当を取得するプログラムのID
	 */
	async function refreshToolList(programId) {
		if (!programId) {
			updateToolTable([]);
			return;
		}
		try {
			const response = await fetch(`/line/program/tools?programId=${programId}`);
			if (!response.ok) throw new Error('工具割当の取得に失敗しました。');
			const assignments = await response.json();
			updateToolTable(assignments);
		} catch (err) {
			console.error("工具割当取得エラー:", err);
			alert(err.message);
			updateToolTable([]);
		}
	}

	/**
	 * 選択されたラインのプログラムリストを再取得・再描画する
	 * @param {string | number} lineId - プログラムを取得するラインのID
	 */
	async function refreshProgramList(lineId) {
		if (!lineId) {
			updateProgramTable([]);
			return;
		}
		try {
			const response = await fetch(`/line/programs?lineId=${lineId}`);
			if (!response.ok) throw new Error('プログラムの取得に失敗しました。');
			const programs = await response.json();
			updateProgramTable(programs);
		} catch (err) {
			console.error("プログラム取得エラー:", err);
			alert(err.message);
			updateProgramTable([]);
		}
		selectedProgramId = null;
		selectedToolId = null;
		programTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
		toolTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
		updateButtonStates();
	}
	
	function updateButtonStates() {
		const isLineSelected = selectedLineId !== null;
		const isProgramSelected = selectedProgramId !== null;
		const isToolSelected = selectedToolId !== null;

		deleteLineButton.disabled = !isLineSelected;
		deleteLineButton.classList.toggle("custom-btn-common--notselectable", !isLineSelected);

		addProgramButton.disabled = !isLineSelected;
		addProgramButton.classList.toggle("custom-btn-common--notselectable", !isLineSelected);

		deleteProgramButton.disabled = !isProgramSelected;
		deleteProgramButton.classList.toggle("custom-btn-common--notselectable", !isProgramSelected);

		allocateToolButton.disabled = !isToolSelected;
		allocateToolButton.classList.toggle("custom-btn-common--notselectable", !isToolSelected);

		// 工具割当解除ボタンの状態更新
		if (deallocateToolButton) {
			deallocateToolButton.disabled = !isToolSelected;
			deallocateToolButton.classList.toggle("custom-btn-common--notselectable", !isToolSelected);
		}

		reprintQrButton.disabled = !isProgramSelected;
		reprintQrButton.classList.toggle("custom-btn-common--notselectable", !isProgramSelected);
	}

	/**
	 * テーブル行の選択処理
	 * @param {HTMLTableElement} currentTable - クリックされたテーブル
	 * @param {HTMLTableRowElement} clickedRow - クリックされた行
	 */
	async function handleRowSelection(currentTable, clickedRow) {
		const firstCell = clickedRow.cells[0];
		const isRowEmpty = !firstCell || firstCell.textContent.trim() === "";
	
		if (currentTable === lineTable) {
			programTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			toolTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			selectedProgramId = null;
			selectedToolId = null;
	
			lineTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			if (!isRowEmpty) {
				clickedRow.classList.add("custom-list--selected");
				selectedLineId = clickedRow.dataset.lineId ?? null;
				selectedLineName = firstCell.textContent.trim();
				
				await refreshProgramList(selectedLineId);
				await refreshToolList(null); // ライン選択時はツールリストをクリア
			} else {
				selectedLineId = null;
				selectedLineName = null;
				await refreshToolList(null); // 空白行選択時はツールリストをクリア
			}
	
		} else if (currentTable === programTable) {
			toolTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			selectedToolId = null;
	
			programTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			if (!isRowEmpty) {
				clickedRow.classList.add("custom-list--selected");
				selectedProgramId = clickedRow.dataset.programId ?? null;
				
				await refreshToolList(selectedProgramId); // プログラム選択時にツールリストを更新

				const parentLineId = clickedRow.dataset.lineId;
				if (parentLineId) {
					// 対応するラインの行を探す
					const lineRowToSelect = lineTable.querySelector(`tr[data-line-id="${parentLineId}"]`);
					if (lineRowToSelect) {
						// ラインの選択状態を更新
						lineTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
						lineRowToSelect.classList.add("custom-list--selected");
						// グローバル変数も更新
						selectedLineId = parentLineId;
						selectedLineName = lineRowToSelect.cells[0].textContent.trim();
					}
				}
			} else {
				selectedProgramId = null;
				await refreshToolList(null); // 空白行選択時はツールリストをクリア
			}
	
		} else if (currentTable === toolTable) {
			// プログラムが選択されていない場合は、ツールリストの行を選択できないようにする
			if (selectedProgramId === null) {
				return;
			}
			toolTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
			if (!isRowEmpty) {
				clickedRow.classList.add("custom-list--selected");
				selectedToolId = firstCell.textContent.trim();
			} else {
				selectedToolId = null;
			}
		}
	
		updateButtonStates();
	}
	
	[lineTable, programTable, toolTable].forEach(table => {
		if (table) {
			table.addEventListener("click", function (event) {
				const clickedRow = event.target.closest("tr");
				if (clickedRow && clickedRow.closest("tbody")) {
					handleRowSelection(table, clickedRow);
				}
			});
		}
	});
	
	/* ここから工具割当モーダルのロジックを追加 */
		
	const filterToolsButton = document.getElementById("filter-tools-btn");
	const toolCandidatesTable = document.getElementById("tool-candidates-table");
	const assignToolConfirmButton = document.getElementById("assign-tool-confirm-btn");

	// 絞り込みボタン
	if (filterToolsButton) {
		filterToolsButton.addEventListener("click", async function () {
			const category = document.getElementById("filterCategory").value;
			const maker = document.getElementById("filterMaker").value;
			const toolName = document.getElementById("filterToolName").value;
			const material = document.getElementById("filterMaterial").value;

			const params = new URLSearchParams();
			// "" (すべて) の場合は、JSの if(category) が false となる
			// パラメータが送信されず、Controller で null となる
			// Service は null と "" の両方で絞り込みをスキップするため、正しく動作する
			if (category) params.append('category', category);
			if (maker) params.append('maker', maker);
			if (toolName) params.append('toolName', toolName);
			if (material) params.append('material', material);

			try {
				const response = await fetch(`/line/tools/filter?${params.toString()}`);
				if (!response.ok) {
					throw new Error('工具の絞り込みに失敗しました。');
				}
				const tools = await response.json();
				updateToolCandidatesTable(tools); // 候補テーブルを更新
			} catch (err) {
				console.error("絞り込みエラー:", err);
				alert(err.message);
			}
		});
	}

	// 候補テーブルの行選択
	if (toolCandidatesTable) {
		toolCandidatesTable.addEventListener("click", function (event) {
			const clickedRow = event.target.closest("tr");
			if (!clickedRow || !clickedRow.closest("tbody")) return;

			const firstCell = clickedRow.cells[0];
			// HTMLの定義 に合わせて &nbsp; も空行とみなす
			const isRowEmpty = !firstCell || firstCell.textContent.trim() === "" || firstCell.innerHTML.trim() === "&nbsp;";

			toolCandidatesTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));

			if (!isRowEmpty) {
				clickedRow.classList.add("custom-list--selected");
				selectedBasicToolId = clickedRow.dataset.basicToolId; // data属性からIDを取得
			} else {
				selectedBasicToolId = null;
			}
		});
	}

	// 確定ボタン
	if (assignToolConfirmButton) {
		assignToolConfirmButton.addEventListener("click", async function () {
			if (selectedProgramId === null) {
				alert("割当先のプログラムが選択されていません。");
				return;
			}
			if (selectedToolId === null) {
				alert("割当先のツール番号(T01, T02...)が選択されていません。");
				return;
			}
			if (selectedBasicToolId === null) {
				alert("割り当てる工具を候補から選択してください。");
				return;
			}

			// [修正] T01 -> 0 への変換を削除
			// selectedToolId ("T01", "T02"...) が Service が期待する toolNum
			const toolNumStr = selectedToolId; 

			const formData = new URLSearchParams();
			formData.append('programId', selectedProgramId);
			formData.append('toolNum', toolNumStr); // [修正]
			formData.append('basicToolId', selectedBasicToolId);

			try {
				// Controllerの /line/program/tool/assign を呼び出す
				const response = await fetch("/line/program/tool/assign", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: formData
				});

				if (!response.ok) {
					throw new Error("工具の割り当てに失敗しました。");
				}

				const modal = bootstrap.Modal.getInstance(document.getElementById("allocationModal"));
				modal.hide();

				// 割当完了後、メインのツールリストを再描画
				await refreshToolList(selectedProgramId);

			} catch (err) {
				console.error("割当失敗:", err);
				alert(err.message);
			}
		});
	}
	/* ここまで工具割当モーダルのロジック */

	function setupModalTextUpdates() {
		const linedelModal = document.getElementById('linedelModal');
		if (linedelModal) {
			linedelModal.addEventListener('show.bs.modal', function () {
				const modalBody = linedelModal.querySelector('.modal-body');
				if (modalBody && selectedLineName) {
					modalBody.innerHTML = `ライン名「${selectedLineName}」及び、<br>それに紐づくプログラム番号、<br>工具割当情報の削除を行います`;
				}
			});
		}

		const progdelModal = document.getElementById('progdelModal');
		if (progdelModal) {
			progdelModal.addEventListener('show.bs.modal', function () {
				const modalBody = progdelModal.querySelector('.modal-body');
				if (modalBody && selectedProgramId) {
					modalBody.innerHTML = `プログラム番号${selectedProgramId} 及び、<br>それに紐づく工具割当情報の<br>削除を行います`;
				}
			});
		}
		
		const allocationModal = document.getElementById('allocationModal');
		if (allocationModal) {
			allocationModal.addEventListener('show.bs.modal', function () {
				// 既存のタイトル更新処理
				const modalTitle = document.getElementById('allocationModalTitle'); // ID でタイトル部分を特定
				if (modalTitle && selectedToolId) {
					modalTitle.innerHTML = `ツール番号${selectedToolId} に<br>工具を登録します`;
				}
				
				/* モーダル表示時に内部の状態をリセット */
				selectedBasicToolId = null;
				updateToolCandidatesTable([]); // テーブルをクリア

				// フォーム入力値もリセット
				document.getElementById('filterCategory').value = '';
				document.getElementById('filterMaker').value = '';
				document.getElementById('filterToolName').value = '';
				document.getElementById('filterMaterial').value = '';
			});
		}

		// インポートモーダルの表示時処理
		const importModal = document.getElementById('importModal');
		if (importModal) {
			importModal.addEventListener('show.bs.modal', function () {
				// フォームをリセット (ファイル選択をクリア)
				const form = document.getElementById("import-form");
				if (form) {
					form.reset();
				}
			});
		}
		
		// 割当解除モーダルのテキスト更新
		const deallocationModal = document.getElementById('deallocationModal');
		if (deallocationModal) {
			deallocationModal.addEventListener('show.bs.modal', function () {
				const modalBody = deallocationModal.querySelector('#deallocationModalBody');
				if (modalBody && selectedToolId) {
					modalBody.innerHTML = `ツール番号${selectedToolId} の<br>工具割り当てを解除します`;
				}
			});
		}

		const lineprintqrcodeModal = document.getElementById('lineprintqrcodeModal');
		if (lineprintqrcodeModal) {
			lineprintqrcodeModal.addEventListener('show.bs.modal', function () {
				const modalBody = lineprintqrcodeModal.querySelector('.modal-body');
				if (modalBody && selectedProgramId) {
					modalBody.innerHTML = `プログラム番号${selectedProgramId} の<br>QRコードを再印刷します`;
				}
			});
		}
	}

	const addOkButton = document.querySelector("#lineaddModal .btn-danger");
	if (addOkButton) {
		addOkButton.addEventListener("click", async function (event) {
			event.preventDefault();
			const addModal = document.getElementById("lineaddModal");
			const form = addModal.querySelector("form");
			if (!form.checkValidity()) {
				form.reportValidity();
				return;
			}
			const formData = new URLSearchParams(new FormData(form));
			try {
				const response = await fetch("/line/add", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: formData
				});
				if (!response.ok) {
					throw new Error('ライン登録に失敗しました。');
				}
				location.reload();
			} catch (err) {
				console.error("登録失敗:", err);
				alert("ライン登録中にエラーが発生しました。");
			}
		});
	}

	const delOkButton = document.querySelector("#linedelModal .btn-danger");
	if (delOkButton) {
		delOkButton.addEventListener("click", function () {
			if (selectedLineId === null) {
				alert("削除するラインを選択してください。");
				return;
			}
			fetch("/line/delete", {
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams({ lineId: selectedLineId })
			})
			.then(res => {
				if (!res.ok) throw new Error("削除に失敗しました。");
				return res.text();
			})
			.then(msg => {
				console.log(msg);
				location.reload();
			})
			.catch(err => {
				console.error("削除失敗:", err);
				alert(err.message);
			});
		});
	}

	const addProgramOkButton = document.querySelector("#progaddModal .btn-danger");
	if (addProgramOkButton) {
		addProgramOkButton.addEventListener("click", async function (event) {
			event.preventDefault();
			if (selectedLineId === null) {
				alert("プログラムを追加するラインを選択してください。");
				return;
			}
			const addModal = document.getElementById("progaddModal");
			const form = addModal.querySelector("form");
			if (!form.checkValidity()) {
				form.reportValidity();
				return;
			}
			const formData = new URLSearchParams(new FormData(form));
			formData.append('lineId', selectedLineId);
			try {
				const response = await fetch("/line/program/add", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: formData
				});
				if (!response.ok) {
					throw new Error('プログラムの登録に失敗しました。');
				}
				
				const newProgram = await response.json();
				const modal = bootstrap.Modal.getInstance(addModal);
				modal.hide();
				form.reset();

				await printQrCode('program', newProgram.programId);
				
				await refreshProgramList(selectedLineId);

			} catch (err) {
				console.error("登録失敗:", err);
				alert("プログラム登録中にエラーが発生しました。");
			}
		});
	}

	const delProgramOkButton = document.querySelector("#progdelModal .btn-danger");
	if (delProgramOkButton) {
		delProgramOkButton.addEventListener("click", async function () {
			if (selectedProgramId === null) {
				alert("削除するプログラムを選択してください。");
				return;
			}
			try {
				const response = await fetch("/line/program/delete", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: new URLSearchParams({ programId: selectedProgramId })
				});

				if (!response.ok) {
					throw new Error("削除に失敗しました。");
				}

				const modal = bootstrap.Modal.getInstance(document.getElementById("progdelModal"));
				modal.hide();
				await refreshProgramList(selectedLineId);
			} catch (err) {
				console.error("削除失敗:", err);
				alert(err.message);
			}
		});
	}
	
	// インポートモーダルのOKボタン
	const importOkButton = document.getElementById("import-confirm-btn");
	if (importOkButton) {
		importOkButton.addEventListener("click", async function(event) {
			event.preventDefault(); // form のデフォルト送信を防ぐ

			// テンプレート (E2, B4:V4) に基づくインポートのため、
			// 選択中のプログラムID (selectedProgramId) のチェックは不要とする

			const importModal = document.getElementById("importModal");
			const form = document.getElementById("import-form");
			const fileInput = document.getElementById("import-file-input");

			if (!fileInput.files || fileInput.files.length === 0) {
				alert("インポートするファイルを選択してください。");
				return; 
			}

			const file = fileInput.files[0];
			const formData = new FormData();
			// formData.append('programId', selectedProgramId); // テンプレート仕様に基づき削除
			formData.append('file', file);

			try {
				// サーバー側のインポート用エンドポイント
				// (バックエンドは E2, B4:V4, B6:V26 等を解析する前提)
				const response = await fetch("/line/program/tool/import", {
					method: "POST",
					body: formData 
					// 'Content-Type' は FormData を使うとブラウザが自動設定
				});

				if (!response.ok) {
					const errorText = await response.text(); // エラー時はテキスト
					throw new Error(errorText || "インポートに失敗しました。");
				}

				// [修正] 成功時は JSON (ProgramDto の配列) を受け取る
				const importedPrograms = await response.json();

				const modal = bootstrap.Modal.getInstance(importModal);
				modal.hide();
				form.reset(); // フォームをリセット

				// [修正] QRコードの連続印刷処理
				if (importedPrograms && importedPrograms.length > 0) {
					alert(`インポートが完了しました。\n${importedPrograms.length}件のプログラムのQRコードを順次印刷します。`);
					
					// for...of ループで await を正しく待機
					for (const program of importedPrograms) {
						try {
							// 1. サーバーにタイムスタンプの更新を依頼
							const updateResponse = await fetch("/line/program/reprint-qr", {
								method: "POST",
								headers: { "Content-Type": "application/x-www-form-urlencoded" },
								body: new URLSearchParams({ programId: program.programId })
							});

							if (!updateResponse.ok) {
								// タイムスタンプ更新失敗
								throw new Error(`プログラムID ${program.programId} のQR情報更新に失敗しました。`);
							}
							
							// 2. 更新後の情報 (updatedProgram) を取得
							const updatedProgram = await updateResponse.json(); 

							// 3. QrCodePrinter.js の printQrCode を呼び出す
							//    (タイムスタンプが更新された programId を使用)
							await printQrCode('program', updatedProgram.programId);

						} catch (printErr) {
							console.error(`プログラムID ${program.programId} の印刷に失敗:`, printErr);
							// 1件失敗してもアラートを出し、次のプログラムへ進む
							alert(`プログラムID ${program.programId} の印刷中にエラーが発生しました。\n${printErr.message}\n\n次のプログラムの印刷に進みます。`);
						}
					}
				} else {
					alert("インポートが完了しました。（対象プログラムなし）");
				}

				// すべての処理が終わったらリロード
				location.reload(); 

			} catch (err) {
				console.error("インポート失敗:", err);
				// [修正] 冗長なメッセージを削除し、サーバーからのエラー(err.message)のみ表示
				alert(err.message);
			}
		});
	}
	
	// 工具割当解除モーダルのOKボタン
	const deallocateOkButton = document.getElementById("deallocate-tool-confirm-btn");
	if (deallocateOkButton) {
		deallocateOkButton.addEventListener("click", async function () {
			if (selectedProgramId === null) {
				alert("プログラムが選択されていません。");
				return;
			}
			if (selectedToolId === null) {
				alert("解除するツール番号が選択されていません。");
				return;
			}

			// [修正] T01 -> 0 への変換を削除
			// selectedToolId ("T01", "T02"...) が Service が期待する toolNum
			const toolNumStr = selectedToolId;

			const formData = new URLSearchParams();
			formData.append('programId', selectedProgramId);
			formData.append('toolNum', toolNumStr); // [修正]

			try {
				const response = await fetch("/line/program/tool/deallocate", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: formData
				});

				if (!response.ok) {
					throw new Error("工具割当の解除に失敗しました。");
				}

				const modal = bootstrap.Modal.getInstance(document.getElementById("deallocationModal"));
				modal.hide();

				// メインのツールリストを再描画
				await refreshToolList(selectedProgramId);
				
				// 選択状態を解除
				selectedToolId = null;
				toolTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
				updateButtonStates();

			} catch (err) {
				console.error("割当解除失敗:", err);
				alert(err.message);
			}
		});
	}
	
	const printOkButton = document.querySelector("#lineprintqrcodeModal .btn-danger");
	if (printOkButton) {
		printOkButton.addEventListener("click", async function () {
			if (selectedProgramId === null) { 
				alert("印刷するプログラムを選択してください。");
				return;
			}
			
			const modal = bootstrap.Modal.getInstance(document.getElementById("lineprintqrcodeModal"));
			modal.hide();

			try {
				const response = await fetch("/line/program/reprint-qr", {
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: new URLSearchParams({ programId: selectedProgramId })
				});

				if (!response.ok) {
					throw new Error("QRコード情報の更新に失敗しました。");
				}

				const updatedProgram = await response.json();

				await printQrCode('program', updatedProgram.programId);

			} catch (err) {
				console.error("再印刷処理失敗:", err);
				alert("QRコードの再印刷中にエラーが発生しました。: " + err.message);
			}
		});
	}

	updateButtonStates();
	setupModalTextUpdates();
});