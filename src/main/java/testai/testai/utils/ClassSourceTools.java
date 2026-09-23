package testai.testai.utils;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class ClassSourceTools {
    @Tool(description = "Найти и прочитать исходник Java-класса по простому имени (без пакета), "
                        + "чтобы посмотреть сигнатуру зависимости для мока или перенять стиль соседнего теста")
    public String readClassSource(
            @ToolParam(description = "Простое имя класса, например OrderRepository") String className) {

        try (Stream<Path> stream = Files.walk(Path.of("."))) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(className + ".java"))
                    .findFirst()
                    .map(this::readFile)
                    .orElse("Класс " + className + " не найден в проекте.");
        } catch (IOException e) {
            throw new UncheckedIOException("Ошибка поиска класса " + className, e);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл: " + path, e);
        }
    }
}
