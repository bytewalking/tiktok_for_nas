package com.tiktoknas.controller;

import com.tiktoknas.service.HlsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/hls")
@RequiredArgsConstructor
public class HlsController {

    private final HlsService hlsService;

    @GetMapping("/{id}/{filename:.+}")
    public void serveHlsFile(@PathVariable Long id,
                             @PathVariable String filename,
                             HttpServletResponse response) throws IOException {
        hlsService.serveHlsFile(id, filename, response);
    }
}
