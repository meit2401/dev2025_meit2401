// userManagement.js

// ページが完全に読み込まれた後に実行される処理
document.addEventListener("DOMContentLoaded", function () {
	// 選択されたユーザーIDを保持するグローバル変数（数値またはnull）
	let selectedUserId = null;
	// 編集ボタンのDOM要素を取得
	const editButton = document.getElementById("edit-user-btn");
	// 削除ボタンのDOM要素を取得
	const deleteButton = document.getElementById("delete-user-btn");
	// QRコード再印刷ボタンのDOM要素を取得
	const printButton = document.getElementById("print-qr-btn");
	// ユーザー一覧テーブルのtbody要素を取得
	const usersTableBody = document.querySelector("#users-table tbody");

	let allUserRows = [];
	if (usersTableBody) {
		// IDセル (cells[0]) が空でない行をデータ行としてキャッシュ
		allUserRows = Array.from(usersTableBody.querySelectorAll("tr")).filter(row => {
			return row.cells[0] && row.cells[0].textContent.trim() !== "" && row.cells[0].textContent.trim() !== "&nbsp;";
		});
	}

	/**
	 * ボタンの状態と見た目を更新する
	 */
	function updateButtonStates() {
		const isAdministratorSelected = selectedUserId === 0;

		if (editButton) {
			editButton.disabled = selectedUserId === null || isAdministratorSelected;
			editButton.classList.toggle("custom-btn-common--notselectable", selectedUserId === null || isAdministratorSelected);
		}
		if (deleteButton) {
			deleteButton.disabled = selectedUserId === null || isAdministratorSelected;
			deleteButton.classList.toggle("custom-btn-common--notselectable", selectedUserId === null || isAdministratorSelected);
		}
		if (printButton) {
			printButton.disabled = selectedUserId === null;
			printButton.classList.toggle("custom-btn-common--notselectable", selectedUserId === null);
		}
	}

	/**
	 * テーブルの行数が最低行数に満たない場合、空行を追加する
	 * @param {number} minRows - 最低限表示したい行数
	 */
	function adjustTableRows(minRows) {
        if (!usersTableBody) return;
        
        // 1. 既存の空行 (cells[0] が空の行) をすべて削除
        const allRows = usersTableBody.querySelectorAll("tr");
        const emptyRows = Array.from(allRows).filter(row => {
            return !row.cells[0] || row.cells[0].textContent.trim() === "" || row.cells[0].textContent.trim() === "&nbsp;";
        });
        emptyRows.forEach(row => row.remove());

        // 2. 現在のデータ行の数をカウント (tbody には一致したものしかいない)
        const currentRowCount = usersTableBody.querySelectorAll("tr").length;
        
        // 3. 足りない分の空行を追加
        if (currentRowCount < minRows) {
            const cellsInRow = 11; // UserManagement.html のテーブル列数は 11
			for (let i = 0; i < minRows - currentRowCount; i++) {
				const newRow = usersTableBody.insertRow(); // 末尾に追加
				for (let j = 0; j < cellsInRow; j++) {
					const newCell = newRow.insertCell();
					newCell.innerHTML = "&nbsp;";
				}
			}
        }
	}

	// --- テーブル行のクリックイベント設定 ---
	const usersTable = document.getElementById("users-table");
	if (usersTable) {
		usersTable.addEventListener("click", function (event) {
			const clickedRow = event.target.closest("tr");
			
			if (!clickedRow || !clickedRow.closest("tbody")) return;

			const userIdCell = clickedRow.cells[0];
			if (userIdCell && userIdCell.textContent.trim() !== "" && userIdCell.textContent.trim() !== "&nbsp;") {
				usersTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
				clickedRow.classList.add("custom-list--selected");
				const cells = clickedRow.querySelectorAll("td");
				selectedUserId = parseInt(cells[0].textContent.trim(), 10);
				const selectedUserName = cells[1].textContent.trim();
				updateButtonStates();

				const editModal = document.getElementById("usereditModal");
				if (editModal) {
					const nameInput = editModal.querySelector("input[type='text']");
					if (nameInput) {
						nameInput.value = selectedUserName;
					}
					const checkboxes = editModal.querySelectorAll("input[type='checkbox']");
					checkboxes.forEach((cb, idx) => {
						cb.checked = cells[idx + 2]?.textContent.trim() === "〇";
					});
				}
				
				const delModal = document.getElementById("userdelModal");
				if (delModal) {
					const modalTitle = delModal.querySelector(".modal-title");
					if (modalTitle) {
						modalTitle.innerHTML = `${selectedUserName} の<br>ユーザー削除を行います`;
					}
				}

				const printModal = document.getElementById("userprintqrcodeModal");
				if (printModal) {
					const modalTitle = printModal.querySelector(".modal-title");
					if (modalTitle) {
						modalTitle.innerHTML = `${selectedUserName} の<br>QRコードを再印刷します`;
					}
				}
			} else {
				usersTable.querySelectorAll("tbody tr").forEach(r => r.classList.remove("custom-list--selected"));
				selectedUserId = null;
				updateButtonStates();
			}
		});
	}


	// --- 新規登録モーダルのOKボタン押下処理 ---
	const addOkButton = document.querySelector("#useraddModal .btn-danger");
	if (addOkButton) {
		addOkButton.addEventListener("click", async function (event) {
			event.preventDefault();
			
			const addModal = document.getElementById("useraddModal");
			const form = addModal.querySelector("form");
			
			if (!form.checkValidity()) {
				form.reportValidity();
				return;
			}
			
			const formData = new URLSearchParams(new FormData(form));

			try {
				const response = await fetch("/user/add", { // 修正: /users/add -> /user/add
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: formData
				});

				if (!response.ok) {
					throw new Error('ユーザー登録に失敗しました。');
				}

				const newUser = await response.json();

				const modal = bootstrap.Modal.getInstance(addModal);
				modal.hide();
				
				await printQrCode('user', newUser.userId);
				
				location.reload();

			} catch (err) {
				console.error("登録失敗:", err);
				alert("ユーザー登録中にエラーが発生しました。");
			}
		});
	}

	// --- 編集モーダルのOKボタン押下処理 ---
	const editOkButton = document.querySelector("#usereditModal .btn-danger");
	if (editOkButton) {
		editOkButton.addEventListener("click", function () {
			if (selectedUserId === null) {
				alert("編集するユーザーを選択してください。");
				return;
			}
	
			const editModal = document.getElementById("usereditModal");
			const nameInput = editModal.querySelector("input[type='text']");
			const checkboxes = editModal.querySelectorAll("input[type='checkbox']");
	
			const payload = {
				userId: selectedUserId,
				userName: nameInput.value,
				perAdd: checkboxes[0].checked ? 1 : 0,
				perInventory: checkboxes[1].checked ? 1 : 0,
				perUser: checkboxes[2].checked ? 1 : 0,
				perTool: checkboxes[3].checked ? 1 : 0,
				perLine: checkboxes[4].checked ? 1 : 0,
				perAnalysis: checkboxes[5].checked ? 1 : 0,
				perHistory: checkboxes[6].checked ? 1 : 0,
				perDb: checkboxes[7].checked ? 1 : 0,
				perSetting: checkboxes[8].checked ? 1 : 0
			};
	
			fetch("/user/edit", { // 修正: /users/edit -> /user/edit
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams(payload)
			})
			.then(res => {
				if (!res.ok) throw new Error("更新に失敗しました。");
				return res.text();
			})
			.then(msg => {
				console.log(msg);
				location.reload();
			})
			.catch(err => {
				console.error("更新失敗:", err);
				alert(err.message);
			});
		});
	}
	
	// --- 削除モーダルのOKボタン処理 ---
	const delOkButton = document.querySelector("#userdelModal .btn-danger");
	if (delOkButton) {
		delOkButton.addEventListener("click", function () {
			if (selectedUserId === null) {
				alert("削除するユーザーを選択してください。");
				return;
			}
	
			fetch("/user/delete", { // 修正: /users/delete -> /user/delete
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams({ userId: selectedUserId })
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

	// --- QRコード再印刷モーダルのOKボタン処理 ---
	const printOkButton = document.querySelector("#userprintqrcodeModal .btn-danger");
	if (printOkButton) {
		printOkButton.addEventListener("click", async function () {
			if (selectedUserId === null) { 
				alert("印刷するユーザーを選択してください。");
				return;
			}
			
			const modal = bootstrap.Modal.getInstance(document.getElementById("userprintqrcodeModal"));
			modal.hide();

			try {
				const response = await fetch("/user/reprint-qr", { // 修正: /users/reprint-qr -> /user/reprint-qr
					method: "POST",
					headers: { "Content-Type": "application/x-www-form-urlencoded" },
					body: new URLSearchParams({ userId: selectedUserId })
				});

				if (!response.ok) {
					throw new Error("QRコード情報の更新に失敗しました。");
				}

				const updatedUser = await response.json();

				await printQrCode('user', updatedUser.userId);

			} catch (err) {
				console.error("再印刷処理失敗:", err);
				alert("QRコードの再印刷中にエラーが発生しました。: " + err.message);
			}
		});
	}
	
	// --- 氏名検索欄の入力イベント設定 ---
	const userNameSearchInput = document.getElementById("userName-search");
    
	if (userNameSearchInput && usersTableBody && usersTable) {
		
		// 'input' イベントでリアルタイム検索を実行
		userNameSearchInput.addEventListener("input", function () { 
			
            // 1. 検索文字列を取得
	        const searchText = this.value.toLowerCase().trim();
	            
			// 2. tbody を一旦クリア
			usersTableBody.innerHTML = '';
			
			// 3. キャッシュ (allUserRows) をフィルタリング
			allUserRows.forEach(row => {
				// 4. 氏名セル(インデックス 1)のテキストを取得
				const userNameCell = row.cells[1];
				const userName = userNameCell ? userNameCell.textContent.toLowerCase() : '';
				
				// 5. 部分一致判定
				if (userName.includes(searchText)) {
					// 一致すれば tbody に戻す (appendChild)
					usersTableBody.appendChild(row);
				}
			});
	            
	        // 6. 検索実行後は選択が解除されている状態にする
	        if (selectedUserId !== null) {
	            selectedUserId = null;
	            updateButtonStates();
	        }

	        // 7. 検索後にテーブルの行数を調整 (空行を追加)
	        adjustTableRows(16); // 16行に設定
		});
	}

	// --- 初期化処理 ---
	updateButtonStates();
	adjustTableRows(16);
});

// モーダル閉鎖時のフォームリセット処理
document.addEventListener('DOMContentLoaded', () => {
	
	// リセット対象となるモーダルのIDを指定
	// ※実際のHTML上のIDと異なる場合は適宜修正してください
	const modalsToReset = ['useraddModal']; 

	modalsToReset.forEach(modalId => {
		const modalEl = document.getElementById(modalId);
		if (modalEl) {
			modalEl.addEventListener('hidden.bs.modal', () => {
				// モーダル内のフォームを探してリセット
				const form = modalEl.querySelector('form');
				if (form) {
					form.reset();
				}
			});
		}
	});
});