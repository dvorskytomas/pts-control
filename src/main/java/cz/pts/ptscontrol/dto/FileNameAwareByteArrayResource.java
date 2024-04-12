package cz.pts.ptscontrol.dto;

import org.springframework.core.io.ByteArrayResource;

public class FileNameAwareByteArrayResource extends ByteArrayResource {

    private final String fileName;

    public FileNameAwareByteArrayResource(String fileName, byte[] byteArray) {
        super(byteArray);
        this.fileName = fileName;
    }

    @Override
    public String getFilename() {
        return fileName;
    }

}
