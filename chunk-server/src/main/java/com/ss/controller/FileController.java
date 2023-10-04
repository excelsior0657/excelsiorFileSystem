package com.ss.controller;

import com.ss.DTO.FileChunkDTO;
import com.ss.response.CommonResponse;
import com.ss.services.FileService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/write")
    public CommonResponse<String> write(@Valid @RequestBody FileChunkDTO fileChunkDTO){
        String md5 = fileService.write(fileChunkDTO);
        return CommonResponse.success(md5);

    }

    @GetMapping("/read")
    public CommonResponse<?> read(@RequestParam("filename") String filename,
                                  @RequestParam("extension") String extension,
                                  @RequestParam("chunkNo") Integer chunkNo,
                                  @RequestParam("bucketName") String bucketName){
        byte[] content = fileService.read(filename, extension, chunkNo, bucketName);
        return CommonResponse.success(content);
    }
}
