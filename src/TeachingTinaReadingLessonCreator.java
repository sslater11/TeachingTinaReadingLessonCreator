/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/


import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;

import libteachingtinadbmanager.*;
import libteachingtinadbmanager.sqlite_db.SQLiteReadingLessonHandler;

import javax.swing.event.*;
import javax.swing.text.DefaultEditorKit.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeachingTinaReadingLessonCreator {
	private JFrame frame;
	private JTextPane text_editor;
	private JComboBox<String> font_size_combo_box;
	
	private ReadingLessonCreator previous_lesson = null;

	private static final String MAIN_TITLE = "Tina's Reading Lesson Creator";
	private static final String DEFAULT_FONT_FAMILY = "SansSerif";
	private static final int DEFAULT_FONT_SIZE = 30;
	private static final int DEFAULT_FONT_SIZE_SMALL = 14;
	private static final int DEFAULT_FONT_SIZE_LARGE = 42;
	//private static final List<String> FONT_LIST = Arrays.asList(new String [] {"Arial", "Calibri", "Cambria", "Courier New", "Comic Sans MS", "Dialog", "Georgia", "Helevetica", "Lucida Sans", "Monospaced", "Tahoma", "Times New Roman", "Verdana"});
	private static final String [] FONT_SIZES  = {"Font Size", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "36", "48", "72"};

	String font_family;
	public static Font small_font = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE_SMALL);
	public static Font large_font = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE_LARGE);
	public static Font medium_font = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE);
	JLabel lbl_reading_level;
	JList previous_lessons_words_list;
	JList most_common_words_list;
	JScrollPane common_words_scroll;
	
	static Connection db_connection;
	static String sqlite_table_name = "reading_lessons";

	public static void main(String[] args) {
		// Load the database
		db_connection = null;
		try {
			db_connection = DriverManager.getConnection("jdbc:sqlite:ReadingLessons.db");
			// Create table
			String create_table = 
				"CREATE TABLE IF NOT EXISTS " + sqlite_table_name + "( " +
				"card_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"date_in_millis integer, " +
				"box_num integer, " +
				"reading_lesson_level integer, " +
				"sound_type text, " +
				"sound_word_or_sentence text, " +
				"id_of_linked_card integer, " +
				"is_spelling_mode integer, " +

				"card_text text, " +
				"card_images text, " +
				"card_audio text, " +
				"card_read_along_timings text );";

			Statement stat = db_connection.createStatement();
			stat.execute( create_table );

			// Load the main window and get into our app.
			TeachingTinaReadingLessonCreator app = new TeachingTinaReadingLessonCreator();
			
			// Loop until we close the main jframe on my app.
			// Without this, the SQLite database connection is closed,
			// so stick to a loop to make sure that it all exit properly.
			while( app.frame.isShowing() ) {
				try {
					Thread.sleep( 1000 );	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("after app loop");

		} catch( SQLException e ) {
			e.printStackTrace();
		}
		finally {
			if( db_connection != null ) {
				try {
					db_connection.close();
					System.out.println("Closed database connection.");

				} catch ( SQLException e ) {
					e.printStackTrace();
				}
			}
		}
	}
	
	TeachingTinaReadingLessonCreator() {
		loadLessons();
		
		// Create the frame and pane.
		frame = new JFrame(MAIN_TITLE);
		text_editor = new JTextPane();

		JScrollPane text_editor_scroll_pane = new JScrollPane(text_editor);

		
		// Create the labels
		JLabel lbl_hint1 = new JLabel("<html><div style='text-align: center; color: #ff0000;'>" + "<b>Red</b> = A new word." + "</div></html>" );
		lbl_hint1.setFont( small_font );

		JLabel lbl_hint2 = new JLabel("<html><div style='text-align: center;'>" + "<b>Black</b> = A word in previous lesson file." + "</div></html>" );
		lbl_hint2.setFont( small_font );
		
		JLabel lbl_hint3 = new JLabel("<html><div style='text-align: center;'>" + "All tabs will be changed to 4 spaces." + "</div></html>" );
		lbl_hint3.setFont( small_font );

		this.lbl_reading_level = new JLabel( "Reading Level is at: " );
		this.lbl_reading_level.setFont( small_font );

		JLabel lbl_previous_lessons_words_title = new JLabel( "<html><div style='text-align: center;'>" + "<b>Previous Lessons<br>Words</b>" + "</div></html>" );
		JLabel lbl_common_words_title = new JLabel("<html><div style='text-align: center;'>" + "<b>Most Common<br>Words<br>That Tina<br>Doesn't Know Yet</b>"  + "</div></html>");

		lbl_previous_lessons_words_title.setFont( small_font );
		lbl_common_words_title.setFont( small_font );


		// Create the most common words list.
		most_common_words_list = new JList( );
		most_common_words_list.setFont( small_font );
		most_common_words_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		most_common_words_list.setLayoutOrientation(JList.VERTICAL);
		most_common_words_list.setVisibleRowCount(-1);
		
		
		// This list loads the previous lesson's words
		this.previous_lessons_words_list = new JList<String>( getAllPreviousLessonWords() );
		previous_lessons_words_list.setFont( small_font );
		previous_lessons_words_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		previous_lessons_words_list.setLayoutOrientation(JList.VERTICAL);
		previous_lessons_words_list.setVisibleRowCount(-1);
		

		// Create the buttons
		FocusOnTextActionListener focus_on_text_action_listener          = new FocusOnTextActionListener();
		CreateLessonActionListener create_lesson_action_listener         = new CreateLessonActionListener();
		PreviewLessonActionListener preview_lesson_action_listener       = new PreviewLessonActionListener();
		FlashcardManagerActionListener flashcard_manager_action_listener = new FlashcardManagerActionListener( db_connection );

		JButton copyButton = new JButton( new CopyAction() );
		copyButton.setText( "Copy" );
		copyButton.addActionListener( focus_on_text_action_listener );
		copyButton.setFont( small_font );

		JButton cutButton = new JButton( new CutAction() );
		cutButton.setText( "Cut" );
		cutButton.addActionListener( focus_on_text_action_listener );
		cutButton.setFont( small_font );

		JButton pasteButton = new JButton( new PasteAction() );
		pasteButton.setText( "Paste" );
		pasteButton.addActionListener( focus_on_text_action_listener );
		pasteButton.setFont( small_font );
		
		JButton preview_lesson = new JButton( "Preview Lesson" );
		preview_lesson.addActionListener( preview_lesson_action_listener );
		preview_lesson.setFont( small_font );

		JButton create_lesson = new JButton( "Create Lesson" );
		create_lesson.addActionListener( create_lesson_action_listener );
		create_lesson.setFont( small_font );

		JButton flashcard_manager_button = new JButton( "Flashcard Designer" );
		flashcard_manager_button.addActionListener( flashcard_manager_action_listener );
		flashcard_manager_button.setFont( large_font );

		// Setup the toolbar panel.
		font_size_combo_box = new JComboBox<String>(FONT_SIZES);
		font_size_combo_box.setEditable(false);
		font_size_combo_box.addItemListener(new FontSizeItemListener());
		font_size_combo_box.setFont( small_font );

		JPanel controls_panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		controls_panel.add( copyButton );
		controls_panel.add( cutButton );
		controls_panel.add( pasteButton );
		controls_panel.add( new JSeparator(SwingConstants.VERTICAL) );
		controls_panel.add( new JSeparator(SwingConstants.VERTICAL) );
		controls_panel.add( font_size_combo_box );
		controls_panel.add( new JSeparator(SwingConstants.VERTICAL) );
		controls_panel.add( create_lesson );
		controls_panel.add( preview_lesson );
		controls_panel.add( flashcard_manager_button );

		JPanel hint_1_panel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		JPanel hint_2_panel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		JPanel hint_3_panel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		hint_1_panel.add( lbl_hint1, BorderLayout.CENTER );
		hint_2_panel.add( lbl_hint2, BorderLayout.CENTER );
		hint_3_panel.add( lbl_hint3, BorderLayout.CENTER );

		JPanel toolbar_panel = new JPanel();
		toolbar_panel.setLayout( new BoxLayout(toolbar_panel, BoxLayout.PAGE_AXIS) );
		toolbar_panel.add( controls_panel );
		toolbar_panel.add( hint_1_panel );
		toolbar_panel.add( hint_2_panel );
		toolbar_panel.add( hint_3_panel );
		
		
		// Setup the most common words panel.
		JPanel common_words_panel = new JPanel();
		this.common_words_scroll = new JScrollPane( most_common_words_list );
		common_words_panel.setLayout(new BoxLayout( common_words_panel, BoxLayout.Y_AXIS) );
		common_words_panel.add( lbl_common_words_title );
		common_words_panel.add( common_words_scroll );

		
		// Setup the previous lessons words panel.
		JPanel previous_lessons_words_panel = new JPanel();
		previous_lessons_words_panel.setLayout( new BoxLayout(previous_lessons_words_panel, BoxLayout.Y_AXIS) );
		previous_lessons_words_panel.add( lbl_previous_lessons_words_title );
		previous_lessons_words_panel.add( new JScrollPane(previous_lessons_words_list) );


		// Setup the status bar panel
		JPanel status_bar_panel = new JPanel();
		status_bar_panel.setLayout( new FlowLayout(FlowLayout.LEFT) );
		status_bar_panel.add( lbl_reading_level );


		// Add all panels to the frame.
		frame.add( toolbar_panel, BorderLayout.NORTH );
		frame.add( text_editor_scroll_pane, BorderLayout.CENTER );
		frame.add( common_words_panel, BorderLayout.WEST );
		frame.add( previous_lessons_words_panel, BorderLayout.EAST );
		frame.add( status_bar_panel, BorderLayout.SOUTH );

		
		// Setup the text editor for checking for new words.
		// Initializes it with a default string.
		text_editor.setDocument( getNewDocument() );
		text_editor.setFont( new Font( DEFAULT_FONT_FAMILY,Font.PLAIN, DEFAULT_FONT_SIZE) );

		
		// Display the frame and update it.
		frame.setSize(600, 500);
		frame.setLocationRelativeTo( null );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
		frame.setExtendedState( JFrame.MAXIMIZED_BOTH );
		
		updateComponents();

		text_editor.requestFocusInWindow();
		
	}
	
	public static boolean isWordInList(String str, DefaultListModel<String> word_list ) {
		// For some reason their code keeps a character at the start of the word.
		// However, it does NOT keep a character at the start when it is the first word written in the document.
		// So remove the first character only if it is not a valid letter.
		if( str.length() > 1
		  && ! isAValidLetter(str.charAt(0)) )
		{
			str = str.substring( 1 );
		}

		for( int i = 0; i < word_list.size(); i++ ) {
			if( str.compareToIgnoreCase(word_list.get(i)) == 0 ) {
				return true;
			}
		}

		return false;
	}

	public static boolean isAValidLetter(char ch) {
		return isAValidLetter( ch + "" );
	}

	public static boolean isAValidLetter(String str) {
		String valid_letters = "abcdefghijklmnopqrstuvwxyz\'-";

		for( int i = 0; i < valid_letters.length(); i++ ) {
			String str_valid_letter = "" + valid_letters.charAt(i);
			if( str.compareToIgnoreCase(str_valid_letter) == 0 ) {
				return true;
			}
		}

		return false;
	}
	private void updateCurrentReadingLevel() {
		this.lbl_reading_level.setText(("<html><b>Current reading level of known words is: <b><font style=\"color:red;\">" + getCurrentReadingLevel() + "</font></b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Max reading level is: <b><font style=\"color:red;\">" + getMaxReadingLevel() + "</font></b></html>") );
	}
	
	public ReadingLessonCreator getPreviousLesson() {
		return this.previous_lesson;
	}

	/**
	 * Get all the previous words from the lessons, to use as a list to show the user.
	 * @return
	 */
	public DefaultListModel<String> getAllPreviousLessonWords() {
		DefaultListModel<String> all_words = new DefaultListModel<String>();
		
		if( this.previous_lesson != null &&
		    this.previous_lesson.getAllLessonsWords() != null &&
		    this.previous_lesson.getAllLessonsWords().size() > 0 )
		{
			// This line doesn't work on Java 1.7 and below.
			// For some reason the current windows java installer is old, so this breaks.
			//all_words.addAll( this.previous_lesson.getAllLessonsWords() );
			
			// Instead, just create it myself, foolproof.
			List<String> words = previous_lesson.getAllLessonsWords();
			for( int i = 0; i < words.size(); i++ ) {
				all_words.addElement( words.get(i) );
			}
		}
		return all_words;
	}
	
	public static ArrayList<String> getWordsListFromText( String text ) {
		ArrayList<String> words = new ArrayList<String>();
		
		String word = "";
		for( int i = 0; i < text.length(); i++ ) {
			if( isAValidLetter(text.charAt(i)) ) {
				word = word + text.charAt(i);
			} else {
				// we've reached a non-valid letter, so we must have reached the end of a word.
				if( word.length() > 0 ) {
					words.add( word );
					word = "";
				}				}
		}
		// Add the last word :).
		if( word.length() > 0 ) {
			words.add(word);
			word = "";
		}
		
		return words;
	}
	
	public ArrayList<String> getWordsListFromEditor() {
		String text = getTextFromEditor();
		return getWordsListFromText( text );
	}
	
	public String getAllWordsFromEditor() {
		ArrayList<String> words = getWordsListFromEditor();
		
		String result = " ";
		for( int i = 0; i < words.size(); i++ ) {
			result = result + words.get( i ) + " ";
		}
		
		return result;
	}
	
	private StyledDocument getNewDocument() {
			/**
			 * This will automatically change the colour for the words as we type in the text editor.
			 */
	
			DefaultStyledDocument doc = new DefaultStyledDocument() {
				public void insertString (int offset, String str, AttributeSet a) throws BadLocationException {
					super.insertString(offset, str, a);
					updateComponents();
				}
	
				public void remove (int offsset, int length) throws BadLocationException {
					super.remove(offsset, length);
					updateComponents();
				}
			};
	
			Style style = text_editor.addStyle("My Highlighting Style", null);
	
			try {
				doc.insertString(
					doc.getLength(),
					"This will highlight all words that aren't in a lesson file as red." +
					"\nSo red = she has not learnt that word." +
					"\nblack = she knows that word.",
					style
				);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return doc;
		}

	private StyledDocument getEditorDocument() {
		StyledDocument doc = (DefaultStyledDocument) text_editor.getDocument();
		return doc;
	}

	private String getTextFromEditor() {
		StyledDocument doc = getEditorDocument();
		//Store the whole sentence
		String text = "";
		try {
			text = doc.getText( 0, doc.getLength() );
			text = text.replaceAll("\t", "    ");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return text;
	}

	/**
	 * Will return the reading level that the current text is at, ignoring new words.
	 */
	private int getCurrentReadingLevel() {
		int max_level = getMaxReadingLevel();
		if( this.previous_lesson == null ) {
			return 0;
		} else {
			// Scan through all the words and their reading levels and see if we have any matches.
			List<String>  all_words  = previous_lesson.getAllLessonsWords();
			List<Integer> all_levels = previous_lesson.getAllLessonsWordsReadingLevels();
			int current_level = 0;

			List<String> list = getWordsListFromEditor();
			for( String word : list ) {
				for( int k = 0; k < all_words.size(); k++ ) {
					if( word.compareToIgnoreCase( all_words.get( k ) ) == 0 ) {
						if( all_levels.get( k ) > current_level ) {
							current_level = all_levels.get( k );
							
							// No point in looping anymore as this is the maximum value we can reach,
							// so saves a bit of cpu power.
							if( current_level == max_level ) {
								return current_level;
							}
						}
					}
				}
			}
			
			return current_level;
			//return this.previous_lesson.getCurrentReadingLevel( getWordsListFromEditor() );
		}
	}
	private int getMaxReadingLevel() {
		if( this.previous_lesson == null ) {
			return 0;
		} else {
			return this.previous_lesson.getLevel();
		}
	}

	private void loadLessons() {
		DeckSettings deck_settings = null;
		try {
			ReadingLessonCreator lesson = TextEditorDBManager.readSQLiteDB( db_connection, sqlite_table_name, deck_settings );
			previous_lesson = lesson;
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	public static void printAList(ArrayList<String> list) {
		for( int i = 0; i < list.size(); i++ ) {
			System.out.println( list.get(i) );
		}
	}

	private void updateComponents() {
		// Update the reading level dispayed.
		updateCurrentReadingLevel();

		// Update the list of previous lessons words
		WordList prev_lesson_words = new WordList( getAllPreviousLessonWords() );
		
		this.previous_lessons_words_list.setListData( prev_lesson_words.getNumberedListReversed().toArray() );
		
		// Update the list of most common words
		MostCommonWords mcw = new MostCommonWords();
		// remove our current typed text and all previous lessons words.
		mcw.removeWords( getAllPreviousLessonWords() );
		mcw.removeWords( getWordsListFromEditor() );
		
		this.most_common_words_list.setListData( mcw.getMostCommonWordsAsNumberedList().toArray() );
		
		// These will change the colours of the text in the editor
		final StyleContext style_context = StyleContext.getDefaultStyleContext();
		final AttributeSet attrRed   = style_context.addAttribute( style_context.getEmptySet(), StyleConstants.Foreground, Color.RED   );
		final AttributeSet attrBlack = style_context.addAttribute( style_context.getEmptySet(), StyleConstants.Foreground, Color.BLACK );

		// Colour the new words.
		StyledDocument doc = getEditorDocument();
		try {
			String text = doc.getText(0, doc.getLength());
			ArrayList<WordWithIndexes> all_words = SentenceAnalyzer.getWordsListWithIndexes( text );
			
			// Loop through all words typed and colour them red for new words and black for known words.
			for( int i = 0; i < all_words.size(); i++ ) {
				int word_length = all_words.get(i).getEndingIndex() - all_words.get(i).getStartingIndex();
				if( ! isWordInList(all_words.get(i).getWord(), getAllPreviousLessonWords()) ) {
					doc.setCharacterAttributes(all_words.get(i).getStartingIndex(), word_length, attrRed, false);
				} else {
					doc.setCharacterAttributes(all_words.get(i).getStartingIndex(), word_length, attrBlack, false);
				}
			}
		} catch( Exception e ) {
			System.out.println("couldn't change the colours for some reason");
		}
	}
	

	private class FocusOnTextActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			text_editor.requestFocusInWindow();
		}
	}
	
	private class PreviewLessonActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			//ReadingLessonCreator deck = new ReadingLessonCreator( getPreviousLesson(), getTextFromEditor() );

			//if( deck.hasNewWords() ) {
			//	String all_lines = "";
			//	for( String i : TextEditorDBManager.getDatabaseOutput( deck ) ) {
			//		all_lines += i + "\n";
			//	}
			//	new PreviewLessonJFrame( all_lines );
			//}
		}
	}

	private boolean isLetterUpperCase( char ch ) {
		if( ch >= 'A' && ch <= 'Z') {
			return true;
		} else {
			return false;
		}
	}
	private class CreateLessonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			ReadingLessonCreator deck = new ReadingLessonCreator( getPreviousLesson(), getTextFromEditor() );
			List <String> words_list = deck.getWords();

			for( int i = 0; i < words_list.size(); i++ ) {
				String word = words_list.get(i);
				
				// If the word starts with a capital, then ask the user if we are keeping the capital letter.
				
				if( isLetterUpperCase( word.charAt(0) ) ) {
					// Ask user if we want to keep the first letter as a capital letter.
					
					String message = "Is this word a proper noun?\n" +
					                 "Word: " + word + "\n\n" +
					                 "Do you want to keep the first letter capitalized?\n\n" +
					                 "If you capitalize the first letter, the flashcard it creates will be of this word with a capital letter.\n" +
					                 "This only makes sense if the word is a proper noun and is always written with a capital.";
					int option_pane = JOptionPane.showConfirmDialog(frame, message, "Is word a proper noun?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					
					if( option_pane == 0 ) {
						// Yes was clicked.
						
						// Keep the capital at the start.
						// Lower case everything else.
						word = word.charAt(0) + word.substring(1).toLowerCase();
					} else {
						// No was clicked.
						// Lower case the whole word.
						word = word.toLowerCase();
					}
				} else {
					word = word.toLowerCase();
				}

				words_list.set( i, word );
			}
			
			if( deck.hasNewWords() ) {
				// Write the SQLite database.
				SQLiteReadingLessonHandler.writeToSQLiteDB(db_connection, sqlite_table_name, deck);

				loadLessons();
				
				updateComponents();
			}
		}
	}

	private class FontSizeItemListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			int font_size = 30;

			try {
				font_size = Integer.parseInt((String) e.getItem());
			}
			catch (NumberFormatException ex) {
				// Do nothing.
			}
			text_editor.setFont( new Font( DEFAULT_FONT_FAMILY, Font.PLAIN, font_size) );
			//text_editor.requestFocusInWindow();
		}
	}

	private class FlashcardManagerActionListener implements ActionListener {
		Connection db_connection;
		
		FlashcardManagerActionListener( Connection db_connection ) {
			this.db_connection = db_connection;
		}

		public void actionPerformed(ActionEvent e) {
			new MyFlashcardManager( this.db_connection );
		}
	}
}

/**
 * Stores a Card object along with it's database file and line number.
 * It allows us to check if the card is missing any media.
 */
//class IncompleteReadingCard {
/**
 * Load a card into memory and allow us to add media to it and save.
 */
class ReadingCardEditor {
	int card_id;
	long date_in_millis;
	int box_num;
	int reading_lesson_level;
	String sound_type;
	String sound_word_or_sentence;
	int id_of_linked_card;
	boolean is_spelling_mode;

	String card_text;
	String card_images;
	String card_audio;
	String card_read_along_timings;

	// Populate these with the cards content for easy displaying.
	ArrayList<String> text_list;
	ArrayList<String> image_list;
	ArrayList<String> audio_list;

	/*
	 * These lists will either be the same size as the lists stored in the card, or they'll be larger for adding new content.
	 */
	public String updated_read_along_timings;
	public ArrayList<String> updated_text_list;
	public ArrayList<String> updated_audio_list;
	public ArrayList<String> updated_image_list;

	ReadingCardEditor ( 
		int card_id,
		long date_in_millis,
		int box_num,
		int reading_lesson_level,
		String sound_type,
		String sound_word_or_sentence,
		int id_of_linked_card,
		boolean is_spelling_mode,

		String card_text,
		String card_images,
		String card_audio,
		String card_read_along_timings
	) {
		initAndReset(
			card_id,
			date_in_millis,
			box_num,
			reading_lesson_level,
			sound_type,
			sound_word_or_sentence,
			id_of_linked_card,
			is_spelling_mode,

			card_text,
			card_images,
			card_audio,
			card_read_along_timings
		);
	}

	/**
	 * Used to initialize or reset our card whenever we update it.
	 */
	public void initAndReset (
		int card_id,
		long date_in_millis,
		int box_num,
		int reading_lesson_level,
		String sound_type,
		String sound_word_or_sentence,
		int id_of_linked_card,
		boolean is_spelling_mode,

		String card_text,
		String card_images,
		String card_audio,
		String card_read_along_timings
	)
	{
		this.card_id                 = card_id;
		this.date_in_millis          = date_in_millis;
		this.box_num                 = box_num;
		this.reading_lesson_level    = reading_lesson_level;
		this.sound_type              = sound_type;
		this.sound_word_or_sentence  = sound_word_or_sentence;
		this.id_of_linked_card       = id_of_linked_card;
		this.is_spelling_mode        = is_spelling_mode;
		this.card_text               = card_text;
		this.card_images             = card_images;
		this.card_audio              = card_audio;
		this.card_read_along_timings = card_read_along_timings;

		this.text_list = CardDBTagManager.makeStringAList( card_text );

		image_list = convertImageListTagsToFilePaths( CardDBTagManager.makeStringAList( card_images ) );
		audio_list = convertAudioListTagsToFilePaths( CardDBTagManager.makeStringAList( card_audio ) );

		resetAllUpdatedFields();
	}
	
	public static boolean isStringAWord( String str ) {
		if( str.compareToIgnoreCase( TextEditorDBManager.WORD ) == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isStringASentence( String str ) {
		if( str.compareToIgnoreCase( TextEditorDBManager.SENTENCE ) == 0 ) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isStringASound( String str ) {
		if( str.compareToIgnoreCase( TextEditorDBManager.SOUND ) == 0 ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isCardAWord() {
		return isStringAWord( this.sound_word_or_sentence );
	}
	
	public boolean isCardASentence() {
		return isStringASentence( this.sound_word_or_sentence );
	}

	public boolean isCardASound() {
		return isStringASound( this.sound_word_or_sentence );
	}

	public void saveToDatabase( Connection db_connection ) {
		// Get all the media tags
		String card_images = "";
		String card_audio = "";

		for (int i = 0; i < updated_audio_list.size(); i++) {
			String path = MyFlashcardManager.getMediaUpdatedFilePath( audio_list, updated_audio_list, i );
			path = path.replaceAll(TextEditorDBManager.getDirectory(), "");
			card_audio += "<audio:\"" + path + "\">";
		}

		for (int i = 0; i < updated_image_list.size(); i++) {
			String path = MyFlashcardManager.getMediaUpdatedFilePath( image_list, updated_image_list, i );
			path = path.replaceAll(TextEditorDBManager.getDirectory(), "");
			card_images += "<image:\"" + path + "\">";
		}

		// Save the media tags to the database
		SQLiteReadingLessonHandler.updateCardMedia( db_connection, card_id, card_images, card_audio, this.card_read_along_timings );
		// Update and reset our stored card, just for consistency.
		initAndReset(
			this.card_id,
			this.date_in_millis,
			this.box_num,
			this.reading_lesson_level,
			this.sound_type,
			this.sound_word_or_sentence,
			this.id_of_linked_card,
			this.is_spelling_mode,

			this.card_text,
			card_images,
			card_audio,
			card_read_along_timings
		);
	}

	/**
	 * Will make the updated lists the same length as their original list and will set their elements to null.
		updated_text_list
		updated_audio_list
		updated_image_list
		
		Sets this string to null aswell
		updated_read_along_timings
	 */
	public void resetAllUpdatedFields() {
		updated_text_list  = new ArrayList<String>();
		updated_audio_list = new ArrayList<String>();
		updated_image_list = new ArrayList<String>();
		updated_read_along_timings = null;

		// Populate the updated lists with null values to show that the current card has no new updated fields.
		for( int i = 0; i < text_list.size(); i++ ) {
			updated_text_list.add( null );
		}
		for( int i = 0; i < audio_list.size(); i++ ) {
			updated_audio_list.add( null );
		}
		for( int i = 0; i < image_list.size(); i++ ) {
			updated_image_list.add( null );
		}
	}

	public boolean isCardComplete() {
		if( ReadingCardEditor.isStringASentence( this.sound_word_or_sentence) ) {
			// Check if our card has audio tags.
			if( ! CardDBTagManager.hasAudioTag( this.card_audio ) ) {
				// No Audio tags.
				return false;
			}

			// Check if our card has images tags.
			else if( ! CardDBTagManager.hasImageTag( this.card_images ) ) {
				// No Image tags.
				return false;
			}

			// Check if our card has read_along timings tags.
			else if( this.card_read_along_timings == null  ) {
				// No Read Along Timings tags.
				return false;
			}

			// Check if all of the tags have their files.
			ArrayList<String> image_list = getImageFilePaths();
			ArrayList<String> audio_list = getAudioFilePaths();

			if( doAllFilesExistInList(              image_list ) &&
			    doAllFilesExistInList(              audio_list ) )
			{
				return true;
			} else {
				return false;
			}
		}

		else if( ReadingCardEditor.isStringAWord( this.sound_word_or_sentence) ) {
			// Check if our card has audio tags.
			if ( ! CardDBTagManager.hasAudioTag( this.card_audio ) ) {
				// No Audio tags.
				return false;
			}

			// Check if our card has images tags.
			else if ( ! CardDBTagManager.hasImageTag( this.card_images ) ) {
				// No Image tags.
				return false;
			}

			// Check if all of the tags have their files.
			ArrayList<String> image_list = getImageFilePaths();
			ArrayList<String> audio_list = getAudioFilePaths();

			if ( doAllFilesExistInList( image_list ) &&
			     doAllFilesExistInList( audio_list ) ) {
				return true;
			} else {
				return false;
			}
		}

		else if( ReadingCardEditor.isStringASound( this.sound_word_or_sentence) ) {
			// Check if our card has audio tags.
			if ( ! CardDBTagManager.hasAudioTag( this.card_audio ) ) {
				// No Audio tags.
				return false;
			}

			// Check if all of the tags have their files.
			ArrayList<String> audio_list = getAudioFilePaths();
			if ( doAllFilesExistInList( audio_list ) ) {
				return true;
			} else {
				return false;
			}
		}

		else {
			return false;
		}
	}

	/**
	 * Check if a file exists or not.
	 */
	public static boolean doesFileExist( String filename ) {
		return MyFlashcardManager.doesFileExist( filename );
	}

	/** Check if all of the files in a list of string filepaths exist. */
	public static boolean doAllFilesExistInList( ArrayList<String> all_filenames ) {
		return MyFlashcardManager.doAllFilesExistInList( all_filenames );
	}

	public void addAudio( String file_path ) {
		updated_audio_list.add( file_path );
	}
	public void addImage( String file_path ) {
		updated_image_list.add( file_path );
	}

	public void setReadAlongTimings( String timings ) {
		updated_read_along_timings = timings;
	}

	/*
	 * Make the updated card just an empty string.
	 * The empty string is a way of showing that we
	 * don't want that image when we save the updated card.
	 */
	public void removeAudio( int index ) {
		updated_audio_list.set( index, "" );
	}
	
	public String getText() {
		return this.card_text;
	}
	
	public String getReadAlongTimings() {
		if( this.updated_read_along_timings != null ) {
			return this.updated_read_along_timings;
		} else {
			return this.card_read_along_timings;
		}
	}

	public void setImage( int index, String file_path ) {
		if( index >= image_list.size() ) {
			updated_image_list.add( file_path );
		} else {
			updated_image_list.set( index, file_path );
		}
	}

	public void setAudio( int index, String file_path ) {
		if( index >= audio_list.size() ) {
			updated_audio_list.add( file_path );
		} else {
			updated_audio_list.set( index, file_path );
		}
	}

	//public void setText( int index, String text) {
	//	if( index >= text_list.size() ) {
	//		updated_text_list.add( text );
	//	} else {
	//		updated_text_list.set( index, text );
	//	}
	//}


	public ArrayList<String> convertImageListTagsToFilePaths( ArrayList<String> original_list ) {
		ArrayList<String> filepaths_list = new ArrayList<String>();

		for( int i = 0; i < original_list.size(); i++ ) {
			String filename = (String) CardDBTagManager.getImageFilename( original_list.get(i) );
			filepaths_list.add( TextEditorDBManager.getDirectory() + filename );

		}

		return filepaths_list;
	}

	public ArrayList<String> convertAudioListTagsToFilePaths( ArrayList<String> original_list ) {
		ArrayList<String> filepaths_list = new ArrayList<String>();

		for( int i = 0; i < original_list.size(); i++ ) {
			String filename = (String) CardDBTagManager.getAudioFilename( original_list.get(i) );
			filepaths_list.add( TextEditorDBManager.getDirectory() + filename );

		}

		return filepaths_list;
	}
	public ArrayList<String> convertReadAlongTimingsListTagsToFilePaths( ArrayList<String> original_list ) {
		ArrayList<String> filepaths_list = new ArrayList<String>();

		for( int i = 0; i < original_list.size(); i++ ) {
			String filename = (String) CardDBTagManager.getReadAlongTimingsFilename( original_list.get(i) );
			filepaths_list.add( TextEditorDBManager.getDirectory() + filename );

		}

		return filepaths_list;
	}

	/**
	 * Will compare 2 lists and will return a merged list where data from the updated list is preferred over the original list's elements..
	 * @param original_list
	 * @param updated_list
	 * @return
	 */
	private ArrayList<String> __getUpdatedMergedList( ArrayList<String> original_list, ArrayList<String> updated_list ) {
		ArrayList<String> list = new ArrayList<String>();
		// Scan through each audio in the list
		for( int i = 0; i < original_list.size(); i++ ) {
			if( updated_list.get( i ) == null ) {
				// Use original
				list.add( original_list.get(i) );
			} else {
				// Use updated version.
				String filename = updated_list.get(i);
				list.add( filename );
			}
		}

		// Scan through the remaining cards in the updated list.
		for( int i = original_list.size(); i < updated_list.size(); i++ ) {
			String filename = updated_list.get(i);
			list.add( filename );
		}

		return list;
	}

	public ArrayList<String> getImageFilePaths() {
		return __getUpdatedMergedList( this.image_list, this.updated_image_list );
	}
	public ArrayList<String> getAudioFilePaths() {
		return __getUpdatedMergedList( this.audio_list, this.updated_audio_list );
	}
}

/**
 * The window(JFrame) to edit and create flashcards.
 *
 */
class MyFlashcardManager {
	ArrayList<ReadingCardEditor> incomplete_cards;
	ArrayList<ReadingCardEditor> complete_cards;

	Connection db_connection;

	// Used to know which card we are displaying on the screen.
	ReadingCardEditor current_card;
	boolean is_current_card_in_the_completed_list;

	// Frame components
	JFrame frame;
	JPanel cards_content_sub_panel;
	JPanel cards_content_panel;
	JScrollPane cards_content_scroll_pane;
	JList complete_cards_list;
	JList incomplete_cards_list;
	JLabel lbl_complete_cards_title;
	JLabel lbl_incomplete_cards_title;

	int BORDER_THICKNESS = 20;

	// Colours
	String STR_LIGHT_RED   = "#FF9999";
	String STR_LIGHT_GREEN = "#CCFF99";
	String STR_DARK_RED    = "#990000";
	String STR_DARK_GREEN  = "#4C9900";
	Color DARK_GREEN  = new Color( 76, 153, 0 );
	Color DARK_RED    = new Color( 153, 0, 0 );
	Color LIGHT_RED   = new Color( 255, 153, 153 );
	Color LIGHT_GREEN = new Color( 204, 255, 153 );

	// Fonts
	Font small_font = TeachingTinaReadingLessonCreator.small_font;
	Font large_font = TeachingTinaReadingLessonCreator.large_font;
	Font medium_font = TeachingTinaReadingLessonCreator.medium_font;


	// Used so that our sentences get truncated when we show them in our JList.
	int MAXIMUM_JLIST_STRING_LENGTH = 30;

	/**
	 * Matches these file extensions at the end of a string
	 * .jpg .jpeg .png and .webm
	 */
	public static Pattern pattern_image_file_extension = Pattern.compile( "\\.(jpg|jpeg|png|webm)$", Pattern.CASE_INSENSITIVE );

	/**
	 * Matches these file extensions at the end of a string
	 * .mp3 .wav .m4a
	 */
	public static Pattern pattern_audio_file_extension = Pattern.compile( "\\.(mp3|wav|m4a)$", Pattern.CASE_INSENSITIVE );

	/**
	 * Matches these file extensions at the end of a string
	 * .timing
	 */
	public static Pattern pattern_read_along_timing_file_extension = Pattern.compile( "\\.timing$", Pattern.CASE_INSENSITIVE );

	class ReadAlongTimingsActionListener implements ActionListener {
		MyFlashcardManager main_app;
		String audio_file_path;
		String timings;
		public ReadAlongTimingsActionListener( MyFlashcardManager main_app, String audio_file_path, String timings ) {
			this.main_app = main_app;
			this.audio_file_path = audio_file_path;
			this.timings = timings;
		}
		
		public void actionPerformed( ActionEvent event ) {
			// Get the sentence from the card and turn it into a list of words that we can pass.
			getCurrentCard();
			ArrayList<String> words = TeachingTinaReadingLessonCreator.getWordsListFromText( getCurrentCard().getText() );
			
			File audio_file   = new File( audio_file_path );
			TeachingTinaReadAlongTimingCreator timing_creator = new TeachingTinaReadAlongTimingCreator( words, audio_file, timings, this.main_app );
		}
	}

	MyFlashcardManager( Connection db_connection ) {
		this.db_connection = db_connection;
		loadCards( this.db_connection );
		
		frame = new JFrame( "Incomplete Reading Cards" );
		
		// Setup the card's contents panel.
		this.cards_content_panel = new JPanel();
		this.cards_content_sub_panel = new JPanel();
		this.cards_content_sub_panel.setLayout( new BoxLayout(cards_content_sub_panel, BoxLayout.Y_AXIS) );
		this.cards_content_panel    .setLayout( new BoxLayout(cards_content_panel,     BoxLayout.Y_AXIS) );
		this.cards_content_panel.add( new JScrollPane( cards_content_sub_panel ) );

		// Cards lists.
		complete_cards_list = new JList( );
		complete_cards_list.setFont( small_font );
		complete_cards_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		complete_cards_list.setLayoutOrientation(JList.VERTICAL);
		complete_cards_list.setVisibleRowCount(-1);

		incomplete_cards_list = new JList( );
		incomplete_cards_list.setFont( small_font );
		incomplete_cards_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		incomplete_cards_list.setLayoutOrientation(JList.VERTICAL);
		incomplete_cards_list.setVisibleRowCount(-1);

		// Assign cards to the lists
		complete_cards_list  .setListData( makeStringArrayFromCards( complete_cards   ) );
		incomplete_cards_list.setListData( makeStringArrayFromCards( incomplete_cards ) );


		// Setup the headings for "Completed Cards" and "Incomplete Cards".
		// the &nbsp; is added for using spaces to add padding to the label.
		String spaces_padding = "&nbsp;&nbsp;&nbsp;&nbsp;";
		lbl_complete_cards_title   = new JLabel( "<html><div style='text-align: center;'>" + spaces_padding + "Completed"  + spaces_padding + "<br>Cards</div></html>", JLabel.CENTER );
		lbl_incomplete_cards_title = new JLabel( "<html><div style='text-align: center;'>" + spaces_padding + "Incomplete" + spaces_padding + "<br>Cards</div></html>", JLabel.CENTER );

		lbl_complete_cards_title  .setBackground( LIGHT_GREEN );
		lbl_incomplete_cards_title.setBackground( LIGHT_RED   );

		lbl_complete_cards_title  .setOpaque( true );
		lbl_incomplete_cards_title.setOpaque( true );
		
		lbl_complete_cards_title  .setAlignmentX( Component.CENTER_ALIGNMENT );
		lbl_incomplete_cards_title.setAlignmentX( Component.CENTER_ALIGNMENT );

		lbl_complete_cards_title  .setFont( medium_font );
		lbl_incomplete_cards_title.setFont( medium_font );
		
		Border border_complete   = BorderFactory.createLineBorder( DARK_GREEN, BORDER_THICKNESS );
		Border border_incomplete = BorderFactory.createLineBorder( DARK_RED,   BORDER_THICKNESS );
		lbl_complete_cards_title  .setBorder( border_complete   );
		lbl_incomplete_cards_title.setBorder( border_incomplete );

		// Setup the incomplete cards panel.
		JPanel incomplete_cards_panel = new JPanel();
		incomplete_cards_panel.setLayout( new BoxLayout(incomplete_cards_panel, BoxLayout.Y_AXIS) );
		incomplete_cards_panel.add( lbl_incomplete_cards_title );
		incomplete_cards_panel.add( new JScrollPane( incomplete_cards_list ) );

		// Setup the complete cards panel.
		JPanel complete_cards_panel = new JPanel();
		complete_cards_panel.setLayout( new BoxLayout(complete_cards_panel, BoxLayout.Y_AXIS) );
		complete_cards_panel.add( lbl_complete_cards_title );
		complete_cards_panel.add( new JScrollPane( complete_cards_list ) );


		// Add action listeners to the lists.
		
		// These will update the card preview whenever we click on an un-selected item in the list.
		// Also allows us to hold down the left click and drag up and down to quickly switch between cards.
		incomplete_cards_list.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = incomplete_cards_list.getSelectedIndex();
				if( index >= 0 ) {
					setIsCurrentCardInTheCompletedList( false );
					displayCard( incomplete_cards.get( index ), false );
				}
			}
		});
		
		complete_cards_list.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = complete_cards_list.getSelectedIndex();
				if( index >= 0 ) {
					setIsCurrentCardInTheCompletedList( true );
					displayCard( complete_cards.get( index ), false );
				}
			}
		});
		
		// These will update the card preview whenever we release a mouse click.
		// This allows us to load a card when it is already highlighted in the list.
		// It's value doesn't change, so the above listeners won't fire.
		//
		// We have the completed and incompleted list, so when switching between the two, a user might click the card that
		// is already highlighted in the list. This allows the card to be shown.
		// e.g. The card "poop" is incomplete. The card "cheese" is completed.
		// The user could now click on poop, then cheese, then poop, then cheese.
		incomplete_cards_list.addMouseListener( new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int index = incomplete_cards_list.getSelectedIndex();
				if( index >= 0 ) {
					setIsCurrentCardInTheCompletedList( false );
					displayCard( incomplete_cards.get( index ), false );
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) { }

			@Override
			public void mousePressed(MouseEvent e) { }

			@Override
			public void mouseEntered(MouseEvent e) { }

			@Override
			public void mouseExited(MouseEvent e) { }
		});

		complete_cards_list.addMouseListener( new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int index = complete_cards_list.getSelectedIndex();
				if( index >= 0 ) {
					setIsCurrentCardInTheCompletedList( true );
					displayCard( complete_cards.get( index ), false );
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) { }

			@Override
			public void mousePressed(MouseEvent e) { }

			@Override
			public void mouseEntered(MouseEvent e) { }

			@Override
			public void mouseExited(MouseEvent e) { }
		});


		// Add everything to the frame.
		frame.add( incomplete_cards_panel, BorderLayout.WEST   );
		frame.add( cards_content_panel,    BorderLayout.CENTER );
		frame.add( complete_cards_panel,   BorderLayout.EAST   );

		// select the first card and load it.
		if( incomplete_cards.size() > 0 ) {
			incomplete_cards_list.setSelectedIndex( 0 );
			setIsCurrentCardInTheCompletedList( false );
			displayCard( incomplete_cards.get(0), true );
		}


		// Add drag and drop for adding media to a card.
		TransferHandler handler = new TransferHandler() {
			/**
			 * Constantly called during the drag and drop to check if our component supports recieving
			 * a drag and drop of this type.
			 * We want drag and drops of the file type.
			 */
			public boolean canImport( TransferHandler.TransferSupport support ) {
				if ( support.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
					// It's a file that's being dragged over our frame.
					// So return true because we want to import this type of drag and drop.
					return true;
				} else {
					// It's not a file that's being dragged over our frame.
					return false;
				}
			}

			/**
			 * Receive the files from the drag and drop.
			 * @param support
			 * @return
			 */
			public boolean importData(TransferHandler.TransferSupport support) {
				if ( ! canImport( support ) ) {
					return false;
				}

				Transferable t = support.getTransferable();

				try {
					List<File> file_list = ( List<File> ) t.getTransferData( DataFlavor.javaFileListFlavor );

					updateCardWithMediaFiles( file_list );
				} catch (UnsupportedFlavorException e) {
					System.out.println( "Unsupported drag and drop item." );
					return false;
				} catch (IOException e) {
					return false;
				}

				return true;
			}
		};

		frame.setTransferHandler( handler );

		// Display the frame.
		frame.setSize(600, 500);
		frame.setLocationRelativeTo( null );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
		frame.setExtendedState( JFrame.MAXIMIZED_BOTH );
	}

	/**
	 * Check if a file exists or not.
	 */
	public static boolean doesFileExist( String filename ) {
		File media_file = new File( filename );
		if( media_file.exists() ) {
			return true;
		} else {
			return false;
		}
	}

	/** Check if all of the files in a list of string filepaths exist. */
	public static boolean doAllFilesExistInList( ArrayList<String> all_filenames ) {
		if( all_filenames.size() == 0 ) {
			return false;
		}
		else {
			for( int i = 0; i < all_filenames.size(); i++ ) {
				if( ! doesFileExist( all_filenames.get(i) ) ) {
					return false;
				}
			}
		}

		return true;
	}
	/**
	 * Check if the files dropped are valid media
	 * types and add them to our card.
	 * @param file_list
	 */
	public void updateCardWithMediaFiles( List<File> file_list ) {
		if( current_card == null ) {
			// We can't update it, because it's not been set yet.
			return;
		}

		for( File file : file_list ) {
			// Used full path just for testing purposes.
			String file_name = file.getAbsolutePath();
			
			Matcher match_image             = pattern_image_file_extension            .matcher( file_name );
			Matcher match_audio             = pattern_audio_file_extension            .matcher( file_name );
			Matcher match_read_along_timing = pattern_read_along_timing_file_extension.matcher( file_name );

			// If it's an audio file, add an audio tag.
			if( match_audio.find() ) {
				
				// Save the new audio
				if( (current_card.audio_list.size() == 1)
				 && (current_card.updated_audio_list.get(0) == null) ) {
					// Save the new audio as the one to replace the original.
					// Test if the audio file is not found.
					String audio_filename = current_card.audio_list.get( 0 );
					File audio_file = new File( audio_filename );

					if( audio_file.exists() ) {
						// Append the new audio to the end of the list
						current_card.addAudio( file_name );
					}
					else {
						// If it's not, then add it to the updated list.
						// Replacing the pre generated audio.
						current_card.setAudio( 0, file_name );
					}
				} else {
					//Just add the new audio.
					current_card.addAudio( file_name );
				}
			}

			// Don't add images if it's a 'sound' card.
			if ( getCurrentCard().isCardAWord() || getCurrentCard().isCardASentence() ) {
				// If it's an image, add an image tag.
				if( match_image.find() ) {
					// Save the new image
					if( (current_card.image_list.size() == 1)
					 && (current_card.updated_image_list.get(0) == null) ) {
						// Save the new image as the one to replace the original.
						// Test if the image file is not found.
						String image_filename = current_card.image_list.get( 0 );
						File image_file = new File( image_filename );

						if( image_file.exists() ) {
							// Append the new image to the end of the list
							current_card.addImage( file_name );
						}
						else {
							// If it's not, then add it to the updated list.
							// Replacing the pre generated image.
							current_card.setImage( 0, file_name );
						}
					} else {
						//Just add the new image.
						current_card.addImage( file_name );
					}
				}
			}
			
			// call the displayCard()to display the new additions.
			displayCard( current_card, true );
		}
	}
	
	
	public static void copyFile( String source, String destination ) {
		// Copy the file over
		Path source_path      = Paths.get( source );
		Path destination_path = Paths.get( destination );

		File make_directory = new File( destination );
		make_directory = new File( make_directory.getParent() );
		make_directory.mkdirs();

		try {
			Files.copy( source_path, destination_path, StandardCopyOption.REPLACE_EXISTING );

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Copy or delete media by comparing 2 different lists.
	 * The updated list must be the same size or bigger than the original list. 
	 * Will return the result of merging the two lists together.
	 * @param original_list
	 * @param updated_list
	 * @return
	 */
	public ArrayList<String> moveFilesAndGetUpdatedList( ArrayList<String> original_list, ArrayList<String> updated_list ) {
		
		ArrayList<String> newest_list = new ArrayList<String>();
		
		// Loop through the cards contents and
		// update all the items that are to be changed.
		for( int i = 0; i < original_list.size(); i++ ) {
			if( updated_list.get(i) == null ) {
				// Add the original data.
				newest_list.add( original_list.get( i ) );
				
			} else if( updated_list.get(i) == "" ) {
				// Delete files, because the user removed it from the card.
				// An empty string "" means we want to delete it from the card.

				// Working code below
				// Commented out for safety. test the variable values before just passing a delete instruction haha.
				
				//File file_to_delete = new File( original_list.get(i) );
				//System.out.println("delete file: " + file_to_delete.getPath() );
				//if ( file_to_delete.delete() ) {
				//	System.out.println("Deleted the file: " + file_to_delete.getName() );
				//} else {
				//	System.out.println("Not deleted: " + file_to_delete.getName() );
				//}
				
			} else {
				// Copy the file over
				String str_destination_path = original_list.get( i );
				String str_source_path      = updated_list.get ( i );

				copyFile( str_source_path, str_destination_path );

				newest_list.add( str_destination_path );
			}
		}

		if( original_list.size() > 0 ) {
			// Loop through the rest of the updated list which
			// will be new data to add to the card.
			for( int i = original_list.size(); i < updated_list.size(); i++ ) {
				if( updated_list.get(i) != null ) {
					String final_path = getMediaUpdatedFilePath(original_list, updated_list, i);

					copyFile(updated_list.get(i), final_path);
					newest_list.add( final_path );
				}
			}
		}
		
		return newest_list;
	}
	
	/**
	 * Get the updated file path for newly added media.
	 * The file will be renamed to match the card.
	 * E.g. audio.wav will become cat.wav
	 * audio.mp3 will become Reading Lesson Sentence 0002.mp3
	 * 
	 * Pass 2 lists and the index of the path we want to change.
	 * 
	 * The new file name will be the same as the one in the original_list, if there's one at the same indes.
	 * If there is no file in the original list, it'll keep the filename the same.
	 * If not, it will use the filename at original_list[0].
	 * If those fail, it'll just return the filename at the updated_list[ index ]
	 * If all fails, it'll return a null string. Null, so we get errors, because this path is used to write files, so I prefer null string to an empty one.
	 * @param original_list
	 * @param updated_list
	 * @param index
	 * @return
	 */
	public static String getMediaUpdatedFilePath( ArrayList<String> original_list, ArrayList<String> updated_list, int index ) {
		// TODO:
		// BUGFIX:
		// Lets say we remove a file cat_02.wav
		// cat_03.wav exists
		// we go to add new audio later on.
		// the audio will overwrite cat_03.wav
		// because this only does a basic check.
		// Fix this in the future.
		
		// Loop through the cards contents and
		// update all the items that are to be changed.
		if( index < original_list.size() ) {
			if( updated_list.get( index ) == null ) {
				// Add the original data.
				return original_list.get( index );
			} else if( updated_list.get( index ) == "" ) {
				// This file is planned for deletion, so pass null, so the program crashes if
				// we try to use this in a string.
				return null;
			} else {
				// Copy the file over
				return original_list.get( index );
			}
		}

		if( original_list.size() > 0 ) {
			// All data in the updated list after the last index in the original list will be new media added.
			if( index < updated_list.size() ) {
				if( updated_list.get( index ) != null ) {
					// Create a new name for the file, based on the filename in the original list.

					File original_filename = new File( original_list.get(0) );

					// Get file extension.
					String filename_without_extension = "";
					String file_extension = "";

					// Get the filename without it's extension.
					String name = original_filename.getName();
					int index_of_extension = name.lastIndexOf('.');
					if ( index_of_extension > 0) {
						filename_without_extension = name.substring( 0, index_of_extension );
					} else {
						filename_without_extension = name;
					}

					// Get the file extension of the file we wish to move.
					name = updated_list.get( index );
					index_of_extension = name.lastIndexOf('.');
					if ( index_of_extension > 0) {
						file_extension = name.substring( index_of_extension, name.length() );
					}

					// Make the new filepath.
					// Add 1 to index, because index should be at least 1 when in this loop,
					// and we want the file counter to start on 2.
					// For example, we want th.wav th_2.wav th_3.wav
					int file_counter = (index + 1);
					String final_path = original_filename.getParent() + File.separator + filename_without_extension + "_" + file_counter + file_extension;

					return final_path;
				}
			}
		}
		
		return null;
	}

	/**
	 * Will copy the files and update the card and then save it to the datebase.
	 */
	public void saveCard() {
		// Update the lists with new data
		current_card.audio_list = moveFilesAndGetUpdatedList( current_card.audio_list, current_card.updated_audio_list );
		current_card.image_list = moveFilesAndGetUpdatedList( current_card.image_list, current_card.updated_image_list );
		current_card.card_read_along_timings = current_card.updated_read_along_timings;
		
		// Saves the card and updates it.
		current_card.saveToDatabase( this.db_connection );
		
		// Move the card to the completed list if it's not already in there
		if( ! is_current_card_in_the_completed_list ) {
			int index = incomplete_cards_list.getSelectedIndex();
			complete_cards.add( incomplete_cards.get( index ) );
			incomplete_cards.remove( index );
			setIsCurrentCardInTheCompletedList( true );
		}
		
		// Update the incomplete and completed lists.
		complete_cards_list  .setListData( makeStringArrayFromCards( complete_cards   ) );
		incomplete_cards_list.setListData( makeStringArrayFromCards( incomplete_cards ) );

		// Draw it on screen.
		displayCard(current_card, true);
		
		// Move on to the next card automatically for us.
		if( incomplete_cards.size() > 0 ) {
			incomplete_cards_list.setSelectedIndex( 0 );
			setIsCurrentCardInTheCompletedList( false );
			displayCard( incomplete_cards.get(0), true );
		}
	}

	/**
	 * Scans through all reading lesson files and populates
	 * the complete_cards and incomplete_cards lists based
	 * on whether they are missing any media.
	 */
	public void loadCards( Connection db_connection ) {
		this.complete_cards   = new ArrayList<ReadingCardEditor>();
		this.incomplete_cards = new ArrayList<ReadingCardEditor>();


		// Load cards from database and add to incomplete and completed lists.
		try {
			String sql_query = "SELECT * FROM " + SQLiteReadingLessonHandler.TABLE_NAME + " ORDER BY card_id ASC;";
			Statement stat = db_connection.createStatement();

			ResultSet rs = stat.executeQuery( sql_query );
			while( rs.next() ) {
				boolean is_spelling_mode;
				if( rs.getInt( "is_spelling_mode" ) == 1 ) {
					is_spelling_mode = true;
				} else {
					is_spelling_mode = false;
				}

				ReadingCardEditor card = new ReadingCardEditor (
					rs.getInt   ( "card_id" ),
					rs.getLong  ( "date_in_millis" ),
					rs.getInt   ( "box_num" ),
					rs.getInt   ( "reading_lesson_level" ),
					rs.getString( "sound_type" ),
					rs.getString( "sound_word_or_sentence" ),
					rs.getInt   ( "id_of_linked_card" ),
					is_spelling_mode,
					rs.getString( "card_text" ),
					rs.getString( "card_images" ),
					rs.getString( "card_audio" ),
					rs.getString( "card_read_along_timings" )
				);

				// Add the card to the completed or incompleted list.
				if( card.isCardComplete() ) {
					complete_cards.add( card );
				} else {
					incomplete_cards.add( card );
				}
			}
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns an Array of Strings by getting the
	 * word, sound, or sentence from a reading lesson card.
	 * Sentences will be truncated to a width of MAXIMUM_JLIST_STRING_LENGTH.
	 */
	public String[] makeStringArrayFromCards ( ArrayList<ReadingCardEditor> cards ) {
		ArrayList<String> card_front_list = new ArrayList<String>();

		for( int i = 0; i < cards.size(); i++ ) {
			String text = cards.get(i).getText();
			cards.get(i).getText();

			// Truncate the string if it's too long.
			if( text.length() > MAXIMUM_JLIST_STRING_LENGTH ) {
				text = text.substring( 0, MAXIMUM_JLIST_STRING_LENGTH - 3 );
				text += "...";
			}

			card_front_list.add( text );
		}

		// Convert the ArrayList to an array and return.
		return card_front_list.toArray( new String[ card_front_list.size() ] );
	}

	public ReadingCardEditor getCurrentCard() {
		return this.current_card;
	}
	public void setCurrentCard( ReadingCardEditor card) {
		this.current_card = card;
	}
	public void setIsCurrentCardInTheCompletedList( boolean b ) {
		this.is_current_card_in_the_completed_list = b;
	}

	/**
	 * Draw the card on the screen.
	 * @param card
	 */
	public void displayCard( ReadingCardEditor card, boolean is_force_refresh ) {
		if( ! is_force_refresh ) {
			// Stop this method from being ran when we are already displaying the card.
			if( card == getCurrentCard() ) {
				return;
			} else {
				setCurrentCard( card );
			}
		}
		
		
		ArrayList<String> text_list = CardDBTagManager.makeStringAList( card.getText() );
		ArrayList<String> image_list = card.getImageFilePaths();
		ArrayList<String> audio_list = card.getAudioFilePaths();
		String read_along_timings = card.getReadAlongTimings();

		// Used later to check if we should display the "Completed" or "Incomplete" heading at the top of the card display.
		MyScrollableJPanel panel = new MyScrollableJPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS) );

		JLabel card_completion_title         = new JLabel( "" );
		JLabel card_text_title               = new JLabel( "Card's text: " );
		JLabel card_image_title              = new JLabel( "Card's Images: " );
		JLabel card_audio_title              = new JLabel( "Card's Audio: " );
		JLabel card_read_along_timings_title = new JLabel( "Card's Read Along Timings: " );

		card_completion_title        .setFont( large_font );
		card_text_title              .setFont( large_font );
		card_image_title             .setFont( large_font );
		card_audio_title             .setFont( large_font );
		card_read_along_timings_title.setFont( large_font );

		card_completion_title        .setAlignmentX( Component.CENTER_ALIGNMENT );
		card_text_title              .setAlignmentX( Component.CENTER_ALIGNMENT );
		card_image_title             .setAlignmentX( Component.CENTER_ALIGNMENT );
		card_audio_title             .setAlignmentX( Component.CENTER_ALIGNMENT );
		card_read_along_timings_title.setAlignmentX( Component.CENTER_ALIGNMENT );

		// Use a JLabel for padding as a JSeparator just doesn't give us enough padding.
		// Yeah, lazy, but it works lol.
		JLabel padding_label_1 = new JLabel("  ");
		JLabel padding_label_2 = new JLabel("  ");
		JLabel padding_label_3 = new JLabel("  ");
		padding_label_1.setFont(large_font);
		padding_label_2.setFont(large_font);
		padding_label_3.setFont(large_font);


		// Add card to the screen and show missing audio, images, and read along timings.
		panel.add( card_completion_title );

		
		JButton button_save_card = new JButton( "Save Card" );
		
		button_save_card.addActionListener(new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				saveCard();
			}
		});
		button_save_card.setEnabled( false );

		panel.add( button_save_card );
	
		// Display Card's text.
		panel.add( card_text_title );
		for(int i = 0; i < text_list.size(); i++ ) {
			String text = text_list.get(i);
			text = text.replaceAll( "<br>", "\n" );

			// Use a JTextPane to display the card's text.
			// This is better than a label, because it has word wrapping.
			JTextPane text_pane = new JTextPane();
			text_pane.setFont( medium_font );
			text_pane.setForeground( DARK_GREEN );

			// Make the text center aligned.
			StyledDocument doc = text_pane.getStyledDocument();
			SimpleAttributeSet center = new SimpleAttributeSet();
			StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
			
			// Add the text to the text_pane.
			try {
				doc.insertString(0, text, center);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			// Center align the text we just added.
			doc.setParagraphAttributes(0, doc.getLength(), center, false);
			text_pane.setStyledDocument( doc );
			
			// Set the text_pane to look more like a JLabel.
			text_pane.setOpaque( false );
			text_pane.setEditable(false);
			
			text_pane.setAlignmentX( Component.CENTER_ALIGNMENT );
			panel.add( text_pane );
		}

		// Display Card's images.
		if( card.isCardASentence() || card.isCardAWord() ) {
			panel.add( padding_label_1 );
			panel.add( new JSeparator() );
			panel.add( card_image_title );
			if( image_list.size() == 0 ) {
				// Display "media missing" label.
				JLabel label_missing = new JLabel( "Missing image - drag and drop image files here to add them.");
				label_missing.setFont( medium_font );
				label_missing.setForeground( DARK_RED );
				label_missing.setAlignmentX( Component.CENTER_ALIGNMENT );
				panel.add( label_missing );
			}
			else {
				// Add the images to the panel.
				for( int i = 0; i < image_list.size(); i++ ) {
					String image_filename = image_list.get(i);

					// Check if the image file exists.
					if( doesFileExist( image_filename ) ) {
						// Display the image
						JLabel label_image = new JLabel();
						ImageIcon icon = new ImageIcon( image_filename );
						label_image.setIcon( icon );
						label_image.setAlignmentX( Component.CENTER_ALIGNMENT );
						panel.add( label_image );

						if( i < image_list.size() -1 ) {
							// Add a blank spacer.
							JLabel pad = new JLabel("  ");
							pad.setFont(small_font);
							panel.add( pad );
						}
					}
					else {
						// Display "file missing" label.
						JLabel label_missing = new JLabel( "<html>File is missing:<br>" + image_filename + "</html>" );
						label_missing.setFont( small_font );
						label_missing.setForeground( DARK_RED );
						label_missing.setAlignmentX( Component.CENTER_ALIGNMENT );
						panel.add( label_missing );
					}
				}
			}
		}

		panel.add( padding_label_2 );
		panel.add( new JSeparator() );


		// Display Card's audio.
		panel.add( card_audio_title );

		if( audio_list.size() == 0 ) {
			// Display "media missing" label.
			JLabel label_missing = new JLabel( "Missing audio - drag and drop audio files here to add them.");
			label_missing.setFont( medium_font );
			label_missing.setForeground( DARK_RED );
			label_missing.setAlignmentX( Component.CENTER_ALIGNMENT );
			panel.add( label_missing );
		}
		else {
			// Add the audio to the panel.
			for( int i = 0; i < audio_list.size(); i++ ) {
				// Check if the image file exists.
				String filepath = audio_list.get( i );
				
				if( doesFileExist( filepath ) ) {
					// Add the audio player button.
					JButton audio_button = new JButton( filepath );
					audio_button.setAlignmentX( Component.CENTER_ALIGNMENT );
					audio_button.addActionListener( new AudioActionListener( filepath ) );
					panel.add( audio_button );

					if( i < audio_list.size() -1 ) {
						// Add a blank spacer.
						JLabel pad = new JLabel("  ");
						pad.setFont(small_font);
						panel.add( pad );
					}
				}
				else {
					// Display "file missing" label.
					JLabel label_missing = new JLabel( "<html>File is missing:<br>" + filepath + "</html>" );
					label_missing.setFont( small_font );
					label_missing.setForeground( DARK_RED );
					label_missing.setAlignmentX( Component.CENTER_ALIGNMENT );
					panel.add( label_missing );
				}
			}
		}

		panel.add( padding_label_3 );
		panel.add( new JSeparator() );


		// Display Card's read along timings.
		if( (audio_list.size() >= 0) && card.isCardASentence() ) {
			panel.add( card_read_along_timings_title );

			if( read_along_timings == null ) {
				// Display "media missing" label.
				JLabel label_missing = new JLabel( "Missing read along timings.");
				label_missing.setFont( medium_font );
				label_missing.setForeground( DARK_RED );
				label_missing.setAlignmentX( Component.CENTER_ALIGNMENT );

				JButton rat_button = new JButton();
				rat_button.setText ( "<html><b>Create Read Along Timing</b></html>" );
				rat_button.setAlignmentX( Component.CENTER_ALIGNMENT );
				
				ReadAlongTimingsActionListener rat_listener = new ReadAlongTimingsActionListener( this, audio_list.get(0), read_along_timings );
				rat_button.addActionListener( rat_listener );

				panel.add( label_missing );
				panel.add( rat_button );
			}
			else {
				// Read along timings exists, so edit it.
				JLabel lbl_rat_found = new JLabel("");

				// Check if the timings file exists.

				JButton rat_button = new JButton();
				rat_button.setFont( medium_font );
				rat_button.setAlignmentX( Component.CENTER_ALIGNMENT );

				rat_button.setText ( "<html><b>Edit Read Along Timing</b></html>" );
				lbl_rat_found = new JLabel("Read Along Timing Found.");
				lbl_rat_found.setFont( large_font );
				lbl_rat_found.setForeground( DARK_GREEN );
				lbl_rat_found.setAlignmentX( Component.CENTER_ALIGNMENT );

				ReadAlongTimingsActionListener rat_listener = new ReadAlongTimingsActionListener( this, audio_list.get(0), read_along_timings );
				rat_button.addActionListener( rat_listener );

				panel.add( lbl_rat_found );
				panel.add( rat_button );
			}
		}


		// Update the heading to show if the card is completed or not.
		if( card.isCardComplete() ) {
			button_save_card.setEnabled( true );

			Border border = BorderFactory.createLineBorder( DARK_GREEN, BORDER_THICKNESS );
			card_completion_title.setBorder( border );

			card_completion_title.setText( "    Card is Completed :)    " );
			card_completion_title.setBackground( LIGHT_GREEN );
			card_completion_title.setOpaque( true );
		} else {
			Border border = BorderFactory.createLineBorder( DARK_RED, BORDER_THICKNESS );
			card_completion_title.setBorder( border );

			card_completion_title.setText( "    Card has missing media!   " );
			card_completion_title.setBackground( LIGHT_RED );
			card_completion_title.setOpaque( true );
		}


		// Display the new panel.
		this.cards_content_panel.removeAll();
		this.cards_content_sub_panel = panel;
		
		JScrollPane scroll_pane = new JScrollPane( this.cards_content_sub_panel );
		scroll_pane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER   );
		scroll_pane.setVerticalScrollBarPolicy  ( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		this.cards_content_panel.add( scroll_pane );

		// For some reason the panel doesn't update on my system, but this forces it to.
		frame.repaint();
		frame.validate();
	}
}
/**
 * Manage lists of words for the two lists at each side of the JFrame.
 * Most common words list and the previous lessons words list.
 *
 */
class WordList {
	private ArrayList<String> word_list;
	// Used to give each word a number that will stay the same.
	private ArrayList<Integer> word_index;
	
	WordList( ArrayList<String> list ) {
		this.word_list = list;
		
		// Make an index, so that each word has it's own number that won't change.
		this.word_index = new ArrayList<Integer>();
		for( int i = 0; i < this.word_list.size(); i++ ) {
			// Add one so the index isn't zero based.
			this.word_index.add( i+1 );
		}

	}
	
	public WordList( DefaultListModel<String> list ) {
		// Convert it to an ArrayList
		ArrayList<String> final_list = Collections.list(list.elements());
		
		this.word_list = final_list;
		
		// Make an index, so that each word has it's own number that won't change.
		this.word_index = new ArrayList<Integer>();
		for( int i = 0; i < this.word_list.size(); i++ ) {
			// Add one so the index isn't zero based.
			this.word_index.add( i+1 );
		}
	}

	public ArrayList<String> getReversedList() {
		ArrayList<String> final_list;
		
		final_list = (ArrayList<String>) word_list.clone();
		
		Collections.reverse(final_list );
		return final_list;
	}
	
	public ArrayList<String> getWordList() {
		return word_list;
	}
	
	public ArrayList<String> getNumberedList() {
		ArrayList<String> final_list = new ArrayList<String>();
		
		for( int i = 0; i < word_list.size(); i++ ) {
			String str = getWordWithCounter( i );
			final_list.add( str );
		}
		
		return final_list;
	}
	
	public ArrayList<String> getNumberedListReversed() {
		ArrayList<String> final_list = getNumberedList();
		Collections.reverse( final_list );
		
		return final_list;
	}
	
	private String getWordWithCounter( int index ) {
		String result = "";
		String seperator = " - ";
		// Add one to the counter, to stop it from being zero indexed
		int counter = word_index.get(index);
		String word = word_list.get(index);
		
		if( counter <= 9 ) {
			result = "00" + counter + seperator + word;
		}
		else if( counter <= 99 ) {
			result = "0"  + counter + seperator + word;
		} else {
			result = ""   + counter + seperator + word;
		}
		
		return result;
	}
	
	public void removeWords(DefaultListModel<String> words_to_remove) {
		for( int i = 0; i < word_list.size(); i++ ) {
			for( int k = 0; k < words_to_remove.size(); k++ ) {
				String remove = words_to_remove.get(k);
				String word = word_list.get(i);
				
				if( word.compareToIgnoreCase(remove) == 0 ){
					word_list.remove( i );
					word_index.remove( i );
					i--;
					break;
				}
			}
		}
	}
	public void removeWords(ArrayList<String> words_to_remove) {
		for( int i = 0; i < word_list.size(); i++ ) {
			for( int k = 0; k < words_to_remove.size(); k++ ) {
				String remove = words_to_remove.get(k);
				String word = word_list.get(i);
				
				if( word.compareToIgnoreCase(remove) == 0 ){
					word_list.remove( i );
					word_index.remove( i );
					i--;
					break;
				}
			}
		}
	}
}

class MostCommonWords {
	//DefaultListModel<Word> most_common_words;
	WordList most_common_words;
	
	public MostCommonWords() {
		makeList();
	}
	
	public ArrayList<String> getMostCommonWords() {
		return most_common_words.getWordList();
	}
	
	public ArrayList<String> getMostCommonWordsAsNumberedList() {
		return most_common_words.getNumberedList();
	}
	
	public void removeWords( ArrayList<String> list ) {
		most_common_words.removeWords( list );
	}
	public void removeWords( DefaultListModel<String> list ) {
		most_common_words.removeWords( list );
	}
	
	private void makeList() {
		ArrayList<String> mcw = new ArrayList<String>();
		mcw.add( "the" );
		mcw.add( "of" );
		mcw.add( "to" );
		mcw.add( "and" );
		mcw.add( "a" );
		mcw.add( "in" );
		mcw.add( "is" );
		mcw.add( "it" );
		mcw.add( "you" );
		mcw.add( "that" );
		mcw.add( "he" );
		mcw.add( "was" );
		mcw.add( "for" );
		mcw.add( "on" );
		mcw.add( "are" );
		mcw.add( "with" );
		mcw.add( "as" );
		mcw.add( "I" );
		mcw.add( "his" );
		mcw.add( "they" );
		mcw.add( "be" );
		mcw.add( "at" );
		mcw.add( "one" );
		mcw.add( "have" );
		mcw.add( "this" );
		mcw.add( "from" );
		mcw.add( "or" );
		mcw.add( "had" );
		mcw.add( "by" );
		mcw.add( "hot" );
		mcw.add( "but" );
		mcw.add( "some" );
		mcw.add( "what" );
		mcw.add( "there" );
		mcw.add( "we" );
		mcw.add( "can" );
		mcw.add( "out" );
		mcw.add( "other" );
		mcw.add( "were" );
		mcw.add( "all" );
		mcw.add( "your" );
		mcw.add( "when" );
		mcw.add( "up" );
		mcw.add( "use" );
		mcw.add( "word" );
		mcw.add( "how" );
		mcw.add( "said" );
		mcw.add( "an" );
		mcw.add( "each" );
		mcw.add( "she" );
		mcw.add( "which" );
		mcw.add( "do" );
		mcw.add( "their" );
		mcw.add( "time" );
		mcw.add( "if" );
		mcw.add( "will" );
		mcw.add( "way" );
		mcw.add( "about" );
		mcw.add( "many" );
		mcw.add( "then" );
		mcw.add( "them" );
		mcw.add( "would" );
		mcw.add( "write" );
		mcw.add( "like" );
		mcw.add( "so" );
		mcw.add( "these" );
		mcw.add( "her" );
		mcw.add( "long" );
		mcw.add( "make" );
		mcw.add( "thing" );
		mcw.add( "see" );
		mcw.add( "him" );
		mcw.add( "two" );
		mcw.add( "has" );
		mcw.add( "look" );
		mcw.add( "more" );
		mcw.add( "day" );
		mcw.add( "could" );
		mcw.add( "go" );
		mcw.add( "come" );
		mcw.add( "did" );
		mcw.add( "my" );
		mcw.add( "sound" );
		mcw.add( "no" );
		mcw.add( "most" );
		mcw.add( "number" );
		mcw.add( "who" );
		mcw.add( "over" );
		mcw.add( "know" );
		mcw.add( "water" );
		mcw.add( "than" );
		mcw.add( "call" );
		mcw.add( "first" );
		mcw.add( "people" );
		mcw.add( "may" );
		mcw.add( "down" );
		mcw.add( "side" );
		mcw.add( "been" );
		mcw.add( "now" );
		mcw.add( "find" );
		mcw.add( "any" );
		mcw.add( "new" );
		mcw.add( "work" );
		mcw.add( "part" );
		mcw.add( "take" );
		mcw.add( "get" );
		mcw.add( "place" );
		mcw.add( "made" );
		mcw.add( "live" );
		mcw.add( "where" );
		mcw.add( "after" );
		mcw.add( "back" );
		mcw.add( "little" );
		mcw.add( "only" );
		mcw.add( "round" );
		mcw.add( "man" );
		mcw.add( "year" );
		mcw.add( "came" );
		mcw.add( "show" );
		mcw.add( "every" );
		mcw.add( "good" );
		mcw.add( "me" );
		mcw.add( "give" );
		mcw.add( "our" );
		mcw.add( "under" );
		mcw.add( "name" );
		mcw.add( "very" );
		mcw.add( "through" );
		mcw.add( "just" );
		mcw.add( "form" );
		mcw.add( "much" );
		mcw.add( "great" );
		mcw.add( "think" );
		mcw.add( "say" );
		mcw.add( "help" );
		mcw.add( "low" );
		mcw.add( "line" );
		mcw.add( "before" );
		mcw.add( "turn" );
		mcw.add( "cause" );
		mcw.add( "same" );
		mcw.add( "mean" );
		mcw.add( "differ" );
		mcw.add( "move" );
		mcw.add( "right" );
		mcw.add( "boy" );
		mcw.add( "old" );
		mcw.add( "too" );
		mcw.add( "does" );
		mcw.add( "tell" );
		mcw.add( "sentence" );
		mcw.add( "set" );
		mcw.add( "three" );
		mcw.add( "want" );
		mcw.add( "air" );
		mcw.add( "well" );
		mcw.add( "also" );
		mcw.add( "play" );
		mcw.add( "small" );
		mcw.add( "end" );
		mcw.add( "put" );
		mcw.add( "home" );
		mcw.add( "read" );
		mcw.add( "hand" );
		mcw.add( "port" );
		mcw.add( "large" );
		mcw.add( "spell" );
		mcw.add( "add" );
		mcw.add( "even" );
		mcw.add( "land" );
		mcw.add( "here" );
		mcw.add( "must" );
		mcw.add( "big" );
		mcw.add( "high" );
		mcw.add( "such" );
		mcw.add( "follow" );
		mcw.add( "act" );
		mcw.add( "why" );
		mcw.add( "ask" );
		mcw.add( "men" );
		mcw.add( "change" );
		mcw.add( "went" );
		mcw.add( "light" );
		mcw.add( "kind" );
		mcw.add( "off" );
		mcw.add( "need" );
		mcw.add( "house" );
		mcw.add( "picture" );
		mcw.add( "try" );
		mcw.add( "us" );
		mcw.add( "again" );
		mcw.add( "animal" );
		mcw.add( "point" );
		mcw.add( "mother" );
		mcw.add( "world" );
		mcw.add( "near" );
		mcw.add( "build" );
		mcw.add( "self" );
		mcw.add( "earth" );
		mcw.add( "father" );
		mcw.add( "head" );
		mcw.add( "stand" );
		mcw.add( "own" );
		mcw.add( "page" );
		mcw.add( "should" );
		mcw.add( "country" );
		mcw.add( "found" );
		mcw.add( "answer" );
		mcw.add( "school" );
		mcw.add( "grow" );
		mcw.add( "study" );
		mcw.add( "still" );
		mcw.add( "learn" );
		mcw.add( "plant" );
		mcw.add( "cover" );
		mcw.add( "food" );
		mcw.add( "sun" );
		mcw.add( "four" );
		mcw.add( "thought" );
		mcw.add( "let" );
		mcw.add( "keep" );
		mcw.add( "eye" );
		mcw.add( "never" );
		mcw.add( "last" );
		mcw.add( "door" );
		mcw.add( "between" );
		mcw.add( "city" );
		mcw.add( "tree" );
		mcw.add( "cross" );
		mcw.add( "since" );
		mcw.add( "hard" );
		mcw.add( "start" );
		mcw.add( "might" );
		mcw.add( "story" );
		mcw.add( "saw" );
		mcw.add( "far" );
		mcw.add( "sea" );
		mcw.add( "draw" );
		mcw.add( "left" );
		mcw.add( "late" );
		mcw.add( "run" );
		mcw.add( "don't" );
		mcw.add( "while" );
		mcw.add( "press" );
		mcw.add( "close" );
		mcw.add( "night" );
		mcw.add( "real" );
		mcw.add( "life" );
		mcw.add( "few" );
		mcw.add( "stop" );
		mcw.add( "open" );
		mcw.add( "seem" );
		mcw.add( "together" );
		mcw.add( "next" );
		mcw.add( "white" );
		mcw.add( "children" );
		mcw.add( "begin" );
		mcw.add( "got" );
		mcw.add( "walk" );
		mcw.add( "example" );
		mcw.add( "ease" );
		mcw.add( "paper" );
		mcw.add( "often" );
		mcw.add( "always" );
		mcw.add( "music" );
		mcw.add( "those" );
		mcw.add( "both" );
		mcw.add( "mark" );
		mcw.add( "book" );
		mcw.add( "letter" );
		mcw.add( "until" );
		mcw.add( "mile" );
		mcw.add( "river" );
		mcw.add( "car" );
		mcw.add( "feet" );
		mcw.add( "care" );
		mcw.add( "second" );
		mcw.add( "group" );
		mcw.add( "carry" );
		mcw.add( "took" );
		mcw.add( "rain" );
		mcw.add( "eat" );
		mcw.add( "room" );
		mcw.add( "friend" );
		mcw.add( "began" );
		mcw.add( "idea" );
		mcw.add( "fish" );
		mcw.add( "mountain" );
		mcw.add( "north" );
		mcw.add( "once" );
		mcw.add( "base" );
		mcw.add( "hear" );
		mcw.add( "horse" );
		mcw.add( "cut" );
		mcw.add( "sure" );
		mcw.add( "watch" );
		mcw.add( "color" );
		mcw.add( "face" );
		mcw.add( "wood" );
		mcw.add( "main" );
		most_common_words = new WordList( mcw );
	}

}

/** TODO:
 * Make an 'ignore this' button for certain consonant-vowel pairs that will
 * add a comment to the lesson file to ignore it. There will be some consonant
 * pairs I will need to skip.
 * 
*/


class PreviewLessonJFrame extends JFrame {
	public PreviewLessonJFrame(  String lesson_contents ) {
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setSize( 500, 500 );
		
		JTextArea text_area = new JTextArea();
		
		String os_name = System.getProperty("os.name" );
		Font monospaced;
		if( os_name.contains("linux") ) {
			monospaced = new Font("DejaVu Sans Mono", Font.PLAIN, 14);
		}
		else if( os_name.contains("windows") ) {
			monospaced = new Font("Courier New", Font.PLAIN, 14);
		}
		else {
			// Give up and use the current font.
			monospaced = text_area.getFont();
		}
		text_area.setFont( monospaced );
		text_area.setText( lesson_contents );
		text_area.setEditable( false );
		
		// Check the max tab size we should use.
		int tab_size = 8;
		for( String line: lesson_contents.split("\n") ) {
			for( String column : line.split("\t") ) {
				if( column.length() > tab_size ) {
					// Ignore the sentence line as it's too long anyway.
					if( ! line.contains("#SENTENCE#") ) {
						tab_size = column.length();
					}
				}
			}
		}
		text_area.setTabSize( tab_size );
		
		text_area.addKeyListener( new CloseWindowActionListener() );
		
		
		this.add( new JScrollPane(text_area) );
		this.pack();
		this.setVisible( true );
		
	}
	
	class CloseWindowActionListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			// Close this.
			dispose();
		}

		@Override
		public void keyTyped(KeyEvent e) { }


		@Override
		public void keyReleased(KeyEvent e) { }
	}
}

class MyScrollableJPanel extends JPanel implements Scrollable {
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
       return 50;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}

class AudioActionListener implements ActionListener {
	String filepath;

	AudioActionListener( String filepath ) {
		this.filepath = filepath;
		
	}
	public void actionPerformed( ActionEvent event ) {
		try {
			MyAudioPlayer audio_player = new MyAudioPlayer( new File( this.filepath ));
			audio_player.play();
		} catch ( UnsupportedAudioFileException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		} catch ( LineUnavailableException e ) {
			e.printStackTrace();
		}
	}
}
