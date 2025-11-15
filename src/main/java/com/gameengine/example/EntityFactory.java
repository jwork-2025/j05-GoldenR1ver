package com.gameengine.example;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;

/**
 * 实体工厂：创建预定义的游戏对象。
 * 使用工厂模式封装对象创建逻辑，便于统一管理。
 */
public final class EntityFactory {
    // 私有构造函数防止实例化
    private EntityFactory() {}

    /**
     * 创建玩家视觉对象：使用自定义渲染绘制复杂形状
     * @param renderer 渲染器实例
     * @return 玩家游戏对象
     */
    public static GameObject createPlayerVisual(IRenderer renderer) {
        return new GameObject("Player") {
            private Vector2 basePosition;
            @Override
            public void update(float dt) {
                super.update(dt);
                // 缓存当前位置用于渲染
                TransformComponent tc = getComponent(TransformComponent.class);
                if (tc != null) basePosition = tc.getPosition();
            }
            @Override
            public void render() {
                if (basePosition == null) return;
                // 绘制玩家角色：身体、头部、左右手臂
                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        };
    }

    /**
     * 创建AI玩家视觉对象：使用RenderComponent进行标准渲染
     * @param renderer 渲染器实例
     * @param w 宽度
     * @param h 高度
     * @param r 红色分量
     * @param g 绿色分量
     * @param b 蓝色分量
     * @param a 透明度
     * @return AI玩家游戏对象
     */
    public static GameObject createAIVisual(IRenderer renderer, float w, float h, float r, float g, float b, float a) {
        GameObject obj = new GameObject("AIPlayer");
        TransformComponent tc = obj.addComponent(new TransformComponent(new Vector2(0, 0)));
        RenderComponent rc = obj.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(Math.max(1, w), Math.max(1, h)),
            new RenderComponent.Color(r, g, b, a)
        ));
        rc.setRenderer(renderer);
        return obj;
    }
}


