package xd.ww.picturegallery.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 热门tag的列表
     */
    private List<String> tagList;

    /**
     * 热门分类
     */
    private List<String> categoryList;
}
