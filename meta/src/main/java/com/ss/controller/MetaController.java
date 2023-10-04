package com.ss.controller;

import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.VO.BucketVO;
import com.ss.dto.CompleteChunkFileDTO;
import com.ss.dto.FileMeta;
import com.ss.response.CommonResponse;
import com.ss.service.MetaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/meta")
@RestController
public class MetaController {
    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    /**
     * 生成 meta 元数据
     * @return 元数据
     */
    @PostMapping("/generate")
    public CommonResponse<MetaFile> generate(@RequestBody FileMeta fileMeta){
        MetaFile metaFile = metaService.generate(fileMeta);
        return CommonResponse.success(metaFile);
    }

    /**
     * 获取 meta 元数据
     * 获取分片信息，包含每个分片的权重最大Server的地址
     * @return 元数据
     */
    @GetMapping("/info")
    public CommonResponse<MetaFile> info(@RequestParam("bucketName") String bucketName,
                                  @RequestParam("filename") String filename){
        MetaFile metaFile = metaService.meta(bucketName, filename);
        return CommonResponse.success(metaFile);
    }

    /**
     * 分片上传完成
     * @return void
     */
    @PostMapping("/chunk/complete")
    public CommonResponse<Void> complete(@RequestBody CompleteChunkFileDTO completeChunkFileDTO){
        metaService.completeChunk(completeChunkFileDTO);
        return CommonResponse.success();
    }

    /**
     * 分片信息
     * @return
     */
    @GetMapping("/chunk/info")
    public CommonResponse<?> chunkInfo(@RequestParam String bucketName,
                                       @RequestParam String filename,
                                       @RequestParam Integer chunkNo){
        List<FileChunkMeta> chunks = metaService.chunkInfo(bucketName, filename, chunkNo);
        return null;
    }

    /**
     * 前端页面查询files
     * 并按照bucketName分类
     * @return
     */
    @GetMapping("files")
    public CommonResponse<List<BucketVO>> files(){
        List<BucketVO> files = metaService.files();
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
        metaService.delete(bucketName, filename);
        return CommonResponse.success();
    }
}
