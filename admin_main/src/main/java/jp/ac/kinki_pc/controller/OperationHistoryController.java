package jp.ac.kinki_pc.controller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.ac.kinki_pc.dto.HistoryData;
import jp.ac.kinki_pc.dto.HistoryFilterDisplayData;
import jp.ac.kinki_pc.dto.OperationHistoryFilterDialogData;
import jp.ac.kinki_pc.dto.YearMonthData;
import jp.ac.kinki_pc.service.OperationHistoryService;

/**
 * 操作履歴画面に関するリクエストを処理するコントローラ
 */
@Controller
@SessionAttributes({
	"yearMonthStructure"
})
public class OperationHistoryController {

	// 1. ロガーのインスタンスを生成
	private static final Logger logger = LoggerFactory.getLogger(OperationHistoryController.class);
	
	@Autowired
	private OperationHistoryService historyService;
	
	// application.properties から動画保存パスを取得 (デフォルトは Z:/recordings)
	@Value("${video.storage.path:Z:/recordings}")
	private String videoStoragePath;
		
	/**
	 * 操作履歴画面を表示します。
	 * @return 遷移先のHTMLファイル名
	 */
	@GetMapping("/history")
	public String showHistoryPage(Model model) {
		
		//動画が格納されているパス (設定ファイルから読み込むように変更)
		//Path sourceDir = Paths.get(videoStoragePath);
		
		model.addAttribute("historyFilterData", new OperationHistoryFilterDialogData());
   
		// DBから絞り込み候補（年度と月リスト）を最新の状態で取得
		
		// メソッド呼び出しのスペルミスを修正 (getHisotryCandidiate -> getHistoryCandidate)
		List<YearMonthData> yearMonthData = historyService.getHistoryCandidate();
		
		// 3. モデル（セッション）に追加
		model.addAttribute("yearMonthStructure", yearMonthData);

		return "OperationHistory";
	}
	
	/**
	 * 絞り込みフォームから送信されたデータを受け取り、表示画面にリダイレクトします。
	 * @param formData		   フォームから送信されたデータ
	 * @param redirectAttributes リダイレクト先にデータを渡すためのオブジェクト
	 * @return リダイレクト先のURL
	 */
	@PostMapping("/historyfiltering")
	public String filterHistory(@ModelAttribute OperationHistoryFilterDialogData formData, RedirectAttributes redirectAttributes) {
		
		//絞り込みフォームから取得した氏名を格納する。
		String username=formData.getUsername();
		
		//絞り込みフォームから取得した作業内容を格納する。
		String work_details=formData.getWork_details();
		
		//絞り込みフォームから取得した期間を格納する。
		String period=historyService.getMonthDateRange(formData.getYear(), formData.getMonth());
		
		// 送信されたフォームデータを、リダイレクト後のGETリクエストで使えるようにする
		HistoryFilterDisplayData displaydata = new HistoryFilterDisplayData();
		
		//絞り込みフォームで取得した氏名欄の値がないときは全てとして扱う。
		if(username.isEmpty()) {
			username="全て";
		}  
		
		//ダイアログから取得したデータを表示用オブジェクトに格納する。
		displaydata.setUsername(username);//氏名の格納
		displaydata.setWork_details(work_details);//作業内容の格納
		displaydata.setPeriod(period);
		
		//入力された絞り込み条件を画面にデータをHTML側に渡す。
		redirectAttributes.addFlashAttribute("絞り込み条件表示", displaydata);
		
		// --- 1. サービス呼び出しのための準備 ---
		// formDataから年と月を取得し、YearMonthオブジェクトを作成
		YearMonth yearMonth = YearMonth.of(
			Integer.parseInt(formData.getYear()), 
			Integer.parseInt(formData.getMonth())
		);
		
		//氏名が全てであるとき、クエリ文で条件を無視するための処理
		if(username=="全て") {
			username=convertEmptyToNull(username);
		}
		
	   
		//絞り込み条件でDBで検索する。
		List<HistoryData> results = historyService.getHistoryForView(
			username,	   // 氏名
			yearMonth,					// 年月
			convertEmptyToNull(work_details)   // 作業内容
		);
		
		//絞り込み結果をターミナルに表示する。
		logger.info("---------- 検索結果 START ----------");
		logger.info("検索件数: {}件", results.size());
		results.forEach(item -> logger.info(item.toString())); // 各結果を1行ずつ表示
		logger.info("----------- 検索結果 END -----------");
		
		//DBからの検索結果をHTML側に渡す。
		redirectAttributes.addFlashAttribute("絞り込みボタン結果表示",results);
		//redirectAttributes.addFlashAttribute("絞り込み結果",results);
	   
		return "redirect:/history";
	}
	
	private String convertEmptyToNull(String s) {
		if ("全て".equals(s) || s == null || s.isEmpty() ) {
			return null;
		}
		return s;
	}

	/**
	 * 外部ディレクトリから動画ファイルをストリーミング配信します。
	 * (OperationHistory.js の player.src = `/video/${videoPath}` から呼び出されます)
	 *
	 * @param filename 配信する動画のファイル名 (例: recording_20251010_134116.mp4)
	 * @return 動画データを含むResponseEntity
	 */
	@GetMapping("/video/{filename:.+}") // :.+ を追加し、ファイル名中のドット(.)が切り捨てられないようにする
	public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
		
		try {
			// もしリクエストされたファイル名に .mp4 が含まれていなければ、自動的に付与します。
			String fullFilename = filename;
			if (filename != null && !filename.toLowerCase().endsWith(".mp4")) {
				fullFilename = filename + ".mp4";
			}

			// 1. 設定ファイルで指定された動画保存パスとファイル名を結合
			Path videoFile = Paths.get(videoStoragePath).resolve(fullFilename);
			Resource resource = new UrlResource(videoFile.toUri());

			// 2. ファイルが存在し、読み取り可能かチェック
			if (resource.exists() && resource.isReadable()) {
				
				// 3. レスポンスを構築して動画データを返す
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_TYPE, "video/mp4")
						// .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength())) // 必要に応じて
						.body(resource);
			} else {
				// ファイルが見つからない場合 (パスが間違っているか、ファイル名がDBと一致しない)
				logger.warn("要求された動画ファイルが見つかりません: {}", videoFile);
				return ResponseEntity.notFound().build();
			}
		} catch (MalformedURLException e) {
			// パスの形式が不正な場合
			logger.error("動画パスの形式が不正です (MalformedURLException): {}", filename, e);
			return ResponseEntity.badRequest().build();
		} catch (Exception e) {
			// その他のエラー (例: 権限不足)
			logger.error("動画ストリーミング中に予期せぬエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}
 
}