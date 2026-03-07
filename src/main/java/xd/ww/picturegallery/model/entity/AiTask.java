package xd.ww.picturegallery.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName ai_task
 */
@TableName(value ="ai_task")
@Data
public class AiTask {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务类型: OUT_PAINTING / TAGGING
     */
    private String taskType;

    /**
     * 状态: 0-等待, 1-执行中, 2-成功, 3-失败
     */
    private Integer status;

    /**
     * 关联图片ID
     */
    private Long pictureId;

    /**
     * 输入参数(JSON)
     */
    private String inputParams;

    /**
     * 输出结果(URL或标签JSON)
     */
    private String outputData;

    /**
     * 外部AI平台任务ID(如阿里云taskId)
     */
    private String outTaskId;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}