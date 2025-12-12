package SnakeAndLadderGame.Respository;

import java.util.HashMap;
import java.util.Map;

public class LadderRepository {

    static Map<Integer, Integer> ladderTopBottomMap;

    public LadderRepository(){
        ladderTopBottomMap = new HashMap<>();
    }
    public  static Map<Integer, Integer> getLadderTopBottomMap(){

        return  ladderTopBottomMap;
    }
}
