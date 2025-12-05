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
import jp.ac.kinki_pc.repository.ToolAssignmentRepository;
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

    @Autowired
    private ToolAssignmentRepository toolAssignmentRepository;

    /**
     * 検索条件に基づいて工具を検索する。
     * @param maker メーカー名(部分一致)
     * @param category 工具カテゴリ(完全一致)
     * @param material 工具材質(部分一致)
     * @param trader 取引先(部分一致)
     * @param toolNameFilter 工具名(部分一致)
     * @return 検索条件に一致する工具のリスト
     */
    public List<ToolDto> searchTools(String maker, String category, String material, String trader, String toolNameFilter) {
        
        Specification<Tool> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("isFrozen"), false));
            
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

    /**
     * 指定されたIDに対応する工具をリストから検索して取得する。
     * @param toolList 工具リスト
     * @param selectedId 検索する工具のID
     * @return 指定されたIDの工具情報。見つからない場合はnull
     */
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
     * 新しい工具を追加し、保管場所を登録する。
     * @param toolName 工具名
     * @param maker メーカー
     * @param toolCategory 工具カテゴリ
     * @param toolMaterial 工具材質
     * @param stc 収納数
     * @param rop 発注点
     * @param buyer 取引先
     * @param storageLocation 保管場所(カンマ区切りで複数指定可)
     */
    @Transactional
    public void addTool(String toolName, String maker, String toolCategory,
						String toolMaterial, Integer stc, Integer rop, String buyer, String storageLocation) {
        
        Tool newTool = new Tool(null, toolName, maker, toolCategory, toolMaterial, stc, rop, buyer, false);
        
        Tool savedTool = toolRepository.save(newTool);
        int newBasicToolId = savedTool.getBasicToolId();

        if (StringUtils.hasText(storageLocation)) {
            String[] addresses = storageLocation.split(",");
            for (String address : addresses) {
                String trimmedAddress = address.trim();
                if (StringUtils.hasText(trimmedAddress)) {
                    
                    if (!addressRepository.existsById(trimmedAddress)) {
                        Address newAddressEntity = new Address();
                        newAddressEntity.setDisplayAddress(trimmedAddress);
                        newAddressEntity.setControlAddress("0"); 
                        addressRepository.save(newAddressEntity);
                    }
                    
                    StorageArea newArea = new StorageArea();
                    newArea.setBasicToolId(newBasicToolId);
                    newArea.setDisplayAddress(trimmedAddress);
                    newArea.setToolcaseStcNum(0);
                    
                    storageAreaRepository.save(newArea);
                }
            }
        }
    }

    /**
     * 指定された基本工具IDを無効化する。
     * 工具割当、保管中工具の有無、在庫数を確認してから無効化を実行する。
     * @param basicToolId 無効化対象の基本工具ID
     */
    @Transactional
	public void disableTool(int basicToolId) {
		// 1. ToolAssignment check
		if (toolAssignmentRepository.existsByBasicToolId(basicToolId)) {
			throw new RuntimeException("プログラムに割り当てられているため、凍結できません。");
		}
		
		// 2. UniqueTool check (保管中 check)
		// ここは UniqueToolRepository を使います
		if (uniqueToolRepository.existsByBasicToolIdAndStorageCondition(basicToolId, "保管中")) {
			throw new RuntimeException("保管中の個体が存在するため、凍結できません。");
		}
		
		// 3. StorageArea check (toolcase_stc_num > 0)
		// ここは StorageAreaRepository を使います
		if (storageAreaRepository.existsByBasicToolIdAndToolcaseStcNumGreaterThan(basicToolId, 0)) {
			throw new RuntimeException("保管場所の在庫数が0ではないため、凍結できません。");
		}
		
		Tool tool = toolRepository.findById(basicToolId)
			.orElseThrow(() -> new RuntimeException("対象の工具が見つかりません。basicToolId=" + basicToolId));
		
		tool.setIsFrozen(true);
		toolRepository.save(tool);
	}
    
    /**
     * 個別工具を作成し、登録する。
     * 基本工具IDと連番を組み合わせて10桁の個別工具IDを生成する。
     * @param basicToolId 基本工具ID
     * @param casePackNumFromForm ケースあたりの入数(フォーム入力値)
     * @return 生成された個別工具ID
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
        
        if (basicToolId > 99999) {
            throw new RuntimeException("基本工具IDが5桁の上限を超えています。 ID: " + basicToolId);
        }
        if (nextUniqueNum > 99999) {
            throw new RuntimeException("個別番号が5桁の上限を超えています。 ID: " + nextUniqueNum);
        }
        
        String basicIdPadded = String.format("%05d", basicToolId);
        String uniqueNumPadded = String.format("%05d", nextUniqueNum);
        
        long newUniqueToolId = Long.parseLong(basicIdPadded + uniqueNumPadded);
        
        UniqueTool newUniqueTool = new UniqueTool();
        newUniqueTool.setUniqueToolId(newUniqueToolId);
        newUniqueTool.setToolPrintTime(LocalDateTime.now());
        newUniqueTool.setBasicToolId(basicToolId);
        newUniqueTool.setUniqueNum(nextUniqueNum);
        newUniqueTool.setCasePackNum(finalCasePackNum);
        newUniqueTool.setStorageAreaId(null);
        newUniqueTool.setStorageCondition("補充前");
        newUniqueTool.setRegrindCount(0);
        
        uniqueToolRepository.save(newUniqueTool);
        
        return newUniqueToolId;
    }

    /**
     * 指定されたQRコード番号(10桁)に基づいて個別工具情報を検索し、印刷日時を更新する。
     * @param qrNumber QRコード番号(基本工具ID 5桁 + 個別番号 5桁)
     * @return 更新された個別工具ID。該当する工具が存在しない場合はnull
     */
    @Transactional
    public Long reprintQrCode(String qrNumber) {
        if (qrNumber == null || qrNumber.length() != 10) {
            throw new IllegalArgumentException("IDは10桁である必要があります。");
        }
        try {
            int basicToolId = Integer.parseInt(qrNumber.substring(0, 5));
            int uniqueNum = Integer.parseInt(qrNumber.substring(5));
            
            Optional<UniqueTool> toolOptional = uniqueToolRepository.findByBasicToolIdAndUniqueNum(basicToolId, uniqueNum);
            
            if (toolOptional.isPresent()) {
                UniqueTool toolToUpdate = toolOptional.get();
                toolToUpdate.setToolPrintTime(LocalDateTime.now());
                uniqueToolRepository.save(toolToUpdate);
                return toolToUpdate.getUniqueToolId();
            } else {
                return null;
            }
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("IDは数字である必要があります。");
        }
    }

    /**
     * 指定された基本工具の発注点(ROP)を更新する。
     * @param basicToolId 更新対象の基本工具ID
     * @param rop 新しい発注点
     */
    @Transactional
    public void updateReorderPoint(int basicToolId, int rop) {
        Optional<Tool> toolOptional = toolRepository.findById(basicToolId);
        if (toolOptional.isPresent()) {
            Tool toolToUpdate = toolOptional.get();
            toolToUpdate.setRop(rop);
            toolRepository.save(toolToUpdate);
        }
    }
    
    /**
     * 各保管場所(住所)ごとの保管数を取得する。
     * @return 保管場所(住所)と保管数のマップ
     */
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
     * 指定された基本工具IDに紐づく個別工具の総数を取得する。
     * @param basicToolId 基本工具ID
     * @return 個別工具の数
     */
    public int getIndividualToolCount(int basicToolId) {
        return (int) uniqueToolRepository.countByBasicToolId(basicToolId);
    }
    
    /**
     * 指定された基本工具IDに紐づく個別工具のリストを取得する。
     * @param basicToolId 基本工具ID
     * @return 個別工具情報のリスト
     */
    public List<IndividualToolDto> getIndividualTools(int basicToolId) {
        List<UniqueTool> entities = uniqueToolRepository.findByBasicToolId(basicToolId);
        return entities.stream()
                        .map(this::convertEntityToDto)
                        .collect(Collectors.toList());
    }
    
    /**
     * 登録されているすべての住所情報を取得する。
     * @return 住所情報のリスト
     */
    public List<AddressDto> getAllAddresses() {
        List<Address> entities = addressRepository.findAll();
        return entities.stream()
                        .map(this::convertEntityToDto)
                        .collect(Collectors.toList());
    }
    
    /**
     * ToolエンティティをToolDtoに変換する。
     * @param tool Toolエンティティ
     * @return 変換されたToolDto
     */
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

    /**
     * UniqueToolエンティティをIndividualToolDtoに変換する。
     * @param uniqueTool UniqueToolエンティティ
     * @return 変換されたIndividualToolDto
     */
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
    
    /**
     * AddressエンティティをAddressDtoに変換する。
     * @param address Addressエンティティ
     * @return 変換されたAddressDto
     */
    private AddressDto convertEntityToDto(Address address) {
        return new AddressDto(
            address.getDisplayAddress(),
            address.getControlAddress()
        );
    }

    /**
     * 指定された個別工具IDに対応するQRコード生成用データを取得する。
     * @param uniqueToolId 個別工具ID
     * @return QRコード表示テキストとデータを含むマップ。該当なしの場合はnull
     */
    public Map<String, String> getQrDataForTool(long uniqueToolId) {
        Optional<UniqueTool> toolOptional = uniqueToolRepository.findById(uniqueToolId);
        if (toolOptional.isEmpty()) {
            return null;
        }
        
        UniqueTool uniqueTool = toolOptional.get();

        String timestampStr = "ERROR";
        if (uniqueTool.getToolPrintTime() != null) {
            timestampStr = uniqueTool.getToolPrintTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        
        String basicToolIdPadded = String.format("%05d", uniqueTool.getBasicToolId());
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