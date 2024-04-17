package cz.pts.ptscontrol.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.UnknownHostException;

public interface FileDistributionService {

    void distributeFile(MultipartFile file, String destinationFolder, String url);
    void distributeFileToAllWorkers(MultipartFile file, String destinationPath) throws UnknownHostException;
}
