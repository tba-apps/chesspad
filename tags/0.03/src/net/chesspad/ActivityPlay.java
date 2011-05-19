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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import net.chesspad.Player.Role;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Activity allos the user to play a Game against the computer
 * @author Jean-Francois Romang <info at chesspad dot net>
 *
 */
public class ActivityPlay extends Activity {

	UCIEngine engine = null;
	HumanPlayer human=null;
	//private TextView engineInfoTextView[];
	private Game game;
	private PowerManager.WakeLock wakeLock;
	private boolean analyse=false;
	private Player.Role engineRoleBeforeAnalyse=null;
	private long searchDuration=0;
	private Vector<String> availableEngines;
	
	//[depth] score line.... \n
	//movenumber:currentmove t:time n:nodes nps:nps
	private String depthString="", scoreString="" , lineString="", moveNumberString="", 
					currentMoveString="", timeString="", nodesString="", npsString="";
	
	private DecimalFormat scoreFormatter;
	private static final int DIALOG_ENGINE_PARAMETERS=1;
	

	public void clic(View view) {
		//Log.d("CBoard", "yepeee");
		showDialog(1);
		//Log.d("playwithcomp", "showdialog!");
	}

	public void analyse(View view) {
		if(!analyse)
		{
			((ImageButton)findViewById(R.id.analyseButton)).setImageResource(R.drawable.systemsearchstop);
			engineRoleBeforeAnalyse=engine.getRole();
			engine.setRole(Role.OBSERVER);
			engine.send("stop");
			engine.update(game,null);
		}
		else
		{
			((ImageButton)findViewById(R.id.analyseButton)).setImageResource(R.drawable.systemsearch);
			engine.send("stop");
			engine.setRole(engineRoleBeforeAnalyse);	
			engine.update(game,null);
		}
		
		analyse=!analyse;
		//Log.d("CBoard", "yepeee2");
		//engine.send("go infinite");
		//Intent myIntent = new Intent(PlayWithComputer.this, InstallEngines.class);
		//PlayWithComputer.this.startActivity(myIntent);

	}
	
	public void switchPlayers(View view)
	{
		engine.send("stop");
		/*
		Player.Role tmp=engine.getRole();
		engine.setRole(human.getRole());
		human.setRole(tmp);
		human.update(game, null);*/
		engine.setRole(human.getRole()==Role.WHITE_PLAYER?Role.WHITE_PLAYER:Role.BLACK_PLAYER);
		human.setRole(human.getRole()==Role.WHITE_PLAYER?Role.BLACK_PLAYER:Role.WHITE_PLAYER);
		
		engine.update(game, null);
	}
	
	public void back(View v)
	{
		if(engine.role!=Role.OBSERVER)
		{
			engine.setRole(game.getCurrentPosition().sideToPlay()?Role.WHITE_PLAYER:Role.BLACK_PLAYER);
			engine.send("stop");
		}
		game.back();
	}
	
	public void forward(View v)
	{
		//engine.send("stop");
		game.forward();
		if(game.isInPlayingMode() && engine.role!=Role.OBSERVER)
			engine.setRole(human.getRole()==Role.WHITE_PLAYER?Role.BLACK_PLAYER:Role.WHITE_PLAYER);
		engine.update(game, null);
	}
	
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//findViewById(R.id.linearLayout1).setBackgroundResource(R.drawable.bg_black_gradient);
		
		Log.d("playWithComputer", "onCreate");

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK , "ChessPad");
		scoreFormatter = new DecimalFormat("###.##");
		availableEngines=new Vector<String>();
	
		// http://www.mail-archive.com/android-developers@googlegroups.com/msg64308.html
		// http://gimite.net/en/index.php?Run%20native%20executable%20in%20Android%20App
		
		//download file from url
		//http://www.helloandroid.com/tutorials/how-download-fileimage-url-your-device

		ProgressDialog waitDialog = ProgressDialog.show(this, "",
				"Loading. Please wait...", true);

		//final ViewStub stub = (ViewStub) findViewById(R.id.viewStub1);
		/* View inflated = *///stub.inflate();

		/*
		engineInfoTextView = new TextView[6];
		engineInfoTextView[UCIEngine.BESTLINE_MESSAGE] = ((TextView) findViewById(R.id.bestLineTextView));
		engineInfoTextView[UCIEngine.CURRENTMOVE_MESSAGE] = ((TextView) findViewById(R.id.currentMoveTextView));
		engineInfoTextView[UCIEngine.MOVENUMBER_MESSAGE] = ((TextView) findViewById(R.id.moveNumberTextView));
		engineInfoTextView[UCIEngine.DEPTH_MESSAGE] = ((TextView) findViewById(R.id.depthTextView));
		engineInfoTextView[UCIEngine.NODES_MESSAGE] = ((TextView) findViewById(R.id.nodesTextView));
		engineInfoTextView[UCIEngine.NPS_MESSAGE] = ((TextView) findViewById(R.id.npsTextView));*/
		//engineInfoTextView[UCIEngine.NAME_MESSAGE] = ((TextView) findViewById(R.id.engineNameTextView));
		
		//[depth] score line.... \n
		//movenumber:currentmove t:time n:nodes nps:nps
		//final TextView engineTextView=((TextView) findViewById(R.id.engineOutputView));
		
		final Context context=this;
		
		final Handler hRefresh = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				
				/*
				if(msg.what<UCIEngine.NAME_MESSAGE)
					engineInfoTextView[msg.what].setText((String) msg.obj);
				else if(msg.what==UCIEngine.NAME_MESSAGE)
					setTitle(getString(R.string.app_name)+" - "+((String) msg.obj));
				else if(msg.what==UCIEngine.BESTMOVE_MESSAGE)
					game.playMove((Move)msg.obj);*/
				switch(msg.what)
				{
					case UCIEngine.NAME_MESSAGE:
						setTitle(getString(R.string.app_name)+" - "+((String) msg.obj));
						break;
					case UCIEngine.DEPTH_MESSAGE:
						depthString=(String)msg.obj;
						break;
					case UCIEngine.SCORE_MESSAGE:
						scoreString=scoreFormatter.format(Double.parseDouble((String)msg.obj)/100.0); //TODO convert to centipawns
						break;
					case UCIEngine.BESTLINE_MESSAGE:
						if(searchDuration>1000)
						{
							lineString=""; //TODO convert algebraic
							Position p_bestline=game.getCurrentPosition();
							List<String> moves=Arrays.asList(((String)msg.obj).split(" "));
							for(String moveString:moves)
							{
								Move m=new Move(moveString,p_bestline);
								lineString+=m.toAlgebraicNotation(p_bestline)+" ";
								p_bestline.makeMove(m);
							}
						}
						else lineString=(String)msg.obj;
						break;
					case UCIEngine.MOVENUMBER_MESSAGE:
						moveNumberString=(String)msg.obj;
						break;
					case UCIEngine.CURRENTMOVE_MESSAGE:
						/*if(searchDuration>2000)
						{
							Move m=new Move((String)msg.obj,game.getCurrentPosition());
							currentMoveString=m.toFastNotation(); //TODO convert algebraic
						}
						else*/ currentMoveString=(String)msg.obj;
						break;
					case UCIEngine.TIME_MESSAGE:
						searchDuration=Long.parseLong((String)msg.obj);
						timeString=millisToHMS(searchDuration);
						break;
					case UCIEngine.NODES_MESSAGE:
						nodesString=(String)msg.obj;
						break;
					case UCIEngine.NPS_MESSAGE:
						npsString=(String)msg.obj;
						break;
					case UCIEngine.BESTMOVE_MESSAGE:
						game.playMove((Move)msg.obj);
						lineString=moveNumberString=currentMoveString=timeString=nodesString=npsString="";
						searchDuration=0;
						break;
					case UCIEngine.AUTHOR_MESSAGE:	
						Toast.makeText(context,engine.name+"\n by "+engine.author, Toast.LENGTH_LONG).show();
						break;
				}
				TextView engineTextView=((TextView) findViewById(R.id.engineOutputView));
				engineTextView.setText("["+depthString+"] "+scoreString+" "+lineString+"\n"+
						moveNumberString+":"+currentMoveString
						+(timeString.equals("") ? "" : " t:"+timeString )
						+(nodesString.equals("") ? "" : " n:"+nodesString)
						+(npsString.equals("") ? "" : " nps:"+npsString)
						);
			}
		};

		//Create the game
		game=new Game();
			
		//Create players
		if (engine == null)
			engine = new UCIEngine(game, "toga2-android", this, waitDialog, hRefresh);
		engine.setRole(Player.Role.BLACK_PLAYER);
		
		human=new HumanPlayer(game, Player.Role.WHITE_PLAYER, (ChessBoardView)findViewById(R.id.chessboard));
		
		//add game observers
		game.addObserver((ChessBoardView)findViewById(R.id.chessboard));
		game.addObserver(engine);
		game.addObserver(human);
		game.addObserver((GameTextView)findViewById(R.id.gametextview));

	}
	
	private void changeEngine(String engineName)
	{
		Player.Role role=Player.Role.BLACK_PLAYER;
		if(engine!=null)
			{
				role=engine.getRole();
				engine.pause();
				game.deleteObserver(engine);
			}
		ProgressDialog waitDialog = ProgressDialog.show(this, "",
				"Loading. Please wait...", true);
		//engine=new UCIEngine(game,engineName, this,waitDialog);

		final Context context=this;

		final Handler hRefresh = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				/*
				if(msg.what<UCIEngine.NAME_MESSAGE)
					engineInfoTextView[msg.what].setText((String) msg.obj);
				else if(msg.what==UCIEngine.NAME_MESSAGE)
					setTitle(getString(R.string.app_name)+" - "+((String) msg.obj));
				else if(msg.what==UCIEngine.BESTMOVE_MESSAGE)
					game.playMove((Move)msg.obj);*/
				switch(msg.what)
				{
				case UCIEngine.NAME_MESSAGE:
					setTitle(getString(R.string.app_name)+" - "+((String) msg.obj));
					break;
				case UCIEngine.DEPTH_MESSAGE:
					depthString=(String)msg.obj;
					break;
				case UCIEngine.SCORE_MESSAGE:
					scoreString=scoreFormatter.format(Double.parseDouble((String)msg.obj)/100.0); //TODO convert to centipawns
					break;
				case UCIEngine.BESTLINE_MESSAGE:
					if(searchDuration>1000)
					{
						lineString=""; //TODO convert algebraic
						Position p_bestline=game.getCurrentPosition();
						List<String> moves=Arrays.asList(((String)msg.obj).split(" "));
						for(String moveString:moves)
						{
							Move m=new Move(moveString,p_bestline);
							lineString+=m.toAlgebraicNotation(p_bestline)+" ";
							p_bestline.makeMove(m);
						}
					}
					else lineString=(String)msg.obj;
					break;
				case UCIEngine.MOVENUMBER_MESSAGE:
					moveNumberString=(String)msg.obj;
					break;
				case UCIEngine.CURRENTMOVE_MESSAGE:
					/*if(searchDuration>2000)
						{
							Move m=new Move((String)msg.obj,game.getCurrentPosition());
							currentMoveString=m.toFastNotation(); //TODO convert algebraic
						}
						else*/ currentMoveString=(String)msg.obj;
						break;
				case UCIEngine.TIME_MESSAGE:
					searchDuration=Long.parseLong((String)msg.obj);
					timeString=millisToHMS(searchDuration);
					break;
				case UCIEngine.NODES_MESSAGE:
					nodesString=(String)msg.obj;
					break;
				case UCIEngine.NPS_MESSAGE:
					npsString=(String)msg.obj;
					break;
				case UCIEngine.BESTMOVE_MESSAGE:
					game.playMove((Move)msg.obj);
					lineString=moveNumberString=currentMoveString=timeString=nodesString=npsString="";
					searchDuration=0;
					break;
				case UCIEngine.AUTHOR_MESSAGE:	
					Toast.makeText(context,engine.name+"\n by "+engine.author, Toast.LENGTH_LONG).show();
					break;
				}
				TextView engineTextView=((TextView) findViewById(R.id.engineOutputView));
				engineTextView.setText("["+depthString+"] "+scoreString+" "+lineString+"\n"+
						moveNumberString+":"+currentMoveString
						+(timeString.equals("") ? "" : " t:"+timeString )
						+(nodesString.equals("") ? "" : " n:"+nodesString)
						+(npsString.equals("") ? "" : " nps:"+npsString)
				);
			}
		};
		
		engine = new UCIEngine(game, engineName, this, waitDialog, hRefresh);
		engine.setRole(role);
		game.addObserver(engine);
		engine.resume();
		this.removeDialog(DIALOG_ENGINE_PARAMETERS);
		engine.update(game, null);
	}
	
	
	private static String millisToHMS(long duration) {
		long s=duration/1000;
		if(s>=3600) return String.format("%d:%02d:%02d", s/3600, (s%3600)/60, (s%60));
		else return String.format("%02d:%02d", (s%3600)/60, (s%60));
	  }

	//
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		Log.d("createdialog","increate");
		if (id == DIALOG_ENGINE_PARAMETERS) // UCI engine parameters dialog
		{
			Log.d("createdialog","params");
			dialog = new Dialog(this);
			dialog.setContentView(engine.getParametersEditView(),
					new LayoutParams(
							android.view.ViewGroup.LayoutParams.FILL_PARENT,
							android.view.ViewGroup.LayoutParams.FILL_PARENT));
			dialog.setTitle(engine.name);
		}

		return dialog;
	}
	

	// Called at the end of the full lifetime.
	@Override
	public void onDestroy() {
		// Clean up any resources including ending threads,
		// closing database connections etc.
		super.onDestroy();
		Log.d("playWithComputer", "onDestroy");
	}

	// Called at the end of the active lifetime.
	@Override
	public void onPause() {
		// Suspend UI updates, threads, or CPU intensive processes
		// that don�t need to be updated when the Activity isn�t
		// the active foreground activity.
		super.onPause();
		wakeLock.release();
		Log.d("playWithComputer", "onPause");
		engine.pause();
	}

	// Called before subsequent visible lifetimes
	// for an activity process.
	@Override
	public void onRestart() {
		super.onRestart();
		// Load changes knowing that the activity has already
		// been visible within this process.
		Log.d("playWithComputer", "onRestart");
	}

	// Called after onCreate has finished, use to restore UI state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		Log.d("playWithComputer", "onRestoreInstanceState");
		game=(Game)savedInstanceState.getSerializable("game");
		game.notify();
	}

	// Called at the start of the active lifetime.
	@Override
	public void onResume() {
		super.onResume();
		// Resume any paused UI updates, threads, or processes required
		// by the activity but suspended when it was inactive.
		Log.d("playWithComputer", "onResume");
		wakeLock.acquire();
		//wakeLock.acquire();
		
		engine.resume();
		engine.update(game, null);
		//game.n
	}

	// Called to save UI state changes at the
	// end of the active lifecycle.
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("game", game);
		Log.d("playWithComputer", "onSaveInstanceSTate");
	}

	// Called at the start of the visible lifetime.
	@Override
	public void onStart() {
		super.onStart();
		// Apply any required UI change now that the Activity is visible.
		Log.d("playWithComputer", "onStart");
	}

	// Called at the end of the visible lifetime.
	@Override
	public void onStop() {
		// Suspend remaining UI updates, threads, or processing
		// that aren�t required when the Activity isn�t visible.
		// Persist all edits or state changes
		// as after this call the process is likely to be killed.
		super.onStop();
		
		Log.d("playWithComputer", "onStop");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.play_menu, menu);
	    
	    SubMenu engine_menu=menu.addSubMenu("Computer Engine");
	    engine_menu.setGroupCheckable(1, true, true);
	    engine_menu.setIcon(R.drawable.ic_menu_laptop);
	    
	    String enginesList="Toga II 1.4.1SE@toga2-android";
	    try {
			URL url = new URL("http://www.chesspad.net/engines/arm/engines.txt"); //you can write here any link
			URLConnection ucon = url.openConnection();
			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}

			enginesList=new String(baf.toByteArray ());
			//Log.d("ENGINELIST",new String(baf.toByteArray ()));
		} catch (IOException e) {
			//Log.d("ENGINELIST", "Error: " + e);
		}
		String engines[] = enginesList.split("\\r?\\n");
		availableEngines.clear();
		for(String e:engines){
			String engineDetails[]=e.split("@");
			engine_menu.add(1, availableEngines.size()+100, Menu.NONE, engineDetails[0]);
			availableEngines.add(engineDetails[1]);
		}
		
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.item_engine_parameters:
	    	showDialog(DIALOG_ENGINE_PARAMETERS);
	        return true;
	    case R.id.item_settings:
		    Intent i = new Intent(this, Preference.class);
		    startActivity(i);
	    	return true;
	    case R.id.item_new_game:
	    	engine.send("stop");
	    	engine.send("ucinewgame");
	    	game.setStartPosition(new Position());
	    	return true;
	    /*case R.id.i:
	    	showDialog(DIALOG_ENGINE_PARAMETERS);
	        return true;*/
	    //case R.id.help:
	    //    showHelp();
	    //    return true;
	    default:
	    	if(item.getItemId()>=100) //change uci engine
	    	{
	    		changeEngine(availableEngines.get(item.getItemId()-100));
	    	}
	    	Log.d("ITEMSELECTION",Integer.toString(item.getItemId()));
	        return super.onOptionsItemSelected(item);
	    }
	}
	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);

	    // Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	        //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
	        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	        //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
	        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }
	    
	    setContentView(R.layout.main);
	  //add game observers
	    human=new HumanPlayer(game, human.getRole(), (ChessBoardView)findViewById(R.id.chessboard));
	    
		game.addObserver((ChessBoardView)findViewById(R.id.chessboard));
		game.addObserver(human);
		game.addObserver((GameTextView)findViewById(R.id.gametextview));
	    /*
	    // Checks whether a hardware keyboard is available
	    if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
	        Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show();
	    } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
	        Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show();
	    }*/
	}
}