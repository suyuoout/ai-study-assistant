package com.study.assisiant;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/test")
public class JwtTestController {


    @GetMapping("/auth")
    public Result<Long> testAuth(
            HttpServletRequest request
    ) {


        Long userId =
                (Long) request.getAttribute("userId");


        return Result.success(
                "JWT验证成功",
                userId
        );
    }
}
