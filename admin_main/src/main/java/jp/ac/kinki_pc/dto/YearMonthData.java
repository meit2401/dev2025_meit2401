package jp.ac.kinki_pc.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // 全ての引数を持つコンストラクタを自動生成
@NoArgsConstructor  // 引数のないコンストラクタを自動生成
public class YearMonthData {
    String year;
    List<String> months;
}