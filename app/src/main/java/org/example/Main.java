package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;

public class Main extends SimpleApplication implements ActionListener {
    private BulletAppState bulletAppState;
    private com.jme3.font.BitmapText debugText;
    private VehicleControl vehicle;
    private final float accelerationForce = 1000.0f;
    private final float brakeForce = 4000.0f;
    private float steeringValue = 0;
    private boolean left = false, right = false, up = false, down = false;
    private float accelerationValue = 0;
    private final float maxSteerAngle = 0.35f;

    private void initKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Lefts", "Rights", "Ups", "Downs", "Reset");
    }

    public static void main(String[] args) {
        new Main().start();
    }

    private Node findNode(Spatial parent, String name) {
        if (parent.getName() != null && parent.getName().equals(name) && parent instanceof Node) {
            return (Node) parent;
        }
        if (parent instanceof Node) {
            for (Spatial child : ((Node) parent).getChildren()) {
                Node result = findNode(child, name);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    @Override
    public void simpleInitApp() {

        debugText = new com.jme3.font.BitmapText(guiFont);
        debugText.setSize(guiFont.getCharSet().getRenderedSize());
        debugText.setLocalTranslation(20, cam.getHeight() - 20, 0); // In alto a sinistra
        guiNode.attachChild(debugText);

        initKeys();

        // 1. Inizializzazione fisica Bullet
        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        // 2. Caricamento modello auto
        Spatial carModel = assetManager.loadModel("Models/porsche_rework.glb");
        rootNode.attachChild(carModel);

        // 3. Ricerca nodi ruote nel modello GLTF
        Node wheelFR = findNode(carModel, "wheel_FR_spin");
        Node wheelFL = findNode(carModel, "wheel_FL_spin");
        Node wheelRR = findNode(carModel, "wheel_RR_spin");
        Node wheelRL = findNode(carModel, "wheel_RL_spin");

        // FONDAMENTALE: Forza l'aggiornamento delle trasformazioni e delle BoundingBox
        // prima di leggere pos e bounds
        carModel.updateGeometricState();

        // 4. Creazione forma fisica telaio e VehicleControl

        // 1. Calcola il box di ingombro (BoundingBox) del modello 3D
        BoundingBox bounds = (BoundingBox) carModel.getWorldBound();

        // 2. Calcola la mezza dimensione (extent) per la box fisica
        // Riduciamo leggermente le dimensioni (es. * 0.85f) per evitare che il bordo
        // inferiore tocchi terra
        Vector3f extents = bounds.getExtent(null).multLocal(0.85f, 0.5f, 0.85f);
        BoxCollisionShape boxShape = new BoxCollisionShape(extents);

        // 3. Calcola l'offset dinamico: sposta il centro della hitbox verso l'alto
        // basandosi sull'altezza totale del modello (bounds.getYExtent())
        float offsetY = bounds.getYExtent() * 0.5f;

        CompoundCollisionShape carShape = new CompoundCollisionShape();
        carShape.addChildShape(boxShape, new Vector3f(0f, offsetY, 0f));

        // 4. Assegna il control
        vehicle = new VehicleControl(carShape, 1265f);
        carModel.addControl(vehicle);
        bulletAppState.getPhysicsSpace().add(vehicle);

        // setup delle sospensioni

        float stiffness = 150.0f;// 200=f1 car
        float compValue = .3f; // (should be lower than damp)
        float dampValue = .4f;
        vehicle.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        vehicle.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        vehicle.setSuspensionStiffness(stiffness);
        vehicle.setMaxSuspensionForce(10000.0f);

        Vector3f wheelDirection = new Vector3f(0, -1, 0); // was 0, -1, 0
        Vector3f wheelAxle = new Vector3f(-1, 0, 0); // was -1, 0, 0
        float radius = 0.5f;
        float restLength = 0.08f;

        Vector3f connectionFR = wheelFR.getLocalTranslation().add(0, restLength, 0);
        Vector3f connectionFL = wheelFL.getLocalTranslation().add(0, restLength, 0);
        Vector3f connectionRR = wheelRR.getLocalTranslation().add(0, restLength, 0);
        Vector3f connectionRL = wheelRL.getLocalTranslation().add(0, restLength, 0);

        // Ruote Anteriori (Front Left / Front Right)
        vehicle.addWheel(wheelFL, connectionFL,
                wheelDirection, wheelAxle, restLength, radius, true);

        vehicle.addWheel(wheelFR, connectionFR,
                wheelDirection, wheelAxle, restLength, radius, true);

        // Ruote Posteriori (Rear Left / Rear Right)
        vehicle.addWheel(wheelRL, connectionRL,
                wheelDirection, wheelAxle, restLength, radius, false);

        vehicle.addWheel(wheelRR, connectionRR,
                wheelDirection, wheelAxle, restLength, radius, false);

        // 6. Pavimento fisico per sostenere la vettura
        Geometry floorGeo = new Geometry("Floor", new Box(50f, 0.1f, 50f));
        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.Gray);
        floorGeo.setMaterial(floorMat);
        floorGeo.setLocalTranslation(0, -1.5f, 0); // Un po' sotto l'origine
        rootNode.attachChild(floorGeo);

        RigidBodyControl floorPhysics = new RigidBodyControl(0.0f);
        floorGeo.addControl(floorPhysics);
        bulletAppState.getPhysicsSpace().add(floorPhysics);

        // 7. Sfondo e Illuminazione
        viewPort.setBackgroundColor(ColorRGBA.Cyan);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1.0f, -0.3f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.2f));
        rootNode.addLight(sun);

        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.4f)));

        // 8. Posizionamento Camera
        flyCam.setEnabled(false);

        // Inizializza la ChaseCamera collegata al modello dell'auto
        ChaseCamera chaseCam = new ChaseCamera(cam, carModel, inputManager);

        // Configurazione distanze e angoli per una visuale GT3
        chaseCam.setMinDistance(5.0f); // Distanza minima quando si fa zoom
        chaseCam.setMaxDistance(12.0f); // Distanza massima
        chaseCam.setDefaultDistance(8.0f); // Distanza di default dietro l'auto
        chaseCam.setDefaultVerticalRotation(FastMath.PI / 6); // Angolo verticale di default (~30 gradi)
        chaseCam.setMinVerticalRotation(0.0f);

        // Smoothness / Fluidità dell'inseguimento
        chaseCam.setSmoothMotion(true);
        chaseCam.setChasingSensitivity(5.0f); // Reattività nell'inseguire la rotazione

    }

    @Override
    public void simpleUpdate(float tpf) {
        if (vehicle != null) {
            float speedKmH = vehicle.getCurrentVehicleSpeedKmHour();
            com.jme3.math.Vector3f pos = vehicle.getPhysicsLocation();

            debugText.setText(String.format(
                    "Velocita: %.1f km/h | Pos: X=%.2f Y=%.2f Z=%.2f | Sterzo: %.2f",
                    speedKmH, pos.x, pos.y, pos.z, steeringValue));
        }
    }

    //

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Lefts":
                left = isPressed;
                break;
            case "Rights":
                right = isPressed;
                break;
            case "Ups":
                up = isPressed;
                break;
            case "Downs":
                down = isPressed;
                break;
            case "Reset":
                if (isPressed) {
                    vehicle.setPhysicsLocation(new Vector3f(0, 1f, 0));
                    vehicle.setPhysicsRotation(new Matrix3f());
                    vehicle.setLinearVelocity(Vector3f.ZERO);
                    vehicle.setAngularVelocity(Vector3f.ZERO);
                    vehicle.resetSuspension();
                }
                break;
        }

        // --- STERZO (A / D) ---
        steeringValue = 0f;
        if (left)
            steeringValue += maxSteerAngle;
        if (right)
            steeringValue -= maxSteerAngle;
        vehicle.steer(steeringValue);

        // --- ACCELERATORE & FRENO (W / S) ---
        if (up) {
            vehicle.accelerate(accelerationForce);
            vehicle.brake(0f);
        } else {
            vehicle.accelerate(0f);
        }

        if (down) {
            vehicle.brake(brakeForce);
        } else if (!up) {
            vehicle.brake(0f);
        }
    }

}