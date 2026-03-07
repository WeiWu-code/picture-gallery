package xd.ww.picturegallery.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import xd.ww.picturegallery.model.entity.AiTask;
import xd.ww.picturegallery.mq.AiTaskMessage;
import xd.ww.picturegallery.service.AiTaskService;
import xd.ww.picturegallery.mapper.AiTaskMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static xd.ww.picturegallery.mq.RabbitMqConfig.*;

/**
* @author 35171
* @description 针对表【ai_task】的数据库操作Service实现
* @createDate 2026-03-07 21:35:19
*/
@Service
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask>
    implements AiTaskService{


    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private AiTaskMapper aiTaskMapper;

    @Override
    public Long publishOutPainting(Long pictureId, String aliyunTaskId, Long userId) {
        AiTask task = new AiTask();
        task.setTaskType("OUT_PAINTING");
        task.setPictureId(pictureId);
        task.setOutTaskId(aliyunTaskId);
        task.setUserId(userId);
        aiTaskMapper.insert(task);
        rabbitTemplate.convertAndSend(AI_EXCHANGE, OUT_PAINTING_KEY, new AiTaskMessage(task.getId(), pictureId));
        return task.getId();
    }

    @Override
    public void publishTagging(Long pictureId, Long userId) {
        AiTask task = new AiTask();
        task.setTaskType("TAGGING");
        task.setPictureId(pictureId);
        task.setUserId(userId);
        aiTaskMapper.insert(task);
        rabbitTemplate.convertAndSend(AI_EXCHANGE, TAGGING_KEY, new AiTaskMessage(task.getId(), pictureId));
    }

}




