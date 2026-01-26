package xd.ww.picturegallery.api.imagesearch.so.sub;

import lombok.extern.slf4j.Slf4j;
import xd.ww.picturegallery.api.imagesearch.so.model.SoImageSearchResult;

import java.util.List;

/**
 * 360搜图图片搜索接口
 * 这里用了 门面模式
 */
@Slf4j
public class SoImageSearchApiFacade {

	/**
	 * 搜索图片
	 *
	 * @param imageUrl 需要以图搜图的图片地址
	 * @param start    开始下表
	 * @return 图片搜索结果列表
	 */
	public static List<SoImageSearchResult> searchImage(String imageUrl, Integer start) {
		String soImageUrl = GetSoImageUrlApi.getSoImageUrl(imageUrl);
        return GetSoImageListApi.getImageList(soImageUrl, start);
	}

	public static void main(String[] args) {
		// 测试以图搜图功能
		String imageUrl = "https://picture-gallery-1315094248.cos.ap-shanghai.myqcloud.com/space/2015723325592555522/2026-01-26_QUjMMpdMX6uDPeD9.webp";
		List<SoImageSearchResult> resultList = searchImage(imageUrl, 0);
		System.out.println("结果列表" + resultList);
	}
}
