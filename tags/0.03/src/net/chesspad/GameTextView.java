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
import java.util.Observer;
import java.util.Vector;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A simple game observer View displaying the moves done.
 * @author Jean-Francois Romang <info at chesspad dot net>
 */
public class GameTextView extends TextView implements Observer{

	public GameTextView(Context context, AttributeSet attr) {
		super(context, attr);
		this.setBackgroundResource(R.drawable.bg_black_gradient);
		this.setText("1.");
	}

	public void update(Observable observable, Object data) {
		String s="";
		Game game=(Game)observable;
		Vector<Move> moves=(Vector<Move>)game.getMoves();

		for(int i=0;i<moves.size();i++)
		{
			if((i%2)==0) s+=Integer.toString(i/2+1)+".";
			s+=moves.get(i).algebraicNotation+" ";
		}
		this.setText(s);
	}

}
