package SnakeAndLadderGame.Service;

import java.util.Scanner;

public class inMemoryService {

    public inMemoryService(Scanner scanner){
        SnakeService.initializeSnakeRepository(scanner);
        LadderService.initializeLadderRepository(scanner);
        PlayerService.initializePlayerRepository(scanner);
        BoardService.initializeBoard(scanner);
    }
}
