package jp.ac.kinki_pc.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors; // DTOへのマッピング用
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.ac.kinki_pc.dto.OperationHistory; // Service層はDTOを扱う
import jp.ac.kinki_pc.repository.HistoryProjection; // ★これが必要です
import jp.ac.kinki_pc.repository.OperationRepository;

@Service
public class DatabaseBackupService {

	@Autowired
	private OperationRepository operationRepository;

	/**
	 * "未バックアップ" の操作履歴リストを取得する
	 * (backupsフォルダ内の最新ZIPに含まれる最新日時よりも新しい履歴を取得)
	 * (RepositoryからProjectionを受け取り、DTOにマッピングして返します)
	 * @return OperationHistoryのリスト
	 */
	public List<OperationHistory> getOperationHistory() {
		LocalDateTime newerThanTimestamp = null; // デフォルトは null (全件取得)

		try {
			// 1. backupsフォルダから最新のZIPファイルを探す
			Optional<Path> latestZipOpt = findLatestBackupZip();

			if (latestZipOpt.isPresent()) {
				// 2. 最新ZIPファイルが見つかった場合、その中のCSVから最新日時を取得
				Optional<LocalDateTime> latestTimestampOpt = getLatestTimestampFromZip(latestZipOpt.get());

				if (latestTimestampOpt.isPresent()) {
					newerThanTimestamp = latestTimestampOpt.get();
				}
			}
		} catch (IOException e) {
			System.err.println("バックアップファイル検索または読み込み中にエラー: " + e.getMessage());
			// エラー時は newerThanTimestamp は null のまま (全件取得)
		}

		// 4. Repository を呼び出す (Projectionのリストが返る)
		List<HistoryProjection> projections = 
			operationRepository.findOperationHistory(null, newerThanTimestamp);

		// 5. Projection を DTO (OperationHistory) にマッピングして返す
		return projections.stream()
			.map(p -> new OperationHistory(
				p.getProcTime(), 
				p.getUserName(), 
				p.getOperationClass()
			))
			.collect(Collectors.toList());
	}

	/**
	 * backups ディレクトリ内から、ファイル名の終了年月が最も新しい backup_YYYYMM_YYYYMM.zip ファイルを探す
	 * @return 最新のZIPファイルのPath (Optional)
	 * @throws IOException ディレクトリの読み込みに失敗した場合
	 */
	private Optional<Path> findLatestBackupZip() throws IOException {
		Path backupDir = Paths.get("backups");
		if (!Files.isDirectory(backupDir)) {
			return Optional.empty(); // backups ディレクトリがない
		}

		Path latestZip = null;
		String latestEndYyyyMm = ""; // "YYYYMM" 形式で比較

		// ファイル名パターン: backup_YYYYMM_YYYYMM.zip
		Pattern pattern = Pattern.compile("backup_(\\d{6})_(\\d{6})\\.zip");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, "backup_*.zip")) {
			for (Path entry : stream) {
				Matcher matcher = pattern.matcher(entry.getFileName().toString());
				if (matcher.matches()) {
					// String startYyyyMm = matcher.group(1); // 開始年月は使わない
					String endYyyyMm = matcher.group(2); // 終了年月を取得

					// より新しい終了年月のファイルが見つかったら更新
					if (latestZip == null || endYyyyMm.compareTo(latestEndYyyyMm) > 0) {
						latestEndYyyyMm = endYyyyMm;
						latestZip = entry;
					}
				}
			}
		}
		return Optional.ofNullable(latestZip);
	}

	/**
	 * 指定されたZIPファイル内の history_... .csv ファイルを読み込み、
	 * 記録されている最も新しい proc_time (日時) を LocalDateTime として取得する
	 * @param zipFilePath ZIPファイルのPath
	 * @return 最も新しい日時 (Optional)
	 * @throws IOException ZIPファイルまたは内部のCSVファイルの読み込みに失敗した場合
	 */
	private Optional<LocalDateTime> getLatestTimestampFromZip(Path zipFilePath) throws IOException {
		LocalDateTime latestTimestamp = null;
		// CSVファイル内の日付と時間のフォーマッタ
		DateTimeFormatter csvDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		DateTimeFormatter csvTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
			// ZIPファイル内の history_... .csv ファイルを探す
			 Optional<? extends ZipEntry> csvEntryOpt = zipFile.stream()
					 .filter(entry -> !entry.isDirectory() && entry.getName().startsWith("history_") && entry.getName().endsWith(".csv"))
					 .findFirst();

			 if (csvEntryOpt.isPresent()) {
				 ZipEntry csvEntry = csvEntryOpt.get();
				 try (BufferedReader reader = new BufferedReader(
						 new InputStreamReader(zipFile.getInputStream(csvEntry), StandardCharsets.UTF_8))) { // UTF-8で読み込み

					 String line;
					 reader.readLine(); // ヘッダー行を読み飛ばし

					 while ((line = reader.readLine()) != null) {
						 // CSV形式に合わせてカンマで分割 (単純分割)
						 String[] columns = line.split(",");

						 if (columns.length >= 2) {
							 try {
								 // 日付と時間を別々にパースし、結合する
								 LocalDate currentDate = LocalDate.parse(columns[0].trim(), csvDateFormatter);
								 LocalTime currentTime = LocalTime.parse(columns[1].trim(), csvTimeFormatter);
								 LocalDateTime currentTimestamp = LocalDateTime.of(currentDate, currentTime);

								 // より新しい日時が見つかったら更新
								 if (latestTimestamp == null || currentTimestamp.isAfter(latestTimestamp)) {
									 latestTimestamp = currentTimestamp;
								 }
							 } catch (DateTimeParseException e) {
								 System.err.println("CSVファイル内の日時パース失敗: Date='" + columns[0] + "', Time='" + columns[1] + "'");
							 }
						 }
					 }
				 }
			 }
		}
		return Optional.ofNullable(latestTimestamp);
	}


	/**
	 * 指定された期間の操作履歴を取得し、CSVファイルとダミーMP4ファイルをZIPにまとめて保存する
	 * (RepositoryからProjectionを受け取り、DTOにマッピングして使用します)
	 * @param startYear 開始年 (無視される)
	 * @param startMonth 開始月 (無視される)
	 * @param endYear 終了年
	 * @param endMonth 終了月
	 * @return 作成されたZIPファイルのパス
	 * @throws IOException ファイル操作中にエラーが発生した場合
	 */
	@Transactional
	public String performBackup(int startYear, int startMonth, int endYear, int endMonth) throws IOException {

		// 正確な開始日時をログから取得
		Map<String, LocalDateTime> logInfo = readBackupLogInfo();
		LocalDateTime latestRecordIncluded = logInfo.get("latestRecordIncluded");
		LocalDateTime newerThanTimestamp = null; // 開始の基準 (これより後)
		if (latestRecordIncluded != null) {
			newerThanTimestamp = latestRecordIncluded;
		}

		// 終了日時をユーザー指定から計算
		YearMonth endYearMonth = YearMonth.of(endYear, endMonth);
		LocalDateTime endDateTime = endYearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);
		LocalDateTime endTimestamp = endDateTime;

		// Repository呼び出し (Projectionのリストが返る)
		List<HistoryProjection> projections = 
			operationRepository.findOperationHistory(endTimestamp, newerThanTimestamp);

		// Projection を DTO (OperationHistory) にマッピング
		List<OperationHistory> historyList = projections.stream()
			.map(p -> new OperationHistory(
				p.getProcTime(), 
				p.getUserName(), 
				p.getOperationClass()
			))
			.collect(Collectors.toList());

		// 一時ディレクトリ作成
		Path tempDir = Paths.get("temp_backup");
		Files.createDirectories(tempDir);

		// ファイル名用に startYear/Month を再計算 (ログ基準 or 最古)
		int actualStartYear = startYear; // デフォルトは引数
		int actualStartMonth = startMonth; // デフォルトは引数
		if (latestRecordIncluded != null) {
			LocalDateTime nextMonth = latestRecordIncluded.plusMonths(1); // ログの翌月
			actualStartYear = nextMonth.getYear();
			actualStartMonth = nextMonth.getMonthValue();
		} else {
			LocalDateTime oldestTs = operationRepository.findOldestOperationTimestamp();
			// ★ ここがタイプミス修正箇所です (oldTs -> oldestTs)
			if (oldestTs != null) {
				LocalDateTime oldestDt = oldestTs;
				actualStartYear = oldestDt.getYear();
				actualStartMonth = oldestDt.getMonthValue();
			}
		}
		String periodStr = String.format("%d%02d_%d%02d", actualStartYear, actualStartMonth, endYear, endMonth);
		String csvFileName = "history_" + periodStr + ".csv";
		String dummyMp4FileName = "video_" + periodStr + ".mp4";
		String zipFileName = "backup_" + periodStr + ".zip";
		Path csvFilePath = tempDir.resolve(csvFileName);
		Path dummyMp4Path = tempDir.resolve(dummyMp4FileName);
		Path zipFilePath = tempDir.resolve(zipFileName);

		// CSVファイルに書き込み (BOM付きUTF-8)
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		try (BufferedWriter writer = Files.newBufferedWriter(csvFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			 writer.write('\uFEFF'); // BOM
			 writer.write("日付,時間,氏名,作業内容");
			 writer.newLine();
			// DTOのリストを使用
			for (OperationHistory history : historyList) {
				LocalDateTime procDateTime = history.getProcTime();
				writer.write(String.format("%s,%s,%s,%s",
						procDateTime.format(dateFormatter),
						procDateTime.format(timeFormatter),
						escapeCsvField(history.getUserName()),
						escapeCsvField(history.getOperationClass())
				));
				writer.newLine();
			}
		}

		// ダミーMP4作成
		Files.createFile(dummyMp4Path);

		// ZIP作成
		try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
			 ZipOutputStream zos = new ZipOutputStream(fos)) {
			addToZip(csvFilePath, zos);
			addToZip(dummyMp4Path, zos);
		}

		// 最終保存先へ移動
		Path backupDir = Paths.get("backups");
		Files.createDirectories(backupDir);
		Path finalZipPath = backupDir.resolve(zipFileName);
		Files.move(zipFilePath, finalZipPath, StandardCopyOption.REPLACE_EXISTING);

		// 一時ファイル削除
		Files.deleteIfExists(csvFilePath);
		Files.deleteIfExists(dummyMp4Path);
		try { Files.delete(tempDir); } catch (IOException e) { /* ignore */ }
		
		// ログファイル作成 (last_backup_info.txt)
		try {
			Path logDir = Paths.get("backup_log");
			Files.createDirectories(logDir);
			Path logFilePath = logDir.resolve("last_backup_info.txt");
			LocalDateTime backupExecutionTime = LocalDateTime.now();
			DateTimeFormatter logTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String backupTimestampStr = backupExecutionTime.format(logTimestampFormatter);
			String latestRecordTimestampStr = "N/A";
			
			// DTOのリストを使用
			if (!historyList.isEmpty()) {
				OperationHistory latestHistory = historyList.get(historyList.size() - 1); // ASCなので最後が最新
				latestRecordTimestampStr = latestHistory.getProcTime().format(logTimestampFormatter);
			}
			try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath.toFile(), false))) {
				logWriter.write("Last Backup Execution: " + backupTimestampStr);
				logWriter.newLine();
				logWriter.write("Latest Record Included: " + latestRecordTimestampStr);
				logWriter.newLine();
			}
		} catch (IOException e) {
			System.err.println("バックアップログファイルの書き込みに失敗しました: " + e.getMessage());
			e.printStackTrace();
		}

		// ZIPファイルの絶対パスを返す
		return finalZipPath.toAbsolutePath().toString();
	}

	/**
	 * 指定されたファイルをZipOutputStreamに追加するヘルパーメソッド
	 */
	private void addToZip(Path fileToAdd, ZipOutputStream zos) throws IOException {
		 try (FileInputStream fis = new FileInputStream(fileToAdd.toFile())) {
			ZipEntry zipEntry = new ZipEntry(fileToAdd.getFileName().toString());
			zos.putNextEntry(zipEntry);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer)) >= 0) {
				zos.write(buffer, 0, length);
			}
			zos.closeEntry();
		}
	}

	/**
	 * CSVのフィールド値をエスケープする
	 */
	private String escapeCsvField(String field) {
		if (field == null) {
			return "";
		}
		boolean needsQuotes = field.contains("\"") || field.contains(",") || field.contains("\n") || field.contains("\r");
		String escapedField = field.replace("\"", "\"\"");
		if (needsQuotes) {
			return "\"" + escapedField + "\"";
		} else {
			return escapedField;
		}
	}

	/**
	 * バックアップログファイル (last_backup_info.txt) を読み込み、
	 * 最終バックアップ日時と、バックアップに含まれる最新レコード日時を LocalDateTime として取得する
	 * @return Map<String, LocalDateTime> キー: "lastBackupExecution", "latestRecordIncluded" (値は日時 or null)
	 */
	public Map<String, LocalDateTime> readBackupLogInfo() {
		Map<String, LocalDateTime> logInfo = new HashMap<>();
		logInfo.put("lastBackupExecution", null);
		logInfo.put("latestRecordIncluded", null);

		Path logFilePath = Paths.get("backup_log", "last_backup_info.txt");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		if (Files.exists(logFilePath)) {
			try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						if (line.startsWith("Last Backup Execution:")) {
							String timestampStr = line.substring("Last Backup Execution: ".length()).trim();
							if (!timestampStr.equals("N/A")) {
								logInfo.put("lastBackupExecution", LocalDateTime.parse(timestampStr, formatter));
							}
						} else if (line.startsWith("Latest Record Included:")) {
							String timestampStr = line.substring("Latest Record Included: ".length()).trim();
							if (!timestampStr.equals("N/A")) {
								logInfo.put("latestRecordIncluded", LocalDateTime.parse(timestampStr, formatter));
							}
						}
					} catch (DateTimeParseException e) {
						System.err.println("ログファイルの日時形式のパースに失敗しました: " + line + " - " + e.getMessage());
					}
				}
			} catch (IOException e) {
				System.err.println("バックアップログファイルの読み込みに失敗しました: " + e.getMessage());
			}
		}
		return logInfo;
	}

	/**
	 * rec_operation テーブルから最も古い操作日時を取得する
	 * @return 最も古い LocalDateTime (レコードがない場合は null)
	 */
	public LocalDateTime getOldestOperationTimestamp() {
		return operationRepository.findOldestOperationTimestamp();
	}
}