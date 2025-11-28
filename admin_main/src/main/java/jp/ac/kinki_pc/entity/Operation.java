// jp/ac/kinki_pc/entity/Operation.java

package jp.ac.kinki_pc.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rec_operation")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class Operation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer operationId;
	private String operationClass;	// ENUM型: '補充','取出','棚卸'
	private Integer userId;
	private LocalDateTime procTime;	// TIMESTAMP型をLocalDateTimeで扱う
	private Integer storageAreaId;
	private Long uniqueToolId;
	private String videoPath;
	private Integer toolNum;
	private Integer lineId;
	private String trackResult;		// ENUM型: '正常','異常'
}