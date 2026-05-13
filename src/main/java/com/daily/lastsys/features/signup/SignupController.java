package com.daily.lastsys.features.signup;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute SignupForm signupForm,
            BindingResult bindingResult,
            Model model
    ) {
        if (!signupForm.passwordsMatch()) {
            bindingResult.rejectValue("passwordConfirm", "passwordConfirm.mismatch", "비밀번호가 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            signupService.signup(signupForm);
        } catch (SignupDuplicateException exception) {
            bindingResult.rejectValue(exception.getField(), exception.getField() + ".duplicate", exception.getMessage());
            return "auth/signup";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("signup.failed", exception.getMessage());
            return "auth/signup";
        }

        model.addAttribute("nickname", signupForm.getNickname());
        return "auth/signup-success";
    }
}
