package xd.ww.picturegallery.manager.cache;

import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xd.ww.picturegallery.model.dto.picture.PictureQueryRequest;
import xd.ww.picturegallery.model.vo.PictureVO;
import xd.ww.picturegallery.utils.CacheKeyUtils;

import javax.annotation.Resource;

/**
 * 多级缓存Manager类
 */
@Component
@Slf4j
public class CacheManager {

    private static final TypeReference<PictureVO> TYPE_PICTURE_VO = new TypeReference<PictureVO>() {};
    private static final TypeReference<Page<PictureVO>> TYPE_PICTURE_VO_PAGE = new TypeReference<Page<PictureVO>>() {};

    @Resource
    private CacheVersionManager cacheVersionManager;

    @Resource(name = "localCache")
    private EnhancedCacheChainTemplate cacheChainTemplate;

    public CacheResult<PictureVO> getPictureVoById(Long id) {
        return cacheChainTemplate.getObjectValueForString(
                CacheKeyUtils.getPictureVoByIdKey(id), TYPE_PICTURE_VO, true);
    }

    public CacheResult<Page<PictureVO>> listPictureVoByPage(PictureQueryRequest pictureQueryRequest) {
        // 获取当前最新的版本号 (e.g., "2")
        String version = cacheVersionManager.getPictureListVersion();
        // 生成带版本的 Key
        String key = CacheKeyUtils.listPictureByPageVoKey(pictureQueryRequest, version);

        return cacheChainTemplate.getObjectValueForString(
                key,
                TYPE_PICTURE_VO_PAGE, false);
    }

    public void setCacheForValue(String key, String value, boolean isBloom) {
        if(!isBloom) {
            cacheChainTemplate.setStringValueChainWithoutBloom(key, value);
        }else{
            cacheChainTemplate.setStringValueChain(key, value);
        }
    }

    public void setCacheForPageValue(PictureQueryRequest pictureQueryRequest, String value) {
        // 获取当前最新的版本号 (e.g., "2")
        String version = cacheVersionManager.getPictureListVersion();
        // 生成带版本的 Key
        String key = CacheKeyUtils.listPictureByPageVoKey(pictureQueryRequest, version);
        cacheChainTemplate.setStringValueChainWithoutBloom(key, value);
    }

    /**
     * 触发列表刷新
     */
    public void refreshPageCache() {
        cacheVersionManager.refreshPictureListVersion();
    }


    public String getValueForString(String key) {
        return cacheChainTemplate.getStringValueChain(key);
    }

    public void deleteCacheByKey(String key) {
        cacheChainTemplate.deleteValueChain(key);
    }




}
