package xd.ww.picturegallery.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import xd.ww.picturegallery.api.aliyunai.AliYunAiApi;
import xd.ww.picturegallery.api.aliyunai.model.GetOutPaintingTaskResponse;
import xd.ww.picturegallery.api.hunyuan.HunyuanImageAnalysis;
import xd.ww.picturegallery.api.hunyuan.model.ImageAnalysisResult;
import xd.ww.picturegallery.model.entity.AiTask;
import xd.ww.picturegallery.model.entity.Picture;
import xd.ww.picturegallery.service.AiTaskService;
import xd.ww.picturegallery.service.PictureService;

import javax.annotation.Resource;

@Component
@Slf4j
public class AiTaskConsumer {

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private HunyuanImageAnalysis hunyuanImageAnalysis;
    @Resource
    private AiTaskService aiTaskService; // 指向你的 AiTaskService
    @Resource
    private PictureService pictureService;

    // 1. 处理扩图（轮询外部接口）
    @RabbitListener(queues = RabbitMqConfig.OUT_PAINTING_QUEUE)
    public void handleOutPainting(AiTaskMessage message) {
        Long aiTaskId = message.getAiTaskId();
        AiTask task = aiTaskService.getById(aiTaskId);
        if (task == null) return;

        // 更新为执行中
        aiTaskService.lambdaUpdate()
                .eq(AiTask::getId, aiTaskId)
                .set(AiTask::getStatus, 1) // 1-执行中
                .update();

        try {
            for (int i = 0; i < 20; i++) {
                GetOutPaintingTaskResponse res = aliYunAiApi.getOutPaintingTask(task.getOutTaskId());
                String status = res.getOutput().getTaskStatus();

                if ("SUCCEEDED".equals(status)) {
                    String imageUrl = res.getOutput().getOutputImageUrl();
                    // 1. 更新图片表
                    pictureService.lambdaUpdate()
                            .eq(Picture::getId, message.getPictureId())
                            .set(Picture::getUrl, imageUrl)
                            .update();
                    // 2. 更新任务表成功
                    aiTaskService.lambdaUpdate()
                            .eq(AiTask::getId, aiTaskId)
                            .set(AiTask::getStatus, 2) // 2-成功
                            .set(AiTask::getOutputData, imageUrl)
                            .update();
                    return;
                } else if ("FAILED".equals(status)) {
                    throw new RuntimeException("阿里云扩图任务失败");
                }
                Thread.sleep(3000); // 间隔轮询
            }
            throw new RuntimeException("轮询超时");
        } catch (Exception e) {
            log.error("扩图任务异常", e);
            aiTaskService.lambdaUpdate()
                    .eq(AiTask::getId, aiTaskId)
                    .set(AiTask::getStatus, 3) // 3-失败
                    .set(AiTask::getErrorMessage, e.getMessage())
                    .update();
        }
    }

    // 2. 处理打标签
    @RabbitListener(queues = RabbitMqConfig.TAGGING_QUEUE)
    public void handleTagging(AiTaskMessage message) {
        Long aiTaskId = message.getAiTaskId();
        Long pictureId = message.getPictureId();
        try {
            // 1. 更新任务为执行中
            aiTaskService.lambdaUpdate().eq(AiTask::getId, aiTaskId).set(AiTask::getStatus, 1).update();

            // 2. 获取当前图片最新数据，判断是否真的需要 AI 分析
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) return;

            // 检查哪些字段是空的（需要 AI 填充）
            boolean needTags = StrUtil.isBlank(picture.getTags()) || "[]".equals(picture.getTags());
            boolean needCategory = StrUtil.isBlank(picture.getCategory());
            boolean needIntroduction = StrUtil.isBlank(picture.getIntroduction());

            // 如果所有字段都已经有值了，直接跳过，标记任务成功
            if (!needTags && !needCategory && !needIntroduction) {
                aiTaskService.lambdaUpdate()
                        .eq(AiTask::getId, aiTaskId)
                        .set(AiTask::getStatus, 2)
                        .set(AiTask::getOutputData, "检测到字段已有值，跳过 AI 覆盖")
                        .update();
                return;
            }

            // 3. 调用 AI 进行分析
            ImageAnalysisResult result = hunyuanImageAnalysis.analyzeImage(picture.getUrl());

            // 4. 条件更新图片表（双重保险：在 SQL 层面再次判断是否为 NULL）
            // MyBatis-Plus 的 set(condition, column, value) 只有 condition 为 true 时才会拼接该字段
            pictureService.lambdaUpdate()
                    .eq(Picture::getId, pictureId)
                    // 只有当数据库里的字段确实为空时，才更新为 AI 的结果
                    .set(needTags, Picture::getTags, JSONUtil.toJsonStr(result.getTags()))
                    .set(needCategory, Picture::getCategory, result.getCategory())
                    .set(needIntroduction, Picture::getIntroduction, result.getDescription())
                    // 额外保险：可以在 SQL 增加 AND (tags IS NULL OR tags = '') 逻辑
                    .update();

            // 5. 更新任务表为成功
            aiTaskService.lambdaUpdate()
                    .eq(AiTask::getId, aiTaskId)
                    .set(AiTask::getStatus, 2)
                    .set(AiTask::getOutputData, JSONUtil.toJsonStr(result))
                    .update();

            log.info("AI 打标签完成，图片ID: {}", pictureId);
        } catch (Exception e) {
            log.error("打标签任务异常，图片ID: {}", pictureId, e);
            aiTaskService.lambdaUpdate()
                    .eq(AiTask::getId, aiTaskId)
                    .set(AiTask::getStatus, 3)
                    .set(AiTask::getErrorMessage, e.getMessage())
                    .update();
        }
    }
}