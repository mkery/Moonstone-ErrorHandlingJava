package edu.cmu.moonstone.ghostcomment;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Holds the relevant info for a single Ghost Comment.
 * 
 * @author Michael Puskas
 */
public class CommentInfo {
	private final List<ExceptionInfo> exceptions; //The fully qualified name of the exception thrown
	private final int commentOffset;
	private final boolean hasGhost;
	private final List<String> stringSegments;
	private final Point[] extentSegments;
	private final String blankStringSegment;
	private final Point blankSegment;
	private final String text;

	private CommentInfo(GC gc, List<ExceptionInfo> exceptions, int commentOffset, boolean hasGhost, List<String> stringSegments, String text) {
		this.exceptions = exceptions;
		this.commentOffset = commentOffset;
		this.hasGhost = hasGhost;
		this.stringSegments = stringSegments;
		this.text = text;

		if (!stringSegments.isEmpty()) {
			String sb = stringSegments.get(0);
			int blankEnd = 0, initialLength = sb.length();
			while (blankEnd < initialLength && Character.isWhitespace(sb.charAt(blankEnd))) blankEnd++;
			blankStringSegment = sb.substring(0, blankEnd);
		} else blankStringSegment = "";

		if (gc != null) {
			extentSegments = stringSegments.stream().map(gc::stringExtent).toArray(Point[]::new);
			blankSegment = gc.stringExtent(blankStringSegment);
		} else {
			Point point = new Point(0, 0);
			extentSegments = stringSegments.stream().map(s -> point).toArray(Point[]::new);
			blankSegment = point;
		}
	}

	public static CommentInfo create(GC gc, List<ExceptionInfo> exceptions, int commentOffset, List<String> stringSegments) {
		return new CommentInfo(gc, exceptions, commentOffset, false, stringSegments, null);
	}

	public static CommentInfo create(GC gc, List<ExceptionInfo> exceptions, int commentOffset) {
		StringBuilder sb = new StringBuilder(" // may throw");
		int exceptionCount = exceptions.size();

		List<String> stringSegments = new ArrayList<>(exceptionCount + 1);
		stringSegments.add(sb.toString());

		int from = sb.length();
		sb.append(' ');
		sb.append(exceptions.get(0).getException().getName());
		for (int i = 1; i < exceptionCount; i++) {
			sb.append(',');
			stringSegments.add(sb.substring(from));
			from = sb.length();
			sb.append(' ');
			sb.append(exceptions.get(i).getException().getName());
		}
		stringSegments.add(sb.substring(from) + " ");
		return new CommentInfo(gc, exceptions, commentOffset, true, stringSegments, sb.toString());
	}

	public static CommentInfo create(GC gc, ExceptionInfo exception, int commentOffset, String text) {
		return new CommentInfo(gc, Collections.singletonList(exception), commentOffset, true, Arrays.asList(text, ""), text);
	}

	public ExceptionInfo confirmHover(ITextViewer viewer, int hoveredX, int hoveredY) {
		Point location = getLocation(viewer);
		if (location == null) return null;

		// Initialize coordinates to beginning of first exception
		int x = location.x;

		// Hover position is to the left or above the coordinates -> outside box
		if (hoveredY < location.y || hoveredX < x + blankSegment.x) return null;

		// count initial segment towards first
		x += extentSegments[0].x;

		// Advance y coordinate by line height
		int y = location.y + extentSegments[0].y;

		for (int i = 1; i < extentSegments.length; i++) {
			// Advance x coordinate by width of exception name
			x += extentSegments[i].x;

			// Hover position is to the left and above the coordinates -> is inside box
			if (hoveredY <= y && hoveredX <= x) {
                // Return corresponding exception
                return exceptions.get(i - 1);
            }
		}
		return null;
	}

	public ExceptionInfo confirmHit(int offset) {
		// Initialize coordinates to beginning of first exception
		int location = commentOffset;

		// Hover position is to the left or above the coordinates -> outside box
		if (offset < location + blankStringSegment.length()) return null;

		// count initial segment towards first
		location += stringSegments.get(0).length();

		for (int i = 1; i < stringSegments.size(); i++) {
			// Advance x coordinate by width of exception name
			location += stringSegments.get(i).length();

			// Hover position is to the left and above the coordinates -> is inside box
			if (offset <= location) {
				// Return corresponding exception
				return exceptions.get(i - 1);
			}
		}
		return null;
	}

	public Point getLocation(ITextViewer viewer) {
		try {
			ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
			StyledText st = viewer.getTextWidget();

			int widgetOffset = extension5.modelOffset2WidgetOffset(commentOffset);
			return st.getLocationAtOffset(widgetOffset);
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public int getCommentOffset() {
		return commentOffset;
	}

	public int getCommentLength() {
		return text.length();
	}

	public String getText() {
		return text;
	}

	public boolean hasGhost() {
		return hasGhost;
	}

	public List<ExceptionInfo> getExceptions() {
		return exceptions;
	}
}
