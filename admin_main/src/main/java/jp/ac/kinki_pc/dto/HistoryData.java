package jp.ac.kinki_pc.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoryData {

	// --- 操作履歴情報 ---
	/**
	 * 処理時刻 (SQLの proc_time AS processingTime に対応)
	 */
	private LocalDateTime processingTime;

	/**
	 * ユーザー名（氏名） (SQLの user_name AS userName に対応)
	 */
	private String userName;

	/**
	 * 作業内容 (SQLの opereation_class AS operationContent に対応)
	 */
	private String operationContent;
	
	//動画のパス
	private String video_path;
	
	/**
	 * 工具の分類 (SQLの tool_category AS toolCategory に対応)
	 */
	private String toolCategory;

	/**
	 * 工具のメーカー (SQLの maker AS maker に対応)
	 */
	private String maker;

	/**
	 * 工具の型番 (SQLの tool_name AS toolName に対応)
	 */
	private String toolName;

	/**
	 * 工具の材質 (SQLの tool_material AS toolMaterial に対応)
	 */
	private String toolMaterial;
	
	// ★フィールドを追加
    private Integer toolNum;       // 操作個数
    
    private String displayAddress; // コンテナ番号
    
    private String trackResult;
}