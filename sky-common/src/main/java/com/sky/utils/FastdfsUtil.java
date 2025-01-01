package com.sky.utils;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;


@Slf4j
@Component
public class FastdfsUtil {

    @Autowired
    private FastFileStorageClient storageClient;

    @Value("${upload.base-url}")
    private String baseUrl;

    /**
     * 上传文件
     * @param file 文件对象
     * @return 文件访问地址
     * @throws IOException
     */
    public String upload(MultipartFile file) throws IOException {
        try {
            //校验文件
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
                throw new RuntimeException("上传文件不是图片");
            }
        } catch (IOException e) {
            log.error("校验文件内容失败....{}", e);
            throw new RuntimeException("校验文件内容失败" + e.getMessage());
        }
        try {
            // 获取扩展名
            String extension =
                    StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath =
                    storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);
            // 返回路径
            return baseUrl + storePath.getFullPath();
        } catch (IOException e) {
            log.error("【文件上传】上传文件失败！....{}", e);
            throw new RuntimeException("【文件上传】上传文件失败！" + e.getMessage());
        }
    }



    /**
     * 删除文件
     * @param fileUrl 文件访问地址
     * @return
     */
    public void deleteFile(String fileUrl){
//        String x="http:/139.196.97.69:8000/group2/M00/00/00/eEzAUmGkchiAfrLpAALN8qC2I6s106.jpg";
        int index1,index2;
        index1 = fileUrl.indexOf("/", fileUrl.indexOf("/") + 1);
        index2 = fileUrl.indexOf("/", index1 + 1);
        String group = fileUrl.substring(index1+1,index2);
        String path = fileUrl.substring(index2+1);
        System.out.println(group+"  "+path);
        storageClient.deleteFile(group,path);
    }

}
