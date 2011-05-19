/*
 *   Copyright (C) 2011 Jean-Francois Romang <info at chesspad dot net>
 *   This file is part of ChessPad.
 *
 *   ChessPad is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   ChessPad is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with ChessPad.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.chesspad;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A move.
 * @author Jean-Francois Romang <info at chesspad dot net>
 *
 */
public class Move implements java.io.Serializable {

	private static final long serialVersionUID = 5802562010518043883L;

	// ♔ ♚ ♕ ♛ ♖ ♜ ♗ ♝ ♘ ♞ ♙ ♟
	// http://en.wikipedia.org/wiki/Algebraic_chess_notation
	// http://en.wikipedia.org/wiki/Chess_symbols_in_Unicode

	public int movingPiece, capturedPiece, promotionPiece;

	public int from, to;
	public String algebraicNotation=null;
	private static Pattern movePattern;
	private static char pieceName[] = { 'X', 'X', 'n', 'b', 'r', 'q', 'X', 'X',
			'n', 'b', 'r', 'q', 'X' };

	static {
		movePattern = Pattern
				.compile("^([a-h]{1,1}[1-8]{1,1})([a-h]{1,1}[1-8]{1,1})([nbrq]?)$");
	}

	public Move(int piece, int from, int to) {
		this.movingPiece = piece;
		this.from = from;
		this.to = to;
		this.capturedPiece = Position.EMPTY;
		this.promotionPiece = Position.EMPTY;

	}

	public Move(int piece, int from, int to, int capture, int promotion) {
		this.movingPiece = piece;
		this.from = from;
		this.to = to;
		this.capturedPiece = capture;
		this.promotionPiece = promotion;

	}

	Move(String moveString, Position p) {
		final Matcher m = movePattern.matcher(moveString);
		if (!m.matches()) {
			// TODO
			System.out.println("move does not match:" + moveString);
			// Log.e("Move","move does not match:"+moveString);
			movingPiece = capturedPiece = promotionPiece = Position.EMPTY;
			from = to = -1;
			return;
		}

		from = (m.group(1).charAt(0) - 'a')
				+ ((8 - Character.getNumericValue(m.group(1).charAt(1))) * 8);
		to = (m.group(2).charAt(0) - 'a')
				+ ((8 - Character.getNumericValue(m.group(2).charAt(1))) * 8);

		movingPiece = p.pieceAt(from); // movingPiece
		capturedPiece = p.pieceAt(to); // capturedPiece

		// enPassant capture
		if (to == p.enPassantSquare && movingPiece == Position.W_PAWN)
			capturedPiece = Position.B_PAWN;
		if (to == p.enPassantSquare && movingPiece == Position.B_PAWN)
			capturedPiece = Position.W_PAWN;

		// promotionPiece
		promotionPiece = Position.EMPTY;
		if (m.group(3).length() != 0)
			switch (m.group(3).charAt(0)) {
			case 'n':
				promotionPiece = (movingPiece == Position.W_PAWN) ? Position.W_KNIGHT
						: Position.B_KNIGHT;
				break;
			case 'b':
				promotionPiece = (movingPiece == Position.W_PAWN) ? Position.W_BISHOP
						: Position.B_BISHOP;
				break;
			case 'r':
				promotionPiece = (movingPiece == Position.W_PAWN) ? Position.W_ROOK
						: Position.B_ROOK;
				break;
			case 'q':
				promotionPiece = (movingPiece == Position.W_PAWN) ? Position.W_QUEEN
						: Position.B_QUEEN;
				break;
			default:
				break;
			}
	}

	@Override
	public boolean equals(Object obj) {
		// Vérification de l'égalité des références
		if (obj == this)
			return true;
		// Vérification du type du paramètre
		if (obj instanceof Move) {
			// Vérification des valeurs des attributs
			final Move other = (Move) obj;
			return (from == other.from && to == other.to
					&& movingPiece == other.movingPiece
					&& capturedPiece == other.capturedPiece && promotionPiece == other.promotionPiece);
		}
		return false;
	}

	@Override
	public String toString() {
		String moveString = "";
		moveString += Position.squareName[from];
		moveString += Position.squareName[to];
		if (promotionPiece != Position.EMPTY)
			moveString += pieceName[promotionPiece];
		return moveString;
	}
	
	// ♔ ♚ ♕ ♛ ♖ ♜ ♗ ♝ ♘ ♞ ♙ ♟
	private static final String pieceNames[]={ "", "", "♞", "♝", "♜", "♛", "♚", "", "♞", "♝", "♜", "♛", "♚"};
	//private static final String pieceNames[]={ "", "", "♘", "♗", "♖", "♕", "♔", "", "♘", "♗", "♖", "♕", "♔"};
	//private static final String pieceNames[]={ "", "", "N", "B", "R", "Q", "K", "", "N", "B", "R", "Q", "K"};
    private static final String filesName[]={"a","b","c","d","e","f","g","h"};
	
	public String toAlgebraicNotation(Position p)
	{
		if(algebraicNotation!=null) return algebraicNotation;
		
	    String moveString=pieceNames[movingPiece]+"";
	    Vector<Move> moves=p.getLegalMoves();

	    //castling
	    if(movingPiece==Position.W_KING && from==Position.E1 && to==Position.G1) return algebraicNotation="0-0";
	    if(movingPiece==Position.W_KING && from==Position.E1 && to==Position.C1) return algebraicNotation="0-0-0";
	    if(movingPiece==Position.B_KING && from==Position.E8 && to==Position.G8) return algebraicNotation="0-0";
	    if(movingPiece==Position.B_KING && from==Position.E8 && to==Position.C8) return algebraicNotation="0-0-0";

	    //desambiguisation ici
	    if(movingPiece!=Position.W_PAWN && movingPiece!=Position.B_PAWN)
	    {
	        Vector<Integer> identicalPieces=new Vector<Integer>();
	        for(Move m :moves)
	        {
	            if(m.movingPiece==movingPiece && m.to==to && !identicalPieces.contains(m.from) && m.from!=from)
	                identicalPieces.add(from);
	        }

	        if(!identicalPieces.isEmpty())
	        {
	            boolean sameRank=false;
	            boolean sameFile=false;
	            for(int i : identicalPieces)
	            {
	                if((i%8) == (from%8)) sameFile=true;
	                if((i/8) == (from/8)) sameRank=true;
	            }
	            if(sameRank && !sameFile) moveString+=filesName[from%8];
	            else if(sameFile && !sameRank) moveString+=Integer.toString(8-(from/8));
	            else moveString+=filesName[from%8]+Integer.toString(8-(from/8));
	        }
	    }

	    //capture
	    if(capturedPiece!=Position.EMPTY)
	    {
	        if(movingPiece==Position.W_PAWN || movingPiece==Position.B_PAWN)
	        {
	            moveString+=filesName[from%8];
	        }
	        moveString+="x";
	    }

	    //destination
	    moveString+=Position.squareName[to];

	    //en passant
	    if((capturedPiece!=Position.EMPTY) && p.pieceAt(to)==Position.EMPTY)
	        moveString+=" e.p.";

	    //promotion
	    if(promotionPiece!=Position.EMPTY)
	        moveString+=pieceNames[promotionPiece];

	    //TODO check & checkmate
		p.makeMove(this);
		if(p.isAttacked(p.kingSquare(p.sideToPlay()), p.sideToPlay() ? Position.BLACK : Position.WHITE))
			moveString+="+"; //TODO checkmate
	    /*
	    Position testPosition=(Position)p.clone();
	    testPosition.makeMove(this);

	    if(testPosition.isCheckMated()) moveString+="#";
	    else if(testPosition.isInCheck()) moveString+="+";*/


	    algebraicNotation=moveString;
	    return moveString;
	}
	
	public String toFastNotation()
	{
		String moveString = pieceNames[movingPiece];
		moveString += Position.squareName[from]+"-";
		moveString += Position.squareName[to];
		if (promotionPiece != Position.EMPTY)
			moveString += pieceName[promotionPiece];
		return moveString;
	}
	
}
