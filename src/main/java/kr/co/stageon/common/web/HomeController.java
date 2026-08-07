package kr.co.stageon.common.web;

import kr.co.stageon.home.service.HomeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 홈 화면 진입 경로를 담당합니다. */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeQueryService homeQueryService;

    @GetMapping({"/", "/index"})
    public String home(Model model) {
        model.addAttribute("homeData", homeQueryService.getHomePage());
        return "index";
    }
}
