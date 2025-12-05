package jp.ac.kinki_pc.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import jp.ac.kinki_pc.dto.AnalysisToolData;
import jp.ac.kinki_pc.dto.DetailedToolUsageData;
import jp.ac.kinki_pc.dto.PeriodRangeData;
import jp.ac.kinki_pc.dto.ToolRelatedData;
import jp.ac.kinki_pc.service.StockAnalysisService;

@Controller
@RequestMapping("/stock")
@SessionAttributes({
    "stockAnalysisTable", "periodStr", "category", 
    "manufacturer", "material", "tradingCompany", "lineName", 
    "makerCandidates", "materialCandidates", "lineCandidates", 
    "yearMonthStructure"
})
public class StockAnalysisController {

    @Autowired
    private StockAnalysisService stockAnalysisService;

    @GetMapping
    public String showStockPage(Model model) {
        // 初期化処理
        model.addAttribute("analysisToolData", new AnalysisToolData());
        if (!model.containsAttribute("stockAnalysisTable")) {
            model.addAttribute("stockAnalysisTable", new ArrayList<>());
        }
        
        // 候補リストの取得 (セッションになければ)
        if (!model.containsAttribute("makerCandidates")) {
            model.addAttribute("makerCandidates", stockAnalysisService.getMakerCandidates());
        }
        if (!model.containsAttribute("materialCandidates")) {
            model.addAttribute("materialCandidates", stockAnalysisService.getMaterialCandidates());
        }
        // ▼▼▼ 追加: ライン名の候補リスト取得 ▼▼▼
        if (!model.containsAttribute("lineCandidates")) {
            model.addAttribute("lineCandidates", stockAnalysisService.getLineCandidates());
        }
        
        if (!model.containsAttribute("yearMonthStructure")) {
            model.addAttribute("yearMonthStructure", stockAnalysisService.getYearMonthStructure());
        }
        
        return "StockAnalysis";
    }

    // 絞り込み実行
    @PostMapping("/filtering")
    public String filterStock(@ModelAttribute AnalysisToolData form, Model model) {
        
        // 期間の計算と表示用文字列生成
        PeriodRangeData periodRange = stockAnalysisService.calculatePeriod(form);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String periodStr = periodRange.getStartDateTime().format(formatter) + "～" + periodRange.getEndDateTime().format(formatter);
        
        // 検索条件のDTO詰め替え
        ToolRelatedData toolData = new ToolRelatedData();
        toolData.setTool_category(convertEmptyToNull(form.getCategory()));
        toolData.setMaker(convertEmptyToNull(form.getManufacturer()));
        toolData.setTool_material(convertEmptyToNull(form.getTool_material()));
        toolData.setBuyer(convertEmptyToNull(form.getTradingCompany()));
        
        // ▼▼▼ 追加: ライン名をDTOにセット ▼▼▼
        toolData.setLineName(convertEmptyToNull(form.getLineName()));
        
        String yearMonth = String.format("%d-%02d", form.getYear(), form.getPeriod());
        List<DetailedToolUsageData> resultList = stockAnalysisService.searchToolUsage(yearMonth, toolData);
        
        // セッションへの保存
        model.addAttribute("periodStr", periodStr);
        model.addAttribute("category", form.getCategory());
        model.addAttribute("manufacturer", form.getManufacturer());
        model.addAttribute("material", form.getTool_material());
        model.addAttribute("tradingCompany", form.getTradingCompany());
        
        // ▼▼▼ 追加: ライン名を画面表示用にModelへ保存 ▼▼▼
        model.addAttribute("lineName", form.getLineName());
        
        model.addAttribute("stockAnalysisTable", resultList);

        return "redirect:/stock";
    }

    // CSVダウンロード
    @SuppressWarnings("unchecked")
    @GetMapping("/download/csv")
    public ResponseEntity<String> saveCsvOnServer(Model model) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Cドライブ配下のフォルダを指定
        String saveDirectory = "C:\\CSVoutput\\"; 
        String fileName = "stock_analysis_" + timestamp + ".csv";
        String fixedSavePath = saveDirectory + fileName;

        try {
            Path directoryPath = Paths.get(saveDirectory);
            if (Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            Object data = model.getAttribute("stockAnalysisTable");
            List<DetailedToolUsageData> toolList = (List<DetailedToolUsageData>) data;

            StringBuilder csvData = new StringBuilder();
            csvData.append("\uFEFF"); // BOM
            csvData.append("\"分類\",\"メーカー\",\"型番\",\"材質\",\"取り出し個数\",\"期間末在庫数\"\n");

            if (toolList != null && !toolList.isEmpty()) {
                for (DetailedToolUsageData tool : toolList) {
                    csvData.append("\"").append(escapeCsv(tool.getToolCategory())).append("\",");
                    csvData.append("\"").append(escapeCsv(tool.getMaker())).append("\",");
                    csvData.append("\"").append(escapeCsv(tool.getToolName())).append("\",");
                    csvData.append("\"").append(escapeCsv(tool.getToolMaterial())).append("\",");
                    csvData.append("\"").append(tool.getOutNum()).append("\",");
                    csvData.append("\"").append(tool.getEndPeriodStock()).append("\"\n");
                }
            }

            Files.writeString(Paths.get(fixedSavePath), csvData.toString(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(fixedSavePath);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("保存に失敗しました。");
        }
    }

    private String convertEmptyToNull(String s) {
        if ("全て".equals(s) || s == null || s.isEmpty()) return null;
        return s;
    }

    private String escapeCsv(String data) {
        return (data == null) ? "" : data.replace("\"", "\"\"");
    }
}