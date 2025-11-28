package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDto {

	private Integer basicToolId;
	private String toolName;
	private String maker;
	private String toolCategory;
	private String toolMaterial;
	private Integer stc;
	private Integer rop;
	private String buyer;
}