package cz.pts.ptscontrol.dto;

public class WorkerNodeResult {

    private boolean resultsReceived;
    private String resultFileName;
    private boolean testEnded;

    public boolean isResultsReceived() {
        return resultsReceived;
    }

    public void setResultsReceived(boolean resultsReceived) {
        this.resultsReceived = resultsReceived;
    }

    public String getResultFileName() {
        return resultFileName;
    }

    public void setResultFileName(String resultFileName) {
        this.resultFileName = resultFileName;
    }

    public boolean isTestEnded() {
        return testEnded;
    }

    public void setTestEnded(boolean testEnded) {
        this.testEnded = testEnded;
    }
}
