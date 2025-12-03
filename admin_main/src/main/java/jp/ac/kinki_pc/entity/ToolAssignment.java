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
@Table(name = "mst_tool_assignment")
@Data				// ゲッター、セッター等を自動生成
@NoArgsConstructor	// 引数なしのコンストラクタを自動生成
@AllArgsConstructor	// 全てのフィールドを引数に持つコンストラクタを自動生成
public class ToolAssignment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)	// AUTO_INCREMENTに対応
	private Integer toolAssignmentId;
	private Integer programId;
	private String toolNum;
	private Integer basicToolId;
}