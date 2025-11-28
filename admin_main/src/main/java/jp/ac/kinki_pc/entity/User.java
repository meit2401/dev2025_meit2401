// jp/ac/kinki_pc/entity/User.java

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
@Table(name = "mst_user")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer userId;
	private String userName;
	private LocalDateTime userPrintTime;	// TIMESTAMP型をLocalDateTimeで扱う
	private Integer perAdd;
	private Integer perInventory;
	private Integer perUser;
	private Integer perTool;
	private Integer perLine;
	private Integer perAnalysis;
	private Integer perHistory;
	private Integer perDb;
	private Integer perSetting;
	private Boolean isFrozen;
}