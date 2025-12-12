package SnakeAndLadderGame;

import SnakeAndLadderGame.Service.BoardService;
import SnakeAndLadderGame.Service.inMemoryService;

import java.util.Scanner;

public class AppRunningPoint {

    public static void main (String[] args){

        Scanner scanner = new Scanner(System.in);
        inMemoryService inMemoryService = new inMemoryService(scanner);
        BoardService boardService = new BoardService();

        try{
             boardService.startGame();
         }catch (Exception e){
             System.out.println(e.getMessage());
         }
    }
}
