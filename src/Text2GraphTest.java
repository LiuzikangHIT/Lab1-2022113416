import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

class Text2GraphWhiteTest {
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

//  // 测试用例1：单词不存在（覆盖无效输入）
//  @Test
//  void testCase1_WordsNotInGraph() {
//    String result = processor.calcShortestPath("and", "world");
//    assertEquals("No and or world in the graph!", result);
//  }
//
//  // 测试用例2：路径不存在（覆盖无连通路径）
//  @Test
//  void testCase2_NoPathExists() {
//    String result = processor.calcShortestPath("again", "analyzed");
//    assertEquals("No path from again to analyzed.", result);
//  }
//
//  // 测试用例3：路径不存在（反向检查）
//  @Test
//  void testCase3_NoPathReverse() {
//    String result = processor.calcShortestPath("it", "analyzed");
//    assertEquals("No path from it to analyzed.", result);
//  }
//
//  // 测试用例4：路径不存在（反向检查）
//  @Test
//  void testCase4_NoPathReverse() {
//    String result = processor.calcShortestPath("it", "shared");
//    assertEquals("No path from it to shared.", result);
//  }
//
//  // 测试用例5：存在最短路径
//  @Test
//  void testCase5_ShortestPathExists() {
//    String result = processor.calcShortestPath("report", "the");
//    assertEquals("Path: report → with → the (Length: 2)", result);
//  }
//
//  // 测试用例6：存在最短路径（单边路径）
//  @Test
//  void testCase6_SingleEdgePath() {
//    String result = processor.calcShortestPath("more", "data");
//    assertEquals("Path: more → data (Length: 1)", result);
//  }

  // 测试用例7：多边路径（验证复杂路径计算）
  @Test
  void testCase7_MultiEdgePath() {
    String result = processor.calcShortestPath("the", "detailed");
    assertEquals("Path: the → data → wrote → a → detailed (Length: 4)", result);
  }
}