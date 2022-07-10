package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class HomeController {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

//	//handler
//	@GetMapping("/test")
//	@ResponseBody
//	public String testing() {
//		
//		User user=new User();
//		user.setName("Santanil");
//		user.setEmail("demo@email.com");
//		
//		userRepository.save(user);
//		return "test";
//	}

	// home handler
	@GetMapping("/")
	private String home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}

	// about handler
	@GetMapping("/about")
	private String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}

	// signup handler
	@GetMapping("/signup")
	private String signup(Model model) {
		model.addAttribute("title", "SignUp - Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	// registration handler to receive data from form
	@RequestMapping(value = "/do_register", method = RequestMethod.POST)
	private String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result1,
			@RequestParam(value = "agreement", defaultValue = "false") Boolean agreement, Model model,
			HttpSession session) {
		try {
			if (!agreement) {
				System.out.println("Please agree to the terms and conditions to proceed");
				throw new Exception("Please agree to the terms and conditions to proceed");
			}
			if (result1.hasErrors()) {
				System.out.println("Error: " + result1.toString());
				model.addAttribute("user", user);
				return "signup";
			}
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));

			User result = this.userRepository.save(user);
			System.out.println("Agreement" + agreement);
			System.out.println("User" + result);

			model.addAttribute("user", new User());
			session.setAttribute("message", new Message("Successfully registered! ", "alert-success"));
			return "signup";
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Something went wrong!" + e.getMessage(), "alert-danger"));

			return "signup";
		}

	}

	// signin handler
	@GetMapping("/signin")
	public String signin(Model model) {
		model.addAttribute("title", "SignIn - Smart Contact Manager");
		return "signin";
	}

}
