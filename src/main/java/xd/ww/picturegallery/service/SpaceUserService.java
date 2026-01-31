package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import xd.ww.picturegallery.model.dto.spaceuser.SpaceUserAddRequest;
import xd.ww.picturegallery.model.dto.spaceuser.SpaceUserQueryRequest;
import xd.ww.picturegallery.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import xd.ww.picturegallery.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wei
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-01-31 18:20:44
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     * @param spaceUserAddRequest 添加空间成员
     * @return 返回添加后的空间成员条目ID
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验添加成员是否合理
     * @param spaceUser 待添加的空间成员表
     * @param add 创建true还是修改false
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 根据spaceUser请求封装类，生成queryWrapper
     * @param spaceUserQueryRequest spaceUser请求封装类
     * @return QueryWrapper对象
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询spaceUserVo，填充SpaceUser内容，和User内容
     * @param spaceUser 待查询的spaceUser
     * @param request HttpServletRequest
     * @return 返回SpaceUserVo
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 同getSpaceUserVO，批量的版本
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
