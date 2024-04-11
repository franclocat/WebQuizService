package com.example.WebQuizService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

import javax.print.attribute.standard.DateTimeAtCompleted;
import javax.print.attribute.standard.DateTimeAtProcessing;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@Validated
public class QuizController {
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
    public ResponseEntity<?> registerAppUser(@RequestBody RegistrationRequest request) {
        Optional<AppUser> user = appUserRepository.findAppUserByUsername(request.email());

        if (user.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .build();
        } else {
            AppUser newUser = new AppUser();
            newUser.setEmail(request.email());
            newUser.setPassword(passwordEncoder.encode(request.password()));
            appUserRepository.save(newUser);

            return ResponseEntity
                    .ok()
                    .build();
        }
    }
    record RegistrationRequest(String email, String password){};

    //API Endpoint with POST Mapping to add a new quiz to the list of quizzes
    @PostMapping("/api/quizzes")
    public ResponseEntity<Quiz> addQuiz(@Valid @RequestBody Quiz quiz, @AuthenticationPrincipal UserDetails details) throws JsonProcessingException {
        //look for the current user in the app user repository
        Optional<AppUser> optionalAppUser = appUserRepository.findAppUserByUsername(details.getUsername());

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
    public ResponseEntity<?> getCorrection(@AuthenticationPrincipal UserDetails details, @PathVariable("id") long id ,@RequestBody Map<String, List<Integer>> request) throws JsonProcessingException {

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new answerCorrection(false,"Wrong answer! Please, try again."));

        //Retrieve the current user from the app user repository for adding the quiz completion afterward
        Optional<AppUser> optionalAppUser = appUserRepository.findAppUserByUsername(details.getUsername());

        if (optionalAppUser.isPresent()) {
            //if the user is found, make it the current user
            AppUser user = optionalAppUser.get();
            //Retrieve the quiz from the repository
            Optional<Quiz> optionalQuiz = quizRepository.findById(id);

            //check if the quiz exists
            if (optionalQuiz.isPresent()) {

                Quiz quiz = optionalQuiz.get();

                //in case the quiz the answer given at creation was null, set the quiz answer to an empty ArrayList
                if (quiz.getAnswer() == null) {
                    quiz.setAnswer(new ArrayList<>());
                }

                //if the answer given and the quiz answer match, change the response body to the corresponding values
                if (request.get("answer").equals(quiz.getAnswer())) {

                    //create a completion object with the user as the author and the quiz's id as the idto put into the users completions and add it
                    Completion completion = new Completion(user,quiz.getId());
                    user.getCompletions().add(completion);
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
        public ResponseEntity<?> getAllQuizzes(@RequestParam (required = false) Integer page, @AuthenticationPrincipal UserDetails details) {

        Optional<AppUser> user = appUserRepository.findAppUserByUsername(details.getUsername());

        if (user.isPresent()) {
            if (page == null) {
                //if the request parameter was not given, return the first 10 quizzes
                return ResponseEntity
                        .ok()
                        //show the first 10 quizzes in the quiz repository
                        .body(quizRepository.findAll(PageRequest.of(0, 10)));
            } else {
                //if the page parameter was given , return 10 quizzes beginning with the given page
                return ResponseEntity
                        .ok()
                        .body(quizRepository.findAll(PageRequest.of(page, 10)));
            }
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
    }

    //API Endpoint to get all the completions of the current user formatted to json
    @GetMapping("api/quizzes/completed")
    public ResponseEntity<?> getCompletedQuizzes(@AuthenticationPrincipal UserDetails details, @RequestParam(required = false) Integer page) throws JsonProcessingException {

        Optional<AppUser> optionalAppUser = appUserRepository.findAppUserByUsername(details.getUsername());
        if (optionalAppUser.isPresent()) {
            AppUser user = optionalAppUser.get();
            Page<Completion> sortedCompletions = completionRepository.findCompletionsByUser(user, PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "completedAt")));

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
            AppUser user = appUserRepository.findAppUserByUsername(details.getUsername()).get();

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