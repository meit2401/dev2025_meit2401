package jp.ac.kinki_pc.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.ac.kinki_pc.dto.AddressDto;
import jp.ac.kinki_pc.dto.IndividualToolDto;
import jp.ac.kinki_pc.dto.ToolDto;
import jp.ac.kinki_pc.entity.UniqueTool;
import jp.ac.kinki_pc.service.ToolManagementService;

@Controller
@RequestMapping("/tool")
public class ToolManagementController {

	@Autowired
	private ToolManagementService toolManagementService;


	@GetMapping
	public String getTools(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String maker,
			@RequestParam(required = false) String material,
			@RequestParam(required = false) String trader,
			@RequestParam(required = false) String toolNameFilter,
			@RequestParam(required = false) Integer selectedId,
			Model model) {

		List<ToolDto> toolList = toolManagementService.searchTools(maker, category, material, trader, toolNameFilter);
		model.addAttribute("toolList", toolList);

		model.addAttribute("makerFilter", maker);
		model.addAttribute("categoryFilter", category);
		model.addAttribute("materialFilter", material);
		model.addAttribute("traderFilter", trader);
		model.addAttribute("toolNameFilter", toolNameFilter);
		model.addAttribute("selectedId", selectedId);

		int individualToolCount = 0;
		if (selectedId != null) {
			individualToolCount = toolManagementService.getIndividualToolCount(selectedId);
		}

		model.addAttribute("individualToolCount", individualToolCount);

		List<IndividualToolDto> individualToolList;
		if (selectedId != null) {
			individualToolList = toolManagementService.getIndividualTools(selectedId);
		} else {
			individualToolList = Collections.emptyList();
		}
		model.addAttribute("individualToolList", individualToolList);

		List<AddressDto> addressList = toolManagementService.getAllAddresses();
		model.addAttribute("addressList", addressList);

		ToolDto selectedTool = toolManagementService.findSelectedTool(toolList, selectedId);
		model.addAttribute("selectedTool", selectedTool);

		java.util.Map<String, Integer> storageCounts = toolManagementService.getStorageCounts();
		model.addAttribute("storageCounts", storageCounts);

		return "ToolManagement";
	}

	@GetMapping("/details")
	@ResponseBody
	public List<IndividualToolDto> getIndividualToolDetails(
			@RequestParam int basicToolId) {
		return toolManagementService.getIndividualTools(basicToolId);
	}

	@PostMapping("/add")
	public String addTool(
			@RequestParam String toolName,
			@RequestParam String maker,
			@RequestParam String toolCategory,
			@RequestParam String toolMaterial,
			@RequestParam int rop,
			@RequestParam String buyer,
			@RequestParam String storageLocation,
			@RequestParam(defaultValue = "0") int stc) {

		toolManagementService.addTool(toolName, maker, toolCategory, toolMaterial, stc, rop, buyer, storageLocation);
		return "redirect:/tool";
	}

	@PostMapping("/delete")
	public String deleteTool(@RequestParam int basicToolId) {
		toolManagementService.deleteTool(basicToolId);
		return "redirect:/tool";
	}

	@PostMapping("/edit")
	public String editRop(
			@RequestParam int basicToolId,
			@RequestParam int rop) {

		toolManagementService.updateReorderPoint(basicToolId, rop);
		return "redirect:/tool?selectedId=" + basicToolId;
	}

	@PostMapping("/createIndividual")
	@ResponseBody // JSONレスポンスを返す
	public ResponseEntity<Map<String, Object>> createIndividualTool(
			@RequestParam int basicToolId,
			@RequestParam(defaultValue = "1") int casePackNum) {

		try {
			long newUniqueToolId = toolManagementService.createIndividualTool(basicToolId, casePackNum);
			// 成功したら uniqueToolId を含むJSONを返す
			return ResponseEntity.ok(Map.of("uniqueToolId", newUniqueToolId));
		} catch (Exception e) {
			// サービス層でエラーが発生した場合 (例: 対象工具が見つからない)
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * QRコード再印刷のためにタイムスタンプを更新し、uniqueToolIdを返す (Ajax用)
	 * 9桁のラベルコード(Hex)に対応
	 */
	@PostMapping("/reprintQrAjax")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> reprintQrCodeAjax(@RequestParam String qrNumber) {
		if (qrNumber == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "コードが入力されていません。"));
		}

		try {
			if (qrNumber.matches("(?i)[0-9a-f]{9}")) { // (?i)は大文字小文字区別なし
				UniqueTool tool = toolManagementService.findUniqueToolByLabelCode(qrNumber);
				if (tool != null) {
					// 工具が見つかった場合、そのIDを使って reprintQrCode (タイムスタンプ更新) を呼び出す
					// reprintQrCode は 10桁のStringID を期待しているため再構築する
					String tenDigitId = String.format("%05d%05d", tool.getBasicToolId(), tool.getUniqueNum());

					Long uniqueToolId = toolManagementService.reprintQrCode(tenDigitId);
					return ResponseEntity.ok(Map.of("uniqueToolId", uniqueToolId));
				} else {
					return ResponseEntity.status(404).body(Map.of("error", "ラベルコードに該当する工具が見つかりません。"));
				}
			}
			// エラー: 形式不一致
			else {
				return ResponseEntity.badRequest().body(Map.of("error", "10桁の数字、または9桁のラベルコードを入力してください。"));
			}

		} catch (IllegalArgumentException e) {
			 return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", "サーバー内部エラーが発生しました。"));
		}
	}
}
