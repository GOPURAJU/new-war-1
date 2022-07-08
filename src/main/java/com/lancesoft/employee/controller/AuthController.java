package com.lancesoft.employee.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lancesoft.employee.model.User;
import com.lancesoft.employee.repository.UserRepository;
import com.lancesoft.employee.request.JwtRequest;
import com.lancesoft.employee.request.PasswordRequest;
import com.lancesoft.employee.response.JwtResponse;
import com.lancesoft.employee.securityconfig.JwtUtil;
import com.lancesoft.employee.service.impl.UserDetailsServiceImpl;
import com.lancesoft.employee.service.impl.UserServiceImpl;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/auth")

public class AuthController {
	
	private static final Logger LOG=LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private AuthenticationManager authenticationManager;// authenticate method will use this to authenticate 

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private UserServiceImpl userService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;


	@PostMapping("/login")
	public ResponseEntity<?> generateToken(@RequestBody JwtRequest jwtRequest) throws Exception{
		LOG.info("Enterd into generateToken method");
		try {

			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.getUsername(), jwtRequest.getPassword()));

		}catch(UsernameNotFoundException unfe) {
			LOG.debug("Unable to login, User Not Found{}",unfe.getMessage());
			unfe.printStackTrace();
			throw new Exception("User Not Found");
		}

		UserDetails userDetails=this.userDetailsService.loadUserByUsername(jwtRequest.getUsername());
		String token=this.jwtUtil.generateToken(userDetails);// this will give token

		return ResponseEntity.ok(new JwtResponse(token));
	}

	private void authenticate(String username, String password) throws Exception {// this method will auth if auth is not sucessfull then is will throegh excep.
		LOG.info("Ented Into authenticate method");
		try {

			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));//takinf uname and pass wich we have set in jwtfilter class

		}catch(DisabledException de) {
			LOG.debug("User Is Disabled due to this {}",de.getMessage());
			throw new Exception("USER IS Disabled"+de.getMessage());
		}catch(BadCredentialsException be) {
			LOG.debug("Invalid Credentials due to this {}",be.getMessage());
			throw new Exception("Invalid Credentials" +be.getMessage());

		}
	}
	
	@PutMapping("/change-password")
	public User changePwd(@RequestBody PasswordRequest pwdRequest, Principal principal){

		String LoggedInUserUsername=principal.getName();
		User currentUser=this.userRepository.findByUsername(LoggedInUserUsername);

		if(pwdRequest.getNewPassword() != null && !pwdRequest.getNewPassword().isEmpty() 
				&& !pwdRequest.getNewPassword().equals("") && !pwdRequest.getNewPassword().contains(" ")) {

			bCryptPasswordEncoder.matches(pwdRequest.getOldPassword(),currentUser.getPassword());//this will check old pwd fiels with current pwd 

			currentUser.setPassword(bCryptPasswordEncoder.encode(pwdRequest.getNewPassword()));
			userRepository.save(currentUser);
	  }
		return currentUser;
    }
	
}
