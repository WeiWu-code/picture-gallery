package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import xd.ww.picturegallery.model.dto.space.SpaceAddRequest;
import xd.ww.picturegallery.model.dto.space.SpaceQueryRequest;
import xd.ww.picturegallery.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author wei
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-24 16:13:59
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     * @param space 待检验的空间
     * @param add 是否是第一次创建
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别，自动填充限额
     * 如果没有设置空间，才会填充，否则会填充
     * @param space 待填充的空间
     */
    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

}
