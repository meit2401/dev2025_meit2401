// jp/ac/kinki_pc/controller/QrCodePrinterController.java

package jp.ac.kinki_pc.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.ac.kinki_pc.service.LineManagementService;
import jp.ac.kinki_pc.service.ToolManagementService;
import jp.ac.kinki_pc.service.UserManagementService;

@RestController
@RequestMapping("/qrcodeprinter")
public class QrCodePrinterController {

	@Autowired
	private UserManagementService userManagementService;
	
	@Autowired
	private LineManagementService lineManagementService;
	
	@Autowired
	private ToolManagementService toolManagementService;

	@GetMapping(value = "/template.lw1", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> getTemplate() throws IOException {
		Path path = Paths.get("src/main/resources/static/qr_template.lw1");
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr_template.lw1")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	@GetMapping(value = "/data.csv", produces = "text/csv; charset=UTF-8")
	public ResponseEntity<String> getQrCsv(@RequestParam("type") String type, @RequestParam("id") String id) {
		
		String displayText;
		String qrCodeData;
		Map<String, String> qrDataMap; 

		switch (type.toLowerCase()) {
			case "user":
				try {
					Integer userId = Integer.parseInt(id);
					qrDataMap = userManagementService.getQrDataForUser(userId);
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body("Invalid user ID format.");
				}
				
				if (qrDataMap == null) {
					return ResponseEntity.notFound().build();
				}
				displayText = qrDataMap.get("displayText");
				qrCodeData = qrDataMap.get("qrCodeData");
				break;
			
			case "program":
				try {
					Integer programId = Integer.parseInt(id);
					qrDataMap = lineManagementService.getQrDataForProgram(programId);
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body("Invalid program ID format.");
				}

				if (qrDataMap == null) {
					return ResponseEntity.notFound().build();
				}
				displayText = qrDataMap.get("displayText");
				qrCodeData = qrDataMap.get("qrCodeData");
				break;

			case "tool":
				try {
					long uniqueToolId = Long.parseLong(id);
					qrDataMap = toolManagementService.getQrDataForTool(uniqueToolId);
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body("Invalid tool ID format.");
				}

				if (qrDataMap == null) {
					return ResponseEntity.notFound().build();
				}
				displayText = qrDataMap.get("displayText");
				qrCodeData = qrDataMap.get("qrCodeData");
				break;

			default: return ResponseEntity.badRequest().body("Invalid QR data type specified: " + type);
		}

		final String bom = "\uFEFF";
		String header = "\"customText\",\"customCode\"\n";
		String data = "\"" + displayText + "\",\"" + qrCodeData + "\"\n";
		String csvContent = bom + header + data;
		
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr_data.csv")
				.body(csvContent);
	}
}