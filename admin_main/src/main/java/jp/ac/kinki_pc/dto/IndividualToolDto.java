package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualToolDto {
	
	private long uniqueToolId;
	private int casePackNum;
	private int regrindCount;
	private String storageCondition;
}