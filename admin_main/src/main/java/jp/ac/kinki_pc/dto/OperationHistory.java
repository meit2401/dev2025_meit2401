package jp.ac.kinki_pc.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class OperationHistory {

	private LocalDateTime procTime;	// 処理日時 (日付と時間)
	private String userName;		// ユーザー名 (氏名)
	private String operationClass;  // 操作分類 (作業内容)
	private Integer toolNum;		// 操作個数
	private String lineName;		// ライン名
	private String toolCategory;	// 分類 ★追加
	private String maker;			// メーカー ★追加
	private String toolName;		// 型番 (工具名)
	private String toolMaterial;	// 材質 ★追加
	private String buyer;			// 商社 ★追加
	private String videoPath;		// 動画パス (※CSVには出力しませんがDTOには保持)
}