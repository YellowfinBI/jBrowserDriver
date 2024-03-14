/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.machinepublishers.glass.ui.monocle;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;

import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

class MonocleRobot extends GlassRobot {
    @Override
    public void create() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void keyPress(KeyCode code) {
        Platform.runLater(() -> {
            KeyState state = new KeyState();
            KeyInput.getInstance().getState(state);
            state.pressKey(code);
            KeyInput.getInstance().setState(state);
        });
    }

    @Override
    public void keyRelease(KeyCode code) {
        Platform.runLater(() -> {
            KeyState state = new KeyState();
            KeyInput.getInstance().getState(state);
            state.releaseKey(code);
            KeyInput.getInstance().setState(state);
        });
    }

    @Override
    public void mouseMove(double x, double y) {
        Platform.runLater(() -> {
            MouseState state = new MouseState();
            MouseInput.getInstance().getState(state);
            state.setX((int) x);
            state.setY((int) y);
            MouseInput.getInstance().setState(state, false);
        });
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Platform.runLater(() -> {
            MouseState state = new MouseState();
            MouseInput.getInstance().getState(state);
            for (MouseButton button : buttons) {
                switch (button) {
                case PRIMARY:
                    state.pressButton(MouseEvent.BUTTON_LEFT);
                    break;
                case MIDDLE:
                    state.pressButton(MouseEvent.BUTTON_OTHER);
                    break;
                case SECONDARY:
                    state.pressButton(MouseEvent.BUTTON_RIGHT);
                    break;
                default:
                    break;
                }
            }
            MouseInput.getInstance().setState(state, false);
        });
    }

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Platform.runLater(() -> {
            MouseState state = new MouseState();
            MouseInput.getInstance().getState(state);
            for (MouseButton button : buttons) {
                switch (button) {
                case PRIMARY:
                    state.releaseButton(MouseEvent.BUTTON_LEFT);
                    break;
                case MIDDLE:
                    state.releaseButton(MouseEvent.BUTTON_OTHER);
                    break;
                case SECONDARY:
                    state.releaseButton(MouseEvent.BUTTON_RIGHT);
                    break;
                default:
                    break;
                }
            }
            MouseInput.getInstance().setState(state, false);
        });
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        Platform.runLater(() -> {
            MouseState state = new MouseState();
            MouseInput mouse = MouseInput.getInstance();
            mouse.getState(state);
            int direction =
                    wheelAmt < 0 ? MouseState.WHEEL_DOWN : MouseState.WHEEL_UP;
            for (int i = 0; i < Math.abs(wheelAmt); i++) {
                state.setWheel(direction);
                mouse.setState(state, false);
                state.setWheel(MouseState.WHEEL_NONE);
                mouse.setState(state, false);
            }
        });
    }

    @Override
    public double getMouseX() {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        return state.getX();
    }

    @Override
    public double getMouseY() {
        MouseState state = new MouseState();
        MouseInput.getInstance().getState(state);
        return state.getY();
    }

    @Override
    public Color getPixelColor(double x, double y) {
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        final int byteDepth = screen.getDepth() >>> 3;
        final int bwidth = screen.getWidth();
        final int bheight = screen.getHeight();

        if (x < 0 || x > bwidth || y < 0 || y > bheight) {
            return GlassRobot.convertFromIntArgb(0);
        }

        synchronized (NativeScreen.framebufferSwapLock) {

            ByteBuffer buffer = screen.getScreenCapture();

            if (byteDepth == 2) {
                ShortBuffer shortbuf = buffer.asShortBuffer();

                int v = shortbuf.get((int) (y * bwidth + x));
                int red = (int) ((v & 0xF800) >> 11) << 3;
                int green = (int) ((v & 0x7E0) >> 5) << 2;
                int blue = (int) (v & 0x1F) << 3;

                int p = (0xff000000
                        | (red << 16)
                        | (green << 8)
                        | blue);
                return GlassRobot.convertFromIntArgb(p);
            } else if (byteDepth >= 4) {
                IntBuffer intbuf = buffer.asIntBuffer();
                return GlassRobot.convertFromIntArgb(intbuf.get((int) (y * bwidth + x)));
            } else {
                throw new RuntimeException("Unknown bit depth");
            }
        }
    }

    @Override
    public WritableImage getScreenCapture(WritableImage image, double x,
            double y, double width, double height, boolean scaleToFit) {
        return convertFromPixels(image, _getScreenCapture((int) x, (int) y,
                (int) width, (int) height, scaleToFit));
    }

    protected Pixels _getScreenCapture(int x, int y, int width, int height,
            boolean isHiDPI) {
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        final int byteDepth = screen.getDepth() >>> 3;
        final int scrWidth = screen.getWidth();
        final int scrHeight = screen.getHeight();

        synchronized (NativeScreen.framebufferSwapLock) {
            IntBuffer buffer = screen.getScreenCapture().asIntBuffer();

            if (x == 0 && y == 0 && width == scrWidth && height == scrHeight) {
                return new MonoclePixels(width, height, buffer);
            }

            IntBuffer ret = IntBuffer.allocate(width * height);
            int rowStop = Math.min(y + height, scrHeight);
            int colStop = Math.min(x + width, scrWidth);
            for (int row = y; row < rowStop; row++) {
                for (int col = x; col < colStop; col++) {
                    ret.put(buffer.get(row * scrWidth + col));
                }
            }

            ret.rewind();
            return new MonoclePixels(width, height, ret);
        }
    }
}
