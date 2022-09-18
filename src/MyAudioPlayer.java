/* © Copyright 2022, Simon Slater

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


public class MyAudioPlayer {
	File audio_file;
	AudioInputStream audio_stream;
	Clip clip;
	public MyAudioPlayer( File audio_file) throws UnsupportedAudioFileException, IOException, LineUnavailableException 
	{
		// Create and open the audio stream.

		// create AudioInputStream object
		this.audio_file = audio_file;
		this.audio_stream = AudioSystem.getAudioInputStream( audio_file );
		// create clip reference
		this.clip = AudioSystem.getClip();

		// open audioInputStream to the clip
		this.clip.open(audio_stream);
	}
	
	public void play() {
		this.clip.start();
	}
	public void stop() {
		this.clip.stop();
	}

	public long getMicrosecondPosition() {
		return this.clip.getMicrosecondPosition();
	}
}
