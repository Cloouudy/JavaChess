import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;


public class Main extends Application {




    // sachovnice rozmery
    private static final int TILE_SIZE = 75;
    private static final int BORDER_SIZE = 30;
    private static final int BOARD_SIZE = 8;


    // kdo je na tahu kdyz TRUE tak bila, kdyz FALSE tak cerny
    private boolean isOnMove = true;
    private Label topLabel;

    private final Piece[][] boardState = new Piece[8][8];

    // Pole pro uložení vizuálních políček, abychom je mohli aktualizovat při tahu
    private final StackPane[][] squarePanes = new StackPane[8][8];

    // pamatuji si souradnice figurky - po prvnim kliknuti na figurku, -1 == nic neni vybrane
    private int selectedRow = -1;
    private int selectedCol = -1;

    @Override
    public void start(Stage primaryStage) {
        initializeBoardData();

        GridPane grid = new GridPane();


        topLabel = new Label();


        if (isOnMove){
            topLabel.setText("Bily na tahu");
        } else {
            topLabel.setText("Cerny na tahu");
        }

        topLabel.setStyle("-fx-background-color: #f19fff; -fx-text-fill: black;");
        topLabel.setAlignment(Pos.CENTER);
        topLabel.setPrefSize(TILE_SIZE * BOARD_SIZE, BORDER_SIZE);


        grid.add(topLabel, 1, 0, BOARD_SIZE, 1);


        Pane bottomLeftCorner = new Pane();
        bottomLeftCorner.setPrefSize(BORDER_SIZE, BORDER_SIZE);
        bottomLeftCorner.setStyle("-fx-background-color: #f19fff;");
        grid.add(bottomLeftCorner, 0, BOARD_SIZE + 1);

        Pane bottomRightCorner = new Pane();
        bottomRightCorner.setPrefSize(BORDER_SIZE, BORDER_SIZE);
        bottomRightCorner.setStyle("-fx-background-color: #f19fff;");
        grid.add(bottomRightCorner, BOARD_SIZE + 1, BOARD_SIZE + 1);

        for (int col = 0; col < BOARD_SIZE; col++) {
            char letter = (char) ('A' + col);
            Label bottomLabel = createCoordinateLabel(String.valueOf(letter));
            bottomLabel.setStyle("-fx-background-color: #f19fff; -fx-text-fill: black;");
            grid.add(bottomLabel, col + 1, BOARD_SIZE + 1);
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            int number = 8 - row;

            Label leftLabel = createCoordinateLabel(String.valueOf(number));
            leftLabel.setStyle("-fx-background-color: #f19fff; -fx-text-fill: black;");
            grid.add(leftLabel, 0, row + 1);

            Label rightLabel = createCoordinateLabel(String.valueOf(number));
            rightLabel.setStyle("-fx-background-color: #f19fff; -fx-text-fill: black;");
            grid.add(rightLabel, BOARD_SIZE + 1, row + 1);
        }

        // generovani sachovnice
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane square = createSquare(row, col);
                squarePanes[row][col] = square; // Uložíme si reference na políčka
                grid.add(square, col + 1, row + 1);
            }
        }

        int totalWidth = (TILE_SIZE * BOARD_SIZE) + (BORDER_SIZE * 2);
        int totalHeight = (TILE_SIZE * BOARD_SIZE) + (BORDER_SIZE * 2);

        Scene scene = new Scene(grid, totalWidth, totalHeight);

        primaryStage.setTitle("Java Chess - Klikací šachovnice");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void initializeBoardData() {


        // cerne figurky
        boardState[0][0] = new Rook(false);
        boardState[0][7] = new Rook(false);

        boardState[0][3] = new Queen(false);
        boardState[0][4] = new King(false);

        boardState[0][2] = new Bishop(false);
        boardState[0][5] = new Bishop(false);

        boardState[0][1] = new Knight(false);
        boardState[0][6] = new Knight(false);

        // cyklus na pawn
        for (int i = 0; i < 8; i++) {
            boardState[1][i] = new Pawn(false);
        }


        // cyklus na pawn akorat bile
        for (int i = 0; i < 8; i++) {
            boardState[6][i] = new Pawn(true);
        }

        //bile figurky
        boardState[7][1] = new Knight(true);
        boardState[7][6] = new Knight(true);

        boardState[7][0] = new Rook(true);
        boardState[7][7] = new Rook(true);

        boardState[7][3] = new Queen(true);
        boardState[7][4] = new King(true);

        boardState[7][2] = new Bishop(true);
        boardState[7][5] = new Bishop(true);
    }


    // vytvoreni policka na sachovnici
    private StackPane createSquare(int row, int col) {
        StackPane square = new StackPane();
        square.setPrefSize(TILE_SIZE, TILE_SIZE);
        square.setAlignment(Pos.CENTER);

        updateSquareVisual(square, row, col);

        // Reakce na kliknutí myší na políčko




        square.setOnMouseClicked(event -> handleSquareClick(row, col));

        return square;
    }

    // Logika kliknutí a pohybování figurkou
    private void handleSquareClick(int row, int col) {
        if (selectedRow == -1 && selectedCol == -1) {

            // vyber figurky
            Piece clickedPiece = boardState[row][col];

            // podle toho koho je turn zjistime jestli spravna barva
            if (clickedPiece != null) {

                if (clickedPiece.isWhite() == isOnMove) {
                    selectedRow = row;
                    selectedCol = col;
                    squarePanes[row][col].setStyle("-fx-background-color: #ffeb3b");
                } else {
                    System.out.println("Spatna barva!");
                }
            }

            } else {

                Piece movingPiece = boardState[selectedRow][selectedCol];

                if (movingPiece.isValidMove(selectedRow, selectedCol, row, col, boardState)) {

                    boardState[row][col] = movingPiece;
                    boardState[selectedRow][selectedCol] = null;

                    isOnMove = !isOnMove;


                    if (isOnMove){
                        topLabel.setText("Bily na tahu");
                    } else {
                        topLabel.setText("Cerny na tahu");
                    }

            }
            

            updateSquareVisual(squarePanes[selectedRow][selectedCol], selectedRow, selectedCol);
            updateSquareVisual(squarePanes[row][col], row, col);


            // reset
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    // Aktualizace grafické podoby políčka (barva + obrázek figurky)
    private void updateSquareVisual(StackPane square, int row, int col) {
        square.getChildren().clear();

        boolean isLight = (row + col) % 2 == 0;
        String defaultBg = isLight ? "#ffffff" : "#191919";
        square.setStyle("-fx-background-color: " + defaultBg + ";");

        Piece piece = boardState[row][col];
        if (piece != null && piece.getImage() != null) {
            ImageView imageView = new ImageView(piece.getImage());
            imageView.setFitWidth(TILE_SIZE * 0.85);
            imageView.setFitHeight(TILE_SIZE * 0.85);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            square.getChildren().add(imageView);
        }
    }

    private Label createCoordinateLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(BORDER_SIZE, BORDER_SIZE);

        if (text.matches("[A-H]")) {
            label.setPrefWidth(TILE_SIZE);
        } else {
            label.setPrefHeight(TILE_SIZE);
        }
        return label;
    }

    public static void main(String[] args) {
        launch(args);
    }
}