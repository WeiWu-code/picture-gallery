package xd.ww.picturegallery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("xd.ww.picturegallery.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class PictureGalleryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureGalleryApplication.class, args);
    }

}
