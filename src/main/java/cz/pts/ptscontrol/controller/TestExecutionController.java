package cz.pts.ptscontrol.controller;

import cz.pts.ptscontrol.dto.TestExecutionDto;
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

    /*@PostMapping("/start")
    public List<String> startTest(@RequestParam(name = "k6", required = false) boolean isK6) throws UnknownHostException {
        List<String> workerNodes = new ArrayList<>();

        InetAddress[] addresses = InetAddress.getAllByName("worker");

        for (InetAddress address : addresses) {
            System.out.println(address.toString());
            workerNodes.add(address.toString());
            String url = "http://" + address.getHostAddress() + ":8083/api/exec";
            if (isK6) {
                url += "/k6";
            }
            restTemplate.postForObject(url, null, Void.class);
        }

        return workerNodes;
    }*/

    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<String> startTest(@RequestPart(value = "testFile") MultipartFile testFile, @RequestPart(value = "testDef") TestExecutionDto testExecutionDto) throws UnknownHostException {
        return executionControlService.distributeAndStartTest(testFile, testExecutionDto);
    }

}
