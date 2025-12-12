package jp.ac.kinki_pc.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.ac.kinki_pc.dto.OperationHistory;
import jp.ac.kinki_pc.service.DatabaseBackupService;

@Controller
public class DatabaseBackupController {

	@Autowired
	private DatabaseBackupService databaseBackupService;

	@GetMapping("/database")
	public String showDatabasePage(Model model) {

		// 1. "未バックアップ" 履歴リストを取得
		List<OperationHistory> historyList = databaseBackupService.getOperationHistory(); //
		model.addAttribute("historyList", historyList);

		// 1.1 リストが空かどうかを判定するフラグを追加
		boolean isHistoryListEmpty = historyList == null || historyList.isEmpty();
		model.addAttribute("isHistoryListEmpty", isHistoryListEmpty);

		// 2. バックアップログ情報を LocalDateTime として取得
		Map<String, LocalDateTime> logInfo = databaseBackupService.readBackupLogInfo(); //
		LocalDateTime lastBackupExecution = logInfo.get("lastBackupExecution");
		LocalDateTime latestRecordIncluded = logInfo.get("latestRecordIncluded");

		// 3. 表示用にフォーマットして Model に追加 (null の場合は "N/A")
		DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		model.addAttribute("lastBackupExecutionTime",
				lastBackupExecution != null ? lastBackupExecution.format(displayFormatter) : "N/A");
		model.addAttribute("latestRecordIncludedTime",
				latestRecordIncluded != null ? latestRecordIncluded.format(displayFormatter) : "N/A");

		// ★★★ ここから追加 ★★★
		// 3.5. 終了年月日の初期値設定 (未バックアップ履歴の最新日時)
		int defaultEndYear = -1;
		int defaultEndMonth = -1;
		int defaultEndDay = -1; // 追加
		if (!isHistoryListEmpty) {
			// Repositoryで ASC (昇順) ソートされているため、リストの末尾が最新
			OperationHistory latestHistory = historyList.get(historyList.size() - 1);
			if (latestHistory != null && latestHistory.getProcTime() != null) {
				// ★ 修正(前回): getProcTime() は既に LocalDateTime のため .toLocalDateTime() を削除
				LocalDateTime latestDateTime = latestHistory.getProcTime();
				defaultEndYear = latestDateTime.getYear();
				defaultEndMonth = latestDateTime.getMonthValue();
				defaultEndDay = latestDateTime.getDayOfMonth(); // 追加
			}
		}
		model.addAttribute("defaultEndYear", defaultEndYear);
		model.addAttribute("defaultEndMonth", defaultEndMonth);
		model.addAttribute("defaultEndDay", defaultEndDay); // 追加
		// ★★★ ここまで追加 ★★★
		// 4. 開始年月日の初期値設定
		
		int defaultStartYear = -1;
		int defaultStartMonth = -1;
		int defaultStartDay = -1; // 追加
		boolean fallbackNeeded = true;

		// 5. まず "バックアップ済日時" (latestRecordIncluded) の翌日を試す
		if (latestRecordIncluded != null) {
			LocalDateTime nextBackupStartDate = latestRecordIncluded.plusDays(1); // 翌日に変更
			defaultStartYear = nextBackupStartDate.getYear();
			defaultStartMonth = nextBackupStartDate.getMonthValue();
			defaultStartDay = nextBackupStartDate.getDayOfMonth(); // 追加
			fallbackNeeded = false;
		}

		// 6. フォールバックが必要な場合 (ログ情報がない)
		if (fallbackNeeded) {
			// ★ 修正: Timestamp -> LocalDateTime
			LocalDateTime oldestTimestamp = databaseBackupService.getOldestOperationTimestamp(); //
			if (oldestTimestamp != null) {
				// ★ 修正: .toLocalDateTime() 削除
				LocalDateTime oldestDateTime = oldestTimestamp;
				defaultStartYear = oldestDateTime.getYear();
				defaultStartMonth = oldestDateTime.getMonthValue();
				defaultStartDay = oldestDateTime.getDayOfMonth(); // 追加
			} else {
				 System.out.println("rec_operation テーブルにレコードが見つかりませんでした。開始年月は設定されません。"); //
			}
		}

		// 7. Modelに開始年月の初期値を追加
		model.addAttribute("defaultStartYear", defaultStartYear);
		model.addAttribute("defaultStartMonth", defaultStartMonth);
		model.addAttribute("defaultStartDay", defaultStartDay); // 追加

		// 8. DatabaseBackup.html を表示
		return "DatabaseBackup"; // ★ 修正 ("database" -> "DatabaseBackup")
	}

	/**
	 * バックアップ実行処理 (POST /database/backup)
	 */
	@PostMapping("/database/backup")
	public String performBackup(
			@RequestParam int startYear, // Serviceでは無視される
			@RequestParam int startMonth, // Serviceでは無視される
			@RequestParam int startDay,   // 追加: Serviceでは無視されるが念のため受け取る
			@RequestParam int endYear,
			@RequestParam int endMonth,
			@RequestParam int endDay,     // 追加
			RedirectAttributes redirectAttributes) {
		try {
			// Service には終了年月日のみ渡す (開始はServiceがログから判断)
			String backupFilePath = databaseBackupService.performBackup(startYear, startMonth, startDay, endYear, endMonth, endDay); //
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/database";
	}
}