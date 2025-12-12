package SnakeAndLadderGame.Utils;

import java.util.concurrent.ThreadLocalRandom;

public class DiceUtil {

    static public int getDiceValue(){

        int numberOfDice = 1;
        int diceValue = ThreadLocalRandom.current()
                                         .nextInt(1*numberOfDice,6*numberOfDice + 1);
        return diceValue;
    }

}
