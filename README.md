[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/iHSjCEgj)

# J05

基于J05示例代码完善而来的新游戏。为了适配更高性能的GPU渲染，选择在J05上直接进行更改。

修改内容包括

## 更好的注释和代码逻辑

通读所有代码，为所有代码添加了易懂的注释。

在Scene类中增加GameEngine和GameLogic对象，将Scene类作为类间通信的枢纽。各个组件可以通过Scene类进行通信。

```java
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
```

为GameObject增加了线程间的消息机制。

```java
private Map<String, Object>userData;
    public synchronized void setUserData(String key, Object value) {userData.put(key, value);}
    public synchronized Object getUserData(String key) {return userData.get(key);}
    public synchronized void removeUserData(String key) {userData.remove(key);}
    public synchronized boolean hasUserData(String key) {return userData.containsKey(key);}

```

## 更多的游玩内容

* 增加了血量组件`HealthComponent` ，敌人和玩家都有血量设定，修改了玩家的死亡逻辑。

* 增加了技能组件`SkillComponent`，实现了四娃的技能喷火，五娃的技能水枪，七娃的技能隐身（加速+无敌）。以及技能对应的魔力系统。喷火技能支持持续释放，水枪技能支持蓄力。

* 增加了武器组件`WeaponComponent`和掉落物`WeaponDrop`，实现了拾取、应用、强化、输出日志的功能。

* 增加了`Config`文件，便于快速调试游戏参数。

* 增加了一些复杂的粒子效果。`

## 进行性能优化

部分粒子效果使用多线程渲染。

敌人的寻路+索敌逻辑使用多线程渲染。

## 存档和读档功能

**读档功能**

基于关键帧和按键事件驱动的存读档系统。记录所有SkillComponent，TransformComponent，RenderComponent的状态。在目前的实现中记录：人类玩家的技能状态，所有玩家的位置和图像，子弹的位置和图像，WeaponDrop的位置和图像。

**数据结构**

使用JSONL格式存储：每行一个JSON对象。 记录头部信息、输入事件、实体状态和时间戳。其中头部信息为版本号、屏幕尺寸，用于控制窗口大小。输入事件为玩家按键的瞬时状态。实体状态为玩家的位置、渲染类型、尺寸、颜色等，修改示例录像系统，通过唯一对象ID机制动态跟踪实体生命周期，适配敌人可能会被杀死的可能，彻底解决敌人瞬移问题。时间戳用于精确地同步回放。 

**性能优化**

主线程生成数据，使用专门的录制线程异步写入文件。通过每0.5s记录游戏完整状态，降低性能开销。且仅在关键帧之间只记录输入事件变化，通过精度控制减少数据量，显著减少录像的文件大小，同时采用插值算法实现平滑回放，性能优化显著。