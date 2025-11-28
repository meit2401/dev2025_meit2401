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
}