package com.grafkom.viewing3d;

import org.joml.*;
import org.lwjgl.system.MemoryStack;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Objects;
import static org.lwjgl.opengl.GL20.*;

/** Mengelola kompilasi shader dan pengiriman uniform ke GPU. */
public final class ShaderProgram implements AutoCloseable {
 private final int id;
 public ShaderProgram(String vertexPath,String fragmentPath){
  int vs=compile(GL_VERTEX_SHADER,read(vertexPath)), fs=compile(GL_FRAGMENT_SHADER,read(fragmentPath));
  id=glCreateProgram(); glAttachShader(id,vs); glAttachShader(id,fs); glLinkProgram(id);
  if(glGetProgrami(id,GL_LINK_STATUS)==GL_FALSE) throw new IllegalStateException("Shader link gagal: "+glGetProgramInfoLog(id));
  glDeleteShader(vs); glDeleteShader(fs);
 }
 private static String read(String path){try(InputStream in=Objects.requireNonNull(ShaderProgram.class.getResourceAsStream(path),"Resource tidak ditemukan: "+path)){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException e){throw new UncheckedIOException(e);}}
 private static int compile(int type,String src){int s=glCreateShader(type);glShaderSource(s,src);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException("Shader compile gagal: "+glGetShaderInfoLog(s));return s;}
 public void use(){glUseProgram(id);} private int loc(String n){return glGetUniformLocation(id,n);}
 public void set(String n,int v){glUniform1i(loc(n),v);} public void set(String n,float v){glUniform1f(loc(n),v);}
 public void set(String n,Vector3f v){glUniform3f(loc(n),v.x,v.y,v.z);} public void set(String n,Matrix4f m){try(MemoryStack st=MemoryStack.stackPush()){FloatBuffer b=st.mallocFloat(16);m.get(b);glUniformMatrix4fv(loc(n),false,b);}}
 @Override public void close(){glDeleteProgram(id);}
}