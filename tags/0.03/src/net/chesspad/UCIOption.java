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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Stores a UCI option of an Engine.
 * @author Jean-Francois Romang <info at chesspad dot net>
 *
 */
public class UCIOption {
	enum Type {
		CHECK, SPIN, COMBO, BUTTON, STRING
	};

	private Variant value;
	private final Context context;
	private final String engineId;
	public Variant defaultValue;

	public Type type;
	public String name;
	public int min, max;
	public Vector<String> vars;

	UCIOption(Context context, String engineId, Type t, String name,
			Variant value, int min, int max, Vector<String> vars) {
		type = t;
		this.name = name;
		this.defaultValue = value;
		this.min = min;
		this.max = max;
		this.vars = vars;

		this.context = context;
		this.engineId = engineId;
		// if the option exists in the preferences, we load the value
		this.value = Variant.valueOf(context.getSharedPreferences(engineId,
				Context.MODE_PRIVATE).getString(name, value.getString()));
	}

	public Variant getValue() {
		return value;
	}

	public void setValue(Variant v) {
		this.value = v;
		final SharedPreferences.Editor ed = context.getSharedPreferences(
				engineId, Context.MODE_PRIVATE).edit();
		ed.putString(name, v.getString());
		ed.commit();
		Log.d("ucioptionset", name
				+ " exists :"
				+ context.getSharedPreferences(engineId, Context.MODE_PRIVATE)
						.contains(name));
	}

	@Override
	public String toString() {
		String s = "setoption name ";
		s += name;
		if (type == Type.BUTTON)
			return s;
		s += " value ";
		s += value.getString();
		// s+="\n";
		return s;
	}
}
