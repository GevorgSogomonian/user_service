package school.faang.user_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import school.faang.user_service.dto.UserDto;
import school.faang.user_service.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
                              @RequestPart(value = "userJson") String userJson) throws JsonProcessingException {
        UserDto userDto = objectMapper.readValue(userJson, UserDto.class);
        return userService.createUser(userDto, multipartFile);
    }

    @PutMapping("/{userId}/avatar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateUserAvatar(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
                                 @PathVariable Long userId) {
        userService.updateUserAvatar(userId, multipartFile);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUser(@PathVariable long userId) {
        return userService.getUser(userId);
    }

    @PostMapping("/byIds")
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getUsersByIds(@RequestBody List<Long> ids) {
        return userService.getUsersByIds(ids);
    }

    @PutMapping("/deactivate/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto deactivateUser(@PathVariable Long userId) {
        return userService.deactivateUser(userId);
    }
}