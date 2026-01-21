package xd.ww.picturegallery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("xd.ww.picturegallery.mapper")
public class PictureGalleryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureGalleryApplication.class, args);
    }

}
