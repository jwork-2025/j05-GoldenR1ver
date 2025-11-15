package com.gameengine.scene;

import com.gameengine.components.HealthComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.SkillComponent;
import com.gameengine.config.GameConfig;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.core.Component;
import com.gameengine.graphics.IRenderer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 场景管理类：管理所有游戏对象（GameObject）的生命周期。
 * 处理对象的添加、移除、更新和渲染。
 */
public class Scene {
    private String name;
    private List<GameObject> gameObjects;   // 当前游戏对象
    private List<GameObject> objectsToAdd;  // 待添加对象
    private List<GameObject> objectsToRemove;   // 待删除对象
    private boolean initialized;    // 场景是否初始化

    // J03: 从Scene出发通信
    private GameEngine engine;
    private GameLogic gameLogic;
    public void setEngine(GameEngine engine) {
        this.engine = engine;
        if (engine != null) {
            this.gameLogic = new GameLogic(this);
        }
    }
    public GameEngine getEngine() {return engine;}
    public IRenderer getRenderer() {return engine != null ? engine.getRenderer() : null;}
    public GameLogic getGameLogic() {return gameLogic;}


    public Scene(String name) {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.objectsToAdd = new ArrayList<>();
        this.objectsToRemove = new ArrayList<>();
        this.initialized = false;
    }

    // 初始化所有对象
    public void initialize() {
        for (GameObject obj : gameObjects) {
            obj.initialize();
        }
        initialized = true;
    }

    // 更新场景：处理增删和调用每个对象的update
    public void update(float deltaTime) {
        // 添加
        for (GameObject obj : objectsToAdd) {
            gameObjects.add(obj);
            if (initialized) {
                obj.initialize();
            }
        }
        objectsToAdd.clear();

        // 删除
        for (GameObject obj : objectsToRemove) {
            gameObjects.remove(obj);
        }
        objectsToRemove.clear();

        // 更新
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isActive()) {
                obj.update(deltaTime);
            } else {
                iterator.remove();
            }
        }
    }

    // J03: 丰富游戏逻辑: UI
    public void renderUI() {
        IRenderer renderer = getRenderer();
        if (renderer == null) return;
        // 渲染玩家血条
        renderPlayerHealthBar();
        // 渲染玩家技能
        renderSkillBar();
    }

    // J03: 丰富游戏逻辑: 技能UI
    private void renderSkillBar(){
        IRenderer renderer = getRenderer();
        if(renderer == null) return;

        GameObject player = findGameObjectsByComponent(com.gameengine.components.TransformComponent.class)
                .stream().findFirst().orElse(null);
        if (player == null) return;

        SkillComponent skill = player.getComponent(SkillComponent.class);
        if(skill == null)return;

        // 技能栏背景
        int skillBarWidth = 3 * GameConfig.SKILL_ICON_SPACING;
        int skillBarX = GameConfig.SKILL_BAR_CENTER_X - skillBarWidth / 2;

        renderer.drawRect(
                skillBarX - 10, GameConfig.SKILL_BAR_Y - 10,
                skillBarWidth + 20, GameConfig.SKILL_ICON_SIZE + 20,
                0.2f, 0.2f, 0.3f, 0.8f
        );

        // 渲染三个技能图标
        SkillComponent.SkillType[] skillTypes = {
                SkillComponent.SkillType.MELEE_ATTACK,
                SkillComponent.SkillType.RANGED_ATTACK,
                SkillComponent.SkillType.DODGE
        };

        String[] skillKeys = {"Z", "X", "C"};
        RenderComponent.Color[] skillColors = {
                new RenderComponent.Color(1.0f, 0.0f, 0.0f, 1.0f), // 红色 - 近战
                new RenderComponent.Color(0.0f, 1.0f, 1.0f, 1.0f), // 青色 - 远程
                new RenderComponent.Color(0.0f, 1.0f, 0.0f, 1.0f)  // 绿色 - 闪避
        };

        for (int i = 0; i < skillTypes.length; i++) {
            int iconX = skillBarX + i * GameConfig.SKILL_ICON_SPACING;

            // 技能图标背景
            RenderComponent.Color bgColor = skill.isSkillReady(skillTypes[i]) ?
                    new RenderComponent.Color(0.3f, 0.3f, 0.5f, 1.0f) :
                    new RenderComponent.Color(0.5f, 0.2f, 0.2f, 1.0f);

            renderer.drawRect(
                    iconX, GameConfig.SKILL_BAR_Y,
                    GameConfig.SKILL_ICON_SIZE, GameConfig.SKILL_ICON_SIZE,
                    bgColor.r, bgColor.g, bgColor.b, bgColor.a
            );

            // 技能图标
            renderer.drawRect(
                    iconX + 10, GameConfig.SKILL_BAR_Y + 10,
                    GameConfig.SKILL_ICON_SIZE - 20, GameConfig.SKILL_ICON_SIZE - 20,
                    skillColors[i].r, skillColors[i].g, skillColors[i].b, skillColors[i].a
            );

            // 冷却时间显示
            float cooldown = skill.getSkillCooldown(skillTypes[i]);
            if (cooldown > 0) {
                float totalCooldown = skill.getSkillTotalCooldown(skillTypes[i]);
                float progress = cooldown / totalCooldown;

                // 冷却覆盖层
                renderer.drawRect(
                        iconX, GameConfig.SKILL_BAR_Y,
                        GameConfig.SKILL_ICON_SIZE, GameConfig.SKILL_ICON_SIZE * progress,
                        0.0f, 0.0f, 0.0f, 0.7f
                );

                // 冷却时间文字
                String cooldownText = String.format("%.1f", cooldown);
                renderer.drawText(
                        iconX + GameConfig.SKILL_ICON_SIZE / 2 - 15,
                        GameConfig.SKILL_BAR_Y + GameConfig.SKILL_ICON_SIZE / 2 - 8,
                        cooldownText, 1.0f, 1.0f, 1.0f, 1.0f
                );
            }

            // 技能键位文字
            renderer.drawText(
                    iconX + 5, GameConfig.SKILL_BAR_Y + 5,
                    skillKeys[i], 1.0f, 1.0f, 1.0f, 1.0f
            );
        }

        //渲染魔力条
        float manaPercent = skill.getManaPercentage();

        // 魔力条背景
        int barWidth = 200;
        int barHeight = 20;
        int barX = 20; // 左侧位置
        int barY = 20;

        renderer.drawRect(
                barX, barY, barWidth, barHeight,
                0.3f, 0.3f, 0.3f, 0.8f
        );

        // 当前魔力
        renderer.drawRect(
                barX, barY, barWidth * manaPercent, barHeight,
                0.0f, 0.5f, 1.0f, 0.8f // 蓝色
        );

        // 魔力条边框
        renderer.drawRect(
                barX, barY, barWidth, barHeight,
                1.0f, 1.0f, 1.0f, 0.3f
        );

        // 魔力文字
        String manaText = String.format("MP: %.0f/%.0f", skill.getCurrentMana(), skill.getMaxMana());
        renderer.drawText(
                barX + 10, barY + 15,
                manaText,
                1.0f, 1.0f, 1.0f, 1.0f
        );
    }




    // J03: 丰富游戏逻辑: 玩家血量UI
    private void renderPlayerHealthBar() {
        IRenderer renderer = getRenderer();
        if (renderer == null) return;

        // 查找玩家
        GameObject player = findGameObjectsByComponent(com.gameengine.components.TransformComponent.class)
                .stream().findFirst().orElse(null);
        if (player == null) return;

        HealthComponent health = player.getComponent(HealthComponent.class);
        if (health == null) return;

        float healthPercent = health.getHealthPercentage();

        // 血条背景
        int barWidth = 200;
        int barHeight = 20;
        int barX = GameConfig.WINDOW_WIDTH - barWidth - 20;
        int barY = 20;

        renderer.drawRect(
                barX, barY, barWidth, barHeight,
                0.3f, 0.3f, 0.3f, 0.8f
        );

        // 当前血量
        if (healthPercent > 0.6f) {
            renderer.drawRect(
                    barX, barY, barWidth * healthPercent, barHeight,
                    0.0f, 1.0f, 0.0f, 0.8f // 绿色
            );
        } else if (healthPercent > 0.3f) {
            renderer.drawRect(
                    barX, barY, barWidth * healthPercent, barHeight,
                    1.0f, 1.0f, 0.0f, 0.8f // 黄色
            );
        } else {
            renderer.drawRect(
                    barX, barY, barWidth * healthPercent, barHeight,
                    1.0f, 0.0f, 0.0f, 0.8f // 红色
            );
        }

        // 血条边框
        renderer.drawRect(
                barX, barY, barWidth, barHeight,
                1.0f, 1.0f, 1.0f, 0.3f
        );

        // 血量文字
        String healthText = String.format("HP: %.0f/%.0f", health.getCurrentHealth(), health.getMaxHealth());
        renderer.drawText(
                barX + 10, barY + 15,
                healthText,
                1.0f, 1.0f, 1.0f, 1.0f
        );

        // 无敌状态指示
        if (health.isInvulnerable()) {
            float invulTime = health.getInvulnerableTimeRemaining();
            String invulText = String.format("无敌: %.1fs", invulTime);
            renderer.drawText(
                    barX, barY + 25,
                    invulText,
                    0.5f, 0.5f, 1.0f, 1.0f
            );
        }
    }

    // 渲染所有活跃对象
    public void render() {
        for (GameObject obj : gameObjects) {
            if (obj.isActive()) {
                obj.render();
            }
        }
    }

    // 添加对象（下一帧）
    public void addGameObject(GameObject gameObject) {
        objectsToAdd.add(gameObject);
    }

    // 删除对象
    public void removeGameObject(GameObject gameObject){
        if(!gameObjects.contains(gameObject))return;
        objectsToRemove.add(gameObject);
    }
    // 查找游戏对象
    public <T extends Component<T>> List<GameObject> findGameObjectsByComponent(Class<T> componentType) {
        return gameObjects.stream()
            .filter(obj -> obj.hasComponent(componentType))
            .collect(Collectors.toList());
    }

    // 获取所有指定组件
    public <T extends Component<T>> List<T> getComponents(Class<T> componentType) {
        return findGameObjectsByComponent(componentType).stream()
            .map(obj -> obj.getComponent(componentType))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    public void clear() {
        gameObjects.clear();
        objectsToAdd.clear();
        objectsToRemove.clear();
    }
    
    public String getName() {
        return name;
    }
    
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }
}
