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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(LineManagementService.class);
	
	// 日時フォーマット定義 (ロールバック用とQR用)
	private static final DateTimeFormatter DTO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
	
	public List<ToolAssignmentDto> findToolAssignmentsByProgramId(Integer programId) {
		List<ToolAssignment> assignments = toolAssignmentRepository.findByProgramId(programId);
		
		if (assignments.isEmpty()) {
			return new ArrayList<>();
		}

		List<Integer> toolIds = assignments.stream()
			.map(ToolAssignment::getBasicToolId)
			.distinct()
			.collect(Collectors.toList());

		Map<Integer, Tool> toolMap = new HashMap<>();
		if (!toolIds.isEmpty()) {
			List<Tool> tools = toolRepository.findAllById(toolIds); 
			
			toolMap = tools.stream()
				.collect(Collectors.toMap(Tool::getBasicToolId, tool -> tool));
		}

		List<ToolAssignmentDto> dtos = new ArrayList<>();
		for (ToolAssignment ta : assignments) {
			Tool tool = toolMap.get(ta.getBasicToolId()); 
			
			if (tool != null) {
				dtos.add(new ToolAssignmentDto(
					ta.getToolNum(),
					tool.getToolCategory(),
					tool.getMaker(),
					tool.getToolName(),
					tool.getToolMaterial()
				));
			} else {
				dtos.add(new ToolAssignmentDto(
					ta.getToolNum(),
					"N/A",
					"N/A",
					"[マスタ未登録]",
					"N/A"
				));
			}
		}
		
		return dtos;
	}
	
	public Optional<Program> findProgramEntityById(Integer programId) {
		return programRepository.findById(programId);
	}
	
	public List<ToolDto> findFilteredTools(String category, String maker, String toolName, String material) {
		StringBuilder sql = new StringBuilder("SELECT * FROM mst_tool WHERE 1=1");
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
					rs.getString("buyer")
				);
			}
		};

		List<Tool> tools = jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
		
		return tools.stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	@Transactional
	public LineDto addLine(LineDto lineDto) {
		// Lineコンストラクタ(Integer, String, Boolean)に適合するように修正
		Line line = new Line(null, lineDto.getLineName(), false);
		Line savedLine = lineRepository.save(line);
		return convertToDto(savedLine);
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
	public ProgramDto addProgram(ProgramDto programDto) {
		Program program = new Program(
			null,
			programDto.getLineId(),
			programDto.getProgramName(),
			LocalDateTime.now()
		);
		Program savedProgram = programRepository.save(program);
		return convertToDto(savedProgram);
	}

	@Transactional
	public void deleteProgram(Integer programId) {
		toolAssignmentRepository.deleteByProgramId(programId);
		programRepository.deleteById(programId);
	}
	
	public ProgramDto updateProgramTimestamp(Integer programId) {
		Optional<Program> currentProgramOptional = programRepository.findById(programId);
		if (currentProgramOptional.isPresent()) {
			Program programToUpdate = currentProgramOptional.get();
			LocalDateTime newDatetime = LocalDateTime.now();
			programToUpdate.setProgramPrintTime(newDatetime);
			
			programRepository.save(programToUpdate); 
			
			logger.info("プログラムのタイムスタンプを更新しました。 ProgramID: {}, New Time: {}", programId, newDatetime.format(QR_FORMATTER));
			return convertToDto(programToUpdate);
		}
		return null;
	}

	/**
	 * プログラムのタイムスタンプを指定された値（文字列）に戻す
	 * @param programId プログラムID
	 * @param timestampStr ロールバックする日時文字列(yyyy-MM-dd HH:mm:ss)
	 */
	public void restoreProgramTimestamp(Integer programId, String timestampStr) {
		Optional<Program> currentProgramOptional = programRepository.findById(programId);
		if (currentProgramOptional.isPresent()) {
			Program program = currentProgramOptional.get();
			try {
				if (timestampStr != null && !timestampStr.isEmpty()) {
					LocalDateTime dt = LocalDateTime.parse(timestampStr, DTO_FORMATTER);
					program.setProgramPrintTime(dt);
				} else {
					program.setProgramPrintTime(null);
				}
				programRepository.save(program);
				logger.info("プログラムのタイムスタンプをロールバックしました。 ProgramID: {}", programId);
			} catch (Exception e) {
				logger.error("プログラムのタイムスタンプの復元に失敗しました。 ProgramID: {}, Error: {}", programId, e.getMessage());
			}
		}
	}
	
	@Transactional
	public void assignToolToProgram(Integer programId, String toolNum, Integer basicToolId) {
		Optional<ToolAssignment> existingAssignment = toolAssignmentRepository.findByProgramIdAndToolNum(programId, toolNum);
		
		ToolAssignment assignmentToSave;
		if (existingAssignment.isPresent()) {
			assignmentToSave = existingAssignment.get();
			assignmentToSave.setBasicToolId(basicToolId);
		} else {
			assignmentToSave = new ToolAssignment(null, programId, toolNum, basicToolId);
		}
		
		toolAssignmentRepository.save(assignmentToSave);
	}
	
	@Transactional
	public void deallocateToolFromProgram(Integer programId, String toolNum) {
		toolAssignmentRepository.deleteByProgramIdAndToolNum(programId, toolNum);
	}
	
	@Transactional
	public List<ProgramDto> importToolAssignments(MultipartFile file) throws IOException, RuntimeException {
		
		try (InputStream is = file.getInputStream();
			 Workbook workbook = new XSSFWorkbook(is)) {

			Sheet sheet = workbook.getSheetAt(0);
			
			Row rowE2 = sheet.getRow(1);
			Cell cellE2 = (rowE2 != null) ? rowE2.getCell(4) : null;
			
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

			Row programRow = sheet.getRow(3);
			if (programRow == null) {
				throw new RuntimeException("4行目（プログラム名）が読み取れません。セル結合やフォーマットを確認してください。");
			}

			Map<Integer, String> columnToProgramName = new HashMap<>();
			List<Program> programsToSave = new ArrayList<>();
			LocalDateTime now = LocalDateTime.now();

			for (int colIndex = 1; colIndex <= 21; colIndex++) {
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

			Optional<Line> existingLineOptional = lineRepository.findByLineName(lineName);
			
			Line targetLine;

			if (existingLineOptional.isPresent()) {
				Line existingLine = existingLineOptional.get();
				Integer lineId = existingLine.getLineId();
				List<Integer> programIds = programRepository.findIdsByLineId(lineId);
				if (!programIds.isEmpty()) {
					toolAssignmentRepository.deleteByProgramIdIn(programIds);
					programRepository.deleteByLineId(lineId);
				}
				targetLine = existingLine;
			} else {
				Line newLine = new Line();
				newLine.setLineName(lineName);
				targetLine = lineRepository.save(newLine);
			}
			
			for (Program p : programsToSave) {
				p.setLineId(targetLine.getLineId());
			}

			if (!programsToSave.isEmpty()) {
				programRepository.saveAll(programsToSave);
			}

			List<Program> savedPrograms = programRepository.findByLineId(targetLine.getLineId());
			
			Map<String, Integer> programNameToIdMap = savedPrograms.stream()
				.collect(Collectors.toMap(Program::getProgramName, Program::getProgramId));

			List<ToolAssignment> assignmentsToSave = new ArrayList<>();

			for (Map.Entry<Integer, String> entry : columnToProgramName.entrySet()) {
				int colIndex = entry.getKey();
				String programName = entry.getValue();
				
				Integer programId = programNameToIdMap.get(programName);
				if (programId == null) {
					continue;
				}

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
						Tool foundTool = toolCache.get(normalizedToolName); 
						
						if (foundTool != null) {
							int toolNumInt = (rowIndex - 5) + 1;
							String toolNum = String.format("T%02d", toolNumInt);
							
							assignmentsToSave.add(new ToolAssignment(
								null,
								programId,
								toolNum, 
								foundTool.getBasicToolId()
							));
						}
					}
				}
			}

			if (!assignmentsToSave.isEmpty()) {
				toolAssignmentRepository.saveAll(assignmentsToSave);
			}

			return savedPrograms.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());

		} catch (IOException e) {
			throw new IOException("Excelファイルの読み込みに失敗しました。", e);
		} catch (RuntimeException e) { 
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("予期せぬエラーが発生しました。", e);
		}
	}
	
	private String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return null;
		}
		
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				double numericValue = cell.getNumericCellValue();
				if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
					return String.valueOf((long) numericValue);
				} else {
					return String.valueOf(numericValue);
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
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
							return null;
					}
				} catch (IllegalStateException e) {
					try {
						return cell.getStringCellValue();
					} catch (Exception e2) {
						return null;
					}
				}
			case BLANK:
				return null;
			default:
				return null;
		}
	}
	
	private Tool findToolByNormalizedName(String normalizedName) {
		String sql = "SELECT * FROM mst_tool WHERE REGEXP_REPLACE(tool_name, '[[:space:]]', '') = ?";
		
		RowMapper<Tool> toolRowMapper = new RowMapper<Tool>() {
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
					rs.getString("buyer")
				);
			}
		};

		try {
			List<Tool> tools = jdbcTemplate.query(sql, toolRowMapper, normalizedName);
			if (!tools.isEmpty()) {
				return tools.get(0); 
			}
			return null;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	public LineDto convertToDto(Line line) {
		return new LineDto(line.getLineId(), line.getLineName());
	}

	public ProgramDto convertToDto(Program program) {
		String formattedTimestamp = (program.getProgramPrintTime() != null)
			? program.getProgramPrintTime().format(DTO_FORMATTER)
			: null;
		return new ProgramDto(
			program.getProgramId(),
			program.getLineId(),
			program.getProgramName(),
			formattedTimestamp
		);
	}
	
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

	private String generateQrCodeData(String prefix, Integer id, LocalDateTime timestamp) {
		String formattedId = String.format("%s%03d", prefix, id);
		String formattedTimestamp = (timestamp != null) ? timestamp.format(QR_FORMATTER) : "";
		return formattedId + "-" + formattedTimestamp;
	}
}