package jp.ac.kinki_pc.repository;

/**
 * StockRepositoryのクエリ結果を受け取るためのProjectionインターフェース。
 * JPQLで取得したデータをSpring Data JPAが自動的にマッピングします。
 */
public interface StockAnalysisProjection {
    String getToolCategory();
    String getMaker();
    String getToolName();
    String getToolMaterial();
    Integer getOutNum();        // ABS関数適用後の値
    Integer getEndPeriodStock();
}