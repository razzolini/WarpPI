package it.cavallium.warppi.gui.graphicengine.impl.jansi256colors;

import it.cavallium.warppi.gui.graphicengine.Renderer;

public class JAnsi256Renderer implements Renderer {

	JAnsi256Font currentFont;
	protected char[] charmatrix = new char[JAnsi256Engine.C_WIDTH * JAnsi256Engine.C_HEIGHT];
	protected int[] fgColorMatrix = new int[JAnsi256Engine.C_WIDTH * JAnsi256Engine.C_HEIGHT];
	protected int[] bgColorMatrix = new int[JAnsi256Engine.C_WIDTH * JAnsi256Engine.C_HEIGHT];
	protected int clearColor = JAnsi256Renderer.rgbToX256(0xc5, 0xc2, 0xaf);
	protected int curColor = clearColor;
	public JAnsi256Skin currentSkin;

	public static final String ANSI_PREFIX = "\u001B[";
	public static final String ansiFgColorPrefix = "38;5;";
	public static final String ansiBgColorPrefix = "48;5;";
	public static final String ansiColorSuffix = "m";

	public static final String ANSI_RESET = "\u001B[0m";
	public static final char FILL = 0xDB;
	public static final int TRANSPARENT = 0xF0000;

	public static int v2ci(final int v_U) {
		return v_U < 48 ? 0 : v_U < 115 ? 1 : (v_U - 35) / 40;
	}

	public static int colorIndex(final int ir, final int ig, final int ib) {
		return 36 * ir + 6 * ig + ib;
	}

	public static int distSquare(final int a, final int b, final int c, final int a_U, final int b_U, final int c_U) {
		return (a - a_U) * (a - a_U) + (b - b_U) * (b - b_U) + (c - c_U) * (c - c_U);
	}

	/**
	 * Calculate the represented colors back from the index
	 */
	public static int[] i2cv = { 0, 0x5f, 0x87, 0xaf, 0xd7, 0xff };

	public static int rgbToX256(final int r_U, final int g_U, final int b_U) {
		// Calculate the nearest 0-based color index at 16 .. 231

		final int ir = JAnsi256Renderer.v2ci(r_U), ig = JAnsi256Renderer.v2ci(g_U), ib = JAnsi256Renderer.v2ci(b_U); // 0..5 each
		/* 0..215, lazy evaluation */

		// Calculate the nearest 0-based gray index at 232 .. 255
		final int average = (r_U + g_U + b_U) / 3;
		final int grayIndex = average > 238 ? 23 : (average - 3) / 10; // 0..23

		final int cr = JAnsi256Renderer.i2cv[ir], cg = JAnsi256Renderer.i2cv[ig], cb = JAnsi256Renderer.i2cv[ib]; // r/g/b, 0..255 each
		final int gv = 8 + 10 * grayIndex; // same value for r/g/b, 0..255

		// Return the one which is nearer to the original input rgb value

		final int colorErr = JAnsi256Renderer.distSquare(cr, cg, cb, r_U, g_U, b_U);
		final int grayErr = JAnsi256Renderer.distSquare(gv, gv, gv, r_U, g_U, b_U);
		return colorErr <= grayErr ? 16 + JAnsi256Renderer.colorIndex(ir, ig, ib) : 232 + grayIndex;
	}

	@Override
	public void glColor3i(final int r, final int gg, final int b) {
		curColor = JAnsi256Renderer.rgbToX256(r, gg, b);
	}

	@Override
	public void glColor(final int c) {
		curColor = JAnsi256Renderer.rgbToX256(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF);
	}

	@Override
	public void glColor4i(final int red, final int green, final int blue, final int alpha) {
		curColor = JAnsi256Renderer.rgbToX256(red, green, blue);
	}

	@Override
	public void glColor3f(final float red, final float green, final float blue) {
		curColor = JAnsi256Renderer.rgbToX256((int) (red * 255), (int) (green * 255), (int) (blue * 255));
	}

	@Override
	public void glColor4f(final float red, final float green, final float blue, final float alpha) {
		curColor = JAnsi256Renderer.rgbToX256((int) (red * 255), (int) (green * 255), (int) (blue * 255));
	}

	@Override
	public void glClearColor4i(final int red, final int green, final int blue, final int alpha) {
		clearColor = JAnsi256Renderer.rgbToX256(red, green, blue);
	}

	@Override
	public void glClearColor4f(final float red, final float green, final float blue, final float alpha) {
		clearColor = JAnsi256Renderer.rgbToX256((int) (red * 255), (int) (green * 255), (int) (blue * 255));
	}

	@Override
	public int glGetClearColor() {
		return clearColor;
	}

	@Override
	public void glClearColor(final int c) {
		clearColor = JAnsi256Renderer.rgbToX256(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF);
	}

	@Override
	public void glClear(final int screenWidth, final int screenHeight) {
		clearAll();
	}

	@Override
	public void glDrawLine(float x1, float y1, float x2, float y2) {
		x1 /= JAnsi256Engine.C_MUL_X;
		x2 /= JAnsi256Engine.C_MUL_X;
		y1 /= JAnsi256Engine.C_MUL_Y;
		y2 /= JAnsi256Engine.C_MUL_Y;

		final int dx = (int) Math.abs(x2 - x1);
		final int dy = (int) Math.abs(y2 - y1);

		final int sx = x1 < x2 ? 1 : -1;
		final int sy = y1 < y2 ? 1 : -1;

		int err = dx - dy;

		while (true) {
			if ((int) x1 >= JAnsi256Engine.C_WIDTH || (int) y1 >= JAnsi256Engine.C_HEIGHT || (int) x2 >= JAnsi256Engine.C_WIDTH || (int) y2 >= JAnsi256Engine.C_HEIGHT)
				break;
			bgColorMatrix[(int) x1 + (int) y1 * JAnsi256Engine.C_WIDTH] = curColor;
			charmatrix[(int) x1 + (int) y1 * JAnsi256Engine.C_WIDTH] = ' ';

			if (x1 == x2 && y1 == y2)
				break;

			final int e2 = 2 * err;

			if (e2 > -dy) {
				err = err - dy;
				x1 = x1 + sx;
			}

			if (e2 < dx) {
				err = err + dx;
				y1 = y1 + sy;
			}
		}
	}

	@Override
	public void glFillRect(final float x, final float y, final float width, final float height, final float uvX,
			final float uvY, final float uvWidth, final float uvHeight) {
		if (currentSkin != null)
			glDrawSkin((int) (x / JAnsi256Engine.C_MUL_X), (int) (y / JAnsi256Engine.C_MUL_Y), (int) (uvX / JAnsi256Engine.C_MUL_X), (int) (uvY / JAnsi256Engine.C_MUL_Y), (int) ((uvWidth + uvX) / JAnsi256Engine.C_MUL_X), (int) ((uvHeight + uvY) / JAnsi256Engine.C_MUL_Y), true);
		else
			glFillColor(x, y, width, height);
	}

	@Override
	public void glFillColor(final float x, final float y, final float width, final float height) {
		final int ix = (int) x / JAnsi256Engine.C_MUL_X;
		final int iy = (int) y / JAnsi256Engine.C_MUL_Y;
		final int iw = (int) width / JAnsi256Engine.C_MUL_X;
		final int ih = (int) height / JAnsi256Engine.C_MUL_Y;

		int x1 = ix + iw;
		int y1 = iy + ih;
		if (ix >= JAnsi256Engine.C_WIDTH || iy >= JAnsi256Engine.C_WIDTH)
			return;
		if (x1 >= JAnsi256Engine.C_WIDTH)
			x1 = JAnsi256Engine.C_WIDTH;
		if (y1 >= JAnsi256Engine.C_HEIGHT)
			y1 = JAnsi256Engine.C_HEIGHT;
		final int sizeW = JAnsi256Engine.C_WIDTH;
		for (int px = ix; px < x1; px++)
			for (int py = iy; py < y1; py++) {
				bgColorMatrix[px + py * sizeW] = curColor;
				charmatrix[px + py * sizeW] = ' ';
			}
	}

	@Override
	public void glDrawCharLeft(final int x, final int y, final char ch) {
		final int cx = x / JAnsi256Engine.C_MUL_X;
		final int cy = y / JAnsi256Engine.C_MUL_Y;
		if (cx >= JAnsi256Engine.C_WIDTH || cy >= JAnsi256Engine.C_HEIGHT)
			return;
		charmatrix[cx + cy * JAnsi256Engine.C_WIDTH] = ch;
		fgColorMatrix[cx + cy * JAnsi256Engine.C_WIDTH] = curColor;
	}

	@Override
	public void glDrawCharCenter(final int x, final int y, final char ch) {
		glDrawCharLeft(x, y, ch);
	}

	@Override
	public void glDrawCharRight(final int x, final int y, final char ch) {
		final int cx = x / JAnsi256Engine.C_MUL_X - 1;
		final int cy = y / JAnsi256Engine.C_MUL_Y;
		if (cx >= JAnsi256Engine.C_WIDTH || cy >= JAnsi256Engine.C_HEIGHT)
			return;
		charmatrix[cx + cy * JAnsi256Engine.C_WIDTH] = ch;
		fgColorMatrix[cx + cy * JAnsi256Engine.C_WIDTH] = curColor;
	}

	@Override
	public void glDrawStringLeft(final float x, final float y, final String text) {
		final int cx = (int) x / JAnsi256Engine.C_MUL_X;
		final int cy = (int) y / JAnsi256Engine.C_MUL_Y;
		int i = 0;
		for (final char c : text.toCharArray()) {
			if (cx + i >= JAnsi256Engine.C_WIDTH || cy >= JAnsi256Engine.C_HEIGHT)
				break;
			charmatrix[cx + i + cy * JAnsi256Engine.C_WIDTH] = c;
			fgColorMatrix[cx + i + cy * JAnsi256Engine.C_WIDTH] = curColor;
			i++;
		}
	}

	@Override
	public void glDrawStringCenter(final float x, final float y, final String text) {
		final int cx = (int) x / JAnsi256Engine.C_MUL_X - text.length() / 2;
		final int cy = (int) y / JAnsi256Engine.C_MUL_Y;
		int i = 0;
		for (final char c : text.toCharArray()) {
			if (cx + i >= JAnsi256Engine.C_WIDTH || cy >= JAnsi256Engine.C_HEIGHT)
				break;
			charmatrix[cx + i + cy * JAnsi256Engine.C_WIDTH] = c;
			fgColorMatrix[cx + i + cy * JAnsi256Engine.C_WIDTH] = curColor;
			i++;
		}
	}

	@Override
	public void glDrawStringRight(final float x, final float y, final String text) {
		// TODO Auto-generated method stub

	}

	private void glDrawSkin(int x0, int y0, int s0, int t0, int s1, int t1, final boolean transparent) {
		int newColor;
		final int onex = s0 <= s1 ? 1 : -1;
		final int oney = t0 <= t1 ? 1 : -1;
		int width = 0;
		int height = 0;
		if (onex == -1) {
			final int s00 = s0;
			s0 = s1;
			s1 = s00;
			width = s1 - s0;
		}
		if (oney == -1) {
			final int t00 = t0;
			t0 = t1;
			t1 = t00;
			height = t1 - t0;
		}
		if (x0 >= JAnsi256Engine.C_WIDTH || y0 >= JAnsi256Engine.C_WIDTH)
			return;
		if (x0 + width >= JAnsi256Engine.C_WIDTH)
			s1 = JAnsi256Engine.C_WIDTH - x0 + s0;
		if (y0 + height >= JAnsi256Engine.C_HEIGHT)
			t1 = JAnsi256Engine.C_HEIGHT - y0 + t0;
		if (x0 < 0) {
			if (onex == -1) {
				width += x0;
				s1 += x0 + 1;
			} else
				s0 -= x0;
			x0 = 0;
		}
		if (y0 < 0) {
			if (oney == -1) {
				height += y0;
				t1 += y0 + 1;
			} else
				t0 -= y0;
			y0 = 0;
		}
		int pixelX;
		int pixelY;
		for (int texx = 0; texx < s1 - s0; texx++)
			for (int texy = 0; texy < t1 - t0; texy++) {
				pixelX = x0 + texx * onex + width;
				pixelY = y0 + texy * oney + height;
				if (pixelY < JAnsi256Engine.C_HEIGHT)
					if (pixelX - pixelX % JAnsi256Engine.C_WIDTH == 0) {
						newColor = currentSkin.skinData[s0 + texx + (t0 + texy) * currentSkin.skinSize[0]];
						if (transparent && !((newColor & JAnsi256Renderer.TRANSPARENT) == JAnsi256Renderer.TRANSPARENT)) {
							charmatrix[pixelX + pixelY * JAnsi256Engine.C_WIDTH] = ' ';
							bgColorMatrix[pixelX + pixelY * JAnsi256Engine.C_WIDTH] = newColor;
						}
					}
			}
	}

	@Override
	public void glClearSkin() {
		currentSkin = null;
	}

	protected void clearAll() {
		for (int i = 0; i < JAnsi256Engine.C_WIDTH * JAnsi256Engine.C_HEIGHT; i++) {
			charmatrix[i] = ' ';
			bgColorMatrix[i] = clearColor;
			fgColorMatrix[i] = 0;
		}
	}

	@Override
	public JAnsi256Font getCurrentFont() {
		return currentFont;
	}

}