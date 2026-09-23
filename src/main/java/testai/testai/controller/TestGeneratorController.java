package testai.testai.controller;

import testai.testai.utils.TestGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class TestGeneratorController {
    private final TestGeneratorService testGeneratorService;
    @PostMapping("/generate-tests-for-file")
    public void generateTestsForFile(@RequestBody String filePath) {
        testGeneratorService.generateTestsForFile(filePath);
    }
}
