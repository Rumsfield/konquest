package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.IllegalFormatException;

public class LanguageManager {

	private final Konquest konquest;
	private FileConfiguration lang;
	private boolean isValid;
	
	public LanguageManager(Konquest konquest) {
		this.konquest = konquest;
		this.isValid = false;
	}
	
	public void initialize() {
		lang = konquest.getConfigManager().getLang();
		if(lang == null) {
			ChatUtil.printConsoleError("Failed to load any language messages");
		} else {
			if(validateMessages()) {
				isValid = true;
			} else {
				ChatUtil.printConsoleError("Failed to validate language messages. Correct the above issues with the language YAML file.");
			}
		}
		ChatUtil.printDebug("Language Manager is ready");
	}
	
	public boolean isLanguageValid() {
		return isValid;
	}
	
	public String get(MessagePath messagePath, Object ...args) {
		String result;
		String path = messagePath.getPath();
		if(lang.contains(path)) {
			int formats = messagePath.getFormats();
			if(formats != args.length) {
				ChatUtil.printConsoleError("Language file message format mismatch. Expected "+formats+", got "+args.length+" for path "+path);
			}
			if(args.length > 0) {
				try {
					result = String.format(lang.getString(messagePath.getPath()), args);
				} catch(IllegalFormatException e) {
					ChatUtil.printConsoleError("Language file has bad message format for path "+path+": "+e.getMessage());
					result = lang.getString(messagePath.getPath());
				}
			} else {
				result = lang.getString(path);
			}
		} else {
			ChatUtil.printConsoleError("Language file is missing path: "+path);
			result = "<MISSING>";
		}
		return result;
	}
	
	private boolean validateMessages() {
		boolean result = true;
		String formatStr = "%s";
		for(MessagePath messagePath : MessagePath.values()) {
			if(lang.contains(messagePath.getPath(),false)) {
				String message = lang.getString(messagePath.getPath(),"");
				int formats = messagePath.getFormats();
				if(message.equals("")) {
					result = false;
					ChatUtil.printConsoleError("Language file is missing message for path: "+messagePath.getPath());
				} else if(formats > 0) {
					// Count occurrences of "%s" within message to verify format count
					int lastIndex = 0;
					int count = 0;
					while(lastIndex != -1) {
						lastIndex = message.indexOf(formatStr,lastIndex);
						if(lastIndex != -1) {
							count++;
							lastIndex += formatStr.length();
						}
					}
					if(count != formats) {
						result = false;
						ChatUtil.printConsoleError("Language file message has bad format. Requires "+formats+" \"%s\" but contains "+count+" for path: "+messagePath.getPath());
					}
				}
			} else {
				result = false;
				ChatUtil.printConsoleError("Language file is missing path: "+messagePath.getPath());
			}
		}
		return result;
	}
}
