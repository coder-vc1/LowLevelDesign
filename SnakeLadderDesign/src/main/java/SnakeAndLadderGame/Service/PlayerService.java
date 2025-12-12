package SnakeAndLadderGame.Service;

import SnakeAndLadderGame.Respository.PlayerRepository;

import java.util.Map;
import java.util.Scanner;
import SnakeAndLadderGame.DTO.Player;

public class PlayerService {

    public static void initializePlayerRepository(Scanner scanner){

        PlayerRepository playerRepository  = new PlayerRepository();

        Map<String, Player> playerMap = playerRepository.getPlayerMap();
        System.out.println("Enter number of players");

        int playerCount = scanner.nextInt();
        scanner.nextLine();

        while(playerCount-- > 0){
            int initialPosition = 0;
            System.out.println("Enter player name");
            String playerName = scanner.nextLine();
            playerMap.put(playerName, new Player(playerName, initialPosition));
        }

    }
}
