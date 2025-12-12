package SnakeAndLadderGame.Service;

import SnakeAndLadderGame.Respository.SnakeRepository;

import java.util.Map;
import java.util.Scanner;

public class SnakeService {

    static boolean isSnakeHeadPosition(int position){

        if(SnakeRepository.getSnakeHeadTailMap().containsKey(position)) {
            System.out.println("Oops! You landed on a snake's head at position " + position + ". Going down!");
            return true;
        }
        else
            return false;
    }

    static int moveToSnakeTail(int position){
        return SnakeRepository.getSnakeHeadTailMap().get(position);

    }

    static void initializeSnakeRepository(Scanner scanner){
        SnakeRepository snakeRepository = new SnakeRepository();
        Map<Integer, Integer> snakeHeadTailMap = snakeRepository.getSnakeHeadTailMap();
        System.out.println("Enter number of snakes");
        int snakeCount = scanner.nextInt();

        scanner.nextLine();
        while(snakeCount-- > 0) {
            String[] snakePositions = scanner.nextLine().trim().split(" ");
            int snakeHeadPosition = Integer.parseInt(snakePositions[0]);
            int snakeTailPosition = Integer.parseInt(snakePositions[1]);
            snakeHeadTailMap.put(snakeHeadPosition, snakeTailPosition);
        }


        /*
        for(int i=0;i<snakeCount;i++){
            System.out.println("Enter snake head position");
            int snakeHeadPosition = scanner.nextInt();
            System.out.println("Enter snake tail position");
            int snakeTailPosition = scanner.nextInt();
            snakeHeadTailMap.put(snakeHeadPosition, snakeTailPosition);
        }
        */

    }


}
