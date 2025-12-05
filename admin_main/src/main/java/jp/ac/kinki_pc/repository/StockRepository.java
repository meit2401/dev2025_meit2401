package jp.ac.kinki_pc.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {

    /**
     * ライン指定なしの検索
     * rec_stock (Stock) と mst_tool (Tool) を結合
     */
    @Query("SELECT " +
           "  t.toolCategory AS toolCategory, " +
           "  t.maker AS maker, " +
           "  t.toolName AS toolName, " +
           "  t.toolMaterial AS toolMaterial, " +
           "  ABS(s.outNum) AS outNum, " +
           "  s.endPeriodStock AS endPeriodStock " +
           "FROM Tool t, Stock s " +
           "WHERE t.basicToolId = s.basicToolId " +
           "  AND s.registerYearMonth = :yearMonth " +
           "  AND (:toolCategory IS NULL OR t.toolCategory = :toolCategory) " +
           "  AND (:maker IS NULL OR t.maker = :maker) " +
           "  AND (:toolMaterial IS NULL OR t.toolMaterial = :toolMaterial) " +
           "  AND (:buyer IS NULL OR t.buyer = :buyer) " +
           "  AND t.stc IS NOT NULL")
    List<StockAnalysisProjection> fetchCombinedToolUsage(
            @Param("yearMonth") String yearMonth,
            @Param("toolCategory") String toolCategory,
            @Param("maker") String maker,
            @Param("toolMaterial") String toolMaterial,
            @Param("buyer") String buyer
    );

    /**
     * ライン指定ありの検索
     * rec_operation (Operation) を起点に各マスタを結合
     */
    @Query("SELECT " +
           "  t.toolCategory AS toolCategory, " +
           "  t.maker AS maker, " +
           "  t.toolName AS toolName, " +
           "  t.toolMaterial AS toolMaterial, " +
           "  ABS(SUM(ro.toolNum)) AS outNum " +
           "FROM Operation ro, UniqueTool ut, Line l, Tool t " +
           "WHERE ro.uniqueToolId = ut.uniqueToolId " +
           "  AND ro.lineId = l.lineId " +
           "  AND ut.basicToolId = t.basicToolId " +
           "  AND ro.operationClass = '取出' " +
           "  AND ro.procTime BETWEEN :startDate AND :endDate " +
           "  AND l.lineName = :lineName " +
           "  AND (:toolCategory IS NULL OR t.toolCategory = :toolCategory) " +
           "  AND (:maker IS NULL OR t.maker = :maker) " +
           "  AND (:buyer IS NULL OR t.buyer = :buyer) " +
           "  AND (:toolMaterial IS NULL OR t.toolMaterial = :toolMaterial) " +
           "GROUP BY " +
           "  t.toolCategory, " +
           "  t.maker, " +
           "  t.toolName, " +
           "  t.toolMaterial " +
           "ORDER BY " +
           "  t.toolCategory, " +
           "  t.maker, " +
           "  t.toolName")
    List<StockAnalysisProjection> fetchToolUsageByLine(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("lineName") String lineName,
            @Param("toolCategory") String toolCategory,
            @Param("maker") String maker,
            @Param("buyer") String buyer,
            @Param("toolMaterial") String toolMaterial
    );

    // ▼ ライン名の候補を取得
    @Query("SELECT DISTINCT l.lineName FROM Line l ORDER BY l.lineName")
    List<String> fetchLineNames();

    // ▼ メーカー候補を取得
    @Query("SELECT DISTINCT t.maker FROM Tool t WHERE t.maker IS NOT NULL AND t.maker <> ''")
    List<String> fetchMaker();

    // ▼ 材質候補を取得
    @Query("SELECT DISTINCT t.toolMaterial FROM Tool t WHERE t.toolMaterial IS NOT NULL AND t.toolMaterial <> ''")
    List<String> fetchToolMaterial();

    // ▼ 年月候補を取得
    @Query("SELECT DISTINCT s.registerYearMonth FROM Stock s WHERE s.registerYearMonth IS NOT NULL AND s.registerYearMonth <> '' ORDER BY s.registerYearMonth DESC")
    List<String> fetchYearMonth();
}