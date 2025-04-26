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
import org.springframework.util.FileSystemUtils;
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
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iox_j.logging.FileLogger;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class Ili2Controller {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Value("${app.ilidirs}")
    private String ilidirs;
    
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
            String logContent = Files.readString(logFile);
            
            FileSystemUtils.deleteRecursively(iliFile.getParent());
            
            if (valid) {
                return ResponseEntity.ok(logContent);
            } else {
                return ResponseEntity.internalServerError().body(logContent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }        
    }
    
    private synchronized boolean runCompiler(Path iliFile, Path logFile) throws IOException {
        FileLogger fileLogger = new FileLogger(logFile.toFile(), false);
        EhiLogger.getInstance().addListener(fileLogger);

        IliManager manager = new IliManager();        
        //manager.setRepositories(Ili2cSettings.DEFAULT_ILIDIRS.split(";"));
        manager.setRepositories(ilidirs.split(";"));
        ArrayList<String> ilifiles = new ArrayList<String>();
        ilifiles.add(iliFile.toAbsolutePath().toString());
        Configuration config;
        try {
            EhiLogger.logState("ili2c"+"-"+TransferDescription.getVersion());
            config = manager.getConfigWithFiles(ilifiles);
        } catch (Ili2cException e) {
            throw new IOException(e.getMessage());
        } 
        
        TransferDescription td = null;
        try {
            td = Ili2c.runCompiler(config);
        } catch (Ili2cFailure e) {}
        
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date today = new Date();
        String dateOut = dateFormatter.format(today);

        if (td == null) {
            EhiLogger.logError("...compiler run failed "+dateOut);
            EhiLogger.getInstance().removeListener(fileLogger);
            return false;
        } else {
            EhiLogger.logState("...compiler run done "+dateOut);
            EhiLogger.getInstance().removeListener(fileLogger);
            return true;
        }
    }
}
