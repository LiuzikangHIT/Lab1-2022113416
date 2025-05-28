import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

class Text2GraphBlackTest {
    private Text2Graph processor;

    @BeforeEach
    void setUp() {
        processor = new Text2Graph();
        String filePath = "./test/Easy Test.txt";

        // 处理文本文件并构建有向图
        try {
            processor.processTextFile(filePath);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // 测试用例1：存在桥接词（覆盖等价类1）
    @Test
    void testCase1_BridgeWordExists() {
        String result = processor.queryBridgeWords("the", "carefully");
        assertEquals("The bridge words from the to carefully are: scientist.", result);
    }

    // 测试用例2：无桥接词（覆盖等价类2）
    @Test
    void testCase2_NoBridgeWords() {
        String result = processor.queryBridgeWords("with", "shared");
        assertEquals("No bridge words from with to shared!", result);
    }

    // 测试用例3：单词不存在（覆盖等价类3）
    @Test
    void testCase3_WordNotInGraph() {
        String result = processor.queryBridgeWords("of", "report");
        assertEquals("No of or report in the graph!", result);
    }

    // 测试用例4：特殊字符输入（覆盖等价类3）
    @Test
    void testCase4_InvalidCharacters() {
        String result = processor.queryBridgeWords("@", "!");
        assertEquals("No @ or ! in the graph!", result);
    }
}