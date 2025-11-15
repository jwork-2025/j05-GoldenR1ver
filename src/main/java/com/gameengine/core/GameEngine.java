package com.gameengine.core;

import com.gameengine.graphics.IRenderer;
import com.gameengine.graphics.RenderBackend;
import com.gameengine.graphics.RendererFactory;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

/**
 * 游戏引擎核心类：管理游戏主循环、场景切换、系统更新和渲染。
 * 协调所有子系统（物理、输入、渲染）的运行。
 */
public class GameEngine {
    private IRenderer renderer;
    private InputManager inputManager;
    private Scene currentScene;
    private PhysicsSystem physicsSystem;
    private boolean running;
    private float targetFPS;    // 目标帧率
    private float deltaTime;
    private long lastTime;
    @SuppressWarnings("unused")
    private String title;   // 窗口标题
    // 新录制服务（可选）
    private com.gameengine.recording.RecordingService recordingService;

    // 构造函数：默认使用GPU后端
    public GameEngine(int width, int height, String title) {
        this(width, height, title, RenderBackend.GPU);
    }

    // 主构造函数：初始化所有核心组件
    public GameEngine(int width, int height, String title, RenderBackend backend) {
        this.title = title;
        this.renderer = RendererFactory.createRenderer(backend, width, height, title);
        this.inputManager = InputManager.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();
        
    }

    // 初始化引擎（待重写添加自定义逻辑）
    public boolean initialize() {
        return true;
    }

    // 启动游戏主循环
    public void run() {
        if (!initialize()) {
            System.err.println("游戏引擎初始化失败");
            return;
        }
        
        running = true;

        // 初始化当前场景和物理系统
        if (currentScene != null) {
            currentScene.initialize();
            // 菜单场景不加载物理系统
            if (currentScene.getName().equals("MainMenu")) {
                physicsSystem = null;
            } else {
                physicsSystem = new PhysicsSystem(currentScene, renderer.getWidth(), renderer.getHeight());
            }
            
        }

        // 主循环定时控制
        long lastFrameTime = System.nanoTime();
        long frameTimeNanos = (long)(1_000_000_000.0 / targetFPS);
        
        while (running) {
            long currentTime = System.nanoTime();

            // 基于目标FPS进行状态更新
            if (currentTime - lastFrameTime >= frameTimeNanos) {
                update();
                if (running) {
                    render();
                }
                lastFrameTime = currentTime;
            }
            
            renderer.pollEvents();

            // 检查窗口关闭请求
            if (renderer.shouldClose()) {
                running = false;
            }

            // 让出CPU，避免过度占用
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * 更新游戏状态：处理输入、更新场景、物理系统和录制服务
     */
    private void update() {
        // 计算帧间隔时间
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f;
        lastTime = currentTime;
        
        renderer.pollEvents();
        
        
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }

        // 更新物理系统
        if (physicsSystem != null) {
            physicsSystem.update(deltaTime);
        }

        // 更新录制服务
        if (recordingService != null && recordingService.isRecording()) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        inputManager.update();

        // ESC 键退出
        if (inputManager.isKeyPressed(27)) {
            running = false;
            cleanup();
        }

        // 关闭窗口退出
        if (renderer.shouldClose() && running) {
            running = false;
            cleanup();
        }
    }

    /**
     * 渲染当前帧
     */
    private void render() {
        if (renderer == null) return;
        
        renderer.beginFrame();
        
        if (currentScene != null) {
            currentScene.render();
        }
        
        renderer.endFrame();
    }

    /**
     * 切换场景：清理旧场景，初始化新场景
     */
    public void setScene(Scene scene) {
        if (currentScene != null) {
            if (physicsSystem != null) {
                physicsSystem.cleanup();
                physicsSystem = null;
            }
            currentScene.clear();
        }
        this.currentScene = scene;
        if (scene != null) {
            // J03: 从scene出发的通信
            scene.setEngine(this);
            if (running) {
                scene.initialize();
                if (!scene.getName().equals("MainMenu") && !scene.getName().equals("Replay")) {
                    physicsSystem = new PhysicsSystem(scene, renderer.getWidth(), renderer.getHeight());
                }
            }
        }
    }
    
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    public void stop() {
        running = false;
    }

    /**
     * 清理资源：停止录制、清理物理系统、场景和渲染器
     */
    public void cleanup() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        if (physicsSystem != null) {
            physicsSystem.cleanup();
        }
        if (currentScene != null) {
            currentScene.clear();
        }
        renderer.cleanup();
    }


    /**
     * 启用录制服务
     */
    public void enableRecording(com.gameengine.recording.RecordingService service) {
        this.recordingService = service;
        try {
            if (service != null && currentScene != null) {
                service.start(currentScene, renderer.getWidth(), renderer.getHeight());
            }
        } catch (Exception e) {
            System.err.println("录制启动失败: " + e.getMessage());
        }
    }

    /**
     * 禁用录制服务
     */
    public void disableRecording() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        recordingService = null;
    }
    
    
    
    public IRenderer getRenderer() {
        return renderer;
    }
    
    public InputManager getInputManager() {
        return inputManager;
    }
    
    public float getDeltaTime() {
        return deltaTime;
    }
    
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
    }
    
    public float getTargetFPS() {
        return targetFPS;
    }
    
    public boolean isRunning() {
        return running;
    }
}
