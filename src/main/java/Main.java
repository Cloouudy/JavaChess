import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;

public class Main extends Application {
    private static final int TILE_SIZE = 75;
    private static final int BORDER_SIZE = 30;
    private static final int BOARD_SIZE = 8;

    private boolean isOnMove = true;
    private boolean gameOver;
    private Label topLabel;

    private final Piece[][] boardState = new Piece[BOARD_SIZE][BOARD_SIZE];
    private final StackPane[][] squarePanes = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private boolean[][] legalHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];
    private boolean[][] captureHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];

    private int selectedRow = -1;
    private int selectedCol = -1;

    private boolean whiteKingSideCastle = true;
    private boolean whiteQueenSideCastle = true;
    private boolean blackKingSideCastle = true;
    private boolean blackQueenSideCastle = true;

    // The square an opposing pawn may move to when capturing en passant.
    private int enPassantRow = -1;
    private int enPassantCol = -1;

    @Override
    public void start(Stage primaryStage) {
        initializeBoardData();

        GridPane grid = new GridPane();
        topLabel = new Label();
        topLabel.setStyle("-fx-background-color: #f19fff; -fx-text-fill: black;");
        topLabel.setAlignment(Pos.CENTER);
        topLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
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

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane square = createSquare(row, col);
                squarePanes[row][col] = square;
                grid.add(square, col + 1, row + 1);
            }
        }

        refreshBoard();
        updateGameStatus();

        int totalWidth = TILE_SIZE * BOARD_SIZE + BORDER_SIZE * 2;
        int totalHeight = TILE_SIZE * BOARD_SIZE + BORDER_SIZE * 2;
        Scene scene = new Scene(grid, totalWidth, totalHeight);

        primaryStage.setTitle("Java Chess");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void initializeBoardData() {
        // Black back rank and pawns.
        boardState[0][0] = new Rook(false);
        boardState[0][1] = new Knight(false);
        boardState[0][2] = new Bishop(false);
        boardState[0][3] = new Queen(false);
        boardState[0][4] = new King(false);
        boardState[0][5] = new Bishop(false);
        boardState[0][6] = new Knight(false);
        boardState[0][7] = new Rook(false);
        for (int col = 0; col < BOARD_SIZE; col++) {
            boardState[1][col] = new Pawn(false);
            boardState[6][col] = new Pawn(true);
        }

        // White back rank.
        boardState[7][0] = new Rook(true);
        boardState[7][1] = new Knight(true);
        boardState[7][2] = new Bishop(true);
        boardState[7][3] = new Queen(true);
        boardState[7][4] = new King(true);
        boardState[7][5] = new Bishop(true);
        boardState[7][6] = new Knight(true);
        boardState[7][7] = new Rook(true);
    }

    private StackPane createSquare(int row, int col) {
        StackPane square = new StackPane();
        square.setPrefSize(TILE_SIZE, TILE_SIZE);
        square.setAlignment(Pos.CENTER);
        square.setOnMouseClicked(event -> handleSquareClick(row, col));
        return square;
    }

    private void handleSquareClick(int row, int col) {
        if (gameOver) {
            return;
        }

        Piece clickedPiece = boardState[row][col];
        if (clickedPiece != null && clickedPiece.isWhite() == isOnMove) {
            if (row == selectedRow && col == selectedCol) {
                clearSelection();
                refreshBoard();
            } else {
                selectPiece(row, col);
            }
            return;
        }

        if (selectedRow == -1) {
            return;
        }

        Piece movingPiece = boardState[selectedRow][selectedCol];
        if (isLegalMove(selectedRow, selectedCol, row, col, isOnMove)) {
            String promotion = null;
            if (movingPiece instanceof Pawn && row == (movingPiece.isWhite() ? 0 : 7)) {
                promotion = choosePromotion(movingPiece.isWhite());
            }
            playMove(selectedRow, selectedCol, row, col, promotion);
        } else {
            clearSelection();
            refreshBoard();
        }
    }

    private void selectPiece(int row, int col) {
        clearSelection();
        selectedRow = row;
        selectedCol = col;

        for (int[] move : getLegalMoves(row, col, isOnMove)) {
            int targetRow = move[0];
            int targetCol = move[1];
            legalHighlights[targetRow][targetCol] = true;
            captureHighlights[targetRow][targetCol] = boardState[targetRow][targetCol] != null
                    || isEnPassantMove(row, col, targetRow, targetCol);
        }
        refreshBoard();
    }

    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        legalHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];
        captureHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];
    }

    private void playMove(int startRow, int startCol, int targetRow, int targetCol, String promotion) {
        Piece movingPiece = boardState[startRow][startCol];
        Piece capturedPiece = boardState[targetRow][targetCol];
        boolean enPassantCapture = isEnPassantMove(startRow, startCol, targetRow, targetCol);
        boolean castling = isCastlingMove(movingPiece, startRow, startCol, targetRow, targetCol);

        if (capturedPiece instanceof Rook) {
            revokeRookCastleRight(capturedPiece.isWhite(), targetRow, targetCol);
        }
        if (enPassantCapture) {
            boardState[startRow][targetCol] = null;
        }

        if (movingPiece instanceof King) {
            revokeKingCastleRights(movingPiece.isWhite());
        } else if (movingPiece instanceof Rook) {
            revokeRookCastleRight(movingPiece.isWhite(), startRow, startCol);
        }

        boardState[targetRow][targetCol] = movingPiece;
        boardState[startRow][startCol] = null;

        if (castling) {
            int rookStartCol = targetCol == 6 ? 7 : 0;
            int rookTargetCol = targetCol == 6 ? 5 : 3;
            boardState[targetRow][rookTargetCol] = boardState[targetRow][rookStartCol];
            boardState[targetRow][rookStartCol] = null;
        }

        if (movingPiece instanceof Pawn && (targetRow == 0 || targetRow == 7)) {
            boardState[targetRow][targetCol] = createPromotedPiece(promotion, movingPiece.isWhite());
        }

        if (movingPiece instanceof Pawn && Math.abs(targetRow - startRow) == 2) {
            enPassantRow = (startRow + targetRow) / 2;
            enPassantCol = startCol;
        } else {
            enPassantRow = -1;
            enPassantCol = -1;
        }

        isOnMove = !isOnMove;
        clearSelection();
        refreshBoard();
        updateGameStatus();
    }

    private String choosePromotion(boolean white) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", "Queen", "Rook", "Bishop", "Knight");
        dialog.setTitle("Pawn promotion");
        dialog.setHeaderText((white ? "White" : "Black") + " pawn reached the last rank");
        dialog.setContentText("Promote to:");
        if (topLabel.getScene() != null) {
            dialog.initOwner(topLabel.getScene().getWindow());
        }
        Optional<String> choice = dialog.showAndWait();
        return choice.orElse("Queen");
    }

    private Piece createPromotedPiece(String choice, boolean white) {
        if ("Rook".equals(choice)) {
            return new Rook(white);
        }
        if ("Bishop".equals(choice)) {
            return new Bishop(white);
        }
        if ("Knight".equals(choice)) {
            return new Knight(white);
        }
        return new Queen(white);
    }

    private void updateGameStatus() {
        boolean inCheck = isInCheck(isOnMove);
        boolean hasLegalMove = hasAnyLegalMove(isOnMove);
        String sideToMove = isOnMove ? "White" : "Black";

        if (!hasLegalMove) {
            gameOver = true;
            if (inCheck) {
                topLabel.setText("Checkmate - " + (isOnMove ? "Black" : "White") + " wins");
            } else {
                topLabel.setText("Draw - stalemate");
            }
            return;
        }

        if (isInsufficientMaterial()) {
            gameOver = true;
            topLabel.setText("Draw - insufficient material");
            return;
        }

        gameOver = false;
        topLabel.setText(inCheck ? sideToMove + " is in check - " + sideToMove + " to move"
                : sideToMove + " to move");
    }

    private boolean isInsufficientMaterial() {
        int bishops = 0;
        int knights = 0;
        int bishopSquareColor = -1;
        boolean bishopsShareSquareColor = true;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece instanceof King) {
                    continue;
                }
                if (piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen) {
                    return false;
                }
                if (piece instanceof Knight) {
                    knights++;
                } else if (piece instanceof Bishop) {
                    bishops++;
                    int color = (row + col) % 2;
                    if (bishopSquareColor == -1) {
                        bishopSquareColor = color;
                    } else if (bishopSquareColor != color) {
                        bishopsShareSquareColor = false;
                    }
                }
            }
        }

        int minorPieceCount = bishops + knights;
        if (minorPieceCount <= 1) {
            return true;
        }

        // Bishops confined to one square color can never force or assist a mate.
        return knights == 0 && bishops > 0 && bishopsShareSquareColor;
    }

    private boolean hasAnyLegalMove(boolean white) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece.isWhite() != white) {
                    continue;
                }
                for (int targetRow = 0; targetRow < BOARD_SIZE; targetRow++) {
                    for (int targetCol = 0; targetCol < BOARD_SIZE; targetCol++) {
                        if (isLegalMove(row, col, targetRow, targetCol, white)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int[][] getLegalMoves(int startRow, int startCol, boolean white) {
        int[][] candidates = new int[BOARD_SIZE * BOARD_SIZE][2];
        int count = 0;
        for (int targetRow = 0; targetRow < BOARD_SIZE; targetRow++) {
            for (int targetCol = 0; targetCol < BOARD_SIZE; targetCol++) {
                if (isLegalMove(startRow, startCol, targetRow, targetCol, white)) {
                    candidates[count][0] = targetRow;
                    candidates[count][1] = targetCol;
                    count++;
                }
            }
        }

        int[][] moves = new int[count][2];
        for (int i = 0; i < count; i++) {
            moves[i] = candidates[i];
        }
        return moves;
    }

    private boolean isLegalMove(int startRow, int startCol, int targetRow, int targetCol, boolean white) {
        if (!isOnBoard(startRow, startCol) || !isOnBoard(targetRow, targetCol)) {
            return false;
        }

        Piece movingPiece = boardState[startRow][startCol];
        Piece targetPiece = boardState[targetRow][targetCol];
        if (movingPiece == null || movingPiece.isWhite() != white
                || (targetPiece != null && (targetPiece.isWhite() == white || targetPiece instanceof King))) {
            return false;
        }

        boolean castling = isCastlingMove(movingPiece, startRow, startCol, targetRow, targetCol);
        boolean enPassantCapture = isEnPassantMove(startRow, startCol, targetRow, targetCol);
        if (castling) {
            if (!canCastle(movingPiece.isWhite(), targetCol)) {
                return false;
            }
        } else if (!enPassantCapture
                && !movingPiece.isValidMove(startRow, startCol, targetRow, targetCol, boardState)) {
            return false;
        }

        return moveLeavesKingSafe(startRow, startCol, targetRow, targetCol, castling, enPassantCapture);
    }

    private boolean moveLeavesKingSafe(int startRow, int startCol, int targetRow, int targetCol,
                                       boolean castling, boolean enPassantCapture) {
        Piece movingPiece = boardState[startRow][startCol];
        Piece targetPiece = boardState[targetRow][targetCol];
        Piece enPassantPawn = enPassantCapture ? boardState[startRow][targetCol] : null;

        int rookStartCol = -1;
        int rookTargetCol = -1;
        Piece rook = null;
        if (castling) {
            rookStartCol = targetCol == 6 ? 7 : 0;
            rookTargetCol = targetCol == 6 ? 5 : 3;
            rook = boardState[startRow][rookStartCol];
        }

        boardState[startRow][startCol] = null;
        boardState[targetRow][targetCol] = movingPiece;
        if (enPassantCapture) {
            boardState[startRow][targetCol] = null;
        }
        if (castling) {
            boardState[startRow][rookTargetCol] = rook;
            boardState[startRow][rookStartCol] = null;
        }

        boolean safe = !isInCheck(movingPiece.isWhite());

        boardState[startRow][startCol] = movingPiece;
        boardState[targetRow][targetCol] = targetPiece;
        if (enPassantCapture) {
            boardState[startRow][targetCol] = enPassantPawn;
        }
        if (castling) {
            boardState[startRow][rookStartCol] = rook;
            boardState[startRow][rookTargetCol] = null;
        }
        return safe;
    }

    private boolean canCastle(boolean white, int targetCol) {
        int homeRow = white ? 7 : 0;
        if (targetCol != 2 && targetCol != 6) {
            return false;
        }
        boolean kingSide = targetCol == 6;
        boolean hasRight = white
                ? (kingSide ? whiteKingSideCastle : whiteQueenSideCastle)
                : (kingSide ? blackKingSideCastle : blackQueenSideCastle);
        if (!hasRight || !(boardState[homeRow][4] instanceof King)
                || boardState[homeRow][4].isWhite() != white) {
            return false;
        }

        int rookCol = kingSide ? 7 : 0;
        Piece rook = boardState[homeRow][rookCol];
        if (!(rook instanceof Rook) || rook.isWhite() != white) {
            return false;
        }

        int step = kingSide ? 1 : -1;
        for (int col = 4 + step; col != rookCol; col += step) {
            if (boardState[homeRow][col] != null) {
                return false;
            }
        }

        if (isSquareAttacked(homeRow, 4, !white)) {
            return false;
        }
        for (int col = 4 + step; ; col += step) {
            if (isSquareAttacked(homeRow, col, !white)) {
                return false;
            }
            if (col == targetCol) {
                break;
            }
        }
        return true;
    }

    private boolean isCastlingMove(Piece piece, int startRow, int startCol, int targetRow, int targetCol) {
        if (!(piece instanceof King)) {
            return false;
        }
        int homeRow = piece.isWhite() ? 7 : 0;
        return startRow == homeRow && startCol == 4 && targetRow == homeRow
                && (targetCol == 2 || targetCol == 6);
    }

    private boolean isEnPassantMove(int startRow, int startCol, int targetRow, int targetCol) {
        Piece movingPiece = boardState[startRow][startCol];
        if (!(movingPiece instanceof Pawn) || targetRow != enPassantRow || targetCol != enPassantCol
                || boardState[targetRow][targetCol] != null) {
            return false;
        }

        int direction = movingPiece.isWhite() ? -1 : 1;
        if (targetRow - startRow != direction || Math.abs(targetCol - startCol) != 1) {
            return false;
        }
        Piece capturedPawn = boardState[startRow][targetCol];
        return capturedPawn instanceof Pawn && capturedPawn.isWhite() != movingPiece.isWhite();
    }

    private boolean isInCheck(boolean white) {
        int kingRow = -1;
        int kingCol = -1;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece instanceof King && piece.isWhite() == white) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) {
                break;
            }
        }
        return kingRow == -1 || isSquareAttacked(kingRow, kingCol, !white);
    }

    private boolean isSquareAttacked(int targetRow, int targetCol, boolean byWhite) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece.isWhite() != byWhite) {
                    continue;
                }

                int rowDistance = targetRow - row;
                int colDistance = targetCol - col;
                int absRow = Math.abs(rowDistance);
                int absCol = Math.abs(colDistance);

                if (piece instanceof Pawn) {
                    int pawnDirection = piece.isWhite() ? -1 : 1;
                    if (rowDistance == pawnDirection && absCol == 1) {
                        return true;
                    }
                } else if (piece instanceof Knight) {
                    if ((absRow == 2 && absCol == 1) || (absRow == 1 && absCol == 2)) {
                        return true;
                    }
                } else if (piece instanceof King) {
                    if ((absRow != 0 || absCol != 0) && absRow <= 1 && absCol <= 1) {
                        return true;
                    }
                } else {
                    boolean diagonal = absRow == absCol && absRow > 0;
                    boolean straight = (rowDistance == 0 || colDistance == 0)
                            && (rowDistance != 0 || colDistance != 0);
                    boolean attacksLine = piece instanceof Queen && (diagonal || straight)
                            || piece instanceof Bishop && diagonal
                            || piece instanceof Rook && straight;
                    if (attacksLine && pathIsClear(row, col, targetRow, targetCol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean pathIsClear(int startRow, int startCol, int targetRow, int targetCol) {
        int rowStep = Integer.compare(targetRow, startRow);
        int colStep = Integer.compare(targetCol, startCol);
        int row = startRow + rowStep;
        int col = startCol + colStep;
        while (row != targetRow || col != targetCol) {
            if (boardState[row][col] != null) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    private void revokeKingCastleRights(boolean white) {
        if (white) {
            whiteKingSideCastle = false;
            whiteQueenSideCastle = false;
        } else {
            blackKingSideCastle = false;
            blackQueenSideCastle = false;
        }
    }

    private void revokeRookCastleRight(boolean white, int row, int col) {
        int homeRow = white ? 7 : 0;
        if (row != homeRow) {
            return;
        }
        if (col == 0) {
            if (white) {
                whiteQueenSideCastle = false;
            } else {
                blackQueenSideCastle = false;
            }
        } else if (col == 7) {
            if (white) {
                whiteKingSideCastle = false;
            } else {
                blackKingSideCastle = false;
            }
        }
    }

    private boolean isOnBoard(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    private void refreshBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                updateSquareVisual(squarePanes[row][col], row, col);
            }
        }
    }

    private void updateSquareVisual(StackPane square, int row, int col) {
        square.getChildren().clear();

        boolean isLight = (row + col) % 2 == 0;
        String background = isLight ? "#ffffff" : "#191919";
        if (legalHighlights[row][col]) {
            background = captureHighlights[row][col] ? "#e28b7a" : "#a9d18e";
        }
        if (row == selectedRow && col == selectedCol) {
            background = "#ffeb3b";
        }

        Piece piece = boardState[row][col];
        boolean checkedKing = piece instanceof King && isInCheck(piece.isWhite());
        String style = "-fx-background-color: " + background + ";";
        if (checkedKing) {
            style += " -fx-border-color: #e53935; -fx-border-width: 4;";
        }
        square.setStyle(style);

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
