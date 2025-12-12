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
			throw new RuntimeException("対象の工具は割り当てられているため、凍結できません。");
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
     * 指定された個別工具を削除する。
     * QRコード印刷等の処理でエラーが発生した場合のロールバック（削除）処理として使用する。
     * @param uniqueToolId 削除対象の個別工具ID
     */
    @Transactional
    public void deleteIndividualTool(long uniqueToolId) {
        if (!uniqueToolRepository.existsById(uniqueToolId)) {
            throw new RuntimeException("削除対象の個別工具が見つかりません。ID=" + uniqueToolId);
        }

        uniqueToolRepository.deleteById(uniqueToolId);
    }

    /**
     * 指定されたQRコード番号(9桁)に基づいて個別工具情報を検索し、印刷日時を更新する。
     * @param qrNumber QRコード番号(基本工具ID)
     * @return 更新された個別工具ID。該当する工具が存在しない場合はnull
     */
    @Transactional
	public Long reprintQrCode(String qrNumber, boolean updateTimestamp) {
		if (qrNumber == null) {
			throw new IllegalArgumentException("IDが入力されていません。");
		}
		
		// 9桁(Hex)のみ対応
		if (qrNumber.length() != 9) {
			throw new IllegalArgumentException("IDは9桁(Hex)である必要があります。");
		}

		// コンソール出力は findUniqueToolByLabelCode 内で行われます
		UniqueTool tool = findUniqueToolByLabelCode(qrNumber);
		if (tool != null) {
			if (updateTimestamp) {
				// 再印刷なのでタイムスタンプを更新
				tool.setToolPrintTime(LocalDateTime.now());
				uniqueToolRepository.save(tool);
			}
			return tool.getUniqueToolId();
		} else {
			// 該当なし
			return null;
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
	@Transactional
	public Map<String, String> getQrDataForTool(long uniqueToolId) {
		Optional<UniqueTool> toolOptional = uniqueToolRepository.findById(uniqueToolId);
		if (toolOptional.isEmpty()) {
			return null; // 存在しないID
		}
		
		UniqueTool uniqueTool = toolOptional.get();

		// 1. 印刷日時を取得
		// 救済措置用コード生成のため、印刷日時が必須となります。
		LocalDateTime printTime = uniqueTool.getToolPrintTime();
		if (printTime == null) {
			printTime = LocalDateTime.now(); // フォールバック: 未設定なら現在日時
			
			// DBの値を更新しないと、生成したコードで検索してもヒットしない
			uniqueTool.setToolPrintTime(printTime);
			uniqueToolRepository.save(uniqueTool);
		}
		
		// [ログ追加] 暗号化入力確認
		System.out.println("---------- 暗号化処理 (Encode) ----------");
		System.out.println("入力 (印刷日時): " + printTime);

		// 2. 36ビットデータの生成
		// 構成: Year(10) | Month(4) | Day(5) | Hour(5) | Minute(6) | Second(6)
		// Yearは下3桁(0-999)を使用
		long year   = printTime.getYear() % 1000; // 10 bit
		long month  = printTime.getMonthValue();  // 4 bit
		long day    = printTime.getDayOfMonth();  // 5 bit
		long hour   = printTime.getHour();        // 5 bit
		long minute = printTime.getMinute();      // 6 bit
		long second = printTime.getSecond();      // 6 bit

		// ビットシフトで結合 (MSB -> LSB の順で Year -> Second と配置)
		long timeBits = (year << 26) 
					  | (month << 22) 
					  | (day << 17) 
					  | (hour << 12) 
					  | (minute << 6) 
					  | second;

		// 3. 16進数9桁に変換 (displayText用)
		String timeHexCode = String.format("%09X", timeBits);

		// [ログ追加] 暗号化結果確認
		System.out.println("結果 (Hexコード): " + timeHexCode);
		System.out.println("----------------------------------------");

		// 4. QRコードデータ用の識別子
		String basicToolIdPadded = String.format("%05d", uniqueTool.getBasicToolId());
		String uniqueNumPadded = String.format("%05d", uniqueTool.getUniqueNum());
		String toolIdentifier = basicToolIdPadded + uniqueNumPadded;
		
		String timestampStr = printTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		
		String qrCodeData = String.format("T%s-%s",
			toolIdentifier,
			timestampStr);

		Map<String, String> qrDataMap = new HashMap<>();
		qrDataMap.put("displayText", timeHexCode); // ラベル印字用テキストを9桁Hexに変更
		qrDataMap.put("qrCodeData", qrCodeData);
		
		return qrDataMap;
	}

	/**
	 * 工具ラベルコード(9桁Hex)から工具を特定する
	 * ラベルコードは秒単位の精度のため、同一秒に印刷された工具が複数ある場合はリストの先頭を返す、
	 * または運用として同一秒印刷はないものとする前提で実装しています。
	 */
	public UniqueTool findUniqueToolByLabelCode(String labelCode) {
		// [ログ追加] 復号化入力確認
		System.out.println("---------- 復号化処理 (Decode) ----------");
		System.out.println("入力 (Hexコード): " + labelCode);

		if (labelCode == null || labelCode.length() != 9) {
			throw new IllegalArgumentException("ラベルコードは9桁の16進数である必要があります。");
		}

		// 1. 16進数から数値(36bit)へ変換
		long timeBits;
		try {
			timeBits = Long.parseLong(labelCode, 16);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ラベルコードの形式が不正です。");
		}

		// 2. ビット列から日時情報を抽出
		long second = timeBits & 0x3F;
		long minute = (timeBits >> 6) & 0x3F;
		long hour   = (timeBits >> 12) & 0x1F;
		long day    = (timeBits >> 17) & 0x1F;
		long month  = (timeBits >> 22) & 0x0F;
		long year   = (timeBits >> 26) & 0x3FF;

		// 3. LocalDateTimeを復元
		// Yearは下3桁(0-999)なので、2000年代を前提として西暦に変換 (2000 + year)
		int fullYear = 2000 + (int) year;

		LocalDateTime targetTimeStart;
		try {
			targetTimeStart = LocalDateTime.of(fullYear, (int) month, (int) day, (int) hour, (int) minute, (int) second);
		} catch (Exception e) {
			// 日付として不正な場合(例: 2月30日など)
			throw new IllegalArgumentException("ラベルコードから有効な日時を復元できませんでした。");
		}
		
		// [ログ追加] 復号化結果確認
		System.out.println("結果 (復元日時): " + targetTimeStart);
		System.out.println("----------------------------------------");
		
		// DBにはミリ秒が含まれている可能性があるため、[targetTime, targetTime + 1秒) の範囲で検索
		LocalDateTime targetTimeEnd = targetTimeStart.plusSeconds(1);

		List<UniqueTool> foundTools = uniqueToolRepository.findByToolPrintTimeBetween(targetTimeStart, targetTimeEnd);

		if (foundTools.isEmpty()) {
			return null;
		}

		// 複数ヒットした場合は、運用ルールに従い先頭を返す(ここではリストの最初の要素)
		return foundTools.get(0);
	}
}