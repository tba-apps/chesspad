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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * View class for displaying a chess board ; also handles input events.
 * Drawing of the pieces highly inspired by Droidfish :-)
 * @author Jean-Francois Romang <info at chesspad dot net>
 */
public class ChessBoardView extends View implements Observer {

	public interface OnMoveListener {
		abstract void onMove(Move m);
	}

	private Paint blackSquarePaint, whiteSquarePaint, selectedSquarePaint; //Paint objects used to draw the squares
	private Paint whitePiecePaint, blackPiecePaint; //Paint objects used to draw the pieces
	private int pieceXDelta, pieceYDelta; // top/left pixel draw position relative to square
	private Position position; //position display by this view

	//Animation handling
	private boolean animation;
	private int animationPiece;
	private int animationFrom, animationTo;
	private long animationStartTime, animationStopTime;
	private Position animationFinalPosition;

	//Selection handling
	private int selectedSquare = -1;

	private boolean acceptInput = true; //If false, this view will not accept user moves
	private OnMoveListener moveListener=null; //Sends events when a move is done

	public ChessBoardView(Context context, AttributeSet ats) {
		super(context, ats);
		initChessBoardView();
	}
	
	public ChessBoardView(Context context, AttributeSet ats, int defaultStyle) {
		super(context, ats, defaultStyle);
		initChessBoardView();
	}

	/**
	 * Sets the view position and animate a piece for graphical transition
	 * @param m the move to animate
	 * @param finalPosition the final position after the move
	 */
	public void animateMove(Move m, Position finalPosition) {
		animation = true;
		animationPiece = m.movingPiece;
		animationFrom = m.from;
		animationTo = m.to;
		animationStartTime = System.currentTimeMillis();
		animationStopTime = animationStartTime + 200;
		animationFinalPosition = (Position) finalPosition.clone();
		invalidate();
	}

	/**
	 * Draws a piece on the canvas
	 * @param canvas
	 * @param xCrd 
	 * @param yCrd
	 * @param sqSize
	 * @param p
	 */
	protected final void drawPiece(Canvas canvas, int xCrd, int yCrd,
			int sqSize, int p) {
		String psb, psw;
		switch (p) {
		default:
		case Position.EMPTY:
			psb = null;
			psw = null;
			break;
		case Position.W_KING:
			psb = "H";
			psw = "k";
			break;
		case Position.W_QUEEN:
			psb = "I";
			psw = "l";
			break;
		case Position.W_ROOK:
			psb = "J";
			psw = "m";
			break;
		case Position.W_BISHOP:
			psb = "K";
			psw = "n";
			break;
		case Position.W_KNIGHT:
			psb = "L";
			psw = "o";
			break;
		case Position.W_PAWN:
			psb = "M";
			psw = "p";
			break;
		case Position.B_KING:
			psb = "N";
			psw = "q";
			break;
		case Position.B_QUEEN:
			psb = "O";
			psw = "r";
			break;
		case Position.B_ROOK:
			psb = "P";
			psw = "s";
			break;
		case Position.B_BISHOP:
			psb = "Q";
			psw = "t";
			break;
		case Position.B_KNIGHT:
			psb = "R";
			psw = "u";
			break;
		case Position.B_PAWN:
			psb = "S";
			psw = "v";
			break;
		}
		if (psb != null) {
			if (pieceXDelta < 0) {
				final Rect bounds = new Rect();
				blackPiecePaint.getTextBounds("H", 0, 1, bounds);
				pieceXDelta = (sqSize - (bounds.left + bounds.right)) / 2;
				pieceYDelta = (sqSize - (bounds.top + bounds.bottom)) / 2;
			}

			xCrd += pieceXDelta;
			yCrd += pieceYDelta;
			canvas.drawText(psw, xCrd, yCrd, whitePiecePaint);
			canvas.drawText(psb, xCrd, yCrd, blackPiecePaint);

		}
	}

	/**
	 * Converts an event coordinate to a square
	 * @param x
	 * @param y
	 * @return the square where the event occured
	 */
	public int eventToSquare(float x, float y) {
		int cx = (int) (x * 8 / getMeasuredWidth());
		int cy = (int) (y * 8 / getMeasuredWidth());

		if (cx > 7)
			cx = 7;
		if (cx < 0)
			cy = 0;
		if (cy > 7)
			cy = 7;
		if (cy < 0)
			cy = 0;

		Log.d("cbview", "sq:" + (cx + cy * 8));

		return (cx + cy * 8);
	}

	/**
	 * Initialisation
	 */
	protected void initChessBoardView() {
		setFocusable(true);

		blackSquarePaint = new Paint();
		whiteSquarePaint = new Paint();

		whitePiecePaint = new Paint();
		whitePiecePaint.setAntiAlias(true);

		blackPiecePaint = new Paint();
		blackPiecePaint.setAntiAlias(true);

		selectedSquarePaint = new Paint();
		selectedSquarePaint.setStyle(Paint.Style.STROKE);
		selectedSquarePaint.setStrokeWidth(5);

		final Resources r = this.getResources();
		blackSquarePaint.setColor(r.getColor(R.color.black_square_color));
		whiteSquarePaint.setColor(r.getColor(R.color.white_square_color));

		whitePiecePaint.setColor(r.getColor(R.color.bright_piece_color));
		blackPiecePaint.setColor(r.getColor(R.color.dark_piece_color));

		final Typeface chessFont = Typeface.createFromAsset(getContext()
				.getAssets(), "ChessCases.ttf");
		whitePiecePaint.setTypeface(chessFont);
		blackPiecePaint.setTypeface(chessFont);

		pieceXDelta = pieceYDelta = -1;

		position = new Position();
		animation = false;

	}

	/**
	 * Returns true if this view is accpeting events from the user
	 * @return
	 */
	public boolean isAcceptInput() {
		return acceptInput;
	}

	private int measure(int measureSpec) {
		int result = 0;
		// Decode the measurement specifications.
		final int specMode = MeasureSpec.getMode(measureSpec);
		final int specSize = MeasureSpec.getSize(measureSpec);
		if (specMode == MeasureSpec.UNSPECIFIED)
			// Return a default size of 200 if no bounds are specified.
			result = 200;
		else
			// As you want to fill the available space
			// always return the full available bounds.
			result = specSize;
		return result;
	}

	/**
	 * The drawing method
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		
		final long now = System.currentTimeMillis();
		if (animation && now >= animationStopTime) {
			animation = false;
			position = animationFinalPosition;
		}
			//invalidate();
			//return;
		
		pieceXDelta=-1;
		
		final int squareSize = getMeasuredWidth() >> 3;
		blackPiecePaint.setTextSize(squareSize);
		whitePiecePaint.setTextSize(squareSize);

		for (int x = 0; x < 8; x++) {
			final int squarePosX = x * squareSize;
			for (int y = 0; y < 8; y++) {

				final int squarePosY = y * squareSize;
				canvas.drawRect(squarePosX, squarePosY,
						squarePosX + squareSize, squarePosY + squareSize,
						((x + y) % 2 == 0) ? whiteSquarePaint
								: blackSquarePaint);

				final int square = (y << 3) + x;
				if (!position.isEmpty(square)
						&& !(animation && animationFrom == square))
					drawPiece(canvas, squarePosX, squarePosY, squareSize,
							position.pieceAt(square));

			}
		}

		if (selectedSquare != -1) {
			final int cx = (selectedSquare % 8) * squareSize;
			final int cy = (selectedSquare / 8) * squareSize;
			canvas.drawRect(cx, cy, cx + squareSize, cy + squareSize,
					selectedSquarePaint);
		}

		if (animation) {
			/*
			final long now = System.currentTimeMillis();
			if (now >= animationStopTime) {
				animation = false;
				position = animationFinalPosition;
				invalidate();
				return;
			}*/

			final float animationProgress = (float) (now - animationStartTime)
					/ (float) (animationStopTime - animationStartTime);

			double pieceX = (animationFrom % 8) * (1.0 - animationProgress)
					+ (animationTo % 8) * animationProgress;
			double pieceY = (animationFrom / 8) * (1.0 - animationProgress)
					+ (animationTo / 8) * animationProgress;

			pieceX *= squareSize;
			pieceY *= squareSize;

			drawPiece(canvas, (int) pieceX, (int) pieceY, squareSize,
					animationPiece);
			invalidate();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// The compass is a circle that fills as much space as possible.
		// Set the measured dimensions by figuring out the shortest boundary,
		// height or width.
		final int measuredWidth = measure(widthMeasureSpec);
		final int measuredHeight = measure(heightMeasureSpec);
		final int d = Math.min(measuredWidth, measuredHeight);
		setMeasuredDimension(d, d);
	}

	/**
	 * Handles touch events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!acceptInput) return false;
		
		// Log.d("chessboardview","event:"+event.getX()+","+event.getY());
		final int touchSquare = eventToSquare(event.getX(), event.getY());

		if (((position.sideToPlay() && position.whitePieceAt(touchSquare)) || (!position
				.sideToPlay() && position.blackPieceAt(touchSquare))))
		{
			selectedSquare = touchSquare;
		}
		else if (selectedSquare != -1) {
			final String moveString = Position.squareName[selectedSquare]
					+ Position.squareName[touchSquare];
			
			// TODO add promotion
			if(	(position.pieceAt(selectedSquare)==Position.W_PAWN && position.sideToPlay() && touchSquare < Position.A7)
				||(position.pieceAt(selectedSquare)==Position.B_PAWN && !position.sideToPlay() && touchSquare > Position.H2)
				)
			{
				final CharSequence[] items = {"Queen", "Rook", "Bishop", "Knight"};
				final CharSequence[] itemsStr = {"q", "r", "b", "n"};

				AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
				builder.setTitle("Choose Promotion");
				builder.setItems(items, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				        //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
				    	
				    	final Move move = new Move(moveString+itemsStr[item], position);
				    	
				    	if (position.getLegalMoves().contains(move)) {
							selectedSquare = -1;
							Log.d("chessbourad", move.toString());
							if(moveListener!=null)
								moveListener.onMove(move);
						}
				    }
				});
				AlertDialog alert = builder.create();
				alert.show();
				selectedSquare=-1;
				invalidate();
				return true;
			}
			
			
			final Move move = new Move(moveString, position);

			if (position.getLegalMoves().contains(move)) {
				selectedSquare = -1;
				Log.d("chessbourad", move.toString());
				if(moveListener!=null)
					moveListener.onMove(move);
			}
		}

		invalidate();
		return true;
	}

	public void setAcceptInput(boolean acceptInput) {
		this.acceptInput = acceptInput;
	}
	
	public void setOnMoveListener(OnMoveListener moveListener) {
		this.moveListener = moveListener;
	}

	public void setPosition(Position p) {
		animation = false;
		position = (Position) p.clone();
		invalidate();
	}

	/**
	 * Called when the game that this view is observing has changed
	 */
	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
		if(data instanceof Move)
		{
			Log.d("cbview","ANIMATE");
			animateMove((Move) data, ((Game)observable).getCurrentPosition());
		}
		else
		{
			Log.d("cbview","NO ANIMATE");
			setPosition(((Game)observable).getCurrentPosition());
			invalidate();
		}
	}

}
