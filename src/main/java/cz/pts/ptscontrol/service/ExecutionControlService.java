package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import cz.pts.ptscontrol.dto.TestStartDto;
import org.springframework.web.multipart.MultipartFile;

import java.net.UnknownHostException;
import java.util.List;

public interface ExecutionControlService {

    TestStartDto distributeAndStartTest(MultipartFile testFile, TestExecutionDto testExecutionDto) throws UnknownHostException;

    List<String> stopTestExecution(String testExecutionId) throws UnknownHostException;

    void processResultFile(String testExecutionId, MultipartFile file, Integer workerNumber);

    String processResultBatch(String testExecutionId, List<String> logLines, String finalLogFileName, Integer workerNumber, boolean lastBatch);

    TestStartDto getTestById(String testExecutionId);

    void registerTestEnd(String testExecutionId, Integer workerNumber);
}
