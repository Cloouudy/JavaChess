

import javafx.scene.image.Image;

public class Queen extends Piece {

    private final Image image;

    public Queen(boolean isWhite) {
        super(isWhite);

        // Dynamická cesta podle barvy a přesného názvu souboru (.webp)
        String imagePath;

        if (isWhite) {
            imagePath = "/pieces_skin/skin01/white/L01queen.png";
        } else {
            imagePath = "/pieces_skin/skin01/black/D01queen.png";
        }
        this.image = new Image(getClass().getResourceAsStream(imagePath));
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int targetRow, int targetCol, Piece[][] board) {
        int dRow = Math.abs(targetRow - startRow);
        int dCol = Math.abs(targetCol - startCol);

        // Dáma se pohybuje buď po přímce (jako věž), nebo po diagonále (jako střelec)
        boolean isStraight = (startRow == targetRow || startCol == targetCol);
        boolean isDiagonal = (dRow == dCol);

        if (!isStraight && !isDiagonal) {
            return false;
        }

        // Směr krokování (-1, 0, nebo 1)
        int rowStep = Integer.compare(targetRow, startRow);
        int colStep = Integer.compare(targetCol, startCol);

        // Kontrola, zda je cesta k cíli volná
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;

        while (currentRow != targetRow || currentCol != targetCol) {
            if (board[currentRow][currentCol] != null) {
                return false; // Na cestě stojí figurka
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        // Cílové políčko musí být prázdné nebo na něm stojí soupeřova figurka
        Piece targetPiece = board[targetRow][targetCol];
        return targetPiece == null || targetPiece.isWhite() != isWhite();
    }
}