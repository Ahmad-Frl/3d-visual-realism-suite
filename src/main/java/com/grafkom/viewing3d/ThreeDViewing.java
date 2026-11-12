package com.grafkom.viewing3d;

import org.joml.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/** Visual Realism Suite — demonstrasi lima indikator praktikum Grafika Komputer Minggu 11. */
public class ThreeDViewing {
    private long window;
    private int width=1200,height=760;
    private final Camera3D camera=new Camera3D();
    private boolean blinn=true,smooth=true,depth=true,culling=true,texture=true,shadows=true,filter=true;
    private int model=0;
    private ShaderProgram sceneShader,depthShader;
    private Mesh3D sphereSmooth,sphereFlat,cube,floor,cullingProbe;
    private Texture2D material;
    private ShadowMapFBO shadow;
    private HudOverlay hud;
    private final Vector3f light=new Vector3f(-3.5f,6.5f,3.5f);
    private final Matrix4f projection=new Matrix4f();
    private long fpsStart=System.nanoTime(); private int fpsFrames=0,fps=0;

    public void run(){
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit())throw new IllegalStateException("GLFW gagal diinisialisasi");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE,GLFW_TRUE);glfwWindowHint(GLFW_SAMPLES,4);
        // Minta framebuffer dengan depth buffer nyata untuk pengujian Z-buffer.
        glfwWindowHint(GLFW_DEPTH_BITS,24);
        window=glfwCreateWindow(width,height,"VISUAL REALISM SUITE | Professional Edition",0,0);
        if(window==0)throw new IllegalStateException("Window gagal dibuat");
        glfwMakeContextCurrent(window);glfwSwapInterval(1);GL.createCapabilities();glEnable(GL_MULTISAMPLE);
        glfwSetFramebufferSizeCallback(window,(w,x,y)->{width=java.lang.Math.max(1,x);height=java.lang.Math.max(1,y);glViewport(0,0,width,height);});
        glfwSetKeyCallback(window,(w,key,sc,action,mods)->{if(action==GLFW_PRESS){switch(key){
            case GLFW_KEY_ESCAPE->glfwSetWindowShouldClose(w,true);case GLFW_KEY_B->blinn=!blinn;case GLFW_KEY_F->smooth=!smooth;
            case GLFW_KEY_Z->depth=!depth;case GLFW_KEY_C->culling=!culling;case GLFW_KEY_T->texture=!texture;
            case GLFW_KEY_G->shadows=!shadows;case GLFW_KEY_N->filter=!filter;case GLFW_KEY_M->model=(model+1)%2;
            case GLFW_KEY_LEFT->camera.orbit(-5,0);case GLFW_KEY_RIGHT->camera.orbit(5,0);
            case GLFW_KEY_UP->camera.orbit(0,4);case GLFW_KEY_DOWN->camera.orbit(0,-4);
            case GLFW_KEY_W->camera.zoom(-.5f);case GLFW_KEY_S->camera.zoom(.5f);
        }}});
        sceneShader=new ShaderProgram("/shaders/realism.vert","/shaders/realism.frag");
        depthShader=new ShaderProgram("/shaders/depth.vert","/shaders/depth.frag");
        sphereSmooth=Mesh3D.sphere(48,72,true);sphereFlat=Mesh3D.sphere(48,72,false);
        cube=Mesh3D.cube();floor=Mesh3D.plane();cullingProbe=Mesh3D.cullingProbe();material=new Texture2D("/textures/material.png");
        shadow=new ShadowMapFBO();hud=new HudOverlay(width,height);
        glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LESS);glDepthMask(true);glClearDepth(1.0);
        glClearColor(.018f,.027f,.043f,1f);
        loop();cleanup();
    }

    private void loop(){while(!glfwWindowShouldClose(window)){
        glfwPollEvents();
        projection.identity().perspective((float)java.lang.Math.toRadians(45),(float)width/height,.1f,60f);
        Matrix4f view=camera.view();
        Matrix4f lightVP=new Matrix4f().ortho(-11,11,-11,11,.1f,28f).lookAt(light,new Vector3f(0,0,0),new Vector3f(0,1,0));
        if(shadows){
            glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LESS);glDepthMask(true);glClearDepth(1.0);
            shadow.bindForWriting();glClear(GL_DEPTH_BUFFER_BIT);
            depthShader.use();depthShader.set("lightVP",lightVP);drawDepth();shadow.unbind(width,height);
        }
        // Kembali ke framebuffer utama setelah shadow pass.
        glBindFramebuffer(GL_FRAMEBUFFER,0);
        glViewport(0,0,width,height);
        // Depth write harus aktif sebelum membersihkan depth buffer.
        glDepthMask(true);
        glClearDepth(1.0);
        HSRManager.setupDepthTest(depth);
        HSRManager.setupBackFaceCulling(culling);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        sceneShader.use();sceneShader.set("view",view);sceneShader.set("projection",projection);sceneShader.set("lightVP",lightVP);
        sceneShader.set("lightPos",light);sceneShader.set("viewPos",camera.position());
        sceneShader.set("lightColor",new Vector3f(1f,.91f,.78f));
        sceneShader.set("useBlinnPhong",blinn?1:0);sceneShader.set("useTexture",texture?1:0);
        sceneShader.set("useShadow",shadows?1:0);sceneShader.set("diffuseTexture",0);sceneShader.set("shadowMap",1);
        sceneShader.set("shininess",blinn?56f:28f);
        material.setFiltering(filter);material.bind(0);shadow.bindForReading(1);drawScene(sceneShader);
        updateFps();hud.update(blinn,smooth,depth,culling,texture,shadows,filter,blinn?"BLINN-PHONG":"PHONG",smooth?"SMOOTH":"FLAT",fps,model==0?"SPHERE":"CUBE");
        hud.render();updateTitle();glfwSwapBuffers(window);
    }}

    /** Renderiza no shadow pass exatamente o mesmo objeto selecionado no pass principal. */
    private void drawDepth(){
        Matrix4f m = new Matrix4f();
        if (model == 0) {
            m.translate(0,1.05f,0).scale(1.12f);
            depthShader.set("model",m);
            sphereSmooth.draw();
        } else {
            m.translate(0,0.82f,0).scale(0.82f);
            depthShader.set("model",m);
            cube.draw();
        }
        m.identity().translate(0,-.055f,0);
        depthShader.set("model",m);
        floor.draw();
    }

    /** M mengganti geometri aktif: 0 = sphere, 1 = cube. Ground tetap ditampilkan. */
    private void drawScene(ShaderProgram s){
        Matrix4f m = new Matrix4f();
        if (model == 0) {
            m.translate(0,1.05f,0).scale(1.12f);
            s.set("model",m);
            s.set("objectColor",new Vector3f(.22f,.55f,.88f));
            s.set("useTexture",texture?1:0);
            (smooth?sphereSmooth:sphereFlat).draw();
        } else {
            m.translate(0,0.82f,0).scale(0.82f);
            s.set("model",m);
            s.set("objectColor",new Vector3f(.94f,.37f,.17f));
            s.set("useTexture",texture?1:0);
            cube.draw();
        }

        // Probe back-face culling: quad sengaja memiliki winding terbalik.
        // Ia harus terlihat hanya ketika GL_CULL_FACE dimatikan.
        m.identity().translate(-1.85f,1.05f,0.15f)
            .rotateY((float)java.lang.Math.toRadians(35.0))
            .rotateX((float)java.lang.Math.toRadians(-24.0));
        s.set("model",m);
        s.set("objectColor",new Vector3f(1.0f,.12f,.72f));
        s.set("useTexture",0);
        cullingProbe.draw();

        // Lantai menggunakan warna material sendiri, bukan tekstur objek utama.
        m.identity().translate(0,-.055f,0);
        s.set("model",m);
        s.set("objectColor",new Vector3f(.56f,.62f,.71f));
        s.set("useTexture",0);
        floor.draw();
        s.set("useTexture",texture?1:0);
    }
    private void updateFps(){fpsFrames++;long now=System.nanoTime();if(now-fpsStart>=1_000_000_000L){fps=fpsFrames;fpsFrames=0;fpsStart=now;}}
    private void updateTitle(){glfwSetWindowTitle(window,String.format("VISUAL REALISM SUITE | %s | %s | Object:%s | Depth:%s Cull:%s Texture:%s Shadow:%s Filter:%s",blinn?"Blinn-Phong":"Phong",smooth?"Smooth":"Flat",model==0?"Sphere":"Cube",on(depth),on(culling),on(texture),on(shadows),filter?"Bilinear":"Nearest"));}
    private String on(boolean b){return b?"ON":"OFF";}
    private void cleanup(){if(hud!=null)hud.close();if(sceneShader!=null)sceneShader.close();if(depthShader!=null)depthShader.close();if(sphereSmooth!=null)sphereSmooth.close();if(sphereFlat!=null)sphereFlat.close();if(cube!=null)cube.close();if(floor!=null)floor.close();if(cullingProbe!=null)cullingProbe.close();if(material!=null)material.close();if(shadow!=null)shadow.close();glfwDestroyWindow(window);glfwTerminate();}
    public static void main(String[] args){new ThreeDViewing().run();}
}
