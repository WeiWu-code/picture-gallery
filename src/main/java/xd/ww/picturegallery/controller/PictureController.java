package xd.ww.picturegallery.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import xd.ww.picturegallery.annotation.AuthCheck;
import xd.ww.picturegallery.common.BaseResponse;
import xd.ww.picturegallery.common.ResultUtils;
import xd.ww.picturegallery.constant.UserConstant;
import xd.ww.picturegallery.model.dto.picture.PictureUploadRequest;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.PictureVO;
import xd.ww.picturegallery.service.PictureService;
import xd.ww.picturegallery.service.UserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

}
