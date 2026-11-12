package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** HUD ringan berbasis Java2D yang ditampilkan sebagai overlay OpenGL. */
public final class HudOverlay implements AutoCloseable {
    private final int width, height, texture, vao, vbo, program;
    private int frame;

    public HudOverlay(int width, int height) {
        this.width = width; this.height = height;
        texture = glGenTextures(); glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        float[] quad = {-1,-1,0,0, 1,-1,1,0, 1,1,1,1, -1,-1,0,0, 1,1,1,1, -1,1,0,1};
        vao=glGenVertexArrays(); vbo=glGenBuffers(); glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER,vbo);
        try(MemoryStack stack=MemoryStack.stackPush()){glBufferData(GL_ARRAY_BUFFER,stack.floats(quad),GL_STATIC_DRAW);}
        glVertexAttribPointer(0,2,GL_FLOAT,false,4*Float.BYTES,0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,2,GL_FLOAT,false,4*Float.BYTES,2L*Float.BYTES); glEnableVertexAttribArray(1); glBindVertexArray(0);
        String vs="#version 330 core\nlayout(location=0) in vec2 p;layout(location=1) in vec2 uv;out vec2 t;void main(){t=uv;gl_Position=vec4(p,0,1);}";
        String fs="#version 330 core\nin vec2 t;out vec4 c;uniform sampler2D hud;void main(){c=texture(hud,t);} ";
        int v=compile(GL_VERTEX_SHADER,vs), f=compile(GL_FRAGMENT_SHADER,fs); program=glCreateProgram(); glAttachShader(program,v);glAttachShader(program,f);glLinkProgram(program);
        if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE) throw new IllegalStateException("HUD shader: "+glGetProgramInfoLog(program));
        glDeleteShader(v);glDeleteShader(f); update(false,true,true,true,true,true,true,"Blinn-Phong","Smooth",60,"SPHERE");
    }
    private static int compile(int type,String src){int s=glCreateShader(type);glShaderSource(s,src);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException(glGetShaderInfoLog(s));return s;}
    public void update(boolean blinn,boolean smooth,boolean depth,boolean cull,boolean textureOn,boolean shadow,boolean bilinear,String lightMode,String shadeMode,int fps,String modelName){
        if((frame++%5)!=0)return;
        BufferedImage img=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB); Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Segoe UI",Font.BOLD,19));g.setColor(new Color(10,16,27,232));g.fillRoundRect(22,18,width-44,58,16,16);
        g.setColor(new Color(85,190,255));g.fillRoundRect(38,34,7,26,5,5);g.setColor(Color.WHITE);g.drawString("VISUAL REALISM",58,43);g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.setColor(new Color(163,180,202));g.drawString("SUITE  /  COMPUTER GRAPHICS LAB",58,59);
        g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(new Color(110,225,180));g.drawString("● RENDERING ACTIVE",width-260,43);g.setColor(new Color(205,216,231));g.drawString("FPS  "+fps,width-260,60);
        panel(g,22,92,238,438,"SCENE HIERARCHY");
        item(g,48,151,"◉  Sphere", "SPHERE".equals(modelName));item(g,48,188,"▧  Cube", "CUBE".equals(modelName));item(g,48,225,"▱  Ground Plane",false);
        g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.setColor(new Color(137,157,181));g.drawString("ACTIVE OBJECT: "+modelName,40,495);
        panel(g,width-270,92,248,438,"RENDER SETTINGS");
        row(g,width-248,151,"Lighting",lightMode,blinn);row(g,width-248,204,"Shading",shadeMode,smooth);row(g,width-248,257,"Depth Test",depth?"ON":"OFF",depth);row(g,width-248,310,"Back-face Cull",cull?"ON":"OFF",cull);row(g,width-248,363,"Texture Map",textureOn?"ON":"OFF",textureOn);row(g,width-248,416,"Shadow Map",shadow?"ON":"OFF",shadow);row(g,width-248,469,"Filtering",bilinear?"BILINEAR":"NEAREST",bilinear);
        g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(new Color(225,235,247));g.drawString("KEYBOARD SHORTCUTS",width/2-105,height-76);
        g.setFont(new Font("Segoe UI",Font.PLAIN,11));g.setColor(new Color(161,180,204));g.drawString("B Lighting   F Shading   Z Depth   C Culling   T Texture   G Shadow   N Filter",width/2-220,height-54);
        g.drawString("M Switch Sphere/Cube     Arrow Keys: Orbit Camera     W / S: Zoom     ESC: Exit",width/2-220,height-36);
        g.setColor(new Color(115,137,164));g.drawString("WEEK 11  •  PHONG / BLINN-PHONG  •  HSR  •  UV MAPPING  •  SHADOW MAPPING",width/2-225, height-12);
        g.dispose();
        int[] pixels=new int[width*height];img.getRGB(0,0,width,height,pixels,0,width);ByteBuffer b=MemoryUtil.memAlloc(width*height*4);
        for(int y=height-1;y>=0;y--)for(int x=0;x<width;x++){int p=pixels[y*width+x];b.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>>24));}b.flip();
        glBindTexture(GL_TEXTURE_2D,texture);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,b);MemoryUtil.memFree(b);
    }
    private static void panel(Graphics2D g,int x,int y,int w,int h,String title){g.setColor(new Color(10,17,29,218));g.fillRoundRect(x,y,w,h,16,16);g.setColor(new Color(69,91,119,150));g.drawRoundRect(x,y,w,h,16,16);g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(new Color(133,184,230));g.drawString(title,x+18,y+30);g.setColor(new Color(67,86,111,110));g.drawLine(x+16,y+43,x+w-16,y+43);}
    private static void item(Graphics2D g,int x,int y,String s,boolean selected){if(selected){g.setColor(new Color(43,112,164,110));g.fillRoundRect(x-10,y-22,195,30,8,8);}g.setFont(new Font("Segoe UI",Font.PLAIN,13));g.setColor(selected?Color.WHITE:new Color(177,193,213));g.drawString(s,x,y);}
    private static void row(Graphics2D g,int x,int y,String label,String value,boolean on){g.setFont(new Font("Segoe UI",Font.PLAIN,12));g.setColor(new Color(176,192,211));g.drawString(label,x,y);g.setFont(new Font("Segoe UI",Font.BOLD,11));g.setColor(on?new Color(104,222,174):new Color(238,164,122));g.drawString(value,x+145,y);}
    public void render(){glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glUseProgram(program);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(glGetUniformLocation(program,"hud"),0);glBindVertexArray(vao);glDrawArrays(GL_TRIANGLES,0,6);glBindVertexArray(0);glDisable(GL_BLEND);glEnable(GL_DEPTH_TEST);}
    @Override public void close(){glDeleteProgram(program);glDeleteBuffers(vbo);glDeleteVertexArrays(vao);glDeleteTextures(texture);}
}
