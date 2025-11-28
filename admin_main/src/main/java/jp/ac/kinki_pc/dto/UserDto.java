// jp/ac/kinki_pc/dto/UserDto.java

package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class UserDto {
	
	private Integer userId;
	private String userName;
	private String userPrintTime;	// プログラム内ではString型で扱う
	private Integer perAdd;
	private Integer perInventory;
	private Integer perUser;
	private Integer perTool;
	private Integer perLine;
	private Integer perAnalysis;
	private Integer perHistory;
	private Integer perDb;
	private Integer perSetting;
}