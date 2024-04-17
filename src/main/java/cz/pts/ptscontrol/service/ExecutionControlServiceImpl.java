package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import cz.pts.ptscontrol.dto.TestStartDto;
import cz.pts.ptscontrol.dto.WorkerNodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ExecutionControlServiceImpl implements ExecutionControlService {

    @Value("${worker.name}")
    private String workerName;
    @Value("${worker.port}")
    private String workerPort;

    private final RestTemplate restTemplate;
    private final FileDistributionService fileDistributionService;

    private final Map<String, TestStartDto> testExecutionMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExecutionControlServiceImpl.class);

    public ExecutionControlServiceImpl(RestTemplate restTemplate,
                                       FileDistributionService fileDistributionService) {
        this.restTemplate = restTemplate;
        this.fileDistributionService = fileDistributionService;
    }

    @Override
    public TestStartDto distributeAndStartTest(MultipartFile testFile, TestExecutionDto testExecutionDto) throws UnknownHostException {
        Assert.notNull(testExecutionDto, "Test execution definition cannot be null.");

        if (testExecutionDto.getTestExecutionId() == null) {
            testExecutionDto.setTestExecutionId(UUID.randomUUID().toString());
        }

        if (testExecutionMap.containsKey(testExecutionDto.getTestExecutionId())) {
            logger.error("Test run with id {} already exists.", testExecutionDto.getTestExecutionId());
            throw new IllegalArgumentException("Test run with id " + testExecutionDto.getTestExecutionId() + " already exists");
        }

        List<String> workerNodes = new ArrayList<>();

        InetAddress[] addresses = InetAddress.getAllByName(workerName);

        // distribute test file to all nodes
        if (testFile != null) {
            for (InetAddress address : addresses) {
                logger.info("Found worker node address: {}", address.toString());
                String url = "http://" + address.getHostAddress() + ":" + workerPort + "/api/upload";
                fileDistributionService.distributeFile(testFile, testExecutionDto.getToolDirectoryPath(), url);
            }
        }

        TestStartDto response = new TestStartDto();
        // start test on all nodes
        for (int i = 0; i < addresses.length; i++) {
            String url = "http://" + addresses[i].getHostAddress() + ":" + workerPort + "/api/exec";
            // we only have one request object - reset it afterward?
            testExecutionDto.setWorkerNumber(i + 1);
            start(url, testExecutionDto);
            response.getWorkerNodeResults().put(i + 1, new WorkerNodeResult());
            workerNodes.add(addresses[i].toString());
        }


        response.setTestExecutionId(testExecutionDto.getTestExecutionId());
        response.setWorkerNodesAddresses(workerNodes);
        response.setTestExecutionDto(testExecutionDto);

        testExecutionMap.put(testExecutionDto.getTestExecutionId(), response);

        if (StringUtils.hasText(testExecutionDto.getLogFileName())) {
            logger.info("Creating new folder for results.");
            File resultsDir = new File("/results/" + testExecutionDto.getTestExecutionId());
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }
            File resultLog = new File("/results/" + testExecutionDto.getTestExecutionId() + "/" + testExecutionDto.getLogFileName());
            try {
                resultLog.createNewFile();
            } catch (IOException e) {
                logger.error("Unable to create result file with name {}", resultLog.getName());
                throw new RuntimeException(e);
            }
        }

        return response;
    }

    @Override
    public List<String> stopTestExecution(String testExecutionId) throws UnknownHostException {
        logger.warn("Stopping test with id {} forcibly.", testExecutionId);
        if (!testExecutionMap.containsKey(testExecutionId)) {
            throw new IllegalArgumentException("Test with id " + testExecutionId + " does not exist.");
        }

        InetAddress[] addresses = InetAddress.getAllByName("worker");

        List<String> workerNodes = new ArrayList<>();

        for (InetAddress address : addresses) {
            String url = "http://" + address.getHostAddress() + ":8083/api/exec/" + testExecutionId;
            restTemplate.delete(url);
            workerNodes.add(address.toString());
        }

        return workerNodes;
    }

    @Override
    public void processResultFile(String testExecutionId, MultipartFile file, Integer workerNumber) {
        logger.info("Received results file from worker node!");
        TestStartDto testStartDto = testExecutionMap.get(testExecutionId);
        if (testStartDto != null) {
            // worker node renames the file and sends the worker number in the fileName
            File testResultLog = new File("/results/" + testExecutionId + "/" + file.getOriginalFilename()); //getFinalLogFile("/results/" + testExecutionId + "/", file.getOriginalFilename(), workerNumber);
            logger.info("Trying to save log into file {}", testResultLog.getAbsolutePath());
            File finalLog = new File("/results/" + testExecutionId + "/" + testStartDto.getTestExecutionDto().getLogFileName());
            try {
                file.transferTo(testResultLog);
                WorkerNodeResult workerNodeResult = testStartDto.getWorkerNodeResults().get(workerNumber);
                if (workerNodeResult != null) {
                    workerNodeResult.setResultsReceived(true);
                    workerNodeResult.setResultFileName(testResultLog.getName());
                } else {
                    logger.warn("Received results from unknown worker with number {} for testExecutionId {}", workerNumber, testExecutionId);
                }

            } catch (IOException e) {
                logger.error("Unable to save file to destination {}", testResultLog.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }

            try (
                    BufferedReader reader = new BufferedReader(new FileReader(testResultLog));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(finalLog, true));
            ) {
                String line;
                Pattern pattern = null;
                if (testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern() != null) {
                    pattern = Pattern.compile(testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern());
                }
                while ((line = reader.readLine()) != null) {
                    if (pattern == null || !pattern.matcher(line).find()) {
                        if (line.endsWith("\n")) {
                            writer.append(line);
                        } else {
                            writer.append(line).append("\n");
                        }
                    } else {
                        logger.info("Skipping line from single result file.");
                    }
                }
                writer.flush();
            } catch (IOException e) {
                logger.error("Error aggregating results files.");
                throw new RuntimeException(e);
            }

        } else {
            throw new IllegalArgumentException("Invalid testExecutionId, control node received unknown results.");
        }
    }

    @Override
    public String processResultBatch(String testExecutionId, List<String> logLines, String finalLogFileName, Integer workerNumber, boolean lastBatch) {
        TestStartDto testStartDto = testExecutionMap.get(testExecutionId);

        logger.info("Received results file from worker node!");
        if (testStartDto != null && testStartDto.getTestExecutionDto() != null) {
            //int fileNumber = 1;
            File testResultLog = finalLogFileName != null ? new File("/results/" + testExecutionId + "/" + finalLogFileName) : getFinalLogFile("/results/" + testExecutionId + "/", testStartDto.getTestExecutionDto().getLogFileName(), workerNumber);
            logger.info("Trying to save log into file {}", testResultLog.getAbsolutePath());
            try (
                    BufferedWriter writer = new BufferedWriter(new FileWriter(testResultLog, true));
                    BufferedWriter aggregatedFileWriter = new BufferedWriter(new FileWriter("/results/" + testExecutionId + "/" + testStartDto.getTestExecutionDto().getLogFileName(), true))) {
                Pattern pattern = null;
                if (testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern() != null) {
                    pattern = Pattern.compile(testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern());
                }
                for (String line : logLines) {
                    if (line.endsWith("\n")) {
                        writer.append(line);
                        if (pattern == null || !pattern.matcher(line).find()) {
                            aggregatedFileWriter.append(line);
                        }
                    } else {
                        writer.append(line).append("\n");
                        if (pattern == null || !pattern.matcher(line).find()) {
                            aggregatedFileWriter.append(line).append("\n");
                        }
                    }
                }

                writer.flush(); // .close() called automatically in try-catch with resources
                aggregatedFileWriter.flush();
            } catch (IOException e) {
                logger.error("Unable to save file to destination {}", testResultLog.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }

            if (lastBatch) {
                logger.info("LastBatch from worker node has been received.");
                WorkerNodeResult workerNodeResult = testStartDto.getWorkerNodeResults().get(workerNumber);
                if (workerNodeResult != null) {
                    workerNodeResult.setResultsReceived(true);
                    workerNodeResult.setResultFileName(testResultLog.getName());
                } else {
                    logger.warn("Received results from unknown worker with number {} for testExecutionId {}", workerNumber, testExecutionId);
                }
            }

            return testResultLog.getName();
        } else {
            throw new IllegalArgumentException("Invalid testExecutionId, control node received unknown results.");
        }
    }

    @Override
    public TestStartDto getTestById(String testExecutionId) {
        TestStartDto testStartDto = testExecutionMap.get(testExecutionId);
        if (testStartDto != null) {
            return testStartDto;
        }
        throw new IllegalArgumentException("TestExecution with id " + testExecutionId + " does not exist.");
    }

    @Override
    public void registerTestEnd(String testExecutionId, Integer workerNumber) {
        logger.info("Test run has ended on workerNode {} for id {}", workerNumber, testExecutionId);
        TestStartDto testStartDto = testExecutionMap.get(testExecutionId);
        if (testStartDto != null) {
            WorkerNodeResult workerNodeResult = testStartDto.getWorkerNodeResults().get(workerNumber);
            if (workerNodeResult != null) {
                workerNodeResult.setTestEnded(true);
                return;
            }
            throw new IllegalArgumentException("WorkerNumber " + workerNumber + " does not exist for id " + testExecutionId);
        }
        throw new IllegalArgumentException("TestExecution with id " + testExecutionId + " does not exist.");
    }

    private File getFinalLogFile(String workDir, String originalFileName, int fileNumber) {
        File f = new File(workDir + fileNumber + "_" + originalFileName);
        /*if (f.exists()) {
            fileNumber += 1;
            return getFinalLogFile(workDir, originalFileName, fileNumber);
        }*/
        return f;
    }

    private void start(String url, TestExecutionDto dto) {
        logger.info("Trying to start test on worker node.");
        restTemplate.postForObject(url, dto, Void.class);
    }

}
