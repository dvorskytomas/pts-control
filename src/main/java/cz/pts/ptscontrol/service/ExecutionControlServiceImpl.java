package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import cz.pts.ptscontrol.dto.TestStartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Service
public class ExecutionControlServiceImpl implements ExecutionControlService {

    private final RestTemplate restTemplate;
    private final Map<String, TestStartDto> testExecutionMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExecutionControlServiceImpl.class);

    public ExecutionControlServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public TestStartDto distributeAndStartTest(MultipartFile testFile, TestExecutionDto testExecutionDto) throws UnknownHostException {
        Assert.notNull(testExecutionDto, "Test execution definition cannot be null.");

        if(testExecutionDto.getTestExecutionId() == null) {
            testExecutionDto.setTestExecutionId(UUID.randomUUID().toString());
        }

        if(testExecutionMap.containsKey(testExecutionDto.getTestExecutionId())) {
            logger.error("Test run with id {} already exists.", testExecutionDto.getTestExecutionId());
            throw new IllegalArgumentException("Test run with id " + testExecutionDto.getTestExecutionId() + " already exists");
        }

        List<String> workerNodes = new ArrayList<>();

        InetAddress[] addresses = InetAddress.getAllByName("worker");

        // distribute test file to all nodes
        if(testFile != null) {
            for (InetAddress address : addresses) {
                logger.info("Found worker node address: {}", address.toString());
                String url = "http://" + address.getHostAddress() + ":8083/api/upload";
                distribute(testFile, testExecutionDto.getToolDirectoryPath(), url);
            }
        }

        // start test on all nodes
        for(InetAddress address : addresses) {
            String url = "http://" + address.getHostAddress() + ":8083/api/exec";
            start(url, testExecutionDto);
            workerNodes.add(address.toString());
        }

        TestStartDto response =  new TestStartDto();
        response.setTestExecutionId(testExecutionDto.getTestExecutionId());
        response.setWorkerNodesAddresses(workerNodes);

        testExecutionMap.put(testExecutionDto.getTestExecutionId(), response);

        // TODO create result folder...
        File resultsDir = new File("/results/" + testExecutionDto.getTestExecutionId());
        if(!resultsDir.exists()) {
            resultsDir.mkdirs();
        }

        return response;
    }

    @Override
    public List<String> stopTestExecution(String testExecutionId) throws UnknownHostException {
        logger.warn("Stopping test with id {} forcibly.", testExecutionId);
        InetAddress[] addresses = InetAddress.getAllByName("worker");

        List<String> workerNodes = new ArrayList<>();

        for(InetAddress address : addresses) {
            String url = "http://" + address.getHostAddress() + ":8083/api/exec/" + testExecutionId;
            restTemplate.delete(url);
            workerNodes.add(address.toString());
        }

        return workerNodes;
    }

    @Override
    public void processResultFile(String testExecutionId, MultipartFile file){
        logger.info("Received results file from worker node!");
        if(testExecutionMap.containsKey(testExecutionId)) {
            int fileNumber = 1;
            File testResultLog = getFinalLogFile("/results/" + testExecutionId + "/", file.getOriginalFilename(), fileNumber);

            try {
                file.transferTo(testResultLog);
            } catch (IOException e) {
                logger.error("Unable to save file to destination {}", testResultLog.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Invalid testExecutionId, control node received unknown results.");
        }
    }

    private File getFinalLogFile(String workDir, String originalFileName, int fileNumber) {
        File f = new File(workDir + fileNumber + "_" + originalFileName);
        if(f.exists()) {
            fileNumber += 1;
            return getFinalLogFile(workDir, originalFileName, fileNumber);
        }
        return f;
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
