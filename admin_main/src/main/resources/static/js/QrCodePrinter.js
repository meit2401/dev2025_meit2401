// QrCodePrinter.js

/**
 * 指定されたタイプとIDのQRコードを印刷する
 * @param {string} type - 印刷対象のタイプ (例: 'user', 'tool')
 * @param {number} id - 印刷対象のID
 */
async function printQrCode(type, id) {
	if (!type || id === null || id === undefined) {
		alert("印刷対象のタイプまたはIDが指定されていません。");
		return;
	}

	try {
		const printersResult = await TepraPrint.getPrinter();
		if (printersResult.errorCode !== TepraPrintError.SUCCESS) {
			throw new Error("プリンターが見つかりません。");
		}
		const printerName = printersResult.printers[0];
		const printerResult = await TepraPrint.createPrinter(printerName);
		if (printerResult.errorCode !== TepraPrintError.SUCCESS) {
			throw new Error("プリンターオブジェクトの作成に失敗しました。");
		}
		const printer = printerResult.printer;
		const printParamResult = await printer.createPrintParameter();
		const printParam = printParamResult.printParameter;

		printParam.skipRecord = true;

		const templateResponse = await fetch('/qrcodeprinter/template.lw1');
		const templateBlob = await templateResponse.blob();
		const templateFile = new File([templateBlob], "qr_template.lw1");

		// URLにタイプとIDを含めるように変更
		const csvUrl = `/qrcodeprinter/data.csv?type=${encodeURIComponent(type)}&id=${encodeURIComponent(id)}`;
		const csvResponse = await fetch(csvUrl);

		if (!csvResponse.ok) {
			const errorText = await csvResponse.text();
			throw new Error(`サーバーエラー: ${csvResponse.statusText} - ${errorText}`);
		}
		
		const csvBlob = await csvResponse.blob();
		const csvFile = new File([csvBlob], "qr_data.csv", {type: 'text/csv; charset=utf-8'});
		
		const printFile = {
			templateFile: templateFile,
			csvFile: csvFile
		};

		const printResult = await printer.doPrint(printParam, printFile);

		if (printResult.errorCode === TepraPrintError.SUCCESS) {
			// Administrator(id:0)の場合は初回通知のため、成功アラートは表示しない
			if (id !== 0) {
				alert('QRコードの印刷要求を正常に送信しました。');
			}
		} else {
			let errorMessage = "QRコードの印刷に失敗しました。";
			switch(printResult.errorCode) {
				case TepraPrintError.PRINTER_NOT_FOUND:
					errorMessage += "\nエラー: プリンターが見つかりません。TEPRA Web Printアプリが起動しているか、またはプリンターが接続されているか確認してください。";
					break;
				case TepraPrintError.INVALID_PARAMETER:
					errorMessage += "\nエラー: 印刷パラメータが不正です。テンプレートファイルまたはCSVファイルに問題がないか確認してください。";
					break;
				case TepraPrintError.PRINT_START_ERROR:
					errorMessage += "\nエラー: 印刷ジョブの開始に失敗しました。";
					break;
				case TepraPrintError.WEBAPI_REQUEST_ERROR:
					errorMessage += "\nエラー: TEPRA Web Printアプリケーションに接続できません。アプリケーションが起動しているか確認してください。";
					break;
				case TepraPrintError.FILE_NOT_FOUND:
					errorMessage += "\nエラー: テンプレートファイルが見つかりません。";
					break;
				default:
					errorMessage += `\n不明なエラーが発生しました。エラーコード: ${printResult.errorCode}`;
					break;
			}
			alert(errorMessage);
		}
	} catch (error) {
		alert('予期せぬエラーが発生しました。:' + error.message);
	}
}