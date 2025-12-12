package SnakeAndLadderGame.Service;
import SnakeAndLadderGame.DTO.Board;
import SnakeAndLadderGame.DTO.Player;
import SnakeAndLadderGame.Respository.PlayerRepository;
import SnakeAndLadderGame.Utils.DiceUtil;

import java.util.Scanner;

public class BoardService {

    //Roll dice
    //Move Player
    //Check for snake or ladder or winning index


    static Board board;

    static void initializeBoard(Scanner scanner) {
        System.out.println("Enter Board Size Start and End (three numbers separated by spaces): ");
        String input = scanner.nextLine();
        String[] boardInputs = input.trim().split(" ");

        if (boardInputs.length != 3) {
            throw new IllegalArgumentException("Please provide three numbers: board size, start point, and end point");
        }

        try {
            int boardSize = Integer.parseInt(boardInputs[0]);
            int boardStartPoint = Integer.parseInt(boardInputs[1]);
            int boardEndPoint = Integer.parseInt(boardInputs[2]);

            board = new Board(boardSize, boardStartPoint, boardEndPoint);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please provide valid numbers for board configuration");
        }

    }

    public void startGame(){
        while (true){
            for(String playerName: PlayerRepository.getPlayerMap().keySet()){
                int updatedPosition = movePlayer(playerName);
                if(updatedPosition == board.getBoardFinalPoint())
                    return;
            }
        }
    }

    int rollDice() {
      return DiceUtil.getDiceValue();
    }

    int movePlayer(String playerName) {

        Player player = PlayerRepository.getPlayerMap().get(playerName);
        int currPosition = player.getCurrentPosition();

        if (currPosition == board.getBoardFinalPoint()) {
            throw new RuntimeException("Game Over!!...Since Player has reached the end of the board");
        }

        int diceValue = rollDice();

        int updatedPosition = currPosition + diceValue;

        if (SnakeService.isSnakeHeadPosition(updatedPosition)) {
            updatedPosition = SnakeService.moveToSnakeTail(updatedPosition);
        }

        if (LadderService.isLadderBottomPosition(updatedPosition)) {
            updatedPosition = LadderService.moveToLadderTopPosition(updatedPosition);
        }

        if (updatedPosition > board.getBoardFinalPoint()) {
            updatedPosition = currPosition;
        }

        if (updatedPosition <= board.getBoardFinalPoint()) {
            player.setCurrentPosition(updatedPosition);
            System.out.println("-- "+ playerName + " rolled a " + diceValue + " and moved from " + currPosition + " to " + updatedPosition);
        }

        if(updatedPosition == board.getBoardFinalPoint()){
            player.setCurrentPosition(updatedPosition);
            System.out.println(playerName + " has wins the game");
        }

        return updatedPosition;
    }

}






