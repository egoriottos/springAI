package testai.testai.utils;

import testai.testai.dto.Decision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class TestGeneratorService {
    private static final Path CACHE_FILE = Path.of(".test-agent-cache.properties");

    private final ChatClient chatClient;
    private final ClassSourceTools classSourceTools;

    public TestGeneratorService(ChatClient.Builder chatClientBuilder, ClassSourceTools classSourceTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt())
                .build();
        this.classSourceTools = classSourceTools;
    }

    // ---- публичные точки входа ----

    public Decision generateTest(String sourceFilePath, String methodName) {
        String sourceCode = readFile(Path.of(sourceFilePath));
        Path testPath = deriveTestPath(Path.of(sourceFilePath));
        String existingTestCode = Files.exists(testPath) ? readFile(testPath) : null;

        Decision decision = chatClient.prompt()
                .user(buildUserMessage(sourceCode, methodName, existingTestCode))
                .tools(classSourceTools)
                .call()
                .entity(Decision.class);

        switch (decision.action()) {
            case CREATE_TEST_CLASS -> writeFile(testPath, decision.testCode());
            case ADD_TEST_METHOD -> writeFile(testPath, insertMethod(existingTestCode, decision.testCode()));
            case UPDATE_TEST_METHOD -> writeFile(testPath,
                    replaceMethod(existingTestCode, decision.existingTestMethodName(), decision.testCode()));
            case NO_CHANGE -> {
                // тест уже актуален
            }
        }
        return decision;
    }

    public void generateTestsForFile(String filePath) {
        Properties cache = loadCache();
        generateTestsForFile(Path.of(filePath), cache);
        saveCache(cache);
    }

    public void generateTestsForPackage(String packageDirectory) {
        Properties cache = loadCache();
        try (Stream<Path> stream = Files.walk(Path.of(packageDirectory))) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> generateTestsForFile(file, cache));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось обойти пакет: " + packageDirectory, e);
        }
        saveCache(cache);
    }

    // ---- внутренняя логика: перебор методов файла + кэш ----

    private void generateTestsForFile(Path sourceFile, Properties cache) {
        String sourceCode = readFile(sourceFile);
        for (String methodName : extractPublicMethodNames(sourceCode)) {
            String methodBody = extractMethodBody(sourceCode, methodName);
            String cacheKey = sourceFile + "#" + methodName;
            String currentHash = hash(methodBody);

            if (currentHash.equals(cache.getProperty(cacheKey))) {
                continue;
            }

            generateTest(sourceFile.toString(), methodName);
            cache.setProperty(cacheKey, currentHash);
        }
    }

    private List<String> extractPublicMethodNames(String sourceCode) {
        Pattern methodPattern = Pattern.compile(
                "public\\s+(?:static\\s+)?[\\w<>\\[\\],\\s]+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = methodPattern.matcher(sourceCode);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private String extractMethodBody(String sourceCode, String methodName) {
        String[] lines = sourceCode.split("\n", -1);
        Pattern signature = Pattern.compile(".*\\b" + Pattern.quote(methodName) + "\\s*\\([^)]*\\)\\s*\\{?.*");

        int sigLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (signature.matcher(lines[i]).matches()) {
                sigLine = i;
                break;
            }
        }
        if (sigLine == -1) {
            return "";
        }

        int braceBalance = 0;
        boolean seenOpenBrace = false;
        int endLine = -1;
        for (int i = sigLine; i < lines.length; i++) {
            for (char c : lines[i].toCharArray()) {
                if (c == '{') { braceBalance++; seenOpenBrace = true; }
                else if (c == '}') { braceBalance--; }
            }
            if (seenOpenBrace && braceBalance == 0) { endLine = i; break; }
        }
        if (endLine == -1) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = sigLine; i <= endLine; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Properties loadCache() {
        Properties props = new Properties();
        if (Files.exists(CACHE_FILE)) {
            try (InputStream in = Files.newInputStream(CACHE_FILE)) {
                props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Не удалось прочитать кэш: " + CACHE_FILE, e);
            }
        }
        return props;
    }

    private void saveCache(Properties props) {
        try (OutputStream out = Files.newOutputStream(CACHE_FILE)) {
            props.store(out, "хэши тел методов, для которых уже сгенерирован тест");
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить кэш: " + CACHE_FILE, e);
        }
    }

    // ---- файловые операции (act-шаг) ----

    private Path deriveTestPath(Path sourcePath) {
        String path = sourcePath.toString()
                .replace("src/main/java", "src/test/java")
                .replace(".java", "Test.java");
        return Path.of(path);
    }

    private String insertMethod(String existingTestCode, String newMethodCode) {
        int idx = existingTestCode.lastIndexOf('}');
        return existingTestCode.substring(0, idx) + "\n" + newMethodCode.strip() + "\n}";
    }

    private String replaceMethod(String content, String methodName, String replacement) {
        String[] lines = content.split("\n", -1);
        Pattern signature = Pattern.compile(".*\\bvoid\\s+" + Pattern.quote(methodName) + "\\s*\\(.*");

        int sigLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (signature.matcher(lines[i]).matches()) {
                sigLine = i;
                break;
            }
        }
        if (sigLine == -1) {
            throw new IllegalStateException("Не нашли метод " + methodName + " для замены");
        }

        int startLine = sigLine;
        while (startLine > 0 && lines[startLine - 1].trim().startsWith("@")) {
            startLine--;
        }

        int braceBalance = 0;
        boolean seenOpenBrace = false;
        int endLine = -1;
        for (int i = sigLine; i < lines.length; i++) {
            for (char c : lines[i].toCharArray()) {
                if (c == '{') { braceBalance++; seenOpenBrace = true; }
                else if (c == '}') { braceBalance--; }
            }
            if (seenOpenBrace && braceBalance == 0) { endLine = i; break; }
        }
        if (endLine == -1) {
            throw new IllegalStateException("Не смогли определить границы метода " + methodName);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < startLine; i++) sb.append(lines[i]).append("\n");
        sb.append(replacement.strip()).append("\n");
        for (int i = endLine + 1; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл: " + path, e);
        }
    }

    private void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать файл: " + path, e);
        }
    }

    // ---- промпт ----

    private String buildUserMessage(String sourceCode, String methodName, String existingTestCode) {
        String testSection = existingTestCode == null
                ? "Тестового класса пока не существует."
                : "Существующий тестовый класс:\n```java\n" + existingTestCode + "\n```";

        return "Метод, для которого нужен тест: " + methodName
               + "\n\nИсходный класс:\n```java\n" + sourceCode + "\n```"
               + "\n\n" + testSection;
    }

    private String systemPrompt() {
        return """
                Ты помогаешь поддерживать юнит-тесты (JUnit 5) в актуальном состоянии
                для одного конкретного метода Java-класса.

                Тебе дают исходный код класса, имя метода и (если есть) текущий тестовый класс.
                Если для теста нужно понять зависимость — используй инструмент readClassSource,
                чтобы посмотреть её исходник.

                Реши одно из четырёх действий:
                - ADD_TEST_METHOD — подходящего теста нет, нужно добавить новый
                - UPDATE_TEST_METHOD — тест есть, но устарел; укажи existingTestMethodName точно
                - CREATE_TEST_CLASS — тестового класса не существует вообще
                - NO_CHANGE — тест уже корректен

                Не трогай тесты, не относящиеся к указанному методу.
                """;
    }
}
