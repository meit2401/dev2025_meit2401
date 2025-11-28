package jp.ac.kinki_pc.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.ac.kinki_pc.dto.InsufficientToolNotificationData;
import jp.ac.kinki_pc.service.ToolShortageAlertService;

@Controller
public class ToolShortageAlertController {

	@Autowired
	private ToolShortageAlertService toolShortageAlertService;
	
	//@Autowired
	//private  TimeSettingService timeService;
	
	@GetMapping("/shortageTools")
	 public String showShortageToolList(Model model) {
		
		List<InsufficientToolNotificationData> 不足工具リスト = toolShortageAlertService.getCachedToolList(); // スペース追加
		model.addAttribute("不足工具リスト", 不足工具リスト);
		
		int toolCount = toolShortageAlertService.getToolShortageCount();
		
		model.addAttribute("件数", toolCount);
		
		//model.addAttribute("currentTime", timeService.getApplicationTime());
		
		return "tool-shortage-alert"; // lack.htmlを表示
	}
	
	@GetMapping("/alert")
	public String showShortageToolList1(Model model) {
		
		List<InsufficientToolNotificationData> 不足工具リスト = toolShortageAlertService.getCachedToolList(); // スペース追加
		model.addAttribute("不足工具リスト", 不足工具リスト);
		
		int toolCount = toolShortageAlertService.getToolShortageCount();
		
		model.addAttribute("件数", toolCount);
		
		
		return "ToolShortageAlert";
	}
	
	
	//AJAX通信用に、不足工具リストのデータだけをJSON形式で返すAPI
	@GetMapping("/api/tool-shortages") // API用のURLパス
	@ResponseBody // このアノテーションが、戻り値をJSONに変換するよう指示します
	public List<InsufficientToolNotificationData> getToolShortagesApi() {
		return toolShortageAlertService.getCachedToolList();
	}
	
	//AJAX通信用に、不足工具の件数だけを返すAPI
	@GetMapping("/api/tool-shortages/count")
	@ResponseBody
	public int getToolShortageCountApi() {
		return toolShortageAlertService.getToolShortageCount();
	}
}