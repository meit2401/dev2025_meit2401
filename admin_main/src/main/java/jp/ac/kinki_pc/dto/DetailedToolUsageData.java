package jp.ac.kinki_pc.dto;

import lombok.Data;

@Data
public class DetailedToolUsageData {

	// --- 追加するフィールド ---
    /**
     * 分類 (mst_tool.tool_category)
     */
     String toolCategory;

    /**
     * メーカー (mst_tool.maker)
     */
     String maker;

    /**
     * 材質 (mst_tool.tool_material)
     */
     String toolMaterial;
    
    // --- 既存のフィールド ---
    /**
     * 型番 (mst_tool.tool_name)
     */
     String toolName;

    /**
     * 取り出し個数 (rec_stock.out_num)
     */
     Integer outNum;

    /**
     * 期間末在庫数 (rec_stock.end_period_stock)
     */
     Integer endPeriodStock;
	
}
