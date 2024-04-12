package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.FileNameAwareByteArrayResource;
import cz.pts.ptscontrol.dto.TestStartDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ResultsServiceImpl implements ResultsService {

    private final ExecutionControlService executionControlService;

    public ResultsServiceImpl(ExecutionControlService executionControlService) {
        this.executionControlService = executionControlService;
    }

    @Override
    public ByteArrayResource downloadResultFile(String testExecutionId) throws IOException {
        try {
            TestStartDto testDef = executionControlService.getTestById(testExecutionId);
            Path path = Paths.get("/results/" + testExecutionId + "/" + testDef.getTestExecutionDto().getLogFileName());
            return new FileNameAwareByteArrayResource(testDef.getTestExecutionDto().getLogFileName(), Files.readAllBytes(path));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Results for testExecutionId " + testExecutionId + " do not exist");
        }
    }
}
