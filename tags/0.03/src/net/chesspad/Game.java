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

import java.util.Observable;
import java.util.Vector;

/**
 * Class holding all information about a game ; it sends events to observers when it changes
 * @author Jean-Francois Romang <info at chesspad dot net>
 */
public class Game extends Observable implements java.io.Serializable {

	private static final long serialVersionUID = 312170620991527099L;
	private Position startPosition; //Game start position
	private final Vector<Move> moves; //All the moves of this game
	private int backMoves=0;

	/**
	 * 
	 * @return the starting position of this Game.
	 */
	public Position getStartPosition() {
		return startPosition;
	}

	public Vector<Move> getMoves() {
		return moves;
	}

	public Game() {
		super();
		startPosition = new Position();
		moves = new Vector<Move>();
	}

	/**
	 * 
	 * @return the position after the last move.
	 */
	public Position getCurrentPosition() {
		final Position currentPosition = (Position) startPosition.clone();
		
		for(int i=0;i<moves.size()-backMoves;i++)
			currentPosition.makeMove(moves.get(i));
		
		/*
		for (final Move m : moves)
			currentPosition.makeMove(m);*/
		return currentPosition;
	}
	
	public void back()
	{
		backMoves++;
		if(backMoves>moves.size()) backMoves=moves.size();
		this.setChanged();
		this.notifyObservers();
	}
	
	public void forward()
	{
		backMoves--;
		if(backMoves<0) backMoves=0;
		this.setChanged();
		this.notifyObservers();
	}

	public boolean isInPlayingMode()
	{
		return(backMoves==0);
	}
	
	/**
	 * Play this move, and notify all observers
	 * @param m move to play
	 */
	public void playMove(Move m) {
		m.toAlgebraicNotation(getCurrentPosition());
		
		while(backMoves!=0)
		{
			moves.remove(moves.size()-1);
			backMoves--;
		}
			
		moves.add(m);
		this.setChanged();
		this.notifyObservers(m);
	}

	/**
	 * Change the starting position. This also clears the moves and notifies all the observers.
	 * @param startPosition
	 */
	public void setStartPosition(Position startPosition) {
		this.startPosition = startPosition;
		moves.clear();
		this.setChanged();
		this.notifyObservers();
	}
	
	
	/**
	 * Returns UCI-style string of this Game
	 */
	public String toString()
	{
		String s="position fen "+startPosition.getFEN()+" moves";
		//TODO renvoyer startposition au lieu de la fen si startposition normale (voir uci protocol)
		/*
		for(Move m: moves)
		{
			s+=" "+m.toString();
		}*/
		for(int i=0;i<moves.size()-backMoves;i++)
			s+=" "+moves.get(i).toString();
		
		return s;
	}

}
