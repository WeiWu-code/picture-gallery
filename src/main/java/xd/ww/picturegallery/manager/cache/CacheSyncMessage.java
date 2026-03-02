package xd.ww.picturegallery.manager.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheSyncMessage {
    // 消息发送者的节点ID
    private String instanceId;
    /**
     * 操作类型，例如 "DELETE", "CLEAR_ALL"
     */
    private String actionType;

    // 涉及的缓存键
    private List<String> keys;
}