package com.study.assisiant.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParseServiceTest {

    @Test
    void emptyAndNullInputProduceNoChunks() {
        assertTrue(DocumentParseService.chunkText(null, 100, 10).isEmpty());
        assertTrue(DocumentParseService.chunkText("   \n  ", 100, 10).isEmpty());
    }

    @Test
    void shortTextStaysInOneChunk() {
        List<String> chunks =
                DocumentParseService.chunkText("很短的一段文字。", 100, 10);
        assertEquals(1, chunks.size());
        assertEquals("很短的一段文字。", chunks.get(0));
    }

    @Test
    void longTextIsSplitAndOverlapped() {
        String text = "句子一。".repeat(60);

        List<String> chunks = DocumentParseService.chunkText(text, 50, 10);

        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50, "分片长度不应超过窗口大小");
        }
        // 相邻分片之间应当有重叠，保证语义不被切断
        String first = chunks.get(0);
        String second = chunks.get(1);
        assertTrue(second.length() > 0 && first.length() > 0);
    }

    @Test
    void businessTextIsSplitOnSentenceBoundary() {
        String text = "第一句话讲的是数据库索引的原理。"
                + "第二句话继续展开B+树的结构细节。"
                + "第三句话说明索引的维护成本。";

        List<String> chunks = DocumentParseService.chunkText(text, 25, 5);

        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 25);
        }
        assertTrue(chunks.stream().anyMatch(c -> c.endsWith("。")),
                "应当尽量在句号处断开");
    }

    @Test
    void extensionIsParsedCaseInsensitively() {
        assertEquals("pdf", DocumentParseService.extensionOf("A.PDF"));
        assertEquals("docx", DocumentParseService.extensionOf("笔记.docx"));
        assertEquals("", DocumentParseService.extensionOf("没有后缀"));
        assertEquals("", DocumentParseService.extensionOf(null));
    }
}
