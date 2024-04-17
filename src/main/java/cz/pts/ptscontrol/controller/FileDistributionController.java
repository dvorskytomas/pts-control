package cz.pts.ptscontrol.controller;

import cz.pts.ptscontrol.service.FileDistributionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/distribute")
public class FileDistributionController {

    private final FileDistributionService fileDistributionService;

    public FileDistributionController(FileDistributionService fileDistributionService) {
        this.fileDistributionService = fileDistributionService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void distributeFile(@RequestPart(value = "file") MultipartFile file, @RequestPart(value = "destinationFolder") String destination) throws UnknownHostException {
        fileDistributionService.distributeFileToAllWorkers(file, destination);
    }

}
