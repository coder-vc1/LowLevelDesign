package SnakeAndLadderGame.Respository;
import SnakeAndLadderGame.DTO.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerRepository {

    static Map<String, Player> playerMap;

    public static Map<String, Player> getPlayerMap(){
        return playerMap;
    }

    public PlayerRepository(){
        playerMap = new HashMap<>();
    }
}
