package cz.pts.ptscontrol.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class TestStartDto {

    private String testExecutionId;
    private List<String> workerNodesAddresses = new ArrayList<>();

    private TestExecutionDto testExecutionDto;

    @JsonIgnore
    private final Map<Integer, Boolean> workerResultsReceived = Collections.synchronizedMap(new HashMap<>());

    public String getTestExecutionId() {
        return testExecutionId;
    }

    public void setTestExecutionId(String testExecutionId) {
        this.testExecutionId = testExecutionId;
    }

    public List<String> getWorkerNodesAddresses() {
        return workerNodesAddresses;
    }

    public void setWorkerNodesAddresses(List<String> workerNodesAddresses) {
        this.workerNodesAddresses = workerNodesAddresses;
    }

    public TestExecutionDto getTestExecutionDto() {
        return testExecutionDto;
    }

    public void setTestExecutionDto(TestExecutionDto testExecutionDto) {
        this.testExecutionDto = testExecutionDto;
    }

    public Map<Integer, Boolean> getWorkerResultsReceived() {
        return workerResultsReceived;
    }

}
