package cz.pts.ptscontrol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class FileDistributionServiceImpl implements FileDistributionService {

    @Value("${worker.name}")
    private String workerName;
    @Value("${worker.port}")
    private String workerPort;
    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(FileDistributionServiceImpl.class);

    public FileDistributionServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void distributeFileToAllWorkers(MultipartFile file, String destinationPath) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(workerName);

        for(InetAddress address : addresses) {
            String url = "http://" + address.getHostAddress() + ":" + workerPort + "/api/upload";
            distributeFile(file, destinationPath, url);
        }
    }

    @Override
    public void distributeFile(MultipartFile file, String destinationFolder, String url) {
        logger.info("Trying to send test file to worker node.");
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("file", file.getResource());
        request.add("destinationFolder", destinationFolder);

        restTemplate.put(url, request);
    }

}
