// jp/ac/kinki_pc/entity/UniqueTool.java

package jp.ac.kinki_pc.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unique_tool")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class UniqueTool {
	
	@Id
	private Long uniqueToolId;
	private LocalDateTime toolPrintTime;	// TIMESTAMP型をLocalDateTimeで扱う
	private Integer basicToolId;
	private Integer uniqueNum;
	private Integer casePackNum;
	private Integer storageAreaId;
	private String storageCondition;		// ENUM型: '補充前','保管中','取出'
	private Integer regrindCount;
}