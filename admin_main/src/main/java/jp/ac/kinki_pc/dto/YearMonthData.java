package jp.ac.kinki_pc.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 年（単数）と、その年に属する月（複数）のリストを
 * 保持するためのデータ転送オブジェクト(DTO)。
 */
@Data
@AllArgsConstructor // 全ての引数を持つコンストラクタを自動生成
@NoArgsConstructor  // 引数のないコンストラクタを自動生成
public class YearMonthData {
    String year;
    List<String> months;
}