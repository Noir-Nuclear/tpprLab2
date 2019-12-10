import javafx.fxml.Initializable;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import loading.TXTLoader;
import math.Alpha;
import math.MathUtils;
import matrixUtils.Utils;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller implements Initializable {
    public Integer ruleSize = 3;
    public List<List<Double>> rows;
    public List<List<Double>> C;
    public List<List<Double>> S;
    public List<List<Double>> G;
    public List<Alpha> alphas;
    public TextField teachError;
    public TextField testError;
    double bestError = Double.MAX_VALUE;
    public Button refreshChart;
    public ScatterChart graphic;
    public TextArea SField;
    public TextArea CField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TXTLoader loader = new TXTLoader("D:\\MyFolder\\Projects\\Java\\tpprLab2\\src\\main\\resources\\txt\\data.txt");
        rows = loader.loadRows();
//        rows = MathUtils.normalizeData(loader.loadRows());
        loader = new TXTLoader("D:\\MyFolder\\Projects\\Java\\tpprLab2\\src\\main\\resources\\txt\\C.txt");
        C = loader.loadC();
        loader = new TXTLoader("D:\\MyFolder\\Projects\\Java\\tpprLab2\\src\\main\\resources\\txt\\S.txt");
        S = loader.loadC();
        fillField(SField, S);
        fillField(CField, C);
//        prepareCoeff();
        renderChart();
        refreshChart.setOnAction(e -> renderChart());
    }

    public void prepareCoeff() {
        S = getFromField(SField);
        C = getFromField(CField);
        List<List<Double>> BestC = C;
        List<List<Double>> BestS = S;
        Set<Integer> numbers = getRandomRows(rows.size(), 0.75);
        for (int generation = 0; generation < 60000; generation++) {
            G = generateG(numbers);
            AtomicReference<Double> error = new AtomicReference<>(0.0);
            double[][] p = generateCoefficients(Utils.listToArray(G),
                    Utils.listToArray(
                            numbers.stream().map(i -> rows.get(i)).
                                    map(row -> Collections.singletonList(row.get(0))).
                                    collect(Collectors.toList())
                    ));
            numbers.forEach(number -> {
                List<Double> row = rows.get(number);
                double y = getResult(number, p);
                error.updateAndGet(v -> v + Math.pow(row.get(0) - y, 2.0));
            });
            error.updateAndGet(Math::sqrt);
            if (bestError > error.get()) {
                bestError = error.get();
                BestC = C;
                BestS = S;
            }
            C = getRandomList(3, 2);
            S = getRandomList(3, 50);
        }
        C = BestC;
        S = BestS;
        fillField(SField, S);
        fillField(CField, C);
    }

    public List<List<Double>> getRandomList(int size, int dispertion) {
        List<List<Double>> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            List<Double> row = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                row.add(Math.random() * dispertion - dispertion / 2);
            }
            rows.add(row);
        }
        return rows;
    }

    public void fillField(TextArea field, List<List<Double>> cells) {
        String text = "";
        for (int i = 0; i < cells.size(); i++) {
            text += String.join("; ", cells.get(i).stream().map(Object::toString).collect(Collectors.toList())) + "\n";
        }
        field.setText(text);
    }

    public List<List<Double>> getFromField(TextArea field) {
        List<List<Double>> cells = new ArrayList<>();
        String text = field.getText();
        Arrays.stream(text.split("\n")).forEach(row ->
                cells.add(
                        Arrays.stream(row.split("; ")).
                                map(Double::parseDouble).
                                collect(Collectors.toList())
                ));
        field.setText(text);
        return cells;
    }

    public void renderChart() {
        alphas = new ArrayList<>();
        S = getFromField(SField);
        C = getFromField(CField);
        Set<Integer> numbers = getRandomRows(rows.size(), 0.75);
        G = generateG(numbers);
        double[][] p = generateCoefficients(Utils.listToArray(G),
                Utils.listToArray(
                        numbers.stream().map(i -> rows.get(i)).
                                map(row -> Collections.singletonList(row.get(0))).
                                collect(Collectors.toList())
                ));
        XYChart.Series series1 = new XYChart.Series();
        series1.setName("Обучающая выборка");
        XYChart.Series series2 = new XYChart.Series();
        series2.setName("Тестовая выборка");
        XYChart.Series series3 = new XYChart.Series();
        series3.setName("Предпологаемая функция");
        AtomicReference<Double> teach = new AtomicReference<>(0.0);
        numbers.forEach(number -> {
            List<Double> row = rows.get(number);
            double y = getResult(number, p);
            series1.getData().add(new XYChart.Data(row.get(0), y));
            teach.updateAndGet(v -> v + Math.pow(row.get(0) - y, 2.0));
            series3.getData().add(new XYChart.Data(row.get(0), row.get(0)));
        });
        teach.updateAndGet(v -> Math.sqrt(v / numbers.size()));
        teachError.setText(teach.get().toString());
        AtomicReference<Double> test = new AtomicReference<>(0.0);
        IntStream.range(0, rows.size()).
                filter(i -> !numbers.contains(i)).
                forEach(number -> {
                    List<Double> row = rows.get(number);
                    double y = getResult(number, p);
                    series2.getData().add(new XYChart.Data(row.get(0), y));
                    test.updateAndGet(v -> v + Math.pow(row.get(0) - y, 2.0));
                    series3.getData().add(new XYChart.Data(row.get(0), row.get(0)));
                });
        test.updateAndGet(v -> Math.sqrt(v / (rows.size() - numbers.size())));
        testError.setText(test.get().toString());
        graphic.getData().clear();
        graphic.getData().add(series1);
        graphic.getData().add(series2);
        graphic.getData().add(series3);
    }

    public Double getResult(Integer number, double[][] p) {
        List<Double> row = rows.get(number);
        Alpha alpha = generateAlpha(row);
        return IntStream.range(0, alpha.values.size()).mapToDouble(i -> {
            Double sum = IntStream.range(1, row.size()).mapToDouble(j ->
                    row.get(j) * p[i * row.size() + j][0]
            ).sum();
            return alpha.values.get(i) * (sum + p[i][0]) / alpha.middle;
        }).sum();
    }

    public List<List<Double>> generateG(Set<Integer> numbers) {
        List<List<Double>> newG = new ArrayList<>();
        numbers.forEach(number -> {
            List<Double> row = rows.get(number);
            Alpha alpha = generateAlpha(row);
            List<Double> g = new ArrayList<>();
            alpha.values.forEach(value -> {
                List<Double> list = new ArrayList<>();
                list.add(1.0);
                list.addAll(row.subList(1, row.size()));
                g.addAll(
                        list.stream().map(
                                x -> x * value / alpha.middle
                        ).collect(Collectors.toList())
                );
            });
            newG.add(g);
        });
        return newG;
    }

    public double[][] generateCoefficients(double[][] G, double[][] f) {
        return Utils.multiply(
                Utils.multiply(
                        Utils.inverse(
                                Utils.multiply(
                                        Utils.transpose(G),
                                        G
                                ), false
                        ),
                        Utils.transpose(G)
                ),
                f
        );
    }

    public Set<Integer> getRandomRows(int size, double percent) {
        int setSize = (int) (size * percent);
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < setSize) {
            numbers.add((int) (Math.random() * (size - 1)));
        }
        return numbers;
    }

    public Alpha generateAlpha(List<Double> row) {
        Alpha alpha = new Alpha();
        alpha.values = IntStream.range(0, C.size()).mapToObj(i ->
                MathUtils.getAlpha(row.subList(1, row.size()), S.get(i), C.get(i))
        ).collect(Collectors.toList());
        alpha.middle = alpha.values.stream().collect(Collectors.summingDouble(v -> (double) v));
        return alpha;
    }
}
