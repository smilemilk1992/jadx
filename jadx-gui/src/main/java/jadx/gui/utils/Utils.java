package jadx.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final ImageIcon ICON_STATIC = openIcon("static_co");
	private static final ImageIcon ICON_FINAL = openIcon("final_co");
	private static final ImageIcon ICON_ABSTRACT = openIcon("abstract_co");
	private static final ImageIcon ICON_NATIVE = openIcon("native_co");

	public static final Font FONT_HACK = openFontTTF("Hack-Regular");

	/**
	 * The minimum about of memory in bytes we are trying to keep free, otherwise the application may run out of heap
	 * which ends up in a Java garbage collector running "amok" (CPU utilization 100% for each core and the UI is
	 * not responsive).
	 * <p>
	 * We can calculate and store this value here as the maximum heap is fixed for each JVM instance
	 * and can't be changed at runtime.
	 */
	public static final long MIN_FREE_MEMORY = calculateMinFreeMemory();

	private Utils() {
	}

	public static ImageIcon openIcon(String name) {
		String iconPath = "/icons-16/" + name + ".png";
		URL resource = Utils.class.getResource(iconPath);
		if (resource == null) {
			throw new JadxRuntimeException("Icon not found: " + iconPath);
		}
		return new ImageIcon(resource);
	}

	public static Image openImage(String path) {
		URL resource = Utils.class.getResource(path);
		if (resource == null) {
			throw new JadxRuntimeException("Image not found: " + path);
		}
		return Toolkit.getDefaultToolkit().createImage(resource);
	}

	@Nullable
	public static Font openFontTTF(String name) {
		String fontPath = "/fonts/" + name + ".ttf";
		try (InputStream is = Utils.class.getResourceAsStream(fontPath)) {
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			return font.deriveFont(12f);
		} catch (Exception e) {
			LOG.error("Failed load font by path: {}", fontPath, e);
			return null;
		}
	}

	public static void addKeyBinding(JComponent comp, KeyStroke key, String id, Action action) {
		comp.getInputMap().put(key, id);
		comp.getActionMap().put(id, action);
	}

	public static String typeFormat(String name, ArgType type) {
		return "<html><body><nobr>" + name
				+ "<span style='color:#888888;'> : " + typeStr(type) + "</span>"
				+ "</nobr></body></html>";
	}

	public static String typeStr(ArgType type) {
		if (type == null) {
			return "null";
		}
		if (type.isObject()) {
			String cls = type.getObject();
			int dot = cls.lastIndexOf('.');
			if (dot != -1) {
				return cls.substring(dot + 1);
			} else {
				return cls;
			}
		}
		if (type.isArray()) {
			return typeStr(type.getArrayElement()) + "[]";
		}
		return type.toString();
	}

	public static OverlayIcon makeIcon(AccessInfo af, Icon pub, Icon pri, Icon pro, Icon def) {
		Icon icon;
		if (af.isPublic()) {
			icon = pub;
		} else if (af.isPrivate()) {
			icon = pri;
		} else if (af.isProtected()) {
			icon = pro;
		} else {
			icon = def;
		}
		OverlayIcon overIcon = new OverlayIcon(icon);
		if (af.isFinal()) {
			overIcon.add(ICON_FINAL);
		}
		if (af.isStatic()) {
			overIcon.add(ICON_STATIC);
		}
		if (af.isAbstract()) {
			overIcon.add(ICON_ABSTRACT);
		}
		if (af.isNative()) {
			overIcon.add(ICON_NATIVE);
		}
		return overIcon;
	}

	/**
	 * @return 20% of the maximum heap size limited to 512 MB (bytes)
	 */
	public static long calculateMinFreeMemory() {
		Runtime runtime = Runtime.getRuntime();
		long minFree = (long) (runtime.maxMemory() * 0.2);
		return Math.min(minFree, 512 * 1024L * 1024L);
	}

	public static boolean isFreeMemoryAvailable() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalFree = runtime.freeMemory() + (maxMemory - runtime.totalMemory());
		return totalFree > MIN_FREE_MEMORY;
	}

	public static String memoryInfo() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		return "heap: " + format(allocatedMemory - freeMemory) +
				", allocated: " + format(allocatedMemory) +
				", free: " + format(freeMemory) +
				", total free: " + format(freeMemory + maxMemory - allocatedMemory) +
				", max: " + format(maxMemory);
	}

	private static String format(long mem) {
		return (long) (mem / (double) (1024L * 1024L)) + "MB";
	}

	/**
	 * Adapt character case for case insensitive searches
	 */
	public static char caseChar(char ch, boolean toLower) {
		return toLower ? Character.toLowerCase(ch) : ch;
	}

	public static void setClipboardString(String text) {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = new StringSelection(text);
			clipboard.setContents(transferable, null);
			LOG.debug("String '{}' copied to clipboard", text);
		} catch (Exception e) {
			LOG.error("Failed copy string '{}' to clipboard", text, e);
		}
	}

	@NotNull
	public static String getFontStyleName(int style) {
		if (style == 0) {
			return "plain";
		}
		StringBuilder sb = new StringBuilder();
		if ((style & Font.BOLD) != 0) {
			sb.append("bold");
		}
		if ((style & Font.ITALIC) != 0) {
			sb.append(" italic");
		}
		return sb.toString().trim();
	}

	public static void setWindowIcons(Window window) {
		List<Image> icons = new ArrayList<>();
		icons.add(Utils.openImage("/logos/jadx-logo-16px.png"));
		icons.add(Utils.openImage("/logos/jadx-logo-32px.png"));
		icons.add(Utils.openImage("/logos/jadx-logo-48px.png"));
		icons.add(Utils.openImage("/logos/jadx-logo.png"));
		window.setIconImages(icons);
	}

	public static final int CTRL_BNT_KEY = getCtrlButton();

	private static int getCtrlButton() {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		} else {
			return InputEvent.CTRL_DOWN_MASK;
		}
	}

	public static int ctrlButton() {
		return CTRL_BNT_KEY;
	}
}
