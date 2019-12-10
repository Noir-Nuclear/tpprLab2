package loading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TXTLoader {
    List<String> lines;

    public TXTLoader(String path) {
        lines = new ArrayList<>();
        try {
            Files.lines(Paths.get(path), StandardCharsets.UTF_8).forEach(line -> lines.add(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<List<Double>> loadRows() {
        return IntStream.range(1, lines.size()).mapToObj(i -> {
            List<Double> row = new ArrayList<>();
            Arrays.stream(lines.get(i).split("\\t    ")).forEach(val -> row.add(Double.parseDouble(val)));
            return row;
        }).collect(Collectors.toList());
    }

    public Integer loadNumberOfColumns() {
        return Integer.parseInt(lines.get(0).split("\\s+")[0]);
    }

    public Integer loadNumberOfRows() {
        return Integer.parseInt(lines.get(0).split("\\s+")[1]);
    }

    public List<List<Double>> loadC() {
        return IntStream.range(0, lines.size()).mapToObj(i -> {
            List<Double> row = new ArrayList<>();
            Arrays.stream(lines.get(i).split("\\s+")).forEach(val -> row.add(Double.parseDouble(val)));
            return row;
        }).collect(Collectors.toList());
    }
}
