package ch.so.agi.ili2c;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@RestController
@RequestMapping("/api/download")
public class DownloadController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @GetMapping({"/linux", "/macos"})
    public ResponseEntity<InputStreamResource> downloadShellScript() throws IOException {
        ClassPathResource file = new ClassPathResource("scripts/ili2c");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ili2c")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(file.getInputStream()));
    }

    @GetMapping("/windows")
    public ResponseEntity<InputStreamResource> downloadWindowsScript() throws IOException {
        ClassPathResource file = new ClassPathResource("scripts/ili2c.bat");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ili2c.bat")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(file.getInputStream()));
    }
}
