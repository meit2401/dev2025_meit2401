// jp/ac/kinki_pc/service/LineManagementService.java

package jp.ac.kinki_pc.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.ac.kinki_pc.dto.LineDto;
import jp.ac.kinki_pc.dto.ProgramDto;
import jp.ac.kinki_pc.dto.ToolAssignmentDto;
import jp.ac.kinki_pc.dto.ToolDto;
import jp.ac.kinki_pc.entity.Line;
import jp.ac.kinki_pc.entity.Program;
import jp.ac.kinki_pc.entity.Tool;
import jp.ac.kinki_pc.entity.ToolAssignment;
import jp.ac.kinki_pc.repository.LineRepository;
import jp.ac.kinki_pc.repository.ProgramRepository;
import jp.ac.kinki_pc.repository.ToolAssignmentRepository;
import jp.ac.kinki_pc.repository.ToolRepository;

@Service
public class LineManagementService {

	@Autowired
	private LineRepository lineRepository;

	@Autowired
	private ProgramRepository programRepository;

	@Autowired
	private ToolAssignmentRepository toolAssignmentRepository;
	
	@Autowired
	private ToolRepository toolRepository;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	// (新規) QRコード用のフォーマッタ
	private static final DateTimeFormatter QR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	public List<LineDto> findAllLines() {
		return lineRepository.findAll().stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	public List<ProgramDto> findProgramsByLineId(Integer lineId) {
		return programRepository.findByLineId(lineId).stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}
	
	/**
	 * [修正] リポジトリからエンティティを取得し、サービス層でDTOに変換する
	 * [修正] toolRepository.findToolsByIds() を toolRepository.findAllById() に変更
	 */
	public List<ToolAssignmentDto> findToolAssignmentsByProgramId(Integer programId) {
		// 1. まず、割り当てエンティティのリストを取得 (Toolマスタの情報はまだ無い)
		List<ToolAssignment> assignments = toolAssignmentRepository.findByProgramId(programId);
		
		if (assignments.isEmpty()) {
			return new ArrayList<>(); // 空のリストを返す
		}

		// 2. N+1問題を回避するため、必要な工具ID(basicToolId)を全て集める
		List<Integer> toolIds = assignments.stream()
			.map(ToolAssignment::getBasicToolId)
			.distinct()
			.collect(Collectors.toList());

		// 3. [修正] JpaRepository が提供する findAllById() メソッドを呼び出す
		Map<Integer, Tool> toolMap = new HashMap<>();
		if (!toolIds.isEmpty()) {
			// [修正] findToolsByIds() ではなく、JpaRepository 標準の findAllById() を使用
			List<Tool> tools = toolRepository.findAllById(toolIds); 
			
			// 4. basicToolId をキーにした Map に変換し、高速にアクセスできるようにする
			toolMap = tools.stream()
				.collect(Collectors.toMap(Tool::getBasicToolId, tool -> tool));
		}

		// 5. 割り当てリストをループ処理し、Mapから工具マスタ情報を取得してDTOに詰める
		List<ToolAssignmentDto> dtos = new ArrayList<>();
		for (ToolAssignment ta : assignments) {
			// Mapから対応するToolエンティティを取得
			Tool tool = toolMap.get(ta.getBasicToolId()); 
			
			if (tool != null) {
				// マスタ情報が見つかった場合
				dtos.add(new ToolAssignmentDto(
					ta.getToolNum(),
					tool.getToolCategory(),
					tool.getMaker(),
					tool.getToolName(),
					tool.getToolMaterial()
				));
			} else {
				// (念のため) 割り当てはあるがマスタに工具が存在しない場合
				dtos.add(new ToolAssignmentDto(
					ta.getToolNum(),
					"N/A",
					"N/A",
					"[マスタ未登録]", // 該当なし
					"N/A"
				));
			}
		}
		
		return dtos;
	}
	
	public Optional<Program> findProgramEntityById(Integer programId) {
		return programRepository.findById(programId);
	}
	
	/**
	 * 絞り込み条件に一致する工具マスタのリストを取得する
	 * @param category 分類
	 * @param maker メーカー
	 * @param toolName 型番 (工具名)
	 * @param material 材質
	 * @return 工具DTOのリスト
	 */
	public List<ToolDto> findFilteredTools(String category, String maker, String toolName, String material) { // 戻り値を List<ToolDto> に変更
		// [修正] is_frozen = 0 (有効) のレコードのみ抽出するように変更
		StringBuilder sql = new StringBuilder("SELECT * FROM mst_tool WHERE is_frozen = 0");
		List<Object> params = new java.util.ArrayList<>();

		if (category != null && !category.isEmpty()) {
			sql.append(" AND tool_category = ?");
			params.add(category);
		}
		if (maker != null && !maker.isEmpty()) {
			sql.append(" AND maker LIKE ?");
			params.add("%" + maker + "%");
		}
		if (toolName != null && !toolName.isEmpty()) {
			sql.append(" AND tool_name LIKE ?");
			params.add("%" + toolName + "%");
		}
		if (material != null && !material.isEmpty()) {
			sql.append(" AND tool_material LIKE ?");
			params.add("%" + material + "%");
		}
		sql.append(" ORDER BY basic_tool_id");

		// Toolエンティティ（Tool.java提供）にマッピングする
		RowMapper<Tool> rowMapper = new RowMapper<Tool>() {
			@Override
			public Tool mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new Tool(
					rs.getInt("basic_tool_id"),
					rs.getString("tool_name"),
					rs.getString("maker"),
					rs.getString("tool_category"),
					rs.getString("tool_material"),
					rs.getInt("stc"),
					rs.getInt("rop"),
					rs.getString("buyer"),
					rs.getBoolean("is_frozen") // [修正] isFrozenを追加
				);
			}
		};

		// まず Entity のリストとして取得
		List<Tool> tools = jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
		
		// Entity のリストを DTO のリストに変換して返す
		return tools.stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	@Transactional
	public LineDto addLine(LineDto lineDto) { // 戻り値を LineDto に変更
		Line line = new Line(null, lineDto.getLineName());
		Line savedLine = lineRepository.save(line);
		return convertToDto(savedLine); // Service内で変換して返す
	}

	@Transactional
	public void deleteLine(Integer lineId) {
		List<Integer> programIdsToDelete = programRepository.findIdsByLineId(lineId);
		for (Integer programId : programIdsToDelete) {
			this.deleteProgram(programId);
		}
		lineRepository.deleteById(lineId);
	}

	@Transactional
	public ProgramDto addProgram(ProgramDto programDto) { // 戻り値を ProgramDto に変更
		Program program = new Program(
			null,
			programDto.getLineId(),
			programDto.getProgramName(),
			LocalDateTime.now()
		);
		Program savedProgram = programRepository.save(program);
		return convertToDto(savedProgram); // Service内で変換して返す
	}

	@Transactional
	public void deleteProgram(Integer programId) {
		toolAssignmentRepository.deleteByProgramId(programId);
		programRepository.deleteById(programId);
	}
	
	/**
	 * プログラムのQRコード情報（タイムスタンプ）を更新する
	 * @param programId 更新するプログラムのID
	 * @return 更新後のProgramDto
	 */
	public ProgramDto updateProgramTimestamp(Integer programId) { // 戻り値を ProgramDto に変更
		Optional<Program> currentProgramOptional = programRepository.findById(programId);
		if (currentProgramOptional.isPresent()) {
			Program programToUpdate = currentProgramOptional.get();
			programToUpdate.setProgramPrintTime(LocalDateTime.now());
			
			// [修正] JpaRepository の規約に従い、update() ではなく save() を使用する
			programRepository.save(programToUpdate); 
			
			return convertToDto(programToUpdate); // Service内で変換して返す
		}
		return null;
	}
	
	/**
	 * プログラムに工具を割り当てる (既存の場合は更新、ない場合は新規登録)
	 * [修正] リポジトリの findBy... が Optional<ToolAssignment> を返すように変更されたため、
	 * nullチェックを isPresent() に変更し、save() メソッドで更新・挿入を行う。
	 * @param programId プログラムID
	 * @param toolNum ツール番号 ("T01", "T02", ...)
	 * @param basicToolId 基本工具ID
	 */
	@Transactional
	public void assignToolToProgram(Integer programId, String toolNum, Integer basicToolId) {
		// ツール番号とプログラムIDで既存の割り当てを探す [修正]
		Optional<ToolAssignment> existingAssignment = toolAssignmentRepository.findByProgramIdAndToolNum(programId, toolNum);
		
		ToolAssignment assignmentToSave;
		if (existingAssignment.isPresent()) {
			// 存在する場合は更新
			assignmentToSave = existingAssignment.get();
			assignmentToSave.setBasicToolId(basicToolId);
		} else {
			// 存在しない場合は挿入
			assignmentToSave = new ToolAssignment(null, programId, toolNum, basicToolId);
		}
		
		// 1つの save() メソッドで挿入・更新の両方を処理
		toolAssignmentRepository.save(assignmentToSave);
	}
	
	/**
	 * (新規) プログラムから工具の割り当てを解除する
	 * @param programId プログラムID
	 * @param toolNum ツール番号 ("T01", "T02", ...)
	 */
	@Transactional
	public void deallocateToolFromProgram(Integer programId, String toolNum) {
		toolAssignmentRepository.deleteByProgramIdAndToolNum(programId, toolNum);
	}
	
	/**
	 * Excelファイルから工具割当データをインポートする
	 * [修正] リポジトリの saveAll() が List<ToolAssignment> を受け取るように変更されたため、
	 * バッチ挿入用リスト (assignmentsToSave) の型と、データ追加方法を変更する。
	 * @param file アップロードされたExcelファイル
	 * @return インポートされたプログラムのDTOリスト
	 * @throws IOException ファイル読み込み、POI処理エラー
	 * @throws RuntimeException データ不備、DB処理エラー、工具マスタ不整合エラー
	 */
	@Transactional
	public List<ProgramDto> importToolAssignments(MultipartFile file) throws IOException, RuntimeException { // 戻り値を List<ProgramDto> に変更
		
		try (InputStream is = file.getInputStream();
			 // .xlsx 形式 (Excel 2007以降) を想定
			 Workbook workbook = new XSSFWorkbook(is)) {

			// === 1. Excel読み取り (省略) ===
			
			Sheet sheet = workbook.getSheetAt(0); // 最初のシートを取得
			
			// E2セル (ライン名)
			Row rowE2 = sheet.getRow(1); // 2行目 (インデックス 1)
			Cell cellE2 = (rowE2 != null) ? rowE2.getCell(4) : null; // E列 (インデックス 4)
			
			if (cellE2 == null) {
				throw new RuntimeException("E2セル（ライン名）が読み取れません。セル結合やフォーマットを確認してください。");
			}
			
			String lineName;
			try {
				lineName = getCellValueAsString(cellE2);
			} catch (Exception e) {
				throw new RuntimeException("E2セル（ライン名）の形式が読み取れません。セル結合やフォーマットを確認してください。", e);
			}
			
			if (lineName == null || lineName.isBlank()) {
				throw new RuntimeException("E2セル（ライン名）が空です。セル結合やフォーマットを確認してください。");
			}
			
			lineName = lineName.trim(); 

			// プログラム名 (B4:V4)
			Row programRow = sheet.getRow(3); // 4行目 (インデックス 3)
			if (programRow == null) {
				throw new RuntimeException("4行目（プログラム名）が読み取れません。セル結合やフォーマットを確認してください。");
			}

			Map<Integer, String> columnToProgramName = new HashMap<>();
			List<Program> programsToSave = new ArrayList<>();
			LocalDateTime now = LocalDateTime.now();

			for (int colIndex = 1; colIndex <= 21; colIndex++) { // B列(1)からV列(21)
				Cell programCell = programRow.getCell(colIndex);
				String programName = null;
				
				if (programCell != null) {
					programName = getCellValueAsString(programCell); 
				}

				if (programName != null && !programName.isBlank()) {
					programName = programName.trim();
					columnToProgramName.put(colIndex, programName); 
					
					Program newProgram = new Program();
					newProgram.setProgramName(programName);
					newProgram.setProgramPrintTime(now);
					programsToSave.add(newProgram);
				}
			}
			
			// [新規] 工具名の収集 (B6:V25 ～ V6:V25)
			java.util.Set<String> normalizedToolNames = new java.util.HashSet<>();
			
			for (Integer colIndex : columnToProgramName.keySet()) {
				for (int rowIndex = 5; rowIndex <= 24; rowIndex++) {
					Row toolRow = sheet.getRow(rowIndex);
					if (toolRow == null) continue; 
					
					Cell toolCell = toolRow.getCell(colIndex);
					String rawToolName = null;

					if (toolCell != null) {
						rawToolName = getCellValueAsString(toolCell);
					}
					
					if (rawToolName != null && !rawToolName.isBlank()) {
						String normalizedToolName = rawToolName.replaceAll("\\s+", "");
						if (!normalizedToolName.isEmpty()) {
							normalizedToolNames.add(normalizedToolName);
						}
					}
				}
			}

			// === 2. [新規] 事前検証 (工具マスタ存在チェック) (省略) ===
			
			List<String> missingToolNames = new ArrayList<>();
			Map<String, Tool> toolCache = new HashMap<>(); 

			for (String normalizedName : normalizedToolNames) {
				Tool foundTool = findToolByNormalizedName(normalizedName);
				if (foundTool == null) {
					missingToolNames.add(normalizedName);
				} else {
					toolCache.put(normalizedName, foundTool); 
				}
			}

			// === 3. [新規] 検証結果の判定 (省略) ===
			
			if (!missingToolNames.isEmpty()) {
				String missingToolsList = missingToolNames.stream()
					.limit(10) 
					.map(name -> "・" + name) 
					.collect(Collectors.joining("\n")); 
				
				throw new RuntimeException(String.format(
					"以下の工具が工具マスタに登録されていません。\n%s%s",
					missingToolsList,
					(missingToolNames.size() > 10) ? "\n..." : ""
				));
			}

			// === 4. DB保存フェーズ (ここから下は、事前検証が成功した場合のみ実行) ===
			
			// (4-1. ラインの処理) (省略)
			// [修正] LineRepository は JpaRepository になっている
			Optional<Line> existingLineOptional = lineRepository.findByLineName(lineName);
			
			Line targetLine; // このインポート処理で対象となるライン

			if (existingLineOptional.isPresent()) {
				// [あれば]：関連データを削除
				Line existingLine = existingLineOptional.get();
				Integer lineId = existingLine.getLineId();
				List<Integer> programIds = programRepository.findIdsByLineId(lineId);
				if (!programIds.isEmpty()) {
					// [修正] JpaRepository の命名規則メソッドを使用
					toolAssignmentRepository.deleteByProgramIdIn(programIds);
					programRepository.deleteByLineId(lineId);
				}
				targetLine = existingLine;
			} else {
				// [なければ]：ライン名を新規登録
				Line newLine = new Line();
				newLine.setLineName(lineName);
				targetLine = lineRepository.save(newLine);
			}
			
			// (4-2. プログラムリストを一括挿入) (省略)
			
			for (Program p : programsToSave) {
				p.setLineId(targetLine.getLineId());
			}

			if (!programsToSave.isEmpty()) {
				// [修正] JpaRepository の saveAll を使用
				programRepository.saveAll(programsToSave);
			}

			// (4-3. 登録したプログラムIDを取得) (省略)
			
			List<Program> savedPrograms = programRepository.findByLineId(targetLine.getLineId());
			
			Map<String, Integer> programNameToIdMap = savedPrograms.stream()
				.collect(Collectors.toMap(Program::getProgramName, Program::getProgramId));

			// [修正] バッチ挿入用のリスト (ToolAssignment エンティティに変更)
			List<ToolAssignment> assignmentsToSave = new ArrayList<>();

			// (4-4. 工具割当 (x6:x25) を *再度* スキャンして割当リスト作成)
			
			for (Map.Entry<Integer, String> entry : columnToProgramName.entrySet()) {
				int colIndex = entry.getKey();
				String programName = entry.getValue();
				
				Integer programId = programNameToIdMap.get(programName);
				if (programId == null) {
					continue;
				}

				// 6行目(インデックス 5) から 25行目(インデックス 24) までループ (T1～T20)
				for (int rowIndex = 5; rowIndex <= 24; rowIndex++) {
					Row toolRow = sheet.getRow(rowIndex);
					if (toolRow == null) continue;
					
					Cell toolCell = toolRow.getCell(colIndex);
					String rawToolName = null;

					if (toolCell != null) {
						rawToolName = getCellValueAsString(toolCell);
					}
					
					if (rawToolName != null && !rawToolName.isBlank()) {
						
						// (4-5. 工具名を正規化して *キャッシュ* を検索)
						String normalizedToolName = rawToolName.replaceAll("\\s+", "");
						Tool foundTool = toolCache.get(normalizedToolName); 
						
						if (foundTool != null) {
							// [修正] "0" ではなく "T01" 形式で toolNum を生成
							// rowIndex 5 -> 1 (T01)
							// rowIndex 24 -> 20 (T20)
							int toolNumInt = (rowIndex - 5) + 1; // 1 から 20 までの数値
							String toolNum = String.format("T%02d", toolNumInt); // "T01", "T02", ..., "T20"
							
							// [修正] バッチ挿入用リストに Object[] ではなく ToolAssignment エンティティを追加
							assignmentsToSave.add(new ToolAssignment(
								null, // toolAssignmentId (自動採番)
								programId,
								toolNum, 
								foundTool.getBasicToolId()
							));
						}
					}
				}
			}

			// (4-6. 工具割当を一括挿入)
			if (!assignmentsToSave.isEmpty()) {
				// [修正] JpaRepository の saveAll を使用
				toolAssignmentRepository.saveAll(assignmentsToSave);
			}

			// (4-7. 処理が成功した場合、登録されたプログラムの *DTO* リストを返す)
			return savedPrograms.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());

		} catch (IOException e) {
			// (省略)
			throw new IOException("Excelファイルの読み込みに失敗しました。", e);
		} catch (RuntimeException e) { 
			// (省略)
			throw e;
		} catch (Exception e) {
			// (省略)
			throw new RuntimeException("予期せぬエラーが発生しました。", e);
		}
	}
	
	/**
	 * (新規) Excelのセル型を問わず、文字列として値を取得するヘルパーメソッド
	 * @param cell 読み取るセル
	 * @return セルの文字列表現 (空セルの場合は null)
	 */
	private String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return null;
		}
		
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				// 数値の場合、整数なら整数文字列、小数なら小数文字列に変換
				double numericValue = cell.getNumericCellValue();
				if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
					// 整数 (例: 123.0 -> "123")
					return String.valueOf((long) numericValue);
				} else {
					// 小数 (例: 1.23 -> "1.23")
					return String.valueOf(numericValue);
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				// 数式の場合は計算結果の型を判定 (簡易版)
				try {
					switch (cell.getCachedFormulaResultType()) {
						case STRING:
							return cell.getStringCellValue();
						case NUMERIC:
							double formulaNumericValue = cell.getNumericCellValue();
							if (formulaNumericValue == Math.floor(formulaNumericValue) && !Double.isInfinite(formulaNumericValue)) {
								return String.valueOf((long) formulaNumericValue);
							} else {
								return String.valueOf(formulaNumericValue);
							}
						case BOOLEAN:
							return String.valueOf(cell.getBooleanCellValue());
						default:
							return null; // ERROR, BLANK
					}
				} catch (IllegalStateException e) {
					// (数式が文字列を返すよう設定されているのに数値を取得しようとした場合など)
					try {
						return cell.getStringCellValue();
					} catch (Exception e2) {
						return null; // 読み取り失敗
					}
				}
			case BLANK:
				return null;
			default:
				return null;
		}
	}
	
	/**
	 * 正規化された工具名（空白・改行除去済み）で mst_tool を検索する
	 * @param normalizedName Excelから読み取り正規化された工具名 (例: "100FAC-A")
	 * @return 見つかった Tool オブジェクト。見つからない場合は null
	 */
	private Tool findToolByNormalizedName(String normalizedName) {
		// mst_tool の tool_name も空白・改行を除去 (REGEXP_REPLACE) して比較する
		// [[:space:]] は空白、タブ、改行など全ての空白文字にマッチする
		// (MariaDB 10.0.5+ / MySQL 8.0+ が必要)
		String sql = "SELECT * FROM mst_tool WHERE REGEXP_REPLACE(tool_name, '[[:space:]]', '') = ?";
		
		// Toolエンティティ用のRowMapper
		RowMapper<Tool> toolRowMapper = new RowMapper<Tool>() {
			@Override
			public Tool mapRow(ResultSet rs, int rowNum) throws SQLException {
				// [修正] Toolエンティティの全フィールドをマッピング
				return new Tool(
					rs.getInt("basic_tool_id"),
					rs.getString("tool_name"),
					rs.getString("maker"),
					rs.getString("tool_category"),
					rs.getString("tool_material"),
					rs.getInt("stc"),
					rs.getInt("rop"),
					rs.getString("buyer"),
					rs.getBoolean("is_frozen") // [修正] isFrozenを追加
				);
			}
		};

		try {
			// queryForObject は結果が1件でないと例外をスローするため、query を使用
			List<Tool> tools = jdbcTemplate.query(sql, toolRowMapper, normalizedName);
			if (!tools.isEmpty()) {
				// 正規化された名前が万が一重複した場合、最初に見つかったものを返す
				return tools.get(0); 
			}
			return null; // 0件の場合
		} catch (EmptyResultDataAccessException e) {
			return null; // (念のため)
		}
	}
	
	public LineDto convertToDto(Line line) {
		return new LineDto(line.getLineId(), line.getLineName());
	}

	public ProgramDto convertToDto(Program program) {
		String formattedTimestamp = (program.getProgramPrintTime() != null)
			? program.getProgramPrintTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
			: null;
		return new ProgramDto(
			program.getProgramId(),
			program.getLineId(),
			program.getProgramName(),
			formattedTimestamp
		);
	}
	
	/**
	 * (新規) ToolエンティティをToolDtoに変換する
	 * @param tool 変換元のToolエンティティ
	 * @return 変換後のToolDto
	 */
	public ToolDto convertToDto(Tool tool) {
		if (tool == null) {
			return null;
		}
		return new ToolDto(
			tool.getBasicToolId(),
			tool.getToolName(),
			tool.getMaker(),
			tool.getToolCategory(),
			tool.getToolMaterial(),
			tool.getStc(),
			tool.getRop(),
			tool.getBuyer()
		);
	}
	
	/**
	 * (新規) QRコード印刷用のデータを取得する
	 * @param programId プログラムID
	 * @return QRコードに必要な情報 (displayText, qrCodeData) を格納したMap。見つからない場合はnull
	 */
	public Map<String, String> getQrDataForProgram(Integer programId) {
		Optional<Program> programOptional = programRepository.findById(programId);
		if (programOptional.isEmpty()) {
			return null;
		}
		Program program = programOptional.get();
		
		String displayText = program.getProgramName();
		String qrCodeData = generateQrCodeData("P", program.getProgramId(), program.getProgramPrintTime());

		Map<String, String> qrData = new HashMap<>();
		qrData.put("displayText", displayText);
		qrData.put("qrCodeData", qrCodeData);
		
		return qrData;
	}

	/**
	 * (新規) QRコード用のデータ文字列を生成する
	 * @param prefix プレフィックス (U, P など)
	 * @param id ID
	 * @param timestamp タイムスタンプ
	 * @return フォーマットされたQRコードデータ文字列
	 */
	private String generateQrCodeData(String prefix, Integer id, LocalDateTime timestamp) {
		String formattedId = String.format("%s%03d", prefix, id);
		// QRコード用のフォーマッタ (Controllerから移動)
		String formattedTimestamp = (timestamp != null) ? timestamp.format(QR_FORMATTER) : "";
		return formattedId + "-" + formattedTimestamp;
	}
}