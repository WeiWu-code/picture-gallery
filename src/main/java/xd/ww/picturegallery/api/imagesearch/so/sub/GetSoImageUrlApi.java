package xd.ww.picturegallery.api.imagesearch.so.sub;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import xd.ww.picturegallery.exception.BusinessException;
import xd.ww.picturegallery.exception.ErrorCode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 获取360搜图的图片的接口
 *
 * @author wei 2026年01月26 22:54
 * @version 1.0
 * @since 1.8
 */
@Slf4j
public class GetSoImageUrlApi {

	public static String getSoImageUrl(String imageUrl) {
        String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
		String url = "https://st.so.com/r?src=st&srcsp=home&img_url=" + encodedUrl + "&submittype=imgurl";

        try {
			Document document = Jsoup.connect(url).timeout(5000).get();
			Element imgElement = document.selectFirst(".img_img");
			if (imgElement != null) {
				String soImageUrl = "";
				// 获取当前元素的属性
				String style = imgElement.attr("style");
				if (style.contains("background-image:url(")) {
					// 提取URL部分
					int start = style.indexOf("url(") + 4;  // 从"Url("之后开始
					int end = style.indexOf(")", start);    // 找到右括号的位置
					if (start > 4 && end > start) {
						soImageUrl = style.substring(start, end);
					}
				}
				return soImageUrl;
			}
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败，请稍后重试");
		} catch (Exception e) {
			log.error("搜图失败", e);
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败，请稍后重试");
		}
	}

	public static void main(String[] args) {
		String imageUrl = "https://picture-gallery-1315094248.cos.ap-shanghai.myqcloud.com/space/2015618797690003457/2026-01-26_TJnGsWGXfDDn5ssc.webp";
		String result = getSoImageUrl(imageUrl);
		System.out.println("搜索成功，结果 URL：" + result);
	}
}
