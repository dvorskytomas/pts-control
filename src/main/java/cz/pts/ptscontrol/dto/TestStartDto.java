package cz.pts.ptscontrol.dto;

import java.util.*;

public class TestStartDto {

    private String testExecutionId;
    private List<String> workerNodesAddresses = new ArrayList<>();

    private TestExecutionDto testExecutionDto;

    private boolean testEnded;

    //@JsonIgnore
    private final Map<Integer, WorkerNodeResult> workerNodeResults = Collections.synchronizedMap(new HashMap<>());

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

    public Map<Integer, WorkerNodeResult> getWorkerNodeResults() {
        return workerNodeResults;
    }

    public boolean isTestEnded() {
        return workerNodeResults.values().stream().allMatch(WorkerNodeResult::isTestEnded);
    }

}
