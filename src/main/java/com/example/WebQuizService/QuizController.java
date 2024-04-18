package com.example.WebQuizService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@Validated
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CompletionRepository completionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public QuizController(QuizRepository quizRepository, AppUserRepository appUserRepository, CompletionRepository completionRepository, PasswordEncoder passwordEncoder) {
        this.quizRepository = quizRepository;
        this.appUserRepository = appUserRepository;
        this.completionRepository = completionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("actuator/shutdown")//API Endpoint for testing purposes.No authentication needed
    public String testing() {
        return "POSTING TEST WORKS";
    }

    @GetMapping("api/health") //API Endpoint where the working is tested
    public String getHealth() {
        return "Health is okay";
    }

    //API Endpoint to register a new user
    @PostMapping("/api/register")
    public ResponseEntity<?> registerAppUser(@Valid @RequestBody RegistrationRequest request) {
        Optional<AppUser> user = appUserRepository.findByEmail(request.email);

        if (user.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("An account with this email already exists");
        } else {
            if (request.password.length() < 5 || !request.email.matches("\\w+@\\w+\\.\\w+")) {
                return ResponseEntity
                        .badRequest()
                        .body("The password must be at least 5 characters long");
            } else {
                // Encode the password before saving
                String encodedPassword = passwordEncoder.encode(request.password);
                AppUser newUser = new AppUser(request.email, encodedPassword);

                appUserRepository.save(newUser);

                return ResponseEntity
                        .ok()
                        .body("The user has been successfully created");
            }
        }
    }
    record RegistrationRequest(String email, String password) {}

    //API Endpoint with POST Mapping to add a new quiz to the list of quizzes
    @PostMapping("/api/quizzes")
    public ResponseEntity<Quiz> addQuiz(@Valid @RequestBody Quiz quiz, @AuthenticationPrincipal UserDetails details) throws JsonProcessingException {
        //look for the current user in the app user repository
        Optional<AppUser> optionalAppUser = appUserRepository.findByEmail(details.getUsername());

        if (optionalAppUser.isPresent()) {
            AppUser author = optionalAppUser.get();
            //make the current user the author of the current quiz
            quiz.setAuthor(author);
            //add the quiz to the database
            quizRepository.save(quiz);
            //add the quiz to the author made quizzes
            author.getQuiz().add(quiz);
            //return the added quiz as teh body of the response
            return ResponseEntity.ok().body(quiz);
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    //API Endpoint with POST Mapping to check if the given answer for a quiz is correct
    @PostMapping("/api/quizzes/{id}/solve")
    public ResponseEntity<?> getCorrection(@AuthenticationPrincipal UserDetails details, @PathVariable("id") long id ,@RequestBody QuizAnswer request) throws JsonProcessingException {

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new answerCorrection(false,"Wrong answer! Please, try again."));

        //Retrieve the current user from the app user repository for adding the quiz completion afterward
        Optional<AppUser> optionalAppUser = appUserRepository.findByEmail(details.getUsername());

        if (optionalAppUser.isPresent()) {
            //if the user is found, make it the current user
            AppUser currentUser = optionalAppUser.get();
            //Retrieve the quiz from the repository
            Optional<Quiz> optionalQuiz = quizRepository.findById(id);

            //check if the quiz exists
            if (optionalQuiz.isPresent()) {

                Quiz quiz = optionalQuiz.get();

                //in case the quiz the answer given at creation was null, set the quiz answer to an empty ArrayList
                if (quiz.getAnswer() == null) {
                    quiz.setAnswer(new ArrayList<>());
                }

                //if the answer given and the quiz answer match, change the response body to the corresponding correct values
                else if (request.answer.equals(quiz.getAnswer())) {

                    //create a completion object with the current user as the author and the quiz's id
                    Completion completion = new Completion(currentUser, quiz.getId());

                    //add the quiz completion to the users completions and save the completions to the repository
                    currentUser.getCompletions().add(completion);
                    completionRepository.save(completion);

                    //create a response body with a json formatted text if the given answer was correct
                    body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new answerCorrection(true,"Congratulations, you're right!"));
                }

                return ResponseEntity.ok().body(body);

            } else {
                //the quiz was not found
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }
    record QuizAnswer(List<Integer> answer){};

    //API Endpoint with GET Mapping to get a quiz with a given id
    @GetMapping("/api/quizzes/{id}")
    public ResponseEntity<?> getQuiz(@PathVariable long id) {

        //Retrieve the quiz from the repository
        Optional<Quiz> optionalQuiz = quizRepository.findById(id);

        //check if the quiz with given id exists
        if (optionalQuiz.isPresent()) {
            //return the wanted quiz if found
            return ResponseEntity.ok().body(optionalQuiz.get());
        } else {
            //the quiz with given id  was not found/doesn't exist
            return ResponseEntity.notFound().build();
        }

    }

    //API Endpoint with GET Mapping to get all quizzes available
    /*@GetMapping("/api/quizzes")
    public ResponseEntity<List<Quiz>> getAllQuizzes() {
        return ResponseEntity
                .ok()
                //show all the quizzes in the quiz repository
                .body(quizRepository.findAll());
    }*/

    //API Endpoint with GET Mapping to get all quizzes available
    @GetMapping("/api/quizzes")
        public ResponseEntity<?> getAllQuizzes(@RequestParam (required = false, defaultValue = "0") Integer page, @AuthenticationPrincipal UserDetails details) {

        Optional<AppUser> user = appUserRepository.findByEmail(details.getUsername());

        if (user.isPresent()) {
            //if the page parameter was given , return 10 quizzes beginning with the given page
            return ResponseEntity
                    .ok()
                    .body(quizRepository.findAll(PageRequest.of(page, 10)));
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    //API Endpoint to get all the completions of the current user formatted to json
    @GetMapping("api/quizzes/completed")
    public ResponseEntity<?> getCompletedQuizzes(@AuthenticationPrincipal UserDetails details, @RequestParam(required = false, defaultValue = "0") Integer page) throws JsonProcessingException {

        Optional<AppUser> optionalAppUser = appUserRepository.findByEmail(details.getUsername());
        if (optionalAppUser.isPresent()) {
            AppUser currentUser = optionalAppUser.get();
            //create a page of 10 elements of completion from the current user and sort them in descending order of the completion time
            Page<Completion> sortedCompletions = completionRepository.findCompletionsByAuthor(currentUser, PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "completedAt")));

            //return the paged completions depending on the given page parameter
            return ResponseEntity
                    .ok()
                    .body(sortedCompletions);

        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    @DeleteMapping("/api/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id, @AuthenticationPrincipal UserDetails details) {
        //look for the quiz in the repository
        Optional<Quiz> optionalQuiz = quizRepository.findById(id);

        if (optionalQuiz.isPresent()) {
            Quiz quiz = optionalQuiz.get();
            //get the current user from the appUser repository
            AppUser user = appUserRepository.findByEmail(details.getUsername()).get();

            //check for a match between the quiz's author and the current user
            if (quiz.getAuthor() == user) {
                //delete the quiz from the repository if the match is positive
                quizRepository.delete(quiz);
                //delete the quiz from the user's quizzes arrayList
                user.getQuiz().remove(quiz);

                return ResponseEntity.noContent().build();
            } else {
                //respond with a forgiven status if the current user is not the author of the selected quiz
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } else {
            //respond with a not found status if the selected quiz doesn't exist
            return ResponseEntity.notFound().build();
        }
    }
}