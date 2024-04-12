package com.example.WebQuizService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Validated
public class QuizController {
    private ObjectMapper mapper;

    private QuizRepository quizRepository;

    private AppUserRepository appUserRepository;

    private PasswordEncoder passwordEncoder;

    @PostMapping("actuator/shutdown")//API Endpoint for testing purposes.No authentication needed
    public String testing() {
        return "POSTING TEST WORKS";
    }

    @GetMapping("api/health") //API Endpoint where the working is tested
    public String getHealth() {
        return "Health is okay";
    }

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
    public ResponseEntity<?> getCorrection(@PathVariable("id") long id ,@RequestBody Map<String, List<Integer>> request) throws JsonProcessingException {

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new answerCorrection(false,"Wrong answer! Please, try again."));

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
                body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new answerCorrection(true,"Congratulations, you're right!"));
            }

            return ResponseEntity.ok().body(body);

        } else {
            //the quiz was not found
            return ResponseEntity.notFound().build();
        }
    }

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
    @GetMapping("/api/quizzes")
    public ResponseEntity<List<Quiz>> getAllQuizzes() {
        return ResponseEntity
                .ok()
                //show all the quizzes in the quiz repository
                .body(quizRepository.findAll());
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