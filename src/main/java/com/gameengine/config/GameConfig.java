package com.gameengine.config;

/**
 * 游戏配置常量类
 */
public final class GameConfig {
    private GameConfig() {} // 防止实例化
    
    // 游戏窗口设置
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    public static final String GAME_TITLE = "葫芦娃大战妖精";
    
    // 游戏边界
    public static final int GAME_BOUNDS_LEFT = 0;
    public static final int GAME_BOUNDS_TOP = 0;
    public static final int GAME_BOUNDS_RIGHT = WINDOW_WIDTH;
    public static final int GAME_BOUNDS_BOTTOM = WINDOW_HEIGHT;
    
    // UI设置
    public static final int SKILL_BAR_Y = WINDOW_HEIGHT - 80;
    public static final int SKILL_ICON_SIZE = 60;
    public static final int SKILL_ICON_SPACING = 80;
    public static final int SKILL_BAR_CENTER_X = WINDOW_WIDTH / 2;
    
    // 技能键位
    public static final int KEY_MELEE = 90;    // Z键
    public static final int KEY_RANGED = 88;   // X键
    public static final int KEY_DODGE = 67;    // C键
    public static final int KEY_RESTART = 82;  // R键
    public static final int KEY_QUIT = 81;     // Q键
    
    // 移动键位
    public static final int KEY_UP = 87;       // W键
    public static final int KEY_DOWN = 83;     // S键
    public static final int KEY_LEFT = 65;     // A键
    public static final int KEY_RIGHT = 68;    // D键

    // 血量配置
    public static final float PLAYER_MAX_HEALTH = 100f;
    public static final float ENEMY_BASE_HEALTH = 50f;
    public static final float ENEMY_HEALTH_GROWTH = 10f; // 每波血量增长
    public static final int ENEMY_BASE_COUNT = 5; // 基础敌人数量
    public static final int ENEMY_COUNT_GROWTH = 2; // 每波敌人数量增长
    public static final float WAVE_INTERVAL = 30f; // 波次间隔时间（秒）
    
    // 伤害配置
    public static final float PLAYER_COLLISION_DAMAGE = 10f; // 玩家碰撞伤害
    public static final float MELEE_ATTACK_DAMAGE = 50f; // 近战攻击伤害
    public static final float RANGED_ATTACK_DAMAGE = 30f; // 远程攻击伤害
    
    // 无敌时间配置
    public static final float PLAYER_INVULNERABLE_TIME = 1.0f; // 玩家无敌时间

    // 魔力系统配置
    public static final float PLAYER_MAX_MANA = 100f;
    public static final float MANA_REGEN_RATE = 25f; // 每秒回复25点魔力
    public static final float MANA_REGEN_DELAY = 3.0f; // 3秒后开始快速回复

    // 近战攻击配置
    public static final float MELEE_DAMAGE = 15f;
    public static final float MELEE_RANGE = 150f;
    public static final float MELEE_BULLET_SPEED = 5.0f;
    public static final float MELEE_COOLDOWN = 0.5f;
    public static final float MELEE_MANA_COST = 10f;
    public static final boolean MELEE_CHARGEABLE = true;
    public static final float MELEE_MAX_CHARGE_TIME = 0f;
    public static final float MELEE_CHARGE_DAMAGE_MULTIPLIER = 1.0f;
    public static final float MELEE_CHARGE_SPEED_MULTIPLIER = 1.0f;
    public static final float MELEE_ANGLE_RANGE = 60.0f; // 近战攻击扇形角度

    // 远程攻击配置
    public static final float RANGED_DAMAGE = 25f;
    public static final float RANGED_RANGE = 800f;
    public static final float RANGED_BULLET_SPEED = 300f;
    public static final float RANGED_COOLDOWN = 2.0f;
    public static final float RANGED_MANA_COST = 20f;
    public static final boolean RANGED_CHARGEABLE = true;
    public static final float RANGED_MAX_CHARGE_TIME = 2.0f;
    public static final float RANGED_CHARGE_DAMAGE_MULTIPLIER = 2.0f;
    public static final float RANGED_CHARGE_SPEED_MULTIPLIER = 2.0f;

    // 闪避配置
    public static final float DODGE_COOLDOWN = 8.0f;
    public static final float DODGE_MANA_COST = 30f;

    // 粒子系统配置
    public static final float MELEE_PARTICLE_SPAWN_RATE = 0.01f;
    public static final float MELEE_PARTICLE_SPEED_MIN = 100f;
    public static final float MELEE_PARTICLE_SPEED_MAX = 300f;
    public static final float MELEE_PARTICLE_LIFE_MIN = 0.3f;
    public static final float MELEE_PARTICLE_LIFE_MAX = 0.8f;
    public static final float MELEE_PARTICLE_SIZE_MIN = 2f;
    public static final float MELEE_PARTICLE_SIZE_MAX = 6f;
    public static final float MELEE_PARTICLE_R = 1.0f; // 橙色 - 红色分量
    public static final float MELEE_PARTICLE_G = 0.5f; // 橙色 - 绿色分量
    public static final float MELEE_PARTICLE_B = 0.0f; // 橙色 - 蓝色分量
    public static final float MELEE_PARTICLE_OPACITY = 0.8f;
    public static final int MELEE_PARTICLES_PER_FRAME = 5;

    // 命中效果配置
    public static final int HIT_EFFECT_PARTICLES = 8;
    public static final float HIT_EFFECT_SPEED_MIN = 50f;
    public static final float HIT_EFFECT_SPEED_MAX = 150f;
    public static final float HIT_EFFECT_LIFE_MIN = 0.3f;
    public static final float HIT_EFFECT_LIFE_MAX = 0.8f;
    public static final float HIT_EFFECT_SIZE_MIN = 2f;
    public static final float HIT_EFFECT_SIZE_MAX = 6f;

    // 碰撞检测配置
    public static final float BULLET_COLLISION_RADIUS = 15f;
    public static final float PLAYER_COLLISION_RADIUS = 30f;

    // 其他配置
    public static final float WEAPON_PICKUP_RANGE = 30f;
    public static final float WEAPON_MAX_RANGE = (float) Math.hypot(WINDOW_WIDTH, WINDOW_HEIGHT);


}
