import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.ui.view.Viewer;

// 文本转图处理类
public class Text2Graph {
  // 存储图结构，键为源节点，值为包含目标节点和权重的映射
  private final Map<String, Map<String, Integer>> graph = new HashMap<>();
  // 存储每个节点的入边
  private final Map<String, List<String>> inEdges = new HashMap<>();
  // 存储每个节点的PageRank值
  private Map<String, Double> pageRankMap = new HashMap<>();
  // 全局随机数生成器
  private final SecureRandom random = new SecureRandom();

  // 主函数
  public static void main(String[] args) {
    Text2Graph processor = new Text2Graph();
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    String filePath = "./test/Easy Test.txt";

    // 处理文本文件并计算PageRank
    try {
      processor.processTextFile(filePath);
      processor.computePageRank(0.85, 100, 0.0001);
    } catch (IOException e) {
      System.out.println("Error reading file: " + e.getMessage());
      return;
    }

    // 启动交互式菜单
    while (true) {
      System.out.println("\n=== Function ===");
      System.out.println("1. Show graph");
      System.out.println("2. Bridge words");
      System.out.println("3. Generate text");
      System.out.println("4. Shortest path");
      System.out.println("5. PageRank");
      System.out.println("6. Random walk");
      System.out.println("0. Exit");

      System.out.print("Enter choice: ");
      int choice = scanner.nextInt();
      scanner.nextLine(); // 消耗换行符

      switch (choice) {
        case 1:
          processor.showDirectedGraph(processor.graph);
          break;
        case 2:
          System.out.print("Enter two words: ");
          String[] words = scanner.nextLine().toLowerCase().split("\\s+");
          System.out.println(processor.queryBridgeWords(words[0], words[1]));
          break;
        case 3:
          System.out.print("Enter text: ");
          String inputText = scanner.nextLine();
          System.out.println("New text: " + processor.generateNewText(inputText));
          break;
        case 4:
          System.out.print("Enter two words: ");
          String[] edgeWords = scanner.nextLine().toLowerCase().split("\\s+");
          System.out.println(processor.calcShortestPath(edgeWords[0], edgeWords[1]));
          break;
        case 5:
          System.out.print("Enter word: ");
          String prWord = scanner.nextLine().toLowerCase();
          System.out.printf("PageRank of '%s': %.4f%n", prWord, processor.calPageRank(prWord));
          break;
        case 6:
          System.out.println("Random walk: " + processor.randomWalk());
          break;
        case 0:
          scanner.close();
          System.exit(0);
          break;
        default:
          System.out.println("Invalid choice");
      }
    }
  }

  // 处理文本文件，构建图结构
  void processTextFile(String filePath) throws IOException {
    String content = Files.readString(Path.of(filePath));
    content = content.replaceAll("[^a-zA-Z]", " ").toLowerCase();
    List<String> words = Arrays.stream(content.split("\\s+"))
            .filter(w -> !w.isEmpty())
            .toList();

    for (int i = 0; i < words.size() - 1; i++) {
      String current = words.get(i);
      String next = words.get(i + 1);
      graph.computeIfAbsent(current, k -> new HashMap<>())
              .merge(next, 1, Integer::sum);
      inEdges.computeIfAbsent(next, k -> new ArrayList<>()).add(current);
    }

    Set<String> allNodes = new HashSet<>(words);
    allNodes.forEach(word -> graph.putIfAbsent(word, new HashMap<>()));
  }

  // 使用GraphStream在CLI中展示图结构
  public void showDirectedGraph(Map<String, Map<String, Integer>> dirGraph) {
    dirGraph.forEach((src, edges) -> {
      System.out.print("\n" + src + " → ");
      edges.forEach((dest, weight) -> System.out.print(dest + "(" + weight + ") "));
    });
    System.out.println();

    System.setProperty("org.graphstream.ui", "swing");
    org.graphstream.graph.Graph gsGraph = new SingleGraph("Text Graph", false, true);
    gsGraph.setAttribute("ui.stylesheet",
            "node { "
                    + "   fill-color: #005375; "
                    + "   size: 30px; "
                    + "   text-size: 30px; "
                    + "   text-alignment: above; "
                    + "} "
                    + "edge { "
                    + "   fill-color: #777777; "
                    + "   size: 2px; "
                    + "   shape: line; "
                    + "   arrow-shape: arrow; "
                    + "   arrow-size: 10px, 5px; "
                    + "   text-alignment: along; "
                    + "   text-size: 30px; "
                    + "}");

    Set<String> allNodes = new HashSet<>();
    dirGraph.forEach((src, edges) -> {
      allNodes.add(src);
      allNodes.addAll(edges.keySet());
    });

    SwingUtilities.invokeLater(() -> {
      allNodes.forEach(node -> {
        org.graphstream.graph.Node n = gsGraph.addNode(node);
        n.setAttribute("ui.label", node);
      });
    });

    SwingUtilities.invokeLater(() -> {
      int edgeId = 0;
      for (Map.Entry<String, Map<String, Integer>> entry : dirGraph.entrySet()) {
        String src = entry.getKey();
        Map<String, Integer> edges = entry.getValue();
        for (Map.Entry<String, Integer> edgeEntry : edges.entrySet()) {
          String dest = edgeEntry.getKey();
          Integer weight = edgeEntry.getValue();
          String edgeIdStr = "edge_" + edgeId++;
          org.graphstream.graph.Edge e = gsGraph.addEdge(edgeIdStr, src, dest, true);
          e.setAttribute("ui.label", weight);
        }
      }
    });

    Viewer viewer = gsGraph.display();
    viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

    try {
      FileSinkImages pic = FileSinkImages.createDefault();
      pic.setOutputType(FileSinkImages.OutputType.PNG);
      pic.setLayoutPolicy(FileSinkImages.LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);   // 布局铺满

      // 保存文件
      String filename = "test/graph.png";
      pic.writeAll(gsGraph, filename);
      System.out.println("图片已保存至: " + new File(filename).getAbsolutePath());
    } catch (Exception e) {
      System.err.println("保存图片失败: " + e.getMessage());
    }
  }

  // 查询两个单词之间的桥接词
  public String queryBridgeWords(String word1, String word2) {
    if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
      return "No " + word1 + " or " + word2 + " in the graph!";
    }

    List<String> bridges = graph.get(word1).keySet().stream()
            .filter(word3 -> graph.containsKey(word3) && graph.get(word3).containsKey(word2))
            .collect(Collectors.toList());

    if (bridges.isEmpty()) {
      return "No bridge words from " + word1 + " to " + word2 + "!";
    }
    return "The bridge words from " + word1 + " to " + word2 + " are: " + formatList(bridges) + ".";
  }

  // 格式化列表为字符串
  private String formatList(List<String> list) {
    return list.size() == 1 ? list.get(0) :
          String.join(", ", list.subList(0, list.size() - 1)) + " and " + list.get(list.size() - 1);
  }

  // 生成新文本，插入桥接词
  public String generateNewText(String inputText) {
    List<String> words = Arrays.stream(inputText.split("[^a-zA-Z]+"))
            .filter(w -> !w.isEmpty())
            .map(String::toLowerCase)
            .toList();
    if (words.size() < 2) {
      return inputText;
    }

    List<String> result = new ArrayList<>();
    result.add(words.get(0));
    for (int i = 0; i < words.size() - 1; i++) {
      String current = words.get(i);
      String next = words.get(i + 1);
      List<String> bridges = getBridgeWords(current, next);
      if (!bridges.isEmpty()) {
        result.add(bridges.get(random.nextInt(bridges.size())));
      }
      result.add(next);
    }
    return String.join(" ", result);
  }

  // 获取两个单词之间的桥接词
  private List<String> getBridgeWords(String word1, String word2) {
    if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
      return new ArrayList<>();
    }
    return graph.get(word1).keySet().stream()
            .filter(word3 -> graph.get(word3).containsKey(word2))
            .collect(Collectors.toList());
  }

  // 计算两个单词之间的最短路径
  public String calcShortestPath(String word1, String word2) {
    if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
      return "No " + word1 + " or " + word2 + " in the graph!";
    }

    Map<String, Integer> dist = new HashMap<>();
    final Map<String, String> prev = new HashMap<>();
    PriorityQueue<Node> pq = new PriorityQueue<>();
    graph.keySet().forEach(k -> dist.put(k, Integer.MAX_VALUE));
    dist.put(word1, 0);
    pq.add(new Node(word1, 0));

    while (!pq.isEmpty()) {
      Node curr = pq.poll();
      if (curr.node.equals(word2)) {
        break;
      }
      graph.getOrDefault(curr.node, Collections.emptyMap())
              .forEach((neighbor, weight) -> {
                int alt = curr.dist + weight;
                if (alt < dist.get(neighbor)) {
                  dist.put(neighbor, alt);
                  prev.put(neighbor, curr.node);
                  pq.add(new Node(neighbor, alt));
                }
              });
    }

    if (dist.get(word2) == Integer.MAX_VALUE) {
      return "No path from " + word1 + " to " + word2 + ".";
    }
    List<String> path = new ArrayList<>();
    for (String at = word2; at != null; at = prev.get(at)) {
      path.add(at);
    }
    Collections.reverse(path);
    return "Path: " + String.join(" → ", path) + " (Length: " + dist.get(word2) + ")";
  }

  // 计算PageRank值
  private void computePageRank(double d, int maxIter, double threshold) {
    int n = graph.size();
    if (n == 0) {
      return;
    }

    graph.keySet().forEach(k -> pageRankMap.put(k, 1.0 / n));
    for (int iter = 0; iter < maxIter; iter++) {
      Map<String, Double> newPageRank = new HashMap<>();

      double sinkPageRank = graph.keySet().stream()
              .filter(node -> graph.get(node).isEmpty())
              .mapToDouble(node -> pageRankMap.get(node))
              .sum();

      graph.keySet().forEach(node -> {
        double pr = (1 - d) / n;

        for (String in : inEdges.getOrDefault(node, Collections.emptyList())) {
          int outDegree = graph.get(in).size();
          if (outDegree > 0) {
            pr += d * pageRankMap.get(in) / outDegree;
          }
        }
        pr += d * sinkPageRank / n;

        newPageRank.put(node, pr);
      });

      boolean converged = true;
      for (String node : graph.keySet()) {
        double delta = Math.abs(newPageRank.get(node) - pageRankMap.get(node));
        if (delta > threshold) {
          converged = false;
          break;
        }
      }
      pageRankMap = new HashMap<>(newPageRank);

      if (converged) {
        break;
      }
    }
  }

  // 获取单词的PageRank值
  public double calPageRank(String word) {
    return pageRankMap.getOrDefault(word, 0.0);
  }

  // 随机游走
  public String randomWalk() {
    List<String> path = new ArrayList<>();
    if (graph.isEmpty()) {
      return "";
    }
    List<String> nodes = new ArrayList<>(graph.keySet());
    String current = nodes.get(random.nextInt(nodes.size()));
    Set<String> visitedEdges = new HashSet<>();

    while (true) {
      path.add(current);
      Map<String, Integer> edges = graph.get(current);
      if (edges == null || edges.isEmpty()) {
        break;
      }

      int total = edges.values().stream().mapToInt(i -> i).sum();
      int rand = random.nextInt(total);
      int sum = 0;
      String next = null;
      for (var e : edges.entrySet()) {
        sum += e.getValue();
        if (rand < sum) {
          next = e.getKey();
          break;
        }
      }
      if (next == null) {
        break;
      }
      String edge = current + " → " + next;
      if (visitedEdges.contains(edge)) {
        break;
      }
      visitedEdges.add(edge);
      current = next;
    }

    try {
      Files.write(Paths.get("random_walk.txt"),
              String.join(" ", path).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return String.join(" ", path);
  }

  // 辅助类，用于优先队列
  static class Node implements Comparable<Node> {
    String node;
    int dist;

    Node(String n, int d) {
      node = n;
      dist = d;
    }

    public int compareTo(Node other) {
      return Integer.compare(dist, other.dist);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Node other = (Node) obj;
      return dist == other.dist && node.equals(other.node);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dist, node);
    }
  }
}