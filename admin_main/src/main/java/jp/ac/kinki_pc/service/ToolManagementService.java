// jp/ac/kinki_pc/service/ToolManagementService.java
package jp.ac.kinki_pc.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.ac.kinki_pc.dto.AddressDto;
import jp.ac.kinki_pc.dto.IndividualToolDto;
import jp.ac.kinki_pc.dto.ToolDto;
import jp.ac.kinki_pc.entity.Address;
import jp.ac.kinki_pc.entity.StorageArea;
import jp.ac.kinki_pc.entity.Tool;
import jp.ac.kinki_pc.entity.UniqueTool;
import jp.ac.kinki_pc.repository.AddressRepository;
import jp.ac.kinki_pc.repository.StorageAreaRepository;
import jp.ac.kinki_pc.repository.ToolRepository;
import jp.ac.kinki_pc.repository.UniqueToolRepository;

@Service
public class ToolManagementService {

	@Autowired
	private ToolRepository toolRepository;

	@Autowired
	private UniqueToolRepository uniqueToolRepository;

	@Autowired
	private AddressRepository addressRepository;

	@Autowired
	private StorageAreaRepository storageAreaRepository;

	/**
	 * JpaSpecificationExecutor を使用した動的検索
	 */
	public List<ToolDto> searchTools(String maker, String category, String material, String trader, String toolNameFilter) {
		
		// Specification (検索条件) を構築
		Specification<Tool> spec = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			
			if (StringUtils.hasText(category)) {
				predicates.add(builder.equal(root.get("toolCategory"), category));
			}
			if (StringUtils.hasText(maker)) {
				predicates.add(builder.like(root.get("maker"), "%" + maker + "%"));
			}
			if (StringUtils.hasText(material)) {
				predicates.add(builder.like(root.get("toolMaterial"), "%" + material + "%"));
			}
			if (StringUtils.hasText(trader)) {
				predicates.add(builder.like(root.get("buyer"), "%" + trader + "%"));
			}
			if (StringUtils.hasText(toolNameFilter)) {
				predicates.add(builder.like(root.get("toolName"), "%" + toolNameFilter + "%"));
			}
			
			return builder.and(predicates.toArray(new Predicate[0]));
		};

		List<Tool> toolEntities = toolRepository.findAll(spec);
		
		return toolEntities.stream()
						   .map(this::convertEntityToDto)
						   .collect(Collectors.toList());
	}

	public ToolDto findSelectedTool(List<ToolDto> toolList, Integer selectedId) {
		if (selectedId != null && toolList != null) {
			return toolList.stream()
					.filter(tool -> tool.getBasicToolId() == selectedId)
					.findFirst()
					.orElse(null);
		}
		return null;
	}

	/**
	 * JpaRepository の save() を使用
	 */
	@Transactional
	public void addTool(String toolName, String maker, String toolCategory,
						String toolMaterial, int stc, int rop, String buyer, String storageLocation) {
		
		// 1. Tool エンティティを作成 (IDは null)
		Tool newTool = new Tool(null, toolName, maker, toolCategory, toolMaterial, stc, rop, buyer);
		
		// 2. save() を呼び出し、DBで採番されたIDを含むエンティティを受け取る
		Tool savedTool = toolRepository.save(newTool);
		int newBasicToolId = savedTool.getBasicToolId();

		// 3. StorageArea の登録
		if (StringUtils.hasText(storageLocation)) {
			String[] addresses = storageLocation.split(",");
			for (String address : addresses) {
				if (StringUtils.hasText(address.trim())) {
					StorageArea newArea = new StorageArea();
					newArea.setBasicToolId(newBasicToolId);
					newArea.setDisplayAddress(address.trim());
					newArea.setToolcaseStcNum(0); // 初期ケース数は 0
					
					storageAreaRepository.save(newArea);
				}
			}
		}
	}

	/**
	 * 命名規則/規約ベースの delete メソッドを呼び出す
	 */
	@Transactional
	public void deleteTool(int basicToolId) {
		uniqueToolRepository.deleteByBasicToolId(basicToolId);
		storageAreaRepository.deleteByBasicToolId(basicToolId);
		toolRepository.deleteById(basicToolId);
	}
	
	/**
	 * IDの手動計算とエンティティ設定のロジックをサービス層に移行
	 * (10桁IDフォーマット + 異常系チェック を反映)
	 */
	@Transactional
	public long createIndividualTool(int basicToolId, int casePackNumFromForm) {
		Tool tool = toolRepository.findById(basicToolId)
			.orElseThrow(() -> new RuntimeException("対象の工具が見つかりません。basicToolId=" + basicToolId));

		int maxNum = uniqueToolRepository.findMaxUniqueNumByBasicToolId(basicToolId);
		int nextUniqueNum = maxNum + 1;
		int finalCasePackNum;
		String category = tool.getToolCategory();
		if ("インサート".equals(category) || "チップ".equals(category)) {
			finalCasePackNum = casePackNumFromForm;
		} else {
			finalCasePackNum = 1;
		}
		
		// 1. IDを手動で計算 (5桁 + 5桁 の 0埋め連結)
		
		// (異常系： 桁あふれチェック)
		if (basicToolId > 99999) {
			throw new RuntimeException("基本工具IDが5桁の上限を超えています。 ID: " + basicToolId);
		}
		if (nextUniqueNum > 99999) {
			throw new RuntimeException("個別番号が5桁の上限を超えています。 ID: " + nextUniqueNum);
		}
		
		String basicIdPadded = String.format("%05d", basicToolId);
		String uniqueNumPadded = String.format("%05d", nextUniqueNum);
		
		long newUniqueToolId = Long.parseLong(basicIdPadded + uniqueNumPadded);
		
		// 2. Entity を作成
		UniqueTool newUniqueTool = new UniqueTool();
		newUniqueTool.setUniqueToolId(newUniqueToolId); // ID を手動設定
		newUniqueTool.setToolPrintTime(LocalDateTime.now());
		newUniqueTool.setBasicToolId(basicToolId);
		newUniqueTool.setUniqueNum(nextUniqueNum);
		newUniqueTool.setCasePackNum(finalCasePackNum);
		newUniqueTool.setStorageAreaId(null); // (storageAreaId)
		newUniqueTool.setStorageCondition("補充前");
		newUniqueTool.setRegrindCount(0);
		
		// 3. save() を呼び出す
		uniqueToolRepository.save(newUniqueTool);
		
		return newUniqueToolId;
	}

	/**
	 * 10桁ID (5桁+5桁) に対応
	 */
	@Transactional
	public Long reprintQrCode(String qrNumber) {
		if (qrNumber == null || qrNumber.length() != 10) {
			throw new IllegalArgumentException("IDは10桁である必要があります。");
		}
		try {
			// [変更] 3桁/7桁 -> 5桁/5桁 に分割
			int basicToolId = Integer.parseInt(qrNumber.substring(0, 5));
			int uniqueNum = Integer.parseInt(qrNumber.substring(5));
			
			// 1. 検索
			// [変更] (uniqueNum % 10000) -> uniqueNum に変更
			Optional<UniqueTool> toolOptional = uniqueToolRepository.findByBasicToolIdAndUniqueNum(basicToolId, uniqueNum);
			
			if (toolOptional.isPresent()) {
				// 2. 存在すれば更新
				UniqueTool toolToUpdate = toolOptional.get();
				toolToUpdate.setToolPrintTime(LocalDateTime.now());
				uniqueToolRepository.save(toolToUpdate); // save() が更新を実行
				return toolToUpdate.getUniqueToolId();
			} else {
				// 該当なし
				return null;
			}
			
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("IDは数字である必要があります。");
		}
	}

	/**
	 * find/updateロジックをサービス層に移行
	 */
	@Transactional
	public void updateReorderPoint(int basicToolId, int rop) {
		Optional<Tool> toolOptional = toolRepository.findById(basicToolId);
		if (toolOptional.isPresent()) {
			Tool toolToUpdate = toolOptional.get();
			toolToUpdate.setRop(rop);
			toolRepository.save(toolToUpdate);
		}
		// (存在しない場合、エラーをスローするかサイレントに無視するかは要件による)
	}
	
	public java.util.Map<String, Integer> getStorageCounts() {
		List<java.util.Map<String, Object>> rows = storageAreaRepository.getStorageAddressCounts();
		java.util.Map<String, Integer> counts = new java.util.HashMap<>();
		for (java.util.Map<String, Object> row : rows) {
			String address = (String) row.get("display_address");
			Integer count = ((Number) row.get("address_count")).intValue(); 
			counts.put(address, count);
		}
		return counts;
	}

	/**
	 * 命名規則メソッドを呼び出し
	 */
	public int getIndividualToolCount(int basicToolId) {
		return (int) uniqueToolRepository.countByBasicToolId(basicToolId);
	}
	
	/**
	 * 命名規則メソッドを呼び出し
	 */
	public List<IndividualToolDto> getIndividualTools(int basicToolId) {
		List<UniqueTool> entities = uniqueToolRepository.findByBasicToolId(basicToolId);
		return entities.stream()
					   .map(this::convertEntityToDto)
					   .collect(Collectors.toList());
	}
	
	/**
	 * JpaRepository.findAll() を呼び出し
	 */
	public List<AddressDto> getAllAddresses() {
		List<Address> entities = addressRepository.findAll();
		return entities.stream()
					   .map(this::convertEntityToDto)
					   .collect(Collectors.toList());
	}
	
	
	private ToolDto convertEntityToDto(Tool tool) {
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

	private IndividualToolDto convertEntityToDto(UniqueTool uniqueTool) {
		long uniqueToolIdAsLong = (uniqueTool.getUniqueToolId() != null) 
									? uniqueTool.getUniqueToolId().longValue() 
									: 0L;

		return new IndividualToolDto(
			uniqueToolIdAsLong,
			uniqueTool.getCasePackNum(),
			uniqueTool.getRegrindCount()
		);
	}
	
	private AddressDto convertEntityToDto(Address address) {
		return new AddressDto(
			address.getDisplayAddress(),
			address.getControlAddress()
		);
	}

	/**
	 * JpaRepository.findById() を呼び出し
	 */
	public Map<String, String> getQrDataForTool(long uniqueToolId) {
		Optional<UniqueTool> toolOptional = uniqueToolRepository.findById(uniqueToolId);
		if (toolOptional.isEmpty()) {
			return null; // 存在しないID
		}
		
		UniqueTool uniqueTool = toolOptional.get();

		String timestampStr = "ERROR";
		if (uniqueTool.getToolPrintTime() != null) {
			timestampStr = uniqueTool.getToolPrintTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		}
		
		String basicToolIdPadded = String.format("%05d", uniqueTool.getBasicToolId());
		// [変更] % 10000 を削除
		String uniqueNumPadded = String.format("%05d", uniqueTool.getUniqueNum());
		String toolIdentifier = basicToolIdPadded + uniqueNumPadded;
		String displayText = toolIdentifier;
		String qrCodeData = String.format("T%s-%s",
			toolIdentifier,
			timestampStr);

		Map<String, String> qrDataMap = new HashMap<>();
		qrDataMap.put("displayText", displayText);
		qrDataMap.put("qrCodeData", qrCodeData);
		
		return qrDataMap;
	}
}