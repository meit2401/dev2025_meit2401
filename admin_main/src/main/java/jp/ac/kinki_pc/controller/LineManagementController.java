// jp/ac/kinki_pc/controller/LineManagementController.java

package jp.ac.kinki_pc.controller;

import java.io.IOException;
import java.util.List;

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
		LineDto savedLineDto = lineManagementService.addLine(newLineDto); // ServiceがDTOを直接返す
		// LineDto savedLineDto = lineManagementService.convertToDto(savedLine); // 変換処理を削除
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
		ProgramDto savedProgramDto = lineManagementService.addProgram(newProgramDto); // ServiceがDTOを直接返す
		// ProgramDto savedProgramDto = lineManagementService.convertToDto(savedProgram); // 変換処理を削除
		return ResponseEntity.ok(savedProgramDto);
	}

	@PostMapping("/program/delete")
	@ResponseBody
	public ResponseEntity<String> deleteProgram(@RequestParam("programId") Integer programId) {
		lineManagementService.deleteProgram(programId);
		return ResponseEntity.ok("deleted");
	}
	
	/**
	 * 指定されたプログラムIDに紐づく工具割当情報を取得する
	 * @param programId プログラムID
	 * @return 工具割当情報DTOのリストを含むResponseEntity
	 */
	@GetMapping("/program/tools")
	@ResponseBody
	public ResponseEntity<List<ToolAssignmentDto>> getToolAssignments(@RequestParam("programId") Integer programId) {
		List<ToolAssignmentDto> assignments = lineManagementService.findToolAssignmentsByProgramId(programId);
		return ResponseEntity.ok(assignments);
	}
	
	/**
	 * 絞り込み条件に一致する工具マスタのリストを返す
	 * @param category 分類
	 * @param maker メーカー
	 * @param toolName 型番 (工具名)
	 * @param material 材質
	 * @return 工具DTOのリスト
	 */
	@GetMapping("/tools/filter")
	@ResponseBody
	public ResponseEntity<List<ToolDto>> getFilteredTools( // 戻り値を List<ToolDto> に変更
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "maker", required = false) String maker,
			@RequestParam(value = "toolName", required = false) String toolName,
			@RequestParam(value = "material", required = false) String material) {
		
		List<ToolDto> tools = lineManagementService.findFilteredTools(category, maker, toolName, material); // Serviceが DTO のリストを直接返す
		return ResponseEntity.ok(tools);
	}

	/**
	 * 指定されたプログラムの指定されたツール番号に、基本工具IDを割り当てる
	 * @param programId プログラムID
	 * @param toolNum ツール番号 ("0", "1"...)
	 * @param basicToolId 基本工具ID
	 * @return 成功レスポンス
	 */
	@PostMapping("/program/tool/assign")
	@ResponseBody
	public ResponseEntity<String> assignTool(
			@RequestParam("programId") Integer programId,
			@RequestParam("toolNum") String toolNum,
			@RequestParam("basicToolId") Integer basicToolId) {
		
		lineManagementService.assignToolToProgram(programId, toolNum, basicToolId);
		return ResponseEntity.ok("assigned");
	}

	/**
	 * (新規) 指定されたプログラムの指定されたツール番号の割り当てを解除する
	 * @param programId プログラムID
	 * @param toolNum ツール番号 ("0", "1"...)
	 * @return 成功レスポンス
	 */
	@PostMapping("/program/tool/deallocate")
	@ResponseBody
	public ResponseEntity<String> deallocateTool(
			@RequestParam("programId") Integer programId,
			@RequestParam("toolNum") String toolNum) {
		
		lineManagementService.deallocateToolFromProgram(programId, toolNum);
		return ResponseEntity.ok("deallocated");
	}

	/**
	 * QRコード再印刷のためにタイムスタンプを更新し、更新後のプログラム情報を返す
	 * @param programId プログラムID
	 * @return 更新後のプログラム情報(DTO)を含むResponseEntity
	 */
	@PostMapping("/program/reprint-qr")
	@ResponseBody
	public ResponseEntity<ProgramDto> reprintQrCode(@RequestParam("programId") Integer programId) {
		ProgramDto updatedProgramDto = lineManagementService.updateProgramTimestamp(programId); // ServiceがDTOを直接返す
		if (updatedProgramDto != null) {
			// ProgramDto updatedProgramDto = lineManagementService.convertToDto(updatedProgram); // 変換処理を削除
			return ResponseEntity.ok(updatedProgramDto);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	/**
	 * 工具割当データをファイルからインポートする
	 * @param file JSから送られてくるアップロードファイル (FormData の 'file' に対応)
	 * @return 処理結果(成功時はProgramDtoのリスト、エラー時はエラーメッセージ)
	 */
	@PostMapping("/program/tool/import") // /line/program/tool/import でPOSTを受け取る
	@ResponseBody // ページ遷移せず、データ(文字列など)を返す
	// public ResponseEntity<String> handleToolImport(@RequestParam("file") MultipartFile file) {
	public ResponseEntity<?> handleToolImport(@RequestParam("file") MultipartFile file) { // 戻り値を汎用化
		
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("ファイルが選択されていません。");
		}

		try {
			// Service層のメソッドを呼び出してインポート処理を実行
			// Service層が DTO のリストを直接返す
			List<ProgramDto> programDtos = lineManagementService.importToolAssignments(file); 

			// ControllerでのDTO変換処理は不要になった
			// List<ProgramDto> programDtos = importedPrograms.stream()
			// 		.map(lineManagementService::convertToDto)
			// 		.collect(Collectors.toList());
			
			return ResponseEntity.ok(programDtos); // DTOのリストを返す

		} catch (IOException e) {
			// [修正] ファイルI/Oエラー (Service層からのメッセージをそのまま使用)
			return ResponseEntity.status(500).body(e.getMessage());
		} catch (RuntimeException e) {
			// [修正] Service層でスローされた業務エラー (E2セル不備、DBエラーなど)
			// Service層からのメッセージ(e.getMessage())をそのまま使用
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			// [修正] その他の予期せぬエラー (詳細は隠蔽)
			return ResponseEntity.status(500).body("予期せぬエラーが発生しました。");
		}
	}
}