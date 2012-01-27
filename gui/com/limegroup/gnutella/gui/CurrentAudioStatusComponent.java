package com.limegroup.gnutella.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.frostwire.alexandria.PlaylistItem;
import com.frostwire.gui.bittorrent.SendFileProgressDialog;
import com.frostwire.gui.library.FileDescriptor;
import com.frostwire.gui.library.LibraryMediator;
import com.frostwire.gui.player.AudioPlayer;
import com.frostwire.gui.player.AudioPlayerListener;
import com.frostwire.gui.player.AudioSource;
import com.frostwire.gui.player.DeviceAudioSource;
import com.frostwire.gui.player.InternetRadioAudioSource;
import com.frostwire.mplayer.MediaPlaybackState;

/*
 * The component at the right bottom of the screen showing what's the current audio being played.
 * Click on it to switch to the library, playlist and scroll to it.
 */
public class CurrentAudioStatusComponent extends JPanel implements AudioPlayerListener {
	
	private static final int MAX_CHARS = 33;
	private static final int BOUND_CHARS = 12;
	
	private static final long serialVersionUID = 9206657876064353272L;
	
	private MediaButton shareButton;
	private Icon speakerIcon;
	private JLabel text;
	private MediaPlaybackState lastState;
	
	private Icon currentStatusIcon;
	private String currentStatusLabel;
	
	public CurrentAudioStatusComponent() {
		AudioPlayer.instance().addAudioPlayerListener(this);
		lastState=MediaPlaybackState.Uninitialized;
		initComponents();
	}

	private void initComponents() {
		Dimension dimension = new Dimension(220,22);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		
		speakerIcon = GUIMediator.getThemeImage("speaker");

		text = new JLabel();
		Font f = new Font("DIALOG",Font.BOLD,10);
		text.setFont(f);
		text.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (AudioPlayer.instance().getCurrentSong().getFile()!=null ||
					AudioPlayer.instance().getCurrentSong().getPlaylistItem()!=null ||
					AudioPlayer.instance().getCurrentSong() instanceof InternetRadioAudioSource ||
					AudioPlayer.instance().getCurrentSong() instanceof DeviceAudioSource) {
					showCurrentSong();
				} else if (AudioPlayer.instance().getCurrentSong().getURL()!=null) {
					
					if (text.getText().startsWith("<html><font color=\"496989\"><u>android")) {
						GUIMediator.instance().setWindow(GUIMediator.Tabs.ANDROID);
					} else {
						GUIMediator.instance().setWindow(GUIMediator.Tabs.LIBRARY);
					}
				}
			}
		});
		
		shareButton = new MediaButton(I18n.tr("Send this file to a friend"), "share", "share");
		shareButton.addActionListener(new SendToFriendActionListener());
		shareButton.setVisible(false);
		
		//Share Button
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0,0,0,3);
		add(shareButton,c);//, BorderLayout.LINE_END);
		
		//Go to Current Audio Control
		c.gridx = 0;
		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(0,0,0,0);
		add(text,c);//, BorderLayout.LINE_END);
	}
	
	private final class SendToFriendActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			AudioSource currentSong = AudioPlayer.instance().getCurrentSong();
			
			if (currentSong == null) {
				return;
			}
			
			File file =  null;
			
			if (currentSong.getFile() != null) {
				file = currentSong.getFile();
			} else if (currentSong.getPlaylistItem()!=null &&
					currentSong.getPlaylistItem().getFilePath() != null) {
				file = new File(currentSong.getPlaylistItem().getFilePath()); 
			}
			
			if (file == null){
				return;
			}

			String fileFolder = file.isFile() ? I18n.tr("file") : I18n
					.tr("folder");
			int result = JOptionPane.showConfirmDialog(
					GUIMediator.getAppFrame(),
					I18n.tr("Do you want to send this {0} to a friend?",
							fileFolder) + "\n\n\"" + file.getName() + "\"",
					I18n.tr("Send files with FrostWire"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				new SendFileProgressDialog(GUIMediator.getAppFrame(), file)
						.setVisible(true);
				GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
			}
		}
	}
	
	public void showCurrentSong() {
		GUIMediator.instance().setWindow(GUIMediator.Tabs.LIBRARY);
		LibraryMediator.instance().selectCurrentSong();
	}

	@Override
	public void songOpened(AudioPlayer audioPlayer, AudioSource audioSource) {
	  //update controls
        AudioSource currentSong = audioPlayer.getCurrentSong();
        PlaylistItem playlistItem = currentSong.getPlaylistItem();
        
        String currentText = null;
        
        if (currentSong instanceof DeviceAudioSource) {
            FileDescriptor fd = ((DeviceAudioSource)currentSong).getFileDescriptor();
            String artistName = fd.artist;
            String songTitle = fd.title;
            
            String albumToolTip = fd.album;
            String yearToolTip = fd.year;
            
            currentText = artistName + " - " + songTitle;
            
            text.setToolTipText(artistName + " - " + songTitle + albumToolTip + yearToolTip);
        } else if (playlistItem != null) {
            //Playing from Playlist.
            String artistName = playlistItem.getTrackArtist();
            String songTitle = playlistItem.getTrackTitle();
            
            String albumToolTip = (playlistItem.getTrackAlbum() != null && playlistItem.getTrackAlbum().length() > 0) ? " - " + playlistItem.getTrackAlbum() : "";
            String yearToolTip = (playlistItem.getTrackYear()!=null && playlistItem.getTrackYear().length() > 0) ? " ("+playlistItem.getTrackYear() +")" : "";
            
            currentText = artistName + " - " + songTitle;
            
            text.setToolTipText(artistName + " - " + songTitle + albumToolTip + yearToolTip);
            
        }  else if (currentSong.getFile()!=null) {
            //playing from Audio.
            currentText = AudioPlayer.instance().getCurrentSong().getFile().getName();
            
            text.setToolTipText(currentSong.getFile().getAbsolutePath());
        } else if (currentSong.getFile() == null && currentSong.getURL() != null) {
            System.out.println("StreamURL: " + currentSong.getURL().toString());
            
            String streamURL = currentSong.getURL().toString();
            Pattern urlStart = Pattern.compile("(http://[\\d\\.]+:\\d+).*");
            Matcher matcher = urlStart.matcher(streamURL);
            
            currentText = "internet "; // generic internet stream
        }
        
        currentStatusIcon = speakerIcon;
        currentStatusLabel = currentText;
	}

	@Override
	public void progressChange(AudioPlayer audioPlayer, float currentTimeInSecs) {
		
	}

	@Override
	public void volumeChange(AudioPlayer audioPlayer, double currentVolume) {
		
	}

	@Override
	public void stateChange(AudioPlayer audioPlayer, MediaPlaybackState state) {

		if (lastState == state) {
			return;
		}

		lastState = state;
		
		if (state != MediaPlaybackState.Playing && state != MediaPlaybackState.Paused) {

			GUIMediator.safeInvokeLater(new Runnable() {
				@Override
				public void run() {
					text.setIcon(null);
					text.setText("");
					shareButton.setVisible(false);
				}
			});
		} else {
		    setupIconAndText(currentStatusIcon, currentStatusLabel);
		}
	}

    private void setupIconAndText(Icon speakerIcon2, String currentText) {
        if (currentText.length() > MAX_CHARS) {
            currentText = currentText.substring(0, BOUND_CHARS) + " ... " + currentText.substring(currentText.length()-BOUND_CHARS);
        }
        
        final String currentTextFinal = currentText;
        
        AudioSource currentSong = AudioPlayer.instance().getCurrentSong();
        
      //only share files that exist
        shareButton
                .setVisible(currentSong != null
                        && (currentSong.getFile() != null || (currentSong
                                .getPlaylistItem() != null
                                && currentSong.getPlaylistItem().getFilePath() != null && new File(currentSong.getPlaylistItem()
                                        .getFilePath())
                                .exists())));
        
        GUIMediator.safeInvokeLater(new Runnable() {
            
            @Override
            public void run() {
                text.setIcon(speakerIcon);
                text.setText("<html><font color=\"496989\"><u>"+currentTextFinal+"</u></font></html>");             
            }
        });
    }

    @Override
    public void icyInfo(AudioPlayer audioPlayer, String data) {
        for (String s : data.split(";")) {
            if (s.startsWith("StreamTitle=")) {
                try {
                    String streamTitle = s.substring(13, s.length() - 1);
                    currentStatusIcon = speakerIcon;
                    currentStatusLabel = "radio " + streamTitle;
                    setupIconAndText(currentStatusIcon, currentStatusLabel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
