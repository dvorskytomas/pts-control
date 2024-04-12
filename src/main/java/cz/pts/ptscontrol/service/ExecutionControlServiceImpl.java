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

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

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

        if (testExecutionDto.getTestExecutionId() == null) {
            testExecutionDto.setTestExecutionId(UUID.randomUUID().toString());
        }

        if (testExecutionMap.containsKey(testExecutionDto.getTestExecutionId())) {
            logger.error("Test run with id {} already exists.", testExecutionDto.getTestExecutionId());
            throw new IllegalArgumentException("Test run with id " + testExecutionDto.getTestExecutionId() + " already exists");
        }

        List<String> workerNodes = new ArrayList<>();

        InetAddress[] addresses = InetAddress.getAllByName("worker");

        // distribute test file to all nodes
        if (testFile != null) {
            for (InetAddress address : addresses) {
                logger.info("Found worker node address: {}", address.toString());
                String url = "http://" + address.getHostAddress() + ":8083/api/upload";
                distribute(testFile, testExecutionDto.getToolDirectoryPath(), url);
            }
        }

        TestStartDto response = new TestStartDto();
        // start test on all nodes
        for (int i = 0; i < addresses.length; i++) {
            String url = "http://" + addresses[i].getHostAddress() + ":8083/api/exec";
            // we only have one request object - reset it afterward?
            testExecutionDto.setWorkerNumber(i + 1);
            start(url, testExecutionDto);
            response.getWorkerResultsReceived().put(i + 1, false);
            workerNodes.add(addresses[i].toString());
        }


        response.setTestExecutionId(testExecutionDto.getTestExecutionId());
        response.setWorkerNodesAddresses(workerNodes);
        response.setTestExecutionDto(testExecutionDto);

        testExecutionMap.put(testExecutionDto.getTestExecutionId(), response);

        // TODO create result folder...
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

        return response;
    }

    @Override
    public List<String> stopTestExecution(String testExecutionId) throws UnknownHostException {
        logger.warn("Stopping test with id {} forcibly.", testExecutionId);
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
            //int fileNumber = 1;
            File testResultLog = getFinalLogFile("/results/" + testExecutionId + "/", file.getOriginalFilename(), workerNumber);
            logger.info("Trying to save log into file {}", testResultLog.getAbsolutePath());
            try {
                file.transferTo(testResultLog);
                testStartDto.getWorkerResultsReceived().put(workerNumber, true);
                logger.info("Results ready: {}/{}", testStartDto.getWorkerResultsReceived().values().stream().filter(Boolean.TRUE::equals).count(), testStartDto.getWorkerResultsReceived().size());

            } catch (IOException e) {
                logger.error("Unable to save file to destination {}", testResultLog.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }

            // FIXME - tohle nebudeme delat na to, az prijde posledni test, ale hned... budeme muset ten file precist
            aggregateResults(testStartDto);

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
            } catch (IOException e) {
                logger.error("Unable to save file to destination {}", testResultLog.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }

            if (lastBatch) {
                logger.info("LastBatch from worker node has been received.");
                testStartDto.getWorkerResultsReceived().put(workerNumber, true);
                logger.info("Results ready: {}/{}", testStartDto.getWorkerResultsReceived().values().stream().filter(Boolean.TRUE::equals).count(), testStartDto.getWorkerResultsReceived().size());
                //aggregateResults(testStartDto);
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

    private void aggregateResults(TestStartDto testStartDto) {
        if (testStartDto.getWorkerResultsReceived().values().stream().allMatch(Boolean.TRUE::equals)) {
            logger.info("Trying to aggregate results from testRun with id {}", testStartDto.getTestExecutionId());
            String logFileName = testStartDto.getTestExecutionDto().getLogFileName();
            File resultsDir = new File("/results/" + testStartDto.getTestExecutionId());
            File[] allResults = resultsDir.listFiles();
            if (allResults != null) {
                logger.info("Found {} results files.", allResults.length);
                for (File resultFile : allResults) {
                    if (!resultFile.getName().equals(logFileName)) {
                        try (
                                BufferedReader reader = new BufferedReader(new FileReader(resultFile));
                                BufferedWriter writer = new BufferedWriter(new FileWriter("/results/" + testStartDto.getTestExecutionId() + "/" + logFileName, true));
                        ) {
                            int linesSkipped = 0;
                            String line;
                            Pattern pattern = null;
                            if (testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern() != null) {
                                pattern = Pattern.compile(testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipPattern());
                            }
                            while ((line = reader.readLine()) != null) {
                                //linesSkipped >= testStartDto.getTestExecutionDto().getResultProcessingConfig().getSkipLines()
                                if (pattern == null || !pattern.matcher(line).find()) {
                                    if (line.endsWith("\n")) {
                                        writer.append(line);
                                    } else {
                                        writer.append(line).append("\n");
                                    }
                                } else {
                                    logger.info("Skipping line from single result file.");
                                    linesSkipped += 1;
                                }
                            }
                            writer.flush();
                        } catch (IOException e) {
                            logger.error("Error aggregating results files.");
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    private File getFinalLogFile(String workDir, String originalFileName, int fileNumber) {
        File f = new File(workDir + fileNumber + "_" + originalFileName);
        if (f.exists()) {
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
