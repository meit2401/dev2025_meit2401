package jp.ac.kinki_pc.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.ac.kinki_pc.dto.HistoryData;
import jp.ac.kinki_pc.dto.YearMonthData;
import jp.ac.kinki_pc.repository.HistoryProjection;
import jp.ac.kinki_pc.repository.OperationRepository;

@Service
public class OperationHistoryService {
	
	@Autowired
	private OperationRepository operationRepository;
	
	public String getMonthDateRange(String yearStr, String monthStr) {
		try {
			// 1. 文字列の引数を数値に変換
			int year = Integer.parseInt(yearStr);
			int month = Integer.parseInt(monthStr);

			// 2. YearMonthオブジェクトを生成
			YearMonth yearMonth = YearMonth.of(year, month);

			// 3. 出力用のフォーマットを定義
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月");

			// 4. フォーマットし、「中」を付けて返す
			return yearMonth.format(formatter) + "中";

		} catch (NumberFormatException | DateTimeParseException e) {
			// 数字に変換できない、または無効な月（例: 13月）が指定された場合
			System.err.println("無効な年または月が入力されました: " + e.getMessage());
			return null;
		}
	}
	
	// リポジトリからの戻り値(Projection)をDTOにマッピングする処理
	public List<HistoryData> getHistoryForView(String userName, YearMonth yearMonth, String operationContent) {
		
		// 1. リポジトリを呼び出して、データベースからProjectionデータを取得する
		List<HistoryProjection> projections = operationRepository.findHistoryWithToolDetails(
			yearMonth.getYear(),
			yearMonth.getMonthValue(),
			userName,
			operationContent
		);
		
		// 2. ProjectionのリストをHistoryData DTOのリストに変換する
		List<HistoryData> historyData = projections.stream()
			.map(p -> new HistoryData(
				p.getProcTime(),
				p.getUserName(),
				p.getOperationClass(),
				p.getVideoPath(),
				// p.getBuyer(), // 削除
				p.getToolCategory(),
				p.getMaker(),
				p.getToolName(),
				p.getToolMaterial(),
				(p.getToolNum() != null ? Math.abs(p.getToolNum()) : 0),
		        p.getDisplayAddress(),
		        p.getTrackResult()
			))
			.collect(Collectors.toList());

		return historyData;
	}
	
	// メソッド名のスペルミスを修正 (getHisotryCandidiate -> getHistoryCandidate)
	public List<YearMonthData> getHistoryCandidate() {
		
		// リポジトリメソッドのスペルミスを修正 (findHistoryCamdodate -> findHistoryCandidate)
		List<String> historyCandidate = operationRepository.findHistoryCandidate();
		 
		// Map<年, 月のSet> に変換（重複排除）
		Map<String, Set<String>> grouped = new LinkedHashMap<>();

		for (String ym : historyCandidate) {
			if (ym == null || ym.isEmpty() || !ym.contains("-")) continue;

			String[] parts = ym.split("-");
			String year = parts[0];
			String month = parts[1];

			grouped.computeIfAbsent(year, k -> new TreeSet<>(Comparator.reverseOrder()))
				   .add(month); 
		}

		// YearMonthData のリストに変換
		List<YearMonthData> result = new ArrayList<>();
		for (Map.Entry<String, Set<String>> entry : grouped.entrySet()) {
			result.add(new YearMonthData(entry.getKey(), new ArrayList<>(entry.getValue())));
		}

		return result;
	}
}