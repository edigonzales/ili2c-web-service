package ch.so.agi.ili2c;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iox_j.logging.FileLogger;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class Ili2Controller {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Value("${app.workDirectory}")
    private String workDirectory;

    private ObjectMapper objectMapper;

    public Ili2Controller(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestHeader Map<String, String> headers, HttpServletRequest request) {
        headers.forEach((key, value) -> {
            log.info(String.format("Header '%s' = %s", key, value));
        });
        
        log.info("server name: " + request.getServerName());
        log.info("context path: " + request.getContextPath());
        
        log.info("ping"); 
        
        return new ResponseEntity<String>("ili2c-web-service", HttpStatus.OK);
    }

    @PostMapping(value = "/api/compile", consumes = {"multipart/form-data"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> compile(@RequestPart(name = "file", required = true) MultipartFile file) {
    
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }
        
        try {
            Path tempDir = Files.createTempDirectory("ili2c_");
            Path iliFile = tempDir.resolve(file.getOriginalFilename());
            Path logFile = tempDir.resolve("ili2c.log");
            
            file.transferTo(iliFile);
            boolean valid = runCompiler(iliFile, logFile);
            
            if (valid) {
                return ResponseEntity.ok(Map.of("success", Files.readString(logFile)));
            } else {
                return ResponseEntity.ok(Map.of("failure", Files.readString(logFile)));
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }        
    }
    
    private synchronized boolean runCompiler(Path iliFile, Path logFile) throws IOException {
        EhiLogger.getInstance().removeListener(StdListener.getInstance());

        FileLogger fileLogger = new FileLogger(logFile.toFile(), true);
        
        EhiLogger.getInstance().addListener(fileLogger);

        IliManager manager = new IliManager();        
        manager.setRepositories(Ili2cSettings.DEFAULT_ILIDIRS.split(";"));
        ArrayList<String> ilifiles = new ArrayList<String>();
        ilifiles.add(iliFile.toAbsolutePath().toString());
        Configuration config;
        try {
            EhiLogger.logState("ili2c"+"-"+TransferDescription.getVersion());

            
            config = manager.getConfigWithFiles(ilifiles);
            TransferDescription td = Ili2c.runCompiler(config);
            
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date today = new Date();
            String dateOut = dateFormatter.format(today);

            if (td == null) {
                EhiLogger.logError("...compiler run failed "+dateOut);
                return false;
            } else {
                EhiLogger.logState("...compiler run done "+dateOut);
                return true;
            }
        } catch (Ili2cException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            EhiLogger.getInstance().removeListener(fileLogger);
        }
    }
}
