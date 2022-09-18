/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class TeachingTinaReadAlongTimingCreator {
	private long audio_start_time_in_millis;
	private boolean is_audio_playing = false;
	private JFrame frame;
	private JButton button_save_timings;
	private MyAudioPlayer audio_player;
	private MyAudioButton button_play_stop;
	private ArrayList<MyTimingJLabel> words_list;
	private File timings_file;
	private File audio_file;
	private MyFlashcardManager main_app;
	
	TeachingTinaReadAlongTimingCreator( ArrayList<String> words, File audio_file, File timings_file, MyFlashcardManager main_app ) {
		this. main_app = main_app;
		this.audio_file = audio_file;
		this.timings_file = timings_file;

		this.frame = new JFrame( "Read Along Timing Creator" );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		
		button_save_timings = new JButton( "Save timings to file" );
		button_save_timings.addActionListener( new TeachingTinaReadAlongTimingCreator.SaveTimingsActionListener() );
		button_play_stop = new MyAudioButton();
		button_play_stop.addActionListener( new TeachingTinaReadAlongTimingCreator.PlayStopAudioListener() );
		
		frame.setLayout( new FlowLayout() );
		
		frame.add( button_save_timings );
		frame.add( button_play_stop );
		button_play_stop.requestFocus();
		
		createWordJLabels( words );
		loadTimingsArrayFromFile( timings_file );
		
		frame.setVisible(true);

		// Load the audio player
		try {
			this.audio_player = new MyAudioPlayer( this.audio_file );
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	public void createWordJLabels( ArrayList<String> words ) {
		this.words_list = new ArrayList<MyTimingJLabel>();
		for( int i = 0; i < words.size(); i++ ) {
			MyTimingJLabel lbl = new MyTimingJLabel( words.get( i ));
			
			lbl.addMouseListener( new LabelClicked( lbl ) );
			
			this.words_list.add( lbl );

			this.frame.add( this.words_list.get(i) );
		}
	}

	public void saveTimingsArrayToFile( File timings_file ) {
		String line = "";
		for( int i = 0; i < words_list.size(); i++ ) {
			
			long start = words_list.get(i).getStartPosition();
			long end   = words_list.get(i).getEndPosition();
			
			if( start == -1 || end == -1 ) {
				// We haven's set the timing yet, so just throw an error message.
				JOptionPane.showMessageDialog(frame, "Cannot save.\n\nYou have not made all the timings.", "Incorrect Timings", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			if( i == 0 ) {
				line +=  start + "\t" + end;
			} else {
				line +=  "\t" + start + "\t" + end;
			}
		}
		
		// Create the directories so we can save to the timings file.
		File make_directory = new File( timings_file.getParent() );
		make_directory.mkdirs();

		// Write to the timings file.
		try ( BufferedWriter bw = new BufferedWriter( new FileWriter( timings_file ) ) ) {
			bw.write( line );
			JOptionPane.showMessageDialog(frame, "Timings have been saved to the file\n" + timings_file.toString(), "Saved Timings", JOptionPane.PLAIN_MESSAGE);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Update the flashcard view of our card, as we now have a timings file.
		main_app.displayCard( main_app.getCurrentCard(), true );
	}
	
	public void loadTimingsArrayFromFile( File timings_file ) {
		// Read in the timings line.
		String timings_line = "";
		try( BufferedReader br = new BufferedReader( new FileReader( timings_file ) ) ) {
			String line = "";
			while( (line = br.readLine()) != null ) {
				timings_line = line;
			}

			// Split it and parse it into long ints.
			String[] str_timings = timings_line.split( "\t" );
		
			for( int i = 0; i < words_list.size(); i++ ) {
				words_list.get(i).start_position_in_millis = Long.parseLong( str_timings[(i * 2)] );
				words_list.get(i).end_position_in_millis   = Long.parseLong( str_timings[(i * 2)+1] );
			}
		} catch (FileNotFoundException e) {
			// Do nothing.
		} catch (IOException e) {
			// Do nothing.
		}
	}

	class SaveTimingsActionListener implements ActionListener {
		public void actionPerformed( ActionEvent event ) {
			saveTimingsArrayToFile( timings_file );
		}
		
	}
	class PlayStopAudioListener implements ActionListener {
		public void actionPerformed( ActionEvent event ) {
			// Switch between playing and not playing.
			if( is_audio_playing ) {
				button_play_stop.stop();
			} else {
				button_play_stop.play();
			}
		}
		
	}
	
	class MyTimingJLabel extends JLabel {
		private long start_position_in_millis = -1;
		private long end_position_in_millis   = -1;
		private boolean is_colour_locked = false; // Used to keep the label green when we are clicking on it.
		private boolean is_colour_red = false;
		
		private static final String DEFAULT_FONT_FAMILY = "SansSerif";
		private static final int DEFAULT_FONT_SIZE = 48;
		private static final int DEFAULT_FONT_SIZE_SMALL = 14;
		//private static final List<String> FONT_LIST = Arrays.asList(new String [] {"Arial", "Calibri", "Cambria", "Courier New", "Comic Sans MS", "Dialog", "Georgia", "Helevetica", "Lucida Sans", "Monospaced", "Tahoma", "Times New Roman", "Verdana"});
		private final String [] FONT_SIZES  = {"Font Size", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "36", "48", "72"};
	
		String font_family;
		Font font = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE);
		MyTimingJLabel( String word ) {
			// Added the space for padding as we need a space between words.
			super( word + " " );
			
			this.setFont( font );
		}
		
		public void colourLabel( long position_in_microseconds ) {
			if( is_colour_locked == false) {
				
				if( position_in_microseconds >= getStartPosition() && position_in_microseconds <= getEndPosition() ) {
					if( is_colour_red == false ) {
						is_colour_red = true;
						setForeground( Color.red );
						System.out.println( getStartPosition() );
					}
				} else {
					if( is_colour_red ) {
						is_colour_red = false;
						setForeground( Color.black );
						System.out.println( getEndPosition() );
					}
				}
			}
		}
		
		public long getStartPosition() {
			return this.start_position_in_millis;
		}
		public long getEndPosition() {
			return this.end_position_in_millis;
		}
		public void setStartPosition() {
			if( is_audio_playing ) {
				this.start_position_in_millis = System.currentTimeMillis() - audio_start_time_in_millis;
			}
		}
		public void setEndPosition() {
			if( is_audio_playing ) {
				this.end_position_in_millis = System.currentTimeMillis() - audio_start_time_in_millis;
			}
		}
	
		public boolean getIsColourLocked() {
			return this.is_colour_locked;
		}
	
		public void setIsColourLocked( boolean is_locked ) {
			this.is_colour_locked = is_locked;
		}
	}
	
	class LabelClicked implements MouseListener {
		MyTimingJLabel lbl;
		LabelClicked( MyTimingJLabel lbl ) {
			this.lbl = lbl;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			lbl.setIsColourLocked( true );
			lbl.setStartPosition();
			lbl.setForeground( Color.green );
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			lbl.setIsColourLocked( false );
			lbl.setEndPosition();
			lbl.setForeground( Color.black );
			
		}

		@Override
		public void mouseClicked(MouseEvent e) { }

		@Override
		public void mouseEntered(MouseEvent e) { }

		@Override
		public void mouseExited(MouseEvent e) { }
	}
	
	class MyAudioButton extends JButton {
		String text;
		
		ThreadHighlightWord thread_highlight_words;
		
		MyAudioButton( String text ) {
			this.text = text;
			setText( "Play - " + text );
		}
		MyAudioButton() {
			this( "Audio" );
		}

		public void play() {
			is_audio_playing = true;
			audio_start_time_in_millis = System.currentTimeMillis();
			audio_player.play();
			this.setText( "Stop - " + text );
			ThreadAudioButtonResetFontColour reset_font_colour = new ThreadAudioButtonResetFontColour();
			this.thread_highlight_words = new ThreadHighlightWord();
			reset_font_colour.start();
			thread_highlight_words.start();
		}
		
		public void stop() {
			is_audio_playing = false;
			audio_player.clip.setFramePosition( 0 );
			audio_player.stop();
			thread_highlight_words.keep_going = false;
			this.setText( "Play - " + text );
			for( int i = 0; i < words_list.size(); i++ ) {
				words_list.get( i ).setForeground( Color.black );
			}
		}
	}
	
	/**
	 * Resets the font colour to black when the audio has reached the end.
	 */
	class ThreadAudioButtonResetFontColour extends Thread {
		public void run() {
			while( true ) {
				// If it's at the end of the audio clip, or it's at the beginning of the clip.
				if( ( audio_player.clip.getLongFramePosition() == audio_player.clip.getFrameLength() ) || ( audio_player.clip.getLongFramePosition() == 0 ) ) {
					button_play_stop.stop();
					for( int i = 0; i < words_list.size(); i++ ) {
						words_list.get(i).setForeground( Color.black );
					}
					return;
				}

				try {
					// Sleep so this loop doesn't run too many times.
					sleep( 100 );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Resets the font colour to black when the audio has reached the end.
	 */
	class ThreadHighlightWord extends Thread {
		public boolean keep_going = true;
		public void run() {
			keep_going = true;
			while( keep_going ) {
				for( int i = 0; i < words_list.size(); i++ ) {
					long pos = System.currentTimeMillis() - audio_start_time_in_millis;
					words_list.get(i).colourLabel( pos );
				}
			}
		}
	}
}
