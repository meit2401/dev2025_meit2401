package jp.ac.kinki_pc.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jp.ac.kinki_pc.dto.LineDto;
import jp.ac.kinki_pc.dto.ProgramDto;
import jp.ac.kinki_pc.dto.ToolAssignmentDto;
import jp.ac.kinki_pc.dto.ToolDto;
import jp.ac.kinki_pc.service.LineManagementService;

@Controller
@RequestMapping("/line") 
public class LineManagementController {
	
	@Autowired
	private LineManagementService lineManagementService;

	@GetMapping
	public String showLinePage(Model model) {
		List<LineDto> lineList = lineManagementService.findAllLines();
		model.addAttribute("lines", lineList);
		model.addAttribute("newLine", new LineDto());
		return "LineManagement";
	}

	@PostMapping("/add")
	@ResponseBody
	public ResponseEntity<LineDto> addLine(@ModelAttribute LineDto newLineDto) {
		LineDto savedLineDto = lineManagementService.addLine(newLineDto);
		return ResponseEntity.ok(savedLineDto);
	}

	@PostMapping("/delete")
	@ResponseBody
	public ResponseEntity<String> deleteLine(@RequestParam("lineId") Integer lineId) {
		lineManagementService.deleteLine(lineId);
		return ResponseEntity.ok("deleted");
	}

	@GetMapping("/programs")
	@ResponseBody
	public ResponseEntity<List<ProgramDto>> getProgramsByLineId(@RequestParam("lineId") Integer lineId) {
		List<ProgramDto> programs = lineManagementService.findProgramsByLineId(lineId);
		return ResponseEntity.ok(programs);
	}

	@PostMapping("/program/add")
	@ResponseBody
	public ResponseEntity<ProgramDto> addProgram(@ModelAttribute ProgramDto newProgramDto) {
		ProgramDto savedProgramDto = lineManagementService.addProgram(newProgramDto);
		return ResponseEntity.ok(savedProgramDto);
	}

	@PostMapping("/program/delete")
	@ResponseBody
	public ResponseEntity<String> deleteProgram(@RequestParam("programId") Integer programId) {
		lineManagementService.deleteProgram(programId);
		return ResponseEntity.ok("deleted");
	}
	
	@GetMapping("/program/tools")
	@ResponseBody
	public ResponseEntity<List<ToolAssignmentDto>> getToolAssignments(@RequestParam("programId") Integer programId) {
		List<ToolAssignmentDto> assignments = lineManagementService.findToolAssignmentsByProgramId(programId);
		return ResponseEntity.ok(assignments);
	}
	
	@GetMapping("/tools/filter")
	@ResponseBody
	public ResponseEntity<List<ToolDto>> getFilteredTools(
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "maker", required = false) String maker,
			@RequestParam(value = "toolName", required = false) String toolName,
			@RequestParam(value = "material", required = false) String material) {
		
		List<ToolDto> tools = lineManagementService.findFilteredTools(category, maker, toolName, material);
		return ResponseEntity.ok(tools);
	}

	@PostMapping("/program/tool/assign")
	@ResponseBody
	public ResponseEntity<String> assignTool(
			@RequestParam("programId") Integer programId,
			@RequestParam("toolNum") String toolNum,
			@RequestParam("basicToolId") Integer basicToolId) {
		
		lineManagementService.assignToolToProgram(programId, toolNum, basicToolId);
		return ResponseEntity.ok("assigned");
	}

	@PostMapping("/program/tool/deallocate")
	@ResponseBody
	public ResponseEntity<String> deallocateTool(
			@RequestParam("programId") Integer programId,
			@RequestParam("toolNum") String toolNum) {
		
		lineManagementService.deallocateToolFromProgram(programId, toolNum);
		return ResponseEntity.ok("deallocated");
	}

	/**
	 * QRコード再印刷のためにタイムスタンプを更新し、更新後のプログラム情報と更新前のタイムスタンプを返す
	 * @param programId プログラムID
	 * @return 更新後のプログラム情報と旧タイムスタンプを含むMap
	 */
	@PostMapping("/program/reprint-qr")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> reprintQrCode(@RequestParam("programId") Integer programId) {
		// ロールバック用に更新前の情報を取得しておく
		ProgramDto oldProgram = lineManagementService.findProgramEntityById(programId)
				.map(lineManagementService::convertToDto)
				.orElse(null);

		ProgramDto updatedProgramDto = lineManagementService.updateProgramTimestamp(programId);
		
		if (updatedProgramDto != null && oldProgram != null) {
			Map<String, Object> response = new HashMap<>();
			response.put("program", updatedProgramDto);
			response.put("oldTimestamp", oldProgram.getProgramPrintTime());
			return ResponseEntity.ok(response);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	/**
	 * 印刷失敗時にプログラムのタイムスタンプを元に戻す処理
	 * @param programId プログラムID
	 * @param oldTimestampStr 戻したいタイムスタンプ文字列
	 * @return 処理結果
	 */
	@PostMapping("/program/restore-timestamp")
	@ResponseBody
	public ResponseEntity<String> restoreTimestamp(@RequestParam("programId") Integer programId, 
			@RequestParam(value = "oldTimestamp", required = false) String oldTimestampStr) {
		lineManagementService.restoreProgramTimestamp(programId, oldTimestampStr);
		return ResponseEntity.ok("restored");
	}
	
	@PostMapping("/program/tool/import")
	@ResponseBody
	public ResponseEntity<?> handleToolImport(@RequestParam("file") MultipartFile file) {
		
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("ファイルが選択されていません。");
		}

		try {
			List<ProgramDto> programDtos = lineManagementService.importToolAssignments(file);
			return ResponseEntity.ok(programDtos);

		} catch (IOException e) {
			return ResponseEntity.status(500).body(e.getMessage());
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("予期せぬエラーが発生しました。");
		}
	}
}