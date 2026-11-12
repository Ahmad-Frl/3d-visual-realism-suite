package com.grafkom.viewing3d;
import org.lwjgl.system.MemoryUtil; import java.nio.FloatBuffer; import java.util.*;
import static org.lwjgl.opengl.GL15.*; import static org.lwjgl.opengl.GL20.*; import static org.lwjgl.opengl.GL30.*;
/** Mesh interleaved: posisi(3), normal(3), UV(2). */
public final class Mesh3D implements AutoCloseable {
 private final int vao,vbo,ebo,count; private final boolean indexed;
 private Mesh3D(float[] data,int[] indices){vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);FloatBuffer fb=MemoryUtil.memAllocFloat(data.length);fb.put(data).flip();glBufferData(GL_ARRAY_BUFFER,fb,GL_STATIC_DRAW);MemoryUtil.memFree(fb);glVertexAttribPointer(0,3,GL_FLOAT,false,8*Float.BYTES,0);glEnableVertexAttribArray(0);glVertexAttribPointer(1,3,GL_FLOAT,false,8*Float.BYTES,3L*Float.BYTES);glEnableVertexAttribArray(1);glVertexAttribPointer(2,2,GL_FLOAT,false,8*Float.BYTES,6L*Float.BYTES);glEnableVertexAttribArray(2);indexed=indices!=null;if(indexed){ebo=glGenBuffers();glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);count=indices.length;}else{ebo=0;count=data.length/8;}glBindVertexArray(0);}
 public void draw(){glBindVertexArray(vao);if(indexed)glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0);else glDrawArrays(GL_TRIANGLES,0,count);glBindVertexArray(0);}
 public static Mesh3D plane(){/* Urutan vertex CCW jika dilihat dari atas agar kompatibel dengan back-face culling. */float[] d={-8,0,-8,0,1,0,0,0, -8,0,8,0,1,0,0,8, 8,0,8,0,1,0,8,8, -8,0,-8,0,1,0,0,0, 8,0,8,0,1,0,8,8, 8,0,-8,0,1,0,8,0};return new Mesh3D(d,null);}
 public static Mesh3D cube(){float[] d=new float[36*8];int k=0;float[][] faces={{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};float[][][] v={{{-1,-1,1},{1,-1,1},{1,1,1},{-1,-1,1},{1,1,1},{-1,1,1}},{{1,-1,-1},{-1,-1,-1},{-1,1,-1},{1,-1,-1},{-1,1,-1},{1,1,-1}},{{1,-1,1},{1,-1,-1},{1,1,-1},{1,-1,1},{1,1,-1},{1,1,1}},{{-1,-1,-1},{-1,-1,1},{-1,1,1},{-1,-1,-1},{-1,1,1},{-1,1,-1}},{{-1,1,1},{1,1,1},{1,1,-1},{-1,1,1},{1,1,-1},{-1,1,-1}},{{-1,-1,-1},{1,-1,-1},{1,-1,1},{-1,-1,-1},{1,-1,1},{-1,-1,1}}};for(int f=0;f<6;f++)for(int i=0;i<6;i++){float[] a=v[f][i];d[k++]=a[0];d[k++]=a[1];d[k++]=a[2];for(float n:faces[f])d[k++]=n;d[k++]=(i==1||i==2||i==4)?1:0;d[k++]=(i==2||i==4||i==5)?1:0;}return new Mesh3D(d,null);}
 public static Mesh3D sphere(int stacks,int slices,boolean smooth){List<Float> out=new ArrayList<>();List<Integer> idx=new ArrayList<>();for(int y=0;y<=stacks;y++){double v=(double)y/stacks,phi=Math.PI*v;for(int x=0;x<=slices;x++){double u=(double)x/slices,theta=2*Math.PI*u;float px=(float)(Math.sin(phi)*Math.cos(theta)),py=(float)Math.cos(phi),pz=(float)(Math.sin(phi)*Math.sin(theta));add(out,px,py,pz,px,py,pz,(float)u,(float)v);}}for(int y=0;y<stacks;y++)for(int x=0;x<slices;x++){int a=y*(slices+1)+x,b=a+slices+1;Collections.addAll(idx,a,a+1,b,a+1,b+1,b);}float[] data=new float[out.size()];for(int i=0;i<data.length;i++)data[i]=out.get(i);int[] ids=idx.stream().mapToInt(Integer::intValue).toArray();if(smooth)return new Mesh3D(data,ids); // Flat mesh: duplikasi tiap vertex segitiga dan normal face.
 List<Float> flat=new ArrayList<>();for(int t=0;t<ids.length;t+=3){float[] p=new float[9];for(int j=0;j<3;j++)for(int c=0;c<3;c++)p[j*3+c]=data[ids[t+j]*8+c];org.joml.Vector3f a=new org.joml.Vector3f(p[0],p[1],p[2]),b=new org.joml.Vector3f(p[3],p[4],p[5]),c=new org.joml.Vector3f(p[6],p[7],p[8]);org.joml.Vector3f n=b.sub(a,new org.joml.Vector3f()).cross(c.sub(a,new org.joml.Vector3f())).normalize();for(int j=0;j<3;j++){int off=ids[t+j]*8;for(int q=0;q<3;q++)flat.add(data[off+q]);flat.add(n.x);flat.add(n.y);flat.add(n.z);flat.add(data[off+6]);flat.add(data[off+7]);}}float[] fd=new float[flat.size()];for(int i=0;i<fd.length;i++)fd[i]=flat.get(i);return new Mesh3D(fd,null);}
 /** Quad uji dengan winding sengaja terbalik (back-facing dari kamera awal).
  * Saat GL_BACK culling aktif quad ini harus hilang; saat culling nonaktif ia terlihat. */
 public static Mesh3D cullingProbe(){
  float[] d={
   -.48f,-.38f,0, 0,0,1, 0,0,
   -.48f, .38f,0, 0,0,1, 0,1,
    .48f, .38f,0, 0,0,1, 1,1,
   -.48f,-.38f,0, 0,0,1, 0,0,
    .48f, .38f,0, 0,0,1, 1,1,
    .48f,-.38f,0, 0,0,1, 1,0
  };
  return new Mesh3D(d,null);
 }
 private static void add(List<Float> a,float...v){for(float f:v)a.add(f);} @Override public void close(){glDeleteVertexArrays(vao);glDeleteBuffers(vbo);if(ebo!=0)glDeleteBuffers(ebo);}
}