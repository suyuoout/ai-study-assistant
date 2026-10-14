
package com.study.assisiant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of(
                "code", "200",
                "message", "智能学习助手后端运行正常"
        );
    }
}