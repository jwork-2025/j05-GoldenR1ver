package com.gameengine.example;

import com.gameengine.config.GameConfig;
import com.gameengine.core.GameEngine;
import com.gameengine.graphics.RenderBackend;

/**
 * 游戏入口点：包含main方法，启动游戏引擎。
 * 负责初始化引擎和设置初始场景。
 */
public class Game {
    /**
     * 游戏主入口方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");

        GameEngine engine = null;
        try {
            // 创建游戏引擎实例（1024x768分辨率）
            System.out.println("使用渲染后端: GPU");
            engine = new GameEngine(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, "游戏引擎", RenderBackend.GPU);


            // 创建并设置菜单场景
            MenuScene menuScene = new MenuScene(engine, "MainMenu");
            engine.setScene(menuScene);

            // 启动游戏主循环
            engine.run();
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
        }

        System.out.println("游戏结束");
    }
}


