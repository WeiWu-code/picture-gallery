package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import xd.ww.picturegallery.model.dto.picture.PictureQueryRequest;
import xd.ww.picturegallery.model.dto.picture.PictureUploadRequest;
import xd.ww.picturegallery.model.entity.Picture;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

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


    /**
     * 根据图片查询请求条件构造查询包装器
     * <p> 根据传入的 PictureQueryRequest 对象构建一个 QueryWrapper<Picture> 实例,
     * 用于生成数据库查询条件, 实现对图片数据的动态查询
     *
     * @param pictureQueryRequest 图片查询请求条件对象, 包含查询参数和过滤条件
     * @return 构建好的 QueryWrapper<Picture> 实例, 用于执行数据库查询操作
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * @param picture 原始图片
     * @param request HttpServletRequest
     * @return 封装后的图片
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页图片的封装
     * @param picturePage 分页图片
     * @param request HttpServletRequest
     * @return 封装好的Page
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 检验图片
     * @param picture 待检验的图片
     */
    void validPicture(Picture picture);
}
