// jp/ac/kinki_pc/dto/ToolAssignmentDto.java

package jp.ac.kinki_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolAssignmentDto {

    private String toolNum;
    private String toolCategory;
    private String maker;
    private String toolName;
    private String toolMaterial;
}