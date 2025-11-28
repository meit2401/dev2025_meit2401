package jp.ac.kinki_pc.dto;

import java.time.YearMonth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryFilterData {
	String username;
	YearMonth yearmonth;
	String Work_details;
}
