package org.example;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class DownloadController {
    private static final int DUMMY_FILE_SIZE = 10 * 1024 * 1024; //10 MB
    private static final File dummy_10Mb; //for demo purposes
    
    static {
        // Let's generate dummy file on-the-fly, in order to not store it in repository as resource
        try {
            dummy_10Mb = File.createTempFile("dummy_10Mb", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileOutputStream stream = new FileOutputStream(dummy_10Mb)) {
            stream.write(new byte[DUMMY_FILE_SIZE]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final MediaType APPLICATION_ZIP = MediaType.valueOf("application/zip");
    
    @GetMapping(value = "/download")
    public ResponseEntity<StreamingResponseBody> download(final HttpServletResponse response) {
        final int nFiles = 100;
        StreamingResponseBody stream = out -> {
            try (final ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(response.getOutputStream())) {
                zipOut.setMethod(ZipArchiveOutputStream.STORED); //for demo purposes, avoid compression to get a big output file
                final byte[] bytes = new byte[1024];
                for (int i = 0; i < nFiles; i++) {
                    try (final InputStream inputStream = Files.newInputStream(dummy_10Mb.toPath())) {
                        ArchiveEntry archiveEntry = zipOut.createArchiveEntry(dummy_10Mb, String.valueOf(i));
                        ((ZipArchiveEntry) archiveEntry).setCrc(0X9eca2accL); //required by STORED method, used for demo purposes
                        zipOut.putArchiveEntry(archiveEntry);
                        int length;
                        while ((length = inputStream.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                        zipOut.closeArchiveEntry();
                    }
                }
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample.zip")
//                .header(HttpHeaders.CONTENT_LENGTH, ...) //but in streaming mode we don't know zipped file size in advance
                .contentType(APPLICATION_ZIP)
                .body(stream);
    }
}