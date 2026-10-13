package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

//sysmsg-start，總共1次修改，第1次//
class SysmsgImageServiceTest {
    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private final SysmsgImageService service = new SysmsgImageService(cloudinary, "demo");

    @Test
    void validatesEveryFileBeforeStartingUpload() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        MockMultipartFile png = new MockMultipartFile("files", "a.png", "image/png", pngBytes());
        MockMultipartFile spoofed = new MockMultipartFile("files", "b.png", "image/png", "not-png".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.upload(List.of(png, spoofed), 7));
        verify(uploader, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void failedLaterUploadDeletesEarlierAsset() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenAnswer(invocation -> response(invocation.getArgument(1)))
                .thenThrow(new IOException("second upload failed"));
        MockMultipartFile first = new MockMultipartFile("files", "a.png", "image/png", pngBytes());
        MockMultipartFile second = new MockMultipartFile("files", "b.png", "image/png", pngBytes());

        assertThrows(RuntimeException.class, () -> service.upload(List.of(first, second), 7));
        verify(uploader).destroy(any(String.class), anyMap());
    }

    private Map<String, Object> response(Map<String, Object> options) {
        String publicId = options.get("public_id").toString();
        return Map.of("asset_id", "asset", "public_id", publicId, "resource_type", "image",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/" + publicId + ".png",
                "format", "png", "bytes", 8, "width", 1, "height", 1);
    }

    private byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
    }
}
//sysmsg-end，總共1次修改，第1次//

