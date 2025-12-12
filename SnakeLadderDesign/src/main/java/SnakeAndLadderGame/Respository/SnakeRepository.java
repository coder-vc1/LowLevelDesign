package SnakeAndLadderGame.Respository;

import java.util.HashMap;
import java.util.Map;

public class SnakeRepository {

    static Map<Integer, Integer> snakeHeadTailMap;

    public SnakeRepository(){
        snakeHeadTailMap = new HashMap<>();
    }

    public static Map<Integer, Integer> getSnakeHeadTailMap(){

        return snakeHeadTailMap;
    }
}
