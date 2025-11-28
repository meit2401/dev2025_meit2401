package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryFilterDisplayData {
	String period;//期間
	String username;//氏名
	String Work_details;//作業内容
}
