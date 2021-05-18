package konquest.manager;

import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class LanguageManager {

	private Konquest konquest;
	private FileConfiguration lang;
	private boolean isValid;
	
	public LanguageManager(Konquest konquest) {
		this.konquest = konquest;
		this.isValid = false;
	}
	
	public void initialize() {
		lang = konquest.getConfigManager().getConfig("language");
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
	
	public String get(MessagePath messagePath) {
		return get(messagePath, Collections.emptyList());
	}
	
	public String get(MessagePath messagePath, List<String> args) {
		if(!isValid) {
			return "Bad Language YAML!";
		}
		String result = "";
		String path = messagePath.getPath();
		if(lang.contains(path)) {
			int formats = messagePath.getFormats();
			if(formats != args.size()) {
				ChatUtil.printConsoleError("Language file message format mismatch. Expected "+formats+", got "+args.size()+" for path "+path);
			}
			if(args.size() > 0) {
				if(args.size() == 1) {
					try {
						result = String.format(lang.getString(messagePath.getPath()), args.get(0));
					} catch(IllegalFormatException e) {
						ChatUtil.printConsoleError("Language file has bad message format, requires 1 \"%s\": "+path);
						result = "<BAD FORMAT> "+lang.getString(messagePath.getPath());
					}
				} else if(args.size() == 2) {
					try {
						result = String.format(lang.getString(messagePath.getPath()), args.get(0), args.get(1));
					} catch(IllegalFormatException e) {
						ChatUtil.printConsoleError("Language file has bad message format, requires 2 \"%s\": "+path);
						result = "<BAD FORMAT> "+lang.getString(messagePath.getPath());
					}
				} else if(args.size() == 3) {
					try {
						result = String.format(lang.getString(path), args.get(0), args.get(1), args.get(2));
					} catch(IllegalFormatException e) {
						ChatUtil.printConsoleError("Language file has bad message format, requires 3 \"%s\": "+path);
						result = "<BAD FORMAT> "+lang.getString(path);
					}
				} else if(args.size() == 4) {
					try {
						result = String.format(lang.getString(path), args.get(0), args.get(1), args.get(2), args.get(3));
					} catch(IllegalFormatException e) {
						ChatUtil.printConsoleError("Language file has bad message format, requires 4 \"%s\": "+path);
						result = "<BAD FORMAT> "+lang.getString(path);
					}
				} else if(args.size() == 5) {
					try {
						result = String.format(lang.getString(path), args.get(0), args.get(1), args.get(2), args.get(3), args.get(4));
					} catch(IllegalFormatException e) {
						ChatUtil.printConsoleError("Language file has bad message format, requires 5 \"%s\": "+path);
						result = "<BAD FORMAT> "+lang.getString(path);
					}
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
					// Count occurances of "%s" within message to verify format count
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
