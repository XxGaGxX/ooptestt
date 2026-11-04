package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
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

public class Main extends SimpleApplication {
    private BulletAppState bulletAppState;
    private VehicleControl vehicle;

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
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(25f);
        cam.setLocation(new Vector3f(0, 4, -12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
}