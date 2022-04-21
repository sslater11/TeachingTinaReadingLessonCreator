/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

import libteachingtinadbmanager.*;
import libteachingtinadbmanager.ReadingLessonCreator;
import libteachingtinadbmanager.SentenceAnalyzer;
import libteachingtinadbmanager.WordWithIndexes;

import javax.swing.event.*;
import javax.swing.text.DefaultEditorKit.*;
import javax.swing.text.StyledEditorKit.*;

import java.io.File;
import java.util.*;
import java.util.List;

public class TeachingTinaReadingLessonCreator {
	private JFrame frame;
	private JTextPane text_editor;
	private JComboBox<String> font_size_combo_box;
	
	private ReadingLessonCreator previous_lesson = null;

	private static final String MAIN_TITLE = "Tina's Reading Lesson Creator";
	private static final String DEFAULT_FONT_FAMILY = "SansSerif";
	private static final int DEFAULT_FONT_SIZE = 30;
	private static final int DEFAULT_FONT_SIZE_SMALL = 14;
	//private static final List<String> FONT_LIST = Arrays.asList(new String [] {"Arial", "Calibri", "Cambria", "Courier New", "Comic Sans MS", "Dialog", "Georgia", "Helevetica", "Lucida Sans", "Monospaced", "Tahoma", "Times New Roman", "Verdana"});
	private static final String [] FONT_SIZES  = {"Font Size", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "36", "48", "72"};

	String font_family;
	Font small_font = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE_SMALL);
	JLabel lbl_reading_level;
	JList previous_lessons_words_list;
	JList most_common_words_list;
	JScrollPane common_words_scroll;
	
	public static void main(String[] args) {
		TeachingTinaReadingLessonCreator app = new TeachingTinaReadingLessonCreator();
	}
	
	TeachingTinaReadingLessonCreator() {
		loadLessons();
		
		// Create the frame and pane.
		frame = new JFrame(MAIN_TITLE);
		text_editor = new JTextPane();

		JScrollPane text_editor_scroll_pane = new JScrollPane(text_editor);

		
		// Create the labels
		JLabel lbl_hint1 = new JLabel("<html><div style='text-align: center;'>" + "<b>Red</b> = A new word." + "</div></html>" );
		lbl_hint1.setFont( small_font );

		JLabel lbl_hint2 = new JLabel("<html><div style='text-align: center;'>" + "<b>Black</b> = A word in previous lesson file." + "</div></html>" );
		lbl_hint2.setFont( small_font );

		this.lbl_reading_level = new JLabel( "Reading Level is at: " );
		this.lbl_reading_level.setFont( small_font );

		JLabel lbl_previous_lessons_words_title = new JLabel( "<html><div style='text-align: center;'>" + "<b>Previous Lessons<br>Words</b>" + "</div></html>" );
		JLabel lbl_common_words_title = new JLabel("<html><div style='text-align: center;'>" + "<b>Most Common<br>Words<br>That Tina<br>Doesn't Know Yet<b>"  + "</div></html>");

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
		FocusOnTextActionListener focus_on_text_action_listener = new FocusOnTextActionListener();
		CreateLessonActionListener create_lesson_action_listener = new CreateLessonActionListener();
		PreviewLessonActionListener preview_lesson_action_listener = new PreviewLessonActionListener();

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

		JPanel hint_1_panel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		JPanel hint_2_panel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		hint_1_panel.add( lbl_hint1, BorderLayout.CENTER );
		hint_2_panel.add( lbl_hint2, BorderLayout.CENTER );

		JPanel toolbar_panel = new JPanel();
		toolbar_panel.setLayout( new BoxLayout(toolbar_panel, BoxLayout.PAGE_AXIS) );
		toolbar_panel.add( controls_panel );
		toolbar_panel.add( hint_1_panel );
		toolbar_panel.add( hint_2_panel );
		
		
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
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
		frame.setExtendedState( JFrame.MAXIMIZED_BOTH );
		
		updateComponents();

		text_editor.requestFocusInWindow();
		
	}
	
	public void addNewLesson( ReadingLessonCreator lesson ) {
		if( this.previous_lesson == null ) {
			this.previous_lesson = lesson;
		}
		else {
			// Link the 2 new lessons together.
			this.previous_lesson.setNextLesson( lesson );
			lesson.setPreviousLesson( this.previous_lesson );
			
			lesson.setLevel( this.previous_lesson.getLevel() + 1 );
			// set this as the most recent lesson.
			this.previous_lesson = lesson;
		}
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
	private void setCurrentReadingLevel( int reading_level ) {
		this.lbl_reading_level.setText(("<html><b>Current reading level of known words is: " + getCurrentReadingLevel() + "&nbsp;&nbsp;&nbsp;&nbsp;Max reading level is: " + getMaxReadingLevel() + "</b></html>") );
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
			final StyleContext style_context = StyleContext.getDefaultStyleContext();
	
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
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return text;
	}

	/**
	 * Will return the reading level that the current text is at
	 */
	private int getCurrentReadingLevel() {
		if( this.previous_lesson == null ) {
			return 0;
		} else {
			return this.previous_lesson.getCurrentReadingLevel( getWordsListFromEditor() );
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
		// TODO: Write the code to load the deck settings, if we need it?
		DeckSettings deck_settings = null;
		
		// Get a list of all lesson files
		ArrayList<String> all_lessons = TextEditorDBManager.getAllLessonFiles();
		/**
		 * Scan through these and setup our array of lessons.
		 **/
		for(int i = 0; i < all_lessons.size(); i++ ) {
			// Load the database file into memory.
			File file = new File( all_lessons.get(i) );
			ReadingLessonCreator lesson = TextEditorDBManager.readDBFile( file, deck_settings );

			addNewLesson( lesson );
		}
	}
	
	public static void printAList(ArrayList<String> list) {
		for( int i = 0; i < list.size(); i++ ) {
			System.out.println( list.get(i) );
		}
	}

	private void updateComponents() {
		// Update the reading level dispayed.
		setCurrentReadingLevel( getCurrentReadingLevel() );

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
			ArrayList<String> list = getWordsListFromEditor();
			
			ReadingLessonCreator deck = new ReadingLessonCreator( getPreviousLesson(), getTextFromEditor() );

			if( deck.hasNewWords() ) {
				String all_lines = "";
				for( String i : TextEditorDBManager.getDatabaseOutput( deck ) ) {
					all_lines += i + "\n";
				}
				new PreviewLessonJFrame( all_lines );
			}
		}
	}

	
	private class CreateLessonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			ReadingLessonCreator deck = new ReadingLessonCreator( getPreviousLesson(), getTextFromEditor() );
			
			
			deck.printAllLessonContentsAsLessonFile();
			
			if( deck.hasNewWords() ) {
				// Write the new lesson to a file.
				String filepath = TextEditorDBManager.getDirectory() + TextEditorDBManager.getFileName( deck.getLevel() );
				File filename = new File( filepath );
				System.out.println("writing to: " + filepath );
				TextEditorDBManager.writeDB(filename, deck);
				
				// Now add the lesson to the previous lessons
				addNewLesson(deck);
				
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

}
 
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
			this.word_index.add( new Integer(i+1) );
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
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
			
			dispose();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
}

