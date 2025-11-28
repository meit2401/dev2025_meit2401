// jp/ac/kinki_pc/service/ToolShortageAlertService.java
package jp.ac.kinki_pc.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jp.ac.kinki_pc.dto.InsufficientToolNotificationData;
import jp.ac.kinki_pc.entity.Tool;
import jp.ac.kinki_pc.repository.ToolRepository; // [変更なし] 呼び出し先は JPA インターフェース


@Service
public class ToolShortageAlertService {
	@Autowired
	private ToolRepository toolRepository; // [変更なし] JpaRepository をインジェクション
	
	private volatile List<InsufficientToolNotificationData> cachedToolList = Collections.emptyList();
	
	@Scheduled(fixedRate = 1000)
	public void refreshToolList() {
		// 1. RepositoryからEntityのリストを取得
		// [変更なし] JpaRepository の @Query メソッドを呼び出す
		List<Tool> toolEntities = toolRepository.findShortageAlerts();
		
		// 2. EntityリストをDTOリストに変換
		List<InsufficientToolNotificationData> dtoList = toolEntities.stream()
			.map(this::convertEntityToDto)
			.collect(Collectors.toList());

		// 3. DTOリストをキャッシュ
		cachedToolList = dtoList;
	}
	
	/**
	 * Tool (Entity) を InsufficientToolNotificationData (DTO) に変換します。
	 * (Entityのstcフィールドには、Repositoryで計算されたcurrent_stockが格納されている前提)
	 * [変更なし] ロジックはそのまま
	 */
	private InsufficientToolNotificationData convertEntityToDto(Tool tool) {
		InsufficientToolNotificationData dto = new InsufficientToolNotificationData();
		dto.setBasicToolId(tool.getBasicToolId());
		dto.setToolName(tool.getToolName());
		dto.setMaker(tool.getMaker());
		dto.setToolCategory(tool.getToolCategory());
		dto.setToolMaterial(tool.getToolMaterial());
		dto.setRop(tool.getRop());
		dto.setBuyer(tool.getBuyer());
		
		// Repositoryで stc フィールドに格納した current_stock を DTO の currentStock にセットする
		dto.setCurrentStock(tool.getStc()); 
		
		return dto;
	}
	
	// コントローラから参照するためのgetterを用意
	public List<InsufficientToolNotificationData> getCachedToolList() {
		return cachedToolList;
	}
	
	public int getToolShortageCount() {
		return cachedToolList.size();
	}
	
}