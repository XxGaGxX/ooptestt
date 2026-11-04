package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Quad;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;

import java.util.HashMap;
import java.util.Map;

public class Main extends SimpleApplication {

    private final Map<Geometry, Material> original = new HashMap<>();
    private final Map<Geometry, Material> unshaded = new HashMap<>();
    private boolean showUnshaded = true;

    Node spinFL;
    Node spinRL;
    Node spinRR;
    Node spinFR;
    Spatial model;

    private float steerAngle = 0f; // In radianti
    private float wheelRadius = 0.35f; // Raggio della ruota in metri

    public static void main(String[] args) {

        new Main().start();
    }

    private Node findNode(Spatial root, String name) {
        Spatial[] out = new Spatial[1];
        root.depthFirstTraversal(s -> {
            if (name.equals(s.getName()))
                out[0] = s;
        });
        return (Node) out[0];
    }

    @Override
    public void simpleInitApp() {

        settings.setWidth(1600); // Larghezza in pixel
        settings.setHeight(900); // Altezza in pixel
        settings.setResizable(true); // Permette di ridimensionarla trascinando i bordi
        restart(); // Applica le modifiche alla finestra

        model = assetManager.loadModel("Models/porsche_rework.glb");
        Node car = new Node("car");
        car.attachChild(model);
        rootNode.attachChild(car);

        spinFL = findNode(model, "wheel_FL_spin");
        spinFR = findNode(car, "wheel_FR_spin");
        spinRR = findNode(car, "wheel_RR_spin");
        spinRL = findNode(car, "wheel_RL_spin");

        rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1).normalizeLocal()));
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.4f)));

        // Cielo
        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.7f, 0.95f, 1f));

        // Terreno: la base delle gomme sta a Y ≈ -0.15
        Geometry ground = new Geometry("ground", new Quad(500, 500));
        Material gm = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        gm.setColor("Color", ColorRGBA.DarkGray);
        ground.setMaterial(gm);
        ground.rotate(-FastMath.HALF_PI, 0, 0); // da verticale a orizzontale
        ground.setLocalTranslation(-250, -0.15f, 250); // centrato sull'origine
        rootNode.attachChild(ground);

        // CameraNode hoodCam = new CameraNode("hoodCam", cam);
        // hoodCam.setControlDir(ControlDirection.SpatialToCamera);
        // car.attachChild(hoodCam);
        // hoodCam.setLocalTranslation(0f, 0.95f, 1.25f);
        // hoodCam.setLocalRotation(new Quaternion().lookAt(Vector3f.UNIT_Z,
        // Vector3f.UNIT_Y));

        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 1000f);
        cam.setLocation(new Vector3f(6, 2, 8));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(10f); // m/s: alza per muoverti più veloce
        flyCam.setRotationSpeed(1.5f); // sensibilità del mouse
    }

    // Dichiara solo questa fuori dal metodo
    private float timer = 0f;
    private float totalSpinAngle = 0f; // Mantiene l'accumulo della rotazione delle ruote

    @Override
    public void simpleUpdate(float tpf) {
        timer += tpf;

        // 1. Calcola l'angolo di sterzata sull'asse Z (da -30° a +30°)
        float steerZ = FastMath.cos(timer * 2.0f) * (18f * FastMath.DEG_TO_RAD);

        // 2. Accumula l'angolo di rotolamento delle ruote (avanti/indietro su X)
        float v = 10f; // m/s
        totalSpinAngle += -v * tpf / 0.35f;

        // 3. Crea i quaternioni per le due rotazioni
        Quaternion qSteer = new Quaternion().fromAngles(0, steerZ, 0); // Sterzo su Z
        Quaternion qSpin = new Quaternion().fromAngles(totalSpinAngle, 0, 0); // Rotolamento su X

        // 4. Combina le due rotazioni (Sterzo * Rotolamento)
        Quaternion combinedFront = qSteer.mult(qSpin);

        // 5. APPLICA ALLE RUOTE ANTERIORI (Sterzo + Spin sullo stesso nodo)
        if (spinFL != null)
            spinFL.setLocalRotation(combinedFront);
        if (spinFR != null)
            spinFR.setLocalRotation(combinedFront);

        // 6. APPLICA ALLE RUOTE POSTERIORI (Solo Spin)
        if (spinRL != null)
            spinRL.setLocalRotation(qSpin);
        if (spinRR != null)
            spinRR.setLocalRotation(qSpin);
    }

}