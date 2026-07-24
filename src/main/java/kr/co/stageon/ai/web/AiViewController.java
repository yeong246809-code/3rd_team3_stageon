package kr.co.stageon.ai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** AI 추천과 FAQ 화면을 일반 예매 흐름과 독립적으로 제공합니다. */
@Controller
public class AiViewController {

    @GetMapping("/ai")
    public String ai() {
        return "ai/recommend-faq";
    }
}
