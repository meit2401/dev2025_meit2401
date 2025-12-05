package jp.ac.kinki_pc.dto;

import lombok.Data;

@Data
public class AnalysisToolData {
    private Integer year;
    private Integer period;
    private String lineName; // ライン名
    private String category;
    private String manufacturer;
    private String tool_material;
    private String tradingCompany;
}
