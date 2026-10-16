package com.study.assisiant.common;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 极简 SSE 写入器：把事件按 {@code event: xxx\ndata: {...}\n\n} 格式写出。
 *
 * <p>数据统一用 JSON 单行序列化，避免换行破坏 SSE 帧结构。</p>
 */
public class SseWriter {

    private static final Logger log =
            LoggerFactory.getLogger(SseWriter.class);

    private final OutputStream out;

    public SseWriter(OutputStream out) {
        this.out = out;
    }

    public void event(String name, Object data) {
        String payload = data == null
                ? "{}" : JSONObject.toJSONString(data);
        String frame = "event: " + name + "\n"
                + "data: " + payload.replace("\n", "\\n") + "\n\n";
        try {
            out.write(frame.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            // 客户端提前断开是常见情况，记录后交给上层结束流程
            throw new SseClosedException(e);
        }
    }

    /** 客户端断开连接 */
    public static class SseClosedException extends RuntimeException {
        public SseClosedException(Throwable cause) {
            super("SSE 连接已关闭", cause);
        }
    }

    public static void safeClose(OutputStream out) {
        try {
            out.close();
        } catch (IOException e) {
            log.debug("关闭流失败", e);
        }
    }
}
