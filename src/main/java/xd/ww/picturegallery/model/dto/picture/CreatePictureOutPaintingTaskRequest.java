package xd.ww.picturegallery.model.dto.picture;

import lombok.Data;
import xd.ww.picturegallery.api.aliyunai.model.CreateOutPaintingTaskRequest;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}

