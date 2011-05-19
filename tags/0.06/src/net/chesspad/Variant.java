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

/**
 * Allows to sotre different types in a String
 * @author Jean-Francois Romang <info at chesspad dot net>
 *
 */
public class Variant
{
	public static Variant valueOf(String s) {
		final Variant v = new Variant();
		v.s = s;
		return v;
	}

	private String s;

	private Variant() {
	}

	public boolean getBoolean() {
		return s.equals("true");
	}

	public float getFloat() {
		return Float.parseFloat(s);
	}

	public int getInt() {
		return Integer.parseInt(s);
	}

	public String getString() {
		return s;
	}
}
