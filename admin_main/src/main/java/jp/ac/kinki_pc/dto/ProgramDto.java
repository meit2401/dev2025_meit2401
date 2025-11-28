// jp/ac/kinki_pc/dto/ProgramDto.java

package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data			   // ゲッター、セッター等を自動生成
@NoArgsConstructor  // 引数なしのコンストラクタを自動生成
@AllArgsConstructor // 全てのフィールドを引数に持つコンストラクタを自動生成
public class ProgramDto {

	private Integer programId;
	private Integer lineId;
	private String programName;
	private String programPrintTime; // TIMESTAMP型をプログラム内ではString型で扱う
}