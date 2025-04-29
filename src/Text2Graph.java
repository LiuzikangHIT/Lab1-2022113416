import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;

import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;

// 文本转图处理类
public class Text2Graph {
    // 存储图结构，键为源节点，值为包含目标节点和权重的映射
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    // 存储每个节点的入边
    private Map<String, List<String>> inEdges = new HashMap<>();
    // 存储每个节点的PageRank值
    private Map<String, Double> pageRankMap = new HashMap<>();

    // 主函数
    public static void main(String[] args) {
        Text2Graph processor = new Text2Graph();
        Scanner scanner = new Scanner(System.in);
        String filePath = "./test/test.txt";

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
            System.out.println("\nChoose function:\n1. Show graph\n2. Bridge words\n3. Generate text\n4. Shortest path\n5. PageRank\n6. Random walk\n0. Exit");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    processor.showDirectedGraphByCLI();
                    break;
                case 2:
                    System.out.println("Enter two words:");
                    String[] words = scanner.nextLine().split("\\s+");
                    System.out.println(processor.queryBridgeWords(words[0], words[1]));
                    break;
                case 3:
                    System.out.println("Enter text:");
                    String text = scanner.nextLine();
                    System.out.println("New text: " + processor.generateNewText(text));
                    break;
                case 4:
                    System.out.println("Enter two words:");
                    String[] ws = scanner.nextLine().split("\\s+");
                    System.out.println(processor.calcShortestPath(ws[0], ws[1]));
                    break;
                case 5:
                    System.out.println("Enter word:");
                    String w = scanner.nextLine();
                    System.out.printf("PageRank: %.4f\n", processor.calPageRank(w));
                    break;
                case 6:
                    System.out.println("Random walk: " + processor.randomWalk());
                    break;
                case 0:
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    // 处理文本文件，构建图结构
    private void processTextFile(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        content = content.replaceAll("[^a-zA-Z]", " ").toLowerCase();
        List<String> words = Arrays.stream(content.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toList());

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

    // 在命令行展示图结构
    public void showDirectedGraphByCLI() {
        graph.forEach((src, edges) -> {
            System.out.print("\n" + src + " -> ");
            edges.forEach((dest, weight) -> System.out.print(dest + "(" + weight + ") "));
        });
        System.out.println();
    }

    // 使用GraphStream在GUI中展示图结构
    public void showDirectedGraphByGUI() {
        System.setProperty("org.graphstream.ui", "swing");
        org.graphstream.graph.Graph gsGraph = new SingleGraph("Text Graph");
        gsGraph.setAttribute("ui.stylesheet",
                "node { fill-color: #A5F5A5; size: 30px; text-alignment: above; }" +
                        "edge { text-alignment: along; }");

        Set<String> allNodes = new HashSet<>();
        graph.forEach((src, edges) -> {
            allNodes.add(src);
            allNodes.addAll(edges.keySet());
        });

        allNodes.forEach(node -> {
            org.graphstream.graph.Node n = gsGraph.addNode(node);
            n.setAttribute("ui.label", node);
        });

        int edgeId = 0;
        for (String src : graph.keySet()) {
            Map<String, Integer> edges = graph.get(src);
            for (String dest : edges.keySet()) {
                String edge = src + dest + (edgeId++);
                org.graphstream.graph.Edge e = gsGraph.addEdge(edge, src, dest, true);
                e.setAttribute("ui.label", edges.get(dest));
            }
        }

        gsGraph.setAttribute("ui.quality");
        gsGraph.setAttribute("ui.antialias");
        gsGraph.display();
    }

    // 查询两个单词之间的桥接词
    public String queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }

        String finalWord = word2;
        List<String> bridges = graph.get(word1).keySet().stream()
                .filter(w3 -> graph.containsKey(w3) && graph.get(w3).containsKey(finalWord))
                .collect(Collectors.toList());

        if (bridges.isEmpty()) return "No bridge words from " + word1 + " to " + word2 + "!";
        return "The bridge words are: " + formatList(bridges);
    }

    // 格式化列表为字符串
    private String formatList(List<String> list) {
        return list.size() == 1 ? list.get(0) :
                String.join(", ", list.subList(0, list.size()-1)) + " and " + list.get(list.size()-1);
    }

    // 生成新文本，插入桥接词
    public String generateNewText(String inputText) {
        List<String> words = Arrays.stream(inputText.split("[^a-zA-Z]+"))
                .filter(w -> !w.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        if (words.size() < 2) return inputText;

        List<String> result = new ArrayList<>();
        result.add(words.get(0));
        for (int i = 0; i < words.size()-1; i++) {
            String current = words.get(i);
            String next = words.get(i+1);
            List<String> bridges = getBridgeWords(current, next);
            if (!bridges.isEmpty())
                result.add(bridges.get(new Random().nextInt(bridges.size())));
            result.add(next);
        }
        return String.join(" ", result);
    }

    // 获取两个单词之间的桥接词
    private List<String> getBridgeWords(String w1, String w2) {
        if (!graph.containsKey(w1) || !graph.containsKey(w2)) return new ArrayList<>();
        return graph.get(w1).keySet().stream()
                .filter(w3 -> graph.get(w3).containsKey(w2))
                .collect(Collectors.toList());
    }

    // 计算两个单词之间的最短路径
    public String calcShortestPath(String w1, String w2) {
        w1 = w1.toLowerCase();
        w2 = w2.toLowerCase();
        if (!graph.containsKey(w1) || !graph.containsKey(w2))
            return "Word not found";

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();
        graph.keySet().forEach(k -> dist.put(k, Integer.MAX_VALUE));
        dist.put(w1, 0);
        pq.add(new Node(w1, 0));

        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            if (curr.node.equals(w2)) break;
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

        if (dist.get(w2) == Integer.MAX_VALUE) return "No path";
        List<String> path = new ArrayList<>();
        for (String at = w2; at != null; at = prev.get(at)) path.add(at);
        Collections.reverse(path);
        return "Path: " + String.join("→", path) + " Length: " + dist.get(w2);
    }

    // 计算PageRank值
    private void computePageRank(double d, int maxIter, double threshold) {
        int N = graph.size();
        if (N == 0) return;

        graph.keySet().forEach(k -> pageRankMap.put(k, 1.0 / N));
        for (int iter = 0; iter < maxIter; iter++) {
            Map<String, Double> newPR = new HashMap<>();

            double sinkPR = graph.keySet().stream()
                    .filter(node -> graph.get(node).isEmpty())
                    .mapToDouble(node -> pageRankMap.get(node))
                    .sum();

            graph.keySet().forEach(node -> {
                double pr = (1 - d) / N;

                for (String in : inEdges.getOrDefault(node, Collections.emptyList())) {
                    int outDegree = graph.get(in).size();
                    if (outDegree > 0) {
                        pr += d * pageRankMap.get(in) / outDegree;
                    }
                }
                pr += d * sinkPR / N;

                newPR.put(node, pr);
            });

            boolean converged = true;
            for (String node : graph.keySet()) {
                double delta = Math.abs(newPR.get(node) - pageRankMap.get(node));
                if (delta > threshold) {
                    converged = false;
                    break;
                }
            }
            pageRankMap = new HashMap<>(newPR);

            if (converged) break;
        }
    }

    // 获取单词的PageRank值
    public double calPageRank(String word) {
        return pageRankMap.getOrDefault(word.toLowerCase(), 0.0);
    }

    // 随机游走
    public String randomWalk() {
        List<String> path = new ArrayList<>();
        if (graph.isEmpty()) return "";
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(new Random().nextInt(nodes.size()));
        Set<String> visitedEdges = new HashSet<>();

        while (true) {
            path.add(current);
            Map<String, Integer> edges = graph.get(current);
            if (edges == null || edges.isEmpty()) break;

            int total = edges.values().stream().mapToInt(i -> i).sum();
            int rand = new Random().nextInt(total);
            int sum = 0;
            String next = null;
            for (var e : edges.entrySet()) {
                sum += e.getValue();
                if (rand < sum) {
                    next = e.getKey();
                    break;
                }
            }
            if (next == null) break;
            String edge = current + "->" + next;
            if (visitedEdges.contains(edge)) break;
            visitedEdges.add(edge);
            current = next;
        }

        try {
            Files.write(Paths.get("random_walk.txt"), String.join(" ", path).getBytes());
        } catch (IOException e) { e.printStackTrace(); }
        return String.join(" ", path);
    }

    // 辅助类，用于优先队列
    static class Node implements Comparable<Node> {
        String node;
        int dist;
        Node(String n, int d) { node = n; dist = d; }
        public int compareTo(Node other) { return Integer.compare(dist, other.dist); }
    }
}