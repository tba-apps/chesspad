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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Observable;
import java.util.Vector;

import org.apache.http.util.ByteArrayBuffer;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This class handles an UCI engine as a Player.
 * @author Jean-Francois Romang <info at chesspad dot net>
 *
 */
public class UCIEngine extends Player implements Runnable {

	public static final int BESTLINE_MESSAGE = 0, CURRENTMOVE_MESSAGE = 1,
			DEPTH_MESSAGE = 2, MOVENUMBER_MESSAGE = 3, NODES_MESSAGE = 4,
			NPS_MESSAGE = 5, NAME_MESSAGE = 6, BESTMOVE_MESSAGE=7, SCORE_MESSAGE=8, TIME_MESSAGE=9, AUTHOR_MESSAGE=10;

	private Process engineProcess = null;
	private InputStream in;
	private OutputStream out;
	private BufferedWriter outwr;
	private Thread reader;
	private File engineFile;
	private final Context context;
	private final ProgressDialog waitDialog;
	private final Handler refreshHandler;
	public String name, author, engineId;
	public Vector<UCIOption> options;
	private static final String commands = "|id|uciok|readyok|bestmove|copyprotection|registration|info|option|type|default|min|max|var|author|name|ponder|depth|seldepth|time|nodes|pv|multipv|score|currmove|currmovenumber|hasfull|nps|tbhits|cpuload|string|refutation|currline|hashfull|cp|mate|lowerbound|upperbound|currmovenumber|currmove|";

	public UCIEngine(Game game, String engineName, Context context,
			ProgressDialog waitDialog, Handler refreshHandler) {
		super(game, Role.OBSERVER);
		this.context = context;
		this.waitDialog = waitDialog;
		this.refreshHandler = refreshHandler;
		
		engineId = name = engineName;
		author = "Unknown";
	
		try {
	    	 //Copy the engine to a file
	    	 
	    	 engineFile=new File(context.getFilesDir(),engineName);
	    	 if(!engineFile.exists())
	    	 {
	    		 // Copy the engine file from the ressources...
	    		 if(Arrays.asList(context.getResources().getAssets().list(context.getFilesDir().getAbsolutePath())).contains(engineName))
	    		 {
	    			 InputStream is=context.getResources ().getAssets().open(engineName);
	    			 FileOutputStream os = new FileOutputStream(engineFile);
	    			 byte[] data = new byte[is.available()];
	    			 is.read(data);
	    			 os.write(data);
	    			 is.close();
	    			 os.close();
	    		 }
	    		 else //...or download it
	    		 {
	    			 downloadFromUrl(engineName,engineFile);
	    		 }

	    		 Runtime.getRuntime().exec("/system/bin/chmod 744 "+engineFile.getAbsolutePath());
	    		 //engineFile.
	    	 }
		} catch (final IOException e) {
			Log.e("ucierror", e.getMessage());
		}

		options = new Vector<UCIOption>();
	}
	
	/**
	 * Launches the engine process
	 */
	private void launchProcess()
	{
		
		try
		{	
			// Launch the process
			final ProcessBuilder processBuilder = new ProcessBuilder(
					engineFile.getAbsolutePath());
			engineProcess = processBuilder.redirectErrorStream(true).start();
			in = engineProcess.getInputStream();
			out = engineProcess.getOutputStream();
			outwr = new BufferedWriter(new OutputStreamWriter(out));
		} catch (final IOException e) {
			Log.e("ucierror", e.getMessage());
		}

		send("uci");
		reader = new Thread(this);
		reader.start();
		
	}

	/**
	 * Constructs a view allowing the user the change the engine parameters.
	 * @return
	 */
	public View getParametersEditView() {
		final ScrollView sv = new ScrollView(context);
		sv.setPadding(8, 0, 8, 0);
		final LinearLayout l = new LinearLayout(context);
		l.setOrientation(LinearLayout.VERTICAL);
		sv.addView(l);

		for (final UCIOption option : options)
			switch (option.type) {
			case STRING:
				final TextView textViewEd = new TextView(context);
				textViewEd.setText("\n" + option.name);
				l.addView(textViewEd);

				final EditText editText = new EditText(context);
				editText.setText(option.getValue().getString());
				editText.setOnKeyListener(new View.OnKeyListener() {

					public boolean onKey(View v, int keyCode, KeyEvent event) {
						// TODO Auto-generated method stub
						Log.d("uciengine", "key!");
						option.setValue(Variant.valueOf(editText.getText()
								.toString()));
						send(option.toString());
						return false;
					}
				});

				l.addView(editText);
				break;

			case COMBO:
				final TextView textViewSp = new TextView(context);
				textViewSp.setText("\n" + option.name);
				l.addView(textViewSp);
				final Spinner spinner = new Spinner(context);

				// Application of the Array to the Spinner
				final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
						context, android.R.layout.simple_spinner_item,
						option.vars);
				spinnerArrayAdapter
						.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The
																									// drop
																									// down
																									// vieww
				spinner.setAdapter(spinnerArrayAdapter);
				spinner.setSelection(spinnerArrayAdapter.getPosition(option
						.getValue().getString()));

				spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						// TODO Auto-generated method stub
						Log.d("uciengine", option.vars.elementAt((int) id));
						option.setValue(Variant.valueOf(option.vars
								.elementAt((int) id)));
						send(option.toString());
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub
					}
				});

				l.addView(spinner);
				break;

			case BUTTON:
				final Button button = new Button(context);
				button.setText(option.name);
				button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						send(option.toString());
					}
				});

				l.addView(button);
				break;

			case CHECK:
				final CheckBox checkBox = new CheckBox(context);
				checkBox.setText(option.name);
				checkBox.setChecked(option.getValue().getBoolean());
				checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						Log.d("ucioptions", "check!");
						option.setValue(isChecked ? Variant.valueOf("true")
								: Variant.valueOf("false"));
						send(option.toString());
						Log.d("ucioptions", "value:"
								+ option.getValue().getBoolean());
					}
				});
				l.addView(checkBox);
				break;
			case SPIN:
				final TextView textViewSl = new TextView(context);
				textViewSl.setText("\n[" + option.getValue().getInt() + "] "
						+ option.name);
				l.addView(textViewSl);

				final SeekBar slider = new SeekBar(context);
				slider.setMax(option.max - option.min);

				// special case for hash
				if (option.name.equals("Hash"))
					slider.setMax(256);

				slider.setProgress(option.getValue().getInt() - option.min);

				slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// TODO Auto-generated method stub
						final int barValue = progress + option.min;
						option.setValue(Variant.valueOf(Integer
								.toString(barValue)));
						send(option.toString());
						textViewSl.setText("\n[" + option.getValue().getInt()
								+ "] " + option.name);
					}

					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}
				});

				l.addView(slider);
				break;

			default:
				final TextView textView = new TextView(context);
				textView.setText("\n" + option.name);
				l.addView(textView);
			}

		return sv;
	}

	public void pause() {
		send("quit");
		SystemClock.sleep(100); //We give a chance to the engine to close properly...
		try {
			engineProcess.exitValue();
			Log.d("uciengine", "shutdown ok");
		} catch (final IllegalThreadStateException e) {
			//...and we kill it if it has not shut down.
			Log.d("uciengine", "killing engine process");
			engineProcess.destroy();
		}
	}

	/**
	 * Handles UCI commands sent by the engine
	 * @param commandString
	 */
	void processCommand(String commandString) {

		final String tokens[] = commandString.split(" ");
		final Vector<String> words = new Vector<String>();
		String currentToken = "";
		for (final String s : tokens)
			if (commands.contains("|" + s + "|")) {
				if (currentToken.length() > 0)
					words.add(currentToken);
				words.add(s);
				currentToken = "";
			} else if (currentToken.length() == 0)
				currentToken += s;
			else
				currentToken += " " + s;
		if (currentToken.length() > 0)
			words.add(currentToken);

		// for(String s : words)
		// Log.d("uciwords", s);

		int index;
		final int maxIndex = words.size() - 1;

		if (words.size() == 0)
			return;

		if (words.get(0).equals("info")) {
			index = words.indexOf("depth");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = DEPTH_MESSAGE;
				refreshHandler.sendMessage(message);
			}

			index = words.indexOf("nps");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = NPS_MESSAGE;
				refreshHandler.sendMessage(message);
			}

			index = words.indexOf("nodes");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = NODES_MESSAGE;
				refreshHandler.sendMessage(message);
			}

			index = words.indexOf("currmove");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = CURRENTMOVE_MESSAGE;
				refreshHandler.sendMessage(message);
			}

			index = words.indexOf("pv");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = BESTLINE_MESSAGE;
				refreshHandler.sendMessage(message);
			}

			index = words.indexOf("currmovenumber");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = MOVENUMBER_MESSAGE;
				refreshHandler.sendMessage(message);
			}
			
			index = words.indexOf("cp");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = SCORE_MESSAGE;
				refreshHandler.sendMessage(message);
			}
			
			index = words.indexOf("time");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				message.obj = words.get(index + 1);
				message.what = TIME_MESSAGE;
				refreshHandler.sendMessage(message);
			}
			

		}
		else if (words.get(0).equals("bestmove")) {
			if(words.size()>1)
			{
				Position p=game.getCurrentPosition();
				Move move=new Move(words.get(1),p); //TODO check if move is legal (send exception in move contructor)
				//Log.d("playing move","uci:"+)
				//game.playMove(m);
				final Message message = new Message();
				message.obj=move;
				message.what= BESTMOVE_MESSAGE;
				
				//check if we are the current player, then play the move
				if( (p.sideToPlay()&&role==Role.WHITE_PLAYER)
					||(!p.sideToPlay()&&role==Role.BLACK_PLAYER)
				)
					refreshHandler.sendMessage(message);
			}
		}
		else if (words.get(0).equals("id")) {
			index = words.indexOf("name");
			if (index > -1 && index < maxIndex) {
				final Message message = new Message();
				name = words.get(index + 1);
				// http://stackoverflow.com/questions/1536654/androidandroid-view-viewrootcalledfromwrongthreadexception-how-to-solve-the-p
				Log.d("uciengine", "INNAME");
				message.obj = name;
				message.what = NAME_MESSAGE;
				refreshHandler.sendMessage(message);
				// if(engineNameView!=null) engineNameView.setText(name);
			}
			index = words.indexOf("author");
			if (index > -1 && index < maxIndex)
			{
				author = words.get(index + 1);
				Message message=new Message();
				message.what= AUTHOR_MESSAGE;
				refreshHandler.sendMessage(message);
				//Toast.makeText(context, name+"\n by "+author, Toast.LENGTH_SHORT).show();
			}
		} else if (words.get(0).equals("option")) {
			String optionName = "Unnamed";
			UCIOption.Type optionType = UCIOption.Type.CHECK;
			Variant optionValue = Variant.valueOf("false");
			final Vector<String> vars = new Vector<String>();
			int min = 1, max = 99;

			index = words.indexOf("name");
			if (index > -1 && index < maxIndex)
				optionName = words.get(index + 1);

			index = words.indexOf("type");
			if (index > -1 && index < maxIndex) {
				final String typeString = words.get(index + 1);
				if (typeString.equals("check"))
					optionType = UCIOption.Type.CHECK;
				if (typeString.equals("spin"))
					optionType = UCIOption.Type.SPIN;
				if (typeString.equals("combo"))
					optionType = UCIOption.Type.COMBO;
				if (typeString.equals("button"))
					optionType = UCIOption.Type.BUTTON;
				if (typeString.equals("string"))
					optionType = UCIOption.Type.STRING;
			}

			index = words.indexOf("default");
			if (index > -1 && index < maxIndex)
				optionValue = Variant.valueOf(words.get(index + 1));

			index = words.indexOf("min");
			if (index > -1 && index < maxIndex)
				min = Integer.parseInt(words.get(index + 1));

			index = words.indexOf("max");
			if (index > -1 && index < maxIndex)
				max = Integer.parseInt(words.get(index + 1));

			// var

			index = 0;
			while ((index = words.indexOf("var", index + 1)) != -1)
				vars.add(words.get(index + 1));

			// while( (i=stringList.indexOf("var",i))!=-1 )
			// vars.append(stringList.at(++i));

			options.add(new UCIOption(context, engineId, optionType,
					optionName, optionValue, min, max, vars));
		} else if (words.get(0).equals("uciok")) {
			Log.d("uciengine", "in uciok");
			// On envoie les parametres differents de ceux par defaut
			for (final UCIOption option : options)
				if (!option.getValue().getString()
						.equals(option.defaultValue.getString()))
					send(option.toString());

			waitDialog.dismiss();
		}
	}

	public void resume() {
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		launchProcess();
	}

	public void run() {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				in), 16);
		String s;
		try {
			while ((s = reader.readLine()) != null) {
				Log.d("ENgine->GUI", s);
				processCommand(s);
				/*
				 * 
				 * try { String s=reader.readLine(); if(s!=null) {
				 * Log.d("ENgine->GUI",s); processCommand(s); }
				 * 
				 * 
				 * } catch (IOException e) { // TODO Auto-generated catch block
				 * //e.printStackTrace(); Log.e("uciengine",e.getMessage()); }
				 */
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			Log.e("uciengine", e.getMessage());
		}

		Log.d("uciengine", "ENDRUN");
	}

	void send(String s) {
		s += "\n";
		Log.d("GUI->Engine", s);
		try {
			if (outwr != null) {
				outwr.write(s);
				outwr.flush();
			} else
				Log.e("uciengine", "nullout");

		} catch (final IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			Log.e("uciengine", e.getMessage());
		}
	}

	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
		send(game.toString());
		boolean side=game.getCurrentPosition().sideToPlay();
		if((role==Role.WHITE_PLAYER && side) ||	(role==Role.BLACK_PLAYER && !side))
			send("go movetime 5000");
		else if(role==Role.OBSERVER)
		{
			send("stop");
			send(game.toString());
			send("go infinite");
		}
	}
	
	/**
	 * Downloads the engine from the chesspad.net website
	 * @param engineFileName the engine's file name on the website
	 * @param file the file to save
	 */
    public static void downloadFromUrl(String engineFileName, File file) {  //this is the downloader method	
		//final String engineFilePath = "/data/data/com.helloandroid.imagedownloader/"; //put the downloaded file here
            try {
                    URL url = new URL("http://www.chesspad.net/engines/arm/" + engineFileName); //you can write here any link
                    //File file = new File(fileName);

                    long startTime = System.currentTimeMillis();
                    Log.d("ImageManager", "download begining");
                    Log.d("ImageManager", "download url:" + url);
                    Log.d("ImageManager", "downloaded file name:" + file.getAbsolutePath());
                    /* Open a connection to that URL. */
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

                    /* Convert the Bytes read to a String. */
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baf.toByteArray());
                    fos.close();
                   
                    Log.d("ImageManager", "download ready in"
                                    + ((System.currentTimeMillis() - startTime) / 1000)
                                    + " sec");

            } catch (IOException e) {
                    Log.d("ImageManager", "Error: " + e);
            }

    }
}
