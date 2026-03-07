package xd.ww.picturegallery.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTaskMessage implements Serializable {
    private Long aiTaskId;    // 本地任务表ID
    private Long pictureId;   // 图片ID
    private static final long serialVersionUID = 1L;
}