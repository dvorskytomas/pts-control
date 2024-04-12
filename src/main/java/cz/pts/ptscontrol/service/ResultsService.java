package cz.pts.ptscontrol.service;

import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;


public interface ResultsService {

    ByteArrayResource downloadResultFile(String testExecutionId) throws IOException;
}
