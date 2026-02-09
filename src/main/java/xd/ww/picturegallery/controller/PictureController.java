package xd.ww.picturegallery.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xd.ww.picturegallery.annotation.AuthCheck;
import xd.ww.picturegallery.api.aliyunai.AliYunAiApi;
import xd.ww.picturegallery.api.aliyunai.model.CreateOutPaintingTaskResponse;
import xd.ww.picturegallery.api.aliyunai.model.GetOutPaintingTaskResponse;
import xd.ww.picturegallery.api.imagesearch.so.model.SearchPictureByPictureRequest;
import xd.ww.picturegallery.api.imagesearch.so.model.SoImageSearchResult;
import xd.ww.picturegallery.api.imagesearch.so.sub.SoImageSearchApiFacade;
import xd.ww.picturegallery.common.BaseResponse;
import xd.ww.picturegallery.common.DeleteRequest;
import xd.ww.picturegallery.common.ResultUtils;
import xd.ww.picturegallery.constant.UserConstant;
import xd.ww.picturegallery.exception.BusinessException;
import xd.ww.picturegallery.exception.ErrorCode;
import xd.ww.picturegallery.exception.ThrowUtils;
import xd.ww.picturegallery.manager.cache.CacheManager;
import xd.ww.picturegallery.manager.auth.SpaceUserAuthManager;
import xd.ww.picturegallery.manager.auth.StpKit;
import xd.ww.picturegallery.manager.auth.annotation.SaSpaceCheckPermission;
import xd.ww.picturegallery.manager.auth.model.SpaceUserPermissionConstant;
import xd.ww.picturegallery.manager.cache.CacheResult;
import xd.ww.picturegallery.model.dto.picture.*;
import xd.ww.picturegallery.model.entity.Picture;
import xd.ww.picturegallery.model.entity.PictureTagCategory;
import xd.ww.picturegallery.model.entity.Space;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.enums.PictureReviewStatusEnum;
import xd.ww.picturegallery.model.vo.PictureVO;
import xd.ww.picturegallery.ratelimit.annotation.RateLimit;
import xd.ww.picturegallery.ratelimit.model.RateLimitType;
import xd.ww.picturegallery.service.PictureService;
import xd.ww.picturegallery.service.SpaceService;
import xd.ww.picturegallery.service.UserService;
import xd.ww.picturegallery.utils.CacheKeyUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static xd.ww.picturegallery.manager.cache.EnhancedCacheChainTemplate.NULL_OBJECT;

@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private AliYunAiApi aliYunAiApi;

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        // 上传后添加
        String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(pictureVO.getId());
        cacheManager.setCacheForValue(pictureVoByIdKey, JSONUtil.toJsonStr(pictureVO), true);
        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        // 查询是否在缓存中
        cacheManager.deleteCacheByKey(CacheKeyUtils.getPictureVoByIdKey(deleteRequest.getId()));
        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }



    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询缓存
        CacheResult<PictureVO> pictureVoById = cacheManager.getPictureVoById(id);
        if(pictureVoById.isHit() && pictureVoById.getData() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }else if(pictureVoById.isHit()) {
            return ResultUtils.success(pictureVoById.getData());
        }
        // 下面是pictureVoById.isHit() false

        // 查询数据库
        Picture picture = pictureService.getById(id);
        if(picture == null){
            // 要设置NULL值
            // 加入缓存
            String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(id);
            cacheManager.setCacheForValue(pictureVoByIdKey, NULL_OBJECT, true);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 加入缓存
        String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(id);
        cacheManager.setCacheForValue(pictureVoByIdKey, JSONUtil.toJsonStr(pictureVO), true);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询缓存链
        CacheResult<Page<PictureVO>> cacheVOPage = cacheManager.listPictureVoByPage(pictureQueryRequest);
        if(cacheVOPage.isHit() && cacheVOPage.getData() != null) {
            return ResultUtils.success(cacheVOPage.getData());
        }
        // 查询数据库
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 添加进缓存
        cacheManager.setCacheForPageValue(pictureQueryRequest, JSONUtil.toJsonStr(pictureVOPage));
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        // 上传后添加
        String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(pictureVO.getId());
        cacheManager.setCacheForValue(pictureVoByIdKey, JSONUtil.toJsonStr(pictureVO), true);

        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param request HttpServletRequest
     * @return 上传的数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(uploadCount);
    }


    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        // 立刻删除缓存
        String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(pictureEditRequest.getId());
        cacheManager.deleteCacheByKey(pictureVoByIdKey);
        return ResultUtils.success(true);
    }


    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "网络", "创意");
        List<String> categoryList = Arrays.asList("自然风光","城市建筑","人像摄影","静物特写","动物植物","美食餐饮","抽象艺术","商务科技");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        // 立刻删除缓存
        String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(pictureReviewRequest.getId());
        cacheManager.deleteCacheByKey(pictureVoByIdKey);

        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<SoImageSearchResult>> searchPictureByPictureIsSo(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 这个 start 是控制查询多少页, 每页是 20 条
        int start = 0;
        List<SoImageSearchResult> resultList = SoImageSearchApiFacade.searchImage(
                oldPicture.getUrl(), start
        );
        return ResultUtils.success(resultList);
    }

    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        // 删除这一批次的缓存
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        for(Long pictureId : pictureIdList){
            // 立刻删除缓存
            String pictureVoByIdKey = CacheKeyUtils.getPictureVoByIdKey(pictureId);
            cacheManager.deleteCacheByKey(pictureVoByIdKey);
        }
        // 升级版本号
        // 这会让所有 listPictureVOByPage 的 Key 瞬间“逻辑失效”
        // 下一个请求会用新版本号生成 Key，查不到缓存，从而去查 DB 拿最新数据
        cacheManager.refreshPageCache();
        return ResultUtils.success(true);
    }


    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60,message = "AI扩图请求过于频繁，请稍后再试")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

}
