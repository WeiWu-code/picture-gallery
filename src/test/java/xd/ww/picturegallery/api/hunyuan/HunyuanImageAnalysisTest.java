package xd.ww.picturegallery.api.hunyuan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import xd.ww.picturegallery.api.hunyuan.model.ImageAnalysisResult;

import javax.annotation.Resource;

@SpringBootTest
class HunyuanImageAnalysisTest {

    @Resource
    private HunyuanImageAnalysis hunyuanImageAnalysis;

    @Test
    void analyzeImage() {
        ImageAnalysisResult imageAnalysisResult = hunyuanImageAnalysis.analyzeImage("https://spicture-gallery-1315094248.cos.ap-guangzhou.myqcloud.com/public/2014964419015958529/2026-01-31_373MnvEnx9XkfN5s.webp?q-sign-algorithm=sha1&q-ak=AKIDW36RneGCHJ4Ci83-lvPZ3bremPKJ4IE3dOsDDDih7XmMUpQT868IucMvTh_nLsZh&q-sign-time=1770542088;1770545688&q-key-time=1770542088;1770545688&q-header-list=host&q-url-param-list=&q-signature=68b2b6bc1ca8de8802132980756e7fa5c69f0fef&x-cos-security-token=55S74TJxnbtKiy1qBRi4mY04QbjG67Sae5e5729e657eeb7e9a70d7c3736cbfe9mudK1HSWQKdw2xNOQrWKRQpnaDFGB02wGxuFZo30n68TwHxLgk8KUb9IB8VvGXGww73mVh-I1xQcQ1YfQpPp1Lhdi_b1n4fozB62EJVxthLTb6fS09BHYJ9cosXFEJ21Gt5XAmq5NPGZk48sYi6YmtrZV_suyzqcGMo5fUplh5JUAmr9yV0eRpf39YNpTRCQefUL_MprTYCPq8e58xh2rClx6USa495FmkqO_tlYCYQVFknnHRxyLF1HrJeQTjrCrdFYNDQ0jBLqMvPNB0YD0Q&");
        Assertions.assertNotNull(imageAnalysisResult);
    }
}