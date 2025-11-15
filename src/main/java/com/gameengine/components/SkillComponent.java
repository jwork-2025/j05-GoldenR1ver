package com.gameengine.components;

import com.gameengine.config.GameConfig;
import com.gameengine.core.Component;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.core.ParticleSystem;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import java.util.*;

/**
 * 技能组件，管理玩家的技能系统
 * 实现内容：魔力机制、蓄力攻击、技能包装
 * 支持近战攻击、远程攻击、闪避三种技能
 */
public class SkillComponent extends Component<SkillComponent> {

    // 技能类型枚举
    public enum SkillType {
        MELEE_ATTACK,   // 近战攻击 (Z键)
        RANGED_ATTACK,  // 远程攻击 (X键)
        DODGE           // 闪避 (C键)
    }

    // 技能状态枚举
    public enum SkillState {
        READY,          // 就绪
        CHARGING,       // 蓄力中
        ACTIVE,         // 激活中
        COOLDOWN        // 冷却中
    }

    // 技能基础配置
    public static class SkillConfig {
        public SkillType type;
        public float damage;           // 基础伤害
        public float range;            // 射程/范围
        public float bulletSpeed;      // 子弹速度
        public float cooldown;         // 基础冷却时间
        public float manaCost;         // 魔力消耗
        public boolean chargeable;     // 是否可蓄力
        public float maxChargeTime;    // 最大蓄力时间
        public float chargeDamageMultiplier; // 蓄力伤害倍率
        public float chargeSpeedMultiplier;  // 蓄力速度倍率

        public SkillConfig(SkillType type, float damage, float range, float bulletSpeed,
                           float cooldown, float manaCost, boolean chargeable,
                           float maxChargeTime, float chargeDamageMultiplier, float chargeSpeedMultiplier) {
            this.type = type;
            this.damage = damage;
            this.range = range;
            this.bulletSpeed = bulletSpeed;
            this.cooldown = cooldown;
            this.manaCost = manaCost;
            this.chargeable = chargeable;
            this.maxChargeTime = maxChargeTime;
            this.chargeDamageMultiplier = chargeDamageMultiplier;
            this.chargeSpeedMultiplier = chargeSpeedMultiplier;
        }
    }

    // 技能实例状态
    public static class SkillInstance {
        public SkillConfig config;
        public SkillState state;
        public float currentCooldown;
        public float chargeTime;
        public float activeTime;
        public GameObject effectObject;
        public ParticleSystem particleSystem;

        public SkillInstance(SkillConfig config) {
            this.config = config;
            this.state = SkillState.READY;
            this.currentCooldown = 0;
            this.chargeTime = 0;
            this.activeTime = 0;
        }
    }

    // 魔力系统
    private float maxMana;
    private float currentMana;
    private float manaRegenRate;
    private float timeSinceLastSkill;
    private float manaRegenDelay;

    // 技能管理
    private Map<SkillType, SkillInstance> skills;
    private Scene scene;
    private InputManager inputManager;

    // 闪避状态
    private boolean isDodging;
    private float dodgeEndTime;
    private Vector2 originalVelocity; // 修复：保存原始速度向量而非标量

    // 近战攻击状态
    private float meleeAttackTimer;
    private List<GameObject> meleeHitEnemies;
    private ParticleSystem meleeParticleSystem;

    // 子弹管理
    private List<GameObject> activeBullets;

    // 基础技能配置
    private static final Map<SkillType, SkillConfig> BASE_SKILL_CONFIGS = new HashMap<>();
    static {
        // 近战攻击：可长按持续攻击，伤害频率受子弹速度影响 - 使用GameConfig常量
        BASE_SKILL_CONFIGS.put(SkillType.MELEE_ATTACK,
                new SkillConfig(SkillType.MELEE_ATTACK,
                        GameConfig.MELEE_DAMAGE,
                        GameConfig.MELEE_RANGE,
                        GameConfig.MELEE_BULLET_SPEED,
                        GameConfig.MELEE_COOLDOWN,
                        GameConfig.MELEE_MANA_COST,
                        GameConfig.MELEE_CHARGEABLE,
                        GameConfig.MELEE_MAX_CHARGE_TIME,
                        GameConfig.MELEE_CHARGE_DAMAGE_MULTIPLIER,
                        GameConfig.MELEE_CHARGE_SPEED_MULTIPLIER));

        // 远程攻击：可蓄力，增加子弹速度和伤害 - 使用GameConfig常量
        BASE_SKILL_CONFIGS.put(SkillType.RANGED_ATTACK,
                new SkillConfig(SkillType.RANGED_ATTACK,
                        GameConfig.RANGED_DAMAGE,
                        GameConfig.RANGED_RANGE,
                        GameConfig.RANGED_BULLET_SPEED,
                        GameConfig.RANGED_COOLDOWN,
                        GameConfig.RANGED_MANA_COST,
                        GameConfig.RANGED_CHARGEABLE,
                        GameConfig.RANGED_MAX_CHARGE_TIME,
                        GameConfig.RANGED_CHARGE_DAMAGE_MULTIPLIER,
                        GameConfig.RANGED_CHARGE_SPEED_MULTIPLIER));

        // 闪避：属性影响效果 - 使用GameConfig常量
        BASE_SKILL_CONFIGS.put(SkillType.DODGE,
                new SkillConfig(SkillType.DODGE, 0, 0, 0,
                        GameConfig.DODGE_COOLDOWN,
                        GameConfig.DODGE_MANA_COST,
                        false, 0, 1.0f, 1.0f));
    }

    public SkillComponent() {
        this.skills = new HashMap<>();
        this.maxMana = GameConfig.PLAYER_MAX_MANA;
        this.currentMana = maxMana;
        this.manaRegenRate = GameConfig.MANA_REGEN_RATE;
        this.manaRegenDelay = GameConfig.MANA_REGEN_DELAY;
        this.timeSinceLastSkill = 0;
        this.isDodging = false;
        this.meleeHitEnemies = new ArrayList<>();
        this.activeBullets = new ArrayList<>();
        this.inputManager = InputManager.getInstance();
        this.originalVelocity = new Vector2(0, 0);
    }

    @Override
    public void initialize() {
        // 初始化所有技能
        for (SkillType type : SkillType.values()) {
            SkillConfig baseConfig = BASE_SKILL_CONFIGS.get(type);
            if (baseConfig != null) {
                skills.put(type, new SkillInstance(new SkillConfig(
                        baseConfig.type, baseConfig.damage, baseConfig.range,
                        baseConfig.bulletSpeed, baseConfig.cooldown, baseConfig.manaCost,
                        baseConfig.chargeable, baseConfig.maxChargeTime,
                        baseConfig.chargeDamageMultiplier, baseConfig.chargeSpeedMultiplier
                )));
            }
        }

        // 获取场景引用
        if (owner != null) {
            this.scene = owner.getScene();
        }

        // 初始化近战粒子系统
        if (scene != null && scene.getEngine() != null) {
            TransformComponent transform = owner.getComponent(TransformComponent.class);
            Vector2 startPos = transform != null ? transform.getPosition() : new Vector2(0, 0);
            meleeParticleSystem = createMeleeParticleSystem(startPos);
        }
    }

    @Override
    public void update(float deltaTime) {
        // 自动获取Scene
        if(scene==null){
            if(this.owner != null){
                scene = owner.getScene();
                System.out.println("已自动分配Scene：" + scene.getName());
            }
            else {
                System.out.println("无主组件：" + this.getName());
            }
        }
        // 更新魔力系统
        updateMana(deltaTime);

        // 更新技能状态
        for (SkillInstance skill : skills.values()) {
            updateSkillState(skill, deltaTime);
        }

        // 处理输入
        handleInput(deltaTime);

        // 更新闪避状态
        if (isDodging && System.currentTimeMillis() / 1000.0f >= dodgeEndTime) {
            endDodge();
        }

        // 更新近战攻击
        updateMeleeAttack(deltaTime);

        // 更新近战粒子系统位置
        if (meleeParticleSystem != null) {
            TransformComponent transform = owner.getComponent(TransformComponent.class);
            if (transform != null) {
                meleeParticleSystem.setPosition(transform.getPosition());
            }
            meleeParticleSystem.update(deltaTime);
        }

        // 更新子弹
        updateBullets(deltaTime);
    }

    @Override
    public void render() {
        // 渲染近战粒子系统
        if (meleeParticleSystem != null) {
            meleeParticleSystem.render();
        }
    }

    public void setScene(Scene scene){
        if(this.scene == null)
            this.scene = scene;
    }

    /**
     * 创建近战粒子系统
     */
    private ParticleSystem createMeleeParticleSystem(Vector2 position) {
        ParticleSystem.Config config = new ParticleSystem.Config();
        // 使用GameConfig常量配置粒子系统
        config.spawnRate = GameConfig.MELEE_PARTICLE_SPAWN_RATE;
        config.initialCount = 0;
        config.speedMin = GameConfig.MELEE_PARTICLE_SPEED_MIN;
        config.speedMax = GameConfig.MELEE_PARTICLE_SPEED_MAX;
        config.lifeMin = GameConfig.MELEE_PARTICLE_LIFE_MIN;
        config.lifeMax = GameConfig.MELEE_PARTICLE_LIFE_MAX;
        config.sizeMin = GameConfig.MELEE_PARTICLE_SIZE_MIN;
        config.sizeMax = GameConfig.MELEE_PARTICLE_SIZE_MAX;
        config.r = GameConfig.MELEE_PARTICLE_R;
        config.g = GameConfig.MELEE_PARTICLE_G;
        config.b = GameConfig.MELEE_PARTICLE_B;
        config.opacityMultiplier = GameConfig.MELEE_PARTICLE_OPACITY;
        config.minRenderSize = 1.0f;

        return new ParticleSystem(scene.getEngine().getRenderer(), position, config);
    }

    /**
     * 更新魔力系统
     */
    private void updateMana(float deltaTime) {
        timeSinceLastSkill += deltaTime;

        // 一段时间不使用技能后开始快速回复
        if (timeSinceLastSkill >= manaRegenDelay && currentMana < maxMana) {
            currentMana = Math.min(maxMana, currentMana + manaRegenRate * deltaTime);
        }
    }

    /**
     * 更新技能状态
     */
    private void updateSkillState(SkillInstance skill, float deltaTime) {
        switch (skill.state) {
            case COOLDOWN:
                skill.currentCooldown -= deltaTime;
                if (skill.currentCooldown <= 0) {
                    skill.state = SkillState.READY;
                    skill.currentCooldown = 0;
                }
                break;

            case CHARGING:
                if (skill.config.chargeable) {
                    skill.chargeTime += deltaTime;
                    if (skill.chargeTime >= skill.config.maxChargeTime) {
                        skill.chargeTime = skill.config.maxChargeTime;
                        // 自动释放满蓄力技能
                        releaseSkill(skill.config.type);
                    }
                }
                break;

            case ACTIVE:
                skill.activeTime += deltaTime;
                // 根据技能类型处理激活状态
                handleActiveSkill(skill, deltaTime);
                break;
        }
    }

    /**
     * 处理输入
     */
    private void handleInput(float deltaTime) {
        // 近战攻击 (Z键)
        if (inputManager.isKeyPressed(90)) { // Z键
            startSkill(SkillType.MELEE_ATTACK);
        } else {
            SkillInstance meleeSkill = skills.get(SkillType.MELEE_ATTACK);
            if (meleeSkill != null && meleeSkill.state == SkillState.ACTIVE) {
                endSkill(SkillType.MELEE_ATTACK);
            }
        }

        // 远程攻击 (X键)
        if (inputManager.isKeyPressed(88)) { // X键
            startSkill(SkillType.RANGED_ATTACK);
        } else {
            SkillInstance rangedSkill = skills.get(SkillType.RANGED_ATTACK);
            if (rangedSkill != null && rangedSkill.state == SkillState.CHARGING) {
                releaseSkill(SkillType.RANGED_ATTACK);
            }
        }

        // 闪避 (C键)
        if (inputManager.isKeyPressed(67)) { // C键
            useSkill(SkillType.DODGE);
        }
    }

    /**
     * 开始技能（蓄力或持续攻击）
     */
    public boolean startSkill(SkillType type) {
        SkillInstance skill = skills.get(type);
        if (skill == null || skill.state != SkillState.READY || currentMana < skill.config.manaCost) {
            return false;
        }

        if (skill.config.chargeable) {
            // 开始蓄力
            skill.state = SkillState.CHARGING;
            skill.chargeTime = 0;
        } else {
            // 立即使用技能
            return useSkill(type);
        }

        return true;
    }

    /**
     * 释放蓄力技能
     */
    public boolean releaseSkill(SkillType type) {
        SkillInstance skill = skills.get(type);
        if (skill == null || skill.state != SkillState.CHARGING) {
            return false;
        }

        // 消耗魔力
        if (!consumeMana(skill.config.manaCost)) {
            skill.state = SkillState.READY;
            return false;
        }

        // 计算蓄力倍率
        float chargeRatio = skill.chargeTime / skill.config.maxChargeTime;
        float damageMultiplier = 1.0f + (skill.config.chargeDamageMultiplier - 1.0f) * chargeRatio;
        float speedMultiplier = 1.0f + (skill.config.chargeSpeedMultiplier - 1.0f) * chargeRatio;

        // 设置冷却
        skill.currentCooldown = skill.config.cooldown;
        skill.state = SkillState.ACTIVE;
        skill.activeTime = 0;

        // 执行技能
        switch (type) {
            case RANGED_ATTACK:
                createRangedAttack(skill, damageMultiplier, speedMultiplier);
                break;
        }

        // 重置蓄力时间
        skill.chargeTime = 0;
        return true;
    }

    /**
     * 立即使用技能（非蓄力）
     */
    public boolean useSkill(SkillType type) {
        SkillInstance skill = skills.get(type);
        if (skill == null || skill.state != SkillState.READY || currentMana < skill.config.manaCost) {
            return false;
        }

        // 消耗魔力
        if (!consumeMana(skill.config.manaCost)) {
            return false;
        }

        // 设置冷却
        skill.currentCooldown = skill.config.cooldown;
        skill.state = SkillState.ACTIVE;
        skill.activeTime = 0;

        switch (type) {
            case MELEE_ATTACK:
                startMeleeAttack(skill);
                break;
            case DODGE:
                activateDodge(skill);
                break;
        }

        return true;
    }

    /**
     * 结束技能
     */
    public void endSkill(SkillType type) {
        SkillInstance skill = skills.get(type);
        if (skill != null && skill.state == SkillState.ACTIVE) {
            skill.state = SkillState.COOLDOWN;
            skill.activeTime = 0;

            if (skill.effectObject != null) {
                skill.effectObject.destroy();
                skill.effectObject = null;
            }

            // 清理近战攻击状态
            if (type == SkillType.MELEE_ATTACK) {
                meleeHitEnemies.clear();
                meleeAttackTimer = 0;
                if (meleeParticleSystem != null) {
                    meleeParticleSystem.setSpawnRate(0); // 停止生成粒子
                }
            }
        }
    }

    /**
     * 消耗魔力
     */
    private boolean consumeMana(float amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            timeSinceLastSkill = 0; // 重置不使用技能计时器
            return true;
        }
        return false;
    }

    /**
     * 开始近战攻击
     */
    private void startMeleeAttack(SkillInstance skill) {
        meleeHitEnemies.clear();
        meleeAttackTimer = 0;

        // 启动粒子系统
        if (meleeParticleSystem != null) {
            meleeParticleSystem.setSpawnRate(0.05f); // 高频率生成粒子
        }
    }

    /**
     * 更新近战攻击
     */
    private void updateMeleeAttack(float deltaTime) {
        SkillInstance meleeSkill = skills.get(SkillType.MELEE_ATTACK);
        if (meleeSkill == null || meleeSkill.state != SkillState.ACTIVE) {
            return;
        }

        meleeAttackTimer += deltaTime;

        // 根据子弹速度计算攻击频率
        float attackInterval = 1.0f / meleeSkill.config.bulletSpeed;
        if (meleeAttackTimer >= attackInterval) {
            performMeleeAttack(meleeSkill);
            meleeAttackTimer = 0;
        }

        // 在近战攻击期间持续生成扇形方向的粒子
        if (meleeParticleSystem != null) {
            generateMeleeParticles(meleeSkill);
        }
    }

    /**
     * 生成近战攻击粒子 - 修复：以玩家为中心向前方扇形范围喷射大量粒子
     */
    private void generateMeleeParticles(SkillInstance skill) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null || meleeParticleSystem == null) return;

        Vector2 playerPos = transform.getPosition();
        Vector2 mousePos = inputManager.getMousePosition();
        Vector2 direction = mousePos.subtract(playerPos).normalize();

        // 使用GameConfig常量 - 扇形角度范围
        float angleRange = GameConfig.MELEE_ANGLE_RANGE;
        int particlesPerFrame = GameConfig.MELEE_PARTICLES_PER_FRAME * 3; // 增加粒子数量

        Random random = new Random();

        for (int i = 0; i < particlesPerFrame; i++) {
            // 在扇形范围内随机角度
            float randomAngle = (random.nextFloat() - 0.5f) * angleRange;
            double angleRad = Math.toRadians(Math.atan2(direction.y, direction.x) + randomAngle);

            // 计算粒子方向
            Vector2 particleDir = new Vector2(
                    (float) Math.cos(angleRad),
                    (float) Math.sin(angleRad)
            );

            // 随机速度
            float speed = GameConfig.MELEE_PARTICLE_SPEED_MIN + random.nextFloat() *
                    (GameConfig.MELEE_PARTICLE_SPEED_MAX - GameConfig.MELEE_PARTICLE_SPEED_MIN);
            Vector2 velocity = particleDir.multiply(speed);

            // 修复：粒子从玩家中心位置生成
            Vector2 spawnPos = new Vector2(playerPos.x, playerPos.y);

            // 创建粒子（通过burst方式）
            meleeParticleSystem.setPosition(spawnPos);

            // 设置粒子方向
            // meleeParticleSystem.setParticleDirection(particleDir);
            meleeParticleSystem.burst(10);
        }
    }

    /**
     * 执行近战攻击
     */
    private void performMeleeAttack(SkillInstance skill) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        Vector2 playerPos = transform.getPosition();

        // 获取鼠标方向
        Vector2 mousePos = inputManager.getMousePosition();
        Vector2 direction = mousePos.subtract(playerPos).normalize();

        // 使用GameConfig常量 - 扇形范围检测
        float angleRange = GameConfig.MELEE_ANGLE_RANGE;
        float distance = skill.config.range;

        for (GameObject enemy : scene.getGameObjects()) {
            if (enemy.getName().equals("AIPlayer") && !meleeHitEnemies.contains(enemy)) {
                TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                if (enemyTransform != null) {
                    Vector2 enemyPos = enemyTransform.getPosition();
                    Vector2 toEnemy = enemyPos.subtract(playerPos);
                    float enemyDistance = toEnemy.magnitude();

                    if (enemyDistance <= distance) {
                        // 计算角度
                        float dot = direction.dot(toEnemy.normalize());
                        float angle = (float) Math.toDegrees(Math.acos(dot));

                        if (angle <= angleRange / 2) {
                            // 在扇形范围内，造成伤害
                            applyDamage(enemy, skill.config.damage);
                            meleeHitEnemies.add(enemy);

                            // 创建命中效果
                            createHitEffect(enemyPos, 1.0f, 0.5f, 0.0f); // 橙色
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建远程攻击 - 修复：改进子弹渲染和添加尾迹粒子
     */
    private void createRangedAttack(SkillInstance skill, float damageMultiplier, float speedMultiplier) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        Vector2 playerPos = transform.getPosition();
        Vector2 mousePos = inputManager.getMousePosition();
        Vector2 direction = mousePos.subtract(playerPos).normalize();

        // 计算实际伤害和速度
        float actualDamage = skill.config.damage * damageMultiplier;
        float actualSpeed = skill.config.bulletSpeed * speedMultiplier;

        // 创建子弹对象
        Random random = new Random();
        String bulletId = "Bullet_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        GameObject bullet = new GameObject("Bullet") {
            private Vector2 position;
            private Vector2 velocity;
            private float travelDistance;
            private ParticleSystem trailParticleSystem;
            private Vector2 startPos = new Vector2(); // 记录起始位置用于弹道线

            @Override
            public void initialize() {
                this.position = new Vector2(playerPos);
                this.velocity = direction.multiply(actualSpeed);
                this.travelDistance = 0;
                this.startPos = new Vector2(playerPos);

                // 添加TransformComponent用于录像系统识别位置
                this.addComponent(new TransformComponent(position));

                // 创建尾迹粒子系统
                ParticleSystem.Config trailConfig = new ParticleSystem.Config();
                trailConfig.spawnRate = 0.01f;
                trailConfig.initialCount = 0;
                trailConfig.speedMin = 0;
                trailConfig.speedMax = 10;
                trailConfig.lifeMin = 0.2f;
                trailConfig.lifeMax = 0.5f;
                trailConfig.sizeMin = 1f;
                trailConfig.sizeMax = 3f;
                trailConfig.r = 0.0f;
                trailConfig.g = 1.0f;
                trailConfig.b = 1.0f;
                trailConfig.opacityMultiplier = 0.6f;

                trailParticleSystem = new ParticleSystem(scene.getEngine().getRenderer(), position, trailConfig);
                trailParticleSystem.setSpawnRate(0.02f);
            }

            @Override
            public void update(float deltaTime) {
                // 移动子弹
                Vector2 delta = velocity.multiply(deltaTime);
                position = position.add(delta);
                travelDistance += delta.magnitude();

                // 更新TransformComponent位置
                TransformComponent bulletTransform = getComponent(TransformComponent.class);
                if (bulletTransform != null) {
                    bulletTransform.setPosition(position);
                }

                // 更新尾迹粒子系统
                if (trailParticleSystem != null) {
                    trailParticleSystem.setPosition(position);
                    trailParticleSystem.update(deltaTime);
                }

                // 检查超出射程
                if (travelDistance >= skill.config.range) {
                    destroy();
                    return;
                }

                // 检查碰撞
                checkCollisions();
            }

            @Override
            public void render() {
                // 渲染子弹（矩形）- 使用用户提供的渲染代码
                if (scene != null && scene.getEngine() != null) {
                    scene.getEngine().getRenderer().drawRect(
                            position.x - 3, position.y - 3, 6, 6,
                            0.0f, 1.0f, 1.0f, 1.0f // 青色
                    );

                    // 渲染弹道线
                    scene.getEngine().getRenderer().drawLine(
                            startPos.x, startPos.y, position.x, position.y,
                            0.0f, 1.0f, 1.0f, 0.3f
                    );

                    // 渲染尾迹粒子
                    if (trailParticleSystem != null) {
                        trailParticleSystem.render();
                    }
                }
            }

            @Override
            public void destroy() {
                // 清理粒子系统
                if (trailParticleSystem != null) {
                    trailParticleSystem.clear();
                }
                activeBullets.remove(this);
                super.destroy();
            }

            private void checkCollisions() {
                for (GameObject enemy : scene.getGameObjects()) {
                    if (enemy.getName().equals("AIPlayer")) {
                        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                        if (enemyTransform != null) {
                            float distance = position.distance(enemyTransform.getPosition());
                            if (distance <= 15) { // 碰撞半径
                                // 对敌人造成伤害
                                applyDamage(enemy, actualDamage);
                                createHitEffect(enemyTransform.getPosition(), 0.0f, 0.0f, 1.0f); // 蓝色
                                destroy();
                                return;
                            }
                        }
                    }
                }
            }
        };

        activeBullets.add(bullet);
        scene.addGameObject(bullet);
    }

    /**
     * 更新所有子弹
     */
    private void updateBullets(float deltaTime) {
        Iterator<GameObject> iterator = activeBullets.iterator();
        while (iterator.hasNext()) {
            GameObject bullet = iterator.next();
            if (!bullet.isActive()) {
                iterator.remove();
            }
        }
    }

    /**
     * 激活闪避技能 - 修复：改进速度逻辑
     */
    private void activateDodge(SkillInstance skill) {
        isDodging = true;

        // 修复：使用合理的速度提升倍数
        float speedBoost = 1.5f; // 改为1.5倍，避免极端速度
        float duration = 0.5f; // 固定持续时间

        skill.currentCooldown = skill.config.cooldown;

        dodgeEndTime = System.currentTimeMillis() / 1000.0f + duration;

        // 应用速度提升
        PhysicsComponent physics = owner.getComponent(PhysicsComponent.class);
        if (physics != null) {
            // 修复：保存当前速度向量
            Vector2 currentVelocity = physics.getVelocity();
            originalVelocity = new Vector2(currentVelocity.x, currentVelocity.y);

            // 应用速度提升
            physics.setVelocity(currentVelocity.multiply(speedBoost));
        }

        // 应用无敌效果
        HealthComponent health = owner.getComponent(HealthComponent.class);
        if (health != null) {
            health.setInvulnerable(duration);
        }

        // 创建闪避视觉效果
        createDodgeEffect(duration);
    }

    /**
     * 结束闪避效果 - 修复：正确恢复速度
     */
    private void endDodge() {
        isDodging = false;

        // 修复：正确恢复速度
        PhysicsComponent physics = owner.getComponent(PhysicsComponent.class);
        if (physics != null) {
            // 恢复到原始速度
            physics.setVelocity(originalVelocity);
        }

        // 恢复为冷却
        SkillInstance dodgeSkill = skills.get(SkillType.DODGE);
        if (dodgeSkill != null && dodgeSkill.state == SkillState.ACTIVE) {
            dodgeSkill.state = SkillState.COOLDOWN;
            dodgeSkill.activeTime = 0;
        }
    }

    /**
     * 创建闪避视觉效果
     */
    private void createDodgeEffect(float duration) {
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        Vector2 playerPos = transform.getPosition();

        GameObject dodgeEffect = new GameObject("DodgeEffect") {
            private float lifetime = duration;

            @Override
            public void update(float deltaTime) {
                lifetime -= deltaTime;
                if (lifetime <= 0) {
                    destroy();
                }
            }

            @Override
            public void render() {
                // 渲染闪避光环
                if (scene != null && scene.getEngine() != null) {
                    float alpha = 0.7f * (lifetime / duration);
                    scene.getEngine().getRenderer().drawCircle(
                            playerPos.x, playerPos.y, 50, 32,
                            0.5f, 0.5f, 1.0f, alpha // 淡蓝色
                    );
                }
            }
        };

        scene.addGameObject(dodgeEffect);
    }

    /**
     * 创建命中效果
     */
    private void createHitEffect(Vector2 position, float r, float g, float b) {
        // 使用粒子系统创建命中效果
        if (scene != null && scene.getEngine() != null) {
            ParticleSystem.Config config = new ParticleSystem.Config();
            // 使用GameConfig常量配置命中效果
            config.initialCount = GameConfig.HIT_EFFECT_PARTICLES;
            config.speedMin = GameConfig.HIT_EFFECT_SPEED_MIN;
            config.speedMax = GameConfig.HIT_EFFECT_SPEED_MAX;
            config.lifeMin = GameConfig.HIT_EFFECT_LIFE_MIN;
            config.lifeMax = GameConfig.HIT_EFFECT_LIFE_MAX;
            config.sizeMin = GameConfig.HIT_EFFECT_SIZE_MIN;
            config.sizeMax = GameConfig.HIT_EFFECT_SIZE_MAX;
            config.r = r;
            config.g = g;
            config.b = b;

            ParticleSystem hitParticles = new ParticleSystem(scene.getEngine().getRenderer(), position, config);
            hitParticles.burst(GameConfig.HIT_EFFECT_PARTICLES);

            // 创建临时对象来管理粒子
            GameObject hitEffect = new GameObject("HitEffect") {
                private ParticleSystem particles = hitParticles;
                private float lifetime = GameConfig.HIT_EFFECT_LIFE_MAX;

                @Override
                public void update(float deltaTime) {
                    particles.update(deltaTime);
                    lifetime -= deltaTime;
                    if (lifetime <= 0) {
                        destroy();
                    }
                }

                @Override
                public void render() {
                    particles.render();
                }

                @Override
                public void destroy() {
                    particles.clear();
                    super.destroy();
                }
            };

            scene.addGameObject(hitEffect);
        }
    }

    /**
     * 处理激活中的技能
     */
    private void handleActiveSkill(SkillInstance skill, float deltaTime) {
        // 根据技能类型处理不同的激活逻辑
        switch (skill.config.type) {
            case MELEE_ATTACK:
                // 近战攻击由updateMeleeAttack处理
                break;
            case RANGED_ATTACK:
                // 远程攻击在短暂激活后结束
                if (skill.activeTime > 0.1f) {
                    endSkill(skill.config.type);
                }
                break;
            case DODGE:
                // 闪避由dodgeEndTime处理
                break;
        }
    }

    /**
     * 对敌人造成伤害
     */
    private void applyDamage(GameObject enemy, float damage) {
        HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
        if (enemyHealth != null) {
            enemyHealth.takeDamage(damage);
        }
    }

    // Getters

    /**
     * 获取当前魔力值
     */
    public float getCurrentMana() {
        return currentMana;
    }

    /**
     * 获取最大魔力值
     */
    public float getMaxMana() {
        return maxMana;
    }

    /**
     * 获取魔力百分比
     */
    public float getManaPercentage() {
        return maxMana > 0 ? currentMana / maxMana : 0;
    }

    /**
     * 获取技能状态
     */
    public SkillState getSkillState(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.state : SkillState.READY;
    }

    /**
     * 获取技能冷却时间
     */
    public float getSkillCooldown(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.currentCooldown : 0;
    }

    /**
     * 获取技能总冷却时间
     */
    public float getSkillTotalCooldown(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.config.cooldown : 0;
    }

    /**
     * 获取技能蓄力时间
     */
    public float getSkillChargeTime(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.chargeTime : 0;
    }

    /**
     * 获取技能最大蓄力时间
     */
    public float getSkillMaxChargeTime(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.config.maxChargeTime : 0;
    }

    /**
     * 检查技能是否就绪
     */
    public boolean isSkillReady(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null && skill.state == SkillState.READY && currentMana >= skill.config.manaCost;
    }

    /**
     * 检查是否正在闪避
     */
    public boolean isDodging() {
        return isDodging;
    }

    /**
     * 获取技能配置
     */
    public SkillConfig getSkillConfig(SkillType type) {
        SkillInstance skill = skills.get(type);
        return skill != null ? skill.config : null;
    }

    /**
     * 强化技能
     */
    public void enhanceSkill(SkillType type, float damageMultiplier, float rangeMultiplier,
                             float speedMultiplier, float cooldownMultiplier) {
        SkillInstance skill = skills.get(type);
        if (skill != null) {
            skill.config.damage *= damageMultiplier;
            skill.config.range *= rangeMultiplier;
            skill.config.bulletSpeed *= speedMultiplier;
            skill.config.cooldown *= cooldownMultiplier;
            skill.config.range = Math.min(skill.config.range, GameConfig.WEAPON_MAX_RANGE);
        }
    }

    /**
     * 恢复魔力
     */
    public void restoreMana(float amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
    }

    /**
     * 设置最大魔力
     */
    public void setMaxMana(float maxMana) {
        this.maxMana = maxMana;
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
}
