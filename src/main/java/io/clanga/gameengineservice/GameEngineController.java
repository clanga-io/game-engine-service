package io.clanga.gameengineservice;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import io.clanga.gamesshared.rng.RandomDraw;
import io.clanga.gamesshared.player.Player;
import io.clanga.gamesshared.payment.Account;
import io.clanga.gamesshared.engine.PlayRequest;

import java.net.URI;

@RestController
@RequestMapping("/game-engine-service/v1")
public class GameEngineController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/test-rngSvc")
    public Boolean testRngSvc() {
        return getOutcome();
    }

    @GetMapping("/test-playerSvc")
    public Player testPlayerSvc() {
        return getPlayerInfo("Player 1");
    }

    @GetMapping("/test-paymentSvc")
    public Account testPaymentSvc() {
        val player = getPlayerInfo("Player 1");
        return getAccountInfo(player.getAccountNumber());
    }

    @PostMapping("/play")
    public String play(@RequestBody final PlayRequest request) {
        // Get player info
        val player = getPlayerInfo(request.getUserName());

        // Attempt to deduct bet; if fail, return error message
        if (!withdraw(player.getAccountNumber(), request.getBet())) {
            return "Error withdrawing funds to complete game";
        }

        if (getOutcome()) {
            val win = request.getBet() * 2;
            deposit(player.getAccountNumber(), win);
            return "You won " + win + ". Account info: " + getAccountInfo(player.getAccountNumber());
        }
        else {
            return "No win. Account info: " + getAccountInfo(player.getAccountNumber());
        }
    }


    // Return outcome of the game; true = win, false = not win
    private Boolean getOutcome() {
        String baseUrl = "http://localhost:8081/rng-service/v1/rng";
        RandomDraw randomDraw = new RandomDraw(0,2);
        return restTemplate.postForObject(baseUrl, randomDraw, Integer.class) == 1;
    }

    private Player getPlayerInfo(String userName) {
        String baseUrl = "http://localhost:8082/player-service/v1/login/" + userName;
        return restTemplate.getForObject(baseUrl, Player.class);
    }

    private Account getAccountInfo(Long accountNumber) {
        String baseUrl = "http://localhost:8083/payment-service/v1/accounts/" + accountNumber;
        return restTemplate.getForObject(baseUrl, Account.class);
    }

    private Boolean withdraw(Long accountNumber, Long bet) {
        String baseUrl = "http://localhost:8083/payment-service/v1/accounts/" + accountNumber + "/withdraw/" + bet;
        return restTemplate.postForObject(baseUrl, null, Boolean.class);
    }

    private Boolean deposit(Long accountNumber, Long win) {
        String baseUrl = "http://localhost:8083/payment-service/v1/accounts/" + accountNumber + "/deposit/" + win;
        return restTemplate.postForObject(baseUrl, null, Boolean.class);
    }
}
