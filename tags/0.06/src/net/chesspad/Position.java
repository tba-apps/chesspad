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
 * This class stores all informations about a chess position.
 * It also has the move generation logic.
 * @author Jean-Francois Romang <info at chesspad dot net>
 */
public class Position implements Cloneable, java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2789035824350562331L;

	public static final int A8 = 0, B8 = 1, C8 = 2, D8 = 3, E8 = 4, F8 = 5,
			G8 = 6, H8 = 7, A7 = 8, B7 = 9, C7 = 10, D7 = 11, E7 = 12, F7 = 13,
			G7 = 14, H7 = 15, A6 = 16, B6 = 17, C6 = 18, D6 = 19, E6 = 20,
			F6 = 21, G6 = 22, H6 = 23, A5 = 24, B5 = 25, C5 = 26, D5 = 27,
			E5 = 28, F5 = 29, G5 = 30, H5 = 31, A4 = 32, B4 = 33, C4 = 34,
			D4 = 35, E4 = 36, F4 = 37, G4 = 38, H4 = 39, A3 = 40, B3 = 41,
			C3 = 42, D3 = 43, E3 = 44, F3 = 45, G3 = 46, H3 = 47, A2 = 48,
			B2 = 49, C2 = 50, D2 = 51, E2 = 52, F2 = 53, G2 = 54, H2 = 55,
			A1 = 56, B1 = 57, C1 = 58, D1 = 59, E1 = 60, F1 = 61, G1 = 62,
			H1 = 63;

	public static final int EMPTY = 0, W_PAWN = 1, W_KNIGHT = 2, W_BISHOP = 3,
			W_ROOK = 4, W_QUEEN = 5, W_KING = 6, B_PAWN = 7, B_KNIGHT = 8,
			B_BISHOP = 9, B_ROOK = 10, B_QUEEN = 11, B_KING = 12;

	public static final int WHITE = 1;

	public static final int BLACK = 0;

	public static final String squareName[];
	private static Pattern fenPattern;
	static {
		// whatever code is needed for initialization goes here
		squareName = new String[] { "a8", "b8", "c8", "d8", "e8", "f8", "g8",
				"h8", "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7", "a6",
				"b6", "c6", "d6", "e6", "f6", "g6", "h6", "a5", "b5", "c5",
				"d5", "e5", "f5", "g5", "h5", "a4", "b4", "c4", "d4", "e4",
				"f4", "g4", "h4", "a3", "b3", "c3", "d3", "e3", "f3", "g3",
				"h3", "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2", "a1",
				"b1", "c1", "d1", "e1", "f1", "g1", "h1" };

		fenPattern = Pattern
				.compile("^((?:[PNBRQKpnbrqk1-8]{1,8}/){7,7}[PNBRQKpnbrqk1-8]{1,8})\\s([wb]{1,1})\\s((?:[KQkq]{1,4})|-)\\s((?:[a-h][36])|-)\\s(\\d\\d?)\\s(\\d+)$");

	}
	private int board[];

	private boolean sideToPlay;

	private boolean kingSideCastlingAllowed[];
	private boolean queenSideCastlingAllowed[];
	public int enPassantSquare; // En passant target square in algebraic
								// notation. If there's no en passant target
								// square, this is "–". If a pawn has just made
								// a 2-square move, this is the position
								// "behind" the pawn. This is recorded
								// regardless of whether there is a pawn in
								// position to make an en passant capture.
	private int rule50moves; // Halfmove clock: This is the number of halfmoves
								// since the last pawn advance or capture. This
								// is used to determine if a draw can be claimed
								// under the fifty-move rule.
	private int fullMove; // Fullmove number: The number of the full move. It
							// starts at 1, and is incremented after Black's
							// move.

	public Position() {
		initPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	public Position(String fen) {
		initPosition(fen);
	}

	public boolean blackPieceAt(int sq) {
		return (pieceAt(sq) >= B_PAWN);
	}

	public Object clone() {
		Position p = null;
		try {
			// On récupère l'instance à renvoyer par l'appel de la
			// méthode super.clone()
			p = (Position) super.clone();
		} catch (final CloneNotSupportedException cnse) {
			// Ne devrait jamais arriver car nous implémentons
			// l'interface Cloneable
			cnse.printStackTrace(System.err);
		}

		// On clone l'attribut de type Patronyme qui n'est pas immuable.
		// personne.patronyme = (Patronyme) patronyme.clone();
		p.board = (int[]) board.clone();
		p.kingSideCastlingAllowed = (boolean[]) kingSideCastlingAllowed.clone();
		p.queenSideCastlingAllowed = (boolean[]) queenSideCastlingAllowed
				.clone();

		// on renvoie le clone
		return p;
	}

	public boolean enPassantAllowed() {
		return (enPassantSquare != -1);
	}

	String getFEN() {
		final String labels = " PNBRQKpnbrqk";
		int buffer = 0;
		String fen = "";

		for (int i = 0; i < 64; i++) {
			if (pieceAt(i) == EMPTY)
				buffer++;
			else {
				if (buffer != 0)
					fen += buffer;
				buffer = 0;
				fen += labels.charAt(pieceAt(i));
			}
			if (i % 8 == 7) {
				if (buffer != 0)
					fen += buffer;
				buffer = 0;
				if (i < 63)
					fen += "/";
			}
		}
		fen += (sideToPlay ? " w " : " b ");
		if (kingSideCastlingAllowed[1])
			fen += "K";
		if (queenSideCastlingAllowed[1])
			fen += "Q";
		if (kingSideCastlingAllowed[0])
			fen += "k";
		if (queenSideCastlingAllowed[0])
			fen += "q";

		if (!kingSideCastlingAllowed[1] && !queenSideCastlingAllowed[1]
				&& !kingSideCastlingAllowed[0] && !queenSideCastlingAllowed[0])
			fen += "-";

		fen += " ";
		if (enPassantAllowed())
			fen += squareName[enPassantSquare];
		else
			fen += "-";

		fen += " " + rule50moves;
		fen += " " + fullMove;

		return fen;
	}

	Vector<Move> getLegalMoves() {
		final Vector<Move> pseudoLegalMoves = getPseudoLegalMoves(false);

		// Test move legality
		for (int i = 0; i < pseudoLegalMoves.size(); i++) {
			final Move m = pseudoLegalMoves.get(i);
			final Position p = (Position) this.clone();
			p.makeMove(m);
			if (p.isAttacked(p.kingSquare(!p.sideToPlay),
					p.sideToPlay ? Position.WHITE : Position.BLACK)) {
				pseudoLegalMoves.remove(m);
				i--;
			}
		}

		return pseudoLegalMoves;
	}


	Vector<Move> getPseudoLegalMoves(boolean capturesOnly) {
		final Vector<Move> q = new Vector<Move>();

		if (sideToPlay)
			for (int from = 0; from < 64; from++)
				switch (pieceAt(from)) {
				case W_PAWN:
					if (isEmpty(from - 8)) // avance une case
					{
						if (from >= A6) // avance normale
							q.addElement(new Move(W_PAWN, from, from - 8));
						else // promotion
						{
							q.addElement(new Move(W_PAWN, from, from - 8,
									EMPTY, W_QUEEN));
							q.addElement(new Move(W_PAWN, from, from - 8,
									EMPTY, W_ROOK));
							q.addElement(new Move(W_PAWN, from, from - 8,
									EMPTY, W_BISHOP));
							q.addElement(new Move(W_PAWN, from, from - 8,
									EMPTY, W_KNIGHT));
						}

						if (from >= A2 && isEmpty(from - 16)) // avance deux
																// cases
							q.addElement(new Move(W_PAWN, from, from - 16));
					}

					if ((from % 8) > 0
							&& (blackPieceAt(from - 9) || (from - 9) == enPassantSquare))
						if (from >= A6) // capture normale
							q.addElement(new Move(W_PAWN, from, from - 9,
									(pieceAt(from - 9) == EMPTY) ? B_PAWN
											: pieceAt(from - 9), EMPTY));
						else // capture avec promotion
						{
							q.addElement(new Move(W_PAWN, from, from - 9,
									pieceAt(from - 9), W_QUEEN));
							q.addElement(new Move(W_PAWN, from, from - 9,
									pieceAt(from - 9), W_ROOK));
							q.addElement(new Move(W_PAWN, from, from - 9,
									pieceAt(from - 9), W_BISHOP));
							q.addElement(new Move(W_PAWN, from, from - 9,
									pieceAt(from - 9), W_KNIGHT));
						}

					if ((from % 8) < 7
							&& (blackPieceAt(from - 7) || (from - 7) == enPassantSquare))
						if (from >= A6) // capture normale
							q.addElement(new Move(W_PAWN, from, from - 7,
									(pieceAt(from - 7) == EMPTY) ? B_PAWN
											: pieceAt(from - 7), EMPTY));
						else // capture avec promotion
						{
							q.addElement(new Move(W_PAWN, from, from - 7,
									pieceAt(from - 7), W_QUEEN));
							q.addElement(new Move(W_PAWN, from, from - 7,
									pieceAt(from - 7), W_ROOK));
							q.addElement(new Move(W_PAWN, from, from - 7,
									pieceAt(from - 7), W_BISHOP));
							q.addElement(new Move(W_PAWN, from, from - 7,
									pieceAt(from - 7), W_KNIGHT));
						}
					break;

				case W_KNIGHT: {
					if (from >= A6 && (from % 8) > 0
							&& (isEmpty(from - 17) || blackPieceAt(from - 17)))
						q.addElement(new Move(W_KNIGHT, from, from - 17,
								pieceAt(from - 17), EMPTY));
					if (from >= A6 && (from % 8) < 7
							&& (isEmpty(from - 15) || blackPieceAt(from - 15)))
						q.addElement(new Move(W_KNIGHT, from, from - 15,
								pieceAt(from - 15), EMPTY));
					if (from >= A7 && (from % 8) < 6
							&& (isEmpty(from - 6) || blackPieceAt(from - 6)))
						q.addElement(new Move(W_KNIGHT, from, from - 6,
								pieceAt(from - 6), EMPTY));
					if (from < A1 && (from % 8) < 6
							&& (isEmpty(from + 10) || blackPieceAt(from + 10)))
						q.addElement(new Move(W_KNIGHT, from, from + 10,
								pieceAt(from + 10), EMPTY));
					if (from < A2 && (from % 8) < 7
							&& (isEmpty(from + 17) || blackPieceAt(from + 17)))
						q.addElement(new Move(W_KNIGHT, from, from + 17,
								pieceAt(from + 17), EMPTY));
					if (from < A2 && (from % 8) > 0
							&& (isEmpty(from + 15) || blackPieceAt(from + 15)))
						q.addElement(new Move(W_KNIGHT, from, from + 15,
								pieceAt(from + 15), EMPTY));
					if (from < A1 && (from % 8) > 1
							&& (isEmpty(from + 6) || blackPieceAt(from + 6)))
						q.addElement(new Move(W_KNIGHT, from, from + 6,
								pieceAt(from + 6), EMPTY));
					if (from >= A7 && (from % 8) > 1
							&& (isEmpty(from - 10) || blackPieceAt(from - 10)))
						q.addElement(new Move(W_KNIGHT, from, from - 10,
								pieceAt(from - 10), EMPTY));
					break;
				}

				case W_BISHOP: {
					boolean cap = false;
					for (int to = from - 9; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 9)
						q.addElement(new Move(W_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 7; (!cap) && to >= 0 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 7)
						q.addElement(new Move(W_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 9; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 9)
						q.addElement(new Move(W_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 7; (!cap) && to < 64 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 7)
						q.addElement(new Move(W_BISHOP, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case W_ROOK: {
					boolean cap = false;
					for (int to = from - 8; (!cap) && to >= 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 8)
						q.addElement(new Move(W_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 8; (!cap) && to < 64
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 8)
						q.addElement(new Move(W_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 1; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 1)
						q.addElement(new Move(W_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 1; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 1)
						q.addElement(new Move(W_ROOK, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case W_QUEEN: {
					boolean cap = false;
					for (int to = from - 9; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 9)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 7; (!cap) && to >= 0 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 7)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 9; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 9)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 7; (!cap) && to < 64 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 7)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 8; (!cap) && to >= 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 8)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 8; (!cap) && to < 64
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 8)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 1; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to += 1)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 1; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = blackPieceAt(to))); to -= 1)
						q.addElement(new Move(W_QUEEN, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case W_KING: {
					if (from >= A7 && (from % 8) > 0
							&& (isEmpty(from - 9) || (blackPieceAt(from - 9))))
						q.addElement(new Move(W_KING, from, from - 9,
								pieceAt(from - 9), EMPTY));
					if (from >= A7
							&& (isEmpty(from - 8) || (blackPieceAt(from - 8))))
						q.addElement(new Move(W_KING, from, from - 8,
								pieceAt(from - 8), EMPTY));
					if (from >= A7 && (from % 8) < 7
							&& (isEmpty(from - 7) || (blackPieceAt(from - 7))))
						q.addElement(new Move(W_KING, from, from - 7,
								pieceAt(from - 7), EMPTY));
					if ((from % 8) < 7
							&& (isEmpty(from + 1) || (blackPieceAt(from + 1))))
						q.addElement(new Move(W_KING, from, from + 1,
								pieceAt(from + 1), EMPTY));
					if (from < A1 && (from % 8) < 7
							&& (isEmpty(from + 9) || (blackPieceAt(from + 9))))
						q.addElement(new Move(W_KING, from, from + 9,
								pieceAt(from + 9), EMPTY));
					if (from < A1
							&& (isEmpty(from + 8) || (blackPieceAt(from + 8))))
						q.addElement(new Move(W_KING, from, from + 8,
								pieceAt(from + 8), EMPTY));
					if (from < A1 && (from % 8) > 0
							&& (isEmpty(from + 7) || (blackPieceAt(from + 7))))
						q.addElement(new Move(W_KING, from, from + 7,
								pieceAt(from + 7), EMPTY));
					if ((from % 8) > 0
							&& (isEmpty(from - 1) || (blackPieceAt(from - 1))))
						q.addElement(new Move(W_KING, from, from - 1,
								pieceAt(from - 1), EMPTY));

					if (!capturesOnly) // castling
					{
						// short
						if (kingSideCastlingAllowed[Position.WHITE]
								&& from == E1 && board[F1] == EMPTY
								&& board[G1] == EMPTY && !isAttacked(E1, BLACK)
								&& !isAttacked(F1, BLACK)
								&& !isAttacked(G1, BLACK))
							q.addElement(new Move(W_KING, from, G1));

						// long
						if (queenSideCastlingAllowed[WHITE] && from == E1
								&& board[B1] == EMPTY && board[C1] == EMPTY
								&& board[D1] == EMPTY && !isAttacked(E1, BLACK)
								&& !isAttacked(C1, BLACK)
								&& !isAttacked(D1, BLACK))
							q.addElement(new Move(W_KING, from, C1));
					}
					break;
				}

				default:
					break;
				}
		else
			for (int from = 0; from < 64; from++)
				switch (pieceAt(from)) {
				case B_PAWN:
					if (isEmpty(from + 8)) // avance une case
					{
						if (from < A2) // avance normale
							q.addElement(new Move(B_PAWN, from, from + 8));
						else // promotion
						{
							q.addElement(new Move(B_PAWN, from, from + 8,
									EMPTY, B_QUEEN));
							q.addElement(new Move(B_PAWN, from, from + 8,
									EMPTY, B_ROOK));
							q.addElement(new Move(B_PAWN, from, from + 8,
									EMPTY, B_BISHOP));
							q.addElement(new Move(B_PAWN, from, from + 8,
									EMPTY, B_KNIGHT));
						}

						if (from < A6 && isEmpty(from + 16)) // avance deux
																// cases
							q.addElement(new Move(B_PAWN, from, from + 16));
					}

					if ((from % 8) > 0
							&& (whitePieceAt(from + 7) || (from + 7) == enPassantSquare))
						if (from < A2) // capture normale
							q.addElement(new Move(B_PAWN, from, from + 7,
									(pieceAt(from + 7) == EMPTY) ? W_PAWN
											: pieceAt(from + 7), EMPTY));
						else // capture avec promotion
						{
							q.addElement(new Move(B_PAWN, from, from + 7,
									pieceAt(from + 7), B_QUEEN));
							q.addElement(new Move(B_PAWN, from, from + 7,
									pieceAt(from + 7), B_ROOK));
							q.addElement(new Move(B_PAWN, from, from + 7,
									pieceAt(from + 7), B_BISHOP));
							q.addElement(new Move(B_PAWN, from, from + 7,
									pieceAt(from + 7), B_KNIGHT));
						}

					if ((from % 8) < 7
							&& (whitePieceAt(from + 9) || (from + 9) == enPassantSquare))
						if (from < A2) // capture normale
							q.addElement(new Move(B_PAWN, from, from + 9,
									(pieceAt(from + 9) == EMPTY) ? W_PAWN
											: pieceAt(from + 9), EMPTY));
						else // capture avec promotion
						{
							q.addElement(new Move(B_PAWN, from, from + 9,
									pieceAt(from + 9), B_QUEEN));
							q.addElement(new Move(B_PAWN, from, from + 9,
									pieceAt(from + 9), B_ROOK));
							q.addElement(new Move(B_PAWN, from, from + 9,
									pieceAt(from + 9), B_BISHOP));
							q.addElement(new Move(B_PAWN, from, from + 9,
									pieceAt(from + 9), B_KNIGHT));
						}
					break;

				case B_KNIGHT: {
					if (from >= A6 && (from % 8) > 0
							&& (isEmpty(from - 17) || whitePieceAt(from - 17)))
						q.addElement(new Move(B_KNIGHT, from, from - 17,
								pieceAt(from - 17), EMPTY));
					if (from >= A6 && (from % 8) < 7
							&& (isEmpty(from - 15) || whitePieceAt(from - 15)))
						q.addElement(new Move(B_KNIGHT, from, from - 15,
								pieceAt(from - 15), EMPTY));
					if (from >= A7 && (from % 8) < 6
							&& (isEmpty(from - 6) || whitePieceAt(from - 6)))
						q.addElement(new Move(B_KNIGHT, from, from - 6,
								pieceAt(from - 6), EMPTY));
					if (from < A1 && (from % 8) < 6
							&& (isEmpty(from + 10) || whitePieceAt(from + 10)))
						q.addElement(new Move(B_KNIGHT, from, from + 10,
								pieceAt(from + 10), EMPTY));
					if (from < A2 && (from % 8) < 7
							&& (isEmpty(from + 17) || whitePieceAt(from + 17)))
						q.addElement(new Move(B_KNIGHT, from, from + 17,
								pieceAt(from + 17), EMPTY));
					if (from < A2 && (from % 8) > 0
							&& (isEmpty(from + 15) || whitePieceAt(from + 15)))
						q.addElement(new Move(B_KNIGHT, from, from + 15,
								pieceAt(from + 15), EMPTY));
					if (from < A1 && (from % 8) > 1
							&& (isEmpty(from + 6) || whitePieceAt(from + 6)))
						q.addElement(new Move(B_KNIGHT, from, from + 6,
								pieceAt(from + 6), EMPTY));
					if (from >= A7 && (from % 8) > 1
							&& (isEmpty(from - 10) || whitePieceAt(from - 10)))
						q.addElement(new Move(B_KNIGHT, from, from - 10,
								pieceAt(from - 10), EMPTY));
					break;
				}

				case B_BISHOP: {
					boolean cap = false;
					for (int to = from - 9; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 9)
						q.addElement(new Move(B_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 7; (!cap) && to >= 0 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 7)
						q.addElement(new Move(B_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 9; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 9)
						q.addElement(new Move(B_BISHOP, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 7; (!cap) && to < 64 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 7)
						q.addElement(new Move(B_BISHOP, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case B_ROOK: {
					boolean cap = false;
					for (int to = from - 8; (!cap) && to >= 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 8)
						q.addElement(new Move(B_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 8; (!cap) && to < 64
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 8)
						q.addElement(new Move(B_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 1; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 1)
						q.addElement(new Move(B_ROOK, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 1; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 1)
						q.addElement(new Move(B_ROOK, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case B_QUEEN: {
					boolean cap = false;
					for (int to = from - 9; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 9)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 7; (!cap) && to >= 0 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 7)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 9; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 9)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 7; (!cap) && to < 64 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 7)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 8; (!cap) && to >= 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 8)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 8; (!cap) && to < 64
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 8)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from + 1; (!cap) && to < 64 && (to % 8) > 0
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to += 1)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					cap = false;
					for (int to = from - 1; (!cap) && to >= 0 && (to % 8) < 7
							&& (isEmpty(to) || (cap = whitePieceAt(to))); to -= 1)
						q.addElement(new Move(B_QUEEN, from, to, pieceAt(to),
								EMPTY));
					break;
				}

				case B_KING: {
					if (from >= A7 && (from % 8) > 0
							&& (isEmpty(from - 9) || (whitePieceAt(from - 9))))
						q.addElement(new Move(B_KING, from, from - 9,
								pieceAt(from - 9), EMPTY));
					if (from >= A7
							&& (isEmpty(from - 8) || (whitePieceAt(from - 8))))
						q.addElement(new Move(B_KING, from, from - 8,
								pieceAt(from - 8), EMPTY));
					if (from >= A7 && (from % 8) < 7
							&& (isEmpty(from - 7) || (whitePieceAt(from - 7))))
						q.addElement(new Move(B_KING, from, from - 7,
								pieceAt(from - 7), EMPTY));
					if ((from % 8) < 7
							&& (isEmpty(from + 1) || (whitePieceAt(from + 1))))
						q.addElement(new Move(B_KING, from, from + 1,
								pieceAt(from + 1), EMPTY));
					if (from < A1 && (from % 8) < 7
							&& (isEmpty(from + 9) || (whitePieceAt(from + 9))))
						q.addElement(new Move(B_KING, from, from + 9,
								pieceAt(from + 9), EMPTY));
					if (from < A1
							&& (isEmpty(from + 8) || (whitePieceAt(from + 8))))
						q.addElement(new Move(B_KING, from, from + 8,
								pieceAt(from + 8), EMPTY));
					if (from < A1 && (from % 8) > 0
							&& (isEmpty(from + 7) || (whitePieceAt(from + 7))))
						q.addElement(new Move(B_KING, from, from + 7,
								pieceAt(from + 7), EMPTY));
					if ((from % 8) > 0
							&& (isEmpty(from - 1) || (whitePieceAt(from - 1))))
						q.addElement(new Move(B_KING, from, from - 1,
								pieceAt(from - 1), EMPTY));

					if (!capturesOnly) // castling
					{
						// short
						if (kingSideCastlingAllowed[BLACK] && from == E8
								&& board[F8] == EMPTY && board[G8] == EMPTY
								&& !isAttacked(E8, WHITE)
								&& !isAttacked(F8, WHITE)
								&& !isAttacked(G8, WHITE))
							q.addElement(new Move(B_KING, from, G8));

						// long
						if (queenSideCastlingAllowed[BLACK] && from == E8
								&& board[B8] == EMPTY && board[C8] == EMPTY
								&& board[D8] == EMPTY && !isAttacked(E8, WHITE)
								&& !isAttacked(C8, WHITE)
								&& !isAttacked(D8, WHITE))
							q.addElement(new Move(B_KING, from, C8));
					}
					break;
				}

				default:
					break;
				}

		if (capturesOnly)
			for (int i = 0; i < q.size(); i++) {
				final Move m = q.get(i);
				if (m.capturedPiece == EMPTY) {
					q.remove(m);
					i--;
				}
				// if(m.capturedPiece==W_KING || m.capturedPiece==B_KING)
				// std::cout<<"KINCPPPPPP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"<<std::endl;
			}

		return q;
	}

	private void initPosition(String fen) {
		board = new int[64];
		kingSideCastlingAllowed = new boolean[2];
		queenSideCastlingAllowed = new boolean[2];

		setFEN(fen);
	}

	boolean isAttacked(int square, int attackerColor) {
		final Position p = (Position) this.clone();
		p.sideToPlay = (attackerColor == WHITE);
		p.board[square] = p.sideToPlay ? B_PAWN : W_PAWN;
		final Vector<Move> moves = p.getPseudoLegalMoves(true);
		for (final Move m : moves)
			if (m.to == square)
				return true;
		return false;
	}

	public boolean isEmpty(int sq) {
		return pieceAt(sq) == EMPTY;
	}

	int kingSquare(boolean color) {
		// boolean color=(col==Position.WHITE);
		for (int square = 0; square < 64; square++)
			if (pieceAt(square) == (color ? W_KING : B_KING))
				return square;

		// TODO lancer erreur
		// std::cout<<"KINGSQUARE ERROR"<<std::endl;
		return -1;
	}

	public void makeMove(Move m) {
		if (m.capturedPiece != EMPTY && board[m.to] == EMPTY) // Enpassant
																// capture
			board[m.to + (sideToPlay ? 8 : -8)] = EMPTY;

		board[m.from] = EMPTY;
		board[m.to] = m.movingPiece;

		// Promotions
		if (m.promotionPiece != EMPTY)
			board[m.to] = m.promotionPiece;

		// Also move the rook if castling
		if (m.movingPiece == W_KING && m.from == E1 && m.to == G1) {
			board[H1] = EMPTY;
			board[F1] = W_ROOK;
		}
		if (m.movingPiece == W_KING && m.from == E1 && m.to == C1) {
			board[A1] = EMPTY;
			board[D1] = W_ROOK;
		}
		if (m.movingPiece == B_KING && m.from == E8 && m.to == G8) {
			board[H8] = EMPTY;
			board[F8] = B_ROOK;
		}
		if (m.movingPiece == B_KING && m.from == E8 && m.to == C8) {
			board[A8] = EMPTY;
			board[D8] = B_ROOK;
		}

		// Castling status update
		if (m.movingPiece == W_KING)
			kingSideCastlingAllowed[WHITE] = queenSideCastlingAllowed[WHITE] = false;
		if ((m.movingPiece == W_ROOK && m.from == H1) || (m.to == H1))
			kingSideCastlingAllowed[WHITE] = false;
		if ((m.movingPiece == W_ROOK && m.from == A1) || (m.to == A1))
			queenSideCastlingAllowed[WHITE] = false;

		if (m.movingPiece == B_KING)
			kingSideCastlingAllowed[BLACK] = queenSideCastlingAllowed[BLACK] = false;
		if ((m.movingPiece == B_ROOK && m.from == H8) || (m.to == H8))
			kingSideCastlingAllowed[BLACK] = false;
		if ((m.movingPiece == B_ROOK && m.from == A8) || (m.to == A8))
			queenSideCastlingAllowed[BLACK] = false;

		// enPassantSquare update
		enPassantSquare = -1;
		if (m.movingPiece == W_PAWN && (m.from - m.to) == 16)
			enPassantSquare = m.to + 8;
		if (m.movingPiece == B_PAWN && (m.to - m.from) == 16)
			enPassantSquare = m.to - 8;

		// rule50moves update
		if (m.movingPiece == W_PAWN || m.movingPiece == B_PAWN
				|| m.capturedPiece == W_PAWN || m.capturedPiece == B_PAWN)
			rule50moves = 0;
		else
			rule50moves++;

		// fullMove update
		if (!sideToPlay)
			fullMove++;

		sideToPlay = !sideToPlay;
	}

	public long perft(int depth) {
		long nodes = 0;
		if (depth == 0)
			return 1;

		final Vector<Move> moves = getLegalMoves();
		for (final Move m : moves) {
			final Position p = (Position) this.clone();
			p.makeMove(m);
			nodes += p.perft(depth - 1);
		}

		return nodes;
	}

	public int pieceAt(int square) {
		return board[square];
	}

	public boolean setFEN(String fen) {
		// regular expression test
		final Matcher m = fenPattern.matcher(fen);
		if (!m.matches()) {
			// TODO
			System.out.println("fen does not match:" + fen);
			// Log.e("Position","fen does not match:"+fen);
			return false;
		}

		// for(int i=1;i<=m.groupCount();i++)
		// Log.d("Position",m.group(i));

		// Initialisation du tableau de pieces (board[64])
		final String boardString = m.group(1);
		int square = 0;
		for (int i = 0; i < 64; i++)
			board[i] = EMPTY;
		for (int i = 0; i < boardString.length() && square < 64; i++)
			switch (boardString.charAt(i)) {
			case 'P':
				board[square++] = W_PAWN;
				break;
			case 'N':
				board[square++] = W_KNIGHT;
				break;
			case 'B':
				board[square++] = W_BISHOP;
				break;
			case 'R':
				board[square++] = W_ROOK;
				break;
			case 'Q':
				board[square++] = W_QUEEN;
				break;
			case 'K':
				board[square++] = W_KING;
				break;
			case 'p':
				board[square++] = B_PAWN;
				break;
			case 'n':
				board[square++] = B_KNIGHT;
				break;
			case 'b':
				board[square++] = B_BISHOP;
				break;
			case 'r':
				board[square++] = B_ROOK;
				break;
			case 'q':
				board[square++] = B_QUEEN;
				break;
			case 'k':
				board[square++] = B_KING;
				break;
			case '1':
				square++;
				break;
			case '2':
				square += 2;
				break;
			case '3':
				square += 3;
				break;
			case '4':
				square += 4;
				break;
			case '5':
				square += 5;
				break;
			case '6':
				square += 6;
				break;
			case '7':
				square += 7;
				break;
			case '8':
				square += 8;
				break;
			case '/':
				if ((square % 8) != 0)
					square += (8 - (square % 8));
				break;
			default:
				// TODO
				System.out.println("Unknown value in Position.setFEN:"
						+ boardString.charAt(i));
				// Log.e("Position","Unknown value in Position.setFEN:"+boardString.charAt(i));
				return false;
			}

		// Determination de la couleur qui a le trait
		// System.out.println("stp:"+m.group(2));
		sideToPlay = ((m.group(2).equals("w")) ? true : false);

		// Castle
		kingSideCastlingAllowed[1] = m.group(3).contains("K") ? true : false;
		queenSideCastlingAllowed[1] = m.group(3).contains("Q") ? true : false;
		kingSideCastlingAllowed[0] = m.group(3).contains("k") ? true : false;
		queenSideCastlingAllowed[0] = m.group(3).contains("q") ? true : false;

		// enpassant
		enPassantSquare = -1;
		for (int i = 0; i < 64; i++)
			if (m.group(4).contains(squareName[i]))
				enPassantSquare = i;

		// 50moves
		rule50moves = Integer.parseInt(m.group(5));

		// fullMove
		fullMove = Integer.parseInt(m.group(6));

		// TODO check pas de pions en bout de course (cause stack overflow dans
		// movegen)
		// TODO check qu'il y a au moins et un seul roi (cause stack ov dans
		// kingsquare)

		return true;
	}

	public boolean sideToPlay() {
		return sideToPlay;
	}

	public String toString() {
		String s = "";

		final String pieceLabels[] = { "   |", " P |", " N |", " B |", " R |",
				" Q |", " K |", " *P|", " *N|", " *B|", " *R|", " *Q|", " *K|" };
		for (int l = 0; l < 8; l++) {
			s += "\n   +---+---+---+---+---+---+---+---+\n ";
			s += (8 - l) + " |";

			for (int c = 0; c < 8; c++)
				s += pieceLabels[pieceAt(l * 8 + c)];
		}
		s += "\n   +---+---+---+---+---+---+---+---+\n";
		s += ".    a   b   c   d   e   f   g   h\n";

		s += "FEN:";
		s += getFEN();
		return s;

	}

	public boolean whitePieceAt(int sq) {
		return (!isEmpty(sq) && !blackPieceAt(sq));
	}

}
