package com.ss.controller;

import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.DTO.FileMeta;
import com.ss.VO.BucketVO;
import com.ss.VO.MetaFileVO;
import com.ss.error.BusinessException;
import com.ss.errors.EnumClientException;
import com.ss.services.FileService;
import com.ss.response.CommonResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 小文件上传
     *
     * @param bucketName 存储桶名
     * @param file       文件
     * @return 文件的访问路径
     */
    @PostMapping("/upload")
    public CommonResponse<String> upload(@RequestParam("bucketName") String bucketName,
                                         @RequestParam("file") MultipartFile file) {
        String fileUrl = fileService.upload(bucketName, file);
        return CommonResponse.success(fileUrl);
    }

    /**
     * 生成meta信息
     * 生成大文件上传所需要的meta信息MetaFileVO
     *
     * @param fileMeta fileMeta
     * @return metaFile
     */
    @PostMapping("/meta")
    public CommonResponse<?> meta(@RequestBody FileMeta fileMeta) {
        MetaFileVO metaFileVO = fileService.meta(fileMeta);
        return CommonResponse.success(metaFileVO);
    }

    /**
     * 分片文件上传
     *
     * @param bucketName 用于拼接查找文件路径
     * @param filename   用于拼接查找文件路径
     * @param md5        分片的md5
     * @param chunkNo    分片的序号
     * @param file       分片文件
     * @return MD5
     */
    @PostMapping("/chunk/upload")
    public CommonResponse<?> chunkUpload(@RequestParam("bucketName") String bucketName,
                                         @RequestParam("filename") String filename,
                                         @RequestParam("md5") String md5,
                                         @RequestParam("chunkNo") Integer chunkNo,
                                         @RequestParam("file") MultipartFile file) {
        String fileMd5 = fileService.uploadChunk(bucketName, filename, md5, chunkNo, file);
        return CommonResponse.success(fileMd5);
    }

    /**
     * 文件下载
     *
     * @param response   respons
     * @param bucketName 存储桶名
     * @param filename   文件名
     */
    @GetMapping("{bucketName}/{filename}")
    public void download(HttpServletResponse response,
                         @PathVariable String bucketName,
                         @PathVariable String filename) {
        // 获取meta信息
        MetaFile metaFile = fileService.getMeta(bucketName, filename);

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength(metaFile.getFilesize().intValue());
        response.setHeader("Content-Disposition", "attachment;filename=" + metaFile.getFilename() + " " + metaFile.getExtension());

        // 下载文件
        for (FileChunkMeta chunk : metaFile.getChunks()) {
            byte[] content = fileService.downloadChunk(chunk);
            try {
                response.getOutputStream().write(content);
            } catch (Exception e) {
                throw new BusinessException(EnumClientException.FAILED_TO_DOWNLOAD_FILE);
            }
        }
    }

    /**
     * 返回文件列表，并按照存储桶分类
     * @return
     */
    @GetMapping("files")
    public CommonResponse<?> files() {
        List<BucketVO> files = fileService.files();
        return CommonResponse.success(files);
    }

    /**
     * 删除文件
     * @param bucketName
     * @param filename
     * @return
     */
    @DeleteMapping("{bucketName}/{filename}")
    public CommonResponse<?> delete(@PathVariable String bucketName,
                                    @PathVariable String filename){
        fileService.delete(bucketName, filename);
        return CommonResponse.success();
    }

}
