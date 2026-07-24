package kr.co.stageon.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 홈 화면 진입 경로를 담당합니다. */
@Controller
public class HomeController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }
}
