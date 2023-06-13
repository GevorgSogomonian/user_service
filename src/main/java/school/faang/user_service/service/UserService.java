package school.faang.user_service.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.MappingIterator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import school.faang.user_service.config.context.UserContext;
import school.faang.user_service.dto.generated.Person;
import school.faang.user_service.dto.user.UserDto;
import school.faang.user_service.dto.user.UserFilterDto;
import school.faang.user_service.dto.user.UserProfileDto;
import school.faang.user_service.entity.User;
import school.faang.user_service.entity.UserProfilePic;
import school.faang.user_service.exception.EntityNotFoundException;
import school.faang.user_service.exception.ErrorMessage;
import school.faang.user_service.exception.FileException;
import school.faang.user_service.mapper.UserMapper;
import school.faang.user_service.mapper.person.CountryInfoMapper;
import school.faang.user_service.mapper.person.PersonMapper;
import school.faang.user_service.messaging.UserProfileViewPublisher;
import school.faang.user_service.parser.PersonCsvParser;
import school.faang.user_service.repository.UserRepository;
import school.faang.user_service.service.filter.user.UserFilter;
import school.faang.user_service.service.s3.ProfilePicService;

import java.io.IOException;
import java.io.InputStream;

import static school.faang.user_service.util.RandomPasswordUtil.generateRandomPassword;

@Service
public class UserService extends AbstractUserService {
    private final UserRepository userRepository;
    private final ProfilePicService profilePicService;
    private final UserContext userContext;
    private final UserProfileViewPublisher userProfileViewPublisher;
    private final PersonMapper personMapper;
    private final CountryInfoMapper countryInfoMapper;
    private final PersonCsvParser personCsvParser;

    public UserService(UserRepository userRepository, List<UserFilter> filters,
                       UserMapper userMapper, UserContext userContext,
                       UserProfileViewPublisher userProfileViewPublisher,
                       ProfilePicService profilePicService, PersonMapper personMapper,
                       CountryInfoMapper countryInfoMapper, PersonCsvParser personCsvParser) {
        super(filters, userMapper);
        this.userRepository = userRepository;
        this.profilePicService = profilePicService;
        this.userContext = userContext;
        this.userProfileViewPublisher = userProfileViewPublisher;
        this.personMapper = personMapper;
        this.countryInfoMapper = countryInfoMapper;
        this.personCsvParser = personCsvParser;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(UserFilterDto filter) {
        Stream<User> users = StreamSupport.stream(userRepository.findAll().spliterator(), false);
        return filterUsers(users, filter);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getPremiumUsers(UserFilterDto filter) {
        Stream<User> users = userRepository.findPremiumUsers();
        return filterUsers(users, filter);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        return StreamSupport.stream(userRepository.findAllById(ids).spliterator(), false)
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(long id) {
        return userMapper.toDto(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(long id) {
        UserProfileDto profile = userMapper.toProfileDto(findUser(id));
        if (userContext.getUserId() != id) {
            userProfileViewPublisher.publish(id, userContext.getUserId());
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public boolean existsById(long id) {
        return userRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean areOwnedSkills(long userId, List<Long> skillIds) {
        if (skillIds.isEmpty()) {
            return true;
        }
        return userRepository.countOwnedSkills(userId, skillIds) == skillIds.size();
    }

    @Transactional
    public void uploadProfilePic(long userId, MultipartFile file) {
        User user = findUser(userId);

        String[] keys = profilePicService.resizeAndSavePic(file);
        if (Objects.isNull(keys[0]) || Objects.isNull(keys[1])) {
            throw new FileException(ErrorMessage.FILE_EXCEPTION);
        }

        UserProfilePic userProfilePic = new UserProfilePic();
        userProfilePic.setFileId(keys[0]);
        userProfilePic.setSmallFileId(keys[1]);
        user.setUserProfilePic(userProfilePic);
        userRepository.save(user);
    }

    public byte[] getProfilePic(long userId, boolean getSmallPic) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorMessage.USER_NOT_FOUND, userId));
        String key = getSmallPic ? user.getUserProfilePic().getSmallFileId() : user.getUserProfilePic().getFileId();
        return profilePicService.downloadFile(key);
    }

    public void deleteProfilePic(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorMessage.USER_NOT_FOUND, userId));

        profilePicService.deleteFile(user.getUserProfilePic().getSmallFileId());
        profilePicService.deleteFile(user.getUserProfilePic().getFileId());
    }

    @Transactional(readOnly = true)
    public User findUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Couldn't find a user with id " + id));
    }

    @Transactional
    public void processUnmappedUserData(InputStream csv) throws IOException {
        MappingIterator<Person> personMappingIterator = personCsvParser.parse(csv);

        while (personMappingIterator.hasNext()) {
            Person person = personMappingIterator.nextValue();
            String country = person.getContactInfo().getAddress().getCountry();
            countryInfoMapper.setCountryIdToPerson(person, country);

            User user = personMapper.toUser(person);
            user.setPassword(generateRandomPassword());
            userRepository.save(user);
        }
    }
}