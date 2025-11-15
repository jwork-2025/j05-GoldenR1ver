package com.gameengine.input;

import com.gameengine.math.Vector2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 输入管理器：单例模式，管理键盘和鼠标输入状态。
 * 支持按键状态跟踪、瞬时按键检测和鼠标位置追踪。
 */
public class InputManager {
    private static InputManager instance;   // 单例实例
    private Set<Integer> pressedKeys;       // 当前按下的键
    private Set<Integer> justPressedKeys;   // 本帧刚按下的键
    private Map<Integer, Boolean> keyStates;    // 键状态映射
    private Vector2 mousePosition;          // 鼠标位置
    private boolean[] mouseButtons;         // 鼠标按钮状态
    private boolean[] mouseButtonsJustPressed;  // 本帧刚按下的鼠标按钮
    
    private InputManager() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
        keyStates = new HashMap<>();
        mousePosition = new Vector2();
        mouseButtons = new boolean[3];
        mouseButtonsJustPressed = new boolean[3];
    }
    
    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    /**
     * 每帧更新：清除瞬时状态
     */
    public void update() {
        justPressedKeys.clear();
        for (int i = 0; i < mouseButtonsJustPressed.length; i++) {
            mouseButtonsJustPressed[i] = false;
        }
    }
    
    public void onKeyPressed(int keyCode) {
        if (!pressedKeys.contains(keyCode)) {
            justPressedKeys.add(keyCode);
        }
        pressedKeys.add(keyCode);
        keyStates.put(keyCode, true);
    }
    
    public void onKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        keyStates.put(keyCode, false);
    }
    
    public void onMouseMoved(float x, float y) {
        mousePosition.x = x;
        mousePosition.y = y;
    }
    
    public void onMousePressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            if (!mouseButtons[button]) {
                mouseButtonsJustPressed[button] = true;
            }
            mouseButtons[button] = true;
        }
    }
    
    public void onMouseReleased(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = false;
        }
    }
    
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }
    
    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }
    
    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtonsJustPressed[button];
        }
        return false;
    }
    
    public boolean isAnyKeyJustPressed() {
        return !justPressedKeys.isEmpty();
    }
    
    public boolean isAnyKeyPressed() {
        return !pressedKeys.isEmpty();
    }

    public java.util.Set<Integer> getJustPressedKeysSnapshot() {
        return new java.util.HashSet<>(justPressedKeys);
    }
    
    public Vector2 getMousePosition() {
        return new Vector2(mousePosition);
    }
    
    public float getMouseX() {
        return mousePosition.x;
    }
    
    public float getMouseY() {
        return mousePosition.y;
    }
}
