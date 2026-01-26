package xd.ww.picturegallery.api.imagesearch.so.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import xd.ww.picturegallery.api.imagesearch.so.model.SoImageSearchResult;
import xd.ww.picturegallery.exception.BusinessException;
import xd.ww.picturegallery.exception.ErrorCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取360搜图搜索的图片的列表
 *
 * @version 1.0
 * @since 1.8
 */
@Slf4j
public class GetSoImageListApi {

	/**
	 * 获取图片列表
	 *
	 * @param imageUrl 图片地址, 在 360 库中的地址
	 * @return 图片列表对象
	 */
	public static List<SoImageSearchResult> getImageList(String imageUrl, Integer start) {
		String url = "https://st.so.com/stu?a=mrecomm&start=" + start;
		Map<String, Object> formData = new HashMap<>();
		formData.put("img_url", imageUrl);
        JSONObject body;
        try (HttpResponse response = HttpRequest.post(url)
                .form(formData)
                .timeout(5000)
                .execute()) {
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败，请稍后重试");
            }
            // 解析响应
            body = JSONUtil.parseObj(response.body());
        }
        // 处理响应结果
		if (!Integer.valueOf(0).equals(body.getInt("errno"))) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败，请稍后重试");
		}
		JSONObject data = body.getJSONObject("data");
		List<SoImageSearchResult> result = data.getBeanList("result", SoImageSearchResult.class);
		// 对结果进行处理, 因为返回的是分开的对象, 不是一个完整的图片路径, 这里需要自己拼接
		for (SoImageSearchResult soImageSearchResult : result) {
			String prefix;
			if (StrUtil.isNotBlank(soImageSearchResult.getHttps())) {
				prefix = "https://" + soImageSearchResult.getHttps() + "/";
			} else {
				prefix = "http://" + soImageSearchResult.getHttp() + "/";
			}
			soImageSearchResult.setImgUrl(prefix + soImageSearchResult.getImgkey());
		}
		return result;
	}

	public static void main(String[] args) {
		List<SoImageSearchResult> imageList = getImageList("\n" +
                "http://p1.so.qhimg.com/t024dae189ec33f700b.jpg", 0);
		System.out.println("搜索结果: " + JSONUtil.parse(imageList));
	}

}

