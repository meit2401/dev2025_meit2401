package jp.ac.kinki_pc.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.ac.kinki_pc.dto.AnalysisToolData;
import jp.ac.kinki_pc.dto.DetailedToolUsageData;
import jp.ac.kinki_pc.dto.PeriodRangeData;
import jp.ac.kinki_pc.dto.ToolRelatedData;
import jp.ac.kinki_pc.dto.YearMonthData;
import jp.ac.kinki_pc.repository.StockAnalysisProjection;
import jp.ac.kinki_pc.repository.StockRepository;

@Service
public class StockAnalysisService {

    @Autowired
    private StockRepository stockRepository;

    /**
     * 条件に応じてリポジトリを使い分ける検索ロジック
     * RepositoryがProjectionを返すようになったため、DTOへの変換を行う
     */
    public List<DetailedToolUsageData> searchToolUsage(String yearMonthStr, ToolRelatedData toolRelatedData) {
        
        List<StockAnalysisProjection> projections;
        String lineName = toolRelatedData.getLineName();

        if (lineName != null && !lineName.isEmpty() && !"全て".equals(lineName)) {
            // 【パターンA: ライン指定あり】
            String[] parts = yearMonthStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            YearMonth ym = YearMonth.of(year, month);
            LocalDateTime startDate = ym.atDay(1).atStartOfDay();
            LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

            projections = stockRepository.fetchToolUsageByLine(startDate, endDate, toolRelatedData.getLineName(),
                    toolRelatedData.getTool_category(), toolRelatedData.getMaker(),
                    toolRelatedData.getBuyer(), toolRelatedData.getTool_material());

        } else {
            // 【パターンB: ライン指定なし】
            projections = stockRepository.fetchCombinedToolUsage(yearMonthStr,
                    toolRelatedData.getTool_category(), toolRelatedData.getMaker(),
                    toolRelatedData.getTool_material(), toolRelatedData.getBuyer());
        }

        // Projection -> DTO (DetailedToolUsageData) への変換
        return projections.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * ProjectionからDTOへの変換ヘルパーメソッド
     */
    private DetailedToolUsageData convertToDto(StockAnalysisProjection proj) {
        DetailedToolUsageData dto = new DetailedToolUsageData();
        dto.setToolCategory(proj.getToolCategory());
        dto.setMaker(proj.getMaker());
        dto.setToolName(proj.getToolName());
        dto.setToolMaterial(proj.getToolMaterial());
        dto.setOutNum(proj.getOutNum());
        dto.setEndPeriodStock(proj.getEndPeriodStock()); // ライン指定ありの場合はnullになる可能性がありますが、画面側で制御済み
        return dto;
    }

    /**
     * 期間計算ロジック
     */
    public PeriodRangeData calculatePeriod(AnalysisToolData form) {
        int year = form.getYear();
        int month = form.getPeriod();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        PeriodRangeData periodRange = new PeriodRangeData();
        periodRange.setStartDateTime(startDate.atTime(0, 0, 0));
        periodRange.setEndDateTime(endDate.atTime(23, 59, 59));
        return periodRange;
    }

    // ▼ ライン名の候補リスト取得
    public List<String> getLineCandidates() {
        // リポジトリ側で既に DISTINCT, ORDER BY されている場合でも、
        // 万が一のnull混入などを防ぐため既存ロジックを残しても良いですし、
        // そのまま返しても構いません。ここでは安全のため既存処理を残します。
        return stockRepository.fetchLineNames().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ▼ メーカー候補リスト取得
    public List<String> getMakerCandidates() {
        return stockRepository.fetchMaker().stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    // ▼ 材質候補リスト取得
    public List<String> getMaterialCandidates() {
        return stockRepository.fetchToolMaterial().stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    // ▼ 年月データ構造の取得
    public List<YearMonthData> getYearMonthStructure() {
        List<String> yearMonths = stockRepository.fetchYearMonth();
        YearMonth currentYearMonth = YearMonth.now();

        Map<String, List<String>> yearMonthMap = yearMonths.stream()
            .map(ym -> ym.split("-"))
            .filter(parts -> parts.length == 2)
            .filter(parts -> {
                try {
                    return YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))
                            .isBefore(currentYearMonth);
                } catch (NumberFormatException e) {
                    return false;
                }
            })
            .collect(Collectors.groupingBy(
                parts -> parts[0],
                Collectors.mapping(parts -> Integer.toString(Integer.parseInt(parts[1])), Collectors.toList())
            ));

        List<YearMonthData> resultList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : yearMonthMap.entrySet()) {
            List<String> months = entry.getValue();
            months.sort(Comparator.comparingInt(Integer::parseInt));
            resultList.add(new YearMonthData(entry.getKey(), months));
        }
        resultList.sort(Comparator.comparing(YearMonthData::getYear).reversed());
        return resultList;
    }
}