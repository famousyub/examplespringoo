package com.artaeum.uaa.controller;

import com.artaeum.uaa.config.Constants;
import com.artaeum.uaa.domain.User;
import com.artaeum.uaa.dto.UserDTO;
import com.artaeum.uaa.dto.UserRegister;
import com.artaeum.uaa.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AccountControllerTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AccountController accountController;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Before
    public void init() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(this.accountController).build();
    }

    @Test
    public void whenCreateValidUserThenSuccess() throws Exception {
        UserRegister user = new UserRegister();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("password");
        user.setLangKey("en");
        String json = this.mapper.writeValueAsString(user);
        this.mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void whenCreateUserWithInvalidLoginThenThrowException() throws Exception {
        UserRegister user = new UserRegister();
        user.setEmail("test@email.com");
        user.setLogin("ba");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("password");
        user.setLangKey("en");
        String json = this.mapper.writeValueAsString(user);
        this.mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenCreateUserWithInvalidLangKeyThenItWillBeEN() throws Exception {
        UserRegister user = new UserRegister();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("password");
        user.setLangKey("cz");
        String json = this.mapper.writeValueAsString(user);
        this.mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        User result = this.userRepository.findByLogin("testlogin").get();
        assertEquals(Constants.DEFAULT_LANG_KEY, result.getLangKey());
    }

    @Test
    public void whenCreateUserWithInvalidEmailThenThrowException() throws Exception {
        UserRegister user = new UserRegister();
        user.setEmail("testemail");
        user.setLogin("testlogin");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("password");
        user.setLangKey("en");
        String json = this.mapper.writeValueAsString(user);
        this.mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenCreateUserWithInvalidPasswordThenThrowException() throws Exception {
        UserRegister user = new UserRegister();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword("pass1");
        user.setLangKey("en");
        String json = this.mapper.writeValueAsString(user);
        this.mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenGetPrincipal() throws Exception {
        this.mockMvc.perform(get("/account/current")
                .principal(new UsernamePasswordAuthenticationToken(
                        "123", "password", Collections.emptyList())))
                .andExpect(jsonPath("$.name").value("123"))
                .andExpect(status().isOk());
    }

    @Test
    public void whenChangePassword() throws Exception {
        this.mockMvc.perform(post("/account/change-password")
                .principal(new UsernamePasswordAuthenticationToken(
                        "123", "password", Collections.emptyList()))
                .content("password"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void whenSaveAccount() throws Exception {
        User user = new User();
        user.setLogin("userlogin");
        user.setEmail("user@test");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLangKey("ru");
        user.setRegisterDate(ZonedDateTime.now());

        this.userRepository.save(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin("newlogin");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("new-emailt@test");
        userDTO.setLangKey("en");
        userDTO.setAuthorities(Collections.singleton(Constants.ADMIN_AUTHORITY));

        this.mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(new UsernamePasswordAuthenticationToken(
                        user.getId(), "password", Collections.emptyList()))
                .content(this.mapper.writeValueAsBytes(userDTO)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findByLogin(user.getLogin()).orElse(null);

        assertNotNull(updatedUser);
        assertEquals(user.getLogin(), updatedUser.getLogin());
        assertEquals(userDTO.getFirstName(), updatedUser.getFirstName());
        assertEquals(userDTO.getLastName(), updatedUser.getLastName());
        assertEquals(userDTO.getEmail(), updatedUser.getEmail());
        assertEquals(userDTO.getLangKey(), updatedUser.getLangKey());
        assertTrue(updatedUser.getAuthorities().isEmpty());
    }
}
