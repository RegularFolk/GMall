package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("admin/product/")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        String path = null;
        if (configFile != null) {
            ClientGlobal.init(configFile);//初始化
            TrackerClient trackerClient = new TrackerClient();//获取客户端，用于获取链接
            TrackerServer connection = trackerClient.getConnection();//获取链接，用户获取storageClient
            StorageClient1 storageClient1 = new StorageClient1(connection, null);
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            System.out.println(fileUrl + path);
        }
        return Result.ok(fileUrl + path);
    }
}
