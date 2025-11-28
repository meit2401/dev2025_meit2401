package jp.ac.kinki_pc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/stock") 
public class StockAnalysisController {

	@GetMapping
	public String showStockPage() {
		return "StockAnalysis";
	}
}