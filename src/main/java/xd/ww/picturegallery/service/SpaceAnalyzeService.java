package xd.ww.picturegallery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xd.ww.picturegallery.model.dto.space.analyze.*;
import xd.ww.picturegallery.model.entity.Space;
import xd.ww.picturegallery.model.entity.User;
import xd.ww.picturegallery.model.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 分析图库使用情况
     * @param spaceUsageAnalyzeRequest 空间使用分析请求
     * @param loginUser 当前登录用户
     * @return SpaceUsageAnalyzeResponse
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 分析图库的类别
     * @param spaceCategoryAnalyzeRequest 图库类别分析请求
     * @param loginUser 当前登录用户
     * @return SpaceCategoryAnalyzeResponse
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 分析图片的标签
     * @param spaceTagAnalyzeRequest 图库标签分析请求
     * @param loginUser 当前登录用户
     * @return List<SpaceTagAnalyzeResponse>
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 分析图片大小
     * @param spaceSizeAnalyzeRequest 空间图片大小分析请求
     * @param loginUser 当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 分析空间用户的行为
     * @param spaceUserAnalyzeRequest 空间用户的行为分析请求
     * @param loginUser 当前登录用户
     * @return List<SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 查看前N个空间，按照存储使用量查询
     * 仅仅管理员可以查看
     * @param spaceRankAnalyzeRequest 空间排行查询请求
     * @param loginUser 当前登录用户
     * @return List<Space>
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
