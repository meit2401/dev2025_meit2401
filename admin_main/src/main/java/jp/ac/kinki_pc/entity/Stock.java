// jp/ac/kinki_pc/entity/Stock.java

package jp.ac.kinki_pc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rec_stock")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class Stock {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer stockId;
	private String registerYearMonth;
	private Integer basicToolId;
	private Integer outNum;
	private Integer endPeriodStock;
}