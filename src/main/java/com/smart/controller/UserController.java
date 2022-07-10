package com.smart.controller;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// common method to add data to all the below handlers
	@ModelAttribute
	public void AddCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);

		// get user using username
		User user = userRepository.getUserByName(userName);
		System.out.println("User details: \n" + user);
		model.addAttribute("user", user);
	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashbard");
		return "normal/user-dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add-contact-form";
	}

	// process add contact form
	// below url should match with the form action url used
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {

			String name = principal.getName();
			User user = this.userRepository.getUserByName(name);

			// processing and uploading file
			if (!file.isEmpty()) {
				// upload the file to folder and update the name in contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("File uploaded!");

			} else {
				// file empty
				System.out.println("Empty file detected!");
				// if picture is not uploaded by default below pic will be attached to the
				// contact created which will be displayed
				contact.setImage("user.png");

			}

			// give contact user
			contact.setUser(user);

			// give user the contact
			user.getContacts().add(contact);
			this.userRepository.save(user);

			System.out.println("Contact Data: " + contact);

			System.out.println("Contact added to db!");
			// success message
			session.setAttribute("message", new Message("Contact added successfully!", "success"));

		} catch (Exception e) {
			System.out.println("Error msg:" + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong! Try again!", "danger"));
		}
		return "normal/add-contact-form";
	}

	// show contacts handler
	// contacts shown per page=5, using path variable for pagination
	// current page = 0
	@GetMapping("/show-contact/{page}")
	public String showContact(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show Contacts");

		String username = principal.getName(); // returns the user name ie the id of the user
		User user = this.userRepository.getUserByName(username);

		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show-contact";
	}

	// show particular contact handler (used to show contact details of a specific
	// contact of a user)
	@RequestMapping("/{cId}/contact")
	// to send data to view we use Model
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID" + cId);
		Optional<Contact> optionalCotacts = this.contactRepository.findById(cId);
		Contact contact = optionalCotacts.get();

		// solving security bug -> other user'd contacts are accessible to any user
		String userName = principal.getName();
		User user = this.userRepository.getUserByName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact-details";
	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session, Principal principal) {

		Contact contact = this.contactRepository.findById(cId).get();
		// before deleting the contact it should be unlinked from user as
		// Cascadetype.ALL is used in mapping
//		contact.setUser(null);
//
//		this.contactRepository.delete(contact);
		
		User user = this.userRepository.getUserByName(principal.getName());
		
		//jvm calls equals method internally to check for object matching
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		
		session.setAttribute("message", new Message("Contact deleted successfully!", "success"));
		return "redirect:/user/show-contact/0";
	}

	// open update form handler
	// post mapping is secure as , if we try to open the same page by copy pasting
	// the url it will return error
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cId).get();
		// send to view
		model.addAttribute("contact", contact);
		model.addAttribute("fileName", contact.getImageUrl());
		return "normal/update-form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model model, HttpSession session, Principal principal) {

		// old contact detail
		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();

		/*
		 * File name should be unique otherwise if the same image is selected while
		 * uploading the new photo will not get uploaded and the old photo will also be
		 * deleted
		 */
		try {

			if (!file.isEmpty()) {
				
					
				// upload new image file
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
				// delete old image file
				File filePath=new ClassPathResource("static/img").getFile();
				File oldFilePath= new File(filePath, oldContactDetail.getImageUrl());

				
				if(oldFilePath.delete())
					System.out.println("Old image was deleted successfully!");
				else
					System.out.println("Old image was not deleted due to an IOException");
				
				//image upload successful message
				session.setAttribute("message", new Message("Image upload successful","success"));

			} else {
				// set the old image as new image was uploaded
				contact.setImage(oldContactDetail.getImageUrl());
			}

			User user = this.userRepository.getUserByName(principal.getName());
			contact.setUser(user);

			this.contactRepository.save(contact);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Contact Name:" + contact.getName());
		System.out.println("Contact ID:" + contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	
	
	//profile view handler
	@GetMapping("/profile")
	public String getProfile(Model model) {
		model.addAttribute("title","Profile");
		return "normal/profile";
	}
	
	
	//open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}
	
	//if we get data from url we can use path variable
	//for form data we can use request param and can access the data by using the same value for name variable value for the field
	
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword , @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session ) {
		
		System.out.println("OLD: "+oldPassword);
		System.out.println("NEW: "+newPassword);
		
		User user = this.userRepository.getUserByName(principal.getName());
		String encryptedOldPassword = user.getPassword();
		
		//checking if user provided old password correctly before pwd reset
		if(bCryptPasswordEncoder.matches(oldPassword, encryptedOldPassword)) {
			//change pwd and save
			user.setPassword(bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Password reset successful","success"));
		}else {
			session.setAttribute("message", new Message("Please provide correct existing password for password reset","danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
}


