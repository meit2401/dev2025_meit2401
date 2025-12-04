package jp.ac.kinki_pc.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PeriodRangeData {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
