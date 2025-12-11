package jp.ac.kinki_pc.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jp.ac.kinki_pc.service.UserAuthenticationService;

@RestController
public class UserAuthenticationController {

	@Autowired
	private UserAuthenticationService userAuthenticationService;
}