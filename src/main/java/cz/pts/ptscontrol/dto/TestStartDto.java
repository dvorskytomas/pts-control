package cz.pts.ptscontrol.dto;

import java.util.ArrayList;
import java.util.List;

public class TestStartDto {

    private String testExecutionId;
    private List<String> workerNodesAddresses = new ArrayList<>();

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

}
