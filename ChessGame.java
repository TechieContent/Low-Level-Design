import java.util.*;

// Enum for color
enum Color {
    WHITE, BLACK
}

// Move (from-square to to-square)
class Move {
    public final int fromRow, fromCol, toRow, toCol;
    public Move(int fr, int fc, int tr, int tc) {
        fromRow = fr;
        fromCol = fc;
        toRow = tr;
        toCol = tc;
    }
}

// Abstract chess piece
abstract class Piece {
    protected Color color;
    public Piece(Color color) { this.color = color; }
    public Color getColor() { return color; }
    // Returns true if move is legal (doesn't check check conditions here)
    public abstract boolean isValidMove(Board board, int fromRow, int fromCol, int toRow, int toCol);
    public abstract String toString();
}

// Concrete subclasses for King, Queen, Rook, Bishop, Knight, Pawn
class King extends Piece {
    public King(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        return Math.abs(fr-tr) <= 1 && Math.abs(fc-tc) <= 1;
    }
    public String toString() { return color == Color.WHITE ? "♔" : "♚"; }
}
class Queen extends Piece {
    public Queen(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = tr - fr, dc = tc - fc;
        return (dr==0 || dc==0 || Math.abs(dr)==Math.abs(dc)) && b.isPathClear(fr,fc,tr,tc);
    }
    public String toString() { return color == Color.WHITE ? "♕" : "♛"; }
}
class Rook extends Piece {
    public Rook(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = tr - fr, dc = tc - fc;
        return (dr==0 || dc==0) && b.isPathClear(fr,fc,tr,tc);
    }
    public String toString() { return color == Color.WHITE ? "♖" : "♜"; }
}
class Bishop extends Piece {
    public Bishop(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = tr - fr, dc = tc - fc;
        return Math.abs(dr)==Math.abs(dc) && b.isPathClear(fr,fc,tr,tc);
    }
    public String toString() { return color == Color.WHITE ? "♗" : "♝"; }
}
class Knight extends Piece {
    public Knight(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr-fr), dc = Math.abs(tc-fc);
        return (dr==2 && dc==1) || (dr==1 && dc==2);
    }
    public String toString() { return color == Color.WHITE ? "♘" : "♞"; }
}
class Pawn extends Piece {
    public Pawn(Color color) { super(color); }
    public boolean isValidMove(Board b, int fr, int fc, int tr, int tc) {
        int direction = (color == Color.WHITE) ? 1 : -1;
        if (fc == tc) { // forward move
            if (tr-fr == direction && b.getPiece(tr,tc) == null)
                return true;
            if ((color == Color.WHITE && fr==1 || color==Color.BLACK && fr==6) && tr-fr==2*direction && b.getPiece(fr+direction,fc)==null && b.getPiece(tr,tc)==null)
                return true;
        } else if (Math.abs(tc-fc)==1 && tr-fr==direction) { // capture
            return b.getPiece(tr,tc) != null && b.getPiece(tr,tc).getColor() != color;
        }
        return false;
    }
    public String toString() { return color == Color.WHITE ? "♙" : "♟"; }
}

// Board (8x8 chess)
class Board {
    private Piece[][] squares = new Piece[8][8];
    public Board() { setup(); }
    public Piece getPiece(int row, int col) {
        if (!onBoard(row, col)) return null;
        return squares[row][col];
    }
    public void setPiece(int row, int col, Piece p) {
        if (onBoard(row, col)) squares[row][col] = p;
    }
    public static boolean onBoard(int row, int col) {
        return row >= 0 && col >=0 && row < 8 && col < 8;
    }
    // Checks path is clear for linear piece moves
    public boolean isPathClear(int fr, int fc, int tr, int tc) {
        int dr = Integer.compare(tr, fr), dc = Integer.compare(tc, fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (getPiece(r, c) != null) return false;
            r += dr; c += dc;
        }
        return true;
    }
    // Standard starting position
    private void setup() {
        for (int c=0; c<8; c++) {
            setPiece(1, c, new Pawn(Color.WHITE));
            setPiece(6, c, new Pawn(Color.BLACK));
        }
        setPiece(0,0,new Rook(Color.WHITE)); setPiece(0,7,new Rook(Color.WHITE));
        setPiece(7,0,new Rook(Color.BLACK)); setPiece(7,7,new Rook(Color.BLACK));
        setPiece(0,1,new Knight(Color.WHITE)); setPiece(0,6,new Knight(Color.WHITE));
        setPiece(7,1,new Knight(Color.BLACK)); setPiece(7,6,new Knight(Color.BLACK));
        setPiece(0,2,new Bishop(Color.WHITE)); setPiece(0,5,new Bishop(Color.WHITE));
        setPiece(7,2,new Bishop(Color.BLACK)); setPiece(7,5,new Bishop(Color.BLACK));
        setPiece(0,3,new Queen(Color.WHITE)); setPiece(0,4,new King(Color.WHITE));
        setPiece(7,3,new Queen(Color.BLACK)); setPiece(7,4,new King(Color.BLACK));
    }
    public void printBoard() {
        for(int r=7;r>=0;--r) {
            System.out.print((r+1)+" ");
            for(int c=0;c<8;++c) {
                Piece p=getPiece(r,c);
                System.out.print((p!=null ? p.toString() : ".")+" ");
            }
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
    }
    public boolean hasKing(Color color) {
        for(int r=0;r<8;++r) for(int c=0;c<8;++c) {
            Piece p=getPiece(r,c);
            if (p instanceof King && p.getColor()==color) return true;
        }
        return false;
    }
}

// Player
class Player {
    private Color color;
    public Player(Color c) { color = c; }
    public Color getColor() { return color; }
}

// Game class: runs the loop, manages turn and state
class Game {
    Board board;
    Player[] players = new Player[2];
    int turn = 0; // 0=White, 1=Black
    boolean gameover = false;

    public Game() {
        board = new Board();
        players[0]=new Player(Color.WHITE);
        players[1]=new Player(Color.BLACK);
    }

    public void play() {
        Scanner scanner = new Scanner(System.in);
        while (!gameover) {
            board.printBoard();
            Player curr = players[turn];
            System.out.println(curr.getColor() + "'s move (e.g., e2 e4): ");
            String fromS = scanner.next();
            String toS = scanner.next();
            int fr = fromS.charAt(1)-'1', fc = fromS.charAt(0)-'a';
            int tr = toS.charAt(1)-'1', tc = toS.charAt(0)-'a';

            Piece p = board.getPiece(fr, fc);
            if (p == null || p.getColor()!= curr.getColor()) {
                System.out.println("No valid piece at that position.");
                continue;
            }
            if (!p.isValidMove(board, fr, fc, tr, tc)) {
                System.out.println("Invalid move for that piece.");
                continue;
            }
            Piece captured = board.getPiece(tr,tc);
            board.setPiece(tr,tc,p); board.setPiece(fr,fc,null);
            // Check if move leaves own king captured
            if (!board.hasKing(curr.getColor())) {
                System.out.println("Illegal move: exposes your King! Undoing...");
                board.setPiece(fr,fc,p); board.setPiece(tr,tc,captured);
                continue;
            }
            if (!board.hasKing(players[1-turn].getColor())) {
                System.out.println(curr.getColor() + " wins!");
                gameover = true;
                board.printBoard();
            }
            turn = 1-turn;
        }
    }
}

// ---- DEMO ----
public class ChessDemo {
    public static void main(String[] args) {
        System.out.println("Simple Chess (Type moves like e2 e4 to move pawn from e2 to e4)");
        new Game().play();
    }
}
