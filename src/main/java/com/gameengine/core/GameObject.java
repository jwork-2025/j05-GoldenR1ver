package com.gameengine.core;

import com.gameengine.scene.Scene;

import java.util.*;

public class GameObject {
    protected boolean active;
    protected String name;
    protected final List<Component<?>> components;

    // J03: 从Scene出发的通信
    private Scene scene; // 所属场景
    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }
    // J04: 支持进程之间的通信
    private Map<String, Object>userData;
    public synchronized void setUserData(String key, Object value) {userData.put(key, value);}
    public synchronized Object getUserData(String key) {return userData.get(key);}
    public synchronized void removeUserData(String key) {userData.remove(key);}
    public synchronized boolean hasUserData(String key) {return userData.containsKey(key);}

    public GameObject() {
        this.active = true;
        this.name = "GameObject";
        this.components = new ArrayList<>();
        // J04
        this.userData = new HashMap<>();
    }
    
    public GameObject(String name) {
        this();
        this.name = name;
    }
    
    public void update(float deltaTime) {
        updateComponents(deltaTime);
    }
    
    public void render() {
        renderComponents();
    }
    
    public void initialize() {
    }
    
    public void destroy() {
        this.active = false;
        for (Component<?> component : components) {
            component.destroy();
        }
        components.clear();
    }
    
    public <T extends Component<T>> T addComponent(T component) {
        component.setOwner(this);
        components.add(component);
        component.initialize();
        return component;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> T getComponent(Class<T> componentType) {
        for (Component<?> component : components) {
            if (componentType.isInstance(component)) {
                return (T) component;
            }
        }
        return null;
    }
    
    public <T extends Component<T>> boolean hasComponent(Class<T> componentType) {
        for (Component<?> component : components) {
            if (componentType.isInstance(component)) {
                return true;
            }
        }
        return false;
    }
    
    public void updateComponents(float deltaTime) {
        for (Component<?> component : components) {
            if (component.isEnabled()) {
                component.update(deltaTime);
            }
        }
    }
    
    public void renderComponents() {
        for (Component<?> component : components) {
            if (component.isEnabled()) {
                component.render();
            }
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
