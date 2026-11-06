package com.ape.marathon.ai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 助手结构化输出：自然语言回答 + 引用的赛事实体列表（面试可演示 entity 输出）
 */
public class EventAssistantReply {

    /** 面向用户的中文回答 */
    private String answer;

    /** 回答所依据的赛事（来自 Tool 查询结果，勿编造） */
    private List<EventCard> events = new ArrayList<>();

    /** 是否已调用过赛事查询工具拿真实数据 */
    private boolean grounded;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<EventCard> getEvents() {
        return events;
    }

    public void setEvents(List<EventCard> events) {
        this.events = events;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }
}
