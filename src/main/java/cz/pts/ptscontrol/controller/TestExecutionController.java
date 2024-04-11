package cz.pts.ptscontrol.controller;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import cz.pts.ptscontrol.dto.TestStartDto;
import cz.pts.ptscontrol.service.ExecutionControlService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestExecutionController {

    private final ExecutionControlService executionControlService;

    public TestExecutionController(ExecutionControlService executionControlService) {
        this.executionControlService = executionControlService;
    }

    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TestStartDto startTest(@RequestPart(value = "testFile", required = false) MultipartFile testFile, @RequestPart(value = "testDef") TestExecutionDto testExecutionDto) throws UnknownHostException {
        return executionControlService.distributeAndStartTest(testFile, testExecutionDto);
    }

    @DeleteMapping("/stop/{testExecutionId}")
    public List<String> stopTest(@PathVariable(name = "testExecutionId") String testExecutionId) throws UnknownHostException {
        return executionControlService.stopTestExecution(testExecutionId);
    }

    @PutMapping(value = "/result/file/{testExecutionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void receiveResultFile(@PathVariable(name = "testExecutionId") String testExecutionId, @RequestPart(value = "results") MultipartFile file) {
        executionControlService.processResultFile(testExecutionId, file);
    }

    @PutMapping(value = "/result/batch/{testExecutionId}")
    public String receiveResultBatch(@PathVariable(name = "testExecutionId") String testExecutionId, @RequestBody List<String> logLines, @RequestParam(name = "logFileName", required = false) String finalLogFileName) {
        return executionControlService.processResultBatch(testExecutionId, logLines, finalLogFileName);
    }

}
