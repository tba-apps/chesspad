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

/**
 * Handling a human player that is playing on a ChessBoardView
 * @author Jean-Francois Romang <info at chesspad dot net>
 */
public class HumanPlayer extends Player {

	private final ChessBoardView chessBoardView;

	public HumanPlayer(final Game game, Role role, ChessBoardView cbv) {
		super(game,role);
		chessBoardView = cbv;
		chessBoardView.setOnMoveListener(new ChessBoardView.OnMoveListener() {
			
			public void onMove(Move m) {
				game.playMove(m);
			}
		});
	}

	public void update(Observable arg0, Object arg1) {
		boolean side=game.getCurrentPosition().sideToPlay();
		chessBoardView.setAcceptInput((role==Role.WHITE_PLAYER && side)
										||	(role==Role.BLACK_PLAYER && !side));
	}
	
}
