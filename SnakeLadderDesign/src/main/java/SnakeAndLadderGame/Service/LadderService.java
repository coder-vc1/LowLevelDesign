package SnakeAndLadderGame.Service;

import SnakeAndLadderGame.Respository.LadderRepository;

import java.util.Map;
import java.util.Scanner;

public class LadderService {

    static  boolean isLadderBottomPosition(int position){

        if(LadderRepository.getLadderTopBottomMap().containsKey(position)){
            System.out.println("Lucky! You found a ladder at position " + position + ". Climbing up!");
            return true;
        }

        else
            return false;
    }

    static  int moveToLadderTopPosition(int position){

        return LadderRepository.getLadderTopBottomMap().get(position);
    }

    static void initializeLadderRepository(Scanner scanner){
        LadderRepository ladderRepository = new LadderRepository();
        Map<Integer, Integer> ladderTopBottomMap = ladderRepository.getLadderTopBottomMap();
        System.out.println("Enter number of ladders");
        int ladderCount = scanner.nextInt();

        scanner.nextLine(); //Clearing input buffer

        while(ladderCount-- > 0){
            String[] ladderPositions = scanner.nextLine().trim().split(" ");
            int ladderTopPosition = Integer.parseInt(ladderPositions[0]);
            int ladderBottomPosition = Integer.parseInt(ladderPositions[1]);
            ladderTopBottomMap.put(ladderTopPosition, ladderBottomPosition);
        }


    }
}
