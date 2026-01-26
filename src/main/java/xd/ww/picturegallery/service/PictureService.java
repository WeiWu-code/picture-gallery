package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.scheduling.annotation.Async;
import xd.ww.picturegallery.api.aliyunai.model.CreateOutPaintingTaskResponse;
import xd.ww.picturegallery.model.dto.picture.*;
import xd.ww.picturegallery.model.entity.Picture;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource 文件内容
     * @param pictureUploadRequest 待上传的请求
     * @param loginUser 当前登录用户
     * @return 封装的Picture对象
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * AI 扩展图片服务
     *
     * @param createPictureOutPaintingTaskRequest 创建扩图任务请求
     * @param loginUser 登录的用户
     * @return CreateOutPaintingTaskResponse
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);



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


    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser 当前登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充Picture的审核状态
     * @param picture 待审核图片
     * @param loginUser 当前登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求
     * @param loginUser 当前登录用户
     * @return 成功创建的图片数
     */
    int uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 清理COS中不需要的图片，支持异步
     * @param oldPicture 待清理的图片
     */
    @Async
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验对图片的权限
     * @param loginUser 当前登录用户
     * @param picture 当前图片
     */
    void checkPictureAuth(User loginUser, Picture picture);


    /**
     * 删除图片
     * @param pictureId 待删除的图片ID
     * @param loginUser 当前登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest 编辑图片的请求
     * @param loginUser 当前登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色在指定的空间中选择图片
     * @param spaceId 空间Id
     * @param picColor 颜色
     * @param loginUser 当前登录用户
     * @return List<PictureVO>
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);


    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest 批量编辑图片请求
     * @param loginUser 当前登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
