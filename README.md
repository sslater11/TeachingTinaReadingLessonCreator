# Currently work in progress :).

# What is this?
This is a reading lesson flashcard creator for the TeachingTina App.

The purpose of this program suite is to be a flashcard program for people with learning difficulties.

The methodology combines Spaced Repetition with Deep Learning.
You can google spaced repetition to save me from explaining what that is. The most popular example is the flashcard app Anki which is renowned for helping medical students, programmers, language learners and more.
The Deep Learning comes from the book The Talent Code, and my own experiences with learning and teaching.

Several flashcards will be shown to the user, and they must answer each card correctly 5 times. If they get it wrong, then they must get it right 5 times.
By doing this we focus on learning just a few small cards in one day. The next day they will be asked these same questions, then again at a later date, and so on. If at any point they fail a flashcard, no matter how well they know it, they must answer it 5 times correctly and it's now treated like a new card, so they'll see it often over the next few days and weeks.

# The Reading Lessons
Each lesson is incremental with the goal of learning the most common words in the English language, along with the sounds that letter pairs can make.
A word will be broken down into smaller to learn chunks. The word "cat" will be broken down into several flashcards. "ca", "at", "cat". By learning that "ca" makes a "ka" sound and "at" makes an "at" sound, along with seeing the word "cat", leaning can take place.

As words are broken down into smaller sounds, those with vowels will be shown in reverse too. For example, "Cat" contains "ca" and "at". I have noticed that Tina sees "ca" and "ac" as the same thing. She also used to read "23" and "32" as the same thing, until I made flashcards that specifically got her to learn both "23" and "32" together. This will generate the cards "ca" and "ac", so she won't get those confused. It will also generate cards for "at" and "ta".

Each lesson will end with a small sentence for the user to read.

# Usage
Type a sentence and the words that haven't already been made into a lesson will be highlighted red.
Click the create lesson button and that's a new lesson file created.

On the right we see these new words added to a list of known words.

On the left is a list of the most common words in the English language, listed in order of how common they are. We should try to pick words from the top of this list as they're more important to learn first.

We then need to record the audio for each card, add images, and also add the timings to highlight words with the audio for the sentence reading mode.

The lesson created will contain all the new words in the sentence and turn them into flashcards. There will also be a flashcard created for the sentence we have typed. This is so the user can get started with reading sentences straight away.

# Methodology
The idea is to write very simple sentences, with very simple words and ideas, and to eventually start writing harder sentences.
By writing any sentence, we will likely add the most common words in the English language. This follows Benny Lewis's idea from his book Fluent in 3 Months. His idea for learning a new language is to take a news article in a language you want to learn and learn all the words it contains. Afterwards, you can read that news article, even though your vocabulary is quite limited, and you know nothing of the language's grammar.

# Dependencies / Source code
To compile this, you will need to link the library LibTeachingTinaDBManager. I do this by having the folder libteachingtinadbmanager inside the src/ folder.

# Credits
Me. It's all me, baby. Well so far.
