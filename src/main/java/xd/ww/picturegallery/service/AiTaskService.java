package xd.ww.picturegallery.service;

import xd.ww.picturegallery.model.entity.AiTask;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 35171
* @description 针对表【ai_task】的数据库操作Service
* @createDate 2026-03-07 21:35:19
*/
public interface AiTaskService extends IService<AiTask> {

    // 发起扩图任务
    Long publishOutPainting(Long pictureId, String aliyunTaskId, Long userId);

    // 发起打标签任务
    void publishTagging(Long pictureId, Long userId);
}
