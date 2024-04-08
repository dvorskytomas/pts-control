package cz.pts.ptscontrol.dto;

public class TestExecutionDto {

    private String testExecutionId;
    private String toolDirectoryPath;
    private String toolRunCommand;
    private String logFileName;
    private String testFileName;

    public String getTestExecutionId() {
        return testExecutionId;
    }

    public void setTestExecutionId(String testExecutionId) {
        this.testExecutionId = testExecutionId;
    }

    public String getToolDirectoryPath() {
        return toolDirectoryPath;
    }

    public void setToolDirectoryPath(String toolDirectoryPath) {
        this.toolDirectoryPath = toolDirectoryPath;
    }

    public String getToolRunCommand() {
        return toolRunCommand;
    }

    public void setToolRunCommand(String toolRunCommand) {
        this.toolRunCommand = toolRunCommand;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public String getTestFileName() {
        return testFileName;
    }

    public void setTestFileName(String testFileName) {
        this.testFileName = testFileName;
    }
}
