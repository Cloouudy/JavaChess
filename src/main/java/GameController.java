import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameController {

    // Prvky napojene z GameView.fxml (Scene Builder)
    @FXML private StackPane boardContainer;
    @FXML private Label statusLabel;
    @FXML private Button resignButton;
    @FXML private ListView<String> moveHistoryList;

    private static final int BORDER_SIZE = 28;
    private static final int BOARD_SIZE = 8;
    private double currentTileSize = 75;

    private GridPane grid;
    private final List<Label> colLabels = new ArrayList<>();
    private final List<Label> rowLabels = new ArrayList<>();

    // obrazek na vykresleni moznych tahu
    private final javafx.scene.image.Image moveDotImage =
            new javafx.scene.image.Image(getClass().getResourceAsStream("/moveDot.png"));

    // Stav hry
    private boolean isOnMove = true;
    private boolean gameOver;
    private boolean resign = false;
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
    private int enPassantRow = -1;
    private int enPassantCol = -1;

    // Historie tahu pro PGN
    private final List<String> sanMoves = new ArrayList<>();
    private String gameResult = "*";


    // sachovnice
    @FXML
    public void initialize() {
        initializeBoardData();
        buildChessGrid();

        // Vlozeni sachovnice do leveho kontejneru ze Scene Builderu
        boardContainer.getChildren().add(grid);

        // Responzivita pri zmene velikosti okna
        boardContainer.widthProperty().addListener((obs, oldVal, newVal) -> resizeBoard());
        boardContainer.heightProperty().addListener((obs, oldVal, newVal) -> resizeBoard());

        refreshBoard();
        updateGameStatus();
    }





    private void buildChessGrid() {
        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        String borderStyle = "-fx-background-color: #262421; -fx-text-fill: #bababa;";

        // Rohove ctverecky
        int[][] corners = {{0, 0}, {BOARD_SIZE + 1, 0}, {0, BOARD_SIZE + 1}, {BOARD_SIZE + 1, BOARD_SIZE + 1}};
        for (int[] c : corners) {
            Pane corner = new Pane();
            corner.setPrefSize(BORDER_SIZE, BORDER_SIZE);
            corner.setStyle(borderStyle);
            grid.add(corner, c[0], c[1]);
        }

        // Popisky sloupcu (A - H) nahore i dole
        for (int col = 0; col < BOARD_SIZE; col++) {
            String letter = String.valueOf((char) ('A' + col));
            Label topLbl = createCoordinateLabel(letter, borderStyle);
            Label botLbl = createCoordinateLabel(letter, borderStyle);
            colLabels.add(topLbl);
            colLabels.add(botLbl);
            grid.add(topLbl, col + 1, 0);
            grid.add(botLbl, col + 1, BOARD_SIZE + 1);
        }

        // Popisky radku (8 - 1) nalevo i napravo
        for (int row = 0; row < BOARD_SIZE; row++) {
            String number = String.valueOf(8 - row);
            Label leftLbl = createCoordinateLabel(number, borderStyle);
            Label rightLbl = createCoordinateLabel(number, borderStyle);
            rowLabels.add(leftLbl);
            rowLabels.add(rightLbl);
            grid.add(leftLbl, 0, row + 1);
            grid.add(rightLbl, BOARD_SIZE + 1, row + 1);
        }

        // 64 policek
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane square = createSquare(row, col);
                squarePanes[row][col] = square;
                grid.add(square, col + 1, row + 1);
            }
        }
    }




    // Dynamicky prepocet velikosti sachovnice podle okna
    private void resizeBoard() {
        double available = Math.min(boardContainer.getWidth(), boardContainer.getHeight()) - 30;
        if (available <= 100) return;

        currentTileSize = (available - (BORDER_SIZE * 2)) / BOARD_SIZE;

        for (Label l : colLabels) l.setPrefWidth(currentTileSize);
        for (Label l : rowLabels) l.setPrefHeight(currentTileSize);

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                squarePanes[r][c].setPrefSize(currentTileSize, currentTileSize);
                if (!squarePanes[r][c].getChildren().isEmpty()
                        && squarePanes[r][c].getChildren().get(0) instanceof ImageView iv) {
                    iv.setFitWidth(currentTileSize * 0.85);
                    iv.setFitHeight(currentTileSize * 0.85);
                }
            }
        }
    }



    // Resign button
    @FXML
    private void onResignClick() throws Exception {
        System.out.println("Resign");
        resign = true;

        updateGameStatus();
    }



    // Zpatky do menu button
    @FXML
    private void onBackToMenuClick() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/MainMenu.fxml"));
        Stage stage = (Stage) boardContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }



    // Logika hry

    // inicializace sachovnice (figurky)
    private void initializeBoardData() {

        // tmave
        boardState[0][0] = new Rook(false);
        boardState[0][1] = new Knight(false);
        boardState[0][2] = new Bishop(false);
        boardState[0][3] = new Queen(false);
        boardState[0][4] = new King(false);
        boardState[0][5] = new Bishop(false);
        boardState[0][6] = new Knight(false);
        boardState[0][7] = new Rook(false);

        // pawn cyklus
        for (int col = 0; col < BOARD_SIZE; col++) {
            boardState[1][col] = new Pawn(false);
            boardState[6][col] = new Pawn(true);
        }

        // svetle
        boardState[7][0] = new Rook(true);
        boardState[7][1] = new Knight(true);
        boardState[7][2] = new Bishop(true);
        boardState[7][3] = new Queen(true);
        boardState[7][4] = new King(true);
        boardState[7][5] = new Bishop(true);
        boardState[7][6] = new Knight(true);
        boardState[7][7] = new Rook(true);
    }

    // jednotlive policko
    private StackPane createSquare(int row, int col) {
        StackPane square = new StackPane();
        square.setPrefSize(currentTileSize, currentTileSize);
        square.setAlignment(Pos.CENTER);
        square.setOnMouseClicked(event -> handleSquareClick(row, col));
        return square;
    }



    // kliknuti na policko
    private void handleSquareClick(int row, int col) {
        if (gameOver) return;

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

        if (selectedRow == -1) return;

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


    // vybrani figurky
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


    // clear selekce
    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        legalHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];
        captureHighlights = new boolean[BOARD_SIZE][BOARD_SIZE];
    }

    // tah
    private void playMove(int startRow, int startCol, int targetRow, int targetCol, String promotion) {
        Piece movingPiece = boardState[startRow][startCol];
        Piece capturedPiece = boardState[targetRow][targetCol];
        boolean enPassantCapture = isEnPassantMove(startRow, startCol, targetRow, targetCol);
        boolean castling = isCastlingMove(movingPiece, startRow, startCol, targetRow, targetCol);

        // Sestaveni PGN zapisu tahu pred zmenou pole
        String moveNotation = buildSanNotation(movingPiece, startRow, startCol, targetRow, targetCol,
                capturedPiece != null || enPassantCapture, castling, promotion);

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

        // Pridani znaku sachu (+) nebo matu (#) a zapis do praveho panelu
        if (gameOver && isInCheck(isOnMove)) {
            moveNotation += "#";
        } else if (isInCheck(isOnMove)) {
            moveNotation += "+";
        }
        recordMoveInHistory(moveNotation);
    }

    private String buildSanNotation(Piece piece, int sRow, int sCol, int tRow, int tCol,
                                    boolean isCapture, boolean isCastling, String promotion) {
        if (isCastling) {
            return tCol == 6 ? "O-O" : "O-O-O";
        }
        String targetSquare = "" + (char) ('a' + tCol) + (8 - tRow);
        if (piece instanceof Pawn) {
            String prefix = isCapture ? ((char) ('a' + sCol)) + "x" : "";
            String promo = promotion != null ? "=" + promotion.charAt(0) : "";
            return prefix + targetSquare + promo;
        }


        String pieceLetter = switch (piece) {
            case Knight k -> "K";
            case Queen q -> "Q";
            case Bishop b -> "B";
            case Rook r -> "R";
            default -> "K";

        };


        return pieceLetter + (isCapture ? "x" : "") + targetSquare;
    }

    private void recordMoveInHistory(String san) {
        sanMoves.add(san);
        int moveNumber = (sanMoves.size() + 1) / 2;
        if (sanMoves.size() % 2 == 1) {
            moveHistoryList.getItems().add(moveNumber + ". " + san);
        } else {
            int lastIdx = moveHistoryList.getItems().size() - 1;
            String prev = moveHistoryList.getItems().get(lastIdx);
            moveHistoryList.getItems().set(lastIdx, prev + "   " + san);
        }
        moveHistoryList.scrollTo(moveHistoryList.getItems().size() - 1);
    }

    private String choosePromotion(boolean white) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", "Queen", "Rook", "Bishop", "Knight");
        dialog.setTitle("Proměna pěšce");
        dialog.setHeaderText((white ? "Bílý" : "Černý") + " pěšec došel na konec šachovnice");
        dialog.setContentText("Vyber figurku:");
        if (boardContainer.getScene() != null) {
            dialog.initOwner(boardContainer.getScene().getWindow());
        }
        Optional<String> choice = dialog.showAndWait();
        return choice.orElse("Queen");
    }

    private Piece createPromotedPiece(String choice, boolean white) {
        if ("Rook".equals(choice)) return new Rook(white);
        if ("Bishop".equals(choice)) return new Bishop(white);
        if ("Knight".equals(choice)) return new Knight(white);
        return new Queen(white);
    }

    // -------------------------------
    //  STATUS HRY
    // -------------------------------

    private void updateGameStatus() {
        boolean inCheck = isInCheck(isOnMove);
        boolean hasLegalMove = hasAnyLegalMove(isOnMove);
        String sideToMove = isOnMove ? "Bílý" : "Černý";



        //  checkmate a stalemate
        if (!hasLegalMove) {
            gameOver = true;
            if (inCheck) {
                gameResult = isOnMove ? "0-1" : "1-0";
                statusLabel.setText("Šachmat! Vyhrává " + (isOnMove ? "Černý" : "Bílý"));
            } else {
                gameResult = "1/2-1/2";
                statusLabel.setText("Remíza - Pat");
            }
            return;
        }

        // material check
        if (isInsufficientMaterial()) {
            gameOver = true;
            gameResult = "1/2-1/2";
            statusLabel.setText("Remíza - Nedostatek materiálu");
            return;
        }

        // resign
        if (resign) {
            gameOver = true;
            gameResult = isOnMove ? "0-1" : "1-0";
            statusLabel.setText(sideToMove + " rezignoval");
            return;
        }

        gameOver = false;
        statusLabel.setText(inCheck ? "ŠACH! Na tahu: " + sideToMove : "Na tahu: " + sideToMove);
        resignButton.setText("Vzdát se? (" + sideToMove + ")");
    }

    private boolean isInsufficientMaterial() {
        int bishops = 0, knights = 0, bishopSquareColor = -1;
        boolean bishopsShareSquareColor = true;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece instanceof King) continue;
                if (piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen) return false;
                if (piece instanceof Knight) {
                    knights++;
                } else if (piece instanceof Bishop) {
                    bishops++;
                    int color = (row + col) % 2;
                    if (bishopSquareColor == -1) bishopSquareColor = color;
                    else if (bishopSquareColor != color) bishopsShareSquareColor = false;
                }
            }
        }
        int minorPieceCount = bishops + knights;
        if (minorPieceCount <= 1) return true;
        return knights == 0 && bishops > 0 && bishopsShareSquareColor;
    }

    private boolean hasAnyLegalMove(boolean white) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece.isWhite() != white) continue;
                for (int targetRow = 0; targetRow < BOARD_SIZE; targetRow++) {
                    for (int targetCol = 0; targetCol < BOARD_SIZE; targetCol++) {
                        if (isLegalMove(row, col, targetRow, targetCol, white)) return true;
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
        System.arraycopy(candidates, 0, moves, 0, count);
        return moves;
    }

    private boolean isLegalMove(int startRow, int startCol, int targetRow, int targetCol, boolean white) {
        if (!isOnBoard(startRow, startCol) || !isOnBoard(targetRow, targetCol)) return false;

        Piece movingPiece = boardState[startRow][startCol];
        Piece targetPiece = boardState[targetRow][targetCol];
        if (movingPiece == null || movingPiece.isWhite() != white
                || (targetPiece != null && (targetPiece.isWhite() == white || targetPiece instanceof King))) {
            return false;
        }

        boolean castling = isCastlingMove(movingPiece, startRow, startCol, targetRow, targetCol);
        boolean enPassantCapture = isEnPassantMove(startRow, startCol, targetRow, targetCol);
        if (castling) {
            if (!canCastle(movingPiece.isWhite(), targetCol)) return false;
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

        int rookStartCol = -1, rookTargetCol = -1;
        Piece rook = null;
        if (castling) {
            rookStartCol = targetCol == 6 ? 7 : 0;
            rookTargetCol = targetCol == 6 ? 5 : 3;
            rook = boardState[startRow][rookStartCol];
        }

        boardState[startRow][startCol] = null;
        boardState[targetRow][targetCol] = movingPiece;
        if (enPassantCapture) boardState[startRow][targetCol] = null;
        if (castling) {
            boardState[startRow][rookTargetCol] = rook;
            boardState[startRow][rookStartCol] = null;
        }

        boolean safe = !isInCheck(movingPiece.isWhite());

        boardState[startRow][startCol] = movingPiece;
        boardState[targetRow][targetCol] = targetPiece;
        if (enPassantCapture) boardState[startRow][targetCol] = enPassantPawn;
        if (castling) {
            boardState[startRow][rookStartCol] = rook;
            boardState[startRow][rookTargetCol] = null;
        }
        return safe;
    }

    private boolean canCastle(boolean white, int targetCol) {
        int homeRow = white ? 7 : 0;
        if (targetCol != 2 && targetCol != 6) return false;
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
        if (!(rook instanceof Rook) || rook.isWhite() != white) return false;

        int step = kingSide ? 1 : -1;
        for (int col = 4 + step; col != rookCol; col += step) {
            if (boardState[homeRow][col] != null) return false;
        }

        if (isSquareAttacked(homeRow, 4, !white)) return false;
        for (int col = 4 + step; ; col += step) {
            if (isSquareAttacked(homeRow, col, !white)) return false;
            if (col == targetCol) break;
        }
        return true;
    }

    private boolean isCastlingMove(Piece piece, int startRow, int startCol, int targetRow, int targetCol) {
        if (!(piece instanceof King)) return false;
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
        if (targetRow - startRow != direction || Math.abs(targetCol - startCol) != 1) return false;
        Piece capturedPawn = boardState[startRow][targetCol];
        return capturedPawn instanceof Pawn && capturedPawn.isWhite() != movingPiece.isWhite();
    }

    private boolean isInCheck(boolean white) {
        int kingRow = -1, kingCol = -1;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece instanceof King && piece.isWhite() == white) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) break;
        }
        return kingRow == -1 || isSquareAttacked(kingRow, kingCol, !white);
    }

    private boolean isSquareAttacked(int targetRow, int targetCol, boolean byWhite) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = boardState[row][col];
                if (piece == null || piece.isWhite() != byWhite) continue;

                int rowDistance = targetRow - row;
                int colDistance = targetCol - col;
                int absRow = Math.abs(rowDistance);
                int absCol = Math.abs(colDistance);

                if (piece instanceof Pawn) {
                    int pawnDirection = piece.isWhite() ? -1 : 1;
                    if (rowDistance == pawnDirection && absCol == 1) return true;
                } else if (piece instanceof Knight) {
                    if ((absRow == 2 && absCol == 1) || (absRow == 1 && absCol == 2)) return true;
                } else if (piece instanceof King) {
                    if ((absRow != 0 || absCol != 0) && absRow <= 1 && absCol <= 1) return true;
                } else {
                    boolean diagonal = absRow == absCol && absRow > 0;
                    boolean straight = (rowDistance == 0 || colDistance == 0)
                            && (rowDistance != 0 || colDistance != 0);
                    boolean attacksLine = piece instanceof Queen && (diagonal || straight)
                            || piece instanceof Bishop && diagonal
                            || piece instanceof Rook && straight;
                    if (attacksLine && pathIsClear(row, col, targetRow, targetCol)) return true;
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
            if (boardState[row][col] != null) return false;
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
        if (row != homeRow) return;
        if (col == 0) {
            if (white) whiteQueenSideCastle = false;
            else blackQueenSideCastle = false;
        } else if (col == 7) {
            if (white) whiteKingSideCastle = false;
            else blackKingSideCastle = false;
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
        String background = isLight ? "#eeeed2" : "#769656"; // Chess.com zelena & kremova
        
        // zvyrazneni figurky
        if (row == selectedRow && col == selectedCol) {
            background = "#f6f669";
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
            imageView.setFitWidth(currentTileSize * 0.85);
            imageView.setFitHeight(currentTileSize * 0.85);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            square.getChildren().add(imageView);
        }

        if (legalHighlights[row][col]) {
            ImageView dotView = new ImageView(moveDotImage);

            double scale = captureHighlights[row][col] ? 0.90 : 0.5;
            dotView.setFitWidth(currentTileSize * scale);
            dotView.setFitHeight(currentTileSize * scale);
            dotView.setPreserveRatio(true);
            dotView.setSmooth(true);
            dotView.setMouseTransparent(true);
            square.getChildren().add(dotView);
        }
        
        
    }

    private Label createCoordinateLabel(String text, String style) {
        Label label = new Label(text);
        label.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setPrefSize(BORDER_SIZE, BORDER_SIZE);
        label.setStyle(style);
        if (text.matches("[A-H]")) label.setPrefWidth(currentTileSize);
        else label.setPrefHeight(currentTileSize);
        return label;
    }
}