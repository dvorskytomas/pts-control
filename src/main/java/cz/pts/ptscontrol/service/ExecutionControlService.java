package cz.pts.ptscontrol.service;

import cz.pts.ptscontrol.dto.TestExecutionDto;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.net.UnknownHostException;
import java.util.List;

public interface ExecutionControlService {

    List<String> distributeAndStartTest(MultipartFile testFile, TestExecutionDto testExecutionDto) throws UnknownHostException;

}
