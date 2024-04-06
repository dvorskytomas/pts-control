package cz.pts.ptscontrol;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestExecutionController {

    private final RestTemplate restTemplate;

    public TestExecutionController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/start")
    public List<String> startTest(@RequestParam(required = false) boolean isK6) throws UnknownHostException {
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
    }

}
