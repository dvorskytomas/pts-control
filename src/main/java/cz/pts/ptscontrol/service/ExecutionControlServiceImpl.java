package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutionControlServiceImpl implements ExecutionControlService {

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ExecutionControlServiceImpl.class);

    public ExecutionControlServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> distributeAndStartTest(MultipartFile testFile, TestExecutionDto testExecutionDto) throws UnknownHostException {
        List<String> workerNodes = new ArrayList<>();

        InetAddress[] addresses = InetAddress.getAllByName("worker");

        // distribute test file to all nodes
        for (InetAddress address : addresses) {
            logger.info("Found worker node address: {}", address.toString());
            workerNodes.add(address.toString());
            String url = "http://" + address.getHostAddress() + ":8083/api/upload";
            distribute(testFile, testExecutionDto.getToolDirectoryPath(), url);
        }

        // start test on all nodes
        for(InetAddress address : addresses) {
            String url = "http://" + address.getHostAddress() + ":8083/api/exec";
            start(url, testExecutionDto);
        }

        return workerNodes;
    }

    private void distribute(MultipartFile testFile, String destinationFolder, String url) {
        logger.info("Trying to send test file to worker node.");
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("file", testFile.getResource());
        request.add("destinationFolder", destinationFolder);

        restTemplate.put(url, request);
    }

    private void start(String url, TestExecutionDto dto) {
        logger.info("Trying to start test on worker node.");
        restTemplate.postForObject(url, dto, Void.class);
    }

}
