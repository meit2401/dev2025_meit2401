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
@Table(name = "mst_tool")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class Tool {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)	// AUTO_INCREMENTに対応
	private Integer basicToolId;
	private String toolName;
	private String maker;
	private String toolCategory; // ENUM型: 'ドリル','インサート','タップ','リーマー','エンドミル'
	private String toolMaterial;
	private Integer stc;
	private Integer rop;
	private String buyer;
	private Boolean isFrozen;
}