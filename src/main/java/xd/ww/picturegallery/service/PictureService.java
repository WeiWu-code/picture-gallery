package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import xd.ww.picturegallery.model.dto.picture.PictureUploadRequest;
import xd.ww.picturegallery.model.entity.Picture;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.PictureVO;

public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile 文件内容
     * @param pictureUploadRequest 待上传的请求
     * @param loginUser 当前登录用户
     * @return 封装的Picture对象
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

}
